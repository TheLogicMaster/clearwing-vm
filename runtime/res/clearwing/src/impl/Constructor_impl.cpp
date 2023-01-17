#include "Clearwing.hpp"
#include <java/lang/reflect/Constructor.hpp>
#include <java/lang/InstantiationException.hpp>
#include "Utils.hpp"

jobject java::lang::reflect::Constructor::SM_nativeCreate_R_java_lang_Object(const jclass &clazzObj) {
    auto classData = ((ClassData *)clazzObj->F_nativeData);
    if (!classData->newObjectFunc)
        vm::throwNew<InstantiationException>();
    return classData->newObjectFunc();
}
