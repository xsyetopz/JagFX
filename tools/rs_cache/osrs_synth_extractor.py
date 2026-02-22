#!/usr/bin/env python3
"""Extract .synth files from RuneLite cache."""

import argparse
import gzip
import bz2
import struct
from pathlib import Path
from typing import Optional, Tuple

SECTOR_SIZE = 520
HEADER_SIZE = 8
DATA_SIZE = SECTOR_SIZE - HEADER_SIZE

def read_medium(b: bytes) -> int:
    return (b[0] << 16) | (b[1] << 8) | b[2]

def read_index_entry(idx_path: Path, group_id: int) -> Optional[Tuple[int, int]]:
    with open(idx_path, "rb") as f:
        f.seek(group_id * 6)
        entry = f.read(6)
        if len(entry) < 6:
            return None
        file_size = read_medium(entry[0:3])
        first_sector = read_medium(entry[3:6])
        if file_size == 0:
            return None
        return file_size, first_sector

def read_group_from_dat2(
    dat2_path: Path,
    archive_id: int,
    group_id: int,
    file_size: int,
    first_sector: int
) -> bytes:
    buf = bytearray()
    remaining = file_size
    sector_id = first_sector
    chunk = 0

    with open(dat2_path, "rb") as f:
        while remaining > 0 and sector_id != 0:
            f.seek(sector_id * SECTOR_SIZE)
            raw = f.read(SECTOR_SIZE)
            if len(raw) < SECTOR_SIZE:
                raise IOError("Short read on dat2")

            next_sector = read_medium(raw[4:7])
            data = raw[HEADER_SIZE:HEADER_SIZE + DATA_SIZE]
            to_take = min(remaining, DATA_SIZE)
            buf.extend(data[:to_take])
            remaining -= to_take
            sector_id = next_sector
            chunk += 1

    return bytes(buf)

def parse_js5_container(buf: bytes) -> Tuple[int, int, bytes, bytes]:
    compression_type = buf[0]
    comp_len = struct.unpack(">I", buf[1:5])[0]
    offset = 5

    if compression_type == 0:
        uncompressed_len = comp_len
    else:
        uncompressed_len = struct.unpack(">I", buf[offset:offset+4])[0]
        offset += 4

    payload = buf[offset:offset+comp_len]
    remainder = buf[offset+comp_len:]
    return compression_type, uncompressed_len, payload, remainder

def decompress_js5(
    compression_type: int,
    payload: bytes,
    expected_len: Optional[int] = None
) -> bytes:
    if compression_type == 0:
        data = payload
    elif compression_type == 1:
        data = bz2.decompress(payload)
    elif compression_type == 2:
        data = gzip.decompress(payload)
    else:
        raise ValueError(f"Unknown compression type {compression_type}")

    if expected_len is not None and len(data) != expected_len:
        print(f"Warning: Length mismatch - got {len(data)}, expected {expected_len}")

    return data

def extract_group(
    cache_dir: Path,
    archive_id: int,
    group_id: int,
    raw: bool = False
) -> Optional[bytes]:
    dat2_path = cache_dir / "main_file_cache.dat2"
    idx_path = cache_dir / f"main_file_cache.idx{archive_id}"

    if not idx_path.exists():
        return None

    entry = read_index_entry(idx_path, group_id)
    if entry is None:
        return None

    file_size, first_sector = entry
    container = read_group_from_dat2(dat2_path, archive_id, group_id, file_size, first_sector)

    if raw:
        return container

    compression_type, uncompressed_len, payload, _ = parse_js5_container(container)
    return decompress_js5(compression_type, payload, uncompressed_len)

def dump_index(
    cache_dir: Path,
    archive_id: int,
    out_dir: Path,
    start_id: int = 0,
    end_id: Optional[int] = None,
    raw: bool = False
) -> int:
    idx_path = cache_dir / f"main_file_cache.idx{archive_id}"

    if not idx_path.exists():
        print(f"Index file not found: {idx_path}")
        return 0

    index_out_dir = out_dir / f"idx{archive_id}"
    index_out_dir.mkdir(parents=True, exist_ok=True)

    if end_id is None:
        idx_size = idx_path.stat().st_size
        end_id = (idx_size // 6) - 1

    print(f"Dumping idx{archive_id} (groups {start_id}-{end_id})...")

    extracted = 0
    for group_id in range(start_id, end_id + 1):
        data = extract_group(cache_dir, archive_id, group_id, raw=raw)

        if data is None:
            continue

        ext = ".bin" if raw else ".synth"
        out_path = index_out_dir / f"{group_id:04d}{ext}"
        out_path.write_bytes(data)
        extracted += 1

    print(f"Extracted {extracted} groups from idx{archive_id}")
    return extracted

def main():
    parser = argparse.ArgumentParser(description="Extract .synth files from RuneLite cache")
    parser.add_argument("--cache", type=Path, required=True, help="Path to RuneLite LIVE cache directory")
    parser.add_argument("--out", type=Path, default=Path("./dump_synth"), help="Output directory")
    parser.add_argument("--indices", type=int, nargs="+", default=[4], choices=[4, 14, 15], help="Indices to dump")
    parser.add_argument("--start-id", type=int, default=0, help="Starting group ID")
    parser.add_argument("--end-id", type=int, default=None, help="Ending group ID")
    parser.add_argument("--raw", action="store_true", help="Write raw JS5 containers")

    args = parser.parse_args()

    if not args.cache.exists():
        print(f"Error: Cache directory does not exist: {args.cache}")
        return 1

    dat2_path = args.cache / "main_file_cache.dat2"
    if not dat2_path.exists():
        print(f"Error: Data file not found: {dat2_path}")
        return 1

    print(f"Cache: {args.cache}")
    print(f"Output: {args.out}")
    print(f"Indices: {args.indices}")

    args.out.mkdir(parents=True, exist_ok=True)

    total_extracted = 0
    for archive_id in args.indices:
        extracted = dump_index(
            args.cache,
            archive_id,
            args.out,
            start_id=args.start_id,
            end_id=args.end_id,
            raw=args.raw
        )
        total_extracted += extracted

    print(f"Total: {total_extracted} groups extracted")
    return 0

if __name__ == "__main__":
    exit(main())
