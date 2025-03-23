# Alesia - Experimental Mutliplatform GameBoy Emulator
Alesia uses Kotlin Multiplatform to create the core GameBoy emulator logic and Compose Multiplatform for the rendering.

Platform supported are Windows, Android and Web assembly.

## Project Architecture
- Alesia
  - Gradle module containing the core GameBoy emulator logic with CPU and PPU emulation
  - This module does not have any platform specific code such as disk read/write and rendering
- DesktopApp
  - Gradle module depending on Alesia module and provides the platform specific logic for Desktop
  - This produces a JVM application making use Compose for Desktop for rendering
- AndroidApp
  - Gradle module depending on Alesia module and provides the platform specific logic for Android
  - This produces aa Android application making use Jetpack Compose for rendering
- WebApp
  - Gradle module depending on Alesia module and provides the platform specific logic for Web via WebAssembly
  - This produces a Web application making use Compose for WASM for rendering
