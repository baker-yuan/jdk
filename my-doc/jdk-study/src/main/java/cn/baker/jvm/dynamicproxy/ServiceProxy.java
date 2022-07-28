package cn.baker.jvm.dynamicproxy;


import java.lang.reflect.Proxy;

public class ServiceProxy {


    public static void main(String[] args) {
        IHello IHello = (IHello) getInstance(IHello.class, new ProxyHandler<>(new Hello()));

        System.out.println(IHello.toString());

        // generateProxyClass();
    }

    // 创建代理对象
    public static <T> Object getInstance(Class<T> clazz, ProxyHandler<T> handler) {
        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, handler);
    }

    // private static void generateProxyClass() {
    //     byte[] classFile = ProxyGenerator.generateProxyClass("$Proxy0", Hello.class.getInterfaces());
    //     String path = "/Users/nathan.yang/workspace/algorithm_Java/out/StuProxy.class";
    //     try (FileOutputStream fos = new FileOutputStream(path)) {
    //         fos.write(classFile);
    //         fos.flush();
    //         System.out.println("代理类文件写入成功");
    //     } catch (Exception e) {
    //         System.out.println("写文件错误");
    //     }
    // }
}