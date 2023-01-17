#pragma once

#include "Clearwing.hpp"

namespace java::lang {

    class Boolean;
    class Integer;

    class Object : public virtual std::enable_shared_from_this<Object> {
    public:
        explicit Object(std::string name);
        Object(const Object &other);
        inline Object() : name(NAME) {};
        ~Object();
        inline static jobject newObject() { return make_shared<Object>(NAME); }
        static const std::string NAME;
        static const jclass CLASS;
        static void clinit();
        void acquireMonitor();
        void releaseMonitor();
        static void interruptedCheck();
        virtual jobject get_this();
        virtual jobject clone();
        virtual void init();
        virtual jint M_hashCode_R_int();
        virtual jbool M_equals_R_boolean(const jobject &other);
        virtual jobject M_clone_R_java_lang_Object();
        virtual jclass M_getClass_R_java_lang_Class();
        virtual jstring M_toString_R_java_lang_String();
        virtual void M_finalize();
        virtual void M_notify();
        virtual void M_notifyAll();
        virtual void M_wait();
        virtual void M_wait(jlong millis);
        virtual void M_wait(jlong millis, int nanos);
        const std::string name;
        jclass clazz;
        bool finalized{};
        static std::mutex currentMonitorLock; // Todo: This could be made a Thread instance field
        std::mutex monitor;
        std::mutex monitorLock;
        int monitorUsageCount{};
        jobject monitorOwner;
        std::condition_variable condition;
        std::mutex conditionMutex;
        static jobject reflect_init(const jobject &object, const shared_ptr<vm::Array> &args);
        static jobject reflect_M_hashCode_R_int(const jobject &object, const shared_ptr<vm::Array> &args);
        static jobject reflect_M_equals_java_lang_Object_R_boolean(const jobject &object, const shared_ptr<vm::Array> &args);
        static jobject reflect_M_clone_R_java_lang_Object(const jobject &object, const shared_ptr<vm::Array> &args);
        static jobject reflect_M_getClass_R_java_lang_Class(const jobject &object, const shared_ptr<vm::Array> &args);
        static jobject reflect_M_toString_R_java_lang_String(const jobject &object, const shared_ptr<vm::Array> &args);
        static jobject reflect_M_finalize(const jobject &object, const shared_ptr<vm::Array> &args);
        static jobject reflect_M_notify(const jobject &object, const shared_ptr<vm::Array> &args);
        static jobject reflect_M_notifyAll(const jobject &object, const shared_ptr<vm::Array> &args);
        static jobject reflect_M_wait(const jobject &object, const shared_ptr<vm::Array> &args);
        static jobject reflect_M_wait_long(const jobject &object, const shared_ptr<vm::Array> &args);
        static jobject reflect_M_wait_long_int(const jobject &object, const shared_ptr<vm::Array> &args);
    };
}
