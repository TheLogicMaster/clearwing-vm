#include "Clearwing.hpp"
#include "Array.hpp"
#include "Utils.hpp"
#include <java/lang/System.hpp>
#include <java/lang/IndexOutOfBoundsException.hpp>
#include <java/lang/ArrayStoreException.hpp>
#include <java/lang/NullPointerException.hpp>
#include <algorithm>

#include <cstring>
#include <utility>
#ifdef __WINRT__
#include <winsock2.h> // Needed for timeval struct...
#else
#include <sys/time.h>
#endif

#ifdef __WINRT__
// https://stackoverflow.com/a/26085827
int gettimeofday(struct timeval* tp, struct timezone* tzp) {
    // Note: some broken versions only have 8 trailing zero's, the correct epoch has 9 trailing zero's
    // This magic number is the number of 100 nanosecond intervals since January 1, 1601 (UTC)
    // until 00:00:00 January 1, 1970
    static const uint64_t EPOCH = ((uint64_t)116444736000000000ULL);

    SYSTEMTIME  system_time;
    FILETIME    file_time;
    uint64_t    time;

    GetSystemTime(&system_time);
    SystemTimeToFileTime(&system_time, &file_time);
    time = ((uint64_t)file_time.dwLowDateTime);
    time += ((uint64_t)file_time.dwHighDateTime) << 32;

    tp->tv_sec = (jlong)((time - EPOCH) / 10000000L);
    tp->tv_usec = (jlong)(system_time.wMilliseconds * 1000);
    return 0;
}
#endif

void java::lang::System::SM_arraycopy(const jobject &src, jint srcOffset, const jobject &dst, jint dstOffset, jint length) {
    if (!src or !dst)
        vm::throwNew<java::lang::NullPointerException>();
    auto srcArray = vm::checkedCast<vm::Array>(src);
    auto dstArray = vm::checkedCast<vm::Array>(dst);
    if (srcArray->primitive != dstArray->primitive or (srcArray->primitive and src->name != dst->name))
        vm::throwNew<java::lang::ArrayStoreException>();
    if (srcOffset < 0 or dstOffset < 0 or length < 0 or srcOffset + length > srcArray->length or dstOffset + length > dstArray->length)
        vm::throwNew<java::lang::IndexOutOfBoundsException>();
    if (srcArray->primitive) {
        auto arrayClass = src->M_getClass_R_java_lang_Class();
        arrayClass->M_ensureInitialized();
        auto componentClass = arrayClass->M_getComponentType_R_java_lang_Class();
        componentClass->M_ensureInitialized();
        auto size = componentClass->F_size;
        if (src.get() == dst.get())
            memmove((char *)dstArray->data + dstOffset * size, (char *)srcArray->data + srcOffset * size, length * size);
        else
            memcpy((char *)dstArray->data + dstOffset * size, (char *)srcArray->data + srcOffset * size, length * size);
    } else {
        if (src.get() == dst.get()) {
            vector<jobject> temp(length);
            std::copy_n((jobject *)srcArray->data + srcOffset, length, temp.data());
            std::copy_n(temp.data(), length, (jobject *) dstArray->data + dstOffset);
        } else
            for (int i = 0; i < length; i++)
                ((jobject *) dstArray->data)[i + dstOffset] = ((jobject *) srcArray->data)[i + srcOffset];
    }
}

jlong java::lang::System::SM_currentTimeMillis_R_long() {
    struct timeval time{};
    gettimeofday(&time, nullptr);
    jlong l = (((jlong)time.tv_sec) * 1000) + (time.tv_usec / 1000);
    return l;
}

void java::lang::System::SM_exit0(jint code) {
    exit(code);
}

jstring java::lang::System::SM_getProperty_R_java_lang_String(const jstring &keyObj) {
    std::string key = vm::getNativeString(keyObj);
    auto value = vm::getSystemProperty(key);
    if (!value.empty())
        return vm::createString(value.c_str());
    if (key == "java.runtime.name")
        return vm::createString("Clearwing");
    return nullptr;
}
