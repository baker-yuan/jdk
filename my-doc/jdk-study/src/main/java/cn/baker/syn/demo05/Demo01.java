package cn.baker.syn.demo05;

public class Demo01 {
    private static Object obj = new Object();
    public static void main(String[] args) {
        synchronized (obj) {
            System.out.println("1");
        }
    }
    public synchronized void test() {
        System.out.println("a");
    }
}