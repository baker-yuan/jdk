package cn.baker.threadlocal;

import java.lang.ref.WeakReference;

/**
 * 弱引用有两种构造方法，多的一种只是多了一个队列
 * public WeakReference(T referent) {
 * super(referent);
 * }
 * public WeakReference(T referent, ReferenceQueue<? super T> q) {
 * super(referent, q);
 * }
 * 例如：
 * WeakReference<Apple> appleWeakReference = new WeakReference<>(apple);
 * Apple apple = appleWeakReference.get();
 * apple是弱引用对象而appleWeakReference是被弱引用对象
 */
/**
 * 篮子
 */
public class Basket extends WeakReference<Apple> {
    public Basket(Apple referent) {
        super(referent);
    }
}