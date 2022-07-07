package cn.baker.collection.map;

import java.util.concurrent.ConcurrentHashMap;

/**
 * https://www.jianshu.com/p/4e03b08dc007
 * @author yuanyu
 */
public class ConcurrentHashMapTest {
    public static void main(String[] args) {
        ConcurrentHashMap<Integer, String> concurrentHashMap = new ConcurrentHashMap<>();
        for (int i = 0; i < 1000; i++) {
            // i = 11 开始扩容(addCount里面)
            concurrentHashMap.put(i, "v");
        }

        System.out.println(concurrentHashMap);

        // Jdk7ConcurrentHashMap<String, Object> jdk7ConcurrentHashMap = new Jdk7ConcurrentHashMap<>();
        // jdk7ConcurrentHashMap.put("k", "v");
        // Object k = jdk7ConcurrentHashMap.get("k");
        // System.out.println(jdk7ConcurrentHashMap);
    }
}
