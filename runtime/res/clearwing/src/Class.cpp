#include "java/lang/Class.h"
#include "java/lang/reflect/Field.h"
#include "java/lang/reflect/Method.h"
#include "java/lang/reflect/Constructor.h"
#include "java/lang/ClassNotFoundException.h"

#include <cstring>

extern "C" {

void M_java_lang_Class_ensureInitialized(jcontext ctx, jobject selfObj) {
    auto self = (jclass) NULL_CHECK(selfObj);
    if (self->initialized) // Todo: Should be made thread safe
        return;

    ((static_init_ptr) self->staticInitializer)(ctx);

    auto nameStr = std::string((const char *) self->nativeName);
    std::replace(nameStr.begin(), nameStr.end(), '/', '.');
    self->name = (intptr_t) stringFromNative(ctx, nameStr.c_str());

    auto interfaces = (jarray) createArray(ctx, &class_java_lang_Class, self->interfaceCount);
    for (int i = 0; i < self->interfaceCount; i++)
        ((jclass *) interfaces->data)[i] = ((jclass *) self->nativeInterfaces)[i];
    self->interfaces = (intptr_t) interfaces;

    auto fields = (jarray) createArray(ctx, &class_java_lang_reflect_Field, self->fieldCount);
    fields->parent.gcMark = GC_MARK_NATIVE;
    for (int i = 0; i < self->fieldCount; i++) {
        auto &data = ((FieldMetadata *) self->nativeFields)[i];
        auto field = gcAllocNative(ctx, &class_java_lang_reflect_Field);
        auto desc = (jobject) stringFromNative(ctx, (const char *) data.desc);
        auto name = (jobject) stringFromNative(ctx, (const char *) data.name);
        init_java_lang_reflect_Field_long_java_lang_Class_java_lang_Class_java_lang_String_java_lang_String_int(ctx, field, data.offset, selfObj, (jobject) data.type, desc, name, data.access);
        field->gcMark = GC_MARK_START;
        ((jobject *) fields->data)[i] = field;
    }
    fields->parent.gcMark = GC_MARK_START;
    self->fields = (intptr_t) fields;

    std::vector<jobject> methodVector;
    std::vector<jobject> constructorVector;
    for (int i = 0; i < self->methodCount; i++) {
        auto &methodData = ((MethodMetadata *) self->nativeMethods)[i];
        auto method = gcAllocNative(ctx, &class_java_lang_reflect_Method);
        auto desc = (jobject) stringFromNative(ctx, (const char *) methodData.desc);
        desc->gcMark = GC_MARK_NATIVE;
        auto name = (jobject) stringFromNative(ctx, (const char *) methodData.name);
        name->gcMark = GC_MARK_NATIVE;
        init_java_lang_reflect_Method_long_java_lang_Class_java_lang_String_java_lang_String_int(ctx, method, methodData.offset, selfObj, desc, name, methodData.access);
        method->gcMark = GC_MARK_START;
        desc->gcMark = GC_MARK_START;
        name->gcMark = GC_MARK_START;
        if (!strcmp("<init>", (const char *) methodData.name))
            constructorVector.push_back(constructObject<&class_java_lang_reflect_Constructor, init_java_lang_reflect_Constructor_java_lang_reflect_Method>(ctx, method));
        else
            methodVector.push_back(method);
    }

    auto methods = (jarray) createArray(ctx, &class_java_lang_reflect_Method, (int) methodVector.size());
    for (int i = 0; i < methodVector.size(); i++)
        ((jobject *) methods->data)[i] = methodVector[i];
    self->methods = (intptr_t) methods;

    auto constructors = (jarray) createArray(ctx, &class_java_lang_reflect_Constructor, (int) constructorVector.size());
    for (int i = 0; i < constructorVector.size(); i++)
        ((jobject *) constructors->data)[i] = constructorVector[i];
    self->constructors = (intptr_t) constructors;

    // Todo
    //    auto annotations = (jarray) createArray(ctx, &class_java_lang_Annotation, 0);

    self->initialized = true;
}

jobject SM_java_lang_Class_forName_java_lang_String_R_java_lang_Class(jcontext ctx, jobject nameObj) {
    auto name = stringToNative(ctx, (jstring) NULL_CHECK(nameObj));
    { // Scope std::string to prevent leak on exception
        auto nameStr = std::string(name);
        std::replace(nameStr.begin(), nameStr.end(), '.', '/');
        auto clazz = (jobject) classForName(nameStr.c_str());
        if (clazz)
            return clazz;
    }
    constructAndThrowMsg<&class_java_lang_ClassNotFoundException, init_java_lang_ClassNotFoundException_java_lang_String>(ctx, name);
    return nullptr;
}

jbool M_java_lang_Class_isAssignableFrom_java_lang_Class_R_boolean(jcontext ctx, jobject self, jobject assignee) {
    return isAssignableFrom(ctx, (jclass) NULL_CHECK(self), (jclass) NULL_CHECK(assignee));
}

jbool M_java_lang_Class_isInstance_java_lang_Object_R_boolean(jcontext ctx, jobject self, jobject object) {
    return isAssignableFrom(ctx, (jclass) NULL_CHECK(self), (jclass) NULL_CHECK(object)->clazz);
}

}
