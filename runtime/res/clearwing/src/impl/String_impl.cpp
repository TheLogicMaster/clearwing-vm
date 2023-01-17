#include <locale>
#include <codecvt>
#include <cstring>

#include <Clearwing.hpp>
#include <java/lang/String.hpp>
#include <java/lang/IndexOutOfBoundsException.hpp>
#include "Utils.hpp"

jarray java::lang::String::SM_bytesToChars_Array1_byte_R_Array1_char(const jarray &bytes, jint offset, jint length, const jstring &encoding) {
    // Todo: Respect encoding
    length = std::min(bytes->length, length);
    if (length + offset > bytes->length or offset < 0 or length < 0)
        vm::throwNew<java::lang::IndexOutOfBoundsException>();
    if (length == 0)
        return make_shared<vm::Array>("C", sizeof(jchar), true, 0);
    auto data = (char *)bytes->data + offset;
    // Todo: Error handling
    auto string = std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t>{}.from_bytes(data, data + length);
    auto array = make_shared<vm::Array>("C", sizeof(jchar), true, (int)string.length());
    memcpy(array->data, string.c_str(), string.length() * 2);
    return array;
}

jarray java::lang::String::M_getBytes_R_Array1_byte(const jstring &encoding) {
    // Todo: Respect encoding
    auto charArray = vm::checkedCast<vm::Array>(F_value);
    if (charArray->length == 0)
        return make_shared<vm::Array>("B", sizeof(jbyte), true, 0);
    auto data = (char16_t *)charArray->data;
    auto string = std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t>{}.to_bytes(data, data + charArray->length);
    auto array = make_shared<vm::Array>("B", sizeof(jbyte), true, (int)string.length());
    memcpy(array->data, string.data(), string.length());
    return array;
}

jbool java::lang::String::M_equals_R_boolean(const jobject &other) {
    if (other.get() == this)
        return true;
    if (!other or other->name != "java/lang/String")
        return false;
    auto string = vm::checkedCast<String>(other);
    if (string->F_count != F_count)
        return false;
    return !std::memcmp(string->F_value->data, F_value->data, sizeof(jchar) * F_count);
}

jbool java::lang::String::M_equalsIgnoreCase_R_boolean(const jstring &other) {
    // Todo: Supposed to throw NPE?
    // Todo: Support more than ascii?
    // Todo: Redo using C++ algorithm
    if (!other or other->name != "java/lang/String")
        return false;
    if (F_count != other->F_count)
        return false;
    auto *array = (jchar *)F_value->data;
    auto *otherArray = (jchar *)other->F_value->data;
    for (int i = 0; i < F_count; i++) {
        jchar c1 = array[i];
        jchar c2 = otherArray[i];
        if ('A' <= c1 and c1 <= 'Z')
            c1 += 'a' - 'A';
        if ('A' <= c2 and c2 <= 'Z')
            c2 += 'a' - 'A';
        if (c1 != c2)
            return false;
    }
    return true;
}

jint java::lang::String::M_hashCode_R_int() {
    if (F_hashCode == 0) {
        if (F_count == 0)
            return 0;
        auto *array = (jchar *)F_value->data;
        for (int i = 0; i < F_count; i++)
            F_hashCode = 31 * F_hashCode + array[i];
    }
    return F_hashCode;
}

jstring java::lang::String::M_replace_R_java_lang_String(const shared_ptr<java::lang::CharSequence> &targetSeq, const shared_ptr<java::lang::CharSequence> &replaceSeq) {
    std::string target = vm::getNativeString(targetSeq->M_toString_R_java_lang_String());
    std::string replacement = vm::getNativeString(replaceSeq->M_toString_R_java_lang_String());
    std::string string = vm::getNativeString(object_cast<String>(get_this()));
    size_t pos = 0;
    while ((pos = string.find(target, pos)) != std::string::npos) {
        string.replace(pos, target.length(), replacement);
        pos += replacement.length();
    }
    return vm::createString(string.begin().base(), string.end().base());
}

jstring java::lang::String::M_toString_R_java_lang_String() {
    return object_cast<java::lang::String>(get_this());
}

void java::lang::String::M_finalize() {
    delete[] (char *)F_nativeString;
}
