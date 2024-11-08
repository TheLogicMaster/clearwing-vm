/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.util.jar;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.*;

/**
 * The {@code JarFile} class is used to read the contents of a jar file
 * from any file that can be opened with {@code java.io.RandomAccessFile}.
 * It extends the class {@code java.util.zip.ZipFile} with support
 * for reading an optional {@code Manifest} entry, and support for
 * processing multi-release jar files.  The {@code Manifest} can be used
 * to specify meta-information about the jar file and its entries.
 *
 * <p><a id="multirelease">A multi-release jar file</a> is a jar file that
 * contains a manifest with a main attribute named "Multi-Release",
 * a set of "base" entries, some of which are public classes with public
 * or protected methods that comprise the public interface of the jar file,
 * and a set of "versioned" entries contained in subdirectories of the
 * "META-INF/versions" directory.  The versioned entries are partitioned by the
 * major version of the Java platform.  A versioned entry, with a version
 * {@code n}, {@code 8 < n}, in the "META-INF/versions/{n}" directory overrides
 * the base entry as well as any entry with a version number {@code i} where
 * {@code 8 < i < n}.
 *
 * <p>By default, a {@code JarFile} for a multi-release jar file is configured
 * to process the multi-release jar file as if it were a plain (unversioned) jar
 * file, and as such an entry name is associated with at most one base entry.
 * The {@code JarFile} may be configured to process a multi-release jar file by
 * creating the {@code JarFile} with the
 * {@link JarFile#JarFile(File, boolean, int, Runtime.Version)} constructor.  The
 * {@code Runtime.Version} object sets a maximum version used when searching for
 * versioned entries.  When so configured, an entry name
 * can correspond with at most one base entry and zero or more versioned
 * entries. A search is required to associate the entry name with the latest
 * versioned entry whose version is less than or equal to the maximum version
 * (see {@link #getEntry(String)}).
 *
 * <p>Class loaders that utilize {@code JarFile} to load classes from the
 * contents of {@code JarFile} entries should construct the {@code JarFile}
 * by invoking the {@link JarFile#JarFile(File, boolean, int, Runtime.Version)}
 * constructor with the value {@code Runtime.version()} assigned to the last
 * argument.  This assures that classes compatible with the major
 * version of the running JVM are loaded from multi-release jar files.
 *
 * <p>If the verify flag is on when opening a signed jar file, the content of
 * the file is verified against its signature embedded inside the file. Please
 * note that the verification process does not include validating the signer's
 * certificate. A caller should inspect the return value of
 * {@link JarEntry#getCodeSigners()} to further determine if the signature
 * can be trusted.
 *
 * <p> Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be
 * thrown.
 *
 * @implNote
 * <div class="block">
 * If the API can not be used to configure a {@code JarFile} (e.g. to override
 * the configuration of a compiled application or library), two {@code System}
 * properties are available.
 * <ul>
 * <li>
 * {@code jdk.util.jar.version} can be assigned a value that is the
 * {@code String} representation of a non-negative integer
 * {@code <= Runtime.version().major()}.  The value is used to set the effective
 * runtime version to something other than the default value obtained by
 * evaluating {@code Runtime.version().major()}. The effective runtime version
 * is the version that the {@link JarFile#JarFile(File, boolean, int, Runtime.Version)}
 * constructor uses when the value of the last argument is
 * {@code JarFile.runtimeVersion()}.
 * </li>
 * <li>
 * {@code jdk.util.jar.enableMultiRelease} can be assigned one of the three
 * {@code String} values <em>true</em>, <em>false</em>, or <em>force</em>.  The
 * value <em>true</em>, the default value, enables multi-release jar file
 * processing.  The value <em>false</em> disables multi-release jar processing,
 * ignoring the "Multi-Release" manifest attribute, and the versioned
 * directories in a multi-release jar file if they exist.  Furthermore,
 * the method {@link JarFile#isMultiRelease()} returns <em>false</em>. The value
 * <em>force</em> causes the {@code JarFile} to be initialized to runtime
 * versioning after construction.  It effectively does the same as this code:
 * {@code (new JarFile(File, boolean, int, JarFile.runtimeVersion())}.
 * </li>
 * </ul>
 * </div>
 *
 * @author  David Connelly
 * @see     Manifest
 * @see     java.util.zip.ZipFile
 * @see     java.util.jar.JarEntry
 * @since   1.2
 */
public
class JarFile extends ZipFile {
    private static final String META_INF = "META-INF/";

    private static final String META_INF_VERSIONS = META_INF + "versions/";

    /**
     * The JAR manifest file name.
     */
    public static final String MANIFEST_NAME = META_INF + "MANIFEST.MF";
    
    /**
     * Creates a new {@code JarFile} to read from the specified
     * file {@code name}. The {@code JarFile} will be verified if
     * it is signed.
     * @param name the name of the jar file to be opened for reading
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
     */
    public JarFile(String name) throws IOException {
        this(new File(name), true, ZipFile.OPEN_READ);
    }

    /**
     * Creates a new {@code JarFile} to read from the specified
     * file {@code name}.
     * @param name the name of the jar file to be opened for reading
     * @param verify whether or not to verify the jar file if
     * it is signed.
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
     */
    public JarFile(String name, boolean verify) throws IOException {
        this(new File(name), verify, ZipFile.OPEN_READ);
    }

