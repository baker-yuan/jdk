// package cn.baker.jvm.lock;
//
//
// import lombok.extern.slf4j.Slf4j;
// import sun.misc.Unsafe;
//
// import java.lang.reflect.Field;
//
// /**
//  * 自己来模拟锁
//  * 自旋来实现同步（局限）
//  *
//  *
//  * 1、什么是锁
//  * 目标：同步 多线程一前一后执行
//  *
//  * 其实就是一个标识：如果这个标识改变成了某个状态我们就理解为获取锁（正常返回）
//  * 拿不到锁其实就是陷入阻塞（死循环）让这个方法不返回
//  *
//  * @author yuanyu
//  */
// @Slf4j
// public class CustomLock {
//
//     /**
//      * status = 1 并不是原子性的 gets=0 set1cache=1 set2=0
//      */
//     private volatile int status = 0;
//     /**
//      * 主要是为了调用CAS的方法，为了获取status变量的偏移量
//      */
//     private static Unsafe unsafe = null;
//
//     /**
//      * status变量的内存偏移量（可以理解为内存地址）
//      */
//     private static long statusOffset;
//
//     static {
//         try {
//             //
//             Field field = Unsafe.class.getDeclaredField("theUnsafe");
//             field.setAccessible(true);
//             unsafe = (Unsafe) field.get(null);
//             //
//             statusOffset = unsafe.objectFieldOffset(
//                     CustomLock.class.getDeclaredField("status")
//             );
//             log.info(">>> statusOffset={}", statusOffset);
//         } catch (Exception e) {
//
//         }
//     }
//
//
//     /**
//      * 加锁
//      */
//     void lock() {
//         while (!compareAndSet(0, 1)) { // cas 原子操作
//         }
//
//         // 错误写法
//         // if (status == 0) {
//         //     status = 1; // 发生了线程上线文切换
//         //     return;
//         // } else {
//         // }
//     }
//
//     /**
//      * 解锁
//      */
//     void unlock() {
//         status = 0;
//     }
//
//     /**
//      * 原子替换
//      *
//      * @param oldVal
//      * @param newVal
//      * @return
//      */
//     private boolean compareAndSet(int oldVal, int newVal) {
//         return unsafe.compareAndSwapInt(this, statusOffset, oldVal, newVal);
//     }
//
// }
