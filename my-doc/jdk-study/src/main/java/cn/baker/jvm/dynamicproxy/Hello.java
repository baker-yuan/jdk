package cn.baker.jvm.dynamicproxy;

public class Hello implements IHello {
    @Override
    public String sayHi() {
        return "Hello";
    }
}