/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.util.zip;

import java.nio.ByteBuffer;

class ZipUtils {
    private static final long WINDOWS_EPOCH_IN_MICROSECONDS = -11644473600000000L;
    public static final long WINDOWS_TIME_NOT_AVAILABLE = Long.MIN_VALUE;
    static final ByteBuffer defaultBuf = ByteBuffer.allocate(0);
    public static final long UPPER_UNIXTIME_BOUND = 2147483647L;
    static final int FILE_ATTRIBUTES_UNIX = 3;
    static final int VERSION_MADE_BY_BASE_UNIX = 768;
    static final long END_MAXLEN = 65557L;
    static final int READBLOCKSZ = 128;

    ZipUtils() {
    }

    public static final int get16(byte[] b, int off) {
        return b[off] & 255 | (b[off + 1] & 255) << 8;
    }

    public static final long get32(byte[] b, int off) {
        return ((long)get16(b, off) | (long)get16(b, off + 2) << 16) & 4294967295L;
    }

    public static final long get64(byte[] b, int off) {
        return get32(b, off) | get32(b, off + 4) << 32;
    }

    public static final int get32S(byte[] b, int off) {
        return get16(b, off) | get16(b, off + 2) << 16;
    }

    static final int CH(byte[] b, int n) {
        return b[n] & 255;
    }

    static final int SH(byte[] b, int n) {
        return b[n] & 255 | (b[n + 1] & 255) << 8;
    }

    static final long LG(byte[] b, int n) {
        return (long)(SH(b, n) | SH(b, n + 2) << 16) & 4294967295L;
    }

    static final long LL(byte[] b, int n) {
        return LG(b, n) | LG(b, n + 4) << 32;
    }

    static final long GETSIG(byte[] b) {
        return LG(b, 0);
    }

    static final long LOCSIG(byte[] b) {
        return LG(b, 0);
    }

    static final int LOCVER(byte[] b) {
        return SH(b, 4);
    }

    static final int LOCFLG(byte[] b) {
        return SH(b, 6);
    }

    static final int LOCHOW(byte[] b) {
        return SH(b, 8);
    }

    static final long LOCTIM(byte[] b) {
        return LG(b, 10);
    }

    static final long LOCCRC(byte[] b) {
        return LG(b, 14);
    }

    static final long LOCSIZ(byte[] b) {
        return LG(b, 18);
    }

    static final long LOCLEN(byte[] b) {
        return LG(b, 22);
    }

    static final int LOCNAM(byte[] b) {
        return SH(b, 26);
    }

    static final int LOCEXT(byte[] b) {
        return SH(b, 28);
    }

    static final long EXTCRC(byte[] b) {
        return LG(b, 4);
    }

    static final long EXTSIZ(byte[] b) {
        return LG(b, 8);
    }

    static final long EXTLEN(byte[] b) {
        return LG(b, 12);
    }

    static final int ENDSUB(byte[] b) {
        return SH(b, 8);
    }

    static final int ENDTOT(byte[] b) {
        return SH(b, 10);
    }

    static final long ENDSIZ(byte[] b) {
        return LG(b, 12);
    }

    static final long ENDOFF(byte[] b) {
        return LG(b, 16);
    }

    static final int ENDCOM(byte[] b) {
        return SH(b, 20);
    }

    static final int ENDCOM(byte[] b, int off) {
        return SH(b, off + 20);
    }

    static final long ZIP64_ENDTOD(byte[] b) {
        return LL(b, 24);
    }

    static final long ZIP64_ENDTOT(byte[] b) {
        return LL(b, 32);
    }

    static final long ZIP64_ENDSIZ(byte[] b) {
        return LL(b, 40);
    }

    static final long ZIP64_ENDOFF(byte[] b) {
        return LL(b, 48);
    }

    static final long ZIP64_LOCOFF(byte[] b) {
        return LL(b, 8);
    }

    static final long CENSIG(byte[] b, int pos) {
        return LG(b, pos + 0);
    }

    static final int CENVEM(byte[] b, int pos) {
        return SH(b, pos + 4);
    }

    static final int CENVEM_FA(byte[] b, int pos) {
        return CH(b, pos + 5);
    }

    static final int CENVER(byte[] b, int pos) {
        return SH(b, pos + 6);
    }

    static final int CENFLG(byte[] b, int pos) {
        return SH(b, pos + 8);
    }

    static final int CENHOW(byte[] b, int pos) {
        return SH(b, pos + 10);
    }

    static final long CENTIM(byte[] b, int pos) {
        return LG(b, pos + 12);
    }

    static final long CENCRC(byte[] b, int pos) {
        return LG(b, pos + 16);
    }

    static final long CENSIZ(byte[] b, int pos) {
        return LG(b, pos + 20);
    }

    static final long CENLEN(byte[] b, int pos) {
        return LG(b, pos + 24);
    }

    static final int CENNAM(byte[] b, int pos) {
        return SH(b, pos + 28);
    }

    static final int CENEXT(byte[] b, int pos) {
        return SH(b, pos + 30);
    }

    static final int CENCOM(byte[] b, int pos) {
        return SH(b, pos + 32);
    }

    static final int CENDSK(byte[] b, int pos) {
        return SH(b, pos + 34);
    }

    static final int CENATT(byte[] b, int pos) {
        return SH(b, pos + 36);
    }

    static final long CENATX(byte[] b, int pos) {
        return LG(b, pos + 38);
    }

    static final int CENATX_PERMS(byte[] b, int pos) {
        return SH(b, pos + 40);
    }

    static final long CENOFF(byte[] b, int pos) {
        return LG(b, pos + 42);
    }
}
