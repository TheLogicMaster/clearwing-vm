#include <iostream>
#include <sys/stat.h>
#include <fcntl.h>
#include <csignal>
#include <fstream>
#include <filesystem>
#include <vector>

namespace fs = std::filesystem;

extern "C" {
#include "cn1_globals.h"
#include "java_nio_Buffer.h"
#include "java_nio_ByteBuffer.h"
#include "java_nio_FloatBuffer.h"
#include "java_nio_IntBuffer.h"
#include "java_io_File.h"
#include "java_io_FileNotFoundException.h"
#include "java_io_FileInputStream.h"
#include "java_io_FileOutputStream.h"
#include "java_lang_String.h"

JAVA_LONG com_thelogicmaster_clearwing_NativeUtils_getLong___long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG address) {
    return *(JAVA_ARRAY_LONG *) address;
}

JAVA_VOID com_thelogicmaster_clearwing_NativeUtils_putLong___long_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG address, JAVA_LONG value) {
    *(JAVA_ARRAY_LONG *) address = value;
}

JAVA_INT com_thelogicmaster_clearwing_NativeUtils_getInt___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG address) {
    return *(JAVA_ARRAY_INT *) address;
}

JAVA_VOID com_thelogicmaster_clearwing_NativeUtils_putInt___long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG address, JAVA_INT value) {
    *(JAVA_ARRAY_INT *) address = value;
}

JAVA_SHORT com_thelogicmaster_clearwing_NativeUtils_getShort___long_R_short(CODENAME_ONE_THREAD_STATE, JAVA_LONG address) {
    return *(JAVA_ARRAY_SHORT *) address;
}

JAVA_VOID com_thelogicmaster_clearwing_NativeUtils_putShort___long_short(CODENAME_ONE_THREAD_STATE, JAVA_LONG address, JAVA_SHORT value) {
    *(JAVA_ARRAY_SHORT *) address = (JAVA_ARRAY_SHORT) value;
}

JAVA_CHAR com_thelogicmaster_clearwing_NativeUtils_getChar___long_R_char(CODENAME_ONE_THREAD_STATE, JAVA_LONG address) {
    return *(JAVA_ARRAY_CHAR *) address;
}

JAVA_VOID com_thelogicmaster_clearwing_NativeUtils_putChar___long_char(CODENAME_ONE_THREAD_STATE, JAVA_LONG address, JAVA_CHAR value) {
    *(JAVA_ARRAY_CHAR *) address = value;
}

JAVA_BYTE com_thelogicmaster_clearwing_NativeUtils_getByte___long_R_byte(CODENAME_ONE_THREAD_STATE, JAVA_LONG address) {
    return *(JAVA_ARRAY_BYTE *) address;
}

JAVA_VOID com_thelogicmaster_clearwing_NativeUtils_putByte___long_byte(CODENAME_ONE_THREAD_STATE, JAVA_LONG address, JAVA_BYTE value) {
    *(JAVA_ARRAY_BYTE *) address = (JAVA_ARRAY_BYTE) value;
}

JAVA_VOID com_thelogicmaster_clearwing_NativeUtils_copyMemory___long_long_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG src, JAVA_LONG dst, JAVA_LONG bytes) {
    memcpy((void *)dst, (void *)src, bytes);
}

JAVA_OBJECT com_thelogicmaster_clearwing_NativeUtils_getPrimitive___java_lang_String_R_java_lang_Class(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT nameObj) {
    std::string name = toNativeString(threadStateData, nameObj);
    if (name == "boolean")
        return (JAVA_OBJECT)&class__JAVA_BOOLEAN;
    else if (name == "byte")
        return (JAVA_OBJECT)&class__JAVA_BYTE;
    else if (name == "short")
        return (JAVA_OBJECT)&class__JAVA_SHORT;
    else if (name == "char")
        return (JAVA_OBJECT)&class__JAVA_CHAR;
    else if (name == "int")
        return (JAVA_OBJECT)&class__JAVA_INT;
    else if (name == "long")
        return (JAVA_OBJECT)&class__JAVA_LONG;
    else if (name == "float")
        return (JAVA_OBJECT)&class__JAVA_FLOAT;
    else if (name == "double")
        return (JAVA_OBJECT)&class__JAVA_DOUBLE;
    else if (name == "void")
        return (JAVA_OBJECT)&class__JAVA_VOID;
    else
        return JAVA_NULL;
}

