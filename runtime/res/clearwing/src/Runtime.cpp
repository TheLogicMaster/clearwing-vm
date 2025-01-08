#include "java/lang/Runtime.h"

extern "C" {

jlong SM_java_lang_Runtime_totalMemoryImpl_R_long(jcontext ctx) {
    return GC_HEAP_THRESHOLD;
}

jlong SM_java_lang_Runtime_freeMemoryImpl_R_long(jcontext ctx) {
    return GC_HEAP_THRESHOLD - getHeapUsage();
}

jlong SM_java_lang_Runtime_maxMemoryImpl_R_long(jcontext ctx) {
    return GC_HEAP_THRESHOLD;
}

}
