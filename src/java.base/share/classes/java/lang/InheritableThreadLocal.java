/*
 * Copyright (c) 1998, 2012, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.ref.*;

/**
 * This class extends {@code ThreadLocal} to provide inheritance of values
 * from parent thread to child thread: when a child thread is created, the
 * child receives initial values for all inheritable thread-local variables
 * for which the parent has values.  Normally the child's values will be
 * identical to the parent's; however, the child's value can be made an
 * arbitrary function of the parent's by overriding the {@code childValue}
 * method in this class.
 *
 * <p>Inheritable thread-local variables are used in preference to
 * ordinary thread-local variables when the per-thread-attribute being
 * maintained in the variable (e.g., User ID, Transaction ID) must be
 * automatically transmitted to any child threads that are created.
 *
 * <p>Note: During the creation of a new {@link
 * Thread#Thread(ThreadGroup,Runnable,String,long,boolean) thread}, it is
 * possible to <i>opt out</i> of receiving initial values for inheritable
 * thread-local variables.
 *
 * @author  Josh Bloch and Doug Lea
 * @see     ThreadLocal
 * @since   1.2
 */

/**
 * InheritableThreadLocal主要用于子线程创建时，需要自动继承父线程的ThreadLocal变量，方便必要信息的进一步传递。
 *
 * 但是InheritableThreadLocal的继承性是在new Thread创建子线程时候在构造函数内把父线程内线程变量拷贝到子线程内部的。
 * 为了不在创建新线程耗费资源，我们一般会用线程池，线程池的线程会复用，那么线程中的ThreadLocal便不对了，可能是旧的，因为线程是旧的。
 */
public class InheritableThreadLocal<T> extends ThreadLocal<T> {
    /**
     * Computes the child's initial value for this inheritable thread-local
     * variable as a function of the parent's value at the time the child
     * thread is created.  This method is called from within the parent
     * thread before the child is started.
     * <p>
     * This method merely returns its input argument, and should be overridden
     * if a different behavior is desired.
     *
     * @param parentValue the parent thread's value
     * @return the child thread's initial value
     */
    /**
     * 该函数在父线程创建子线程，向子线程复制InheritableThreadLocal变量时使用
     * @param parentValue the parent thread's value
     * @return the child thread's initial value
     */
    protected T childValue(T parentValue) {
        return parentValue;
    }

    /**
     * Get the map associated with a ThreadLocal.
     *
     * @param t the current thread
     */
    /**
     * 由于重写了getMap，操作InheritableThreadLocal时，
     * 将只影响Thread类中的inheritableThreadLocals变量，
     * 与threadLocals变量不再有关系
     * @param t the current thread
     * @return ThreadLocalMap
     */
    ThreadLocalMap getMap(Thread t) {
       return t.inheritableThreadLocals;
    }

    /**
     * Create the map associated with a ThreadLocal.
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the table.
     */
    /**
     * 类似于getMap，操作InheritableThreadLocal时，
     * 将只影响Thread类中的inheritableThreadLocals变量，
     * 与threadLocals变量不再有关系
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the table.
     */
    void createMap(Thread t, T firstValue) {
        t.inheritableThreadLocals = new ThreadLocalMap(this, firstValue);
    }
}
