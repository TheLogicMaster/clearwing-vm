#include <Clearwing.hpp>
#include <java/lang/ref/WeakReference.hpp>
#include "Utils.hpp"

jlong java::lang::ref::WeakReference::M_create_R_long(const jobject &ref) {
    return (jlong) new weak_ptr<Object>(ref);
}

jobject java::lang::ref::WeakReference::M_get_R_java_lang_Object() {
    return ((weak_ptr<Object> *) F_reference)->lock();
}

void java::lang::ref::WeakReference::M_clear() {
    ((weak_ptr<Object> *) F_reference)->reset();
}

jbool java::lang::ref::WeakReference::M_refersTo_R_boolean(const jobject &ref) {
    return ((weak_ptr<Object> *) F_reference)->lock() == ref;
}

void java::lang::ref::WeakReference::M_finalize() {
    delete (weak_ptr<Object> *) F_reference;
    F_reference = 0;
}
