/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

/**
 * A task that can be scheduled for one-time or repeated execution by a
 * {@link Timer}.
 *
 * <p>A timer task is <em>not</em> reusable.  Once a task has been scheduled
 * for execution on a {@code Timer} or cancelled, subsequent attempts to
 * schedule it for execution will throw {@code IllegalStateException}.
 *
 * @author  Josh Bloch
 * @since   1.3
 */

/**
 * TimerTask是一个任务抽象类，实现了Runnable接口，是可被线程执行的。
 */
public abstract class TimerTask implements Runnable {

    //---------------------------------------------------------------------
    // 成员变量
    //---------------------------------------------------------------------
    /**
     * 当一个TimerTask对象创建后，其初始状态为VIRGIN；
     * 当调用Timer的schedule方法调度了此TimerTask对象后，其状态变更为SCHEDULED；
     * 如果TimerTask是一次性任务，此任务执行后，状态将变为EXECUTED，可重复执行任务执行后状态不变；
     * 当中途调用了TimerTask.cancel方法，该任务的状态将变为CANCELLED。
     */
    /**
     * 任务状态，初始状态为待未调度状态
     *
     * The state of this task, chosen from the constants below.
     */
    int state = VIRGIN;
    /**
     * 未调度状态
     *
     * This task has not yet been scheduled.
     */
    static final int VIRGIN = 0;
    /**
     * 任务已调度，但未执行
     * java.util.Timer#sched(java.util.TimerTask, long, long)
     *
     * This task is scheduled for execution.  If it is a non-repeating task,
     * it has not yet been executed.
     */
    static final int SCHEDULED = 1;
    /**
     * 若是一次性任务表示已执行，可重复执行任务，该状态无效
     *
     * This non-repeating task has already executed (or is currently
     * executing) and has not been cancelled.
     */
    static final int EXECUTED = 2;
    /**
     * 任务被取消 cancel()
     *
     * This task has been cancelled (with a call to TimerTask.cancel).
     */
    static final int CANCELLED = 3;




    /**
     * 任务的下一次执行时间点
     *
     * Next execution time for this task in the format returned by
     * System.currentTimeMillis, assuming this task is scheduled for execution.
     * For repeating tasks, this field is updated prior to each task execution.
     */
    long nextExecutionTime;

    /**
     * 任务执行的时间间隔。正数表示固定速率；负数表示固定时延；0表示只执行一次
     *
     * Period in milliseconds for repeating tasks.  A positive value indicates
     * fixed-rate execution.  A negative value indicates fixed-delay execution.
     * A value of 0 indicates a non-repeating task.
     */
    long period = 0;


    /**
     * 用于加锁控制多线程修改TimerTask内部状态
     *
     * This object is used to control access to the TimerTask internals.
     */
    final Object lock = new Object();



    //---------------------------------------------------------------------
    // 构造函数
    //---------------------------------------------------------------------

    /**
     * Creates a new timer task.
     */
    protected TimerTask() {
    }



    //---------------------------------------------------------------------
    // 方法
    //---------------------------------------------------------------------

    /**
     * 实现了Runnable接口，创建TimerTask需要重写此方法，编写任务执行代码
     *
     * The action to be performed by this timer task.
     */
    public abstract void run();

    /**
     * 取消任务
     *
     * Cancels this timer task.  If the task has been scheduled for one-time
     * execution and has not yet run, or has not yet been scheduled, it will
     * never run.  If the task has been scheduled for repeated execution, it
     * will never run again.  (If the task is running when this call occurs,
     * the task will run to completion, but will never run again.)
     *
     * <p>Note that calling this method from within the {@code run} method of
     * a repeating timer task absolutely guarantees that the timer task will
     * not run again.
     *
     * <p>This method may be called repeatedly; the second and subsequent
     * calls have no effect.
     *
     * @return true if this task is scheduled for one-time execution and has
     *         not yet run, or this task is scheduled for repeated execution.
     *         Returns false if the task was scheduled for one-time execution
     *         and has already run, or if the task was never scheduled, or if
     *         the task was already cancelled.  (Loosely speaking, this method
     *         returns {@code true} if it prevents one or more scheduled
     *         executions from taking place.)
     */
    public boolean cancel() {
        // 在cancel方法内，使用synchronized加锁，这是因为Timer内部的线程会对TimerTask状态进行修改，而调用cancel方法一般会是另外一个线程。
        // 为了避免线程同步问题，cancel在修改状态前进行了加锁操作。
        // 调用cancel方法将会把任务状态变更为CANCELLED状态，即任务取消状态，并返回一个布尔值，该布尔值表示此任务之前是否已是SCHEDULED 已调度状态。
        synchronized(lock) {
            boolean result = (state == SCHEDULED);
            state = CANCELLED;
            return result;
        }
    }

    /**
     * 计算执行时间点
     *
     * Returns the <i>scheduled</i> execution time of the most recent
     * <i>actual</i> execution of this task.  (If this method is invoked
     * while task execution is in progress, the return value is the scheduled
     * execution time of the ongoing task execution.)
     *
     * <p>This method is typically invoked from within a task's run method, to
     * determine whether the current execution of the task is sufficiently
     * timely to warrant performing the scheduled activity:
     * <pre>{@code
     *   public void run() {
     *       if (System.currentTimeMillis() - scheduledExecutionTime() >=
     *           MAX_TARDINESS)
     *               return;  // Too late; skip this execution.
     *       // Perform the task
     *   }
     * }</pre>
     * This method is typically <i>not</i> used in conjunction with
     * <i>fixed-delay execution</i> repeating tasks, as their scheduled
     * execution times are allowed to drift over time, and so are not terribly
     * significant.
     *
     * @return the time at which the most recent execution of this task was
     *         scheduled to occur, in the format returned by Date.getTime().
     *         The return value is undefined if the task has yet to commence
     *         its first execution.
     * @see Date#getTime()
     */
    public long scheduledExecutionTime() {
        // 该方法返回此任务的下次执行时间点。
        synchronized(lock) {
            // period
            // 1、正数表示固定速率
            // 2、负数表示固定时延
            // 3、0表示只执行一次
            if (period < 0) {
                return nextExecutionTime + period;
            }
            // >= 0
            else {
                nextExecutionTime - period;
            }
            // return (period < 0 ? nextExecutionTime + period : nextExecutionTime - period);
        }
    }
}
