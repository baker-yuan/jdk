package cn.baker.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;

public class UserServiceProxy extends Proxy implements UserService {
    private static Method testMethod;
    static {
        try {
            testMethod = Class.forName("cn.baker.test.UserService").getMethod("test", Boolean.TYPE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    protected UserServiceProxy(InvocationHandler invocationHandler) {
        super(invocationHandler);
    }
    // 动态代理，不就是生成了这一段代码
    @Override
    public final void test(boolean throwException) {
        try {
            // 调用父类InvocationHandler的invoke方法
            // 父类的InvocationHandler也是创建UserServiceProxy传入的
            // 其实就是调用为们传入的InvocationHandler的invoke方法

            // super.h.invoke 就值执行这段代码
            // /**
            //  * @param obj    被代理对象
            //  * @param method 对象方法
            //  * @param args   方法参数
            //  */
            // @Override
            // public Object invoke(Object obj, Method method, Object[] args) throws Throwable {
            //     // 不处理
            //     if (Object.class.equals(method.getDeclaringClass())){
            //         return method.invoke(this, args);
            //     }
            //     log.info("method.invoke，before...");
            //     // 使用方法的反射
            //     Object invoke = method.invoke(target, args);
            //     log.info("method.invoke，after...");
            //     return invoke;
            // }
            super.h.invoke(this, testMethod, new Object[]{throwException});
        } catch (RuntimeException | Error var3) {
            throw var3;
        } catch (Throwable var4) {
            throw new UndeclaredThrowableException(var4);
        }
    }
}