    /**
     * Creates a new {@code JarFile} to read from the specified
     * {@code File} object. The {@code JarFile} will be verified if
     * it is signed.
     * @param file the jar file to be opened for reading
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
     */
    public JarFile(File file) throws IOException {
        this(file, true, ZipFile.OPEN_READ);
    }

    /**
     * Creates a new {@code JarFile} to read from the specified
     * {@code File} object.
     * @param file the jar file to be opened for reading
     * @param verify whether or not to verify the jar file if
     * it is signed.
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager.
     */
    public JarFile(File file, boolean verify) throws IOException {
        this(file, verify, ZipFile.OPEN_READ);
    }


    /**
     * Creates a new {@code JarFile} to read from the specified
     * {@code File} object in the specified mode.  The mode argument
     * must be either {@code OPEN_READ} or {@code OPEN_READ | OPEN_DELETE}.
     * The version argument, after being converted to a canonical form, is
     * used to configure the {@code JarFile} for processing
     * multi-release jar files.
     * <p>
     * The canonical form derived from the version parameter is
     * {@code Runtime.Version.parse(Integer.toString(n))} where {@code n} is
     * {@code Math.max(version.major(), JarFile.baseVersion().major())}.
     *
     * @param file the jar file to be opened for reading
     * @param verify whether or not to verify the jar file if
     * it is signed.
     * @param mode the mode in which the file is to be opened
     * @throws IOException if an I/O error has occurred
     * @throws IllegalArgumentException
     *         if the {@code mode} argument is invalid
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
     * @throws NullPointerException if {@code version} is {@code null}
     */
    public JarFile(File file, boolean verify, int mode) throws IOException {
        super(file, mode);
//        this.verify = verify;
//        if (MULTI_RELEASE_FORCED || version.major() == RUNTIME_VERSION.major()) {
//            // This deals with the common case where the value from JarFile.runtimeVersion() is passed
//            this.version = RUNTIME_VERSION;
//        } else if (version.major() <= BASE_VERSION_MAJOR) {
//            // This also deals with the common case where the value from JarFile.baseVersion() is passed
//            this.version = BASE_VERSION;
//        } else {
//            // Canonicalize
//            this.version = Runtime.Version.parse(Integer.toString(version.major()));
//        }
//        this.versionMajor = this.version.major();
    }
    
    /**
     * Returns the jar file manifest, or {@code null} if none.
     *
     * @return the jar file manifest, or {@code null} if none
     *
     * @throws IllegalStateException
     *         may be thrown if the jar file has been closed
     * @throws IOException  if an I/O error has occurred
     */
//    public Manifest getManifest() throws IOException {
//        return getManifestFromReference();
//    }

    /**
     * Returns the {@code JarEntry} for the given base entry name or
     * {@code null} if not found.
     *
     * <p>If this {@code JarFile} is a multi-release jar file and is configured
     * to be processed as such, then a search is performed to find and return
     * a {@code JarEntry} that is the latest versioned entry associated with the
     * given entry name.  The returned {@code JarEntry} is the versioned entry
     * corresponding to the given base entry name prefixed with the string
     * {@code "META-INF/versions/{n}/"}, for the largest value of {@code n} for
     * which an entry exists.  If such a versioned entry does not exist, then
     * the {@code JarEntry} for the base entry is returned, otherwise
     * {@code null} is returned if no entries are found.  The initial value for
     * the version {@code n} is the maximum version as returned by the method
     * {@link JarFile#getVersion()}.
     *
     * @param name the jar file entry name
     * @return the {@code JarEntry} for the given entry name, or
     *         the versioned entry name, or {@code null} if not found
     *
     * @throws IllegalStateException
     *         may be thrown if the jar file has been closed
     *
     * @see java.util.jar.JarEntry
     *
     * @implSpec
     * <div class="block">
     * This implementation invokes {@link JarFile#getEntry(String)}.
     * </div>
     */
    public JarEntry getJarEntry(String name) {
        return (JarEntry)getEntry(name);
    }

    private class JarEntryIterator implements Enumeration<JarEntry>,
            Iterator<JarEntry>
    {
        final Enumeration<? extends ZipEntry> e = JarFile.super.entries();

        public boolean hasNext() {
            return e.hasMoreElements();
        }

        public JarEntry next() {
            ZipEntry ze = e.nextElement();
            return new JarFileEntry(ze.getName(), ze);
        }

        public boolean hasMoreElements() {
            return hasNext();
        }

        public JarEntry nextElement() {
            return next();
        }
    }

    /**
     * Returns an enumeration of the jar file entries.
     *
     * @return an enumeration of the jar file entries
     * @throws IllegalStateException
     *         may be thrown if the jar file has been closed
     */
    public Enumeration<JarEntry> entries() {
        return new JarEntryIterator();
    }

    private class JarFileEntry extends JarEntry {
        final private String name;

        JarFileEntry(ZipEntry ze) {
            super(ze);
            this.name = ze.getName();
        }
        JarFileEntry(String name, ZipEntry vze) {
            super(vze);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * Returns {@code true} iff this JAR file has a manifest with the
     * Class-Path attribute
     */
    boolean hasClassPathAttribute() throws IOException {
        return false;
    }
}