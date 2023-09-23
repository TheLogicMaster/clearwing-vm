#include "java/io/File.h"
#include "java/lang/String.h"

#include <iostream>
#include <sys/stat.h>
#include <fcntl.h>
#include <fstream>
#include <filesystem>

namespace fs = std::filesystem;

extern "C" {

jbool SM_java_io_File_createFile_java_lang_String_R_boolean(jcontext ctx, jobject path) {
    auto nativePath = stringToNative(ctx, (jstring) NULL_CHECK(path));
    std::ifstream test(nativePath);
    if (test.good())
        return false;
    test.close();
    std::ofstream file(nativePath);
    return file.good();
}

jbool M_java_io_File_delete_R_boolean(jcontext ctx, jobject self) {
    return !std::remove(stringToNative(ctx, (jstring) ((java_io_File *) NULL_CHECK(self))->F_path));
}

jbool M_java_io_File_exists_R_boolean(jcontext ctx, jobject self) {
    struct stat s{};
    return !stat(stringToNative(ctx, (jstring) ((java_io_File *) NULL_CHECK(self))->F_path), &s);
}

jbool M_java_io_File_isDirectory_R_boolean(jcontext ctx, jobject self) {
    struct stat s{};
    if (stat(stringToNative(ctx, (jstring) ((java_io_File *) NULL_CHECK(self))->F_path), &s))
        return false;
#ifdef __WINRT__
    return (S_IFDIR & s.st_mode) != 0;
#else
    return S_ISDIR(s.st_mode);
#endif
}

jlong M_java_io_File_lastModified_R_long(jcontext ctx, jobject self) {
    struct stat s{};
    if (stat(stringToNative(ctx, (jstring) ((java_io_File *) NULL_CHECK(self))->F_path), &s))
        return 0;
#if defined(__WIN32__) || defined(__WINRT__)
    return s.st_mtime;
#else
    return s.st_mtim.tv_sec;
#endif
}

jlong M_java_io_File_length_R_long(jcontext ctx, jobject self) {
    struct stat s{};
    if (stat(stringToNative(ctx, (jstring) ((java_io_File *) NULL_CHECK(self))->F_path), &s))
        return 0;
    return s.st_size;
}

jobject M_java_io_File_list_R_Array1_java_lang_String(jcontext ctx, jobject self) {
    std::vector<jobject> collected;
    auto path = stringToNative(ctx, (jstring) ((java_io_File *) NULL_CHECK(self))->F_path);
    if (!fs::is_directory(path))
        return nullptr;
    for (const auto & entry : fs::directory_iterator(path)) {
#if defined(__WIN32__) || defined(__WINRT__)
        std::wstring wstring(entry.path().filename().c_str());
        std::string filePath(wstring.begin(), wstring.end());
#else
        auto filePath = entry.path().filename().string();
#endif
        collected.emplace_back((jobject) stringFromNative(ctx, filePath.c_str()));
    }
    auto array = createArray(ctx, &class_java_lang_String, (int)collected.size());
    for (int i = 0; i < (int)collected.size(); i++)
        ((jobject *)array->data)[i] = collected[i];
    return (jobject) array;
}

jbool M_java_io_File_mkdir_R_boolean(jcontext ctx, jobject self) {
#if defined(__WINRT__)
    std::string path(stringToNative(ctx, (jstring) ((java_io_File *) NULL_CHECK(self))->F_path));
    std::wstring wpath(path.begin(), path.end());
    return !_wmkdir(wpath.c_str());
#elif defined(__WIN32__)
    return !mkdir(stringToNative(ctx, (jstring) ((java_io_File *) NULL_CHECK(self))->F_path));
#else
    return !mkdir(stringToNative(ctx, (jstring) ((java_io_File *) NULL_CHECK(self))->F_path), 0777);
#endif
}

}
