/*
 * Copyright (c) 1995, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * This class implements a vector of bits that grows as needed. Each
 * component of the bit set has a {@code boolean} value. The
 * bits of a {@code BitSet} are indexed by nonnegative integers.
 * Individual indexed bits can be examined, set, or cleared. One
 * {@code BitSet} may be used to modify the contents of another
 * {@code BitSet} through logical AND, logical inclusive OR, and
 * logical exclusive OR operations.
 *
 * <p>By default, all bits in the set initially have the value
 * {@code false}.
 *
 * <p>Every bit set has a current size, which is the number of bits
 * of space currently in use by the bit set. Note that the size is
 * related to the implementation of a bit set, so it may change with
 * implementation. The length of a bit set relates to logical length
 * of a bit set and is defined independently of implementation.
 *
 * <p>Unless otherwise noted, passing a null parameter to any of the
 * methods in a {@code BitSet} will result in a
 * {@code NullPointerException}.
 *
 * <p>A {@code BitSet} is not safe for multithreaded use without
 * external synchronization.
 *
 * @author  Arthur van Hoff
 * @author  Michael McCloskey
 * @author  Martin Buchholz
 * @since   1.0
 */
/**
 * https://blog.csdn.net/qq_41243873/article/details/119787610
 * https://blog.csdn.net/z69183787/article/details/70208656
 */

/**
 * BitSet简介
 * 1、
 * 2、
 * 3、默认情况下，BitSet中所有位的初始值都是0(false)。
 * 4、除非另行说明，否则将 null 参数传递给BitSet中的任何方法都将导致NullPointerException。
 * 5、在没有外部同步的情况下，多个线程操作一个BitSet是不安全的
 *
 * 基本原理
 * BitSet是位操作的对象，值只有0或1即false和true，内部维护了一个long数组，初始只有一个long，所以BitSet最小的size是64，当随着存储的元素越来越多，BitSet内部会动态扩充，最终内部是由N个long来存储，这些针对操作都是透明的。
 * 用1位来表示一个数据是否出现过，0为没有出现过，1表示出现过。使用用的时候既可根据某一个是否为0表示，此数是否出现过。
 * 一个1G的空间，有 8*1024*1024*1024=8.58*10^9bit，也就是可以表示85亿个不同的数
 *
 * 使用场景
 *
 *
 *
 *
 */
public class BitSet implements Cloneable, java.io.Serializable {

    /*
     * BitSets are packed into arrays of "words."  Currently a word is
     * a long, which consists of 64 bits, requiring 6 address bits.
     * The choice of word size is determined purely by performance concerns.
     */
    /**
     * ADDRESS_BITS_PER_WORD: 固定值为6，主要用作位运算的常量
     *
     * x >> 6 相当于除以64，由于long类型是64，这个运算是为了定位数组位置
     * x << 6 相当于乘以64，由于long类型是64，这个运算是为了定位数组位置
     */
    private static final int ADDRESS_BITS_PER_WORD = 6;
    /**
     * BITS_PER_WORD：值为64，作为长度计算值
     */
    private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    /**
     * BIT_INDEX_MASK：索引掩码
     */
    private static final int BIT_INDEX_MASK = BITS_PER_WORD - 1;

    /**
     * Used to shift left or right for a partial word mask
     */
    private static final long WORD_MASK = 0xffffffffffffffffL;

