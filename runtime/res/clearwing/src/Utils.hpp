#pragma once

#include "Clearwing.hpp"
#include "Array.hpp"
#include <java/lang/ClassCastException.hpp>
#include <java/lang/ArithmeticException.hpp>
#include <java/lang/ExceptionInInitializerError.hpp>
#include <java/io/IOException.hpp>
#include <java/lang/NullPointerException.hpp>
#include <java/lang/Object.hpp>
#include <java/lang/String.hpp>
#include <java/lang/Class.hpp>
#include <java/lang/Boolean.hpp>
#include <java/lang/Byte.hpp>
#include <java/lang/Character.hpp>
#include <java/lang/Short.hpp>
#include <java/lang/Integer.hpp>
#include <java/lang/Long.hpp>
#include <java/lang/Float.hpp>
#include <java/lang/Double.hpp>
#include <java/nio/Buffer.hpp>
#include <utility>

namespace vm {

    template <class T>
    inline void throwEx(const shared_ptr<T> &object) {
        throw object_cast<java::lang::Object>(object);
    }

    template <typename T, class ...P>
    inline void throwNew(P... params) {
        auto t = make_shared<T>();
        t->init(params...);
        vm::throwEx(t);
    }

    template <typename T>
    inline void throwExceptionCause(const char *cause) {
        auto ex = make_shared<T>();
        ex->init(vm::createString(cause));
        vm::throwEx(ex);
    }

    inline void throwIOException(const char *cause) {
        throwExceptionCause<java::io::IOException>(cause);
    }

    inline void throwInitializerException(const jobject &ex) {
        throwNew<java::lang::ExceptionInInitializerError>(object_cast<java::lang::Throwable>(ex));
    }

    static inline void throwDivisionByZero() {
        throwExceptionCause<java::lang::ArithmeticException>("Division by zero");
    }

    template <typename T>
    inline T divideByZeroCheck(T t) {
#if USE_VALUE_CHECKS
        if (t == T(0))
            throwDivisionByZero();
#endif
        return t;
    }

    inline jobject nullCheck(const jobject &object) {
#if USE_VALUE_CHECKS
        if (!object)
            vm::throwNew<java::lang::NullPointerException>();
#endif
        return object;
    }

    inline void nullCheck(void *object) {
#if USE_VALUE_CHECKS
        if (!object)
            vm::throwNew<java::lang::NullPointerException>();
#endif
    }

    template <typename T>
    inline shared_ptr<T> checkedCast(const jobject &object) {
        auto t = object_cast<T>(object);
#if USE_VALUE_CHECKS
        if (!object)
            vm::throwNew<java::lang::NullPointerException>();
        if (!t)
            vm::throwNew<java::lang::ClassCastException>();
#endif
        return t;
    }

    template<typename T>
    inline jint floatCompare(T t1, T t2, jint nanVal) {
        if (std::isnan(t1) or std::isnan(t2))
            return nanVal;
        if (t1 > t2)
            return 1;
        if (t1 < t2)
            return -1;
        return 0;
    }

    inline jint longCompare(jlong l1, jlong l2) {
        if (l1 > l2)
            return 1;
        else if (l1 < l2)
            return -1;
        else
            return 0;
    }

    template<typename T, typename V>
    shared_ptr<T> wrap(V value);

    template<typename T>
    T unwrap(const jobject &wrapper);

    template<>
    inline shared_ptr<java::lang::Boolean> wrap<>(jbool value) {
        return java::lang::Boolean::SM_valueOf_R_java_lang_Boolean(value);
    }

    template<>
    inline shared_ptr<java::lang::Byte> wrap<java::lang::Byte>(jbyte value) {
        return java::lang::Byte::SM_valueOf_R_java_lang_Byte(value);
    }

    template<>
    inline shared_ptr<java::lang::Character> wrap<>(jchar value) {
        return java::lang::Character::SM_valueOf_R_java_lang_Character(value);
    }

    template<>
    inline shared_ptr<java::lang::Short> wrap<>(jshort value) {
        return java::lang::Short::SM_valueOf_R_java_lang_Short(value);
    }

    template<>
    inline shared_ptr<java::lang::Integer> wrap<>(jint value) {
        return java::lang::Integer::SM_valueOf_R_java_lang_Integer(value);
    }

    template<>
    inline shared_ptr<java::lang::Long> wrap<>(jlong value) {
        return java::lang::Long::SM_valueOf_R_java_lang_Long(value);
    }

    template<>
    inline shared_ptr<java::lang::Float> wrap<>(jfloat value) {
        return java::lang::Float::SM_valueOf_R_java_lang_Float(value);
    }

    template<>
    inline shared_ptr<java::lang::Double> wrap<>(jdouble value) {
        return java::lang::Double::SM_valueOf_R_java_lang_Double(value);
    }

    template<>
    inline jbool unwrap<jbool>(const jobject &wrapper) {
        return vm::checkedCast<java::lang::Boolean>(wrapper)->M_booleanValue_R_boolean();
    }

    template<>
    inline jbyte unwrap<jbyte>(const jobject &wrapper) {
        return vm::checkedCast<java::lang::Byte>(wrapper)->M_byteValue_R_byte();
    }

    template<>
    inline jshort unwrap<jshort>(const jobject &wrapper) {
        return vm::checkedCast<java::lang::Short>(wrapper)->M_shortValue_R_short();
    }

    template<>
    inline jchar unwrap<jchar>(const jobject &wrapper) {
        return vm::checkedCast<java::lang::Character>(wrapper)->M_charValue_R_char()
        ;
    }

    template<>
    inline jint unwrap<jint>(const jobject &wrapper) {
        return vm::checkedCast<java::lang::Integer>(wrapper)->M_intValue_R_int();
    }

    template<>
    inline jlong unwrap<jlong>(const jobject &wrapper) {
        return vm::checkedCast<java::lang::Long>(wrapper)->M_longValue_R_long();
    }

    template<>
    inline jfloat unwrap<jfloat>(const jobject &wrapper) {
        return vm::checkedCast<java::lang::Float>(wrapper)->M_floatValue_R_float();
    }

    template<>
    inline jdouble unwrap<jdouble>(const jobject &wrapper) {
        return vm::checkedCast<java::lang::Double>(wrapper)->M_doubleValue_R_double();
    }
}
