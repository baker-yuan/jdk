package cn.baker.jvm.dynamicproxy;

public class Hello implements IHello {
    @Override
    public String sayHi() {
        this.sayMY();
        return "Hello";
    }

    public String sayMY() {
        System.out.println("My is Hello");
        return "My is Hello";
    }
}