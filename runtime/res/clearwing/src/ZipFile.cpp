#include "java/util/zip/ZipFile.h"
#include "java/util/zip/ZipEntry.h"
#include "java/util/ArrayList.h"

#include <zzip/lib.h>

extern "C" {

jlong SM_java_util_zip_ZipFile_open_java_lang_String_java_util_ArrayList_R_long(jcontext ctx, jobject path, jobject entries) {
    zzip_error_t errorCode;
    ZZIP_DIR *zip = zzip_dir_open(stringToNative(ctx, (jstring) NULL_CHECK(path)), &errorCode);
    if (errorCode)
        return -1;
    ZZIP_DIRENT dirent;
    while (zzip_dir_read(zip, &dirent)) {
        auto entry = (java_util_zip_ZipEntry *) gcAllocProtected(ctx, &class_java_util_zip_ZipEntry);
        entry->F_name = (jref) stringFromNative(ctx, dirent.d_name); // Stored on protected object
        entry->F_csize = dirent.d_csize;
        entry->F_size = dirent.st_size;
        entry->F_method = dirent.d_compr;
        M_java_util_ArrayList_add_java_lang_Object_R_boolean(ctx, entries, (jobject) entry);
        unprotectObject((jobject)entry);
    }
    return (jlong)zip;
}

void SM_java_util_zip_ZipFile_close0_long(jcontext ctx, jlong handle) {
    zzip_dir_close((ZZIP_DIR *) handle);
}

jlong SM_java_util_zip_ZipFile_openEntry_long_java_lang_String_R_long(jcontext ctx, jlong handle, jobject name) {
    return (jlong) zzip_file_open((ZZIP_DIR *)handle, stringToNative(ctx, (jstring) name), 0);
}

jint SM_java_util_zip_ZipFile_readEntry_long_R_int(jcontext ctx, jlong handle) {
    unsigned char val;
    zzip_ssize_t count = zzip_file_read((ZZIP_FILE *) handle, &val, 1);
    return count > 0 ? val : -1;
}

void SM_java_util_zip_ZipFile_closeEntry_long(jcontext ctx, jlong handle) {
    zzip_file_close((ZZIP_FILE *) handle);
}

}
