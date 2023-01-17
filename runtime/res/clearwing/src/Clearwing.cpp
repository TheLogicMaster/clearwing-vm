#include <locale>
#include <codecvt>
#include <cstring>

#include "Clearwing.hpp"
#include <java/lang/String.hpp>
#include <java/lang/ClassNotFoundException.hpp>
#include <java/lang/reflect/Field.hpp>
#include <java/lang/reflect/Method.hpp>
#include <java/lang/reflect/Constructor.hpp>
#include <java/lang/annotation/Annotation.hpp>
#include <functional>
#include "Utils.hpp"

using namespace java::lang;

static std::map<Object *, std::map<int, std::pair<jclass *, ClassData>>> arrayClasses;
static std::map<const char8_t *, jstring> stringPool;
std::map<std::string, jclass> *vm::classRegistry; // Allocated dynamically to ensure initialization order
thread_local ThreadData vm::threadData;

static const ClassData classBooleanData {"Z", 0x400, "", {}, {}, {}, nullptr, nullptr, false, false, true, sizeof(jbool), 0, ""};
static const ClassData classByteData {"B", 0x400, "", {}, {}, {}, nullptr, nullptr, false, false, true, sizeof(jbyte), 0, ""};
static const ClassData classShortData {"S", 0x400, "", {}, {}, {}, nullptr, nullptr, false, false, true, sizeof(jshort), 0, ""};
static const ClassData classCharData {"C", 0x400, "", {}, {}, {}, nullptr, nullptr, false, false, true, sizeof(jchar), 0, ""};
static const ClassData classIntData {"I", 0x400, "", {}, {}, {}, nullptr, nullptr, false, false, true, sizeof(jint), 0, ""};
static const ClassData classLongData {"L", 0x400, "", {}, {}, {}, nullptr, nullptr, false, false, true, sizeof(jlong), 0, ""};
static const ClassData classFloatData {"F", 0x400, "", {}, {}, {}, nullptr, nullptr, false, false, true, sizeof(jfloat), 0, ""};
static const ClassData classDoubleData {"D", 0x400, "", {}, {}, {}, nullptr, nullptr, false, false, true, sizeof(jdouble), 0, ""};
static const ClassData classVoidData {"V", 0x400, "", {}, {}, {}, nullptr, nullptr, false, false, true, 0, 0, ""};

jclass vm::classBoolean = vm::registerClass(&classBooleanData);
jclass vm::classByte = vm::registerClass(&classByteData);
jclass vm::classShort = vm::registerClass(&classShortData);
jclass vm::classChar = vm::registerClass(&classCharData);
jclass vm::classInt = vm::registerClass(&classIntData);
jclass vm::classLong = vm::registerClass(&classLongData);
jclass vm::classFloat = vm::registerClass(&classFloatData);
jclass vm::classDouble = vm::registerClass(&classDoubleData);
jclass vm::classVoid = vm::registerClass(&classVoidData);

void vm::init() {
    // Ensure String/Class dependant array classes are initialized
    vm::getArrayClass(classByte, 1);
    vm::getArrayClass(classChar, 1);
    vm::getArrayClass(java::lang::reflect::Field::CLASS, 1);
    vm::getArrayClass(java::lang::reflect::Method::CLASS, 1);
    vm::getArrayClass(java::lang::reflect::Constructor::CLASS, 1);
    vm::getArrayClass(java::lang::annotation::Annotation::CLASS, 1);
    vm::getArrayClass(java::lang::Class::CLASS, 1);
}

jstring operator "" _j(const char8_t *string, size_t size) {
    auto &entry = stringPool[string];
    if (!entry)
        entry = vm::createString((const char *)string, (const char *)string + size);
    return entry;
}

jstring vm::createString(const char *start, const char *end) {
    if (!end)
        end = start + strlen(start);
    auto encoded = std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t>{}.from_bytes(start, end);
    int encodedLength = (int)encoded.length();
    auto array = make_shared<Array>("C", sizeof(jchar), true, encodedLength);
    memcpy(array->data, encoded.c_str(), encoded.length() * 2);
    auto string = make_shared<String>();
    string->F_value = array;
    string->F_count = encodedLength;
    return string;
}

