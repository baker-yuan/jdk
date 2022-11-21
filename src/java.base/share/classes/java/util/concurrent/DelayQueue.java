/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An unbounded {@linkplain BlockingQueue blocking queue} of
 * {@code Delayed} elements, in which an element can only be taken
 * when its delay has expired.  The <em>head</em> of the queue is that
 * {@code Delayed} element whose delay expired furthest in the
 * past.  If no delay has expired there is no head and {@code poll}
 * will return {@code null}. Expiration occurs when an element's
 * {@code getDelay(TimeUnit.NANOSECONDS)} method returns a value less
 * than or equal to zero.  Even though unexpired elements cannot be
 * removed using {@code take} or {@code poll}, they are otherwise
 * treated as normal elements. For example, the {@code size} method
 * returns the count of both expired and unexpired elements.
 * This queue does not permit null elements.
 *
 * <p>This class and its iterator implement all of the <em>optional</em>
 * methods of the {@link Collection} and {@link Iterator} interfaces.
 * The Iterator provided in method {@link #iterator()} is <em>not</em>
 * guaranteed to traverse the elements of the DelayQueue in any
 * particular order.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this queue
 */

/**
 * https://zhuanlan.zhihu.com/p/443335543
 * https://blog.csdn.net/piaoranyuji/article/details/124042408
 * https://blog.csdn.net/c15158032319/article/details/118636233
 *
 * 一、DelayQueue是什么
 * DelayQueue是一个无界的BlockingQueue，用于放置实现了Delayed接口的对象，其中的对象只能在其到期时才能从队列中取走。
 * 这种队列是有序的，即队头对象的延迟到期时间最长。注意：不能将null元素放置到这种队列中。
 *
 * 二、方法
 * 2.1、插入
 * add          抛异常
 * offer        返回值
 * put          一直阻塞
 * offer(time)  超时退出
 *
 * 2.2、移除
 * remove      抛异常
 * poll        返回值
 * take        一直阻塞
 * poll(time)  超时退出
 *
 * 2.3、检测
 * element      抛异常
 * peek         返回值
 *
 * 三、DelayQueue基本原理
 * DelayQueue是一个没有边界BlockingQueue实现，加入其中的元素必需实现Delayed接口。
 * 当生产者线程调用put之类的方法加入元素时，会触发Delayed接口中的compareTo方法进行排序，
 * 也就是说队列中元素的顺序是按到期时间排序的，而非它们进入队列的顺序。
 * 排在队列头部的元素是最早到期的，越往后到期时间越晚。
 *
 * 消费者线程查看队列头部的元素，注意是查看不是取出。然后调用元素的getDelay方法，
 * 如果此方法返回的值小０或者等于０，则消费者线程会从队列中取出此元素，并进行处理。如果getDelay方法返回的值大于0，
 * 则消费者线程wait返回的时间值后，再从队列头部取出元素，此时元素应该已经到期。
 *
 * DelayQueue是Leader-Followr模式的变种，消费者线程处于等待状态时，
 * 总是等待最先到期的元素，而不是长时间的等待。消费者线程尽量把时间花在处理任务上，最小化空等的时间，以提高线程的利用效率。
 *
 *
 *
 */

/**
 * https://zhuanlan.zhihu.com/p/358776619
 * https://www.cnblogs.com/duzouzhe/archive/2009/09/28/1575813.html
 *
 *
 */
public class DelayQueue<E extends Delayed> extends AbstractQueue<E> implements BlockingQueue<E> {

    /**
     * 优先队列 底层使用堆实现
     */
    private final PriorityQueue<E> q = new PriorityQueue<E>();

    /**
     * 等待头部元素到期的线程，但是这个线程不干活（leader），单纯是盯着要到期的元素，到期了唤起其他线程干活
     *
     * Thread designated to wait for the element at the head of
     * the queue.  This variant of the Leader-Follower pattern
     * (http://www.cs.wustl.edu/~schmidt/POSA/POSA2/) serves to
     * minimize unnecessary timed waiting.  When a thread becomes
     * the leader, it waits only for the next delay to elapse, but
     * other threads await indefinitely.  The leader thread must
     * signal some other thread before returning from take() or
     * poll(...), unless some other thread becomes leader in the
     * interim.  Whenever the head of the queue is replaced with
     * an element with an earlier expiration time, the leader
     * field is invalidated by being reset to null, and some
     * waiting thread, but not necessarily the current leader, is
     * signalled.  So waiting threads must be prepared to acquire
     * and lose leadership while waiting.
     */
    private Thread leader;



    /**
     * 锁
     *
     * offer
     * poll
     */
    private final transient ReentrantLock lock = new ReentrantLock();

    /**
     * https://blog.csdn.net/qq_40794973/article/details/104111726#t11
     *
     * 作用
     * 当线程1需要等待某个条件的时候，它就去执行condition.await()方法，一旦执行了await()方法，线程就会进入阻塞状态。
     * 然后通常会有另外—个线程，假设是线程2，去执行对应的条件，直到这个条件达成的时候，线程2就会去执行condition.signal()方法，
     * 这时JVM就会从被阻塞的线程里找，找到那些等待该condition的线程，当线程1收到可执行信号的时候，它的线程状态就会变成Runnable可执行状态。
     *
     * Condition signalled when a newer element becomes available
     * at the head of the queue or a new thread may need to
     * become leader.
     */
    private final Condition available = lock.newCondition();


    //---------------------------------------------------------------------
    // 构造函数
    //---------------------------------------------------------------------

    /**
     * Creates a new {@code DelayQueue} that is initially empty.
     */
    public DelayQueue() {}

    /**
     * Creates a {@code DelayQueue} initially containing the elements of the
     * given collection of {@link Delayed} instances.
     *
     * @param c the collection of elements to initially contain
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    public DelayQueue(Collection<? extends E> c) {
        this.addAll(c);
    }



    //---------------------------------------------------------------------
    // 添加元素
    //---------------------------------------------------------------------

    /**
     * Inserts the specified element into this delay queue.
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        return offer(e);
    }

    /**
     * Inserts the specified element into this delay queue.
     *
     * @param e the element to add
     * @return {@code true}
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            q.offer(e);
            // 添加的元素是最先到期的
            if (q.peek() == e) {
                //
                leader = null;
                //
                available.signal();
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Inserts the specified element into this delay queue. As the queue is
     * unbounded this method will never block.
     *
     * @param e the element to add
     * @throws NullPointerException {@inheritDoc}
     */
    public void put(E e) {
        offer(e);
    }

    /**
     * Inserts the specified element into this delay queue. As the queue is
     * unbounded this method will never block.
     *
     * @param e the element to add
     * @param timeout This parameter is ignored as the method never blocks
     * @param unit This parameter is ignored as the method never blocks
     * @return {@code true}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e);
    }




    //---------------------------------------------------------------------
    // 取出元素
    // remove      抛异常
    // poll        返回值
    // take        一直阻塞
    // poll(time)  超时退出
    //---------------------------------------------------------------------
    /**
     * 返回值
     *
     * Retrieves and removes the head of this queue, or returns {@code null}
     * if this queue has no elements with an expired delay.
     *
     * @return the head of this queue, or {@code null} if this
     *         queue has no elements with an expired delay
     */
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 获取最先到期的那个元素
            E first = q.peek();
            // 如果这个元素以及到时间了，就返回这个元素
            return (first == null || first.getDelay(NANOSECONDS) > 0)
                ? null
                : q.poll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 一直阻塞
     *
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element with an expired delay is available on this queue.
     *
     * @return the head of this queue
     * @throws InterruptedException {@inheritDoc}
     */
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();

        try {

            for (;;) {
                E first = q.peek();
                if (first == null) {
                    // 没有元素，直接睡大觉，到时候有人添加元素会唤起我的
                    available.await();
                }
                else {
                    long delay = first.getDelay(NANOSECONDS);
                    if (delay <= 0L) {
                        // 消息以及过期，可以拿出来了
                        return q.poll();
                    }


                    first = null; // don't retain ref while waiting
                    if (leader != null) {
                        // 以及有人盯着队列的元素，我先休息让他盯着队列，到时候队列首位元素到期后，他们会通知我的
                        available.await();
                    }
                    // 没有leader，我得顶上去，盯着队列看谁要过期了，到时候到期了，直接唤起其他线程干活
                    else {
                        Thread thisThread = Thread.currentThread();
                        // 我升级为leader
                        // 皇帝轮流做，今年到我家
                        leader = thisThread;
                        try {
                            // 休眠
                            available.awaitNanos(delay);
                        } finally {
                            // 一觉醒来，队列头的那个元素到期了，我去要叫其他人起来干活了，并且我的任务也完成了，我需要休息
                            if (leader == thisThread) {
                                // 我现在要排队等着干活了，我不在是leader
                                leader = null;
                            }
                        }
                    }
                }
            }

        } finally {
            //
            if (leader == null && q.peek() != null) {
                // 唤醒其他人起来干活了
                available.signal();
            }
            //
            lock.unlock();
        }
    }

    /**
     * 超时退出
     *
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element with an expired delay is available on this queue,
     * or the specified wait time expires.
     *
     * @return the head of this queue, or {@code null} if the
     *         specified waiting time elapses before an element with
     *         an expired delay becomes available
     * @throws InterruptedException {@inheritDoc}
     */
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (;;) {
                E first = q.peek();
                if (first == null) {
                    if (nanos <= 0L) {
                        return null;
                    }
                    else {
                        nanos = available.awaitNanos(nanos);
                    }
                } else {
                    long delay = first.getDelay(NANOSECONDS);
                    if (delay <= 0L) {
                        return q.poll();
                    }
                    if (nanos <= 0L) {
                        return null;
                    }
                    first = null; // don't retain ref while waiting
                    if (nanos < delay || leader != null) {
                        nanos = available.awaitNanos(nanos);
                    }
                    else {
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread;
                        try {
                            long timeLeft = available.awaitNanos(delay);
                            nanos -= delay - timeLeft;
                        } finally {
                            if (leader == thisThread) {
                                leader = null;
                            }
                        }
                    }
                }
            }
        } finally {
            if (leader == null && q.peek() != null) {
                available.signal();
            }
            lock.unlock();
        }
    }


    //---------------------------------------------------------------------
    // 查看元素
    //---------------------------------------------------------------------

    /**
     * Retrieves, but does not remove, the head of this queue, or
     * returns {@code null} if this queue is empty.  Unlike
     * {@code poll}, if no expired elements are available in the queue,
     * this method returns the element that will expire next,
     * if one exists.
     *
     * @return the head of this queue, or {@code null} if this
     *         queue is empty
     */
    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.peek();
        } finally {
            lock.unlock();
        }
    }




    //---------------------------------------------------------------------
    // 其他方法
    //---------------------------------------------------------------------


    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c, int maxElements) {
        Objects.requireNonNull(c);
        if (c == this)
            throw new IllegalArgumentException();
        if (maxElements <= 0)
            return 0;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = 0;
            for (E first;
                 n < maxElements
                     && (first = q.peek()) != null
                     && first.getDelay(NANOSECONDS) <= 0;) {
                c.add(first);   // In this order, in case add() throws.
                q.poll();
                ++n;
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Atomically removes all of the elements from this delay queue.
     * The queue will be empty after this call returns.
     * Elements with an unexpired delay are not waited for; they are
     * simply discarded from the queue.
     */
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            q.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Always returns {@code Integer.MAX_VALUE} because
     * a {@code DelayQueue} is not capacity constrained.
     *
     * @return {@code Integer.MAX_VALUE}
     */
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    /**
     * Returns an array containing all of the elements in this queue.
     * The returned array elements are in no particular order.
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this queue.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this queue
     */
    public Object[] toArray() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.toArray();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns an array containing all of the elements in this queue; the
     * runtime type of the returned array is that of the specified array.
     * The returned array elements are in no particular order.
     * If the queue fits in the specified array, it is returned therein.
     * Otherwise, a new array is allocated with the runtime type of the
     * specified array and the size of this queue.
     *
     * <p>If this queue fits in the specified array with room to spare
     * (i.e., the array has more elements than this queue), the element in
     * the array immediately following the end of the queue is set to
     * {@code null}.
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>The following code can be used to dump a delay queue into a newly
     * allocated array of {@code Delayed}:
     *
     * <pre> {@code Delayed[] a = q.toArray(new Delayed[0]);}</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the queue are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this queue
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this queue
     * @throws NullPointerException if the specified array is null
     */
    public <T> T[] toArray(T[] a) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.toArray(a);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes a single instance of the specified element from this
     * queue, if it is present, whether or not it has expired.
     */
    public boolean remove(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.remove(o);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Identity-based version for use in Itr.remove.
     */
    void removeEQ(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (Iterator<E> it = q.iterator(); it.hasNext(); ) {
                if (o == it.next()) {
                    it.remove();
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns an iterator over all the elements (both expired and
     * unexpired) in this queue. The iterator does not return the
     * elements in any particular order.
     *
     * <p>The returned iterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * @return an iterator over the elements in this queue
     */
    public Iterator<E> iterator() {
        return new Itr(toArray());
    }

    /**
     * Snapshot iterator that works off copy of underlying q array.
     */
    private class Itr implements Iterator<E> {
        final Object[] array; // Array of all elements
        int cursor;           // index of next element to return
        int lastRet;          // index of last element, or -1 if no such

        Itr(Object[] array) {
            lastRet = -1;
            this.array = array;
        }

        public boolean hasNext() {
            return cursor < array.length;
        }

        @SuppressWarnings("unchecked")
        public E next() {
            if (cursor >= array.length)
                throw new NoSuchElementException();
            return (E)array[lastRet = cursor++];
        }

        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            removeEQ(array[lastRet]);
            lastRet = -1;
        }
    }

}
