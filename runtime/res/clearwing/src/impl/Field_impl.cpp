#include "Clearwing.hpp"
#include <java/lang/reflect/Field.hpp>
#include <java/lang/IllegalArgumentException.hpp>
#include <Utils.hpp>

jobject java::lang::reflect::Field::M_get_R_java_lang_Object(const jobject &object) {
    auto clazz = F_type;
    clazz->M_ensureInitialized();
    auto isStatic = M_isStatic_R_boolean();
    auto declaringClass = F_declaringClass;
    if (!isStatic and !declaringClass->M_isInstance_R_boolean(object))
        vm::throwNew<java::lang::IllegalArgumentException>();
    ((ClassData *) declaringClass->F_nativeData)->staticInitializer();
    return ((field_getter_ptr)F_getter)(object);
}

void java::lang::reflect::Field::M_set(const jobject &object, const jobject &value) {
    auto clazz = F_type;
    clazz->M_ensureInitialized();
    auto isStatic = M_isStatic_R_boolean();
    auto declaringClass = F_declaringClass;
    if ((!isStatic and !declaringClass->M_isInstance_R_boolean(object)) or (value and !clazz->F_primitive and !clazz->M_isInstance_R_boolean(value)))
        vm::throwNew<java::lang::IllegalArgumentException>();
    ((ClassData *) declaringClass->F_nativeData)->staticInitializer();
    ((field_setter_ptr)F_setter)(object, value);
}
