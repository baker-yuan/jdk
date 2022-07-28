// package cn.baker.jvm.lock;
//
// import lombok.extern.slf4j.Slf4j;
//
// import java.util.concurrent.TimeUnit;
// import java.util.concurrent.locks.LockSupport;
//
// /**
//  * 锁（对象）就是有一个标识来标识是否上锁了
//  * 如果上锁了，调用lock方法无法正常正常返回（阻塞）
//  *
//  *
//  * @author yuanyu
//  */
// @Slf4j
// public class TestNxLock {
//
//     public static void main(String[] args) throws InterruptedException {
//         // Thread t1 = new Thread(() -> {
//         //     log.debug("1");
//         //     // 让线程阻塞
//         //     LockSupport.park();
//         //     log.debug("4");
//         // });
//         // // 告诉CPU 这个t1可以调度了 但是有没有调度是随机的
//         // t1.start();
//         //
//         // TimeUnit.SECONDS.sleep(2);
//         // log.debug("3");
//         // LockSupport.unpark(t1);
//
//
//         CustomLock lock = new CustomLock();
//         Thread t1 = new Thread(() -> {
//             lock.lock();
//             log.info("t1...lock");
//             try {
//                 log.info("exec...t1");
//             } finally {
//                 lock.unlock();
//                 log.info("t1...unlock");
//             }
//         }, "t1");
//
//         Thread t2 = new Thread(() -> {
//             lock.lock();
//             log.info("t2...lock");
//             try {
//                 log.info("exec...t2");
//             } finally {
//                 lock.unlock();
//                 log.info("t2...unlock");
//             }
//         }, "t2");
//         t1.start();
//         t2.start();
//         TimeUnit.SECONDS.sleep(3);
//     }
//
//
// }
