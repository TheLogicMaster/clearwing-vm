#include "Clearwing.hpp"
#include <java/lang/reflect/Array.hpp>
#include <java/lang/IndexOutOfBoundsException.hpp>
#include "Utils.hpp"

jobject java::lang::reflect::Array::SM_newInstanceImpl_R_java_lang_Object(const jclass &type, jint length) {
    return vm::newArray(vm::checkedCast<Class>(type), length);
}

void java::lang::reflect::Array::SM_set(const jobject &arrayObj, jint index, const jobject &value) {
    auto array = vm::checkedCast<vm::Array>(arrayObj);
    if (index >= array->length)
        vm::throwNew<java::lang::IndexOutOfBoundsException>();
    if (!array->primitive)
        array->get<jobject>(index) = value;
    auto clazz = vm::getClass(array->name.c_str())->M_getComponentType_R_java_lang_Class();
    if (clazz == vm::classBoolean)
        array->get<jbool>(index) = vm::unwrap<jbool>(value);
    else if (clazz == vm::classByte)
        array->get<jbyte>(index) = vm::unwrap<jbyte>(value);
    else if (clazz == vm::classChar)
        array->get<jchar>(index) = vm::unwrap<jchar>(value);
    else if (clazz == vm::classShort)
        array->get<jshort>(index) = vm::unwrap<jshort>(value);
    else if (clazz == vm::classInt)
        array->get<jint>(index) = vm::unwrap<jint>(value);
    else if (clazz == vm::classLong)
        array->get<jlong>(index) = vm::unwrap<jlong>(value);
    else if (clazz == vm::classFloat)
        array->get<jfloat>(index) = vm::unwrap<jfloat>(value);
    else if (clazz == vm::classDouble)
        array->get<jdouble>(index) = vm::unwrap<jdouble>(value);
}

jobject java::lang::reflect::Array::SM_get_R_java_lang_Object(const jobject &arrayObj, jint index) {
    auto array = vm::checkedCast<vm::Array>(arrayObj);
    if (index >= array->length)
        vm::throwNew<java::lang::IndexOutOfBoundsException>();
    if (!array->primitive)
        return array->get<jobject>(index);
    auto clazz = vm::getClass(array->name.c_str())->M_getComponentType_R_java_lang_Class();
    if (clazz == vm::classBoolean)
        return vm::wrap<Boolean>(array->get<jbool>(index));
    else if (clazz == vm::classByte)
        return vm::wrap<Byte>(array->get<jbyte>(index));
    else if (clazz == vm::classShort)
        return vm::wrap<Short>(array->get<jshort>(index));
    else if (clazz == vm::classChar)
        return vm::wrap<Character>(array->get<jchar>(index));
    else if (clazz == vm::classInt)
        return vm::wrap<Integer>(array->get<jint>(index));
    else if (clazz == vm::classLong)
        return vm::wrap<Long>(array->get<jlong>(index));
    else if (clazz == vm::classFloat)
        return vm::wrap<Float>(array->get<jfloat>(index));
    else if (clazz == vm::classDouble)
        return vm::wrap<Double>(array->get<jdouble>(index));
    return nullptr;
}

jint java::lang::reflect::Array::SM_getLength_R_int(const jobject &array) {
    return vm::checkedCast<vm::Array>(array)->length;
}
