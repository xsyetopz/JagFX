#!/usr/bin/env python3
"""OSRS Cache Sound Extractor v2"""

import os
import sys
import struct
import zlib
import argparse
from pathlib import Path
from typing import Optional, List, Tuple
import wave


class IndexReader:
    """Reads OSRS cache index files."""

    def __init__(self, cache_dir: Path):
        self.cache_dir = cache_dir

    def read(self, index_id: int) -> Tuple[int, List[int]]:
        index_file = self.cache_dir / f"main_file_cache.idx{index_id}"
        if not index_file.exists():
            raise FileNotFoundError(f"Index file not found: {index_file}")

        with open(index_file, 'rb') as f:
            data = f.read()

        return self._parse_index(data)

    def _parse_index(self, data: bytes) -> Tuple[int, List[int]]:
        if len(data) < 4:
            return 0, []

        count = struct.unpack('<I', data[:4])[0]
        remaining = len(data) - 4
        expected_sizes = remaining // 2

        if count == expected_sizes:
            sizes = []
            for i in range(expected_sizes):
                offset = 4 + i * 2
                size = struct.unpack('<H', data[offset:offset+2])[0]
                sizes.append(size)
            return count, sizes

        num_entries = len(data) // 2
        sizes = []
        for i in range(num_entries):
            offset = i * 2
            size = struct.unpack('<H', data[offset:offset+2])[0]
            sizes.append(size)
        return len(sizes), sizes


class ArchiveReader:
    """Reads and decompresses archive data."""

    def __init__(self, data_file: Path):
        self.data_file = data_file

    def read(self, offset: int, size: int) -> bytes:
        with open(self.data_file, 'rb') as f:
            f.seek(offset)
            return f.read(size)

    def decompress(self, data: bytes) -> bytes:
        if len(data) < 6:
            raise ValueError(f"Archive data too short: {len(data)} bytes")

        compressed_size = data[0] | (data[1] << 8) | (data[2] << 16)
        compressed_data = data[6:]

        try:
            return zlib.decompress(compressed_data)
        except zlib.error as e:
            raise ValueError(f"Failed to decompress: {e}")


class PCMExtractor:
    """Extracts PCM entries from decompressed archive data."""

    def extract(self, data: bytes) -> List[bytes]:
        entries = []
        if len(data) < 4:
            return entries

        offset = 4
        while offset < len(data):
            if offset + 2 > len(data):
                break

            payload_size = struct.unpack('<h', data[offset:offset + 2])[0]
            offset += 2

            if payload_size <= 0:
                break

            if offset + payload_size > len(data):
                break

            payload = data[offset:offset + payload_size]
            offset += payload_size
            entries.append(payload)

        return entries


class WAVWriter:
    """Writes PCM data as WAV files."""

    @staticmethod
    def save(pcm_data: bytes, output_path: Path, sample_rate: int = 22050):
        with wave.open(str(output_path), 'w') as wav_file:
            wav_file.setnchannels(1)
            wav_file.setsampwidth(1)
            wav_file.setframerate(sample_rate)
            wav_file.writeframes(pcm_data)

    @staticmethod
    def compare(file1: Path, file2: Path) -> Tuple[bool, float]:
        def read_wav_data(path: Path) -> Tuple[int, int, bytes]:
            with wave.open(str(path), 'r') as wav:
                n_channels = wav.getnchannels()
                samp_width = wav.getsampwidth()
                n_frames = wav.getnframes()
                data = wav.readframes(n_frames)
                return n_channels, samp_width, data

        try:
            ch1, sw1, data1 = read_wav_data(file1)
            ch2, sw2, data2 = read_wav_data(file2)

            if ch1 != ch2 or sw1 != sw2:
                return False, -1

            if len(data1) != len(data2):
                return False, -1

            max_diff = max((abs(b1 - b2) for b1, b2 in zip(data1, data2)), default=0)
            return max_diff == 0, max_diff

        except Exception as e:
            print(f"Error comparing files: {e}")
            return False, -1


