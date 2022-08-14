package cn.baker.jvm.dynamicproxy.javap;

/**
 *
 * @author yuanyu
 */

interface TestInt {
}

public class Test implements TestInt {
    private int field = 1;

    public int add(int a, int b) {
        return a + b;
    }
}