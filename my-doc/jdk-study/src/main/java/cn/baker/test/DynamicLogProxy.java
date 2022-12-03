package cn.baker.test;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Slf4j
public class DynamicLogProxy implements InvocationHandler {
    // 需要代理的对象类
    private final Object target;
    public DynamicLogProxy(Object target) {
        this.target = target;
    }
    /**
     * 获取代理对象
     */
    public Object getProxyInstance(){
        Class<?> clazz = target.getClass();
        // 通过Proxy.newProxyInstance(代理类的类加载器, 代理类实现的所有接口, Handler) 加载动态代理
        return Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), this);
    }
    /**
     * @param obj    被代理对象
     * @param method 对象方法
     * @param args   方法参数
     */
    @Override
    public Object invoke(Object obj, Method method, Object[] args) throws Throwable {
        // 不处理
        if (Object.class.equals(method.getDeclaringClass())){
            return method.invoke(this, args);
        }
        log.info("method.invoke，before...");
        // 使用方法的反射
        Object invoke = method.invoke(target, args);
        log.info("method.invoke，after...");
        return invoke;
    }
}
