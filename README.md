# Clearwing VM
[![Release](https://jitpack.io/v/com.thelogicmaster/clearwing-vm.svg)](https://jitpack.io/#com.thelogicmaster/clearwing-vm)

## About
This is a fork of the CodenameOne Parpar VM that produces portable C and C++ and implements more of the Java
runtime library. It was primarily created for the [SwitchGDX](https://github.com/TheLogicMaster/switch-gdx) Nintendo Switch 
LibGDX backend but doesn't depend on it. An example project is provided in the `example` directory.

## Features implemented
- Regex (RegExodus)
- Basic File I/O
- Buffers (Only direct buffers and ByteBuffer wrappers are supported for now)
- System language/locale
- Field, Method, Constructor, and Class Annotation Reflection

## Todo
- Internal VM logging
- Switch wrapper buffers to use memory directly since it's always aligned, rather than the super inefficient byte by byte implementation (Profile using BufferUtilsTest)
- Ensure code licensing is all good (Probably noting modifications to comply with GPLv2)
- Fix java.util.concurrent stuff (such as Atomic*)
- Enable NPE checks, possibly enhance the feature further
- VSCode debugging testing/instructions
- Organize natives better, probably port C native function implementations to C++
- Debug server (Stretch goal)
- Proper Unicode support (Character.isLetterOrDigit(), for instance)
- Fix Date formatting
- Make main Thread catch exceptions like other Threads and fix UncaughtExceptionHandler
- Add internal types to non-optimized list internally
- Object.clone()
- Fix GZIP compression
- Fix try-catch/synchronized block nesting (See SwitchGDX ThreadTest)
- Dynamic cast checks
- Reformat runtime and transpiler sources

## Notes
- Requires retrolambda for lambda, default method, and method reference support (RetroLambda Gradle plugin doesn't support Gradle 7)
- Exceptions thrown without a try-catch block on the main Thread are ignored
- JDK-8 is required for compiling the Transpiler project
- Classes referenced with reflection must be included in the JSON config file
- Only supports up to 3D arrays
- Sometimes the Runtime fails to compile, just try it again
- Ensure that the Runtime is always fully rebuilt using _Build_->_Rebuild Project_ in Intellij

## Bugs Fixed
- For exceptions, local variable restoreTo* must be volatile, presumably as a result of setjmp
- Parameter evaluation order isn't specified in C, so POPs in member setters must be evaluated outside
- JAVA_ARRAY_BYTE must be explicitly `unsigned char`, since it's compiler specific behavior otherwise
- Array components in multidimensional arrays should refer to array classes of decreasing size
- Added dedicated primitive types
