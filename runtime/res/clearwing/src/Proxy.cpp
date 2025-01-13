#include "java/lang/reflect/Proxy.h"
#include "java/lang/reflect/InvocationHandler.h"
#include "java/lang/reflect/Method.h"
#include "java/lang/Class.h"
#include "java/lang/Throwable.h"

#include <mutex>
#include <map>
#include <set>
#include <vector>
#include <string>
#include <algorithm>

#include <asmjit/x86.h>
#include <asmjit/a64.h>

using namespace asmjit;

struct ProxyClass {
    jclass cls{};
    std::string name;
    std::vector<jclass> interfaces;
    std::vector<MethodMetadata> methods;
    std::vector<VtableEntry> vtableEntries;
    std::vector<void *> vtable;
};

static TypeId mapType(jclass type) {
    if (type == &class_boolean)
        return TypeId::kUInt8;
    if (type == &class_byte)
        return TypeId::kUInt8;
    if (type == &class_short)
        return TypeId::kInt16;
    if (type == &class_char)
        return TypeId::kUInt16;
    if (type == &class_int)
        return TypeId::kInt32;
    if (type == &class_long)
        return TypeId::kInt64;
    if (type == &class_float)
        return TypeId::kFloat32;
    if (type == &class_double)
        return TypeId::kFloat64;
    return TypeId::kUIntPtr;
}

static jobject setByteArg(jcontext ctx, jarray args, int index, jbyte value) {
    return ((jobject *)args->data)[index] = boxByte(ctx, value);
}

static jobject setCharacterArg(jcontext ctx, jarray args, int index, jchar value) {
    return ((jobject *)args->data)[index] = boxCharacter(ctx, value);
}

static jobject addShortArg(jcontext ctx, jarray args, int index, jshort value) {
    return ((jobject *)args->data)[index] = boxShort(ctx, value);
}

static jobject setIntegerArg(jcontext ctx, jarray args, int index, jint value) {
    return ((jobject *)args->data)[index] = boxInteger(ctx, value);
}

static jobject setLongArg(jcontext ctx, jarray args, int index, jlong value) {
    return ((jobject *)args->data)[index] = boxLong(ctx, value);
}

static jobject setFloatArg(jcontext ctx, jarray args, int index, jfloat value) {
    return ((jobject *)args->data)[index] = boxFloat(ctx, value);
}

static jobject setDoubleArg(jcontext ctx, jarray args, int index, jdouble value) {
    return ((jobject *)args->data)[index] = boxDouble(ctx, value);
}

static jobject setBooleanArg(jcontext ctx, jarray args, int index, jbool value) {
    return ((jobject *)args->data)[index] = boxBoolean(ctx, value);
}

static jobject setObjectArg(jcontext ctx, jarray args, int index, jobject value) {
    return ((jobject *)args->data)[index] = value;
}

static void *getSetArgFunc(jclass type) {
    if (type == &class_boolean)
        return (void *)&setBooleanArg;
    if (type == &class_byte)
        return (void *)&setByteArg;
    if (type == &class_short)
        return (void *)&addShortArg;
    if (type == &class_char)
        return (void *)&setCharacterArg;
    if (type == &class_int)
        return (void *)&setIntegerArg;
    if (type == &class_long)
        return (void *)&setLongArg;
    if (type == &class_float)
        return (void *)&setFloatArg;
    if (type == &class_double)
        return (void *)&setDoubleArg;
    return (void *)&setObjectArg;
}

static void *getUnboxFunc(jclass type) {
    if (type == &class_boolean)
        return (void *)&unboxBoolean;
    if (type == &class_byte)
        return (void *)&unboxByte;
    if (type == &class_short)
        return (void *)&unboxShort;
    if (type == &class_char)
        return (void *)&unboxCharacter;
    if (type == &class_int)
        return (void *)&unboxInteger;
    if (type == &class_long)
        return (void *)&unboxLong;
    if (type == &class_float)
        return (void *)&unboxFloat;
    if (type == &class_double)
        return (void *)&unboxDouble;
    return nullptr;
}

static jarray createArgArray(jcontext ctx, int size) {
    return createArrayProtected(ctx, &class_java_lang_Object, size);
}

static jobject invoke(jcontext ctx, jobject self, jarray args, int methodIndex) {
    auto proxy = (java_lang_reflect_Proxy *)NULL_CHECK(self);
    auto method = ((jobject *)((jarray)((jclass)self->clazz)->methods)->data)[methodIndex];
    jobject ret = INVOKE_INTERFACE(java_lang_reflect_InvocationHandler, invoke_java_lang_Object_java_lang_reflect_Method_Array1_java_lang_Object_R_java_lang_Object, (jobject)proxy->F_h, self, method, (jobject)args);
    unprotectObject((jobject)args);
    return ret;
}

