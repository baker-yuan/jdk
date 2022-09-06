package cn.baker.collection.set;



import sun.jvm.hotspot.utilities.BitMap;

import java.util.BitSet;

public class BitSetTest {
    /**
     * https://www.jianshu.com/p/da990cd30715
     * https://blog.csdn.net/jiangnan2014/article/details/53735429
     * @param args
     */
    public static void main(String[] args) {

        BitSet bs = new BitSet();

        // BitSet在索引0处的值为false
        // 因为BitSet对象默认所有的索引都为false
        System.out.println(bs.get(0));

        // BitSet设置索引0处的值变为true,set的参数是索引位置
        bs.set(0);
        // 输出设置后的结果，变为true
        System.out.println(bs.get(0));

        //默认所有的索引都为false，参数是不管是几，结果都是false;
        System.out.println(bs.get(2));

        System.out.println();
    }
}
