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

package java.lang;

import java.io.*;
import java.util.concurrent.TimeUnit;

public abstract class Process {
    public Process() {
    }

    public abstract OutputStream getOutputStream();

    public abstract InputStream getInputStream();

    public abstract InputStream getErrorStream();

    public abstract int waitFor() throws InterruptedException;

    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        long startTime = System.nanoTime();
        long rem = unit.toNanos(timeout);

        while(true) {
            try {
                this.exitValue();
                return true;
            } catch (IllegalThreadStateException var9) {
                if (rem > 0L) {
                    Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1L, 100L));
                }

                rem = unit.toNanos(timeout) - (System.nanoTime() - startTime);
                if (rem <= 0L) {
                    return false;
                }
            }
        }
    }

    public abstract int exitValue();

    public abstract void destroy();

    public Process destroyForcibly() {
        this.destroy();
        return this;
    }

    public boolean supportsNormalTermination() {
        throw new UnsupportedOperationException(this.getClass() + ".supportsNormalTermination() not supported");
    }

    public boolean isAlive() {
        try {
            this.exitValue();
            return false;
        } catch (IllegalThreadStateException var2) {
            return true;
        }
    }

    public long pid() {
        return 0;
    }

    static class PipeInputStream extends FileInputStream {
        PipeInputStream(FileDescriptor fd) {
            super(fd);
        }

        public long skip(long n) throws IOException {
            long remaining = n;
            if (n <= 0L) {
                return 0L;
            } else {
                int size = (int)Math.min(2048L, remaining);

                int nr;
                for(byte[] skipBuffer = new byte[size]; remaining > 0L; remaining -= (long)nr) {
                    nr = this.read(skipBuffer, 0, (int)Math.min((long)size, remaining));
                    if (nr < 0) {
                        break;
                    }
                }

                return n - remaining;
            }
        }
    }
}
