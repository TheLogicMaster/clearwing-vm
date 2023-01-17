#pragma once

#include <cmath>
#include <utility>
#include <algorithm>

#include "Clearwing.hpp"
#include "Utils.hpp"

namespace inst {
    inline void aaload(DataVariant *&sp) {
        sp--;
        sp[-1] = vm::checkedCast<vm::Array>(get<jobject>(sp[-1]))->get<jobject>(get<jint>(sp[0]));
    }

    inline void aastore(DataVariant *&sp) {
        vm::checkedCast<vm::Array>(get<jobject>(sp[-3]))->get<jobject>(get<jint>(sp[-2])) = get<jobject>(sp[-1]);
        sp -= 3;
    }

    inline void aconst_null(DataVariant *&sp) {
        *(sp++) = jobject(nullptr);
    }

    inline void aload(DataVariant *&sp, DataVariant &local) {
        *(sp++) = local;
    }

    inline void anewarray(DataVariant *&sp, const jobject& type) {
        sp[-1] = vm::newArray(vm::checkedCast<java::lang::Class>(type), get<jint>(sp[-1]));
    }

    inline void arraylength(DataVariant *&sp) {
        sp[-1] = vm::checkedCast<vm::Array>(get<jobject>(sp[-1]))->length;
    }

    inline void astore(DataVariant *&sp, DataVariant &local) {
        local = *(--sp);
    }

    inline void athrow(DataVariant *&sp) {
        throw vm::nullCheck(get<jobject>(*(--sp)));
    }

    inline void baload(DataVariant *&sp) {
        sp--;
        sp[-1] = jint(vm::checkedCast<vm::Array>(get<jobject>(sp[-1]))->get<jbyte>(get<jint>(sp[0])));
    }

    inline void bastore(DataVariant *&sp) {
        vm::checkedCast<vm::Array>(get<jobject>(sp[-3]))->get<jbyte>(get<jint>(sp[-2])) = jbyte(get<jint>(sp[-1]));
        sp -= 3;
    }

    inline void bipush(DataVariant *&sp, jbyte value) {
        *(sp++) = value;
    }

    inline void caload(DataVariant *&sp) {
        sp--;
        sp[-1] = jint(vm::checkedCast<vm::Array>(get<jobject>(sp[-1]))->get<jchar>(get<jint>(sp[0])));
    }

    inline void castore(DataVariant *&sp) {
        vm::checkedCast<vm::Array>(get<jobject>(sp[-3]))->get<jchar>(get<jint>(sp[-2])) = jchar(get<jint>(sp[-1]));
        sp -= 3;
    }

    inline void checkcast(DataVariant *&sp, const jobject& type) {
        if (!get<jobject>(sp[-1])) // Do nothing for null
            return;
        if (!vm::checkedCast<java::lang::Class>(type)->M_isInstance_R_boolean(get<jobject>(sp[-1])))
            vm::throwNew<java::lang::ClassCastException>();
    }

    inline void d2f(DataVariant *&sp) {
        sp[-1] = jfloat(get<jdouble>(sp[-1]));
    }

    inline void d2i(DataVariant *&sp) {
        sp[-1] = jint(get<jdouble>(sp[-1]));
    }

    inline void d2l(DataVariant *&sp) {
        sp[-1] = jlong(get<jdouble>(sp[-1]));
    }

    inline void dadd(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jdouble>(sp[-1]) + get<jdouble>(sp[0]);
    }

    inline void daload(DataVariant *&sp) {
        sp--;
        sp[-1] = vm::checkedCast<vm::Array>(get<jobject>(sp[-1]))->get<jdouble>(get<jint>(sp[0]));
    }

    inline void dastore(DataVariant *&sp) {
        vm::checkedCast<vm::Array>(get<jobject>(sp[-3]))->get<jdouble>(get<jint>(sp[-2])) = get<jdouble>(sp[-1]);
        sp -= 3;
    }

    inline void dcmpg(DataVariant *&sp) {
        sp--;
        sp[-1] = vm::floatCompare(get<jdouble>(sp[-1]), get<jdouble>(sp[0]), 1);
    }

    inline void dcmpl(DataVariant *&sp) {
        sp--;
        sp[-1] = vm::floatCompare(get<jdouble>(sp[-1]), get<jdouble>(sp[0]), -1);
    }

    inline void dconst(DataVariant *&sp, jdouble value) {
        *(sp++) = value;
    }

    inline void ddiv(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jdouble>(sp[-1]) / get<jdouble>(sp[0]);
    }

    inline void dload(DataVariant *&sp, DataVariant &local) {
        *(sp++) = get<jdouble>(local);
    }

    inline void dmul(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jdouble>(sp[-1]) * get<jdouble>(sp[0]);
    }

