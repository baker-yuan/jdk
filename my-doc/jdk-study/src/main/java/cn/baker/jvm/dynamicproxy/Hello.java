package cn.baker.jvm.dynamicproxy;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Hello implements IHello {

    @Override
    public String sayHi(String a, Integer b) throws FileNotFoundException {
        // this.sayMY();
        return "Hello";
    }

    // public String sayMY() {
    //     System.out.println("My is Hello");
    //     return "My is Hello";
    // }


}