const char *vm::getNativeString(const jstring &stringObject) {
    auto string = object_cast<java::lang::String>(stringObject);
    if (!string->F_nativeString) {
        auto bytes = object_cast<Array>(string->M_getBytes_R_Array1_byte());
        string->F_nativeString = (jlong)new char[bytes->length + 1]{};
        memcpy((char *)string->F_nativeString, bytes->data, bytes->length);
    }
    return (const char *)string->F_nativeString;
}

jclass &vm::registerClass(const ClassData *classData) {
    if (!classRegistry)
        classRegistry = new std::map<std::string, jclass>;
    auto &name = classData->name;
    auto string = make_shared<String>();
    const_cast<std::string &>(string->name) = "java/lang/String"; // Don't think about this too much
    auto value = make_shared<Array>("C", sizeof(jchar), true, name.length());
    for (int i = 0; i < name.length(); i++)
        ((jchar *)value->data)[i] = (unsigned char)name[i];
    string->F_value = value;
    string->F_count = (int)name.length();
    auto clazz = make_shared<Class>();
    const_cast<std::string &>(clazz->name) = "java/lang/Class"; // Static initialization order issue
    object_cast<Class>(clazz)->F_nativeData = (jlong)classData;
    return (*classRegistry)[name] = clazz;
}

jclass &vm::getClass(const char *name) {
    auto &entry = (*classRegistry)[name];
    if (entry)
        return entry;

    // Try to create array class, if needed
    std::string string(name);
    int dimensions = 0;
    while (string.starts_with("[")) {
        dimensions++;
        string = string.substr(1);
        if (classRegistry->contains(string))
            return vm::getArrayClass((*classRegistry)[string], dimensions);
    }

    vm::throwNew<java::lang::ClassNotFoundException>();
}

jclass &vm::getArrayClass(const jclass &clazzObj, int dimensions) {
    auto clazz = object_cast<Class>(clazzObj);
    auto &map = arrayClasses[clazzObj.get()];
    if (!map.contains(dimensions)) {
        auto data = (ClassData *)clazz->F_nativeData;
        std::string name = data->name;
        for (int i = 0; i < dimensions; i++)
            name.insert(name.begin(), '[');
        if ((*classRegistry)[name])
            map[dimensions].first = &(*classRegistry)[name];
        else {
            for (int i = 1; i < dimensions; i++)
                getArrayClass(clazzObj, i);
            map[dimensions].second = {name, 0x400, "java/lang/Object", {"java/lang/Cloneable"}, {}, {}, nullptr, nullptr, false, false, false, sizeof(jobject), data->arrayDimensions + dimensions, name.substr(1)};
            map[dimensions].first = &registerClass(&map[dimensions].second);
        }
    }
    return *map[dimensions].first;
}

jarray vm::newArray(const shared_ptr<java::lang::Class> &type, int size) {
    return make_shared<Array>(type, vector<int>{size});
}

jarray vm::newMultiArray(const shared_ptr<java::lang::Class> &baseType, const vector<int> &dimensions) {
    return make_shared<Array>(baseType, dimensions);
}

SynchronizationObserver::SynchronizationObserver(jobject monitor) : monitor(std::move(monitor)) {
    this->monitor->acquireMonitor();
}

SynchronizationObserver::~SynchronizationObserver() {
    monitor->releaseMonitor();
}

#if USE_STACK_COOKIES
CallStackObserver::CallStackObserver(const std::string &className, const char *methodName, DataVariant *cookie) : cookie(cookie) {
#else
CallStackObserver::CallStackObserver(const std::string &className, const char *methodName) {
#endif
#if USE_STACK_TRACES
    vm::threadData.callStack.emplace_back(className, methodName, -1);
#endif
#if USE_STACK_COOKIES
    if (cookie)
        *cookie = COOKIE;
#endif
}

CallStackObserver::~CallStackObserver() noexcept(false) {
#if USE_STACK_TRACES
    vm::threadData.callStack.pop_back();
#endif
#if USE_STACK_COOKIES
    if (cookie and (!std::holds_alternative<jlong>(*cookie) or get<jlong>(*cookie) != COOKIE))
        throw std::runtime_error("Method stack overflow (Should not happen)");
#endif
}
