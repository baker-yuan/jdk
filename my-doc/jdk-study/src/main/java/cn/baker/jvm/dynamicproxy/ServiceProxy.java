package cn.baker.jvm.dynamicproxy;


import java.lang.reflect.Proxy;

/**
 * https://zhuanlan.zhihu.com/p/355575054
 */
public class ServiceProxy {


    /**
     * -Djdk.proxy.debug=debug
     * jdk.proxy.ProxyGenerator.saveGeneratedFiles
     * @param args
     */
    public static void main(String[] args) {


        Hello origin = new Hello();
        IHello IHello = (IHello) getInstance(IHello.class, new ProxyHandler<>(origin));

        IHello.sayHi();
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