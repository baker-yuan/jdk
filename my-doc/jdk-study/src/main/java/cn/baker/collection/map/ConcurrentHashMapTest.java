package cn.baker.collection.map;

import java.util.concurrent.ConcurrentHashMap;

/**
 * https://www.jianshu.com/p/4e03b08dc007
 * @author yuanyu
 */
public class ConcurrentHashMapTest {
    public static void main(String[] args) {
        ConcurrentHashMap<Integer, Object> concurrentHashMap = new ConcurrentHashMap<>();
        concurrentHashMap.put(0, "v");
        System.out.println(concurrentHashMap);

        // Jdk7ConcurrentHashMap<String, Object> jdk7ConcurrentHashMap = new Jdk7ConcurrentHashMap<>();
        // jdk7ConcurrentHashMap.put("k", "v");
        // Object k = jdk7ConcurrentHashMap.get("k");
        // System.out.println(jdk7ConcurrentHashMap);
    }
}
