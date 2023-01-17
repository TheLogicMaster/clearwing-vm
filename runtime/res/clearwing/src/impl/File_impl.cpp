#include "Clearwing.hpp"
#include "Array.hpp"
#include "Utils.hpp"
#include <java/io/File.hpp>

#include <iostream>
#include <sys/stat.h>
#include <fcntl.h>
#include <fstream>
#include <filesystem>

namespace fs = std::filesystem;

jbool java::io::File::SM_createFile_R_boolean(const jstring &path) {
    auto nativePath = vm::getNativeString(path);
    std::ifstream test(nativePath);
    if (test.good())
        return false;
    test.close();
    std::ofstream file(nativePath);
    return file.good();
}

jbool java::io::File::M_delete_R_boolean() {
    return !std::remove(vm::getNativeString(F_path));
}

jbool java::io::File::M_exists_R_boolean() {
    struct stat s{};
    return !stat(vm::getNativeString(F_path), &s);
}

jbool java::io::File::M_isDirectory_R_boolean() {
    struct stat s{};
    if (stat(vm::getNativeString(F_path), &s))
        return false;
#ifdef __WINRT__
    return (S_IFDIR & s.st_mode) != 0;
#else
    return S_ISDIR(s.st_mode);
#endif
}

jlong java::io::File::M_lastModified_R_long() {
    struct stat s{};
    if (stat(vm::getNativeString(F_path), &s))
        return 0;
#if defined(__WIN32__) || defined(__WINRT__)
    return s.st_mtime;
#else
    return s.st_mtim.tv_sec;
#endif
}

jlong java::io::File::M_length_R_long() {
    struct stat s{};
    if (stat(vm::getNativeString(F_path), &s))
        return 0;
    return s.st_size;
}

jarray java::io::File::M_list_R_Array1_java_lang_String() {
    std::vector<jobject> collected;
    auto path = vm::getNativeString(F_path);
    if (!fs::is_directory(path))
        return nullptr;
    for (const auto & entry : fs::directory_iterator(path)) {
#if defined(__WIN32__) || defined(__WINRT__)
        std::wstring wstring(entry.path().filename().c_str());
        std::string filePath(wstring.begin(), wstring.end());
#else
        auto filePath = entry.path().filename().string();
#endif
        collected.emplace_back(vm::createString(filePath.c_str()));
    }
    auto array = vm::newArray(vm::getClass("java/lang/String"), (int)collected.size());
    for (int i = 0; i < (int)collected.size(); i++)
        ((jobject *)array->data)[i] = collected[i];
    return array;
}

jbool java::io::File::M_mkdir_R_boolean() {
#if defined(__WINRT__)
    std::string path(vm::getNativeString(F_path));
    std::wstring wpath(path.begin(), path.end());
    return !_wmkdir(wpath.c_str());
#elif defined(__WIN32__)
    return !mkdir(vm::getNativeString(F_path));
#else
    return !mkdir(vm::getNativeString(F_path), 0777);
#endif
}
