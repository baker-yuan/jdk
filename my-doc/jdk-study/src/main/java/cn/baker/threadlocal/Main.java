package cn.baker.threadlocal;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        test1();
        test2();
        test3();
        Thread currentThread = Thread.currentThread();
        System.gc();
        Thread.sleep(5000);

        ThreadLocal<Object> threadLocal = new ThreadLocal<Object>();
        threadLocal.set(new Apple("白苹果"));

        System.out.println(currentThread);
    }

    public static void test1() {
        ThreadLocal<Object> threadLocal = new ThreadLocal<Object>();
        threadLocal.set(new Apple("黑苹果"));
    }
    public static void test2() {
        ThreadLocal<Object> threadLocal = new ThreadLocal<Object>();
        threadLocal.set(new Apple("红苹果"));
    }
    public static void test3() {
        ThreadLocal<Object> threadLocal = new ThreadLocal<Object>();
        threadLocal.set(new Apple("黄苹果"));
    }




    // public static ExecutorService threadPool = Executors.newFixedThreadPool(10);
    // public static void main(String[] args) {
    //     for (int i = 0; i < 1000; i++) {
    //         int finalI = i;
    //         threadPool.submit(() -> {
    //             String date = date(finalI);
    //             System.out.println(date);
    //         });
    //     }
    //     threadPool.shutdown();
    // }
    // public static String date(int seconds) {
    //     //参数的单位是毫秒，从1970.1.1 00:00:00 GMT计时
    //     Date date = new Date(1000L * seconds);
    //     SimpleDateFormat dateFormat = ThreadSafeFormatter.dateFormatThreadLocal.get();
    //     return dateFormat.format(date);
    // }

    // public static void main(String[] args) throws InterruptedException {
        // // 虚拟机参数-XX:+PrintGCDetails，输出gc信息
        //
        // Basket basket = new Basket(new Apple("红富士"));
        //
        // // 通过WeakReference调用Apple
        // // Apple apple = basket.get(); // 强引用
        // System.out.println("Apple1 " + basket.get());
        //
        // System.gc();
        //
        // Thread.sleep(5000);
        // // 如果为空，代表被回收了
        // if (basket.get() == null) {
        //     System.out.println("clear Apple。");


        // HashMap<String, Basket> basketMap = new HashMap<>();
        // basketMap.put("篮子1", new Basket(new Apple("红富士")));
        // basketMap.put("篮子2", new Basket(new Apple("黑富士")));
        // // Apple apple = basketMap.get("篮子1").get();
        // System.gc();
        // Thread.sleep(5000);

        // Apple apple1 = new Apple("红富士");
        // Apple apple2 = new Apple("黑富士");
        // ArrayList<Entry> entryList = new ArrayList<>();
        // entryList.add(new Entry("app1", apple1));
        // entryList.add(new Entry("app2", apple2));
        // entryList = null;
        // apple1 = null;
        // System.gc();
        // Thread.sleep(5000);

        // entryList.forEach(vo-> System.out.println(vo.get() + " -> " + vo.getValue()));

    // }

}
