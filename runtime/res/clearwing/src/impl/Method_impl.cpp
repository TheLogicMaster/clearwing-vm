#include "Clearwing.hpp"
#include <java/lang/reflect/Method.hpp>
#include "Utils.hpp"

jobject java::lang::reflect::Method::M_invoke_Array1_java_lang_Object_R_java_lang_Object(const jobject &object, const jarray &args) {
    ((ClassData *)F_declaringClass->F_nativeData)->staticInitializer();
    return ((reflect_method_ptr)F_ptr)(object, args);
}
