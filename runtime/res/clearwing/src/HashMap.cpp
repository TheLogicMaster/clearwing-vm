#include "java/util/HashMap.h"
#include "java/util/HashMap_Entry.h"

extern "C" {

jobject M_java_util_HashMap_findNonNullKeyEntry_java_lang_Object_int_int_R_java_util_HashMap$Entry(jcontext ctx, jobject self, jobject key, jint index, jint keyHash) {
    auto map = (java_util_HashMap *) NULL_CHECK(self);
    auto elements = ((jarray) map->F_elementData);
    auto m = (java_util_HashMap$Entry *) ((jobject *) elements->data)[index];
    while (m) {
        if (m->F_origKeyHash == keyHash and SM_java_util_HashMap_areEqualKeys_java_lang_Object_java_lang_Object_R_boolean(ctx, key, (jobject) m->parent.F_key))
            break;
        m = (java_util_HashMap$Entry *) m->F_next;
    }
    return (jobject) m;
}

jbool SM_java_util_HashMap_areEqualKeys_java_lang_Object_java_lang_Object_R_boolean(jcontext ctx, jobject key1, jobject key2) {
    if (key1 == key2)
        return true;
    return invokeVirtual<func_java_lang_Object_equals_java_lang_Object_R_boolean, VTABLE_java_lang_Object_equals_java_lang_Object_R_boolean>(ctx, key1, key2);
}

}
