#include "Clearwing.h"
#include "java/lang/reflect/Field.h"
#include "java/lang/reflect/Method.h"
#include "java/lang/reflect/Constructor.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/reflect/InvocationTargetException.h"

#include <ffi.h>

extern "C" {

jobject SM_java_lang_reflect_Array_newInstanceImpl_java_lang_Class_int_R_java_lang_Object(jcontext ctx, jobject type, jint length) {
    return (jobject) createArray(ctx, (jclass) NULL_CHECK(type), length);
}

void SM_java_lang_reflect_Array_set_java_lang_Object_int_java_lang_Object(jcontext ctx, jobject arrayObj, jint index, jobject value) {
    auto array = (jarray) NULL_CHECK(arrayObj);
    if (index >= array->length or index < 0)
        throwIndexOutOfBounds(ctx);
    auto clazz = (jclass)((jclass) arrayObj->clazz)->componentClass;
    if (!clazz->primitive)
        ((jobject *) array->data)[index] = value;
    else if (clazz == &class_byte)
        ((jbyte *) array->data)[index] = unboxByte(ctx, value);
    else if (clazz == &class_short)
        ((jshort *) array->data)[index] = unboxShort(ctx, value);
    else if (clazz == &class_char)
        ((jchar *) array->data)[index] = unboxCharacter(ctx, value);
    else if (clazz == &class_int)
        ((jint *) array->data)[index] = unboxInteger(ctx, value);
    else if (clazz == &class_long)
        ((jlong *) array->data)[index] = unboxLong(ctx, value);
    else if (clazz == &class_float)
        ((jfloat *) array->data)[index] = unboxFloat(ctx, value);
    else if (clazz == &class_double)
        ((jdouble *) array->data)[index] = unboxDouble(ctx, value);
    else if (clazz == &class_boolean)
        ((jbool *) array->data)[index] = unboxBoolean(ctx, value);
}

jobject SM_java_lang_reflect_Array_get_java_lang_Object_int_R_java_lang_Object(jcontext ctx, jobject arrayObj, jint index) {
    auto array = (jarray) NULL_CHECK(arrayObj);
    if (index >= array->length or index < 0)
        throwIndexOutOfBounds(ctx);
    auto clazz = (jclass)((jclass) arrayObj->clazz)->componentClass;
    if (!clazz->primitive)
        return ((jobject *) array->data)[index];
    else if (clazz == &class_byte)
        return boxByte(ctx, ((jbyte *) array->data)[index]);
    else if (clazz == &class_short)
        return boxShort(ctx, ((jshort *) array->data)[index]);
    else if (clazz == &class_char)
        return boxCharacter(ctx, ((jchar *) array->data)[index]);
    else if (clazz == &class_int)
        return boxInteger(ctx, ((jint *) array->data)[index]);
    else if (clazz == &class_long)
        return boxLong(ctx, ((jlong *) array->data)[index]);
    else if (clazz == &class_float)
        return boxFloat(ctx, ((jfloat *) array->data)[index]);
    else if (clazz == &class_double)
        return boxDouble(ctx, ((jdouble *) array->data)[index]);
    else if (clazz == &class_boolean)
        return boxBoolean(ctx, ((jbool *) array->data)[index]);
    return nullptr;
}

jint SM_java_lang_reflect_Array_getLength_java_lang_Object_R_int(jcontext ctx, jobject arrayObj) {
    return ((jarray) NULL_CHECK(arrayObj))->length;
}

jobject SM_java_lang_reflect_Constructor_nativeCreate_java_lang_Class_R_java_lang_Object(jcontext ctx, jobject classObj) {
    return gcAlloc(ctx, (jclass) NULL_CHECK(classObj));
}

jobject M_java_lang_reflect_Field_get_java_lang_Object_R_java_lang_Object(jcontext ctx, jobject self, jobject object) {
    auto field = (java_lang_reflect_Field *) NULL_CHECK(self);
    bool isStatic = field->F_modifiers & 0x8;
    if (isStatic)
        ((static_init_ptr)((jclass)field->F_declaringClass)->staticInitializer)(ctx);
    else
        NULL_CHECK(object);
    auto ptr = isStatic ? (char *) field->F_offset : ((char *) object) + field->F_offset;
    auto fieldClass = (jclass) field->F_type;
    if (!fieldClass->primitive) return *(jobject *) ptr;
    else if (fieldClass == &class_byte) return boxByte(ctx, *(jbyte *) ptr);
    else if (fieldClass == &class_short) return boxShort(ctx, *(jshort *) ptr);
    else if (fieldClass == &class_char) return boxCharacter(ctx, *(jchar *) ptr);
    else if (fieldClass == &class_int) return boxInteger(ctx, *(jint *) ptr);
    else if (fieldClass == &class_long) return boxLong(ctx, *(jlong *) ptr);
    else if (fieldClass == &class_float) return boxFloat(ctx, *(jfloat *) ptr);
    else if (fieldClass == &class_double) return boxDouble(ctx, *(jdouble *) ptr);
    else if (fieldClass == &class_boolean) return boxBoolean(ctx, *(jbool *) ptr);
    else return nullptr;
}

void M_java_lang_reflect_Field_set_java_lang_Object_java_lang_Object(jcontext ctx, jobject self, jobject object, jobject value) {
    auto field = (java_lang_reflect_Field *) NULL_CHECK(self);
    bool isStatic = field->F_modifiers & 0x8;
    if (isStatic)
        ((static_init_ptr)((jclass)field->F_declaringClass)->staticInitializer)(ctx);
    else
        NULL_CHECK(object);
    auto ptr = isStatic ? (char *) field->F_offset : ((char *) object) + field->F_offset;
    auto fieldClass = (jclass) field->F_type;
    if (!fieldClass->primitive) *(jobject *) ptr = value;
    else if (fieldClass == &class_byte) *(jbyte *) ptr = unboxByte(ctx, value);
    else if (fieldClass == &class_short) *(jshort *) ptr = unboxShort(ctx, value);
    else if (fieldClass == &class_char) *(jchar *) ptr = unboxCharacter(ctx, value);
    else if (fieldClass == &class_int) *(jint *) ptr = unboxInteger(ctx, value);
    else if (fieldClass == &class_long) *(jlong *) ptr = unboxLong(ctx, value);
    else if (fieldClass == &class_float) *(jfloat *) ptr = unboxFloat(ctx, value);
    else if (fieldClass == &class_double) *(jdouble *) ptr = unboxDouble(ctx, value);
    else if (fieldClass == &class_boolean) *(jbool *) ptr = unboxBoolean(ctx, value);
}

static ffi_type *typeToFFI(jclass type) {
    if (type == &class_byte) return &ffi_type_sint8;
    else if (type == &class_short) return &ffi_type_sint16;
    else if (type == &class_char) return &ffi_type_uint16;
    else if (type == &class_int) return &ffi_type_sint32;
    else if (type == &class_long) return &ffi_type_sint64;
    else if (type == &class_float) return &ffi_type_float;
    else if (type == &class_double) return &ffi_type_double;
    else if (type == &class_boolean) return &ffi_type_uint8;
    else return &ffi_type_pointer;
}

jobject M_java_lang_reflect_Method_invoke_java_lang_Object_Array1_java_lang_Object_R_java_lang_Object(jcontext ctx, jobject self, jobject object, jobject argsObj) {
    auto argsArray = (jarray) NULL_CHECK(argsObj);
    auto argsObjects = (jobject *) argsArray->data;
    auto method = (java_lang_reflect_Method *) NULL_CHECK(self);
    auto owner = (jclass) method->F_declaringClass;
    bool isStatic = (method->F_modifiers & 0x8) == 0x8;
    bool isInterface = (owner->access & 0x200) == 0x200;
    bool isConstructor = stringToNative(ctx, (jstring) method->F_name) == std::string("<init>");
    auto paramTypesArray = (jarray) method->F_parameterTypes;
    auto paramTypesObjects = (jclass *) paramTypesArray->data;
    auto returnType = (jclass) method->F_returnType;

    if (isStatic)
        ((static_init_ptr)((jclass)method->F_declaringClass)->staticInitializer)(ctx);
    else
        NULL_CHECK(object);

    auto argTypes = new ffi_type*[2 + paramTypesArray->length];
    auto argValues = new jtype[paramTypesArray->length]{};
    auto args = new void*[2 + paramTypesArray->length]; // List of pointers to actual values

    argTypes[0] = &ffi_type_pointer;
    args[0] = &ctx;
    if (!isStatic) {
        argTypes[1] = &ffi_type_pointer;
        args[1] = &object;
    }

    int argOffset = isStatic ? 1 : 2;
    if (argsArray->length != paramTypesArray->length)
        constructAndThrow<&class_java_lang_IllegalArgumentException, init_java_lang_IllegalArgumentException>(ctx);
    for (int i = 0; i < paramTypesArray->length; i++) {
        auto paramType = paramTypesObjects[i];
        if (!paramType->primitive && argsObjects[i] && !isInstance(ctx, argsObjects[i], paramType))
            constructAndThrow<&class_java_lang_IllegalArgumentException, init_java_lang_IllegalArgumentException>(ctx);
        argTypes[i + argOffset] = typeToFFI(paramType);
        args[i + argOffset] = &argValues[i];

        auto arg = argsObjects[i];
        if (paramType == &class_byte) argValues[i].i = (jchar) unboxByte(ctx, arg);
        else if (paramType == &class_short) argValues[i].i = unboxShort(ctx, arg);
        else if (paramType == &class_char) argValues[i].i = unboxCharacter(ctx, arg);
        else if (paramType == &class_int) argValues[i].i = unboxInteger(ctx, arg);
        else if (paramType == &class_long) argValues[i].l = unboxLong(ctx, arg);
        else if (paramType == &class_float) argValues[i].f = unboxFloat(ctx, arg);
        else if (paramType == &class_double) argValues[i].d = unboxDouble(ctx, arg);
        else if (paramType == &class_boolean) argValues[i].i = unboxBoolean(ctx, arg);
        else argValues[i].o = arg;
    }

    jtype returnValue;
    ffi_status result;

    auto frameRef = pushStackFrame(ctx, 0, nullptr, "java/lang/Method:invoke", nullptr);
    tryCatch(frameRef, [&]{
        void *func;
        if (isInterface)
            func = resolveInterfaceMethod(ctx, owner, (int) method->F_offset, object);
        else if (isStatic or isConstructor)
            func = (void *) method->F_offset;
        else
            func = ((void **) object->vtable)[method->F_offset];
        ffi_cif cif;
        result = ffi_prep_cif(&cif, FFI_DEFAULT_ABI, paramTypesArray->length + argOffset, typeToFFI(returnType), argTypes);
        if (result == FFI_OK)
            ffi_call(&cif, (void (*)()) func, &returnValue, args);
    }, nullptr, [&](jobject ex){
        delete[] argTypes;
        delete[] argValues;
        delete[] args;
        constructAndThrow<&class_java_lang_reflect_InvocationTargetException, init_java_lang_reflect_InvocationTargetException_java_lang_Throwable>(ctx, ex);
    });
    popStackFrame(ctx);

    delete[] argTypes;
    delete[] argValues;
    delete[] args;

    if (result != FFI_OK)
        constructAndThrowMsg<&class_java_lang_reflect_InvocationTargetException, init_java_lang_reflect_InvocationTargetException_java_lang_Throwable_java_lang_String>
                (ctx, nullptr, "Native invocation failed");

    if (returnType == &class_byte) return boxByte(ctx, (jbyte) returnValue.i);
    else if (returnType == &class_short) return boxShort(ctx, (jshort) returnValue.i);
    else if (returnType == &class_char) return boxCharacter(ctx, returnValue.i);
    else if (returnType == &class_int) return boxInteger(ctx, returnValue.i);
    else if (returnType == &class_long) return boxLong(ctx, returnValue.l);
    else if (returnType == &class_float) return boxFloat(ctx, returnValue.f);
    else if (returnType == &class_double) return boxDouble(ctx, returnValue.d);
    else if (returnType == &class_boolean) return boxBoolean(ctx, returnValue.i);
    else if (returnType == &class_void) return nullptr;
    else return returnValue.o;
}

}
