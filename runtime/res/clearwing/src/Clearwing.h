#ifndef CLEARWING
#define CLEARWING

#include "Config.h"

#include <csetjmp>

#ifdef __cplusplus
#include <cstdint>
#else
#include <stdint.h>
typedef uint8_t bool;
typedef int64_t intptr_t;
#define true 1
#define false 0
#endif

#ifdef __cplusplus
extern "C" {
#endif

#define GC_MARK_START 0
#define GC_MARK_END 100
#define GC_MARK_NATIVE (-1)
#define GC_MARK_ETERNAL (-2)
#define GC_MARK_COLLECTED (-3)

#ifndef MAX_STACK_DEPTH
#define MAX_STACK_DEPTH 1000
#endif

typedef int8_t jbyte;
typedef uint16_t jchar;
typedef int16_t jshort;
typedef int32_t jint;
typedef int64_t jlong;
typedef float jfloat;
typedef double jdouble;
typedef bool jbool;
typedef void jvoid;
typedef jlong jref; // Store references as jlong to ensure unspecified native pointer size doesn't change struct layout

typedef struct Array *jarray;
typedef struct Context *jcontext;
typedef struct StackFrame *jframe;
typedef struct ObjectMonitor *jmonitor;
typedef struct Class *jclass;

typedef struct java_lang_Object *jobject;
typedef struct java_lang_String *jstring;
typedef struct java_lang_Thread *jthread;

typedef void (*static_init_ptr)(jcontext ctx);
typedef void (*finalizer_ptr)(jcontext ctx, jobject self);
typedef void (*gc_mark_ptr)(jobject object, jint mark);
typedef void (*main_ptr)(jcontext ctx, jobject args);

typedef struct VtableEntry {
    const char *name;
    const char *desc;
} VtableEntry;

typedef struct FieldMetadata {
    const char *name;
    jclass type;
    jlong offset;
    const char *desc;
    int access;
} FieldMetadata;

typedef struct MethodMetadata {
    const char *name;
    jlong offset;
    const char *desc;
    int access;
} MethodMetadata;

typedef struct ClassMetadata {
    int access;
    int interfaceCount;
    const char *interfaces;
    int fieldCount;
    FieldMetadata *fields;
    int methodCount;
    MethodMetadata *methods;
    bool anonymous;
    bool synthetic;
} ClassMetadata;

typedef struct java_lang_Object {
    jref clazz;
    jint gcMark;
    jref vtable;
    jref monitor;
} java_lang_Object;

typedef struct Array {
    java_lang_Object parent;
    int length;
    void *data;
} Array;

// This is a mirror of the generated java_lang_Class for sanity
typedef struct Class {
    // The alignment/padding here is sometimes different from explicitly relisting the fields, so use this nested struct approach in LLVM and such
    java_lang_Object parent;
    jlong nativeName;
    jref parentClass;
    jint size;
    jlong classVtable;
    jlong staticInitializer;
    jlong markFunction;
    jbool primitive;
    jint arrayDimensions;
    jref componentClass;
    jint access;
    jint interfaceCount;
    jlong nativeInterfaces;
    jint fieldCount;
    jlong nativeFields;
    jint methodCount;
    jlong nativeMethods;
    jint vtableSize;
    jlong vtableEntries;
    jbool anonymous;
    jbool synthetic;
    jlong instanceOfCache;
    // Lazy-init fields start here
    jbool initialized;
    jref name;
    jref interfaces;
    jref fields;
    jref methods;
    jref constructors;
    jref annotations;
} Class;

typedef struct {
    const char *string;
    int length;
} StringLiteral;

typedef union {
    jobject o;
    jint i;
    jlong l;
    jfloat f;
    jdouble d;
} jtype;

extern Class class_java_lang_Object, class_byte, class_char, class_short, class_int, class_long, class_float, class_double, class_boolean, class_void;

extern Class class_java_lang_NullPointerException;
extern void init_java_lang_NullPointerException(jcontext ctx, jobject object);

extern volatile bool suspendVM;

void runVM(main_ptr entrypoint);
void threadEntrypoint(jcontext ctx, jthread thread);

const char *getOSLanguage();
const char *getSystemProperty(const char *key);

bool registerClass(jclass clazz);
jclass classForName(const char *name);
bool isAssignableFrom(jcontext ctx, jclass type, jclass assignee);
bool isInstance(jcontext ctx, jobject object, jclass type);
void *resolveInterfaceMethod(jcontext ctx, jclass interface, int method, jobject object);
jobject gcAlloc(jcontext ctx, jclass clazz);
jobject gcAllocNative(jcontext ctx, jclass clazz);
void runGC(jcontext ctx);

jclass getArrayClass(jclass componentType, int dimensions);
jarray createArray(jcontext ctx, jclass type, int length);
void disposeArray(jcontext ctx, jarray array);
jstring stringFromNative(jcontext ctx, const char *string);
jstring stringFromNativeLength(jcontext ctx, const char *string, int length);
jstring createStringLiteral(jcontext ctx, StringLiteral string);
const char *stringToNative(jcontext ctx, jstring string);

void acquireCriticalLock();
void releaseCriticalLock();
void safepointSuspend(jcontext ctx);

jframe pushStackFrame(jcontext ctx, int size, const jtype *stack, const char *method, jobject monitor);
void popStackFrame(jcontext ctx);
jmp_buf *pushExceptionFrame(jframe frame, jclass type);
jobject popExceptionFrame(jframe frame);
void monitorEnter(jcontext ctx, jobject object);
void monitorExit(jcontext ctx, jobject object);
void monitorOwnerCheck(jcontext ctx, jobject object);
void interruptedCheck(jcontext ctx);
void adjustHeapUsage(int64_t amount);

jobject nullCheck(jcontext ctx, jobject object);
void throwException(jcontext ctx, jobject exception);
void throwDivisionByZero(jcontext ctx);
void throwClassCast(jcontext ctx);
void throwNullPointer(jcontext ctx);
void throwIndexOutOfBounds(jcontext ctx);
void throwIllegalArgument(jcontext ctx);
void throwIOException(jcontext ctx, const char *message);

jobject boxByte(jcontext ctx, jbyte value);
jobject boxCharacter(jcontext ctx, jchar value);
jobject boxShort(jcontext ctx, jshort value);
jobject boxInteger(jcontext ctx, jint value);
jobject boxLong(jcontext ctx, jlong value);
jobject boxFloat(jcontext ctx, jfloat value);
jobject boxDouble(jcontext ctx, jdouble value);
jobject boxBoolean(jcontext ctx, jbool value);
jbyte unboxByte(jobject boxed);
jchar unboxCharacter(jobject boxed);
jshort unboxShort(jobject boxed);
jint unboxInteger(jobject boxed);
jlong unboxLong(jobject boxed);
jfloat unboxFloat(jobject boxed);
jdouble unboxDouble(jobject boxed);
jbool unboxBoolean(jobject boxed);

void instMultiANewArray(jcontext ctx, volatile jtype * volatile &sp, jclass type, int dimensions);
jint floatCompare(jfloat value1, jfloat value2, jint nanValue);
jint doubleCompare(jdouble value1, jdouble value2, jint nanValue);
jint longCompare(jlong value1, jlong value2);

#define SEMICOLON_RECEPTOR 0

#define CONCAT_(a, b) CONCAT_INNER_(a, b)
#define CONCAT_INNER_(a, b) a ## b
#define ex_CATCH_ ,ex,
#define CATCH(pair) ,pair##_CATCH_
#define TRY_(block, type, name, except) \
    if (setjmp(*pushExceptionFrame(frameRef, &class_##type))) \
        goto CONCAT_(catch_target_, __LINE__); \
    {block \
    popExceptionFrame(frameRef); \
    goto CONCAT_(catch_done_, __LINE__); \
    CONCAT_(catch_target_, __LINE__): { \
    jobject name = popExceptionFrame(frameRef); \
    name->gcMark = GC_MARK_NATIVE; \
    except \
    name->gcMark = GC_MARK_START; } \
    } CONCAT_(catch_done_, __LINE__):;
#define TRY(args) TRY_(args)

#define TRY_CATCH(block, type, name, except) \
    if (setjmp(*pushExceptionFrame(frameRef, &class_##type))) { \
        jobject name = popExceptionFrame(frameRef); \
        name->gcMark = GC_MARK_NATIVE; \
        except; \
        name->gcMark = GC_MARK_START; \
        goto CONCAT_(try_done_, __LINE__); \
    } \
    block; \
    popExceptionFrame(frameRef); \
    CONCAT_(try_done_, __LINE__):

#define CONSTRUCT_OBJECT(clazz, constructor, ...) \
    ({ jobject object = gcAllocNative(ctx, clazz); \
    constructor(ctx, object __VA_OPT__(,) __VA_ARGS__); \
    object->gcMark = GC_MARK_START; \
    object; })

#define CONSTRUCT_AND_THROW(clazz, constructor, ...) \
    throwException(ctx, CONSTRUCT_OBJECT(clazz, constructor __VA_OPT__(,) __VA_ARGS__))

#define INVOKE_VIRTUAL(func, index, obj, ...) \
    ((func) ((void **) NULL_CHECK(obj)->vtable)[index])(ctx, obj __VA_OPT__(,) __VA_ARGS__)

#define INVOKE_INTERFACE(func, clazz, index, obj, ...) \
    ((func) resolveInterfaceMethod(ctx, clazz, index, obj))(ctx, obj __VA_OPT__(,) __VA_ARGS__)

// When calling this, all objects must be safely stored, either on a stack frame, a class/object field, or have the mark value set to one of the special constants
#define SAFEPOINT() \
    if (suspendVM)\
        safepointSuspend(ctx)

#define NULL_CHECK(object) \
    (({ if (!object) throwNullPointer(ctx); }), object)

#define ARRAY_ACCESS(obj, type, index) \
    (({ if (index >= ((jarray) NULL_CHECK(obj))->length) throwIndexOutOfBounds(ctx); }), ((type *) ((jarray) obj)->data)[index])

#define POP_N(count) \
    sp -= count

#define PUSH_OBJECT(object) \
    (sp++)->o = object

#define POP_OBJECT() \
    (--sp)->o

#define INST_AALOAD() \
    sp--; \
    sp[-1].o = ARRAY_ACCESS(sp[-1].o, jobject, sp[0].i)

#define INST_AASTORE() \
    ARRAY_ACCESS(sp[-3].o, jobject, sp[-2].i) = sp[-1].o; \
    sp -= 3

#define INST_ACONST_NULL() \
    (sp++)->l = 0

#define INST_ALOAD(local) \
    (sp++)->o = frame[local].o

#define INST_ANEWARRAY(clazz) \
    sp[-1].o = (jobject) createArray(ctx, clazz, sp[-1].i)

#define INST_ARRAYLENGTH() \
    sp[-1].i = NULL_CHECK((jarray) sp[-1].o)->length

#define INST_ASTORE(local) \
    frame[local].o = (--sp)->o

#define INST_ATHROW() \
    throwException(ctx, POP_OBJECT())

#define INST_BALOAD() \
    sp--; \
    sp[-1].i = ARRAY_ACCESS(sp[-1].o, jbyte, sp[0].i)

#define INST_BASTORE() \
    ARRAY_ACCESS(sp[-3].o, jbyte, sp[-2].i) = sp[-1].i; \
    sp -= 3

#define INST_BIPUSH(value) \
    (sp++)->i = value

#define INST_CALOAD() \
    sp--; \
    sp[-1].i = ARRAY_ACCESS(sp[-1].o, jchar, sp[0].i)

#define INST_CASTORE() \
    ARRAY_ACCESS(sp[-3].o, jchar, sp[-2].i) = sp[-1].i; \
    sp -= 3

#define INST_CHECKCAST(type) \
    if (sp[-1].o && !isInstance(ctx, sp[-1].o, type)) throwClassCast(ctx)

#define INST_FASTORE() \
    ARRAY_ACCESS(sp[-3].o, jfloat, sp[-2].i) = sp[-1].f; \
    sp -= 3

#define INST_D2F() \
    sp[-1].f = sp[-1].d

#define INST_D2I() \
    sp[-1].i = (jint) sp[-1].d

#define INST_D2L() \
    sp[-1].l = (jlong) sp[-1].d

#define INST_DADD() \
    sp--; \
    sp[-1].d = sp[-1].d + sp[0].d

#define INST_DALOAD() \
    sp--; \
    sp[-1].d = ARRAY_ACCESS(sp[-1].o, jdouble, sp[0].i)

#define INST_DASTORE() \
    ARRAY_ACCESS(sp[-3].o, jdouble, sp[-2].i) = sp[-1].d; \
    sp -= 3

#define INST_DCMPG() \
    sp--; \
    sp[-1].i = doubleCompare(sp[-1].d, sp[0].d, 1)

#define INST_DCMPL() \
    sp--; \
    sp[-1].i = doubleCompare(sp[-1].d, sp[0].d, -1)

#define INST_DCONST(value) \
    (sp++)->d = value

#define INST_DDIV() \
    sp--; \
    sp[-1].d = sp[-1].d / sp[0].d

#define INST_DLOAD(local) \
    (sp++)->d = frame[local].d

#define INST_DMUL() \
    sp--; \
    sp[-1].d = sp[-1].d * sp[0].d

#define INST_DNEG() \
    sp[-1].d = -sp[-1].d

#define INST_DREM() \
    sp--; \
    sp[-1].d = fmod(sp[-1].d, sp[0].d)

#define INST_DSTORE(local) \
    frame[local].d = (--sp)->d

#define INST_DSUB() \
    sp--; \
    sp[-1].d = sp[-1].d - sp[0].d

#define INST_DUP() \
    sp[0].l = sp[-1].l; \
    sp++

#define INST_DUP_X1() \
    sp[0].l = sp[-1].l; \
    sp[-1].l = sp[-2].l; \
    sp[-2].l = sp[0].l; \
    sp++

#define INST_DUP_X2_1() \
    sp[0].l = sp[-1].l; \
    sp[-1].l = sp[-2].l; \
    sp[-2].l = sp[-3].l; \
    sp[-3].l = sp[0].l; \
    sp++

#define INST_DUP_X2_2() \
    sp[0].l = sp[-1].l; \
    sp[-1].l = sp[-2].l; \
    sp[-2].l = sp[0].l; \
    sp++

#define INST_DUP2_1() \
    sp[0].l = sp[-2].l; \
    sp[1].l = sp[-1].l; \
    sp += 2

#define INST_DUP2_2() \
    sp[0].l = sp[-1].l; \
    sp++

#define INST_DUP2_X1_1() \
    sp[1].l = sp[-1].l; \
    sp[0].l = sp[-2].l; \
    sp[-1].l = sp[-3].l; \
    sp[-2].l = sp[1].l; \
    sp[-3].l = sp[0].l; \
    sp += 2

#define INST_DUP2_X1_2() \
    sp[0].l = sp[-1].l; \
    sp[-1].l = sp[-2].l; \
    sp[-2].l = sp[0].l; \
    sp++

#define INST_DUP2_X2_1() \
    sp[1].l = sp[-1].l; \
    sp[0].l = sp[-2].l; \
    sp[-1].l = sp[-3].l; \
    sp[-2].l = sp[-4].l; \
    sp[-3].l = sp[1].l; \
    sp[-4].l = sp[0].l; \
    sp += 2

#define INST_DUP2_X2_2() \
    sp[0].l = sp[-1].l; \
    sp[-1].l = sp[-2].l; \
    sp[-2].l = sp[-3].l; \
    sp[-3].l = sp[0].l; \
    sp++

#define INST_DUP2_X2_3() \
    sp[1].l = sp[-1].l; \
    sp[0].l = sp[-2].l; \
    sp[-1].l = sp[-3].l; \
    sp[-2].l = sp[1].l; \
    sp[-3].l = sp[0].l; \
    sp += 2

#define INST_DUP2_X2_4() \
    sp[0].l = sp[-1].l; \
    sp[-1].l = sp[-2].l; \
    sp[-2].l = sp[0].l; \
    sp++

#define INST_F2D() \
    sp[-1].d = sp[-1].f

#define INST_F2I() \
    sp[-1].i = (jint) sp[-1].f

#define INST_F2L() \
    sp[-1].l = (jlong) sp[-1].f

#define INST_FADD() \
    sp--; \
    sp[-1].f = sp[-1].f + sp[0].f

#define INST_FALOAD() \
    sp--; \
    sp[-1].f = ARRAY_ACCESS(sp[-1].o, jfloat, sp[0].i)

#define INST_FCMPG() \
    sp--; \
    sp[-1].i = floatCompare(sp[-1].f, sp[0].f, 1)

#define INST_FCMPL() \
    sp--; \
    sp[-1].i = floatCompare(sp[-1].f, sp[0].f, -1)

#define INST_FCONST(value) \
    (sp++)->f = value

#define INST_FDIV() \
    sp--; \
    sp[-1].f = sp[-1].f / sp[0].f

#define INST_FLOAD(local) \
    (sp++)->f = frame[local].f

#define INST_FMUL() \
    sp--; \
    sp[-1].f = sp[-1].f * sp[0].f

#define INST_FNEG() \
    sp[-1].f = -sp[-1].f

#define INST_FREM() \
    sp--; \
    sp[-1].f = fmodf(sp[-1].f, sp[0].f)

#define INST_FSTORE(local) \
    frame[local].f = (--sp)->f

#define INST_FSUB() \
    sp--; \
    sp[-1].f = sp[-1].f - sp[0].f

#define INST_I2B() \
    sp[-1].i = (jint) (jbyte) (sp[-1].i & 0xFF)

#define INST_I2C() \
    sp[-1].i = sp[-1].i & 0xFFFF

#define INST_I2D() \
    sp[-1].d = (jdouble) sp[-1].i

#define INST_I2F() \
    sp[-1].f = (jfloat) sp[-1].i

#define INST_I2L() \
    sp[-1].l = (jlong) sp[-1].i

#define INST_I2S() \
    sp[-1].i = (jint) (jshort) (sp[-1].i & 0xFFFF)

#define INST_IADD() \
    sp--; \
    sp[-1].i = sp[-1].i + sp[0].i

#define INST_IALOAD() \
    sp--; \
    sp[-1].i = ARRAY_ACCESS(sp[-1].o, jint, sp[0].i)

#define INST_IAND() \
    sp--; \
    sp[-1].i = sp[-1].i & sp[0].i

#define INST_IASTORE() \
    ARRAY_ACCESS(sp[-3].o, jint, sp[-2].i) = sp[-1].i; \
    sp -= 3

#define INST_ICONST(value) \
    (sp++)->i = value

#define INST_IDIV() \
    sp--; \
    if (sp[0].i == 0) throwDivisionByZero(ctx); \
    sp[-1].i = sp[-1].i / sp[0].i

#define INST_IINC(local, amount) \
    frame[local].i += amount

#define INST_ILOAD(local) \
    (sp++)->i = frame[local].i

#define INST_IMUL() \
    sp--; \
    sp[-1].i = sp[-1].i * sp[0].i

#define INST_INEG() \
    sp[-1].i = -sp[-1].i

#define INST_INSTANCEOF(type) \
    sp[-1].i = isInstance(ctx, sp[-1].o, type)

#define INST_IOR() \
    sp--; \
    sp[-1].i = sp[-1].i | sp[0].i

#define INST_IREM() \
    sp--; \
    if (sp[0].i == 0) throwDivisionByZero(ctx); \
    sp[-1].i = sp[-1].i % sp[0].i

#define INST_ISHL() \
    sp--; \
    sp[-1].i = sp[-1].i << sp[0].i

#define INST_ISHR() \
    sp--; \
    sp[-1].i = sp[-1].i >> sp[0].i

#define INST_ISTORE(local) \
    frame[local].i = (--sp)->i

#define INST_ISUB() \
    sp--; \
    sp[-1].i = sp[-1].i - sp[0].i

#define INST_IUSHR() \
    sp--; \
    sp[-1].i = *(uint32_t *) &sp[-1].i >> sp[0].i

#define INST_IXOR() \
    sp--; \
    sp[-1].i = sp[-1].i ^ sp[0].i

#define INST_L2D() \
    sp[-1].d = (jdouble) sp[-1].l

#define INST_L2F() \
    sp[-1].f = (jfloat) sp[-1].l

#define INST_L2I() \
    sp[-1].i = (jint) sp[-1].l

#define INST_LADD() \
    sp--; \
    sp[-1].l = sp[-1].l + sp[0].l

#define INST_LALOAD() \
    sp--; \
    sp[-1].l = ARRAY_ACCESS(sp[-1].o, jlong, sp[0].i)

#define INST_LAND() \
    sp--; \
    sp[-1].l = sp[-1].l & sp[0].l

#define INST_LASTORE() \
    ARRAY_ACCESS(sp[-3].o, jlong, sp[-2].i) = sp[-1].l; \
    sp -= 3

#define INST_LCMP() \
    sp--; \
    sp[-1].i = longCompare(sp[-1].l, sp[0].l)

#define INST_LCONST(value) \
    (sp++)->l = value

#define INST_LDIV() \
    sp--; \
    if (sp[0].l == 0) throwDivisionByZero(ctx); \
    sp[-1].l = sp[-1].l / sp[0].l

#define INST_LLOAD(local) \
    (sp++)->l = frame[local].l

#define INST_LMUL() \
    sp--; \
    sp[-1].l = sp[-1].l * sp[0].l

#define INST_LNEG() \
    sp[-1].l = -sp[-1].l

#define INST_LOR() \
    sp--; \
    sp[-1].l = sp[-1].l | sp[0].l

#define INST_LREM() \
    sp--; \
    if (sp[0].l == 0) throwDivisionByZero(ctx); \
    sp[-1].l = sp[-1].l % sp[0].l

#define INST_LSHL() \
    sp--; \
    sp[-1].l = sp[-1].l << sp[0].i

#define INST_LSHR() \
    sp--; \
    sp[-1].l = sp[-1].l >> sp[0].i

#define INST_LSTORE(local) \
    frame[local].l = (--sp)->l

#define INST_LSUB() \
    sp--; \
    sp[-1].l = sp[-1].l - sp[0].l

#define INST_LUSHR() \
    sp--; \
    sp[-1].l = *(uint64_t *) &sp[-1].l >> sp[0].i

#define INST_LXOR() \
    sp--; \
    sp[-1].l = sp[-1].l ^ sp[0].l

#define INST_MONITORENTER() \
    monitorEnter(ctx, POP_OBJECT())

#define INST_MONITOREXIT() \
    monitorExit(ctx, POP_OBJECT())

#define INST_MULTIANEWARRAY(type, dimensions) \
    instMultiANewArray(ctx, sp, type, dimensions)

#define INST_NEWARRAY(type) \
    sp[-1].o = (jobject) createArray(ctx, type, sp[-1].i)

#define INST_NOP() SEMICOLON_RECEPTOR

#define INST_POP() \
    sp--

#define INST_POP2_1() \
    sp -= 2

#define INST_POP2_2() \
    sp--

#define INST_SALOAD() \
    sp--; \
    sp[-1].i = ARRAY_ACCESS(sp[-1].o, jshort, sp[0].i)

#define INST_SASTORE() \
    ARRAY_ACCESS(sp[-3].o, jshort, sp[-2].i) = sp[-1].i; \
    sp -= 3

#define INST_SIPUSH(value) \
    (sp++)->i = value

#define INST_SWAP() { \
    jlong temp = sp[-1].l; \
    sp[-1].l = sp[-2].l; \
    sp[-2].l = temp.l; \
    } SEMICOLON_RECEPTOR

#define INST_IRETURN() \
    return (--sp)->i

#define INST_LRETURN() \
    return (--sp)->l

#define INST_FRETURN() \
    return (--sp)->f

#define INST_DRETURN() \
    return (--sp)->d

#define INST_ARETURN() \
    return POP_OBJECT()

#define INST_RETURN() \
    return

#ifdef __cplusplus
}
#endif

#ifdef __cplusplus

#include <utility>
#include <vector>
#include <map>
#include <cmath>
#include <mutex>
#include <condition_variable>
#include <bit>
#include <functional>

using std::bit_cast;

template <typename B, typename E>
inline void tryCatch(jframe frameRef, B block, jclass clazz, E except) requires std::invocable<B> and std::invocable<E, jobject> {
    if (setjmp(*pushExceptionFrame(frameRef, clazz))) {
        auto ex = popExceptionFrame(frameRef);
        ex->gcMark = GC_MARK_NATIVE; // Doesn't cause a leak if exception is rethrow in except block, since it will be re-caught and fixed there
        except(ex);
        ex->gcMark = GC_MARK_START;
        return;
    }
    block();
    popExceptionFrame(frameRef);
}

template <typename F, int I, typename ...P>
inline auto invokeVirtual(jcontext ctx, jobject obj, P... params) {
    return ((F) ((void **) NULL_CHECK(obj)->vtable)[I])(ctx, obj, params...);
}

template <typename F, auto C, int I, typename ...P>
inline auto invokeInterface(jcontext ctx, jobject obj, P... params) {
    return ((F) resolveInterfaceMethod(ctx, C, I, obj))(ctx, obj, params...);
}

template <jclass T, auto C, typename ...P>
inline jobject constructObject(jcontext ctx, P... params) {
    auto object = gcAllocNative(ctx, T);
    C(ctx, object, params...);
    object->gcMark = GC_MARK_START;
    return object;
}

template <jclass T, auto C, typename ...P>
inline void constructAndThrow(jcontext ctx, P... params) {
    throwException(ctx, constructObject<T, C, P...>(ctx, params...));
}

template <jclass T, auto C>
inline void constructAndThrowMsg(jcontext ctx, const char *message) {
    throwException(ctx, constructObject<T, C>(ctx, (jobject) stringFromNative(ctx, message)));
}

template <jclass T, auto C>
inline void constructAndThrowMsg(jcontext ctx, jobject ex, const char *message) {
    throwException(ctx, constructObject<T, C>(ctx, ex, (jobject) stringFromNative(ctx, message)));
}

jarray createMultiArray(jcontext ctx, jclass type, const std::vector<int> &dimensions);

inline StringLiteral operator "" _j(const char8_t *string, size_t length) {
    return {(const char *) string, (int) length};
}

template<typename T>
inline jint floatingCompare(T t1, T t2, jint nanVal) {
    if (std::isnan(t1) or std::isnan(t2))
        return nanVal;
    if (t1 > t2)
        return 1;
    if (t1 < t2)
        return -1;
    return 0;
}

typedef struct ObjectMonitor {
    std::recursive_mutex lock;
    jcontext owner;
    std::condition_variable condition;
    std::mutex conditionMutex;
} ObjectMonitor;

struct ExceptionFrame {
    jclass type;
    jmp_buf landingPad;
};

struct StackFrame {
    int size{}; // Size of frame data (Number of jlong/StackEntry words)
    const jtype *frame{}; // Pointer to frame data
    const char *method{}; // Qualified method name
    jobject monitor{}; // The monitor only for synchronized methods, if used
    std::vector<ExceptionFrame> exceptionFrames; // Stack of current exception frames in the method
    int lineNumber{}; // Current line number
    jobject exception{}; // The currently thrown exception // Todo: Why isn't this part of the ExceptionFrame, again?
};

struct Context {
    jthread thread{};
    std::thread *nativeThread; // Null for main thread
    StackFrame frames[MAX_STACK_DEPTH];
    int stackDepth{};
    bool suspended{}; // Considered at safepoint, must check for suspendVM flag when un-suspending
    std::recursive_mutex lock; // Lock on changing the stack or blocking monitor
    jobject blockedBy; // Object monitor blocking the current thread, or null
};

// Only use during a long-running native block where all objects are secured like at a safepoint (Not exception safe)
// Todo: Capture a list of objects to preserve
class SafepointGuard {
public:
    explicit inline SafepointGuard(jcontext ctx) : ctx(ctx) {
        ctx->suspended = true;
    }

    inline ~SafepointGuard() {
        ctx->suspended = false;
        if (suspendVM) // Native functions that suspend the thread must poll for safepoint upon completion
            safepointSuspend(ctx);
    }

private:
    jcontext ctx;
};

#endif

#endif