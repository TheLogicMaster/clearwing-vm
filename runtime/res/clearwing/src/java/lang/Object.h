#ifndef HEADER_java_lang_Object
#define HEADER_java_lang_Object

#include "Clearwing.h"

#ifdef __cplusplus
extern "C" {
#endif

#define VTABLE_java_lang_Object_hashCode_R_int 0
#define VTABLE_java_lang_Object_equals_java_lang_Object_R_boolean 1
#define VTABLE_java_lang_Object_clone_R_java_lang_Object 2
#define VTABLE_java_lang_Object_getClass_R_java_lang_Class 3
#define VTABLE_java_lang_Object_toString_R_java_lang_String 4
#define VTABLE_java_lang_Object_finalize 5
#define VTABLE_java_lang_Object_notify 6
#define VTABLE_java_lang_Object_notifyAll 7
#define VTABLE_java_lang_Object_wait 8
#define VTABLE_java_lang_Object_wait_long 9
#define VTABLE_java_lang_Object_wait_long_int 10

typedef jint (*func_java_lang_Object_hashCode_R_int)(jcontext ctx, jobject self);
typedef jbool (*func_java_lang_Object_equals_java_lang_Object_R_boolean)(jcontext ctx, jobject self, jobject other);
typedef jobject (*func_java_lang_Object_clone_R_java_lang_Object)(jcontext ctx, jobject self);
typedef jobject (*func_java_lang_Object_getClass_R_java_lang_Class)(jcontext ctx, jobject self);
typedef jobject (*func_java_lang_Object_toString_R_java_lang_String)(jcontext ctx, jobject self);
typedef void (*func_java_lang_Object_finalize)(jcontext ctx, jobject self);
typedef void (*func_java_lang_Object_notify)(jcontext ctx, jobject self);
typedef void (*func_java_lang_Object_notifyAll)(jcontext ctx, jobject self);
typedef void (*func_java_lang_Object_wait)(jcontext ctx, jobject self);
typedef void (*func_java_lang_Object_wait_long)(jcontext ctx, jobject self, jlong millis);
typedef void (*func_java_lang_Object_wait_long_int)(jcontext ctx, jobject self, jlong millis, jint nanos);

void clinit_java_lang_Object(jcontext ctx);
void init_java_lang_Object(jcontext ctx, jobject self);
jint M_java_lang_Object_hashCode_R_int(jcontext ctx, jobject self);
jbool M_java_lang_Object_equals_java_lang_Object_R_boolean(jcontext ctx, jobject self, jobject other);
jobject M_java_lang_Object_clone_R_java_lang_Object(jcontext ctx, jobject self);
jobject M_java_lang_Object_getClass_R_java_lang_Class(jcontext ctx, jobject self);
jobject M_java_lang_Object_toString_R_java_lang_String(jcontext ctx, jobject self);
void M_java_lang_Object_finalize(jcontext ctx, jobject self);
void M_java_lang_Object_notify(jcontext ctx, jobject self);
void M_java_lang_Object_notifyAll(jcontext ctx, jobject self);
void M_java_lang_Object_wait(jcontext ctx, jobject self);
void M_java_lang_Object_wait_long(jcontext ctx, jobject self, jlong millis);
void M_java_lang_Object_wait_long_int(jcontext ctx, jobject self, jlong millis, jint nanos);

#ifdef __cplusplus
}
#endif

#endif