JAVA_BOOLEAN java_io_File_createFile___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
    auto nativePath = toNativeString(threadStateData, ((obj__java_io_File *) path)->java_io_File_path);
    std::ifstream test(nativePath);
    if (test.good())
        return false;
    test.close();
    std::ofstream file(nativePath);
    return file.good();
}

JAVA_BOOLEAN java_io_File_delete___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject) {
    return !remove(toNativeString(threadStateData, ((obj__java_io_File *) __cn1ThisObject)->java_io_File_path));
}

JAVA_BOOLEAN java_io_File_exists___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    std::ifstream f(toNativeString(threadStateData, ((obj__java_io_File *) __cn1ThisObject)->java_io_File_path));
    return f.good();
}

JAVA_BOOLEAN java_io_File_isDirectory___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    struct stat s;
    stat(toNativeString(threadStateData, ((obj__java_io_File *) __cn1ThisObject)->java_io_File_path), &s);
    return S_ISDIR(s.st_mode);
}

JAVA_LONG java_io_File_lastModified___R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject) {
    struct stat s;
    stat(toNativeString(threadStateData, ((obj__java_io_File *) __cn1ThisObject)->java_io_File_path), &s);
#ifdef _WIN32
    return s.st_mtime;
#else
    return s.st_mtim.tv_sec;
#endif
}

JAVA_LONG java_io_File_length___R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject) {
    struct stat s;
    stat(toNativeString(threadStateData, ((obj__java_io_File *) __cn1ThisObject)->java_io_File_path), &s);
    return s.st_size;
}

JAVA_OBJECT java_io_File_list___R_java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject) {
    std::vector<JAVA_OBJECT> collected;
    auto path = toNativeString(threadStateData, ((obj__java_io_File *) __cn1ThisObject)->java_io_File_path);
    if (!fs::is_directory(path))
        return JAVA_NULL;
    for (const auto & entry : fs::directory_iterator(path)) {
#ifdef _WIN32
        std::wstring wstring(entry.path().c_str());
        std::string string(wstring.begin(), wstring.end());
        auto filePath = string.c_str();
#else
        auto filePath = entry.path().c_str();
#endif
        collected.emplace_back(newStringFromCString(threadStateData, filePath));
    }
    auto array = __NEW_ARRAY_java_lang_String(threadStateData, (int)collected.size());
    for (int i = 0; i < (int)collected.size(); i++)
        ((JAVA_OBJECT *)((JAVA_ARRAY)array)->data)[i] = collected[i];
    return array;
}

JAVA_BOOLEAN java_io_File_mkdir___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject) {
#ifdef _WIN32
    return !mkdir(toNativeString(threadStateData, ((obj__java_io_File *) __cn1ThisObject)->java_io_File_path));
#else
    return !mkdir(toNativeString(threadStateData, ((obj__java_io_File *) __cn1ThisObject)->java_io_File_path), 0777);
#endif
}

void throwIOException(CODENAME_ONE_THREAD_STATE, const char *reason) {
    JAVA_OBJECT exception = __NEW_java_io_IOException(threadStateData);
    java_io_IOException___INIT_____java_lang_String(threadStateData, exception, fromNativeString(threadStateData, reason));
    throwException(threadStateData, exception);
}

JAVA_VOID java_io_FileInputStream_open___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT name) {
    auto f = fopen(toNativeString(threadStateData, name), "rb");
    if (f)
        ((obj__java_io_FileInputStream *) __cn1ThisObject)->java_io_FileInputStream_file = (JAVA_LONG) f;
    else {
        JAVA_OBJECT exception = __NEW_java_io_FileNotFoundException(threadStateData);
        java_io_FileNotFoundException___INIT_____java_lang_String(threadStateData, exception, name);
        throwException(threadStateData, exception);
    }
}

JAVA_INT java_io_FileInputStream_read___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    auto f = (FILE *) ((obj__java_io_FileInputStream *) __cn1ThisObject)->java_io_FileInputStream_file;
    if (f)
        return fgetc(f);
    else
        throwIOException(threadStateData, "File closed");
    return 0;
}

