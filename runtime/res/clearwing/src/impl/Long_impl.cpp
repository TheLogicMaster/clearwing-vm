#include <Clearwing.hpp>
#include <java/lang/Long.hpp>
#include "Utils.hpp"
#include <charconv>

jstring java::lang::Long::SM_toString_R_java_lang_String(jlong value, jint radix) {
    std::string str;
    str.resize(30);
    std::to_chars(str.data(), str.data() + str.length(), value, radix);
    return vm::createString(str.c_str());
}
