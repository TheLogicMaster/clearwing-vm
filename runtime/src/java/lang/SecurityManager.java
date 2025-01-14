/*
 * Copyright (c) 1995, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleReference;
import java.lang.reflect.Member;
import java.io.FileDescriptor;
import java.io.File;
import java.io.FilePermission;
import java.net.InetAddress;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.Security;
import java.security.SecurityPermission;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The security manager is a class that allows
 * applications to implement a security policy. It allows an
 * application to determine, before performing a possibly unsafe or
 * sensitive operation, what the operation is and whether
 * it is being attempted in a security context that allows the
 * operation to be performed. The
 * application can allow or disallow the operation.
 * <p>
 * The <code>SecurityManager</code> class contains many methods with
 * names that begin with the word <code>check</code>. These methods
 * are called by various methods in the Java libraries before those
 * methods perform certain potentially sensitive operations. The
 * invocation of such a <code>check</code> method typically looks like this:
 * <blockquote><pre>
 *     SecurityManager security = System.getSecurityManager();
 *     if (security != null) {
 *         security.check<i>XXX</i>(argument, &nbsp;.&nbsp;.&nbsp;.&nbsp;);
 *     }
 * </pre></blockquote>
 * <p>
 * The security manager is thereby given an opportunity to prevent
 * completion of the operation by throwing an exception. A security
 * manager routine simply returns if the operation is permitted, but
 * throws a <code>SecurityException</code> if the operation is not
 * permitted.
 * <p>
 * The current security manager is set by the
 * <code>setSecurityManager</code> method in class
 * <code>System</code>. The current security manager is obtained
 * by the <code>getSecurityManager</code> method.
 * <p>
 * The special method
 * {@link SecurityManager#checkPermission(java.security.Permission)}
 * determines whether an access request indicated by a specified
 * permission should be granted or denied. The
 * default implementation calls
 *
 * <pre>
 *   AccessController.checkPermission(perm);
 * </pre>
 *
 * <p>
 * If a requested access is allowed,
 * <code>checkPermission</code> returns quietly. If denied, a
 * <code>SecurityException</code> is thrown.
 * <p>
 * As of Java 2 SDK v1.2, the default implementation of each of the other
 * <code>check</code> methods in <code>SecurityManager</code> is to
 * call the <code>SecurityManager checkPermission</code> method
 * to determine if the calling thread has permission to perform the requested
 * operation.
 * <p>
 * Note that the <code>checkPermission</code> method with
 * just a single permission argument always performs security checks
 * within the context of the currently executing thread.
 * Sometimes a security check that should be made within a given context
 * will actually need to be done from within a
 * <i>different</i> context (for example, from within a worker thread).
 * The {@link SecurityManager#getSecurityContext getSecurityContext} method
 * and the {@link SecurityManager#checkPermission(java.security.Permission,
 * java.lang.Object) checkPermission}
 * method that includes a context argument are provided
 * for this situation. The
 * <code>getSecurityContext</code> method returns a "snapshot"
 * of the current calling context. (The default implementation
 * returns an AccessControlContext object.) A sample call is
 * the following:
 *
 * <pre>
 *   Object context = null;
 *   SecurityManager sm = System.getSecurityManager();
 *   if (sm != null) context = sm.getSecurityContext();
 * </pre>
 *
 * <p>
 * The <code>checkPermission</code> method
 * that takes a context object in addition to a permission
 * makes access decisions based on that context,
 * rather than on that of the current execution thread.
 * Code within a different context can thus call that method,
 * passing the permission and the
 * previously-saved context object. A sample call, using the
 * SecurityManager <code>sm</code> obtained as in the previous example,
 * is the following:
 *
 * <pre>
 *   if (sm != null) sm.checkPermission(permission, context);
 * </pre>
 *
 * <p>Permissions fall into these categories: File, Socket, Net,
 * Security, Runtime, Property, AWT, Reflect, and Serializable.
 * The classes managing these various
 * permission categories are <code>java.io.FilePermission</code>,
 * <code>java.net.SocketPermission</code>,
 * <code>java.net.NetPermission</code>,
 * <code>java.security.SecurityPermission</code>,
 * <code>java.lang.RuntimePermission</code>,
 * <code>java.util.PropertyPermission</code>,
 * <code>java.awt.AWTPermission</code>,
 * <code>java.lang.reflect.ReflectPermission</code>, and
 * <code>java.io.SerializablePermission</code>.
 *
 * <p>All but the first two (FilePermission and SocketPermission) are
 * subclasses of <code>java.security.BasicPermission</code>, which itself
 * is an abstract subclass of the
 * top-level class for permissions, which is
 * <code>java.security.Permission</code>. BasicPermission defines the
 * functionality needed for all permissions that contain a name
 * that follows the hierarchical property naming convention
 * (for example, "exitVM", "setFactory", "queuePrintJob", etc).
 * An asterisk
 * may appear at the end of the name, following a ".", or by itself, to
 * signify a wildcard match. For example: "a.*" or "*" is valid,
 * "*a" or "a*b" is not valid.
 *
 * <p>FilePermission and SocketPermission are subclasses of the
 * top-level class for permissions
 * (<code>java.security.Permission</code>). Classes like these
 * that have a more complicated name syntax than that used by
 * BasicPermission subclass directly from Permission rather than from
 * BasicPermission. For example,
 * for a <code>java.io.FilePermission</code> object, the permission name is
 * the path name of a file (or directory).
 *
 * <p>Some of the permission classes have an "actions" list that tells
 * the actions that are permitted for the object.  For example,
 * for a <code>java.io.FilePermission</code> object, the actions list
 * (such as "read, write") specifies which actions are granted for the
 * specified file (or for files in the specified directory).
 *
 * <p>Other permission classes are for "named" permissions -
 * ones that contain a name but no actions list; you either have the
 * named permission or you don't.
 *
 * <p>Note: There is also a <code>java.security.AllPermission</code>
 * permission that implies all permissions. It exists to simplify the work
 * of system administrators who might need to perform multiple
 * tasks that require all (or numerous) permissions.
 * <p>
 * See {@extLink security_guide_permissions
 * Permissions in the Java Development Kit (JDK)}
 * for permission-related information.
 * This document includes, for example, a table listing the various SecurityManager
 * <code>check</code> methods and the permission(s) the default
 * implementation of each such method requires.
 * It also contains a table of all the version 1.2 methods
 * that require permissions, and for each such method tells
 * which permission it requires.
 *
 * @author  Arthur van Hoff
 * @author  Roland Schemers
 *
 * @see     java.lang.ClassLoader
 * @see     java.lang.SecurityException
 * @see     java.lang.System#getSecurityManager() getSecurityManager
 * @see     java.lang.System#setSecurityManager(java.lang.SecurityManager)
 *  setSecurityManager
 * @see     java.security.AccessController AccessController
 * @see     java.security.AccessControlContext AccessControlContext
 * @see     java.security.AccessControlException AccessControlException
 * @see     java.security.Permission
 * @see     java.security.BasicPermission
 * @see     java.io.FilePermission
 * @see     java.net.SocketPermission
 * @see     java.util.PropertyPermission
 * @see     java.lang.RuntimePermission
 * @see     java.awt.AWTPermission
 * @see     java.security.Policy Policy
 * @see     java.security.SecurityPermission SecurityPermission
 * @see     java.security.ProtectionDomain
 *
 * @since   1.0
 */