// Todo: Throw EOFException
JAVA_INT java_io_FileInputStream_readBytes___byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT b, JAVA_INT off, JAVA_INT len) {
    auto f = (FILE *) ((obj__java_io_FileInputStream *) __cn1ThisObject)->java_io_FileInputStream_file;
    if (f) {
        auto array = (JAVA_ARRAY) b;
        auto read = fread((JAVA_ARRAY_BYTE *) array->data + off, 1, len, f);
        if (!read)
            return -1;
        return (JAVA_INT) read;
    } else
        throwIOException(threadStateData, "File closed");
    return 0;
}

JAVA_LONG java_io_FileInputStream_skip___long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG n) {
    auto f = (FILE *) ((obj__java_io_FileInputStream *) __cn1ThisObject)->java_io_FileInputStream_file;
    if (!f)
        throwIOException(threadStateData, "File closed");
    auto pos = ftell(f);
    auto result = fseek(f, n, SEEK_CUR);
    if (result < 0)
        throwIOException(threadStateData, nullptr);
    return result - pos;
}

JAVA_INT java_io_FileInputStream_available___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    auto f = (FILE *) ((obj__java_io_FileInputStream *) __cn1ThisObject)->java_io_FileInputStream_file;
    if (!f)
        throwIOException(threadStateData, "File closed");
    auto pos = ftell(f);
    fseek(f, 0, SEEK_END);
    auto len = ftell(f);
    fseek(f, pos, SEEK_SET);
    return (JAVA_INT) (len - pos);
}

JAVA_VOID java_io_FileInputStream_close0__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    auto f = (FILE *) ((obj__java_io_FileInputStream *) __cn1ThisObject)->java_io_FileInputStream_file;
    if (!f)
        throwIOException(threadStateData, "File closed");
    if (fclose(f))
        throwIOException(threadStateData, nullptr);
    ((obj__java_io_FileInputStream *) __cn1ThisObject)->java_io_FileInputStream_file = 0;
}

JAVA_VOID java_io_FileOutputStream_open___java_lang_String_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT name, JAVA_BOOLEAN append) {
    auto f = fopen(toNativeString(threadStateData, name), append ? "wb+" : "wb");
    if (f)
        ((obj__java_io_FileOutputStream *) __cn1ThisObject)->java_io_FileOutputStream_file = (JAVA_LONG) f;
    else {
        JAVA_OBJECT exception = __NEW_java_io_FileNotFoundException(threadStateData);
        java_io_FileNotFoundException___INIT_____java_lang_String(threadStateData, exception, name);
        throwException(threadStateData, exception);
    }
}

JAVA_VOID java_io_FileOutputStream_write___int_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT b, JAVA_BOOLEAN append) {
    auto f = (FILE *) ((obj__java_io_FileOutputStream *) __cn1ThisObject)->java_io_FileOutputStream_file;
    if (!f)
        throwIOException(threadStateData, "File closed");
    const char byte = (char) b;
    if (append)
        fseek(f, 0, SEEK_END);
    auto written = fwrite(&byte, 1, 1, f);
    if (written != 1)
        throwIOException(threadStateData, nullptr);
}

JAVA_VOID java_io_FileOutputStream_writeBytes___byte_1ARRAY_int_int_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT b, JAVA_INT off, JAVA_INT len, JAVA_BOOLEAN append) {
    auto f = (FILE *) ((obj__java_io_FileOutputStream *) __cn1ThisObject)->java_io_FileOutputStream_file;
    if (!f)
        throwIOException(threadStateData, "File closed");
    if (append)
        fseek(f, 0, SEEK_END);
    auto written = (int)fwrite((char *) ((JAVA_ARRAY) b)->data + off, 1, len, f);
    if (written != len)
        throwIOException(threadStateData, nullptr);
}

JAVA_VOID java_io_FileOutputStream_close0__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    auto f = (FILE *) ((obj__java_io_FileOutputStream *) __cn1ThisObject)->java_io_FileOutputStream_file;
    if (!f)
        throwIOException(threadStateData, "File closed");
    if (fclose(f))
        throwIOException(threadStateData, nullptr);
    ((obj__java_io_FileOutputStream *) __cn1ThisObject)->java_io_FileOutputStream_file = 0;
}
}
