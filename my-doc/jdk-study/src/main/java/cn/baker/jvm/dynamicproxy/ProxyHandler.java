package cn.baker.jvm.dynamicproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 动态代理类
 */
public class ProxyHandler<T> implements InvocationHandler {

    private T origin;

    public ProxyHandler(T origin) {
        this.origin = origin;
    }

    /**
     * @param proxy  代理对象引用
     * @param method 正在执行目标的方法
     * @param args   目标方法执行时的入参
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("proxy start");
        Object result = method.invoke(origin, args);
        System.out.println("proxy end");
        return result;
    }
}