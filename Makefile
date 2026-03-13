# JagFx build targets
#
# Development
#   make run            run the desktop GUI
#   make build          build (debug)
#   make test           run all tests
#
# Release -- single-file publish (unsigned)
#   make publish-macos-arm64
#   make publish-macos-x64
#   make publish-windows
#   make publish-linux
#
# Release -- distributable archives
#   make release-macos-arm64    .tar.gz (.app bundle, Apple Silicon)
#   make release-macos-x64      .tar.gz (.app bundle, Intel)
#   make release-macos           both arches
#   make release-linux           .tar.gz
#   make release-windows         .zip
#   make release-all             all platforms

DESKTOP  := src/JagFx.Desktop
CONF     := Release
VERSION  := $(shell grep -oE '<Version>[^<]+' Directory.Build.props | head -1 | sed 's/<Version>//')

.PHONY: run build test \
        publish-macos-arm64 publish-macos-x64 publish-windows publish-linux \
        release-macos-arm64 release-macos-x64 release-macos \
        release-linux release-windows release-all

# -- Development --------------------------------------------------------------

run:
	dotnet run --project $(DESKTOP)

build:
	dotnet build --nologo

test:
	dotnet test --nologo

# -- Unsigned publish ---------------------------------------------------------

publish-macos-arm64:
	dotnet publish $(DESKTOP) -c $(CONF) -r osx-arm64 -o publish/osx-arm64 --nologo

publish-macos-x64:
	dotnet publish $(DESKTOP) -c $(CONF) -r osx-x64 -o publish/osx-x64 --nologo

publish-windows:
	dotnet publish $(DESKTOP) -c $(CONF) -r win-x64 --self-contained -o publish/win-x64 --nologo

publish-linux:
	dotnet publish $(DESKTOP) -c $(CONF) -r linux-x64 --self-contained -o publish/linux-x64 --nologo

# -- Distributable archives ---------------------------------------------------

release-macos-arm64: publish-macos-arm64
	rm -f publish/osx-arm64/JagFx.app/Contents/MacOS/*.pdb
	tar -czf publish/JagFx-$(VERSION)-osx-arm64.tar.gz -C publish/osx-arm64 JagFx.app

release-macos-x64: publish-macos-x64
	rm -f publish/osx-x64/JagFx.app/Contents/MacOS/*.pdb
	tar -czf publish/JagFx-$(VERSION)-osx-x64.tar.gz -C publish/osx-x64 JagFx.app

release-macos: release-macos-arm64 release-macos-x64

release-linux: publish-linux
	rm -f publish/linux-x64/*.pdb
	tar -czf publish/JagFx-$(VERSION)-linux-x64.tar.gz -C publish/linux-x64 .

release-windows: publish-windows
	rm -f publish/win-x64/*.pdb
	cd publish/win-x64 && zip -rq ../JagFx-$(VERSION)-win-x64.zip .

release-all: release-macos release-linux release-windows