public
class SecurityManager {

    /**
     * This field is <code>true</code> if there is a security check in
     * progress; <code>false</code> otherwise.
     *
     * @deprecated This type of security checking is not recommended.
     *  It is recommended that the <code>checkPermission</code>
     *  call be used instead. This field is subject to removal in a
     *  future version of Java SE.
     */
    @Deprecated(since="1.2", forRemoval=true)
    protected boolean inCheck;

    /*
     * Have we been initialized. Effective against finalizer attacks.
     */
    private boolean initialized = false;


    /**
     * returns true if the current context has been granted AllPermission
     */
    private boolean hasAllPermission() {
        return true;
    }

    /**
     * Tests if there is a security check in progress.
     *
     * @return the value of the <code>inCheck</code> field. This field
     *          should contain <code>true</code> if a security check is
     *          in progress,
     *          <code>false</code> otherwise.
     * @see     java.lang.SecurityManager#inCheck
     * @deprecated This type of security checking is not recommended.
     *  It is recommended that the <code>checkPermission</code>
     *  call be used instead. This method is subject to removal in a
     *  future version of Java SE.
     */
    @Deprecated(since="1.2", forRemoval=true)
    public boolean getInCheck() {
        return inCheck;
    }

    /**
     * Constructs a new <code>SecurityManager</code>.
     *
     * <p> If there is a security manager already installed, this method first
     * calls the security manager's <code>checkPermission</code> method
     * with the <code>RuntimePermission("createSecurityManager")</code>
     * permission to ensure the calling thread has permission to create a new
     * security manager.
     * This may result in throwing a <code>SecurityException</code>.
     *
     * @exception  java.lang.SecurityException if a security manager already
     *             exists and its <code>checkPermission</code> method
     *             doesn't allow creation of a new security manager.
     * @see        java.lang.System#getSecurityManager()
     * @see        #checkPermission(java.security.Permission) checkPermission
     * @see java.lang.RuntimePermission
     */
    public SecurityManager() {
//        synchronized(SecurityManager.class) {
//            SecurityManager sm = System.getSecurityManager();
//            if (sm != null) {
//                // ask the currently installed security manager if we
//                // can create a new one.
//                sm.checkPermission(new RuntimePermission
//                        ("createSecurityManager"));
//            }
//            initialized = true;
//        }
    }

    /**
     * Returns the current execution stack as an array of classes.
     * <p>
     * The length of the array is the number of methods on the execution
     * stack. The element at index <code>0</code> is the class of the
     * currently executing method, the element at index <code>1</code> is
     * the class of that method's caller, and so on.
     *
     * @return  the execution stack.
     */
    protected Class<?>[] getClassContext() {
        return new Class<?>[0];
    }