    inline void dneg(DataVariant *&sp) {
        sp[-1] = -get<jdouble>(sp[-1]);
    }

    inline void drem(DataVariant *&sp) {
        sp--;
        sp[-1] = fmod(get<jdouble>(sp[-1]), get<jdouble>(sp[0]));
    }

    inline void dstore(DataVariant *&sp, DataVariant &local) {
        local = get<jdouble>(*(--sp));
    }

    inline void dsub(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jdouble>(sp[-1]) - get<jdouble>(sp[0]);
    }

    inline void dup(DataVariant *&sp) {
        sp[0] = sp[-1];
        sp++;
    }

    inline void dup_x1(DataVariant *&sp) {
        sp[0] = sp[-1];
        sp[-1] = sp[-2];
        sp[-2] = sp[0];
        sp++;
    }

    inline void dup_x2(DataVariant *&sp) {
        if (isDataVariantWide(sp[-2])) { // Form 2
            sp[0] = sp[-1];
            sp[-1] = sp[-2];
            sp[-2] = sp[0];
        } else { // Form 1
            sp[0] = sp[-1];
            sp[-1] = sp[-2];
            sp[-2] = sp[-3];
            sp[-3] = sp[0];
        }
        sp++;
    }

    inline void dup2(DataVariant *&sp) {
        if (isDataVariantWide(sp[-1])) { // Form 2
            sp[0] = sp[-1];
            sp++;
        } else { // Form 1
            sp[0] = sp[-2];
            sp[1] = sp[-1];
            sp += 2;
        }
    }

    inline void dup2_x1(DataVariant *&sp) {
        if (isDataVariantWide(sp[-1])) { // Form 2
            sp[0] = sp[-1];
            sp[-1] = sp[-2];
            sp[-2] = sp[0];
            sp++;
        } else { // Form 1
            sp[1] = sp[-1];
            sp[0] = sp[-2];
            sp[-1] = sp[-3];
            sp[-2] = sp[1];
            sp[-3] = sp[0];
            sp += 2;
        }
    }

    inline void dup2_x2(DataVariant *&sp) {
        if (isDataVariantWide(sp[-2])) { // Form 4
            sp[0] = sp[-1];
            sp[-1] = sp[-2];
            sp[-2] = sp[0];
            sp++;
        } else if (isDataVariantWide(sp[-1])) { // Form 2
            sp[0] = sp[-1];
            sp[-1] = sp[-2];
            sp[-2] = sp[-3];
            sp[-3] = sp[0];
            sp++;
        } else if (isDataVariantWide(sp[-3])) { // Form 3
            sp[1] = sp[-1];
            sp[0] = sp[-2];
            sp[-1] = sp[-3];
            sp[-2] = sp[1];
            sp[-3] = sp[0];
            sp += 2;
        } else { // Form 1
            sp[1] = sp[-1];
            sp[0] = sp[-2];
            sp[-1] = sp[-3];
            sp[-2] = sp[-4];
            sp[-3] = sp[1];
            sp[-4] = sp[0];
            sp += 2;
        }
    }

    inline void f2d(DataVariant *&sp) {
        sp[-1] = jdouble(get<jfloat>(sp[-1]));
    }

    inline void f2i(DataVariant *&sp) {
        sp[-1] = jint(get<jfloat>(sp[-1]));
    }

    inline void f2l(DataVariant *&sp) {
        sp[-1] = jlong(get<jfloat>(sp[-1]));
    }

    inline void fadd(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jfloat>(sp[-1]) + get<jfloat>(sp[0]);
    }

    inline void faload(DataVariant *&sp) {
        sp--;
        sp[-1] = vm::checkedCast<vm::Array>(get<jobject>(sp[-1]))->get<jfloat>(get<jint>(sp[0]));
    }

    inline void fastore(DataVariant *&sp) {
        vm::checkedCast<vm::Array>(get<jobject>(sp[-3]))->get<jfloat>(get<jint>(sp[-2])) = get<jfloat>(sp[-1]);
        sp -= 3;
    }

    inline void fcmpg(DataVariant *&sp) {
        sp--;
        sp[-1] = vm::floatCompare(get<jfloat>(sp[-1]), get<jfloat>(sp[0]), 1);
    }

    inline void fcmpl(DataVariant *&sp) {
        sp--;
        sp[-1] = vm::floatCompare(get<jfloat>(sp[-1]), get<jfloat>(sp[0]), -1);
    }

    inline void fconst(DataVariant *&sp, jfloat value) {
        *(sp++) = value;
    }

    inline void fdiv(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jfloat>(sp[-1]) / get<jfloat>(sp[0]);
    }

