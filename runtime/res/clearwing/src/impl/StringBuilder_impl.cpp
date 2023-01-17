#include <Clearwing.hpp>
#include <java/lang/StringBuilder.hpp>
#include <java/lang/ArrayIndexOutOfBoundsException.hpp>
#include "java/lang/System.hpp"
#include "Utils.hpp"

shared_ptr<java::lang::StringBuilder> java::lang::StringBuilder::M_append_R_java_lang_StringBuilder(jchar c) {
    if (F_count == vm::checkedCast<vm::Array>(F_value)->length)
        M_enlargeBuffer(F_count + 1);
    vm::checkedCast<vm::Array>(F_value)->get<jchar>(F_count) = c;
    F_count++;
    return object_cast<java::lang::StringBuilder>(get_this());
}

shared_ptr<java::lang::StringBuilder> java::lang::StringBuilder::M_append_R_java_lang_StringBuilder(const jobject &object) {
    if (!object) {
        M_appendNull();
        return object_cast<java::lang::StringBuilder>(get_this());
    }
    return M_append_R_java_lang_StringBuilder(object->M_toString_R_java_lang_String());
}

shared_ptr<java::lang::StringBuilder> java::lang::StringBuilder::M_append_R_java_lang_StringBuilder(const jstring &string) {
    if (!string) {
        M_appendNull();
        return object_cast<java::lang::StringBuilder>(get_this());
    }
    int newLength = F_count + string->F_count;
    if (newLength > vm::checkedCast<vm::Array>(F_value)->length)
        M_enlargeBuffer(newLength);
    string->M_getChars_Array1_char(0, string->F_count, F_value, F_count);
    F_count = newLength;
    return object_cast<java::lang::StringBuilder>(get_this());
}

jchar java::lang::StringBuilder::M_charAt_R_char(jint index) {
    auto array = vm::checkedCast<vm::Array>(F_value);
    if (index < 0 or index >= array->length)
        vm::throwNew<java::lang::IndexOutOfBoundsException>();
    return array->get<jchar>(index);
}

void java::lang::StringBuilder::M_getChars_Array1_char(jint start, jint end, const jarray &dst, jint dstStart) {
    java::lang::System::SM_arraycopy(F_value, start, dst, dstStart, end - start);
}
