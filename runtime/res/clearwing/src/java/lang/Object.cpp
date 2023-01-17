#include <java/lang/Object.hpp>
#include <java/lang/Thread.hpp>
#include <java/lang/InterruptedException.hpp>
#include <java/lang/IllegalMonitorStateException.hpp>
#include <java/lang/CloneNotSupportedException.hpp>
#include <memory>
#include <mutex>
#include <utility>
#include <algorithm>
#include "Utils.hpp"
#include "RuntimeTypes.hpp"

const std::string Object::NAME = "java/lang/Object";
static const ClassData classData{
        Object::NAME, 1, "", {}, {},
        {
                {"<init>", 1, {}, "V", Object::reflect_init},
                {"hashCode", 1, {}, "I", Object::reflect_M_hashCode_R_int},
                {"equals", 1, {"java/lang/Object"}, "Z", Object::reflect_M_equals_java_lang_Object_R_boolean},
                {"clone", 1, {}, "java/lang/Object", Object::reflect_M_clone_R_java_lang_Object},
                {"getClass", 1, {}, "java/lang/Class", Object::reflect_M_getClass_R_java_lang_Class},
                {"toString", 1, {}, "java/lang/String", Object::reflect_M_toString_R_java_lang_String},
                {"finalize", 1, {}, "V", Object::reflect_M_finalize},
                {"notify", 1, {}, "V", Object::reflect_M_notify},
                {"notifyAll", 1, {}, "V", Object::reflect_M_notifyAll},
                {"wait", 1, {}, "V", Object::reflect_M_wait},
                {"wait", 1, {"L"}, "V", Object::reflect_M_wait_long},
                {"wait", 1, {"L", "I"}, "V", Object::reflect_M_wait_long_int},
        },
        Object::newObject, nullptr, false, false, false, sizeof(jobject), 0, ""
};
const jclass Object::CLASS = vm::registerClass(&classData);
std::mutex Object::currentMonitorLock;

Object::Object(std::string name) : name(std::move(name)) {
}

Object::Object(const Object &other) : enable_shared_from_this(other), name(other.name) {
}

Object::~Object() {
    // Todo: Potentially just return after System.exit() has been called to prevent using the global callstack after it's disposed
    if (finalized)
        return;
    finalized = true;
    try {
        M_finalize();
    } catch (jobject &ignored){}
}

/**
 * This either returns a normal shared pointer to this object, or it
 * returns a cursed, dummy shared pointer if the object is partially disposed
 */
jobject Object::get_this() {
    if (finalized)
        return {shared_ptr<Object>(), this};
    return shared_from_this();
}

jobject Object::clone() {
    if (!vm::instanceof<java::lang::Cloneable>(get_this()))
        vm::throwNew<java::lang::CloneNotSupportedException>();
    return make_shared<Object>(*this);
}

void Object::clinit() {
}

void Object::acquireMonitor() {
    auto thread = java::lang::Thread::SM_currentThread_R_java_lang_Thread();
    {
        std::scoped_lock<std::mutex> lock(monitorLock);
        if (monitorOwner == thread) {
            monitorUsageCount++;
            return;
        }
    }

    monitor.lock();
    {
        std::scoped_lock<std::mutex> lock(monitorLock);
        monitorUsageCount = 1;
        monitorOwner = thread;
    }
}

void Object::releaseMonitor() {
    // Todo: Throw IllegalMonitorStateException if thread is not the owner
    std::unique_lock<std::mutex> lock(monitorLock);
    if (--monitorUsageCount == 0) {
        monitorOwner = nullptr;
        monitor.unlock();
    }
}

void Object::init() {

}

jint Object::M_hashCode_R_int() {
    return (jint) (intptr_t) this;
}

jbool Object::M_equals_R_boolean(const jobject &other) {
    return other.get() == this;
}

jobject Object::M_clone_R_java_lang_Object() {
    // Todo: Ensure Cloneable is implemented by this object
    return clone();
}

jstring Object::M_toString_R_java_lang_String() {
    std::string string(name);
    std::replace(string.begin(), string.end(), '/', '.');
    string += " " + std::to_string((jint) (intptr_t) this);
    return vm::createString(string.c_str());
}

void Object::M_finalize() {
}

void Object::interruptedCheck() {
    std::scoped_lock<std::mutex> currentLock(currentMonitorLock);
    auto thread = Thread::SM_currentThread_R_java_lang_Thread();
    if (thread->F_interrupted) {
        thread->F_interrupted = false;
        vm::throwNew<java::lang::InterruptedException>();
    }
}