    inline void fload(DataVariant *&sp, DataVariant &local) {
        *(sp++) = get<jfloat>(local);
    }

    inline void fmul(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jfloat>(sp[-1]) * get<jfloat>(sp[0]);
    }

    inline void fneg(DataVariant *&sp) {
        sp[-1] = -get<jfloat>(sp[-1]);
    }

    inline void frem(DataVariant *&sp) {
        sp--;
        sp[-1] = fmodf(get<jfloat>(sp[-1]), get<jfloat>(sp[0]));
    }

    inline void fstore(DataVariant *&sp, DataVariant &local) {
        local = get<jfloat>(*(--sp));
    }

    inline void fsub(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jfloat>(sp[-1]) - get<jfloat>(sp[0]);
    }

    inline void i2b(DataVariant *&sp) {
        sp[-1] = jint(jbyte(get<jint>(sp[-1]) & 0xFF));
    }

    inline void i2c(DataVariant *&sp) {
        sp[-1] = jint(get<jint>(sp[-1]) & 0xFFFF);
    }

    inline void i2d(DataVariant *&sp) {
        sp[-1] = jdouble(get<jint>(sp[-1]));
    }

    inline void i2f(DataVariant *&sp) {
        sp[-1] = jfloat(get<jint>(sp[-1]));
    }

    inline void i2l(DataVariant *&sp) {
        sp[-1] = jlong(get<jint>(sp[-1]));
    }

    inline void i2s(DataVariant *&sp) {
        sp[-1] = jint(jshort(get<jint>(sp[-1]) & 0xFFFF));
    }

    inline void iadd(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jint>(sp[-1]) + get<jint>(sp[0]);
    }

    inline void iaload(DataVariant *&sp) {
        sp--;
        sp[-1] = vm::checkedCast<vm::Array>(get<jobject>(sp[-1]))->get<jint>(get<jint>(sp[0]));
    }

    inline void iand(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jint>(sp[-1]) & get<jint>(sp[0]);
    }

    inline void iastore(DataVariant *&sp) {
        vm::checkedCast<vm::Array>(get<jobject>(sp[-3]))->get<jint>(get<jint>(sp[-2])) = get<jint>(sp[-1]);
        sp -= 3;
    }

    inline void iconst(DataVariant *&sp, jint value) {
        *(sp++) = value;
    }

    inline void idiv(DataVariant *&sp) {
        sp--;
        if (get<jint>(sp[0]) == 0)
            vm::throwDivisionByZero();
        sp[-1] = get<jint>(sp[-1]) / get<jint>(sp[0]);
    }

    inline void iinc(DataVariant *&sp, DataVariant &local, jint amount) {
        get<jint>(local) += amount;
    }

    inline void iload(DataVariant *&sp, DataVariant &local) {
        *(sp++) = get<jint>(local);
    }

    inline void imul(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jint>(sp[-1]) * get<jint>(sp[0]);
    }

    inline void ineg(DataVariant *&sp) {
        sp[-1] = -get<jint>(sp[-1]);
    }

    inline void instanceof(DataVariant *&sp, const jobject& type) {
        sp[-1] = vm::checkedCast<java::lang::Class>(type)->M_isInstance_R_boolean(get<jobject>(sp[-1]));
    }

    inline void ior(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jint>(sp[-1]) | get<jint>(sp[0]);
    }

    inline void irem(DataVariant *&sp) {
        sp--;
        if (get<jint>(sp[0]) == 0)
            vm::throwDivisionByZero();
        sp[-1] = get<jint>(sp[-1]) % get<jint>(sp[0]);
    }

    inline void ishl(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jint>(sp[-1]) << get<jint>(sp[0]);
    }

    inline void ishr(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jint>(sp[-1]) >> get<jint>(sp[0]);
    }

    inline void istore(DataVariant *&sp, DataVariant &local) {
        local = get<jint>(*(--sp));
    }

    inline void isub(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jint>(sp[-1]) - get<jint>(sp[0]);
    }

    inline void iushr(DataVariant *&sp) {
        sp--;
        sp[-1] = bit_cast<jint>(bit_cast<uint32_t>(get<jint>(sp[-1])) >> get<jint>(sp[0]));
    }

    inline void ixor(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jint>(sp[-1]) ^ get<jint>(sp[0]);
    }

    inline void l2d(DataVariant *&sp) {
        sp[-1] = jdouble(get<jlong>(sp[-1]));
    }

    inline void l2f(DataVariant *&sp) {
        sp[-1] = jfloat(get<jlong>(sp[-1]));
    }

    inline void l2i(DataVariant *&sp) {
        sp[-1] = jint(get<jlong>(sp[-1]));
    }

    inline void ladd(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jlong>(sp[-1]) + get<jlong>(sp[0]);
    }