static void *createProxyFunc(jclass *proxyArgTypes, int proxyArgCount, jclass retType, int methodIndex) {
#ifdef ASMJIT_NO_JIT
    return nullptr;
#else
    static JitRuntime rt;

    StringLogger logger;
    CodeHolder code;
    code.init(rt.environment(), rt.cpuFeatures());

#if defined(_M_X64) || defined(__x86_64__)
    x86::Compiler cc(&code);
#else
    a64::Compiler cc(&code);
#endif
    
    code.setLogger(&logger);

    FuncSignature proxySig{};
    std::vector<BaseReg> proxyArgs{(size_t)proxyArgCount};
    BaseReg ctx{};
    proxySig.addArgT<jcontext>();
    cc._newReg(&ctx, TypeId::kUIntPtr, "ctx");
    BaseReg self{};
    proxySig.addArgT<jobject>();
    cc._newReg(&self, TypeId::kUIntPtr, "self");
    proxySig.setRet(retType == &class_void ? TypeId::kVoid : mapType(retType));
    for (int i = 0; i < proxyArgCount; i++) {
        TypeId type = mapType(proxyArgTypes[i]);
        cc._newRegFmt(&proxyArgs[i], type, "arg", i);
        proxySig.addArg(type);
    }
    FuncNode* func = cc.addFunc(proxySig);
    func->setArg(0, ctx);
    func->setArg(1, self);
    for (int i = 0; i < (int)proxyArgs.size(); i++)
        func->setArg(2 + i, proxyArgs[i]);

    BaseReg args{};
    cc._newReg(&args, TypeId::kUIntPtr, "args");
    InvokeNode* createArgsInvokeNode;
    cc.invoke(&createArgsInvokeNode, imm((void*)createArgArray), FuncSignature::build<jarray, jcontext, int>());
    createArgsInvokeNode->setArg(0, ctx);
    createArgsInvokeNode->setArg(1, imm((int)proxyArgs.size()));
    createArgsInvokeNode->setRet(0, args);

    std::vector<BaseReg> invokeArgs{proxyArgs.size()};
    for (int i = 0; i < proxyArgCount; i++) {
        FuncSignature boxSig{};
        boxSig.addArgT<jcontext>();
        boxSig.addArgT<jarray>();
        boxSig.addArgT<int>();
        boxSig.addArg(mapType(proxyArgTypes[i]));
        boxSig.setRetT<jobject>();
        InvokeNode* setArgInvokeNode;
        cc.invoke(&setArgInvokeNode, imm(getSetArgFunc(proxyArgTypes[i])), boxSig);
        setArgInvokeNode->setArg(0, ctx);
        setArgInvokeNode->setArg(1, args);
        setArgInvokeNode->setArg(2, imm(i));
        setArgInvokeNode->setArg(3, proxyArgs[i]);
    }

    BaseReg boxedRet{};
    cc._newReg(&boxedRet, TypeId::kUIntPtr, "boxedRet");
    InvokeNode* invokeNode;
    cc.invoke(&invokeNode, imm((void*)invoke), FuncSignature::build<jobject, jcontext, jobject, jarray, int>());
    invokeNode->setArg(0, ctx);
    invokeNode->setArg(1, self);
    invokeNode->setArg(2, args);
    invokeNode->setArg(3, imm(methodIndex));
    invokeNode->setRet(0, boxedRet);

    if (retType == &class_void) {
        cc.ret();
    } else {
        if (void *boxFunc = getUnboxFunc(retType)) {
            BaseReg retVal{};
            cc._newReg(&retVal, mapType(retType), "ret");
            FuncSignature unboxSig{};
            unboxSig.addArgT<jcontext>();
            unboxSig.addArgT<jobject>();
            unboxSig.setRet(mapType(retType));
            InvokeNode* unboxInvokeNode;
            cc.invoke(&unboxInvokeNode, imm(boxFunc), unboxSig);
            unboxInvokeNode->setArg(0, ctx);
            unboxInvokeNode->setArg(1, boxedRet);
            unboxInvokeNode->setRet(0, retVal);
            cc.ret(retVal);
        } else {
            cc.ret(boxedRet);
        }
    }

    cc.endFunc();
    cc.finalize();

    void *fn;
    Error err = rt.add(&fn, &code); // Todo: Error handling
    return fn;
#endif
}