static void monitorOwnerCheck(const Object *self) {
    if (!self->monitorOwner or self->monitorOwner != object_cast<Object>(java::lang::Thread::SM_currentThread_R_java_lang_Thread()))
        vm::throwNew<java::lang::IllegalMonitorStateException>();
}

void Object::M_notify() {
    monitorOwnerCheck(this);
    condition.notify_one();
}

void Object::M_notifyAll() {
    monitorOwnerCheck(this);
    condition.notify_all();
}

void Object::M_wait() {
    monitorOwnerCheck(this);
    auto thread = Thread::SM_currentThread_R_java_lang_Thread();
    interruptedCheck();
    {
        std::scoped_lock<std::mutex> currentLock(currentMonitorLock);
        thread->F_currentMonitor = get_this();
    }
    releaseMonitor();
    {
        std::unique_lock<std::mutex> lock(conditionMutex);
        condition.wait(lock);
    }
    acquireMonitor();
    {
        std::scoped_lock<std::mutex> currentLock(currentMonitorLock);
        thread->F_currentMonitor = nullptr;
    }
    interruptedCheck();
}

void Object::M_wait(jlong millis) {
    M_wait(millis, 0);
}

void Object::M_wait(jlong millis, int nanos) {
    monitorOwnerCheck(this);
    auto thread = Thread::SM_currentThread_R_java_lang_Thread();
    interruptedCheck();
    {
        std::scoped_lock<std::mutex> currentLock(currentMonitorLock);
        thread->F_currentMonitor = get_this();
    }
    releaseMonitor();
    {
        std::unique_lock<std::mutex> lock(conditionMutex);
        condition.wait_for(lock, std::chrono::milliseconds(millis) + std::chrono::nanoseconds(nanos));
    }
    acquireMonitor();
    {
        std::scoped_lock<std::mutex> currentLock(currentMonitorLock);
        thread->F_currentMonitor = nullptr;
    }
    interruptedCheck();
}

jclass Object::M_getClass_R_java_lang_Class() {
    if (clazz)
        return clazz;
    clazz = vm::getClass(name.c_str());
    return clazz;
}

jobject Object::reflect_init(const jobject &object, const shared_ptr<vm::Array> &args) {
    object->init();
    return nullptr;
}

jobject Object::reflect_M_hashCode_R_int(const jobject &object, const shared_ptr<vm::Array> &args) {
    return vm::wrap<java::lang::Integer>(object->M_hashCode_R_int());
}

jobject Object::reflect_M_equals_java_lang_Object_R_boolean(const jobject &object, const shared_ptr<vm::Array> &args) {
    return vm::wrap<Boolean>(object->M_equals_R_boolean(args->get<jobject>(0)));
}

jobject Object::reflect_M_clone_R_java_lang_Object(const jobject &object, const shared_ptr<vm::Array> &args) {
    return object->M_clone_R_java_lang_Object();
}

jobject Object::reflect_M_getClass_R_java_lang_Class(const jobject &object, const shared_ptr<vm::Array> &args) {
    return object->M_getClass_R_java_lang_Class();
}

jobject Object::reflect_M_toString_R_java_lang_String(const jobject &object, const shared_ptr<vm::Array> &args) {
    return object->M_toString_R_java_lang_String();
}

jobject Object::reflect_M_finalize(const jobject &object, const shared_ptr<vm::Array> &args) {
    object->M_finalize();
    return nullptr;
}

jobject Object::reflect_M_notify(const jobject &object, const shared_ptr<vm::Array> &args) {
    object->M_notify();
    return nullptr;
}

jobject Object::reflect_M_notifyAll(const jobject &object, const shared_ptr<vm::Array> &args) {
    object->M_notifyAll();
    return nullptr;
}

jobject Object::reflect_M_wait(const jobject &object, const shared_ptr<vm::Array> &args) {
    object->M_wait();
    return nullptr;
}

jobject Object::reflect_M_wait_long(const jobject &object, const shared_ptr<vm::Array> &args) {
    object->M_wait(vm::unwrap<jlong>(args->get<jobject>(0)));
    return nullptr;
}

jobject Object::reflect_M_wait_long_int(const jobject &object, const shared_ptr<vm::Array> &args) {
    object->M_wait(vm::unwrap<jlong>(args->get<jobject>(0)), vm::unwrap<jint>(args->get<jobject>(1)));
    return nullptr;
}
