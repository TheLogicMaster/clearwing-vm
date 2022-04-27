# Clearwing VM
[![Release](https://jitpack.io/v/com.thelogicmaster/clearwing-vm.svg)](https://jitpack.io/#com.thelogicmaster/clearwing-vm)

## About
This is a fork of the CodenameOne Parpar VM that produces portable C and C++ and implements more of the Java
runtime library. It was primarily created for the SwitchGDX Nintendo Switch LibGDX backend but doesn't depend
on it.

## Features implemented
- Regex (RegExodus)
- Basic File I/O
- Buffers (Only direct buffers and ByteBuffer wrappers are supported for now)
- System language/locale
- Field and Method Reflection

## Todo
- Internal VM logging
- Interruptable Thread.sleep() for clean exit
- Incremental compilation, if possible, to cut down on compilation times (Something like rsync to only copy changed files into dist)
- Switch wrapper buffers to use memory directly since it's always aligned, rather than the super inefficient byte by byte implementation (Profile using BufferUtilsTest)
- Ensure code licensing is all good (Probably noting modifications to comply with GPLv2)
- Remove any unnecessary java.util.concurrent stuff
- Replace any class stubs with generic equivalents
- Enable NPE checks, possibly enhance the feature further
- VSCode debugging testing/instructions
- Organize natives better, probably port C native function implementations to C++
- Debug server
- Don't optimize out methods needed for reflection
- Include native source files from dependency JARs
- Constructor reflection
- Example Gradle project
- Proper Unicode support (Character.isLetterOrDigit(), for instance)
- Fix Date formatting

## Notes
- Requires retrolambda for lambda support (Use pre-v7 Gradle wrapper)
- Exceptions thrown without a try-catch block are ignored (Maybe adding try-catch block in thread init code, in addition to main function)
- JDK-8 is required for compiling the Transpiler project
- Classes referenced with reflection must be included in the JSON config file
- Only supports up to 3D arrays
- Sometimes the Runtime fails to compile, just try it again

## Bugs Fixed
- For exceptions, local variable restoreTo* must be volatile, presumably as a result of setjmp
- Parameter evaluation order isn't specified in C, so POPs in member setters must be evaluated outside