    /**
     * Returns the class loader of the most recently executing method from
     * a class defined using a non-system class loader. A non-system
     * class loader is defined as being a class loader that is not equal to
     * the system class loader (as returned
     * by {@link ClassLoader#getSystemClassLoader}) or one of its ancestors.
     * <p>
     * This method will return
     * <code>null</code> in the following three cases:
     * <ol>
     *   <li>All methods on the execution stack are from classes
     *   defined using the system class loader or one of its ancestors.
     *
     *   <li>All methods on the execution stack up to the first
     *   "privileged" caller
     *   (see {@link java.security.AccessController#doPrivileged})
     *   are from classes
     *   defined using the system class loader or one of its ancestors.
     *
     *   <li> A call to <code>checkPermission</code> with
     *   <code>java.security.AllPermission</code> does not
     *   result in a SecurityException.
     *
     * </ol>
     *
     * @return  the class loader of the most recent occurrence on the stack
     *          of a method from a class defined using a non-system class
     *          loader.
     *
     * @deprecated This type of security checking is not recommended.
     *  It is recommended that the <code>checkPermission</code>
     *  call be used instead. This method is subject to removal in a
     *  future version of Java SE.
     *
     * @see  java.lang.ClassLoader#getSystemClassLoader() getSystemClassLoader
     * @see  #checkPermission(java.security.Permission) checkPermission
     */
    @Deprecated(since="1.2", forRemoval=true)
    protected ClassLoader currentClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
    
    /**
     * Returns the class of the most recently executing method from
     * a class defined using a non-system class loader. A non-system
     * class loader is defined as being a class loader that is not equal to
     * the system class loader (as returned
     * by {@link ClassLoader#getSystemClassLoader}) or one of its ancestors.
     * <p>
     * This method will return
     * <code>null</code> in the following three cases:
     * <ol>
     *   <li>All methods on the execution stack are from classes
     *   defined using the system class loader or one of its ancestors.
     *
     *   <li>All methods on the execution stack up to the first
     *   "privileged" caller
     *   (see {@link java.security.AccessController#doPrivileged})
     *   are from classes
     *   defined using the system class loader or one of its ancestors.
     *
     *   <li> A call to <code>checkPermission</code> with
     *   <code>java.security.AllPermission</code> does not
     *   result in a SecurityException.
     *
     * </ol>
     *
     * @return  the class  of the most recent occurrence on the stack
     *          of a method from a class defined using a non-system class
     *          loader.
     *
     * @deprecated This type of security checking is not recommended.
     *  It is recommended that the <code>checkPermission</code>
     *  call be used instead. This method is subject to removal in a
     *  future version of Java SE.
     *
     * @see  java.lang.ClassLoader#getSystemClassLoader() getSystemClassLoader
     * @see  #checkPermission(java.security.Permission) checkPermission
     */
    @Deprecated(since="1.2", forRemoval=true)
    protected Class<?> currentLoadedClass() {
        return null;
    }

    /**
     * Returns the stack depth of the specified class.
     *
     * @param   name   the fully qualified name of the class to search for.
     * @return  the depth on the stack frame of the first occurrence of a
     *          method from a class with the specified name;
     *          <code>-1</code> if such a frame cannot be found.
     * @deprecated This type of security checking is not recommended.
     *  It is recommended that the <code>checkPermission</code>
     *  call be used instead. This method is subject to removal in a
     *  future version of Java SE.
     */
    @Deprecated(since="1.2", forRemoval=true)
    protected int classDepth(String name) {
        return -1;
    }

    /**
     * Returns the stack depth of the most recently executing method
     * from a class defined using a non-system class loader.  A non-system
     * class loader is defined as being a class loader that is not equal to
     * the system class loader (as returned
     * by {@link ClassLoader#getSystemClassLoader}) or one of its ancestors.
     * <p>
     * This method will return
     * -1 in the following three cases:
     * <ol>
     *   <li>All methods on the execution stack are from classes
     *   defined using the system class loader or one of its ancestors.
     *
     *   <li>All methods on the execution stack up to the first
     *   "privileged" caller
     *   (see {@link java.security.AccessController#doPrivileged})
     *   are from classes
     *   defined using the system class loader or one of its ancestors.
     *
     *   <li> A call to <code>checkPermission</code> with
     *   <code>java.security.AllPermission</code> does not
     *   result in a SecurityException.
     *
     * </ol>
     *
     * @return the depth on the stack frame of the most recent occurrence of
     *          a method from a class defined using a non-system class loader.
     *
     * @deprecated This type of security checking is not recommended.
     *  It is recommended that the <code>checkPermission</code>
     *  call be used instead. This method is subject to removal in a
     *  future version of Java SE.
     *
     * @see   java.lang.ClassLoader#getSystemClassLoader() getSystemClassLoader
     * @see   #checkPermission(java.security.Permission) checkPermission
     */
    @Deprecated(since="1.2", forRemoval=true)
    protected int classLoaderDepth() {
        return -1;
    }
    
    /**
     * Tests if a method from a class with the specified
     *         name is on the execution stack.
     *
     * @param  name   the fully qualified name of the class.
     * @return <code>true</code> if a method from a class with the specified
     *         name is on the execution stack; <code>false</code> otherwise.
     * @deprecated This type of security checking is not recommended.
     *  It is recommended that the <code>checkPermission</code>
     *  call be used instead. This method is subject to removal in a
     *  future version of Java SE.
     */
    @Deprecated(since="1.2", forRemoval=true)
    protected boolean inClass(String name) {
        return classDepth(name) >= 0;
    }

