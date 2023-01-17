#include "Clearwing.hpp"
#include <java/lang/Class.hpp>
#include <java/lang/annotation/Annotation.hpp>
#include <java/lang/reflect/Method.hpp>
#include <java/lang/reflect/Constructor.hpp>
#include <java/lang/reflect/Field.hpp>
#include <algorithm>
#include "Utils.hpp"

using namespace java::lang::reflect;
using java::lang::Class;

jstring Class::M_getName0_R_java_lang_String() {
    std::string string(((ClassData *)F_nativeData)->name);
    std::replace(string.begin(), string.end(), '/', '.');
    return vm::createString(string.c_str());
}

jclass Class::SM_forName_R_java_lang_Class(const jstring &name) {
    std::string publicName = vm::getNativeString(name);
    std::replace(publicName.begin(), publicName.end(), '.', '/');
    return vm::getClass(publicName.c_str());
}

void Class::M_ensureInitialized() {
    if (F_name)
        return;

    auto classData = (ClassData *)F_nativeData;

    F_name = vm::createString(classData->name.c_str());
    F_access = classData->access;
    if (!classData->parent.empty())
        F_parent = vm::getClass(classData->parent.c_str());

    auto interfaces = make_shared<vm::Array>("java/lang/Class", sizeof(jobject), false, classData->interfaces.size());
    F_interfaces = interfaces;
    for (int i = 0; i < classData->interfaces.size(); i++)
        interfaces->get<jobject>(i) = vm::getClass(classData->interfaces[i].c_str());

    auto fieldsArray = make_shared<vm::Array>("java/lang/reflect/Field", sizeof(jobject), false, classData->fields.size());
    for (int i = 0; i < classData->fields.size(); i++) {
        auto &fieldData = classData->fields[i];
        auto field = make_shared<Field>();
        field->init(
            (jlong)fieldData.getterFunc,
            (jlong)fieldData.setterFunc,
            object_cast<Class>(get_this()),
            vm::getClass(fieldData.type.c_str()),
            fieldData.signature.empty() ? nullptr : vm::createString(fieldData.signature.c_str()),
            vm::createString(fieldData.name.c_str()),
            fieldData.access
        );
        fieldsArray->get<jobject>(i) = field;
    }
    F_fields = fieldsArray;

    vector<jobject> methods;
    vector<jobject> constructors;
    for (auto &methodData: classData->methods) {
        auto method = make_shared<Method>();
        auto parameters = make_shared<vm::Array>("java/lang/Class", sizeof(jobject), false, methodData.paramTypes.size());
        for (int i = 0; i < methodData.paramTypes.size(); i++)
            parameters->get<jobject>(i) = vm::getClass(methodData.paramTypes[i].c_str());
        method->init_Array1_java_lang_Class(
            (jlong)methodData.funcPtr,
            object_cast<Class>(get_this()),
            parameters,
            methodData.returnType.empty() ? nullptr : vm::getClass(methodData.returnType.c_str()),
            vm::createString(methodData.name.c_str()),
            methodData.access
        );
        if (methodData.name == "<init>") {
            auto constructor = make_shared<Constructor>();
            constructor->init(method);
            constructors.emplace_back(constructor);
        } else
            methods.emplace_back(method);
    }

    auto methodsArray = make_shared<vm::Array>("java/lang/reflect/Method", sizeof(jobject), false, methods.size());
    for (int i = 0; i < methods.size(); i++)
        methodsArray->get<jobject>(i) = methods[i];
    F_methods = methodsArray;

    auto constructorsArray = make_shared<vm::Array>("java/lang/reflect/Constructor", sizeof(jobject), false, constructors.size());
    for (int i = 0; i < constructors.size(); i++)
        constructorsArray->get<jobject>(i) = constructors[i];
    F_constructors = constructorsArray;

    F_annotations = make_shared<vm::Array>("java/lang/annotation/Annotation", sizeof(jobject), false, 0);

    F_anonymous = classData->anonymous;
    F_synthetic = classData->synthetic;
    F_primitive = classData->primitive;
    F_size = classData->size;
    F_arrayDimensions = classData->arrayDimensions;

    if (!classData->arrayItemType.empty())
        F_arrayItemType = vm::getClass(classData->arrayItemType.c_str());

    if (classData->staticInitializer)
        classData->staticInitializer();
}

jbool Class::M_isAssignableFrom_R_boolean(const jclass &cls) {
    M_ensureInitialized();
    if (!cls)
        vm::throwNew<java::lang::NullPointerException>();
    auto clazz = vm::checkedCast<Class>(cls);
    clazz->M_ensureInitialized();
    if (cls.get() == this)
        return true;
    if (clazz->F_parent and M_isAssignableFrom_R_boolean(clazz->F_parent))
        return true;
    if (clazz->F_arrayDimensions > 0 and F_arrayDimensions > 0 and F_arrayItemType->M_isAssignableFrom_R_boolean(clazz->F_arrayItemType))
        return true;
    auto interfaces = clazz->F_interfaces;
    for (int i = 0; i < interfaces->length; i++)
        if (M_isAssignableFrom_R_boolean(vm::checkedCast<Class>(interfaces->get<jobject>(i))))
            return true;
    return false;
}

jbool Class::M_isInstance_R_boolean(const jobject &obj) {
    if (!obj)
        return false;
    return M_isAssignableFrom_R_boolean(obj->M_getClass_R_java_lang_Class());
}
