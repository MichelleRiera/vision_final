@echo off
"C:\\Users\\Miche\\AppData\\Local\\Android\\Sdk\\cmake\\3.18.1\\bin\\cmake.exe" ^
  "-HC:\\Users\\Miche\\Desktop\\UNI\\OCTAVO\\VISION\\SEGUNDO INTERCICLO\\PROYECTO\\visio_final\\opencv\\libcxx_helper" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=21" ^
  "-DANDROID_PLATFORM=android-21" ^
  "-DANDROID_ABI=x86" ^
  "-DCMAKE_ANDROID_ARCH_ABI=x86" ^
  "-DANDROID_NDK=C:\\Users\\Miche\\AppData\\Local\\Android\\Sdk\\ndk\\23.1.7779620" ^
  "-DCMAKE_ANDROID_NDK=C:\\Users\\Miche\\AppData\\Local\\Android\\Sdk\\ndk\\23.1.7779620" ^
  "-DCMAKE_TOOLCHAIN_FILE=C:\\Users\\Miche\\AppData\\Local\\Android\\Sdk\\ndk\\23.1.7779620\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=C:\\Users\\Miche\\AppData\\Local\\Android\\Sdk\\cmake\\3.18.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=C:\\Users\\Miche\\Desktop\\UNI\\OCTAVO\\VISION\\SEGUNDO INTERCICLO\\PROYECTO\\visio_final\\opencv\\build\\intermediates\\cxx\\Debug\\5u374r4k\\obj\\x86" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=C:\\Users\\Miche\\Desktop\\UNI\\OCTAVO\\VISION\\SEGUNDO INTERCICLO\\PROYECTO\\visio_final\\opencv\\build\\intermediates\\cxx\\Debug\\5u374r4k\\obj\\x86" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BC:\\Users\\Miche\\Desktop\\UNI\\OCTAVO\\VISION\\SEGUNDO INTERCICLO\\PROYECTO\\visio_final\\opencv\\.cxx\\Debug\\5u374r4k\\x86" ^
  -GNinja ^
  "-DANDROID_STL=c++_shared"
