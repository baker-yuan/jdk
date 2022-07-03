package cn.baker;

import java.util.concurrent.ConcurrentHashMap;

/**
 * https://www.jianshu.com/p/4e03b08dc007
 * @author yuanyu
 */
public class ConcurrentHashMapTest {
    public static void main(String[] args) {
        ConcurrentHashMap<String, Object> concurrentHashMap = new ConcurrentHashMap<>();
        concurrentHashMap.put("k", "v");
        System.out.println(concurrentHashMap);

        // Jdk7ConcurrentHashMap<String, Object> jdk7ConcurrentHashMap = new Jdk7ConcurrentHashMap<>();
        // System.out.println(jdk7ConcurrentHashMap);
    }
}
