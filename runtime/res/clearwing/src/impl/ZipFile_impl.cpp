#include <Clearwing.hpp>
#include <java/util/zip/ZipFile.hpp>
#include <java/util/zip/ZipEntry.hpp>
#include <java/util/ArrayList.hpp>
#include "Array.hpp"
#include "Utils.hpp"

#include <zzip/lib.h>

using java::util::zip::ZipFile;

jlong ZipFile::SM_open_R_long(const jstring &path, const shared_ptr<java::util::ArrayList> &entries) {
    zzip_error_t errorCode;
    ZZIP_DIR *zip = zzip_dir_open(vm::getNativeString(path), &errorCode);
    if (errorCode)
        return -1;
    ZZIP_DIRENT dirent;
    while (zzip_dir_read(zip, &dirent)) {
        auto entry = make_shared<java::util::zip::ZipEntry>();
        entry->F_name = vm::createString(dirent.d_name);
        entry->F_csize = dirent.d_csize;
        entry->F_size = dirent.st_size;
        entry->F_method = dirent.d_compr;
        entries->M_add_R_boolean(entry);
    }
    return (jlong)zip;
}

void ZipFile::SM_close0(jlong handle) {
    zzip_dir_close((ZZIP_DIR *) handle);
}

jlong ZipFile::SM_openEntry_R_long(jlong handle, const jstring &name) {
    return (jlong)zzip_file_open((ZZIP_DIR *)handle, vm::getNativeString(name), 0);
}

jint ZipFile::SM_readEntry_R_int(jlong handle) {
    unsigned char val;
    zzip_ssize_t count = zzip_file_read((ZZIP_FILE *) handle, &val, 1);
    return count > 0 ? val : -1;
}

void ZipFile::SM_closeEntry(jlong handle) {
    zzip_file_close((ZZIP_FILE *) handle);
}
