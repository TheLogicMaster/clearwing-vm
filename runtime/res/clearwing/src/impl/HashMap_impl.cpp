#include <Clearwing.hpp>
#include <java/util/HashMap.hpp>
#include <java/util/HashMap_Entry.hpp>
#include "Utils.hpp"

shared_ptr<java::util::HashMap$Entry> java::util::HashMap::M_findNonNullKeyEntry_R_java_util_HashMap$Entry(const jobject &key, jint index, jint keyHash) {
    auto elements = F_elementData;
    auto m = object_cast<java::util::HashMap$Entry>(elements->get<jobject>(index));
    while (m and (m->F_origKeyHash != keyHash or !SM_areEqualKeys_R_boolean(key, m->F_key)))
        m = m->F_next;
    return m;
}

jbool java::util::HashMap::SM_areEqualKeys_R_boolean(const jobject &key0, const jobject &key1) {
    if(key0 == key1)
        return true;
    return key0->M_equals_R_boolean(key1);
}