    /**
     * Basically, tests if a method from a class defined using a
     *          class loader is on the execution stack.
     *
     * @return  <code>true</code> if a call to <code>currentClassLoader</code>
     *          has a non-null return value.
     *
     * @deprecated This type of security checking is not recommended.
     *  It is recommended that the <code>checkPermission</code>
     *  call be used instead. This method is subject to removal in a
     *  future version of Java SE.
     * @see        #currentClassLoader() currentClassLoader
     */
    @Deprecated(since="1.2", forRemoval=true)
    protected boolean inClassLoader() {
        return currentClassLoader() != null;
    }

    /**
     * Creates an object that encapsulates the current execution
     * environment. The result of this method is used, for example, by the
     * three-argument <code>checkConnect</code> method and by the
     * two-argument <code>checkRead</code> method.
     * These methods are needed because a trusted method may be called
     * on to read a file or open a socket on behalf of another method.
     * The trusted method needs to determine if the other (possibly
     * untrusted) method would be allowed to perform the operation on its
     * own.
     * <p> The default implementation of this method is to return
     * an <code>AccessControlContext</code> object.
     *
     * @return  an implementation-dependent object that encapsulates
     *          sufficient information about the current execution environment
     *          to perform some security checks later.
     * @see     java.lang.SecurityManager#checkConnect(java.lang.String, int,
     *   java.lang.Object) checkConnect
     * @see     java.lang.SecurityManager#checkRead(java.lang.String,
     *   java.lang.Object) checkRead
     * @see     java.security.AccessControlContext AccessControlContext
     */
    public Object getSecurityContext() {
        return null;
    }