class CacheExtractor:
    """Orchestrates cache extraction."""

    def __init__(self, cache_dir: str):
        self.cache_dir = Path(cache_dir)
        self.data_file = self.cache_dir / "main_file_cache.dat2"
        self.index_reader = IndexReader(self.cache_dir)
        self.archive_reader = ArchiveReader(self.data_file)
        self.pcm_extractor = PCMExtractor()
        self.wav_writer = WAVWriter()

    def extract_index(self, index_id: int, output_dir: Path) -> List[Path]:
        if not self.data_file.exists():
            raise FileNotFoundError(f"Data file not found: {self.data_file}")

        count, sizes = self.index_reader.read(index_id)
        offsets = self._calculate_offsets(sizes)

        print(f"Index {index_id}: {count} archives")
        output_dir.mkdir(parents=True, exist_ok=True)

        extracted = []
        for i, size in enumerate(sizes):
            try:
                archive_data = self.archive_reader.read(offsets[i], size)
                decompressed = self.archive_reader.decompress(archive_data)
                pcm_entries = self.pcm_extractor.extract(decompressed)

                for j, pcm_data in enumerate(pcm_entries):
                    if len(pcm_data) > 0:
                        output_path = output_dir / f"idx{index_id}_archive{i}_entry{j}.wav"
                        self.wav_writer.save(pcm_data, output_path)
                        extracted.append(output_path)

            except Exception as e:
                print(f"  Error processing archive {i}: {e}")

        return extracted

    def extract_all_indices(self, output_dir: Path, indices: Optional[List[int]] = None) -> dict:
        if indices is None:
            indices = [4, 14, 15]

        results = {}
        for index_id in indices:
            try:
                extracted = self.extract_index(index_id, output_dir / f"idx{index_id}")
                results[index_id] = extracted
                print(f"Index {index_id}: extracted {len(extracted)} sounds")
            except FileNotFoundError as e:
                print(f"Index {index_id}: {e}")
                results[index_id] = []
            except Exception as e:
                print(f"Index {index_id}: Error - {e}")
                results[index_id] = []

        return results

    @staticmethod
    def _calculate_offsets(sizes: List[int]) -> List[int]:
        offsets = []
        current_offset = 0
        for size in sizes:
            offsets.append(current_offset)
            current_offset += size
        return offsets


def main():
    parser = argparse.ArgumentParser(
        description="Extract sound files from OSRS cache (v2 format)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python cache_extractor_v2.py --all
  python cache_extractor_v2.py --index 14 --cache-dir /path/to/cache
  python cache_extractor_v2.py --compare --reference-dir references/synths
        """
    )

    parser.add_argument('--all', '-a', action='store_true', help='Extract from all indices')
    parser.add_argument('--index', '-i', type=int, choices=[4, 14, 15], help='Extract from specific index')
    parser.add_argument('--cache-dir', '-c', type=str, default='~/.runelite/jagexcache/oldschool/LIVE')
    parser.add_argument('--output', '-o', type=str, default='references/synths/extracted_v2')
    parser.add_argument('--compare', '-m', action='store_true', help='Compare with reference files')
    parser.add_argument('--reference-dir', '-r', type=str, default='references/synths')

    args = parser.parse_args()

    if not args.all and not args.index:
        parser.error("Must specify --all or --index")

    try:
        cache_dir = os.path.expanduser(args.cache_dir)
        extractor = CacheExtractor(cache_dir)
        output_dir = Path(args.output)

        if args.index:
            extracted = extractor.extract_index(args.index, output_dir / f"idx{args.index}")
            print(f"Extracted {len(extracted)} sounds")

        elif args.all:
            results = extractor.extract_all_indices(output_dir, [4, 14, 15])
            total = sum(len(files) for files in results.values())
            print(f"Total extracted: {total} sounds")

            if args.compare:
                _compare_results(results, Path(args.reference_dir))

    except (FileNotFoundError, Exception) as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


def _compare_results(results: dict, reference_dir: Path):
    if not reference_dir.exists():
        print(f"Reference directory not found: {reference_dir}")
        return

    reference_files = {
        ref_file.stem.replace('_cs', ''): ref_file
        for ref_file in reference_dir.glob("*_cs.wav")
    }

    matches = mismatches = 0
    for index_id, extracted_files in results.items():
        for extracted_file in extracted_files:
            extracted_name = extracted_file.stem
            for ref_base, ref_file in reference_files.items():
                if extracted_name.replace(f'idx{index_id}_archive', '') in ref_base:
                    are_equal, max_diff = WAVWriter.compare(extracted_file, ref_file)
                    if are_equal:
                        matches += 1
                        print(f"  MATCH: {extracted_file.name}")
                    else:
                        mismatches += 1
                        print(f"  MISMATCH: {extracted_file.name} (diff: {max_diff})")
                    break

    print(f"Results: {matches} matches, {mismatches} mismatches")


if __name__ == "__main__":
    main()
