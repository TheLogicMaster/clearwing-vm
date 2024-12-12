#include "java/lang/System.h"
#include "java/lang/ArrayStoreException.h"
#include "java/lang/IllegalArgumentException.h"

#include <cstring>
#include <chrono>

extern "C" {

void SM_java_lang_System_arraycopy_java_lang_Object_int_java_lang_Object_int_int(jcontext ctx, jobject src, jint srcOffset, jobject dst, jint dstOffset, jint length) {
    if (jclass(NULL_CHECK(src)->clazz)->arrayDimensions == 0 or jclass(NULL_CHECK(dst)->clazz)->arrayDimensions == 0)
        constructAndThrow<&class_java_lang_IllegalArgumentException, init_java_lang_IllegalArgumentException>(ctx);
    auto srcArray = (jarray) src;
    auto dstArray = (jarray) dst;
    auto srcType = (jclass) jclass(src->clazz)->componentClass;
    auto dstType = (jclass) jclass(dst->clazz)->componentClass;
    if (srcType->primitive != dstType->primitive or (srcType->primitive and srcType->nativeName != dstType->nativeName))
        constructAndThrow<&class_java_lang_ArrayStoreException, init_java_lang_ArrayStoreException>(ctx);
    if (srcOffset < 0 or dstOffset < 0 or length < 0 or srcOffset + length > srcArray->length or dstOffset + length > dstArray->length)
        throwIndexOutOfBounds(ctx);
    auto size = srcType->primitive ? srcType->size : sizeof(jobject);
    if (src == dst)
        memmove((char *) dstArray->data + dstOffset * size, (char *) srcArray->data + srcOffset * size, length * size);
    else
        memcpy((char *) dstArray->data + dstOffset * size, (char *) srcArray->data + srcOffset * size, length * size);
}

jlong SM_java_lang_System_currentTimeMillis_R_long(jcontext ctx) {
    return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
}

void SM_java_lang_System_exit0_int(jcontext ctx, jint code) {
    exit(code); // Todo: Cleanup
}

void SM_java_lang_System_gc(jcontext ctx) {
    runGC(ctx);
}

jobject SM_java_lang_System_getProperty_java_lang_String_R_java_lang_String(jcontext ctx, jobject keyObj) {
    static std::map<const char *, const char *> cache;
    static std::mutex lock;
    auto key = stringToNative(ctx, (jstring) NULL_CHECK(keyObj));
    if (!strcmp(key, "java.runtime.name"))
        return (jobject) createStringLiteral(ctx, u8"Clearwing"_j);
    std::lock_guard guard(lock);
    auto it = cache.find(key);
    if (it != cache.end())
        return (jobject) stringFromNative(ctx, it->second);
    auto &entry = cache[key];
    entry = getSystemProperty(key);
    return entry ? (jobject) stringFromNative(ctx, entry) : nullptr;
}

}
