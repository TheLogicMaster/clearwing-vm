#include "java/lang/System.h"
#include "java/lang/ArrayStoreException.h"
#include "java/lang/IllegalArgumentException.h"

#include <cstring>

extern "C" {

void SM_java_lang_System_arraycopy_java_lang_Object_int_java_lang_Object_int_int(jcontext ctx, jobject src, jint srcOffset, jobject dst, jint dstOffset, jint length) {
    if (jclass(src->clazz)->arrayDimensions == 0 or jclass(dst->clazz)->arrayDimensions == 0)
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
    struct timeval time{};
    gettimeofday(&time, nullptr);
    return (((jlong)time.tv_sec) * 1000) + (time.tv_usec / 1000);
}

void SM_java_lang_System_exit0_int(jcontext ctx, jint code) {
    exit(code); // Todo: Cleanup
}

jobject SM_java_lang_System_getProperty_java_lang_String_R_java_lang_String(jcontext ctx, jobject keyObj) {
    static std::map<const char *, jobject> cache;
    static std::mutex lock;
    auto key = stringToNative(ctx, (jstring) keyObj);
    if (!strcmp(key, "java.runtime.name"))
        return (jobject) createStringLiteral(ctx, u8"Clearwing"_j);
    try {
        return cache.at(key);
    } catch (std::out_of_range &ex) {
        auto value = getSystemProperty(key);
        if (value) {
            auto string = (jobject) stringFromNative(ctx, value);
            lock.lock();
            cache[key] = string;
            lock.unlock();
            return string;
        }
    }
    return nullptr;
}

}
