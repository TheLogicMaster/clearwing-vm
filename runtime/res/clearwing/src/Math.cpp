#include "java/lang/Math.h"

#include <cmath>

extern "C" {

jdouble SM_java_lang_Math_abs_double_R_double(jcontext ctx, jdouble value) {
    return std::abs(value);
}

jfloat SM_java_lang_Math_abs_float_R_float(jcontext ctx, jfloat value) {
    return std::abs(value);
}

jint SM_java_lang_Math_abs_int_R_int(jcontext ctx, jint value) {
    return std::abs(value);
}

jlong SM_java_lang_Math_abs_long_R_long(jcontext ctx, jlong value) {
    return std::abs(value);
}

jdouble SM_java_lang_Math_ceil_double_R_double(jcontext ctx, jdouble value) {
    auto l = jlong(value);
    if (jdouble(l) == value || value < 0)
        return jdouble(l);
    return jdouble(l + 1);
}

jdouble SM_java_lang_Math_floor_double_R_double(jcontext ctx, jdouble value) {
    auto l = jlong(value);
    if (jdouble(l) == value || value >= 0)
        return jdouble(l);
    return jdouble(l - 1);
}

jdouble SM_java_lang_Math_max_double_double_R_double(jcontext ctx, jdouble value1, jdouble value2) {
    return std::max(value1, value2);
}

jdouble SM_java_lang_Math_pow_double_double_R_double(jcontext ctx, jdouble value1, jdouble value2) {
    return std::pow(value1, value2);
}

jfloat SM_java_lang_Math_max_float_float_R_float(jcontext ctx, jfloat value1, jfloat value2) {
    return std::max(value1, value2);
}

jint SM_java_lang_Math_max_int_int_R_int(jcontext ctx, jint value1, jint value2) {
    return std::max(value1, value2);
}

jlong SM_java_lang_Math_max_long_long_R_long(jcontext ctx, jlong value1, jlong value2) {
    return std::max(value1, value2);
}

jdouble SM_java_lang_Math_min_double_double_R_double(jcontext ctx, jdouble value1, jdouble value2) {
    return std::min(value1, value2);
}

jfloat SM_java_lang_Math_min_float_float_R_float(jcontext ctx, jfloat value1, jfloat value2) {
    return std::min(value1, value2);
}

jint SM_java_lang_Math_min_int_int_R_int(jcontext ctx, jint value1, jint value2) {
    return std::min(value1, value2);
}

jlong SM_java_lang_Math_min_long_long_R_long(jcontext ctx, jlong value1, jlong value2) {
    return std::min(value1, value2);
}

jdouble SM_java_lang_Math_cos_double_R_double(jcontext ctx, jdouble value) {
    return std::cos(value);
}

jdouble SM_java_lang_Math_sin_double_R_double(jcontext ctx, jdouble value) {
    return std::sin(value);
}

jdouble SM_java_lang_Math_sqrt_double_R_double(jcontext ctx, jdouble value) {
    return std::sqrt(value);
}

jdouble SM_java_lang_Math_cbrt_double_R_double(jcontext ctx, jdouble value) {
    return std::cbrt(value);
}

jdouble SM_java_lang_Math_tan_double_R_double(jcontext ctx, jdouble value) {
    return std::tan(value);
}

jdouble SM_java_lang_Math_atan_double_R_double(jcontext ctx, jdouble value) {
    return std::atan(value);
}

jdouble SM_java_lang_Math_asin_double_R_double(jcontext ctx, jdouble value) {
    return std::asin(value);
}

jdouble SM_java_lang_Math_acos_double_R_double(jcontext ctx, jdouble value) {
    return std::acos(value);
}

jdouble SM_java_lang_Math_atan2_double_double_R_double(jcontext ctx, jdouble value1, jdouble value2) {
    return std::atan2(value1, value2);
}

jdouble SM_java_lang_Math_sinh_double_R_double(jcontext ctx, jdouble value) {
    return std::sinh(value);
}

jdouble SM_java_lang_Math_cosh_double_R_double(jcontext ctx, jdouble value) {
    return std::cosh(value);
}

jdouble SM_java_lang_Math_tanh_double_R_double(jcontext ctx, jdouble value) {
    return std::tanh(value);
}

jdouble SM_java_lang_Math_exp_double_R_double(jcontext ctx, jdouble value) {
    return std::exp(value);
}

jdouble SM_java_lang_Math_expm1_double_R_double(jcontext ctx, jdouble value) {
    return std::expm1(value);
}

jdouble SM_java_lang_Math_log10_double_R_double(jcontext ctx, jdouble value) {
    return std::log10(value);
}

jdouble SM_java_lang_Math_log1p_double_R_double(jcontext ctx, jdouble value) {
    return std::log1p(value);
}

jdouble SM_java_lang_Math_rint_double_R_double(jcontext ctx, jdouble value) {
    return std::rint(value);
}

jdouble SM_java_lang_Math_IEEEremainder_double_double_R_double(jcontext ctx, jdouble value1, jdouble value2) {
    return std::remainder(value1, value2);
}

jdouble SM_java_lang_Math_log_double_R_double(jcontext ctx, jdouble value) {
    return std::log(value);
}

jdouble SM_java_lang_Math_hypot_double_double_R_double(jcontext ctx, jdouble value1, jdouble value2) {
    return std::hypot(value1, value2);
}

}
