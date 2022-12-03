package cn.baker.threadlocal;

import java.lang.ref.WeakReference;

/**
 * @author yuanyu
 */
public class Entry extends WeakReference<Object> {
    private Apple value;
    Entry(Object key, Apple value) {
        super(key);
        this.value = value;
    }
    public Apple getValue() {
        return value;
    }
    /**
     * 对象回收最后调用方法，回收时执行
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("Entry "+ value.getName() + " finalize");
    }
}