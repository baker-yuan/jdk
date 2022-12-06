package cn.baker.syn.demo03;

/**
 * 可重入特性指的是同一个线程获得锁之后，可以直接再次获取该锁。
 */
public class Demo01 {
    public static void main(String[] args) {
        Runnable sellTicket = new Runnable() {
            @Override
            public void run() {
                synchronized (Demo01.class) {
                    System.out.println("我是run");
                    test01();
                }
            }
            public void test01() {
                synchronized (Demo01.class) {
                    System.out.println("我是test01");
                }
            }
        };
        new Thread(sellTicket).start();
        new Thread(sellTicket).start();
    }
}