    inline void laload(DataVariant *&sp) {
        sp--;
        sp[-1] = vm::checkedCast<vm::Array>(get<jobject>(sp[-1]))->get<jlong>(get<jint>(sp[0]));
    }

    inline void land(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jlong>(sp[-1]) & get<jlong>(sp[0]);
    }

    inline void lastore(DataVariant *&sp) {
        vm::checkedCast<vm::Array>(get<jobject>(sp[-3]))->get<jlong>(get<jint>(sp[-2])) = get<jlong>(sp[-1]);
        sp -= 3;
    }

    inline void lcmp(DataVariant *&sp) {
        sp--;
        if (get<jlong>(sp[-1]) > get<jlong>(sp[0]))
            sp[-1] = jint(1);
        else if (get<jlong>(sp[-1]) < get<jlong>(sp[0]))
            sp[-1] = jint(-1);
        else
            sp[-1] = jint(0);
    }

    inline void lconst(DataVariant *&sp, jlong value) {
        *(sp++) = value;
    }

    inline void ldiv(DataVariant *&sp) {
        sp--;
        if (get<jlong>(sp[0]) == 0)
            vm::throwDivisionByZero();
        sp[-1] = get<jlong>(sp[-1]) / get<jlong>(sp[0]);
    }

    inline void lload(DataVariant *&sp, DataVariant &local) {
        *(sp++) = get<jlong>(local);
    }

    inline void lmul(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jlong>(sp[-1]) * get<jlong>(sp[0]);
    }

    inline void lneg(DataVariant *&sp) {
        sp[-1] = -get<jlong>(sp[-1]);
    }

    inline void lor(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jlong>(sp[-1]) | get<jlong>(sp[0]);
    }

    inline void lrem(DataVariant *&sp) {
        sp--;
        if (get<jlong>(sp[0]) == 0)
            vm::throwDivisionByZero();
        sp[-1] = get<jlong>(sp[-1]) % get<jlong>(sp[0]);
    }

    inline void lshl(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jlong>(sp[-1]) << get<jint>(sp[0]);
    }

    inline void lshr(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jlong>(sp[-1]) >> get<jint>(sp[0]);
    }

    inline void lstore(DataVariant *&sp, DataVariant &local) {
        local = get<jlong>(*(--sp));
    }

    inline void lsub(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jlong>(sp[-1]) - get<jlong>(sp[0]);
    }

    inline void lushr(DataVariant *&sp) {
        sp--;
        sp[-1] = bit_cast<jlong>(bit_cast<uint64_t>(get<jlong>(sp[-1])) >> get<jint>(sp[0]));
    }

    inline void lxor(DataVariant *&sp) {
        sp--;
        sp[-1] = get<jlong>(sp[-1]) ^ get<jlong>(sp[0]);
    }

    inline void monitorenter(DataVariant *&sp) {
        vm::pop<jobject>(sp)->acquireMonitor();
    }

    inline void monitorexit(DataVariant *&sp) {
        vm::pop<jobject>(sp)->releaseMonitor();
    }

    inline void multianewarray(DataVariant *&sp, const jobject& type, int dimensionCount) {
        std::vector<int> dimensions;
        for (int i = 0; i < dimensionCount; i++)
            dimensions.push_back(get<jint>(*(--sp)));
        std::reverse(dimensions.begin(), dimensions.end());
        *(sp++) = vm::newMultiArray(vm::checkedCast<java::lang::Class>(type), dimensions);
    }

    inline void newarray(DataVariant *&sp, const jobject& type) {
        sp[-1] = vm::newArray(vm::checkedCast<java::lang::Class>(type), get<jint>(sp[-1]));
    }

    inline void nop(DataVariant *&sp) {
    }

    inline void pop(DataVariant *&sp) {
        sp--;
    }

    inline void pop2(DataVariant *&sp) {
        if (isDataVariantWide(sp[-1]))
            sp--; // Form 2
        else
            sp -= 2; // Form 1
    }

    inline void saload(DataVariant *&sp) {
        sp--;
        sp[-1] = jint(vm::checkedCast<vm::Array>(get<jobject>(sp[-1]))->get<jshort>(get<jint>(sp[0])));
    }

    inline void sastore(DataVariant *&sp) {
        vm::checkedCast<vm::Array>(get<jobject>(sp[-3]))->get<jshort>(get<jint>(sp[-2])) = jshort(get<jint>(sp[-1]));
        sp -= 3;
    }

    inline void sipush(DataVariant *&sp, jshort value) {
        *(sp++) = jint(value);
    }

    inline void swap(DataVariant *&sp) {
        std::swap(sp[-1], sp[-2]);
    }
}
