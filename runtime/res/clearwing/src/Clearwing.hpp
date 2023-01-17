#pragma once

#include "Config.hpp"
#include <memory>
#include <utility>
#include <variant>
#include <vector>
#include <string>
#include <map>
#include <stack>
#include <condition_variable>
#include <cmath>

using std::make_shared;
using std::shared_ptr;
using std::weak_ptr;
using std::dynamic_pointer_cast;
using std::get;
using std::bit_cast;
using std::vector;
using std::map;
using std::stack;
using std::mutex;
using std::condition_variable;
using std::unique_lock;

#define object_cast dynamic_pointer_cast

namespace java::lang {
    class Object;
    class Class;
    class String;
    class Thread;
}
namespace vm {
    class Array;
}

typedef int8_t jbyte;
typedef uint16_t jchar;
typedef int16_t jshort;
typedef int32_t jint;
typedef int64_t jlong;
typedef float jfloat;
typedef double jdouble;
typedef bool jbool;
typedef std::shared_ptr<java::lang::Object> jobject;
typedef std::shared_ptr<java::lang::Class> jclass;
typedef std::shared_ptr<java::lang::String> jstring;
typedef std::shared_ptr<java::lang::Thread> jthread;
typedef std::shared_ptr<vm::Array> jarray;

typedef std::variant<jint, jfloat, jdouble, jlong, jobject> DataVariant;

typedef jobject (*reflect_method_ptr)(const jobject &, const jarray &);
typedef void (*field_setter_ptr)(const jobject &, const jobject &);
typedef jobject (*field_getter_ptr)(const jobject &);
typedef jobject (*new_object_ptr)();
typedef void (*static_init_ptr)();

struct FieldData {
    std::string name;
    int access;
    std::string type;
    std::string signature;
    field_getter_ptr getterFunc;
    field_setter_ptr setterFunc;
};

struct MethodData {
    std::string name;
    int access;
    vector<std::string> paramTypes;
    std::string returnType;
    reflect_method_ptr funcPtr;
};

struct ClassData {
    std::string name;
    int access;
    std::string parent;
    vector<std::string> interfaces;
    vector<FieldData> fields;
    vector<MethodData> methods;
    new_object_ptr newObjectFunc; // Creates new instance of type
    static_init_ptr staticInitializer;
    bool anonymous;
    bool synthetic;
    bool primitive;
    int size;
    int arrayDimensions;
    std::string arrayItemType;
};

struct CallStackEntry {
    inline CallStackEntry(const std::string &className, const char *methodName, int line) : className(className), methodName(methodName), line(line) {}
    const std::string &className;
    const char *methodName;
    int line;
};

struct ThreadData {
    vector<CallStackEntry> callStack;
    jthread thread;
};

inline bool isDataVariantWide(const DataVariant& value) {
    return std::holds_alternative<jlong>(value) or std::holds_alternative<jdouble>(value);
}

jstring operator "" _j(const char8_t *, size_t);

namespace vm {
    extern std::map<std::string, jclass> *classRegistry;
    extern thread_local ThreadData threadData;

    extern jclass classBoolean;
    extern jclass classByte;
    extern jclass classShort;
    extern jclass classChar;
    extern jclass classInt;
    extern jclass classLong;
    extern jclass classFloat;
    extern jclass classDouble;
    extern jclass classVoid;

    void init();

    jstring createString(const char *text, const char *end = nullptr);

    const char *getNativeString(const jstring &string);

    jclass &registerClass(const ClassData *classData);

    jclass &getClass(const char *name);

    jclass &getArrayClass(const jclass &clazz, int dimensions);

    /**
     * Create a new object with non-array parameters
     */
    template<class T, class ...P>
    inline shared_ptr<T> newObject(P... params) {
        auto clazz = make_shared<T>();
        object_cast<T>(clazz)->init(params...);
        return clazz;
    }

    jarray newArray(const jclass &type, int size);

    jarray newMultiArray(const jclass &baseType, const vector<int> &dimensions);

    template<typename Base>
    inline bool instanceof(const jobject &object) {
        return object_cast<Base>(object) != nullptr;
    }

    inline void setLineNumber(int line) {
#if USE_LINE_NUMBERS
        threadData.callStack.back().line = line;
#endif
    }

    inline void push(DataVariant* &sp, DataVariant value) {
        *(sp++) = std::move(value);
    }

    template <typename T>
    inline T &pop(DataVariant* &sp) {
        return get<T>(*(--sp));
    }

    /**
     * Pop "slots" (No special case for wide variants)
     */
    inline void pop(DataVariant* &sp, int count) {
        sp -= count;
    }

    extern std::string getSystemProperty(const std::string &key);

    extern std::string getOSLanguage();
}

class SynchronizationObserver {
public:
    explicit SynchronizationObserver(jobject monitor);

    ~SynchronizationObserver();

    jobject monitor;
};

class CallStackObserver {
public:
#if USE_STACK_COOKIES
    CallStackObserver(const std::string &className, const char *methodName, DataVariant *cookie = nullptr);
    static constexpr jlong COOKIE = 0x6969696969696969ll;
    DataVariant *cookie;
#else
    CallStackObserver(const std::string &className, const char *methodName);
#endif

    ~CallStackObserver() noexcept(false);
};
