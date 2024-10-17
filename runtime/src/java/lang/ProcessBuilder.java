/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public final class ProcessBuilder {
    private List<String> command;
    private File directory;
    private Map<String, String> environment;
    private boolean redirectErrorStream;
    private Redirect[] redirects;

    public ProcessBuilder(List<String> command) {
        if (command == null) {
            throw new NullPointerException();
        } else {
            this.command = command;
        }
    }

    public ProcessBuilder(String... command) {
        this.command = new ArrayList(command.length);
        String[] var2 = command;
        int var3 = command.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            String arg = var2[var4];
            this.command.add(arg);
        }

    }

    public ProcessBuilder command(List<String> command) {
        if (command == null) {
            throw new NullPointerException();
        } else {
            this.command = command;
            return this;
        }
    }

    public ProcessBuilder command(String... command) {
        this.command = new ArrayList(command.length);
        String[] var2 = command;
        int var3 = command.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            String arg = var2[var4];
            this.command.add(arg);
        }

        return this;
    }

    public List<String> command() {
        return this.command;
    }

    public Map<String, String> environment() {
        if (this.environment == null) {
            this.environment = new HashMap<>();
        }
        
        return this.environment;
    }

    ProcessBuilder environment(String[] envp) {
        assert this.environment == null;

        if (envp != null) {
            this.environment = new HashMap<>();
            
            String[] var2 = envp;
            int var3 = envp.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                String envstring = var2[var4];
                if (envstring.indexOf(0) != -1) {
                    envstring = envstring.replaceFirst("\u0000.*", "");
                }

                int eqlsign = envstring.indexOf(61, 0);
                if (eqlsign != -1) {
                    this.environment.put(envstring.substring(0, eqlsign), envstring.substring(eqlsign + 1));
                }
            }
        }

        return this;
    }

    public File directory() {
        return this.directory;
    }

    public ProcessBuilder directory(File directory) {
        this.directory = directory;
        return this;
    }

    private Redirect[] redirects() {
        if (this.redirects == null) {
            this.redirects = new Redirect[]{ProcessBuilder.Redirect.PIPE, ProcessBuilder.Redirect.PIPE, ProcessBuilder.Redirect.PIPE};
        }

        return this.redirects;
    }

    public ProcessBuilder redirectInput(Redirect source) {
        if (source.type() != ProcessBuilder.Redirect.Type.WRITE && source.type() != ProcessBuilder.Redirect.Type.APPEND) {
            this.redirects()[0] = source;
            return this;
        } else {
            throw new IllegalArgumentException("Redirect invalid for reading: " + source);
        }
    }

    public ProcessBuilder redirectOutput(Redirect destination) {
        if (destination.type() == ProcessBuilder.Redirect.Type.READ) {
            throw new IllegalArgumentException("Redirect invalid for writing: " + destination);
        } else {
            this.redirects()[1] = destination;
            return this;
        }
    }

    public ProcessBuilder redirectError(Redirect destination) {
        if (destination.type() == ProcessBuilder.Redirect.Type.READ) {
            throw new IllegalArgumentException("Redirect invalid for writing: " + destination);
        } else {
            this.redirects()[2] = destination;
            return this;
        }
    }

    public ProcessBuilder redirectInput(File file) {
        return this.redirectInput(ProcessBuilder.Redirect.from(file));
    }

    public ProcessBuilder redirectOutput(File file) {
        return this.redirectOutput(ProcessBuilder.Redirect.to(file));
    }

    public ProcessBuilder redirectError(File file) {
        return this.redirectError(ProcessBuilder.Redirect.to(file));
    }

    public Redirect redirectInput() {
        return this.redirects == null ? ProcessBuilder.Redirect.PIPE : this.redirects[0];
    }

    public Redirect redirectOutput() {
        return this.redirects == null ? ProcessBuilder.Redirect.PIPE : this.redirects[1];
    }

    public Redirect redirectError() {
        return this.redirects == null ? ProcessBuilder.Redirect.PIPE : this.redirects[2];
    }

    public ProcessBuilder inheritIO() {
        Arrays.fill(this.redirects(), ProcessBuilder.Redirect.INHERIT);
        return this;
    }

    public boolean redirectErrorStream() {
        return this.redirectErrorStream;
    }

    public ProcessBuilder redirectErrorStream(boolean redirectErrorStream) {
        this.redirectErrorStream = redirectErrorStream;
        return this;
    }

    public Process start() throws IOException {
        return this.start(this.redirects);
    }

    private Process start(Redirect[] redirects) throws IOException {
        String[] cmdarray = this.command.toArray(new String[this.command.size()]);
        cmdarray = cmdarray.clone();
        String[] var3 = cmdarray;
        int var4 = cmdarray.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String arg = var3[var5];
            if (arg == null) {
                throw new NullPointerException();
            }
        }

        String prog = cmdarray[0];

        String dir = this.directory == null ? null : this.directory.toString();
        String[] var18 = cmdarray;
        int var7 = cmdarray.length;

        for(int var8 = 0; var8 < var7; ++var8) {
            String s = var18[var8];
            if (s.indexOf(0) >= 0) {
                throw new IOException("invalid null character in command");
            }
        }

        throw new IOException("Commands not supported on this platform");
    }

    public static List<Process> startPipeline(List<ProcessBuilder> builders) throws Exception {
        int numBuilders = builders.size();
        List<Process> processes = new ArrayList(numBuilders);

        try {
            Redirect prevOutput = null;

            for(int index = 0; index < builders.size(); ++index) {
                ProcessBuilder builder = (ProcessBuilder)builders.get(index);
                Redirect[] redirects = builder.redirects();
                if (index > 0) {
                    if (builder.redirectInput() != ProcessBuilder.Redirect.PIPE) {
                        throw new IllegalArgumentException("builder redirectInput() must be PIPE except for the first builder: " + builder.redirectInput());
                    }

                    redirects[0] = prevOutput;
                }

                if (index < numBuilders - 1) {
                    if (builder.redirectOutput() != ProcessBuilder.Redirect.PIPE) {
                        throw new IllegalArgumentException("builder redirectOutput() must be PIPE except for the last builder: " + builder.redirectOutput());
                    }

                    redirects[1] = new RedirectPipeImpl();
                }

                processes.add(builder.start(redirects));
                if (prevOutput instanceof RedirectPipeImpl) {
                    (new Process.PipeInputStream(((RedirectPipeImpl)prevOutput).getFd())).close();
                }

                prevOutput = redirects[1];
            }

            return processes;
        } catch (Exception var8) {
            Exception ex = var8;
            processes.forEach(Process::destroyForcibly);
            processes.forEach((p) -> {
                try {
                    p.waitFor();
                } catch (InterruptedException var2) {
                    Thread.currentThread().interrupt();
                }

            });
            throw ex;
        }
    }

    public abstract static class Redirect {
        private static final File NULL_FILE = new File("/dev/null");
        public static final Redirect PIPE = new Redirect() {
            public Type type() {
                return ProcessBuilder.Redirect.Type.PIPE;
            }

            public String toString() {
                return this.type().toString();
            }
        };
        public static final Redirect INHERIT = new Redirect() {
            public Type type() {
                return ProcessBuilder.Redirect.Type.INHERIT;
            }

            public String toString() {
                return this.type().toString();
            }
        };
        public static final Redirect DISCARD = new Redirect() {
            public Type type() {
                return ProcessBuilder.Redirect.Type.WRITE;
            }

            public String toString() {
                return this.type().toString();
            }

            public File file() {
                return ProcessBuilder.Redirect.NULL_FILE;
            }

            boolean append() {
                return false;
            }
        };

        public abstract Type type();

        public File file() {
            return null;
        }

        boolean append() {
            throw new UnsupportedOperationException();
        }

        public static Redirect from(final File file) {
            if (file == null) {
                throw new NullPointerException();
            } else {
                return new Redirect() {
                    public Type type() {
                        return ProcessBuilder.Redirect.Type.READ;
                    }

                    public File file() {
                        return file;
                    }

                    public String toString() {
                        return "redirect to read from file \"" + file + "\"";
                    }
                };
            }
        }

        public static Redirect to(final File file) {
            if (file == null) {
                throw new NullPointerException();
            } else {
                return new Redirect() {
                    public Type type() {
                        return ProcessBuilder.Redirect.Type.WRITE;
                    }

                    public File file() {
                        return file;
                    }

                    public String toString() {
                        return "redirect to write to file \"" + file + "\"";
                    }

                    boolean append() {
                        return false;
                    }
                };
            }
        }

        public static Redirect appendTo(final File file) {
            if (file == null) {
                throw new NullPointerException();
            } else {
                return new Redirect() {
                    public Type type() {
                        return ProcessBuilder.Redirect.Type.APPEND;
                    }

                    public File file() {
                        return file;
                    }

                    public String toString() {
                        return "redirect to append to file \"" + file + "\"";
                    }

                    boolean append() {
                        return true;
                    }
                };
            }
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof Redirect) {
                Redirect r = (Redirect)obj;
                if (r.type() != this.type()) {
                    return false;
                } else {
                    assert this.file() != null;

                    return this.file().equals(r.file());
                }
            } else {
                return false;
            }
        }

        public int hashCode() {
            File file = this.file();
            return file == null ? super.hashCode() : file.hashCode();
        }

        private Redirect() {
        }

        public static enum Type {
            PIPE,
            INHERIT,
            READ,
            WRITE,
            APPEND;

            private Type() {
            }
        }
    }

    static class RedirectPipeImpl extends Redirect {
        final FileDescriptor fd = new FileDescriptor();

        RedirectPipeImpl() {
        }

        public Redirect.Type type() {
            return ProcessBuilder.Redirect.Type.PIPE;
        }

        public String toString() {
            return this.type().toString();
        }

        FileDescriptor getFd() {
            return this.fd;
        }
    }

    static class NullOutputStream extends OutputStream {
        static final NullOutputStream INSTANCE = new NullOutputStream();

        private NullOutputStream() {
        }

        public void write(int b) throws IOException {
            throw new IOException("Stream closed");
        }
    }

    static class NullInputStream extends InputStream {
        static final NullInputStream INSTANCE = new NullInputStream();

        private NullInputStream() {
        }

        public int read() {
            return -1;
        }

        public int available() {
            return 0;
        }
    }
}
