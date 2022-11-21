package cn.baker.collection.map;

//https://blog.csdn.net/wangshuaiwsws95/article/details/107375724/
//https://www.jianshu.com/p/b8177780c939

import java.util.HashMap;
import java.util.Hashtable;

// https://www.jianshu.com/p/9ad7fdb46efb
// https://www.iteye.com/blog/ldbjakyo-1340153
// https://zhuanlan.zhihu.com/p/90508170
//
public class MapStudyTest {
    // https://www.bilibili.com/video/BV1nJ411J7AA?p=6&spm_id_from=pageDriver&vd_source=adf8e33f74383f4a39f53f0bf2abfcdc
    public static void main(String[] args) {
        System.out.println("hello jdk.");
        HashMap<String, Object> hm = new HashMap<>(200);


        String a;

        // hm.put("1", "1");
        // Object oldValue = hm.put("1", "222");

        // 9个hash值一样的
        hm.put("3Qj", null);
        hm.put("2pj", null);
        hm.put("2qK", null);
        hm.put("2r,", null);
        hm.put("3RK", null);
        hm.put("3S,", null);
        hm.put("42j", null);
        hm.put("43K", null);
        hm.put("44,", null);

        hm.remove("44,");
        hm.get("44,");




        System.out.println("重地".hashCode());
        System.out.println("通话".hashCode());

        // 00000000 00000000 00000000 00000001
        int one = Integer.numberOfLeadingZeros(1);
        // 00000000 00000000 00000000 00000010
        int two = Integer.numberOfLeadingZeros(2);




        System.out.println(hm);




    }


}