    /**
     * Throws a <code>SecurityException</code> if the requested
     * access, specified by the given permission, is not permitted based
     * on the security policy currently in effect.
     * <p>
     * This method calls <code>AccessController.checkPermission</code>
     * with the given permission.
     *
     * @param     perm   the requested permission.
     * @exception SecurityException if access is not permitted based on
     *            the current security policy.
     * @exception NullPointerException if the permission argument is
     *            <code>null</code>.
     * @since     1.2
     */
    public void checkPermission(Permission perm) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * specified security context is denied access to the resource
     * specified by the given permission.
     * The context must be a security
     * context returned by a previous call to
     * <code>getSecurityContext</code> and the access control
     * decision is based upon the configured security policy for
     * that security context.
     * <p>
     * If <code>context</code> is an instance of
     * <code>AccessControlContext</code> then the
     * <code>AccessControlContext.checkPermission</code> method is
     * invoked with the specified permission.
     * <p>
     * If <code>context</code> is not an instance of
     * <code>AccessControlContext</code> then a
     * <code>SecurityException</code> is thrown.
     *
     * @param      perm      the specified permission
     * @param      context   a system-dependent security context.
     * @exception  SecurityException  if the specified security context
     *             is not an instance of <code>AccessControlContext</code>
     *             (e.g., is <code>null</code>), or is denied access to the
     *             resource specified by the given permission.
     * @exception  NullPointerException if the permission argument is
     *             <code>null</code>.
     * @see        java.lang.SecurityManager#getSecurityContext()
     * @see java.security.AccessControlContext#checkPermission(java.security.Permission)
     * @since      1.2
     */
    public void checkPermission(Permission perm, Object context) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to create a new class loader.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>RuntimePermission("createClassLoader")</code>
     * permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkCreateClassLoader</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @exception SecurityException if the calling thread does not
     *             have permission
     *             to create a new class loader.
     * @see        java.lang.ClassLoader#ClassLoader()
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkCreateClassLoader() {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to modify the thread argument.
     * <p>
     * This method is invoked for the current security manager by the
     * <code>stop</code>, <code>suspend</code>, <code>resume</code>,
     * <code>setPriority</code>, <code>setName</code>, and
     * <code>setDaemon</code> methods of class <code>Thread</code>.
     * <p>
     * If the thread argument is a system thread (belongs to
     * the thread group with a <code>null</code> parent) then
     * this method calls <code>checkPermission</code> with the
     * <code>RuntimePermission("modifyThread")</code> permission.
     * If the thread argument is <i>not</i> a system thread,
     * this method just returns silently.
     * <p>
     * Applications that want a stricter policy should override this
     * method. If this method is overridden, the method that overrides
     * it should additionally check to see if the calling thread has the
     * <code>RuntimePermission("modifyThread")</code> permission, and
     * if so, return silently. This is to ensure that code granted
     * that permission (such as the JDK itself) is allowed to
     * manipulate any thread.
     * <p>
     * If this method is overridden, then
     * <code>super.checkAccess</code> should
     * be called by the first statement in the overridden method, or the
     * equivalent security check should be placed in the overridden method.
     *
     * @param      t   the thread to be checked.
     * @exception  SecurityException  if the calling thread does not have
     *             permission to modify the thread.
     * @exception  NullPointerException if the thread argument is
     *             <code>null</code>.
     * @see        java.lang.Thread#resume() resume
     * @see        java.lang.Thread#setDaemon(boolean) setDaemon
     * @see        java.lang.Thread#setName(java.lang.String) setName
     * @see        java.lang.Thread#setPriority(int) setPriority
     * @see        java.lang.Thread#stop() stop
     * @see        java.lang.Thread#suspend() suspend
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkAccess(Thread t) {
    }
    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to modify the thread group argument.
     * <p>
     * This method is invoked for the current security manager when a
     * new child thread or child thread group is created, and by the
     * <code>setDaemon</code>, <code>setMaxPriority</code>,
     * <code>stop</code>, <code>suspend</code>, <code>resume</code>, and
     * <code>destroy</code> methods of class <code>ThreadGroup</code>.
     * <p>
     * If the thread group argument is the system thread group (
     * has a <code>null</code> parent) then
     * this method calls <code>checkPermission</code> with the
     * <code>RuntimePermission("modifyThreadGroup")</code> permission.
     * If the thread group argument is <i>not</i> the system thread group,
     * this method just returns silently.
     * <p>
     * Applications that want a stricter policy should override this
     * method. If this method is overridden, the method that overrides
     * it should additionally check to see if the calling thread has the
     * <code>RuntimePermission("modifyThreadGroup")</code> permission, and
     * if so, return silently. This is to ensure that code granted
     * that permission (such as the JDK itself) is allowed to
     * manipulate any thread.
     * <p>
     * If this method is overridden, then
     * <code>super.checkAccess</code> should
     * be called by the first statement in the overridden method, or the
     * equivalent security check should be placed in the overridden method.
     *
     * @param      g   the thread group to be checked.
     * @exception  SecurityException  if the calling thread does not have
     *             permission to modify the thread group.
     * @exception  NullPointerException if the thread group argument is
     *             <code>null</code>.
     * @see        java.lang.ThreadGroup#destroy() destroy
     * @see        java.lang.ThreadGroup#resume() resume
     * @see        java.lang.ThreadGroup#setDaemon(boolean) setDaemon
     * @see        java.lang.ThreadGroup#setMaxPriority(int) setMaxPriority
     * @see        java.lang.ThreadGroup#stop() stop
     * @see        java.lang.ThreadGroup#suspend() suspend
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkAccess(ThreadGroup g) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to cause the Java Virtual Machine to
     * halt with the specified status code.
     * <p>
     * This method is invoked for the current security manager by the
     * <code>exit</code> method of class <code>Runtime</code>. A status
     * of <code>0</code> indicates success; other values indicate various
     * errors.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>RuntimePermission("exitVM."+status)</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkExit</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      status   the exit status.
     * @exception SecurityException if the calling thread does not have
     *              permission to halt the Java Virtual Machine with
     *              the specified status.
     * @see        java.lang.Runtime#exit(int) exit
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkExit(int status) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to create a subprocess.
     * <p>
     * This method is invoked for the current security manager by the
     * <code>exec</code> methods of class <code>Runtime</code>.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>FilePermission(cmd,"execute")</code> permission
     * if cmd is an absolute path, otherwise it calls
     * <code>checkPermission</code> with
     * <code>FilePermission("&lt;&lt;ALL FILES&gt;&gt;","execute")</code>.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkExec</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      cmd   the specified system command.
     * @exception  SecurityException if the calling thread does not have
     *             permission to create a subprocess.
     * @exception  NullPointerException if the <code>cmd</code> argument is
     *             <code>null</code>.
     * @see     java.lang.Runtime#exec(java.lang.String)
     * @see     java.lang.Runtime#exec(java.lang.String, java.lang.String[])
     * @see     java.lang.Runtime#exec(java.lang.String[])
     * @see     java.lang.Runtime#exec(java.lang.String[], java.lang.String[])
     * @see     #checkPermission(java.security.Permission) checkPermission
     */
    public void checkExec(String cmd) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to dynamic link the library code
     * specified by the string argument file. The argument is either a
     * simple library name or a complete filename.
     * <p>
     * This method is invoked for the current security manager by
     * methods <code>load</code> and <code>loadLibrary</code> of class
     * <code>Runtime</code>.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>RuntimePermission("loadLibrary."+lib)</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkLink</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      lib   the name of the library.
     * @exception  SecurityException if the calling thread does not have
     *             permission to dynamically link the library.
     * @exception  NullPointerException if the <code>lib</code> argument is
     *             <code>null</code>.
     * @see        java.lang.Runtime#load(java.lang.String)
     * @see        java.lang.Runtime#loadLibrary(java.lang.String)
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkLink(String lib) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to read from the specified file
     * descriptor.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>RuntimePermission("readFileDescriptor")</code>
     * permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkRead</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      fd   the system-dependent file descriptor.
     * @exception  SecurityException  if the calling thread does not have
     *             permission to access the specified file descriptor.
     * @exception  NullPointerException if the file descriptor argument is
     *             <code>null</code>.
     * @see        java.io.FileDescriptor
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkRead(FileDescriptor fd) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to read the file specified by the
     * string argument.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>FilePermission(file,"read")</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkRead</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      file   the system-dependent file name.
     * @exception  SecurityException if the calling thread does not have
     *             permission to access the specified file.
     * @exception  NullPointerException if the <code>file</code> argument is
     *             <code>null</code>.
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkRead(String file) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * specified security context is not allowed to read the file
     * specified by the string argument. The context must be a security
     * context returned by a previous call to
     * <code>getSecurityContext</code>.
     * <p> If <code>context</code> is an instance of
     * <code>AccessControlContext</code> then the
     * <code>AccessControlContext.checkPermission</code> method will
     * be invoked with the <code>FilePermission(file,"read")</code> permission.
     * <p> If <code>context</code> is not an instance of
     * <code>AccessControlContext</code> then a
     * <code>SecurityException</code> is thrown.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkRead</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      file      the system-dependent filename.
     * @param      context   a system-dependent security context.
     * @exception  SecurityException  if the specified security context
     *             is not an instance of <code>AccessControlContext</code>
     *             (e.g., is <code>null</code>), or does not have permission
     *             to read the specified file.
     * @exception  NullPointerException if the <code>file</code> argument is
     *             <code>null</code>.
     * @see        java.lang.SecurityManager#getSecurityContext()
     * @see        java.security.AccessControlContext#checkPermission(java.security.Permission)
     */
    public void checkRead(String file, Object context) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to write to the specified file
     * descriptor.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>RuntimePermission("writeFileDescriptor")</code>
     * permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkWrite</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      fd   the system-dependent file descriptor.
     * @exception SecurityException  if the calling thread does not have
     *             permission to access the specified file descriptor.
     * @exception  NullPointerException if the file descriptor argument is
     *             <code>null</code>.
     * @see        java.io.FileDescriptor
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkWrite(FileDescriptor fd) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to write to the file specified by
     * the string argument.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>FilePermission(file,"write")</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkWrite</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      file   the system-dependent filename.
     * @exception  SecurityException  if the calling thread does not
     *             have permission to access the specified file.
     * @exception  NullPointerException if the <code>file</code> argument is
     *             <code>null</code>.
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkWrite(String file) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to delete the specified file.
     * <p>
     * This method is invoked for the current security manager by the
     * <code>delete</code> method of class <code>File</code>.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>FilePermission(file,"delete")</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkDelete</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      file   the system-dependent filename.
     * @exception  SecurityException if the calling thread does not
     *             have permission to delete the file.
     * @exception  NullPointerException if the <code>file</code> argument is
     *             <code>null</code>.
     * @see        java.io.File#delete()
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkDelete(String file) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to open a socket connection to the
     * specified host and port number.
     * <p>
     * A port number of <code>-1</code> indicates that the calling
     * method is attempting to determine the IP address of the specified
     * host name.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>SocketPermission(host+":"+port,"connect")</code> permission if
     * the port is not equal to -1. If the port is equal to -1, then
     * it calls <code>checkPermission</code> with the
     * <code>SocketPermission(host,"resolve")</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkConnect</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      host   the host name port to connect to.
     * @param      port   the protocol port to connect to.
     * @exception  SecurityException  if the calling thread does not have
     *             permission to open a socket connection to the specified
     *               <code>host</code> and <code>port</code>.
     * @exception  NullPointerException if the <code>host</code> argument is
     *             <code>null</code>.
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkConnect(String host, int port) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * specified security context is not allowed to open a socket
     * connection to the specified host and port number.
     * <p>
     * A port number of <code>-1</code> indicates that the calling
     * method is attempting to determine the IP address of the specified
     * host name.
     * <p> If <code>context</code> is not an instance of
     * <code>AccessControlContext</code> then a
     * <code>SecurityException</code> is thrown.
     * <p>
     * Otherwise, the port number is checked. If it is not equal
     * to -1, the <code>context</code>'s <code>checkPermission</code>
     * method is called with a
     * <code>SocketPermission(host+":"+port,"connect")</code> permission.
     * If the port is equal to -1, then
     * the <code>context</code>'s <code>checkPermission</code> method
     * is called with a
     * <code>SocketPermission(host,"resolve")</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkConnect</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      host      the host name port to connect to.
     * @param      port      the protocol port to connect to.
     * @param      context   a system-dependent security context.
     * @exception  SecurityException if the specified security context
     *             is not an instance of <code>AccessControlContext</code>
     *             (e.g., is <code>null</code>), or does not have permission
     *             to open a socket connection to the specified
     *             <code>host</code> and <code>port</code>.
     * @exception  NullPointerException if the <code>host</code> argument is
     *             <code>null</code>.
     * @see        java.lang.SecurityManager#getSecurityContext()
     * @see        java.security.AccessControlContext#checkPermission(java.security.Permission)
     */
    public void checkConnect(String host, int port, Object context) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to wait for a connection request on
     * the specified local port number.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>SocketPermission("localhost:"+port,"listen")</code>.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkListen</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      port   the local port.
     * @exception  SecurityException  if the calling thread does not have
     *             permission to listen on the specified port.
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkListen(int port) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not permitted to accept a socket connection from
     * the specified host and port number.
     * <p>
     * This method is invoked for the current security manager by the
     * <code>accept</code> method of class <code>ServerSocket</code>.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>SocketPermission(host+":"+port,"accept")</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkAccept</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      host   the host name of the socket connection.
     * @param      port   the port number of the socket connection.
     * @exception  SecurityException  if the calling thread does not have
     *             permission to accept the connection.
     * @exception  NullPointerException if the <code>host</code> argument is
     *             <code>null</code>.
     * @see        java.net.ServerSocket#accept()
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkAccept(String host, int port) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to use
     * (join/leave/send/receive) IP multicast.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>java.net.SocketPermission(maddr.getHostAddress(),
     * "accept,connect")</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkMulticast</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      maddr  Internet group address to be used.
     * @exception  SecurityException  if the calling thread is not allowed to
     *  use (join/leave/send/receive) IP multicast.
     * @exception  NullPointerException if the address argument is
     *             <code>null</code>.
     * @since      1.1
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkMulticast(InetAddress maddr) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to use
     * (join/leave/send/receive) IP multicast.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>java.net.SocketPermission(maddr.getHostAddress(),
     * "accept,connect")</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkMulticast</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      maddr  Internet group address to be used.
     * @param      ttl        value in use, if it is multicast send.
     * Note: this particular implementation does not use the ttl
     * parameter.
     * @exception  SecurityException  if the calling thread is not allowed to
     *  use (join/leave/send/receive) IP multicast.
     * @exception  NullPointerException if the address argument is
     *             <code>null</code>.
     * @since      1.1
     * @deprecated Use #checkPermission(java.security.Permission) instead
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    @Deprecated(since="1.4")
    public void checkMulticast(InetAddress maddr, byte ttl) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to access or modify the system
     * properties.
     * <p>
     * This method is used by the <code>getProperties</code> and
     * <code>setProperties</code> methods of class <code>System</code>.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>PropertyPermission("*", "read,write")</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkPropertiesAccess</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @exception  SecurityException  if the calling thread does not have
     *             permission to access or modify the system properties.
     * @see        java.lang.System#getProperties()
     * @see        java.lang.System#setProperties(java.util.Properties)
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkPropertiesAccess() {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to access the system property with
     * the specified <code>key</code> name.
     * <p>
     * This method is used by the <code>getProperty</code> method of
     * class <code>System</code>.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>PropertyPermission(key, "read")</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkPropertyAccess</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param      key   a system property key.
     *
     * @exception  SecurityException  if the calling thread does not have
     *             permission to access the specified system property.
     * @exception  NullPointerException if the <code>key</code> argument is
     *             <code>null</code>.
     * @exception  IllegalArgumentException if <code>key</code> is empty.
     *
     * @see        java.lang.System#getProperty(java.lang.String)
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkPropertyAccess(String key) {
    }

    /**
     * Returns {@code true} if the calling thread has {@code AllPermission}.
     *
     * @param      window   not used except to check if it is {@code null}.
     * @return     {@code true} if the calling thread has {@code AllPermission}.
     * @exception  NullPointerException if the {@code window} argument is
     *             {@code null}.
     * @deprecated This method was originally used to check if the calling thread
     *             was trusted to bring up a top-level window. The method has been
     *             obsoleted and code should instead use {@link #checkPermission}
     *             to check {@code AWTPermission("showWindowWithoutWarningBanner")}.
     *             This method is subject to removal in a future version of Java SE.
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    @Deprecated(since="1.8", forRemoval=true)
    public boolean checkTopLevelWindow(Object window) {
        return hasAllPermission();
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to initiate a print job request.
     * <p>
     * This method calls
     * <code>checkPermission</code> with the
     * <code>RuntimePermission("queuePrintJob")</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkPrintJobAccess</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @exception  SecurityException  if the calling thread does not have
     *             permission to initiate a print job request.
     * @since   1.1
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkPrintJobAccess() {
    }

    /**
     * Throws {@code SecurityException} if the calling thread does
     * not have {@code AllPermission}.
     *
     * @since   1.1
     * @exception  SecurityException  if the calling thread does not have
     *             {@code AllPermission}
     * @deprecated This method was originally used to check if the calling
     *             thread could access the system clipboard. The method has been
     *             obsoleted and code should instead use {@link #checkPermission}
     *             to check {@code AWTPermission("accessClipboard")}.
     *             This method is subject to removal in a future version of Java SE.
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkSystemClipboardAccess() {
    }

    /**
     * Throws {@code SecurityException} if the calling thread does
     * not have {@code AllPermission}.
     *
     * @since   1.1
     * @exception  SecurityException  if the calling thread does not have
     *             {@code AllPermission}
     * @deprecated This method was originally used to check if the calling
     *             thread could access the AWT event queue. The method has been
     *             obsoleted and code should instead use {@link #checkPermission}
     *             to check {@code AWTPermission("accessEventQueue")}.
     *             This method is subject to removal in a future version of Java SE.
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkAwtEventQueueAccess() {
    }

    /**
     * Throws a {@code SecurityException} if the calling thread is not allowed
     * to access the specified package.
     * <p>
     * During class loading, this method may be called by the {@code loadClass}
     * method of class loaders and by the Java Virtual Machine to ensure that
     * the caller is allowed to access the package of the class that is
     * being loaded.
     * <p>
     * This method checks if the specified package starts with or equals
     * any of the packages in the {@code package.access} Security Property.
     * An implementation may also check the package against an additional
     * list of restricted packages as noted below. If the package is restricted,
     * {@link #checkPermission(Permission)} is called with a
     * {@code RuntimePermission("accessClassInPackage."+pkg)} permission.
     * <p>
     * If this method is overridden, then {@code super.checkPackageAccess}
     * should be called as the first line in the overridden method.
     *
     * @implNote
     * This implementation also restricts all non-exported packages of modules
     * loaded by {@linkplain ClassLoader#getPlatformClassLoader
     * the platform class loader} or its ancestors. A "non-exported package"
     * refers to a package that is not exported to all modules. Specifically,
     * it refers to a package that either is not exported at all by its
     * containing module or is exported in a qualified fashion by its
     * containing module.
     *
     * @param      pkg   the package name.
     * @throws     SecurityException  if the calling thread does not have
     *             permission to access the specified package.
     * @throws     NullPointerException if the package name argument is
     *             {@code null}.
     * @see        java.lang.ClassLoader#loadClass(String, boolean) loadClass
     * @see        java.security.Security#getProperty getProperty
     * @see        #checkPermission(Permission) checkPermission
     */
    public void checkPackageAccess(String pkg) {
    }

    /**
     * Throws a {@code SecurityException} if the calling thread is not
     * allowed to define classes in the specified package.
     * <p>
     * This method is called by the {@code loadClass} method of some
     * class loaders.
     * <p>
     * This method checks if the specified package starts with or equals
     * any of the packages in the {@code package.definition} Security
     * Property. An implementation may also check the package against an
     * additional list of restricted packages as noted below. If the package
     * is restricted, {@link #checkPermission(Permission)} is called with a
     * {@code RuntimePermission("defineClassInPackage."+pkg)} permission.
     * <p>
     * If this method is overridden, then {@code super.checkPackageDefinition}
     * should be called as the first line in the overridden method.
     *
     * @implNote
     * This implementation also restricts all non-exported packages of modules
     * loaded by {@linkplain ClassLoader#getPlatformClassLoader
     * the platform class loader} or its ancestors. A "non-exported package"
     * refers to a package that is not exported to all modules. Specifically,
     * it refers to a package that either is not exported at all by its
     * containing module or is exported in a qualified fashion by its
     * containing module.
     *
     * @param      pkg   the package name.
     * @throws     SecurityException  if the calling thread does not have
     *             permission to define classes in the specified package.
     * @throws     NullPointerException if the package name argument is
     *             {@code null}.
     * @see        java.lang.ClassLoader#loadClass(String, boolean)
     * @see        java.security.Security#getProperty getProperty
     * @see        #checkPermission(Permission) checkPermission
     */
    public void checkPackageDefinition(String pkg) {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to set the socket factory used by
     * <code>ServerSocket</code> or <code>Socket</code>, or the stream
     * handler factory used by <code>URL</code>.
     * <p>
     * This method calls <code>checkPermission</code> with the
     * <code>RuntimePermission("setFactory")</code> permission.
     * <p>
     * If you override this method, then you should make a call to
     * <code>super.checkSetFactory</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @exception  SecurityException  if the calling thread does not have
     *             permission to specify a socket factory or a stream
     *             handler factory.
     *
     * @see        java.net.ServerSocket#setSocketFactory(java.net.SocketImplFactory) setSocketFactory
     * @see        java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory) setSocketImplFactory
     * @see        java.net.URL#setURLStreamHandlerFactory(java.net.URLStreamHandlerFactory) setURLStreamHandlerFactory
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkSetFactory() {
    }

    /**
     * Throws a <code>SecurityException</code> if the
     * calling thread is not allowed to access members.
     * <p>
     * The default policy is to allow access to PUBLIC members, as well
     * as access to classes that have the same class loader as the caller.
     * In all other cases, this method calls <code>checkPermission</code>
     * with the <code>RuntimePermission("accessDeclaredMembers")
     * </code> permission.
     * <p>
     * If this method is overridden, then a call to
     * <code>super.checkMemberAccess</code> cannot be made,
     * as the default implementation of <code>checkMemberAccess</code>
     * relies on the code being checked being at a stack depth of
     * 4.
     *
     * @param clazz the class that reflection is to be performed on.
     *
     * @param which type of access, PUBLIC or DECLARED.
     *
     * @exception  SecurityException if the caller does not have
     *             permission to access members.
     * @exception  NullPointerException if the <code>clazz</code> argument is
     *             <code>null</code>.
     *
     * @deprecated This method relies on the caller being at a stack depth
     *             of 4 which is error-prone and cannot be enforced by the runtime.
     *             Users of this method should instead invoke {@link #checkPermission}
     *             directly.
     *             This method is subject to removal in a future version of Java SE.
     *
     * @see java.lang.reflect.Member
     * @since 1.1
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkMemberAccess(Class<?> clazz, int which) {
    }

    /**
     * Determines whether the permission with the specified permission target
     * name should be granted or denied.
     *
     * <p> If the requested permission is allowed, this method returns
     * quietly. If denied, a SecurityException is raised.
     *
     * <p> This method creates a <code>SecurityPermission</code> object for
     * the given permission target name and calls <code>checkPermission</code>
     * with it.
     *
     * <p> See the documentation for
     * <code>{@link java.security.SecurityPermission}</code> for
     * a list of possible permission target names.
     *
     * <p> If you override this method, then you should make a call to
     * <code>super.checkSecurityAccess</code>
     * at the point the overridden method would normally throw an
     * exception.
     *
     * @param target the target name of the <code>SecurityPermission</code>.
     *
     * @exception SecurityException if the calling thread does not have
     * permission for the requested access.
     * @exception NullPointerException if <code>target</code> is null.
     * @exception IllegalArgumentException if <code>target</code> is empty.
     *
     * @since   1.1
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    public void checkSecurityAccess(String target) {
        
    }
    
    /**
     * Returns the thread group into which to instantiate any new
     * thread being created at the time this is being called.
     * By default, it returns the thread group of the current
     * thread. This should be overridden by a specific security
     * manager to return the appropriate thread group.
     *
     * @return  ThreadGroup that new threads are instantiated into
     * @since   1.1
     * @see     java.lang.ThreadGroup
     */
    public ThreadGroup getThreadGroup() {
        return Thread.currentThread().getThreadGroup();
    }

}