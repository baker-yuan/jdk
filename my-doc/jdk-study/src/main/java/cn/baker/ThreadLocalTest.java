package cn.baker;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * http://events.jianshu.io/p/5900eab156a8
 * @author yuanyu
 */
// FastThreadLocal
// InheritableThreadLocal
// TransmittableThreadLocal
// https://www.jianshu.com/p/3bb70ae81828
public class ThreadLocalTest {
    public static void main(String[] args) throws InterruptedException {
        // test1();
        //test2();

        // new Thread(()->{
        //     test2();
        // }, "thread1").start();
        // // TimeUnit.MICROSECONDS.sleep(11000);
        // new Thread(()->{
        //     test2();
        // }, "thread2").start();

        test3();


    }

    public static class ThreadLocalContext {
        public static InheritableThreadLocal<HashMap<String, Object>> INHERITABLE_THREAD_LOCAL = new InheritableThreadLocal<>();
        public static ThreadLocal<HashMap<String, Object>> THREAD_LOCAL = new ThreadLocal<>();
    }


    private static void test3() {
        new Thread(()->{
            HashMap<String, Object> data = new HashMap<>();
            data.put("id", Thread.currentThread().getId());
            ThreadLocalContext.INHERITABLE_THREAD_LOCAL.set(data);
            new Thread(()->{
                HashMap<String, Object> value = ThreadLocalContext.INHERITABLE_THREAD_LOCAL.get();
                System.out.println("inheritableThreadLocal: "+ value);
            }, "son").start();
        }, "father").start();


        new Thread(()->{
            HashMap<String, Object> data = new HashMap<>();
            data.put("id", Thread.currentThread().getId());
            ThreadLocalContext.THREAD_LOCAL.set(data);
            new Thread(()->{
                HashMap<String, Object> value = ThreadLocalContext.THREAD_LOCAL.get();
                System.out.println("threadLocal: "+ value);
            }, "son").start();
        }, "father").start();


    }

    private synchronized static void test2() {


        ThreadLocal<HashMap<String, Object>> threadLocal1 = new ThreadLocal<>();
        HashMap<String, Object> data = new HashMap<>();
        data.put("name", Thread.currentThread().getName());
        threadLocal1.set(data); // java.lang.ThreadLocal.ThreadLocalMap.set

        ThreadLocal<HashMap<String, Object>> threadLocal2 = new ThreadLocal<>();
        HashMap<String, Object> data2 = new HashMap<>();
        data2.put("id", Thread.currentThread().getId());
        threadLocal2.set(data2);



        HashMap<String, Object> getData = threadLocal1.get(); // java.lang.ThreadLocal.ThreadLocalMap.getEntry
        HashMap<String, Object> getData2 = threadLocal2.get();
        System.out.println(Thread.currentThread().getName() + " ------------------");
        System.out.println(getData);
        System.out.println(getData2);
        System.out.println(Thread.currentThread().getName() + " ------------------\n");

        threadLocal1.remove(); // java.lang.ThreadLocal.ThreadLocalMap.remove
        threadLocal2.remove();
    }

    private static void test1() {
        // 创建ThreadLocal
        ThreadLocal<HashMap<String, Object>> threadLocal = new ThreadLocal<>();

        // 设置当前线程绑定的局部变量
        HashMap<String, Object> data = new HashMap<>();
        threadLocal.set(data);

        // 获取当前线程绑定的局部变量
        HashMap<String, Object> getData = threadLocal.get();

        // 移除当前线程绑定的局部变量
        threadLocal.remove();
    }

}
