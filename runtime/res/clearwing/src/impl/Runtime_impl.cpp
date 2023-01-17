#include <Clearwing.hpp>
#include <java/lang/Runtime.hpp>

// Todo: Implement using mallinfo, possibly. Or potentially use Object constructor/destructor somehow. Or custom allocators.

jlong java::lang::Runtime::SM_totalMemoryImpl_R_long() {
    return 0;
}

jlong java::lang::Runtime::SM_freeMemoryImpl_R_long() {
    return 0;
}
