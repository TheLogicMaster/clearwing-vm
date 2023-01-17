#include <Clearwing.hpp>
#include <java/lang/Integer.hpp>
#include "Utils.hpp"
#include <charconv>

jstring java::lang::Integer::SM_toString_R_java_lang_String(jint value, jint radix) {
    std::string str;
    str.resize(20);
    std::to_chars(str.data(), str.data() + str.length(), value, radix);
    return vm::createString(str.c_str());
}
