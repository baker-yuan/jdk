package cn.baker.jvm.dynamicproxy;


import java.io.FileNotFoundException;

public interface IHello {
    String sayHi(String a, Integer b) throws FileNotFoundException;
}