    /**
     * @serialField bits long[]
     *
     * The bits in this BitSet.  The ith bit is stored in bits[i/64] at
     * bit position i % 64 (where bit position 0 refers to the least
     * significant bit and 63 refers to the most significant bit).
     */
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("bits", long[].class),
    };

    /**
     * Bitset位图，其中最核心的部分是words数组，也就是桶位，每个桶的存放类型为long类型
     *
     * The internal field corresponding to the serialField "bits".
     */
    private long[] words;

    /**
     * 记录数组桶位的有效长度
     *
     * The number of words in the logical size of this BitSet.
     */
    private transient int wordsInUse = 0;

    /**
     * 保护初始化构造方法时自定义的数组长度
     *
     * Whether the size of "words" is user-specified.  If so, we assume
     * the user knows what he's doing and try harder to preserve it.
     */
    private transient boolean sizeIsSticky = false;

    /**
     * use serialVersionUID from JDK 1.0.2 for interoperability
     */
    private static final long serialVersionUID = 7997698588986878753L;

    /**
     * Every public method must preserve these invariants.
     */
    private void checkInvariants() {
        assert(wordsInUse == 0 || words[wordsInUse - 1] != 0);
        assert(wordsInUse >= 0 && wordsInUse <= words.length);
        assert(wordsInUse == words.length || words[wordsInUse] == 0);
    }

    /**
     * Sets the field wordsInUse to the logical size in words of the bit set.
     * WARNING:This method assumes that the number of words actually in use is
     * less than or equal to the current value of wordsInUse!
     */
    private void recalculateWordsInUse() {
        // Traverse the bitset until a used word is found
        int i;
        for (i = wordsInUse-1; i >= 0; i--) {
            if (words[i] != 0) {
                break;
            }
        }
        wordsInUse = i+1; // The new logical size
    }



    /**
     * 无参构造函数
     * initWords方法初始化了数组，所有位置都设置为false，由于使用了默认是数组长度，则长度保护关闭。
     *
     * Creates a new bit set. All bits are initially {@code false}.
     */
    public BitSet() {
        // 初始化数组
        initWords(BITS_PER_WORD);
        // 长度保护关闭
        sizeIsSticky = false;
    }

    // 有参构造函数
    // 1、自定义nbits，是按比特位的多少进行初始化，调用initWords中的wordIndex方法中，将nbits作了位运算 nbits >> 64，目的是为了知道多少个long加起来的比特位可以容纳下nbits，使初始化数组的长度不会冗余。
    /**
     * Creates a bit set whose initial size is large enough to explicitly
     * represent bits with indices in the range {@code 0} through
     * {@code nbits-1}. All bits are initially {@code false}.
     *
     * @param  nbits the initial size of the bit set
     * @throws NegativeArraySizeException if the specified initial size is negative
     */
    public BitSet(int nbits) {
        // nbits can't be negative; size 0 is OK
        // 自定义长度小于0则抛出异常
        if (nbits < 0) {
            throw new NegativeArraySizeException("nbits < 0: " + nbits);
        }
        // 根据自定义长度初始化数组
        initWords(nbits);
        // 自定义长度保护开启
        sizeIsSticky = true;
    }

    /**
     * 有参构造函数
     * 2、直接传入的words数组，则按自定义的数组长度为准
     *
     * Creates a bit set using words as the internal representation.
     * The last word (if there is one) must be non-zero.
     */
    private BitSet(long[] words) {
        // 直接使用传入的桶位数组
        this.words = words;
        // 再用数组长度赋值
        this.wordsInUse = words.length;
        checkInvariants();
    }

    /**
     * 初始化函数
     * initWords：初始化数组
     *
     * @param nbits
     */
    private void initWords(int nbits) {
        // nbits=64 => len=1
        int len = wordIndex(nbits-1) + 1;
        words = new long[len];
    }

    /**
     * 初始化函数
     * wordIndex：根据比特位索引，除以64（bitIndex >> 6），得到第n个桶位。
     *
     * Given a bit index, return word index containing it.
     */
    private static int wordIndex(int bitIndex) {
        // 计算数组的索引位置
        // bitIndex / 64
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }



    /**
     * Returns a new bit set containing all the bits in the given long array.
     *
     * <p>More precisely,
     * <br>{@code BitSet.valueOf(longs).get(n) == ((longs[n/64] & (1L<<(n%64))) != 0)}
     * <br>for all {@code n < 64 * longs.length}.
     *
     * <p>This method is equivalent to
     * {@code BitSet.valueOf(LongBuffer.wrap(longs))}.
     *
     * @param longs a long array containing a little-endian representation
     *        of a sequence of bits to be used as the initial bits of the
     *        new bit set
     * @return a {@code BitSet} containing all the bits in the long array
     * @since 1.7
     */
    public static BitSet valueOf(long[] longs) {
        int n;
        for (n = longs.length; n > 0 && longs[n - 1] == 0; n--)
            ;
        return new BitSet(Arrays.copyOf(longs, n));
    }

    /**
     * Returns a new bit set containing all the bits in the given long
     * buffer between its position and limit.
     *
     * <p>More precisely,
     * <br>{@code BitSet.valueOf(lb).get(n) == ((lb.get(lb.position()+n/64) & (1L<<(n%64))) != 0)}
     * <br>for all {@code n < 64 * lb.remaining()}.
     *
     * <p>The long buffer is not modified by this method, and no
     * reference to the buffer is retained by the bit set.
     *
     * @param lb a long buffer containing a little-endian representation
     *        of a sequence of bits between its position and limit, to be
     *        used as the initial bits of the new bit set
     * @return a {@code BitSet} containing all the bits in the buffer in the
     *         specified range
     * @since 1.7
     */
    public static BitSet valueOf(LongBuffer lb) {
        lb = lb.slice();
        int n;
        for (n = lb.remaining(); n > 0 && lb.get(n - 1) == 0; n--)
            ;
        long[] words = new long[n];
        lb.get(words);
        return new BitSet(words);
    }

    /**
     * Returns a new bit set containing all the bits in the given byte array.
     *
     * <p>More precisely,
     * <br>{@code BitSet.valueOf(bytes).get(n) == ((bytes[n/8] & (1<<(n%8))) != 0)}
     * <br>for all {@code n <  8 * bytes.length}.
     *
     * <p>This method is equivalent to
     * {@code BitSet.valueOf(ByteBuffer.wrap(bytes))}.
     *
     * @param bytes a byte array containing a little-endian
     *        representation of a sequence of bits to be used as the
     *        initial bits of the new bit set
     * @return a {@code BitSet} containing all the bits in the byte array
     * @since 1.7
     */
    public static BitSet valueOf(byte[] bytes) {
        return BitSet.valueOf(ByteBuffer.wrap(bytes));
    }

    /**
     * Returns a new bit set containing all the bits in the given byte
     * buffer between its position and limit.
     *
     * <p>More precisely,
     * <br>{@code BitSet.valueOf(bb).get(n) == ((bb.get(bb.position()+n/8) & (1<<(n%8))) != 0)}
     * <br>for all {@code n < 8 * bb.remaining()}.
     *
     * <p>The byte buffer is not modified by this method, and no
     * reference to the buffer is retained by the bit set.
     *
     * @param bb a byte buffer containing a little-endian representation
     *        of a sequence of bits between its position and limit, to be
     *        used as the initial bits of the new bit set
     * @return a {@code BitSet} containing all the bits in the buffer in the
     *         specified range
     * @since 1.7
     */
    public static BitSet valueOf(ByteBuffer bb) {
        bb = bb.slice().order(ByteOrder.LITTLE_ENDIAN);
        int n;
        for (n = bb.remaining(); n > 0 && bb.get(n - 1) == 0; n--)
            ;
        long[] words = new long[(n + 7) / 8];
        bb.limit(n);
        int i = 0;
        while (bb.remaining() >= 8)
            words[i++] = bb.getLong();
        for (int remaining = bb.remaining(), j = 0; j < remaining; j++)
            words[i] |= (bb.get() & 0xffL) << (8 * j);
        return new BitSet(words);
    }

    /**
     * Returns a new byte array containing all the bits in this bit set.
     *
     * <p>More precisely, if
     * <br>{@code byte[] bytes = s.toByteArray();}
     * <br>then {@code bytes.length == (s.length()+7)/8} and
     * <br>{@code s.get(n) == ((bytes[n/8] & (1<<(n%8))) != 0)}
     * <br>for all {@code n < 8 * bytes.length}.
     *
     * @return a byte array containing a little-endian representation
     *         of all the bits in this bit set
     * @since 1.7
    */
    public byte[] toByteArray() {
        int n = wordsInUse;
        if (n == 0)
            return new byte[0];
        int len = 8 * (n-1);
        for (long x = words[n - 1]; x != 0; x >>>= 8)
            len++;
        byte[] bytes = new byte[len];
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < n - 1; i++)
            bb.putLong(words[i]);
        for (long x = words[n - 1]; x != 0; x >>>= 8)
            bb.put((byte) (x & 0xff));
        return bytes;
    }

    /**
     * Returns a new long array containing all the bits in this bit set.
     *
     * <p>More precisely, if
     * <br>{@code long[] longs = s.toLongArray();}
     * <br>then {@code longs.length == (s.length()+63)/64} and
     * <br>{@code s.get(n) == ((longs[n/64] & (1L<<(n%64))) != 0)}
     * <br>for all {@code n < 64 * longs.length}.
     *
     * @return a long array containing a little-endian representation
     *         of all the bits in this bit set
     * @since 1.7
    */
    public long[] toLongArray() {
        return Arrays.copyOf(words, wordsInUse);
    }

    /**
     * Ensures that the BitSet can hold enough words.
     * @param wordsRequired the minimum acceptable number of words.
     */
    private void ensureCapacity(int wordsRequired) {
        if (words.length < wordsRequired) {
            // Allocate larger of doubled size or required size
            int request = Math.max(2 * words.length, wordsRequired);
            words = Arrays.copyOf(words, request);
            sizeIsSticky = false;
        }
    }

    /**
     * Ensures that the BitSet can accommodate a given wordIndex,
     * temporarily violating the invariants.  The caller must
     * restore the invariants before returning to the user,
     * possibly using recalculateWordsInUse().
     * @param wordIndex the index to be accommodated.
     */
    private void expandTo(int wordIndex) {
        // 由于初始化函数时，也是根据wordIndex计算结果+1，故此处也要+1
        int wordsRequired = wordIndex+1;
        if (wordsInUse < wordsRequired) {
            ensureCapacity(wordsRequired);
            wordsInUse = wordsRequired;
        }
    }

    /**
     * Checks that fromIndex ... toIndex is a valid range of bit indices.
     */
    private static void checkRange(int fromIndex, int toIndex) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        if (toIndex < 0)
            throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
        if (fromIndex > toIndex)
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex + " > toIndex: " + toIndex);
    }

    /**
     * 索引翻转
     * 异或的作用：相同为0，不同为1，因此flip方法实质上是对撞抵消
     * 场景：计算商品是否发货，含重复id的商品，通过flip令id抵消，表示商品已发出
     * 执行逻辑：
     * 1、检验索引范围
     * 2、计算flip的索引位置在哪个桶上
     * 3、重置桶位使用长度
     * 4、将flip的索引位通过 ‘异或’ 进行翻转
     * 5、重新计算桶位有效长度
     *
     * Sets the bit at the specified index to the complement of its
     * current value.
     *
     * @param  bitIndex the index of the bit to flip
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since  1.4
     */
    public void flip(int bitIndex) {
        // 1.检验索引范围
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        // 2.计算flip的索引位置在哪个桶上
        int wordIndex = wordIndex(bitIndex);
        // 3.重置桶位使用长度
        expandTo(wordIndex);
        // 4.将flip的索引位通过 ‘异或’ 进行翻转
        words[wordIndex] ^= (1L << bitIndex);
        // 5.重新计算桶位有效长度
        recalculateWordsInUse();
        checkInvariants();
    }

    /**
     * Sets each bit from the specified {@code fromIndex} (inclusive) to the
     * specified {@code toIndex} (exclusive) to the complement of its current
     * value.
     *
     * @param  fromIndex index of the first bit to flip
     * @param  toIndex index after the last bit to flip
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *         or {@code toIndex} is negative, or {@code fromIndex} is
     *         larger than {@code toIndex}
     * @since  1.4
     */
    public void flip(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);

        if (fromIndex == toIndex)
            return;

        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex   = wordIndex(toIndex - 1);
        expandTo(endWordIndex);

        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask  = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            // Case 1: One word
            words[startWordIndex] ^= (firstWordMask & lastWordMask);
        } else {
            // Case 2: Multiple words
            // Handle first word
            words[startWordIndex] ^= firstWordMask;

            // Handle intermediate words, if any
            for (int i = startWordIndex+1; i < endWordIndex; i++)
                words[i] ^= WORD_MASK;

            // Handle last word
            words[endWordIndex] ^= lastWordMask;
        }

        recalculateWordsInUse();
        checkInvariants();
    }

    /**
     * 设置索引位有效
     * 执行逻辑：
     * 1、检验索引有效范围
     * 2、计算比特位索引对应的桶位角标
     * 3、expand重置桶位使用长度
     * 4、将set的索引位通过 ‘或运算’ 置为1，1即为有效
     *
     * Sets the bit at the specified index to {@code true}.
     *
     * @param  bitIndex a bit index
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since  1.0
     */
    public void set(int bitIndex) {
        // 1.索引检验
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        // 2.计算比特位索引对应的桶位
        int wordIndex = wordIndex(bitIndex);
        // 3.重置桶位使用长度
        expandTo(wordIndex);
        // 4.将set的索引位通过‘或运算’置为1
        // 这个我们讲下这个，转化下：words[wordIndex] = words[wordIndex] | (1L << bitIndex);
        // 先看下(1L << bitIndex) 说明：左移动数组的bitIndex 位，然后为1，怎么说呢？看下面吧
        // 1L<<1 转化为2进制，10，
        // 1L<<2 转化为2进制，100，
        // 1L<<3 转化为2进制，1000，
        // || 运算，只要有一个条件为true，即为true
        // 这个是否明白，哈，然在看看下words[wordIndex] ||(1L << bitIndex)，
        // 啰嗦下，bitset存的就是1或0的2进制数组，0代表为没有存在这个集合，1代表存在这个集合
        // 这下是不是明白，就是我看看words[wordIndex]的数组是不是1，如有为1还是1（在啰嗦下，或就是两个只有有一个是1就是1，多读几次就知道）


        String before = this.toBinaryStringV2();
        words[wordIndex] |= (1L << bitIndex); // Restores invariants
        String after = this.toBinaryStringV2();


        // 等效代码
        // String a = Long.toBinaryStringV2(1L << bitIndex);
        // String b = Long.toBinaryStringV2(words[wordIndex]);
        // words[wordIndex] = words[wordIndex] | (1L << bitIndex);
        // String c = this.toBinaryStringV2();

        //检测数据
        checkInvariants();
    }

    /**
     * Sets the bit at the specified index to the specified value.
     *
     * @param  bitIndex a bit index
     * @param  value a boolean value to set
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since  1.4
     */
    public void set(int bitIndex, boolean value) {
        if (value) {
            set(bitIndex);
        } else {
            clear(bitIndex);
        }
    }

    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the
     * specified {@code toIndex} (exclusive) to {@code true}.
     *
     * @param  fromIndex index of the first bit to be set
     * @param  toIndex index after the last bit to be set
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *         or {@code toIndex} is negative, or {@code fromIndex} is
     *         larger than {@code toIndex}
     * @since  1.4
     */
    public void set(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);

        if (fromIndex == toIndex) {
            return;
        }

        // Increase capacity if necessary
        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex   = wordIndex(toIndex - 1);
        expandTo(endWordIndex);

        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask  = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            // Case 1: One word
            words[startWordIndex] |= (firstWordMask & lastWordMask);
        } else {
            // Case 2: Multiple words
            // Handle first word
            words[startWordIndex] |= firstWordMask;

            // Handle intermediate words, if any
            for (int i = startWordIndex+1; i < endWordIndex; i++) {
                words[i] = WORD_MASK;
            }

            // Handle last word (restores invariants)
            words[endWordIndex] |= lastWordMask;
        }

        checkInvariants();
    }

    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the
     * specified {@code toIndex} (exclusive) to the specified value.
     *
     * @param  fromIndex index of the first bit to be set
     * @param  toIndex index after the last bit to be set
     * @param  value value to set the selected bits to
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *         or {@code toIndex} is negative, or {@code fromIndex} is
     *         larger than {@code toIndex}
     * @since  1.4
     */
    public void set(int fromIndex, int toIndex, boolean value) {
        if (value) {
            set(fromIndex, toIndex);
        } else {
            clear(fromIndex, toIndex);
        }
    }

    /**
     * Sets the bit specified by the index to {@code false}.
     *
     * @param  bitIndex the index of the bit to be cleared
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since  1.0
     */
    public void clear(int bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }

        int wordIndex = wordIndex(bitIndex);
        if (wordIndex >= wordsInUse) {
            return;
        }

        words[wordIndex] &= ~(1L << bitIndex);

        recalculateWordsInUse();
        checkInvariants();
    }

    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the
     * specified {@code toIndex} (exclusive) to {@code false}.
     *
     * @param  fromIndex index of the first bit to be cleared
     * @param  toIndex index after the last bit to be cleared
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *         or {@code toIndex} is negative, or {@code fromIndex} is
     *         larger than {@code toIndex}
     * @since  1.4
     */
    public void clear(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);

        if (fromIndex == toIndex) {
            return;
        }

        int startWordIndex = wordIndex(fromIndex);
        if (startWordIndex >= wordsInUse) {
            return;
        }

        int endWordIndex = wordIndex(toIndex - 1);
        if (endWordIndex >= wordsInUse) {
            toIndex = length();
            endWordIndex = wordsInUse - 1;
        }

        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask  = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            // Case 1: One word
            words[startWordIndex] &= ~(firstWordMask & lastWordMask);
        } else {
            // Case 2: Multiple words
            // Handle first word
            words[startWordIndex] &= ~firstWordMask;

            // Handle intermediate words, if any
            for (int i = startWordIndex+1; i < endWordIndex; i++) {
                words[i] = 0;
            }

            // Handle last word
            words[endWordIndex] &= ~lastWordMask;
        }

        recalculateWordsInUse();
        checkInvariants();
    }

    /**
     * 清空桶
     * 执行逻辑：
     * 1、循环便利有效桶位，并将每个桶置为0，同时有效桶位的计量单位wordsInUse也置为0
     *
     * Sets all of the bits in this BitSet to {@code false}.
     *
     * @since 1.4
     */
    public void clear() {
        // 1.循环便利有效桶位，并将每个桶置为0，同时有效桶位的计量单位wordsInUse也置为0
        while (wordsInUse > 0) {
            words[--wordsInUse] = 0;
        }
    }

    /**
     * 获取索引位是否存在
     * 执行逻辑：
     * 1、检验索引范围
     * 2、使用的桶位长度检验
     * 3、计算get的索引位置在哪个桶上
     * 4、桶位未发生异常的情况下（防止并发），返回对应索引是否为1，为1返回true
     *
     *
     * Returns the value of the bit with the specified index. The value
     * is {@code true} if the bit with the index {@code bitIndex}
     * is currently set in this {@code BitSet}; otherwise, the result
     * is {@code false}.
     *
     * @param  bitIndex   the bit index
     * @return the value of the bit with the specified index
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    public boolean get(int bitIndex) {
        // 1.检验索引范围
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        // 2.使用的桶位长度检验
        checkInvariants();
        // 3.计算get的索引位置在哪个桶上
        int wordIndex = wordIndex(bitIndex);
        // 4.桶位未发生异常的情况下（防止并发），返回对应索引是否为1，为1返回true
        return (wordIndex < wordsInUse) && ((words[wordIndex] & (1L << bitIndex)) != 0);
    }

    /**
     * Returns a new {@code BitSet} composed of bits from this {@code BitSet}
     * from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     *
     * @param  fromIndex index of the first bit to include
     * @param  toIndex index after the last bit to include
     * @return a new {@code BitSet} from a range of this {@code BitSet}
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *         or {@code toIndex} is negative, or {@code fromIndex} is
     *         larger than {@code toIndex}
     * @since  1.4
     */
    public BitSet get(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);

        checkInvariants();

        int len = length();

        // If no set bits in range return empty bitset
        if (len <= fromIndex || fromIndex == toIndex) {
            return new BitSet(0);
        }

        // An optimization
        if (toIndex > len) {
            toIndex = len;
        }

        BitSet result = new BitSet(toIndex - fromIndex);
        int targetWords = wordIndex(toIndex - fromIndex - 1) + 1;
        int sourceIndex = wordIndex(fromIndex);
        boolean wordAligned = ((fromIndex & BIT_INDEX_MASK) == 0);

        // Process all words but the last word
        for (int i = 0; i < targetWords - 1; i++, sourceIndex++) {
            result.words[i] = wordAligned ? words[sourceIndex] : (words[sourceIndex] >>> fromIndex) | (words[sourceIndex+1] << -fromIndex);
        }

        // Process the last word
        long lastWordMask = WORD_MASK >>> -toIndex;
        result.words[targetWords - 1] =
            ((toIndex-1) & BIT_INDEX_MASK) < (fromIndex & BIT_INDEX_MASK)
            ? /* straddles source words */
            ((words[sourceIndex] >>> fromIndex) |
             (words[sourceIndex+1] & lastWordMask) << -fromIndex)
            :
            ((words[sourceIndex] & lastWordMask) >>> fromIndex);

        // Set wordsInUse correctly
        result.wordsInUse = targetWords;
        result.recalculateWordsInUse();
        result.checkInvariants();

        return result;
    }

    /**
     * Returns the index of the first bit that is set to {@code true}
     * that occurs on or after the specified starting index. If no such
     * bit exists then {@code -1} is returned.
     *
     * <p>To iterate over the {@code true} bits in a {@code BitSet},
     * use the following loop:
     *
     *  <pre> {@code
     * for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
     *     // operate on index i here
     *     if (i == Integer.MAX_VALUE) {
     *         break; // or (i+1) would overflow
     *     }
     * }}</pre>
     *
     * @param  fromIndex the index to start checking from (inclusive)
     * @return the index of the next set bit, or {@code -1} if there
     *         is no such bit
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since  1.4
     */
    public int nextSetBit(int fromIndex) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

        checkInvariants();

        int u = wordIndex(fromIndex);
        if (u >= wordsInUse)
            return -1;

        long word = words[u] & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0) {
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            }
            if (++u == wordsInUse) {
                return -1;
            }
            word = words[u];
        }
    }

    /**
     * Returns the index of the first bit that is set to {@code false}
     * that occurs on or after the specified starting index.
     *
     * @param  fromIndex the index to start checking from (inclusive)
     * @return the index of the next clear bit
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since  1.4
     */
    public int nextClearBit(int fromIndex) {
        // Neither spec nor implementation handle bitsets of maximal length.
        // See 4816253.
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }

        checkInvariants();

        int u = wordIndex(fromIndex);
        if (u >= wordsInUse) {
            return fromIndex;
        }

        long word = ~words[u] & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0) {
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            }
            if (++u == wordsInUse) {
                return wordsInUse * BITS_PER_WORD;
            }
            word = ~words[u];
        }
    }

    /**
     * Returns the index of the nearest bit that is set to {@code true}
     * that occurs on or before the specified starting index.
     * If no such bit exists, or if {@code -1} is given as the
     * starting index, then {@code -1} is returned.
     *
     * <p>To iterate over the {@code true} bits in a {@code BitSet},
     * use the following loop:
     *
     *  <pre> {@code
     * for (int i = bs.length(); (i = bs.previousSetBit(i-1)) >= 0; ) {
     *     // operate on index i here
     * }}</pre>
     *
     * @param  fromIndex the index to start checking from (inclusive)
     * @return the index of the previous set bit, or {@code -1} if there
     *         is no such bit
     * @throws IndexOutOfBoundsException if the specified index is less
     *         than {@code -1}
     * @since  1.7
     */
    public int previousSetBit(int fromIndex) {
        if (fromIndex < 0) {
            if (fromIndex == -1) {
                return -1;
            }
            throw new IndexOutOfBoundsException("fromIndex < -1: " + fromIndex);
        }

        checkInvariants();

        int u = wordIndex(fromIndex);
        if (u >= wordsInUse) {
            return length() - 1;
        }

        long word = words[u] & (WORD_MASK >>> -(fromIndex+1));

        while (true) {
            if (word != 0) {
                return (u+1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word);
            }
            if (u-- == 0) {
                return -1;
            }
            word = words[u];
        }
    }

    /**
     * Returns the index of the nearest bit that is set to {@code false}
     * that occurs on or before the specified starting index.
     * If no such bit exists, or if {@code -1} is given as the
     * starting index, then {@code -1} is returned.
     *
     * @param  fromIndex the index to start checking from (inclusive)
     * @return the index of the previous clear bit, or {@code -1} if there
     *         is no such bit
     * @throws IndexOutOfBoundsException if the specified index is less
     *         than {@code -1}
     * @since  1.7
     */
    public int previousClearBit(int fromIndex) {
        if (fromIndex < 0) {
            if (fromIndex == -1) {
                return -1;
            }
            throw new IndexOutOfBoundsException("fromIndex < -1: " + fromIndex);
        }

        checkInvariants();

        int u = wordIndex(fromIndex);
        if (u >= wordsInUse) {
            return fromIndex;
        }

        long word = ~words[u] & (WORD_MASK >>> -(fromIndex+1));

        while (true) {
            if (word != 0) {
                return (u+1) * BITS_PER_WORD -1 - Long.numberOfLeadingZeros(word);
            }
            if (u-- == 0) {
                return -1;
            }
            word = ~words[u];
        }
    }

    /**
     * Returns the "logical size" of this {@code BitSet}: the index of
     * the highest set bit in the {@code BitSet} plus one. Returns zero
     * if the {@code BitSet} contains no set bits.
     *
     * @return the logical size of this {@code BitSet}
     * @since  1.2
     */
    public int length() {
        if (wordsInUse == 0) {
            return 0;
        }

        return BITS_PER_WORD * (wordsInUse - 1) + (BITS_PER_WORD - Long.numberOfLeadingZeros(words[wordsInUse - 1]));
    }

    /**
     * Returns true if this {@code BitSet} contains no bits that are set
     * to {@code true}.
     *
     * @return boolean indicating whether this {@code BitSet} is empty
     * @since  1.4
     */
    public boolean isEmpty() {
        return wordsInUse == 0;
    }

    /**
     * Returns true if the specified {@code BitSet} has any bits set to
     * {@code true} that are also set to {@code true} in this {@code BitSet}.
     *
     * @param  set {@code BitSet} to intersect with
     * @return boolean indicating whether this {@code BitSet} intersects
     *         the specified {@code BitSet}
     * @since  1.4
     */
    public boolean intersects(BitSet set) {
        for (int i = Math.min(wordsInUse, set.wordsInUse) - 1; i >= 0; i--) {
            if ((words[i] & set.words[i]) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算有效索引位的数量
     * 执行逻辑：
     * 1、循环计算每个桶内的long类型的数的比特位为1的数量，计算所有桶的所有有效位数量
     *
     * Returns the number of bits set to {@code true} in this {@code BitSet}.
     *
     * @return the number of bits set to {@code true} in this {@code BitSet}
     * @since  1.4
     */
    public int cardinality() {
        // 1.循环计算每个桶内的long类型的数的比特位为1的数量，计算所有桶的所有有效位数量
        int sum = 0;
        for (int i = 0; i < wordsInUse; i++) {
            sum += Long.bitCount(words[i]);
        }
        return sum;
    }

    /**
     * 与运算
     * 执行逻辑：
     * 1、自身相计算，则结果不有变更，终止执行
     * 2、若当前Bitset的有效桶位多于传入的Bitset的有效桶位数量，则将当前Bitset的多余桶位全部归0，因为1与0作 ‘与运算’ 结果为0，因此多余桶部分计算的结果也为0，免去位运算过程，直接归0，提效
     * 3、对有效桶进行 ‘与运算’
     * 4、重新计算桶位有效长度
     *
     * Performs a logical <b>AND</b> of this target bit set with the
     * argument bit set. This bit set is modified so that each bit in it
     * has the value {@code true} if and only if it both initially
     * had the value {@code true} and the corresponding bit in the
     * bit set argument also had the value {@code true}.
     *
     * @param set a bit set
     */
    public void and(BitSet set) {
        // 1.自身相计算，则结果不有变更，终止执行
        if (this == set) {
            return;
        }
        // 2.若当前Bitset的有效桶位多于传入的Bitset的有效桶位数量，则将当前Bitset的多余桶位全部归0
        while (wordsInUse > set.wordsInUse) {
            words[--wordsInUse] = 0;
        }
        // 3.对有效桶进行 ‘与运算’
        // Perform logical AND on words in common
        for (int i = 0; i < wordsInUse; i++) {
            words[i] &= set.words[i];
        }
        // 4.重新计算桶位有效长度
        recalculateWordsInUse();
        checkInvariants();
    }

    /**
     * 或运算
     * 执行逻辑：
     * 1、自身相计算，则结果不有变更，终止执行
     * 2、使用两个Bitset公共桶位
     * 3、当前Bitset桶位长度若小于传入的Bitset的桶位长度，则将当前Bitset桶位长度上升
     * 4、对每个桶都进行 ‘或运算’
     * 5、若公共桶位长度小于传入的Bitset的有效桶位长度，则将传入的Bitset的增量桶赋值给当前Bitset对应桶位
     *
     * Performs a logical <b>OR</b> of this bit set with the bit set
     * argument. This bit set is modified so that a bit in it has the
     * value {@code true} if and only if it either already had the
     * value {@code true} or the corresponding bit in the bit set
     * argument has the value {@code true}.
     *
     * @param set a bit set
     */
    public void or(BitSet set) {
        // 1.自身相计算，则结果不有变更，终止执行
        if (this == set) {
            return;
        }
        // 2.使用两个Bitset公共桶位长度
        int wordsInCommon = Math.min(wordsInUse, set.wordsInUse);
        // 3.当前Bitset桶位长度若小于传入的Bitset的桶位长度，则将当前Bitset桶位长度上升
        if (wordsInUse < set.wordsInUse) {
            ensureCapacity(set.wordsInUse);
            wordsInUse = set.wordsInUse;
        }
        // 4.对每个桶都进行 ‘或运算’
        // Perform logical OR on words in common
        for (int i = 0; i < wordsInCommon; i++) {
            words[i] |= set.words[i];
        }
        // 5.若公共桶位长度小于传入的Bitset的有效桶位长度，则将传入的Bitset的增量桶赋值给当前Bitset对应桶位
        // Copy any remaining words
        if (wordsInCommon < set.wordsInUse) {
            System.arraycopy(set.words, wordsInCommon, words, wordsInCommon, wordsInUse - wordsInCommon);
        }

        // recalculateWordsInUse() is unnecessary
        checkInvariants();
    }

    /**
     * 异或运算
     * 执行逻辑：
     * 1、使用两个Bitset公共桶位
     * 2、当前Bitset桶位长度若小于传入的Bitset的桶位长度，则将当前Bitset桶位长度上升
     * 3、对每个桶都进行 ‘异或运算’
     * 4、若公共桶位长度小于传入的Bitset的有效桶位长度，则将传入的Bitset的增量桶赋值给当前Bitset对应桶位
     * 5、重新计算桶位有效长度
     *
     * Performs a logical <b>XOR</b> of this bit set with the bit set
     * argument. This bit set is modified so that a bit in it has the
     * value {@code true} if and only if one of the following
     * statements holds:
     * <ul>
     * <li>The bit initially has the value {@code true}, and the
     *     corresponding bit in the argument has the value {@code false}.
     * <li>The bit initially has the value {@code false}, and the
     *     corresponding bit in the argument has the value {@code true}.
     * </ul>
     *
     * @param  set a bit set
     */
    public void xor(BitSet set) {
        // 1.使用两个Bitset公共桶位
        int wordsInCommon = Math.min(wordsInUse, set.wordsInUse);
        // 2.当前Bitset桶位长度若小于传入的Bitset的桶位长度，则将当前Bitset桶位长度上升
        if (wordsInUse < set.wordsInUse) {
            ensureCapacity(set.wordsInUse);
            wordsInUse = set.wordsInUse;
        }
        // 3.对每个桶都进行 ‘异或运算’
        // Perform logical XOR on words in common
        for (int i = 0; i < wordsInCommon; i++) {
            words[i] ^= set.words[i];
        }
        // 4.若公共桶位长度小于传入的Bitset的有效桶位长度，则将传入的Bitset的增量桶赋值给当前Bitset对应桶位
        // Copy any remaining words
        if (wordsInCommon < set.wordsInUse) {
            System.arraycopy(set.words, wordsInCommon, words, wordsInCommon, set.wordsInUse - wordsInCommon);
        }
        // 5.重新计算桶位有效长度
        recalculateWordsInUse();
        checkInvariants();
    }

    /**
     * 非运算
     * 执行逻辑：
     * 1、循环的最大次数取两个Bitset的公共有效桶位
     * 2、对有效桶进行 当前桶 ‘与运算’ 传入的桶（传入的桶先进行 ‘非运算’ 取反）
     *
     *
     * Clears all of the bits in this {@code BitSet} whose corresponding
     * bit is set in the specified {@code BitSet}.
     *
     * @param  set the {@code BitSet} with which to mask this
     *         {@code BitSet}
     * @since  1.2
     */
    public void andNot(BitSet set) {
        // 1.循环的最大次数取两个Bitset的公共有效桶位
        // 2.对有效桶进行 当前桶 ‘与运算’ 传入的桶（传入的桶先进行 ‘非运算’ 取反）
        // Perform logical (a & !b) on words in common
        for (int i = Math.min(wordsInUse, set.wordsInUse) - 1; i >= 0; i--) {
            words[i] &= ~set.words[i];
        }

        recalculateWordsInUse();
        checkInvariants();
    }

    /**
     * Returns the hash code value for this bit set. The hash code depends
     * only on which bits are set within this {@code BitSet}.
     *
     * <p>The hash code is defined to be the result of the following
     * calculation:
     *  <pre> {@code
     * public int hashCode() {
     *     long h = 1234;
     *     long[] words = toLongArray();
     *     for (int i = words.length; --i >= 0; )
     *         h ^= words[i] * (i + 1);
     *     return (int)((h >> 32) ^ h);
     * }}</pre>
     * Note that the hash code changes if the set of bits is altered.
     *
     * @return the hash code value for this bit set
     */
    public int hashCode() {
        long h = 1234;
        for (int i = wordsInUse; --i >= 0; ) {
            h ^= words[i] * (i + 1);
        }

        return (int)((h >> 32) ^ h);
    }

    /**
     * Returns the number of bits of space actually in use by this
     * {@code BitSet} to represent bit values.
     * The maximum element in the set is the size - 1st element.
     *
     * @return the number of bits currently in this bit set
     */
    public int size() {
        return words.length * BITS_PER_WORD;
    }

    /**
     * Compares this object against the specified object.
     * The result is {@code true} if and only if the argument is
     * not {@code null} and is a {@code Bitset} object that has
     * exactly the same set of bits set to {@code true} as this bit
     * set. That is, for every nonnegative {@code int} index {@code k},
     * <pre>((BitSet)obj).get(k) == this.get(k)</pre>
     * must be true. The current sizes of the two bit sets are not compared.
     *
     * @param  obj the object to compare with
     * @return {@code true} if the objects are the same;
     *         {@code false} otherwise
     * @see    #size()
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof BitSet)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        BitSet set = (BitSet) obj;

        checkInvariants();
        set.checkInvariants();

        if (wordsInUse != set.wordsInUse)
            return false;

        // Check words in use by both BitSets
        for (int i = 0; i < wordsInUse; i++) {
            if (words[i] != set.words[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Cloning this {@code BitSet} produces a new {@code BitSet}
     * that is equal to it.
     * The clone of the bit set is another bit set that has exactly the
     * same bits set to {@code true} as this bit set.
     *
     * @return a clone of this bit set
     * @see    #size()
     */
    public Object clone() {
        if (! sizeIsSticky) {
            trimToSize();
        }

        try {
            BitSet result = (BitSet) super.clone();
            result.words = words.clone();
            result.checkInvariants();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Attempts to reduce internal storage used for the bits in this bit set.
     * Calling this method may, but is not required to, affect the value
     * returned by a subsequent call to the {@link #size()} method.
     */
    private void trimToSize() {
        if (wordsInUse != words.length) {
            words = Arrays.copyOf(words, wordsInUse);
            checkInvariants();
        }
    }

    /**
     * Save the state of the {@code BitSet} instance to a stream (i.e.,
     * serialize it).
     */
    private void writeObject(ObjectOutputStream s)
        throws IOException {

        checkInvariants();

        if (! sizeIsSticky) {
            trimToSize();
        }

        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("bits", words);
        s.writeFields();
    }

    /**
     * Reconstitute the {@code BitSet} instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException {

        ObjectInputStream.GetField fields = s.readFields();
        words = (long[]) fields.get("bits", null);

        // Assume maximum length then find real length
        // because recalculateWordsInUse assumes maintenance
        // or reduction in logical size
        wordsInUse = words.length;
        recalculateWordsInUse();
        sizeIsSticky = (words.length > 0 && words[words.length-1] == 0L); // heuristic
        checkInvariants();
    }

    /**
     *
     * @return binaryString
     */
    public String toBinaryString() {
        StringBuilder sb = new StringBuilder();
        for (long word : words) {
            sb.append(Long.toBinaryString(word)).append(" ");
        }
        return sb.toString();
    }
    /**
     *
     * @return binaryString
     */
    public String toBinaryStringV2() {
        StringBuilder sb = new StringBuilder();
        for (long word : words) {
            sb.append(Long.toBinaryStringV2(word)).append(" ");
        }
        return sb.toString();
    }

    /**
     * Returns a string representation of this bit set. For every index
     * for which this {@code BitSet} contains a bit in the set
     * state, the decimal representation of that index is included in
     * the result. Such indices are listed in order from lowest to
     * highest, separated by ",&nbsp;" (a comma and a space) and
     * surrounded by braces, resulting in the usual mathematical
     * notation for a set of integers.
     *
     * <p>Example:
     * <pre>
     * BitSet drPepper = new BitSet();</pre>
     * Now {@code drPepper.toString()} returns "{@code {}}".
     * <pre>
     * drPepper.set(2);</pre>
     * Now {@code drPepper.toString()} returns "{@code {2}}".
     * <pre>
     * drPepper.set(4);
     * drPepper.set(10);</pre>
     * Now {@code drPepper.toString()} returns "{@code {2, 4, 10}}".
     *
     * @return a string representation of this bit set
     */
    public String toString() {
        checkInvariants();

        int numBits = (wordsInUse > 128) ? cardinality() : wordsInUse * BITS_PER_WORD;
        StringBuilder b = new StringBuilder(6*numBits + 2);
        b.append('{');

        int i = nextSetBit(0);
        if (i != -1) {
            b.append(i);
            while (true) {
                if (++i < 0) break;
                if ((i = nextSetBit(i)) < 0) break;
                int endOfRun = nextClearBit(i);
                do { b.append(", ").append(i); }
                while (++i != endOfRun);
            }
        }

        b.append('}');
        return b.toString();
    }

    /**
     * Returns a stream of indices for which this {@code BitSet}
     * contains a bit in the set state. The indices are returned
     * in order, from lowest to highest. The size of the stream
     * is the number of bits in the set state, equal to the value
     * returned by the {@link #cardinality()} method.
     *
     * <p>The stream binds to this bit set when the terminal stream operation
     * commences (specifically, the spliterator for the stream is
     * <a href="Spliterator.html#binding"><em>late-binding</em></a>).  If the
     * bit set is modified during that operation then the result is undefined.
     *
     * @return a stream of integers representing set indices
     * @since 1.8
     */
    public IntStream stream() {
        class BitSetSpliterator implements Spliterator.OfInt {
            private int index; // current bit index for a set bit
            private int fence; // -1 until used; then one past last bit index
            private int est;   // size estimate
            private boolean root; // true if root and not split
            // root == true then size estimate is accurate
            // index == -1 or index >= fence if fully traversed
            // Special case when the max bit set is Integer.MAX_VALUE

            BitSetSpliterator(int origin, int fence, int est, boolean root) {
                this.index = origin;
                this.fence = fence;
                this.est = est;
                this.root = root;
            }

            private int getFence() {
                int hi;
                if ((hi = fence) < 0) {
                    // Round up fence to maximum cardinality for allocated words
                    // This is sufficient and cheap for sequential access
                    // When splitting this value is lowered
                    hi = fence = (wordsInUse >= wordIndex(Integer.MAX_VALUE))
                                 ? Integer.MAX_VALUE
                                 : wordsInUse << ADDRESS_BITS_PER_WORD;
                    est = cardinality();
                    index = nextSetBit(0);
                }
                return hi;
            }

            @Override
            public boolean tryAdvance(IntConsumer action) {
                Objects.requireNonNull(action);

                int hi = getFence();
                int i = index;
                if (i < 0 || i >= hi) {
                    // Check if there is a final bit set for Integer.MAX_VALUE
                    if (i == Integer.MAX_VALUE && hi == Integer.MAX_VALUE) {
                        index = -1;
                        action.accept(Integer.MAX_VALUE);
                        return true;
                    }
                    return false;
                }

                index = nextSetBit(i + 1, wordIndex(hi - 1));
                action.accept(i);
                return true;
            }

            @Override
            public void forEachRemaining(IntConsumer action) {
                Objects.requireNonNull(action);

                int hi = getFence();
                int i = index;
                index = -1;

                if (i >= 0 && i < hi) {
                    action.accept(i++);

                    int u = wordIndex(i);      // next lower word bound
                    int v = wordIndex(hi - 1); // upper word bound

                    words_loop:
                    for (; u <= v && i <= hi; u++, i = u << ADDRESS_BITS_PER_WORD) {
                        long word = words[u] & (WORD_MASK << i);
                        while (word != 0) {
                            i = (u << ADDRESS_BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
                            if (i >= hi) {
                                // Break out of outer loop to ensure check of
                                // Integer.MAX_VALUE bit set
                                break words_loop;
                            }

                            // Flip the set bit
                            word &= ~(1L << i);

                            action.accept(i);
                        }
                    }
                }

                // Check if there is a final bit set for Integer.MAX_VALUE
                if (i == Integer.MAX_VALUE && hi == Integer.MAX_VALUE) {
                    action.accept(Integer.MAX_VALUE);
                }
            }

            @Override
            public OfInt trySplit() {
                int hi = getFence();
                int lo = index;
                if (lo < 0) {
                    return null;
                }

                // Lower the fence to be the upper bound of last bit set
                // The index is the first bit set, thus this spliterator
                // covers one bit and cannot be split, or two or more
                // bits
                hi = fence = (hi < Integer.MAX_VALUE || !get(Integer.MAX_VALUE))
                        ? previousSetBit(hi - 1) + 1
                        : Integer.MAX_VALUE;

                // Find the mid point
                int mid = (lo + hi) >>> 1;
                if (lo >= mid) {
                    return null;
                }

                // Raise the index of this spliterator to be the next set bit
                // from the mid point
                index = nextSetBit(mid, wordIndex(hi - 1));
                root = false;

                // Don't lower the fence (mid point) of the returned spliterator,
                // traversal or further splitting will do that work
                return new BitSetSpliterator(lo, mid, est >>>= 1, false);
            }

            @Override
            public long estimateSize() {
                getFence(); // force init
                return est;
            }

            @Override
            public int characteristics() {
                // Only sized when root and not split
                return (root ? Spliterator.SIZED : 0) |
                    Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED;
            }

            @Override
            public Comparator<? super Integer> getComparator() {
                return null;
            }
        }
        return StreamSupport.intStream(new BitSetSpliterator(0, -1, 0, true), false);
    }

    /**
     * Returns the index of the first bit that is set to {@code true}
     * that occurs on or after the specified starting index and up to and
     * including the specified word index
     * If no such bit exists then {@code -1} is returned.
     *
     * @param  fromIndex the index to start checking from (inclusive)
     * @param  toWordIndex the last word index to check (inclusive)
     * @return the index of the next set bit, or {@code -1} if there
     *         is no such bit
     */
    private int nextSetBit(int fromIndex, int toWordIndex) {
        int u = wordIndex(fromIndex);
        // Check if out of bounds
        if (u > toWordIndex)
            return -1;

        long word = words[u] & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            // Check if out of bounds
            if (++u > toWordIndex)
                return -1;
            word = words[u];
        }
    }

}
