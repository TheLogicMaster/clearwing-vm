#include "java/lang/Class.h"
#include "java/lang/reflect/Field.h"
#include "java/lang/reflect/Method.h"
#include "java/lang/reflect/Constructor.h"
#include "java/lang/ClassNotFoundException.h"
#include "java/lang/annotation/Annotation.h"

#include <cstring>

extern "C" {

void M_java_lang_Class_ensureInitialized(jcontext ctx, jobject selfObj) {
    auto self = (jclass) NULL_CHECK(selfObj);
    if (self->initialized) // Todo: Should be made thread safe
        return;

    jtype frame[2];
    auto frameRef = pushStackFrame(ctx, 2, frame, "java/lang/Thread:threadEntrypoint", nullptr);

    auto nameStr = std::string((const char *) self->nativeName);
    std::replace(nameStr.begin(), nameStr.end(), '/', '.');
    self->name = (intptr_t) stringFromNative(ctx, nameStr.c_str());

    auto interfaces = (jarray) createArray(ctx, &class_java_lang_Class, self->interfaceCount);
    self->interfaces = (intptr_t) interfaces;
    for (int i = 0; i < self->interfaceCount; i++)
        ((jclass *) interfaces->data)[i] = ((jclass *) self->nativeInterfaces)[i];

    auto fields = (jarray) createArray(ctx, &class_java_lang_reflect_Field, self->fieldCount);
    self->fields = (intptr_t) fields;
    for (int i = 0; i < self->fieldCount; i++) {
        auto &data = ((FieldMetadata *) self->nativeFields)[i];
        auto field = gcAlloc(ctx, &class_java_lang_reflect_Field);
        ((jobject *) fields->data)[i] = field;
        auto desc = (jobject) stringFromNative(ctx, (const char *) data.desc);
        frame[0].o = desc;
        auto name = (jobject) stringFromNative(ctx, (const char *) data.name);
        frame[1].o = name;
        init_java_lang_reflect_Field_long_java_lang_Class_java_lang_Class_java_lang_String_java_lang_String_int(ctx, field, data.offset, selfObj, (jobject) data.type, desc, name, data.access);
    }

    std::vector<jobject> methodVector;
    std::vector<jobject> constructorVector;
    for (int i = 0; i < self->methodCount; i++) {
        auto &methodData = ((MethodMetadata *) self->nativeMethods)[i];
        auto method = gcAllocNative(ctx, &class_java_lang_reflect_Method);
        auto desc = (jobject) stringFromNative(ctx, (const char *) methodData.desc);
        frame[0].o = desc;
        auto name = (jobject) stringFromNative(ctx, (const char *) methodData.name);
        frame[1].o = name;
        init_java_lang_reflect_Method_long_java_lang_Class_java_lang_String_java_lang_String_int(ctx, method, methodData.offset, selfObj, desc, name, methodData.access);
        if (!strcmp("<init>", (const char *) methodData.name))
            constructorVector.push_back(constructObject<&class_java_lang_reflect_Constructor, init_java_lang_reflect_Constructor_java_lang_reflect_Method>(ctx, method));
        else
            methodVector.push_back(method);
    }

    auto methods = (jarray) createArray(ctx, &class_java_lang_reflect_Method, (int) methodVector.size());
    self->methods = (intptr_t) methods;
    for (int i = 0; i < methodVector.size(); i++) {
        ((jobject *) methods->data)[i] = methodVector[i];
        methodVector[i]->gcMark = GC_MARK_START;
    }

    auto constructors = (jarray) createArray(ctx, &class_java_lang_reflect_Constructor, (int) constructorVector.size());
    self->constructors = (intptr_t) constructors;
    for (int i = 0; i < constructorVector.size(); i++) {
        ((jobject *) constructors->data)[i] = constructorVector[i];
        constructorVector[i]->gcMark = GC_MARK_START;
    }

    if (self->annotationInitializer)
        ((init_annotations_ptr) self->annotationInitializer)(ctx);
    else {
        self->annotations = (intptr_t) createArray(ctx, &class_java_lang_annotation_Annotation, 0);
        for (auto method : methodVector)
            ((java_lang_reflect_Method *) method)->F_annotations = (intptr_t) createArray(ctx, &class_java_lang_annotation_Annotation, 0);
        for (auto constructor : constructorVector)
            ((java_lang_reflect_Method *) ((java_lang_reflect_Constructor *) constructor)->F_method)->F_annotations = (intptr_t) createArray(ctx, &class_java_lang_annotation_Annotation, 0);
        for (int i = 0; i < self->fieldCount; i++)
            ((java_lang_reflect_Field **) fields->data)[i]->F_annotations = (intptr_t) createArray(ctx, &class_java_lang_annotation_Annotation, 0);
    }

    self->initialized = true;

    popStackFrame(ctx);
}

jobject SM_java_lang_Class_forName_java_lang_String_R_java_lang_Class(jcontext ctx, jobject nameObj) {
    auto name = stringToNative(ctx, (jstring) NULL_CHECK(nameObj));
    { // Scope std::string to prevent leak when throwing ClassNotFoundException
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