jobject SM_java_lang_reflect_Proxy_getProxyClass_java_lang_ClassLoader_Array1_java_lang_Class_R_java_lang_Class(jcontext ctx, jobject loader, jobject interfaceArray) {
    static std::mutex lock;
    static std::map<std::set<jclass>, ProxyClass*> proxyClasses;

    auto interfaces = (jarray)interfaceArray;
    std::set<jclass> interfaceSet;
    for (int i = 0; i < interfaces->length; i++) {
        auto cls = ((jclass *)interfaces->data)[i];
        if (cls->access & 512) // Ensure interface
            interfaceSet.insert(cls);
    }

    lock.lock();
    if (auto it = proxyClasses.find(interfaceSet); it != proxyClasses.end()) {
        lock.unlock();
        return (jobject)it->second->cls;
    }

    auto proxy = new ProxyClass{
        .name = "Proxy" + std::to_string(proxyClasses.size())
    };

    tryCatchFinally(ctx, "getProxyClass", [&] {
        auto processMethod = [&](java_lang_reflect_Method *method) {
            int index = (int)proxy->methods.size();
            auto name = stringToNative(ctx, (jstring)method->F_name);
            auto desc = stringToNative(ctx, (jstring)method->F_desc);
            std::string nameStr{name};
            std::string descStr{desc};
            if (std::find_if(proxy->methods.begin(), proxy->methods.end(), [&](const MethodMetadata &m) {
                return nameStr == m.name && descStr == m.desc;
            }) != proxy->methods.end())
                return;
            M_java_lang_reflect_Method_ensureSignatureInitialized(ctx, (jobject)method);
            proxy->methods.emplace_back(name, index, desc, method->F_modifiers);
            proxy->vtableEntries.emplace_back(name, desc);
            auto params = (jarray)method->F_parameterTypes;
            void *func = createProxyFunc((jclass *)params->data, params->length, (jclass)method->F_returnType, index);
            if (!func)
                throwRuntimeException(ctx, "Failed to create function for proxy class");
            proxy->vtable.push_back(func);
        };

        M_java_lang_Class_ensureInitialized(ctx, (jobject)&class_java_lang_Object);
        auto objectMethods = jarray(class_java_lang_Object.methods);
        for (int i = 0; i < objectMethods->length; i++)
            processMethod(((java_lang_reflect_Method **)objectMethods->data)[i]);

        std::function<void(jclass)> processInterface;

        processInterface = [&](jclass interface) {
            M_java_lang_Class_ensureInitialized(ctx, (jobject)interface);
            for (int i = 0; i < interface->interfaceCount; i++)
                processInterface(((jclass *)interface->nativeInterfaces)[i]);
            auto methods = jarray(interface->methods);
            for (int i = 0; i < methods->length; i++) {
                const auto &method = ((java_lang_reflect_Method **)methods->data)[i];
                if (method->F_modifiers & 0x8) // Ignore static methods
                    continue;
                processMethod(method);
            }
        };

        for (jclass interface : interfaceSet) {
            proxy->interfaces.push_back(interface);
            processInterface(interface);
        }

        proxy->cls = new Class {
            .nativeName = (intptr_t) proxy->name.c_str(),
            .parentClass = (intptr_t) &class_java_lang_reflect_Proxy,
            .size = sizeof(java_lang_reflect_Proxy),
            .classVtable = (intptr_t) proxy->vtable.data(),
            .staticInitializer = (intptr_t) clinit_java_lang_reflect_Proxy,
            .annotationInitializer = (intptr_t) nullptr,
            .markFunction = (intptr_t) mark_java_lang_reflect_Proxy,
            .primitive = false,
            .arrayDimensions = 0,
            .componentClass = (intptr_t) nullptr,
            .outerClass = (intptr_t) nullptr,
            .innerClassCount = 0,
            .nativeInnerClasses = (intptr_t) nullptr,
            .access = 0x400,
            .interfaceCount = (int)proxy->interfaces.size(),
            .nativeInterfaces = (intptr_t) proxy->interfaces.data(),
            .fieldCount = 0,
            .nativeFields = (intptr_t) nullptr,
            .methodCount = (int) proxy->methods.size(),
            .nativeMethods = (intptr_t) proxy->methods.data(),
            .vtableSize = (int)proxy->vtable.size(),
            .vtableEntries = (intptr_t) proxy->vtableEntries.data(),
            .anonymous = false,
            .synthetic = false,
        };

        proxyClasses[interfaceSet] = proxy;
        registerClass(proxy->cls);
    }, &class_java_lang_Throwable, [&](jobject ex) {
        delete proxy;
        throwException(ctx, ex);
    }, [&] {
        lock.unlock();
    });

    return (jobject)proxy->cls;
}
