#include <Clearwing.hpp>
#include <java/lang/Math.hpp>
#include <cmath>

jdouble java::lang::Math::SM_abs_R_double(jdouble value) {
    return std::abs(value);
}

jfloat java::lang::Math::SM_abs_R_float(jfloat value) {
    return std::abs(value);
}

jint java::lang::Math::SM_abs_R_int(jint value) {
    return std::abs(value);
}

jlong java::lang::Math::SM_abs_R_long(jlong value) {
    return std::abs(value);
}

jdouble java::lang::Math::SM_ceil_R_double(jdouble value) {
    auto l = jlong(value);
    if (jdouble(l) == value || value < 0)
        return jdouble(l);
    return jdouble(l + 1);
}

jdouble java::lang::Math::SM_floor_R_double(jdouble value) {
    auto l = jlong(value);
    if (jdouble(l) == value || value >= 0)
        return jdouble(l);
    return jdouble(l - 1);
}

jdouble java::lang::Math::SM_max_R_double(jdouble value1, jdouble value2) {
    return std::max(value1, value2);
}

jdouble java::lang::Math::SM_pow_R_double(jdouble value1, jdouble value2) {
    return std::pow(value1, value2);
}

jfloat java::lang::Math::SM_max_R_float(jfloat value1, jfloat value2) {
    return std::max(value1, value2);
}

jint java::lang::Math::SM_max_R_int(jint value1, jint value2) {
    return std::max(value1, value2);
}

jlong java::lang::Math::SM_max_R_long(jlong value1, jlong value2) {
    return std::max(value1, value2);
}

jdouble java::lang::Math::SM_min_R_double(jdouble value1, jdouble value2) {
    return std::min(value1, value2);
}

jfloat java::lang::Math::SM_min_R_float(jfloat value1, jfloat value2) {
    return std::min(value1, value2);
}

jint java::lang::Math::SM_min_R_int(jint value1, jint value2) {
    return std::min(value1, value2);
}

jlong java::lang::Math::SM_min_R_long(jlong value1, jlong value2) {
    return std::min(value1, value2);
}

jdouble java::lang::Math::SM_cos_R_double(jdouble value) {
    return std::cos(value);
}

jdouble java::lang::Math::SM_sin_R_double(jdouble value) {
    return std::sin(value);
}

jdouble java::lang::Math::SM_sqrt_R_double(jdouble value) {
    return std::sqrt(value);
}

jdouble java::lang::Math::SM_cbrt_R_double(jdouble value) {
    return std::cbrt(value);
}

jdouble java::lang::Math::SM_tan_R_double(jdouble value) {
    return std::tan(value);
}

jdouble java::lang::Math::SM_atan_R_double(jdouble value) {
    return std::atan(value);
}

jdouble java::lang::Math::SM_asin_R_double(jdouble value) {
    return std::asin(value);
}

jdouble java::lang::Math::SM_acos_R_double(jdouble value) {
    return std::acos(value);
}

jdouble java::lang::Math::SM_atan2_R_double(jdouble value1, jdouble value2) {
    return std::atan2(value1, value2);
}

jdouble java::lang::Math::SM_sinh_R_double(jdouble value) {
    return std::sinh(value);
}

jdouble java::lang::Math::SM_cosh_R_double(jdouble value) {
    return std::cosh(value);
}

jdouble java::lang::Math::SM_tanh_R_double(jdouble value) {
    return std::tanh(value);
}

jdouble java::lang::Math::SM_exp_R_double(jdouble value) {
    return std::exp(value);
}

jdouble java::lang::Math::SM_expm1_R_double(jdouble value) {
    return std::expm1(value);
}

jdouble java::lang::Math::SM_log10_R_double(jdouble value) {
    return std::log10(value);
}

jdouble java::lang::Math::SM_log1p_R_double(jdouble value) {
    return std::log1p(value);
}

jdouble java::lang::Math::SM_rint_R_double(jdouble value) {
    return std::rint(value);
}

jdouble java::lang::Math::SM_IEEEremainder_R_double(jdouble value1, jdouble value2) {
    return std::remainder(value1, value2);
}

jdouble java::lang::Math::SM_log_R_double(jdouble value) {
    return std::log(value);
}

jdouble java::lang::Math::SM_hypot_R_double(jdouble value1, jdouble value2) {
    return std::hypot(value1, value2);
}
