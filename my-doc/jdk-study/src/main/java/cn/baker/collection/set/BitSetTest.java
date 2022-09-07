package cn.baker.collection.set;


import java.util.BitSet;

public class BitSetTest {


    /**
     * https://www.jianshu.com/p/da990cd30715
     * https://blog.csdn.net/jiangnan2014/article/details/53735429
     * https://blog.csdn.net/a372663325/article/details/106665227
     * https://blog.csdn.net/hebian1994/article/details/126355375
     * https://blog.csdn.net/LB_Captain/article/details/125187158
     * https://www.cnblogs.com/jasonkoo/articles/2213768.html
     *
     * @param args
     */
    public static void main(String[] args) {
        // long l = 0L | (1L << 1);
        // System.out.println(Long.toBinaryString(l));
        long l2 = 0L | (1L << 6);
        System.out.println(Long.toBinaryString(l2));

        BitSet bs = new BitSet();
        bs.set(0);
        bs.set(1);
        bs.set(6);


        // String s = Long.toBinaryStringV2(6);

        //String binaryString = bs.toBinaryString();


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
