/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.reflect;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import sun.security.action.GetBooleanAction;

/**
 * ProxyGenerator contains the code to generate a dynamic proxy class
 * for the java.lang.reflect.Proxy API.
 * <p>
 * The external interfaces to ProxyGenerator is the static
 * "generateProxyClass" method.
 *
 * @author Peter Jones
 * @since 1.3
 */
class ProxyGenerator {
    /*
     * In the comments below, "JVMS" refers to The Java Virtual Machine
     * Specification Second Edition and "JLS" refers to the original
     * version of The Java Language Specification, unless otherwise
     * specified.
     */

    /* generate 1.5-era class file version */
    private static final int CLASSFILE_MAJOR_VERSION = 49;
    private static final int CLASSFILE_MINOR_VERSION = 0;

    /*
     * beginning of constants copied from sun.tools.java.RuntimeConstants (which no longer exists):
     */

    // import com.sun.tools.classfile.ConstantPool;
    /* constant pool tags */
    // 用于表示字符串常量的值
    // CONSTANT_Utf8_info {
    //   u1 tag; // tag=1
    //   u2 length; // 表示这个utf-8编码的字节数组的长度，即有多少个字节
    //   u1 bytes[length]; // 使用了utf-8编码后的字节数组
    // }
    private static final int CONSTANT_UTF8 = 1;
    //
    private static final int CONSTANT_UNICODE = 2;
    // 表示4字节(int)的数值常量
    // CONSTANT_Integer_info {
    // u1 tag; // tag=3
    // u4 byte;
    // }
    private static final int CONSTANT_INTEGER = 3;
    // 表示4字节(float)的数值常量
    // CONSTANT_Float_info {
    //   u1 tag; // tag=4
    //   u4 byte;
    // }
    private static final int CONSTANT_FLOAT = 4;
    // 表示8字节(long)的数值常量
    // 并没有用u8来存储value，用来两个u4来存储。形成来高4个字节，低4个字节的结构，先存低4个字节的，存不下在往高位存储。
    // 为啥这样做，是为了兼容32位操作系统(4*8=32指令只能读取4个字节的数据)，cpu并不会把两次操作变成原子操作，所以会存在问题(小概率)。
    // CONSTANT_Long_info {
    //   u1 tag; // tag=5
    //   u4 high_bytes;
    //   u4 low bytes;
    // }
    private static final int CONSTANT_LONG = 5;
    // 表示8字节(double)的数值常量
    // CONSTANT_Double_info {
    //   u1 tag; // tag=6
    //   u4 high_bytes;
    //   u4 low bytes;
    // }
    private static final int CONSTANT_DOUBLE = 6;
    //
    // name_index的值是某个CONSTANT_Utf8_info结构体在常量池中的索引，对应的CONSTANT_Utf8_info结构体存储了对应的二进制形式的完全限定名称的字符串。
    // CONSTANT_Class_info {
    //   u1 tag; // tag=7
    //   u2 name_index; // 全限定名 java.lang.String
    // }
    private static final int CONSTANT_CLASS = 7;
    // 结构体中占用2个字节的string_index值指向某个CONSTANT_Utf8_info结构体，
    // CONSTANT_String_info {
    //   u1 tag; // tag=8
    //   u2 string_index; // string_index的值是某个CONSTANT_Utf8_info结构体在常量池中的索引
    // }
    private static final int CONSTANT_STRING = 8;
    // 表示类中的字段
    // CONSTANT_Fieldref_info {
    //     u1 tag;
    //     u2 class_index;
    //     u2 name_and_type_index;
    // }
    private static final int CONSTANT_FIELD = 9;
    // 表示类中的方法
    // CONSTANT_Methodref_info {
    //     u1 tag;
    //     u2 class_index;
    //     u2 name_and_type_index;
    // }
    private static final int CONSTANT_METHOD = 10;
    // 表示类所实现的接口的方法
    // CONSTANT_InterfaceMethodref_info {
    //     u1 tag;
    //     u2 class_index;
    //     u2 name_and_type_index;
    // }
    private static final int CONSTANT_INTERFACEMETHOD = 11;
    // 表示字段或方法的名称和类型
    // CONSTANT_NameAndType_info {
    //     u1 tag;
    //     u2 name_index;
    //     u2 descriptor_index;
    // }
    private static final int CONSTANT_NAMEANDTYPE = 12;


    /* access and modifier flags */
    private static final int ACC_PUBLIC = 0x00000001;
    private static final int ACC_PRIVATE = 0x00000002;
    //  private static final int ACC_PROTECTED              = 0x00000004;
    private static final int ACC_STATIC = 0x00000008;
    private static final int ACC_FINAL = 0x00000010;
    //  private static final int ACC_SYNCHRONIZED           = 0x00000020;
    //  private static final int ACC_VOLATILE               = 0x00000040;
    //  private static final int ACC_TRANSIENT              = 0x00000080;
    //  private static final int ACC_NATIVE                 = 0x00000100;
    //  private static final int ACC_INTERFACE              = 0x00000200;
    //  private static final int ACC_ABSTRACT               = 0x00000400;
    private static final int ACC_SUPER = 0x00000020;
    //  private static final int ACC_STRICT                 = 0x00000800;

    /* opcodes */
    //  private static final int opc_nop                    = 0;
    private static final int opc_aconst_null = 1;
    //  private static final int opc_iconst_m1              = 2;
    private static final int opc_iconst_0 = 3;
    //  private static final int opc_iconst_1               = 4;
    //  private static final int opc_iconst_2               = 5;
    //  private static final int opc_iconst_3               = 6;
    //  private static final int opc_iconst_4               = 7;
    //  private static final int opc_iconst_5               = 8;
    //  private static final int opc_lconst_0               = 9;
    //  private static final int opc_lconst_1               = 10;
    //  private static final int opc_fconst_0               = 11;
    //  private static final int opc_fconst_1               = 12;
    //  private static final int opc_fconst_2               = 13;
    //  private static final int opc_dconst_0               = 14;
    //  private static final int opc_dconst_1               = 15;
    private static final int opc_bipush = 16;
    private static final int opc_sipush = 17;
    private static final int opc_ldc = 18;
    private static final int opc_ldc_w = 19;
    //  private static final int opc_ldc2_w                 = 20;
    private static final int opc_iload = 21;
    private static final int opc_lload = 22;
    private static final int opc_fload = 23;
    private static final int opc_dload = 24;
    private static final int opc_aload = 25;
    private static final int opc_iload_0 = 26;
    //  private static final int opc_iload_1                = 27;
    //  private static final int opc_iload_2                = 28;
    //  private static final int opc_iload_3                = 29;
    private static final int opc_lload_0 = 30;
    //  private static final int opc_lload_1                = 31;
    //  private static final int opc_lload_2                = 32;
    //  private static final int opc_lload_3                = 33;
    private static final int opc_fload_0 = 34;
    //  private static final int opc_fload_1                = 35;
    //  private static final int opc_fload_2                = 36;
    //  private static final int opc_fload_3                = 37;
    private static final int opc_dload_0 = 38;
    //  private static final int opc_dload_1                = 39;
    //  private static final int opc_dload_2                = 40;
    //  private static final int opc_dload_3                = 41;
    private static final int opc_aload_0 = 42;
    //  private static final int opc_aload_1                = 43;
    //  private static final int opc_aload_2                = 44;
    //  private static final int opc_aload_3                = 45;
    //  private static final int opc_iaload                 = 46;
    //  private static final int opc_laload                 = 47;
    //  private static final int opc_faload                 = 48;
    //  private static final int opc_daload                 = 49;
    //  private static final int opc_aaload                 = 50;
    //  private static final int opc_baload                 = 51;
    //  private static final int opc_caload                 = 52;
    //  private static final int opc_saload                 = 53;
    //  private static final int opc_istore                 = 54;
    //  private static final int opc_lstore                 = 55;
    //  private static final int opc_fstore                 = 56;
    //  private static final int opc_dstore                 = 57;
    private static final int opc_astore = 58;
    //  private static final int opc_istore_0               = 59;
    //  private static final int opc_istore_1               = 60;
    //  private static final int opc_istore_2               = 61;
    //  private static final int opc_istore_3               = 62;
    //  private static final int opc_lstore_0               = 63;
    //  private static final int opc_lstore_1               = 64;
    //  private static final int opc_lstore_2               = 65;
    //  private static final int opc_lstore_3               = 66;
    //  private static final int opc_fstore_0               = 67;
    //  private static final int opc_fstore_1               = 68;
    //  private static final int opc_fstore_2               = 69;
    //  private static final int opc_fstore_3               = 70;
    //  private static final int opc_dstore_0               = 71;
    //  private static final int opc_dstore_1               = 72;
    //  private static final int opc_dstore_2               = 73;
    //  private static final int opc_dstore_3               = 74;
    private static final int opc_astore_0 = 75;
    //  private static final int opc_astore_1               = 76;
    //  private static final int opc_astore_2               = 77;
    //  private static final int opc_astore_3               = 78;
    //  private static final int opc_iastore                = 79;
    //  private static final int opc_lastore                = 80;
    //  private static final int opc_fastore                = 81;
    //  private static final int opc_dastore                = 82;
    private static final int opc_aastore = 83;
    //  private static final int opc_bastore                = 84;
    //  private static final int opc_castore                = 85;
    //  private static final int opc_sastore                = 86;
    private static final int opc_pop = 87;
    //  private static final int opc_pop2                   = 88;
    private static final int opc_dup = 89;
    //  private static final int opc_dup_x1                 = 90;
    //  private static final int opc_dup_x2                 = 91;
    //  private static final int opc_dup2                   = 92;
    //  private static final int opc_dup2_x1                = 93;
    //  private static final int opc_dup2_x2                = 94;
    //  private static final int opc_swap                   = 95;
    //  private static final int opc_iadd                   = 96;
    //  private static final int opc_ladd                   = 97;
    //  private static final int opc_fadd                   = 98;
    //  private static final int opc_dadd                   = 99;
    //  private static final int opc_isub                   = 100;
    //  private static final int opc_lsub                   = 101;
    //  private static final int opc_fsub                   = 102;
    //  private static final int opc_dsub                   = 103;
    //  private static final int opc_imul                   = 104;
    //  private static final int opc_lmul                   = 105;
    //  private static final int opc_fmul                   = 106;
    //  private static final int opc_dmul                   = 107;
    //  private static final int opc_idiv                   = 108;
    //  private static final int opc_ldiv                   = 109;
    //  private static final int opc_fdiv                   = 110;
    //  private static final int opc_ddiv                   = 111;
    //  private static final int opc_irem                   = 112;
    //  private static final int opc_lrem                   = 113;
    //  private static final int opc_frem                   = 114;
    //  private static final int opc_drem                   = 115;
    //  private static final int opc_ineg                   = 116;
    //  private static final int opc_lneg                   = 117;
    //  private static final int opc_fneg                   = 118;
    //  private static final int opc_dneg                   = 119;
    //  private static final int opc_ishl                   = 120;
    //  private static final int opc_lshl                   = 121;
    //  private static final int opc_ishr                   = 122;
    //  private static final int opc_lshr                   = 123;
    //  private static final int opc_iushr                  = 124;
    //  private static final int opc_lushr                  = 125;
    //  private static final int opc_iand                   = 126;
    //  private static final int opc_land                   = 127;
    //  private static final int opc_ior                    = 128;
    //  private static final int opc_lor                    = 129;
    //  private static final int opc_ixor                   = 130;
    //  private static final int opc_lxor                   = 131;
    //  private static final int opc_iinc                   = 132;
    //  private static final int opc_i2l                    = 133;
    //  private static final int opc_i2f                    = 134;
    //  private static final int opc_i2d                    = 135;
    //  private static final int opc_l2i                    = 136;
    //  private static final int opc_l2f                    = 137;
    //  private static final int opc_l2d                    = 138;
    //  private static final int opc_f2i                    = 139;
    //  private static final int opc_f2l                    = 140;
    //  private static final int opc_f2d                    = 141;
    //  private static final int opc_d2i                    = 142;
    //  private static final int opc_d2l                    = 143;
    //  private static final int opc_d2f                    = 144;
    //  private static final int opc_i2b                    = 145;
    //  private static final int opc_i2c                    = 146;
    //  private static final int opc_i2s                    = 147;
    //  private static final int opc_lcmp                   = 148;
    //  private static final int opc_fcmpl                  = 149;
    //  private static final int opc_fcmpg                  = 150;
    //  private static final int opc_dcmpl                  = 151;
    //  private static final int opc_dcmpg                  = 152;
    //  private static final int opc_ifeq                   = 153;
    //  private static final int opc_ifne                   = 154;
    //  private static final int opc_iflt                   = 155;
    //  private static final int opc_ifge                   = 156;
    //  private static final int opc_ifgt                   = 157;
    //  private static final int opc_ifle                   = 158;
    //  private static final int opc_if_icmpeq              = 159;
    //  private static final int opc_if_icmpne              = 160;
    //  private static final int opc_if_icmplt              = 161;
    //  private static final int opc_if_icmpge              = 162;
    //  private static final int opc_if_icmpgt              = 163;
    //  private static final int opc_if_icmple              = 164;
    //  private static final int opc_if_acmpeq              = 165;
    //  private static final int opc_if_acmpne              = 166;
    //  private static final int opc_goto                   = 167;
    //  private static final int opc_jsr                    = 168;
    //  private static final int opc_ret                    = 169;
    //  private static final int opc_tableswitch            = 170;
    //  private static final int opc_lookupswitch           = 171;
    private static final int opc_ireturn = 172;
    private static final int opc_lreturn = 173;
    private static final int opc_freturn = 174;
    private static final int opc_dreturn = 175;
    private static final int opc_areturn = 176;
    private static final int opc_return = 177;
    private static final int opc_getstatic = 178;
    private static final int opc_putstatic = 179;
    private static final int opc_getfield = 180;
    //  private static final int opc_putfield               = 181;
    private static final int opc_invokevirtual = 182;
    private static final int opc_invokespecial = 183;
    private static final int opc_invokestatic = 184;
    private static final int opc_invokeinterface = 185;
    private static final int opc_new = 187;
    //  private static final int opc_newarray               = 188;
    private static final int opc_anewarray = 189;
    //  private static final int opc_arraylength            = 190;
    private static final int opc_athrow = 191;
    private static final int opc_checkcast = 192;
    //  private static final int opc_instanceof             = 193;
    //  private static final int opc_monitorenter           = 194;
    //  private static final int opc_monitorexit            = 195;
    private static final int opc_wide = 196;
    //  private static final int opc_multianewarray         = 197;
    //  private static final int opc_ifnull                 = 198;
    //  private static final int opc_ifnonnull              = 199;
    //  private static final int opc_goto_w                 = 200;
    //  private static final int opc_jsr_w                  = 201;

    // end of constants copied from sun.tools.java.RuntimeConstants

    /**
     * name of the superclass of proxy classes
     */
    private static final String superclassName = "java/lang/reflect/Proxy";

    /**
     * name of field for storing a proxy instance's invocation handler
     */
    private static final String handlerFieldName = "h";

    /**
     * 生成代理对象到文件里面
     */
    /**
     * debugging flag for saving generated class files
     */
    private static final boolean saveGeneratedFiles = java.security.AccessController.doPrivileged(new GetBooleanAction("jdk.proxy.ProxyGenerator.saveGeneratedFiles")).booleanValue();

    /**
     * Generate a public proxy class given a name and a list of proxy interfaces.
     */
    static byte[] generateProxyClass(final String name, Class<?>[] interfaces) {
        return generateProxyClass(name, interfaces, (ACC_PUBLIC | ACC_FINAL | ACC_SUPER));
    }

    /**
     * Generate a proxy class given a name and a list of proxy interfaces.
     *
     * @param name        the class name of the proxy class
     * @param interfaces  proxy interfaces
     * @param accessFlags access flags of the proxy class
     */
    /**
     * 生成代理对象
     *
     * @param name        代理对象全路径名称
     * @param interfaces  代理对象实现到接口
     * @param accessFlags 代理对象访问标识符
     * @return
     */
    static byte[] generateProxyClass(final String name, Class<?>[] interfaces, int accessFlags) {
        //
        ProxyGenerator gen = new ProxyGenerator(name, interfaces, accessFlags);
        // 获取byte数组
        final byte[] classFile = gen.generateClassFile();

        // 是否需要生成代理类到文件里面
        if (saveGeneratedFiles) {
            java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<Void>() {
                        public Void run() {
                            try {
                                int i = name.lastIndexOf('.');
                                Path path;
                                if (i > 0) {
                                    Path dir = Path.of(name.substring(0, i).replace('.', File.separatorChar));
                                    Files.createDirectories(dir);
                                    path = dir.resolve(name.substring(i + 1, name.length()) + ".class");
                                } else {
                                    path = Path.of(name + ".class");
                                }
                                Files.write(path, classFile);
                                System.out.println("生成代理类路径：" + path);
                                return null;
                            } catch (IOException e) {
                                throw new InternalError("I/O exception saving generated file: " + e);
                            }
                        }
                    });
        }

        return classFile;
    }

    /* preloaded Method objects for methods in java.lang.Object */
    private static Method hashCodeMethod;
    private static Method equalsMethod;
    private static Method toStringMethod;
    static {
        try {
            hashCodeMethod = Object.class.getMethod("hashCode");
            equalsMethod = Object.class.getMethod("equals", new Class<?>[]{Object.class});
            toStringMethod = Object.class.getMethod("toString");
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    /**
     * 代理对象全路径名称
     * name of proxy class
     */
    private String className;
    /**
     * 代理对象实现到接口
     * proxy interfaces
     */
    private Class<?>[] interfaces;
    /**
     * 代理对象访问标识符
     * proxy class access flags
     */
    private int accessFlags;

    /**
     * constant pool of class being generated
     */
    private ConstantPool cp = new ConstantPool();

    /**
     * FieldInfo struct for each field of generated class
     */
    private List<FieldInfo> fields = new ArrayList<>();

    /**
     * 所有的方法，需要代理的方法(proxyMethods)最终也是添加到这个集合里面
     * MethodInfo struct for each method of generated class
     */
    private List<MethodInfo> methods = new ArrayList<>();

    /**
     * 需要的代理到方法
     * key = method.getName() + getParameterDescriptors(method.getParameterTypes())
     * value=
     *
     * maps method signature string to list of ProxyMethod objects for proxy methods with that signature
     */
    private Map<String, List<ProxyMethod>> proxyMethods = new HashMap<>();

    /**
     * count of ProxyMethod objects added to proxyMethods
     */
    private int proxyMethodCount = 0;

    /**
     * Construct a ProxyGenerator to generate a proxy class with the
     * specified name and for the given interfaces.
     *
     * A ProxyGenerator object contains the state for the ongoing
     * generation of a particular proxy class.
     */
    /**
     * @param className   类名
     * @param interfaces  接口
     * @param accessFlags access_flag
     */
    private ProxyGenerator(String className, Class<?>[] interfaces, int accessFlags) {
        this.className = className;
        this.interfaces = interfaces;
        this.accessFlags = accessFlags;
    }

    /**
     * Generate a class file for the proxy class.  This method drives the
     * class file generation process.
     */
    private byte[] generateClassFile() {
        // class结构
        // ClassFile {
        //     u4             magic;//固定的开头，值为0xCAFEBABE
        //     u2             minor_version;//版本号，用来标记class的版本
        //     u2             major_version;//版本号，用来标记class的版本
        //     u2             constant_pool_count;//静态池大小，是静态池对象数量+1
        //     cp_info        constant_pool[constant_pool_count-1];//静态池对象，有效索引是1 ~ count-1
        //     u2             access_flags;//public、final等描述
        //     u2             this_class;//当前类的信息
        //     u2             super_class;//父类的信息
        //     u2             interfaces_count;//接口数量
        //     u2             interfaces[interfaces_count];//接口对象
        //     u2             fields_count;//字段数量
        //     field_info     fields[fields_count];//字段对象
        //     u2             methods_count;//方法数量
        //     method_info    methods[methods_count];//方法对象
        //     u2             attributes_count;//属性数量
        //     attribute_info attributes[attributes_count];//属性对象
        // }


        // 方法
        /* ============================================================
         * Step 1: Assemble ProxyMethod objects for all methods to
         * generate proxy dispatching code for.
         */
        /*
         * Record that proxy methods are needed for the hashCode, equals,
         * and toString methods of java.lang.Object.  This is done before
         * the methods from the proxy interfaces so that the methods from
         * java.lang.Object take precedence over duplicate methods in the
         * proxy interfaces.
         */
        // Object方法的预处理
        // hashCode
        addProxyMethod(hashCodeMethod, Object.class);
        // equals
        addProxyMethod(equalsMethod, Object.class);
        // toString
        addProxyMethod(toStringMethod, Object.class);
        // 接口方法的预处理
        /*
         * Now record all of the methods from the proxy interfaces, giving
         * earlier interfaces precedence over later ones with duplicate
         * methods.
         */
        for (Class<?> intf : interfaces) {
            for (Method m : intf.getMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) {
                    // 非static
                    addProxyMethod(m, intf);
                }
            }
        }
        // 检查
        /*
         * For each set of proxy methods with the same signature,
         * verify that the methods' return types are compatible.
         */
        for (List<ProxyMethod> sigmethods : proxyMethods.values()) {
            checkReturnTypes(sigmethods);
        }
        // 字段和方法的字节码写入
        /* ============================================================
         * Step 2: Assemble FieldInfo and MethodInfo structs for all of
         * fields and methods in the class we are generating.
         */
        try {
            // 有参构造函数 入参是InvocationHandler
            methods.add(generateConstructor());
            for (List<ProxyMethod> sigmethods : proxyMethods.values()) {
                for (ProxyMethod proxyMethod : sigmethods) {
                    // add static field for method's Method object
                    FieldInfo fieldInfo =  new FieldInfo(proxyMethod.methodFieldName, "Ljava/lang/reflect/Method;", ACC_PRIVATE | ACC_STATIC);
                    fields.add(fieldInfo);

                    // proxy方法的写入
                    // generate code for proxy method and add it
                    MethodInfo methodInfo = proxyMethod.generateMethod();
                    methods.add(methodInfo);
                }
            }
            //
            MethodInfo staticInitializer = generateStaticInitializer();
            methods.add(staticInitializer);
        } catch (IOException e) {
            throw new InternalError("unexpected I/O Exception", e);
        }
        if (methods.size() > 65535) {
            throw new IllegalArgumentException("method limit exceeded");
        }
        if (fields.size() > 65535) {
            throw new IllegalArgumentException("field limit exceeded");
        }

        // 常量池
        /* ============================================================
         * Step 3: Write the final class file.
         */
        /*
         * Make sure that constant pool indexes are reserved for the
         * following items before starting to write the final class file.
         */
        cp.getClass(dotToSlash(className));
        cp.getClass(superclassName);
        for (Class<?> intf : interfaces) {
            cp.getClass(dotToSlash(intf.getName()));
        }
        /*
         * Disallow new constant pool additions beyond this point, since
         * we are about to write the final constant pool table.
         */
        cp.setReadOnly();


        // 字节数组输出流
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            /*
             * Write all the items of the "ClassFile" structure.
             * See JVMS section 4.1.
             */
            // u1表示1个无符号字节，u4表示4个无符号字节

            // 1、魔术cafebabe
            // u4 magic;
            dataOutputStream.writeInt(0xCAFEBABE);

            // 2、副版本号
            // u2 minor_version;
            dataOutputStream.writeShort(CLASSFILE_MINOR_VERSION);

            // 3、主版本号
            // u2 major_version;
            dataOutputStream.writeShort(CLASSFILE_MAJOR_VERSION);

            // 4、常量池计数器+常量池数据区
            // 把输入流传进去了
            cp.write(dataOutputStream); // (write constant pool)

            // 6、访问标志
            // u2 access_flags;
            dataOutputStream.writeShort(accessFlags);

            // 7、类索引
            // u2 this_class;
            dataOutputStream.writeShort(cp.getClass(dotToSlash(className)));

            // 8、父类索引 java/lang/reflect/Proxy
            // u2 super_class;
            dataOutputStream.writeShort(cp.getClass(superclassName));

            // 9、接口计数器
            // u2 interfaces_count;
            dataOutputStream.writeShort(interfaces.length);
            // 10、接口信息计数区
            // u2 interfaces[interfaces_count];
            for (Class<?> intf : interfaces) {
                dataOutputStream.writeShort(cp.getClass(dotToSlash(intf.getName())));
            }

            // 11、字段(成员变量)计数器
            // u2 fields_count;
            dataOutputStream.writeShort(fields.size());
            // 12、字段(成员变量)信息数据区
            // field_info fields[fields_count];
            for (FieldInfo fieldInfo : fields) {
                fieldInfo.write(dataOutputStream);
            }

            // 13、方法计数器
            // u2 methods_count;
            dataOutputStream.writeShort(methods.size());

            // 14、方法信息数据区
            // method_info methods[methods_count];
            for (MethodInfo m : methods) {
                m.write(dataOutputStream);
            }

            // 15、属性计数器（代理对象没有属性）
            // u2 attributes_count;
            dataOutputStream.writeShort(0); // (no ClassFile attributes for proxy classes)

        } catch (IOException e) {
            throw new InternalError("unexpected I/O Exception", e);
        }
        // 返回byte数组
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Add another method to be proxied, either by creating a new
     * ProxyMethod object or augmenting an old one for a duplicate
     * method.
     * <p>
     * "fromClass" indicates the proxy interface that the method was
     * found through, which may be different from (a subinterface of)
     * the method's "declaring class".  Note that the first Method
     * object passed for a given name and descriptor identifies the
     * Method object (and thus the declaring class) that will be
     * passed to the invocation handler's "invoke" method for a given
     * set of duplicate methods.
     */
    private void addProxyMethod(Method method, Class<?> fromClass) {
        String name = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Class<?> returnType = method.getReturnType();
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        String sig = name + getParameterDescriptors(parameterTypes);
        List<ProxyMethod> sigmethods = proxyMethods.get(sig);
        if (sigmethods != null) {
            for (ProxyMethod pm : sigmethods) {
                if (returnType == pm.returnType) {
                    /*
                     * Found a match: reduce exception types to the
                     * greatest set of exceptions that can thrown
                     * compatibly with the throws clauses of both
                     * overridden methods.
                     */
                    List<Class<?>> legalExceptions = new ArrayList<>();
                    collectCompatibleTypes(exceptionTypes, pm.exceptionTypes, legalExceptions);
                    collectCompatibleTypes(pm.exceptionTypes, exceptionTypes, legalExceptions);
                    pm.exceptionTypes = new Class<?>[legalExceptions.size()];
                    pm.exceptionTypes = legalExceptions.toArray(pm.exceptionTypes);
                    return;
                }
            }
        } else {
            sigmethods = new ArrayList<>(3);
            proxyMethods.put(sig, sigmethods);
        }
        //
        sigmethods.add(new ProxyMethod(name, parameterTypes, returnType, exceptionTypes, fromClass));
    }

    /**
     * For a given set of proxy methods with the same signature, check
     * that their return types are compatible according to the Proxy
     * specification.
     * <p>
     * Specifically, if there is more than one such method, then all
     * of the return types must be reference types, and there must be
     * one return type that is assignable to each of the rest of them.
     */
    private static void checkReturnTypes(List<ProxyMethod> methods) {
        /*
         * If there is only one method with a given signature, there
         * cannot be a conflict.  This is the only case in which a
         * primitive (or void) return type is allowed.
         */
        if (methods.size() < 2) {
            return;
        }

        /*
         * List of return types that are not yet known to be
         * assignable from ("covered" by) any of the others.
         */
        LinkedList<Class<?>> uncoveredReturnTypes = new LinkedList<>();

        nextNewReturnType:
        for (ProxyMethod pm : methods) {
            Class<?> newReturnType = pm.returnType;
            if (newReturnType.isPrimitive()) {
                throw new IllegalArgumentException("methods with same signature " + getFriendlyMethodSignature(pm.methodName, pm.parameterTypes) + " but incompatible return types: " + newReturnType.getName() + " and others");
            }
            boolean added = false;

            /*
             * Compare the new return type to the existing uncovered
             * return types.
             */
            ListIterator<Class<?>> liter = uncoveredReturnTypes.listIterator();
            while (liter.hasNext()) {
                Class<?> uncoveredReturnType = liter.next();

                /*
                 * If an existing uncovered return type is assignable
                 * to this new one, then we can forget the new one.
                 */
                if (newReturnType.isAssignableFrom(uncoveredReturnType)) {
                    assert !added;
                    continue nextNewReturnType;
                }

                /*
                 * If the new return type is assignable to an existing
                 * uncovered one, then should replace the existing one
                 * with the new one (or just forget the existing one,
                 * if the new one has already be put in the list).
                 */
                if (uncoveredReturnType.isAssignableFrom(newReturnType)) {
                    // (we can assume that each return type is unique)
                    if (!added) {
                        liter.set(newReturnType);
                        added = true;
                    } else {
                        liter.remove();
                    }
                }
            }

            /*
             * If we got through the list of existing uncovered return
             * types without an assignability relationship, then add
             * the new return type to the list of uncovered ones.
             */
            if (!added) {
                uncoveredReturnTypes.add(newReturnType);
            }
        }

        /*
         * We shouldn't end up with more than one return type that is
         * not assignable from any of the others.
         */
        if (uncoveredReturnTypes.size() > 1) {
            ProxyMethod pm = methods.get(0);
            throw new IllegalArgumentException("methods with same signature " + getFriendlyMethodSignature(pm.methodName, pm.parameterTypes) + " but incompatible return types: " + uncoveredReturnTypes);
        }
    }

    /**
     * A FieldInfo object contains information about a particular field
     * in the class being generated.  The class mirrors the data items of
     * the "field_info" structure of the class file format (see JVMS 4.5).
     */
    private class FieldInfo {
        public int accessFlags;
        public String name;
        public String descriptor;

        /**
         *
         * @param name m+递增数字生成的methodFieldName
         * @param descriptor 类型描述
         * @param accessFlags accessFlags
         *        10表示private static (Modifier.PRIVATE | Modifier.STATIC)
         */
        public FieldInfo(String name, String descriptor, int accessFlags) {
            // field_info {
            //   u2  access_flags; // this.accessFlags
            //   u2  name_index; // this.name
            //   u2  descriptor_index; // this.descriptor
            // }
            this.name = name;
            this.descriptor = descriptor;
            this.accessFlags = accessFlags;
            /*
             * Make sure that constant pool indexes are reserved for the following items before starting to write the final class file.
             */
            cp.getUtf8(name);
            cp.getUtf8(descriptor);
        }

        public void write(DataOutputStream out) throws IOException {
            // 用于描述接口或类中声明的变量
            // field_info {
            //     u2             access_flags;
            //     u2             name_index;       //常量池中的一个有效索引，必须是Utf8类型（表示方法或字段的名字）
            //     u2             descriptor_index; //常量池中的一个有效索引，必须是Utf8类型（表示字段的描述）
            //     u2             attributes_count;
            //     attribute_info attributes[attributes_count];
            // }
            /*
             * Write all the items of the "field_info" structure. See JVMS section 4.5.
             */
            // u2 access_flags;
            out.writeShort(accessFlags);
            // u2 name_index;
            out.writeShort(cp.getUtf8(name));
            // u2 descriptor_index;
            out.writeShort(cp.getUtf8(descriptor));
            // u2 attributes_count;
            out.writeShort(0);  // (no field_info attributes for proxy classes)
        }
    }

    /**
     * An ExceptionTableEntry object holds values for the data items of
     * an entry in the "exception_table" item of the "Code" attribute of
     * "method_info" structures (see JVMS 4.7.3).
     */
    private static class ExceptionTableEntry {
        public short startPc;
        public short endPc;
        public short handlerPc;
        public short catchType;

        public ExceptionTableEntry(short startPc, short endPc, short handlerPc, short catchType) {
            this.startPc = startPc;
            this.endPc = endPc;
            this.handlerPc = handlerPc;
            this.catchType = catchType;
        }
    }

    ;

    /**
     * A MethodInfo object contains information about a particular method
     * in the class being generated.  This class mirrors the data items of
     * the "method_info" structure of the class file format (see JVMS 4.6).
     */
    private class MethodInfo {
        public int accessFlags;
        public String name;
        public String descriptor;
        public short maxStack;
        public short maxLocals;
        public ByteArrayOutputStream code = new ByteArrayOutputStream();
        public List<ExceptionTableEntry> exceptionTable = new ArrayList<ExceptionTableEntry>();
        public short[] declaredExceptions;

        /**
         *
         * @param name 方法名
         * @param descriptor 方法描述
         * @param accessFlags access_flag 1表示public（参见Modifier.java）
         */
        public MethodInfo(String name, String descriptor, int accessFlags) {
            // method_info {
            //     u2             access_flags;//access_flag
            //     u2             name_index;//常量池中的一个有效索引，必须是Utf8类型（表示方法或字段的名字）
            //     u2             descriptor_index;//常量池中的一个有效索引，必须是Utf8类型（表示方法的描述）
            //     u2             attributes_count;//属性数量
            //     attribute_info attributes[attributes_count];//属性的具体内容
            // }
            this.name = name;
            this.descriptor = descriptor;
            this.accessFlags = accessFlags;

            /*
             * Make sure that constant pool indexes are reserved for the
             * following items before starting to write the final class file.
             */
            cp.getUtf8(name);
            cp.getUtf8(descriptor);
            cp.getUtf8("Code"); // Code表示执行代码
            cp.getUtf8("Exceptions"); // Exceptions表示方法会抛出的异常
        }

        public void write(DataOutputStream out) throws IOException {
            // method_info {
            //     u2             access_flags;                 // access_flags
            //     u2             name_index;                   // 常量池中的一个有效索引，必须是Utf8类型（表示方法或字段的名字）
            //     u2             descriptor_index;             // 常量池中的一个有效索引，必须是Utf8类型（表示方法的描述）
            //     u2             attributes_count;             // 属性数量
            //     attribute_info attributes[attributes_count]; // 属性的具体内容
            // }

            /*
             * Write all the items of the "method_info" structure.
             * See JVMS section 4.6.
             */
            // u2 access_flags;
            out.writeShort(accessFlags);

            // u2 name_index;
            out.writeShort(cp.getUtf8(name));

            // u2 descriptor_index;
            out.writeShort(cp.getUtf8(descriptor));

            // 写入属性的数量
            // u2 attributes_count;
            out.writeShort(2);  // (two method_info attributes:)

            // Code属性
            // Write "Code" attribute. See JVMS section 4.7.3.
            // Code_attribute {
            //     u2 attribute_name_index;
            //     u4 attribute_length;
            //     u2 max_stack;
            //     u2 max_locals;
            //     u4 code_length;
            //     u1 code[code_length];
            //     u2 exception_table_length;
            //     {   u2 start_pc;
            //         u2 end_pc;
            //         u2 handler_pc;
            //         u2 catch_type;
            //     } exception_table[exception_table_length];
            //     u2 attributes_count;
            //     attribute_info attributes[attributes_count];
            // }
            //
            // attribute_info {
            //     u2 attribute_name_index;//名字在常量池的索引
            //     u4 attribute_length;//attribute的字节长度
            //     u1 info[attribute_length];//attribute的实际数据
            // }

            // 执行代码
            //
            // u2 attribute_name_index;
            out.writeShort(cp.getUtf8("Code"));

            // 12 = max_stack + max_locals + code_length + exception_table_length + attributes_count
            // 8 = start_pc、end_pc、handler_pc、catch_type
            // u4 attribute_length;
            out.writeInt(12 + code.size() + 8 * exceptionTable.size());

            // 写入栈深max_stack和max_locals本地变量数量，这2个值在generateMethod()方法详细介绍中涉及到
            // u2 max_stack;
            out.writeShort(maxStack);
            // u2 max_locals;
            out.writeShort(maxLocals);

            // 写入方法执行体字节的长度code_length和方法执行体具体字节code[code_length]，这部分也会在generateMethod()方法中涉及到
            // u2 code_length;
            out.writeInt(code.size());
            // u1 code[code_length];
            code.writeTo(out);

            // 写入方法会抛出的异常数量exception_table_length
            // u2 exception_table_length;
            out.writeShort(exceptionTable.size());
            // 写入异常的具体结构
            for (ExceptionTableEntry e : exceptionTable) {
                // 每一个异常都有4个字段，start_pc、end_pc、handler_pc、catch_type，都是short类型，因此一个Exception就会有8个字节，这个8正对应了上面attribute_length中的8
                // u2 start_pc;
                out.writeShort(e.startPc);

                // u2 end_pc;
                out.writeShort(e.endPc);

                // u2 handler_pc;
                out.writeShort(e.handlerPc);

                // u2 catch_type;
                out.writeShort(e.catchType);
            }
            // 最后写入attributes自身的attributes_count，因为没有，所以直接写0
            // 这个数量是一个short类型，加上之前累积的10个字节，一共12个字节，对应了attribute_length中的12
            // u2 attributes_count;
            out.writeShort(0);

            // write "Exceptions" attribute.  See JVMS section 4.7.4.
            // Exceptions_attribute {
            //   u2 attribute_name_index;
            //   u4 attribute_length;
            //   u2 number_of_exceptions;
            //   u2 exception_index_table[number_of_exceptions];
            // }
            // 方法会抛出的异常
            // 写入常量池的索引attribute_name_index
            // u2 attribute_name_index;
            out.writeShort(cp.getUtf8("Exceptions"));

            // 写入常量池的索引attribute_name_index
            // u4 attributes_length;
            out.writeInt(2 + 2 * declaredExceptions.length);

            // u2 number_of_exceptions;
            out.writeShort(declaredExceptions.length);

            // u2 exception_index_table[number_of_exceptions];
            for (short value : declaredExceptions) {
                out.writeShort(value);
            }
        }

    }

    /**
     * 代理方法
     *
     * A ProxyMethod object represents a proxy method in the proxy class
     * being generated: a method whose implementation will encode and
     * dispatch invocations to the proxy instance's invocation handler.
     */
    private class ProxyMethod {
        /**
         * 方法名称
         * eg: equals
         */
        public String methodName;
        /**
         *
         */
        public Class<?>[] parameterTypes;
        /**
         *
         */
        public Class<?> returnType;
        /**
         *
         */
        public Class<?>[] exceptionTypes;
        /**
         *
         */
        public Class<?> fromClass;
        /**
         *
         */
        public String methodFieldName;

        /**
         *
         * @param methodName  methodName
         * @param parameterTypes parameterTypes
         * @param returnType returnType
         * @param exceptionTypes exceptionTypes
         * @param fromClass fromClass
         */
        private ProxyMethod(String methodName, Class<?>[] parameterTypes, Class<?> returnType, Class<?>[] exceptionTypes, Class<?> fromClass) {
            this.methodName = methodName; // 方法名
            this.parameterTypes = parameterTypes; //
            this.returnType = returnType;
            this.exceptionTypes = exceptionTypes;
            this.fromClass = fromClass;
            this.methodFieldName = "m" + proxyMethodCount++; // 该方法在最终生成的类中的Method类型的字段的名称
        }

        /**
         * proxy方法的写入
         *
         * Return a MethodInfo object for this method, including generating the code and exception table entry.
         */
        // 反编译代理类
        // public final java.lang.String sayHi(java.lang.String, java.lang.Integer) throws java.io.FileNotFoundException;
        //   descriptor: (Ljava/lang/String;Ljava/lang/Integer;)Ljava/lang/String;
        //   flags: (0x0011) ACC_PUBLIC, ACC_FINAL
        //   Code:
        //     stack=7, locals=4, args_size=3
        //        0: aload_0
        //        1: getfield      #2                  // Field java/lang/reflect/Proxy.h:Ljava/lang/reflect/InvocationHandler;
        //        4: aload_0
        //        5: getstatic     #13                 // Field m3:Ljava/lang/reflect/Method;
        //        8: iconst_2
        //        9: anewarray     #4                  // class java/lang/Object
        //       12: dup
        //       13: iconst_0
        //       14: aload_1
        //       15: aastore
        //       16: dup
        //       17: iconst_1
        //       18: aload_2
        //       19: aastore
        //       20: invokeinterface #5,  4            // InterfaceMethod java/lang/reflect/InvocationHandler.invoke:(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;
        //       25: checkcast     #14                 // class java/lang/String
        //       28: areturn
        //       29: astore_3
        //       30: aload_3
        //       31: athrow
        //       32: astore_3
        //       33: new           #11                 // class java/lang/reflect/UndeclaredThrowableException
        //       36: dup
        //       37: aload_3
        //       38: invokespecial #12                 // Method java/lang/reflect/UndeclaredThrowableException."<init>":(Ljava/lang/Throwable;)V
        //       41: athrow
        //     Exception table:
        //        from    to  target type
        //            0    28    29   Class java/lang/RuntimeException
        //            0    28    29   Class java/io/FileNotFoundException
        //            0    28    29   Class java/lang/Error
        //            0    28    32   Class java/lang/Throwable
        //     LineNumberTable:
        //       line 32: 0
        //       line 33: 29
        //       line 34: 30
        //       line 35: 32
        //       line 36: 33
        //     LocalVariableTable:
        //       Start  Length  Slot  Name   Signature
        //          30       2     3  var4   Ljava/lang/Throwable;
        //          33       9     3  var5   Ljava/lang/Throwable;
        //           0      42     0  this   Lcn/baker/jvm/dynamicproxy/out/MyProxy;
        //           0      42     1  var1   Ljava/lang/String;
        //           0      42     2  var2   Ljava/lang/Integer;
        //     StackMapTable: number_of_entries = 2
        //       frame_type = 93 /* same_locals_1_stack_item */
        //         stack = [ class java/lang/Throwable ]
        //       frame_type = 66 /* same_locals_1_stack_item */
        //         stack = [ class java/lang/Throwable ]
        //   Exceptions:
        //     throws java.io.FileNotFoundException
        private MethodInfo generateMethod() throws IOException {
            // 获取方法的描述，类似于 ()V 描述方法的参数和返回参数，这里()V表示获取0个参数，返回为void的方法
            String desc = getMethodDescriptor(parameterTypes, returnType);
            // 生成一个MethodInfo对象
            MethodInfo minfo = new MethodInfo(methodName, desc, ACC_PUBLIC | ACC_FINAL);
            // 存放静态池编号的数组
            int[] parameterSlot = new int[parameterTypes.length];

            //
            int nextSlot = 1;
            for (int i = 0; i < parameterSlot.length; i++) {
                parameterSlot[i] = nextSlot;
                nextSlot += getWordsPerType(parameterTypes[i]);
            }

            int localSlot0 = nextSlot;
            short pc;
            short tryBegin = 0;
            short tryEnd;

            DataOutputStream out = new DataOutputStream(minfo.code);
            // aload_0，加载栈帧本地变量表的第一个参数到操作栈，因为是实例方法，所以是就是指this
            code_aload(0, out);

            // getfield，获取this的实例字段
            out.writeByte(opc_getfield);

            // 从Proxy类中，获取类型是InvocationHandler，字段名为h的对象
            out.writeShort(cp.getFieldRef(superclassName, handlerFieldName, "Ljava/lang/reflect/InvocationHandler;"));

            // aload_0
            code_aload(0, out);

            // getstatic，获取静态字段
            out.writeByte(opc_getstatic);

            // 获取当前代理类，名字是methodFieldName，类型是Method的对象（之前在写入静态池的时候，用的也是methodFieldName）
            out.writeShort(cp.getFieldRef(dotToSlash(className), methodFieldName, "Ljava/lang/reflect/Method;"));

            // 准备写入参数
            if (parameterTypes.length > 0) {
                /**
                 * 写入参数的数量，如果再仔细看一下code_ipush
                 * 当length小于等于5时，写入的命令是iconst_m1~iconst_5
                 * 当length在-128~127闭区间时，写入的命令是bipush
                 * 否则就写入sipush
                 */
                code_ipush(parameterTypes.length, out);
                // anewarray，创建一个数组
                out.writeByte(opc_anewarray);
                // 数组的类型是object
                out.writeShort(cp.getClass("java/lang/Object"));
                // 循环参数
                for (int i = 0; i < parameterTypes.length; i++) {
                    // dup，复制栈顶的操作数
                    out.writeByte(opc_dup);
                    // iconst、bipush、sipush
                    code_ipush(i, out);
                    // 对参数类型等做一个编码
                    codeWrapArgument(parameterTypes[i], parameterSlot[i], out);
                    // aastore，将对象存入数组
                    out.writeByte(opc_aastore);
                }
            } else {
                // 如果没参数的话 aconst_null，push一个null
                out.writeByte(opc_aconst_null);
            }
            // invokeinterface 调用接口方法
            out.writeByte(opc_invokeinterface);
            // 找到InvocationHandler的invoke方法
            out.writeShort(cp.getInterfaceMethodRef("java/lang/reflect/InvocationHandler", "invoke", "(Ljava/lang/Object;Ljava/lang/reflect/Method;" + "[Ljava/lang/Object;)Ljava/lang/Object;"));
            // iconst_1，将1压入操作栈
            out.writeByte(4);
            // nop，不做事情
            out.writeByte(0);

            if (returnType == void.class) {
                // 如果是void方法 pop，将栈顶的操作数弹出
                out.writeByte(opc_pop);
                // return
                out.writeByte(opc_return);
            } else {
                // 对返回值进行编码
                codeUnwrapReturnValue(returnType, out);
            }
            //
            tryEnd = pc = (short) minfo.code.size();
            // 获取方法可能抛出的异常
            List<Class<?>> catchList = computeUniqueCatchList(exceptionTypes);
            if (catchList.size() > 0) {
                // 对异常进行预处理
                for (Class<?> ex : catchList) {
                    // 这里注意tryBegin, tryEnd, pc参数，和pc register有关，用于抛出Exception时能确定接下去要执行的指令
                    minfo.exceptionTable.add(new ExceptionTableEntry(tryBegin, tryEnd, pc, cp.getClass(dotToSlash(ex.getName()))));
                }
                // athrow，抛出异常
                out.writeByte(opc_athrow);
                // 重新获取异常的处理点
                pc = (short) minfo.code.size();
                // 添加异常的基类
                minfo.exceptionTable.add(new ExceptionTableEntry(tryBegin, tryEnd, pc, cp.getClass("java/lang/Throwable")));
                /**
                 * 根据constantPoolNumber的值
                 * astore_0 = 75 (0x4b)
                 * astore_1 = 76 (0x4c)
                 * astore_2 = 77 (0x4d)
                 * astore_3 = 78 (0x4e)
                 * astore
                 */
                code_astore(localSlot0, out);
                // new 创建一个新对象
                out.writeByte(opc_new);
                // 对象是UndeclaredThrowableException
                out.writeShort(cp.getClass("java/lang/reflect/UndeclaredThrowableException"));
                // dup 复制栈顶操作数
                out.writeByte(opc_dup);
                /**
                 * 根据constantPoolNumber的值
                 * aload_0 = 42 (0x2a)
                 * aload_1 = 43 (0x2b)
                 * aload_2 = 44 (0x2c)
                 * aload_3 = 45 (0x2d)
                 * aload
                 */
                code_aload(localSlot0, out);
                // invokespecial，调用父类的方法
                out.writeByte(opc_invokespecial);
                // 父类的构造函数
                out.writeShort(cp.getMethodRef("java/lang/reflect/UndeclaredThrowableException", "<init>", "(Ljava/lang/Throwable;)V"));
                // athrow,抛出异常
                out.writeByte(opc_athrow);
            }

            if (minfo.code.size() > 65535) {
                throw new IllegalArgumentException("code size limit exceeded");
            }

            minfo.maxStack = 10;
            minfo.maxLocals = (short) (localSlot0 + 1);
            minfo.declaredExceptions = new short[exceptionTypes.length];
            for (int i = 0; i < exceptionTypes.length; i++) {
                minfo.declaredExceptions[i] = cp.getClass(dotToSlash(exceptionTypes[i].getName()));
            }

            return minfo;
        }

        /**
         * Generate code for wrapping an argument of the given type
         * whose value can be found at the specified local variable
         * index, in order for it to be passed (as an Object) to the
         * invocation handler's "invoke" method.  The code is written
         * to the supplied stream.
         */
        private void codeWrapArgument(Class<?> type, int slot,
                                      DataOutputStream out)
                throws IOException {
            if (type.isPrimitive()) {
                PrimitiveTypeInfo prim = PrimitiveTypeInfo.get(type);

                if (type == int.class ||
                        type == boolean.class ||
                        type == byte.class ||
                        type == char.class ||
                        type == short.class) {
                    code_iload(slot, out);
                } else if (type == long.class) {
                    code_lload(slot, out);
                } else if (type == float.class) {
                    code_fload(slot, out);
                } else if (type == double.class) {
                    code_dload(slot, out);
                } else {
                    throw new AssertionError();
                }

                out.writeByte(opc_invokestatic);
                out.writeShort(cp.getMethodRef(
                        prim.wrapperClassName,
                        "valueOf", prim.wrapperValueOfDesc));

            } else {

                code_aload(slot, out);
            }
        }

        /**
         * Generate code for unwrapping a return value of the given
         * type from the invocation handler's "invoke" method (as type
         * Object) to its correct type.  The code is written to the
         * supplied stream.
         */
        private void codeUnwrapReturnValue(Class<?> type, DataOutputStream out)
                throws IOException {
            if (type.isPrimitive()) {
                PrimitiveTypeInfo prim = PrimitiveTypeInfo.get(type);

                out.writeByte(opc_checkcast);
                out.writeShort(cp.getClass(prim.wrapperClassName));

                out.writeByte(opc_invokevirtual);
                out.writeShort(cp.getMethodRef(
                        prim.wrapperClassName,
                        prim.unwrapMethodName, prim.unwrapMethodDesc));

                if (type == int.class ||
                        type == boolean.class ||
                        type == byte.class ||
                        type == char.class ||
                        type == short.class) {
                    out.writeByte(opc_ireturn);
                } else if (type == long.class) {
                    out.writeByte(opc_lreturn);
                } else if (type == float.class) {
                    out.writeByte(opc_freturn);
                } else if (type == double.class) {
                    out.writeByte(opc_dreturn);
                } else {
                    throw new AssertionError();
                }

            } else {

                out.writeByte(opc_checkcast);
                out.writeShort(cp.getClass(dotToSlash(type.getName())));

                out.writeByte(opc_areturn);
            }
        }

        /**
         * Generate code for initializing the static field that stores
         * the Method object for this proxy method.  The code is written
         * to the supplied stream.
         */
        private void codeFieldInitialization(DataOutputStream out)
                throws IOException {
            codeClassForName(fromClass, out);

            code_ldc(cp.getString(methodName), out);

            code_ipush(parameterTypes.length, out);

            out.writeByte(opc_anewarray);
            out.writeShort(cp.getClass("java/lang/Class"));

            for (int i = 0; i < parameterTypes.length; i++) {

                out.writeByte(opc_dup);

                code_ipush(i, out);

                if (parameterTypes[i].isPrimitive()) {
                    PrimitiveTypeInfo prim =
                            PrimitiveTypeInfo.get(parameterTypes[i]);

                    out.writeByte(opc_getstatic);
                    out.writeShort(cp.getFieldRef(
                            prim.wrapperClassName, "TYPE", "Ljava/lang/Class;"));

                } else {
                    codeClassForName(parameterTypes[i], out);
                }

                out.writeByte(opc_aastore);
            }

            out.writeByte(opc_invokevirtual);
            out.writeShort(cp.getMethodRef(
                    "java/lang/Class",
                    "getMethod",
                    "(Ljava/lang/String;[Ljava/lang/Class;)" +
                            "Ljava/lang/reflect/Method;"));

            out.writeByte(opc_putstatic);
            out.writeShort(cp.getFieldRef(
                    dotToSlash(className),
                    methodFieldName, "Ljava/lang/reflect/Method;"));
        }
    }

    /**
     * Generate the constructor method for the proxy class.
     */
    // 反编译动态代理类 javap -v MyProxy.class
    // public cn.baker.jvm.dynamicproxy.out.MyProxy(java.lang.reflect.InvocationHandler);
    //   descriptor: (Ljava/lang/reflect/InvocationHandler;)V
    //   flags: (0x0001) ACC_PUBLIC
    //   Code:
    //     stack=2, locals=2, args_size=2
    //        0: aload_0
    //        1: aload_1
    //        2: invokespecial #1                  // Method java/lang/reflect/Proxy."<init>":(Ljava/lang/reflect/InvocationHandler;)V
    //        5: return
    //     LineNumberTable:
    //       line 17: 0
    //       line 18: 5
    //     LocalVariableTable:
    //       Start  Length  Slot  Name   Signature
    //           0       6     0  this   Lcn/baker/jvm/dynamicproxy/out/MyProxy;
    //           0       6     1  var1   Ljava/lang/reflect/InvocationHandler;
    private MethodInfo generateConstructor() throws IOException {
        // 方法名       ：构造函数，所以方法名为<init>
        // 方法描述     ：方法的描述表示，该方法获取一个java.lang.reflect.InvocationHandler类型的参数，返回值为V（表示void）
        // access_flag ：方法的access_flag为1，表示public
        MethodInfo minfo = new MethodInfo("<init>", "(Ljava/lang/reflect/InvocationHandler;)V", ACC_PUBLIC);

        DataOutputStream out = new DataOutputStream(minfo.code);
        // aload_0
        code_aload(0, out);

        // aload_1
        code_aload(1, out);

        // 183号操作指令 invokespecial 调用实例方法，特别用来处理父类的构造函数
        out.writeByte(opc_invokespecial);

        // 在Code中写入需要调用的方法名和方法的参数
        // 注意：这里的方法是通过cp.getMethodRef方法得到的。也就是说，这里写入的最终数据，其实是一个符合该方法描述的常量池中的一个有效索引
        out.writeShort(cp.getMethodRef(superclassName, "<init>", "(Ljava/lang/reflect/InvocationHandler;)V"));

        // 在Code中写入177号指令 return
        out.writeByte(opc_return);

        // 注意这里并非是直接writeByte，而是对MethodInfo的属性做了一个设置，这部分的字节码依然会在MethodInfo的write方法中写入
        // 栈深和本地变量数量
        minfo.maxStack = 10;
        minfo.maxLocals = 2;

        // 方法会抛出的异常数量，因为构造函数不主动抛出异常，所以异常数量直接为0
        minfo.declaredExceptions = new short[0];
        return minfo;
    }

    /**
     * Generate the static initializer method for the proxy class.
     */
    private MethodInfo generateStaticInitializer() throws IOException {
        MethodInfo minfo = new MethodInfo("<clinit>", "()V", ACC_STATIC);

        int localSlot0 = 1;
        short pc, tryBegin = 0, tryEnd;

        DataOutputStream out = new DataOutputStream(minfo.code);

        for (List<ProxyMethod> sigmethods : proxyMethods.values()) {
            for (ProxyMethod pm : sigmethods) {
                pm.codeFieldInitialization(out);
            }
        }

        out.writeByte(opc_return);

        tryEnd = pc = (short) minfo.code.size();

        minfo.exceptionTable.add(new ExceptionTableEntry(tryBegin, tryEnd, pc, cp.getClass("java/lang/NoSuchMethodException")));

        code_astore(localSlot0, out);

        out.writeByte(opc_new);
        out.writeShort(cp.getClass("java/lang/NoSuchMethodError"));

        out.writeByte(opc_dup);

        code_aload(localSlot0, out);

        out.writeByte(opc_invokevirtual);
        out.writeShort(cp.getMethodRef("java/lang/Throwable", "getMessage", "()Ljava/lang/String;"));

        out.writeByte(opc_invokespecial);
        out.writeShort(cp.getMethodRef("java/lang/NoSuchMethodError", "<init>", "(Ljava/lang/String;)V"));

        out.writeByte(opc_athrow);

        pc = (short) minfo.code.size();

        minfo.exceptionTable.add(new ExceptionTableEntry(tryBegin, tryEnd, pc, cp.getClass("java/lang/ClassNotFoundException")));

        code_astore(localSlot0, out);

        out.writeByte(opc_new);
        out.writeShort(cp.getClass("java/lang/NoClassDefFoundError"));

        out.writeByte(opc_dup);

        code_aload(localSlot0, out);

        out.writeByte(opc_invokevirtual);
        out.writeShort(cp.getMethodRef("java/lang/Throwable", "getMessage", "()Ljava/lang/String;"));

        out.writeByte(opc_invokespecial);
        out.writeShort(cp.getMethodRef("java/lang/NoClassDefFoundError", "<init>", "(Ljava/lang/String;)V"));

        out.writeByte(opc_athrow);

        if (minfo.code.size() > 65535) {
            throw new IllegalArgumentException("code size limit exceeded");
        }

        minfo.maxStack = 10;
        minfo.maxLocals = (short) (localSlot0 + 1);
        minfo.declaredExceptions = new short[0];

        return minfo;
    }


    /*
     * =============== Code Generation Utility Methods ===============
     */

    /*
     * The following methods generate code for the load or store operation
     * indicated by their name for the given local variable.  The code is
     * written to the supplied stream.
     */

    private void code_iload(int lvar, DataOutputStream out)
            throws IOException {
        codeLocalLoadStore(lvar, opc_iload, opc_iload_0, out);
    }

    private void code_lload(int lvar, DataOutputStream out)
            throws IOException {
        codeLocalLoadStore(lvar, opc_lload, opc_lload_0, out);
    }

    private void code_fload(int lvar, DataOutputStream out)
            throws IOException {
        codeLocalLoadStore(lvar, opc_fload, opc_fload_0, out);
    }

    private void code_dload(int lvar, DataOutputStream out)
            throws IOException {
        codeLocalLoadStore(lvar, opc_dload, opc_dload_0, out);
    }

    private void code_aload(int lvar, DataOutputStream out)
            throws IOException {
        codeLocalLoadStore(lvar, opc_aload, opc_aload_0, out);
    }

//  private void code_istore(int lvar, DataOutputStream out)
//      throws IOException
//  {
//      codeLocalLoadStore(lvar, opc_istore, opc_istore_0, out);
//  }

//  private void code_lstore(int lvar, DataOutputStream out)
//      throws IOException
//  {
//      codeLocalLoadStore(lvar, opc_lstore, opc_lstore_0, out);
//  }

//  private void code_fstore(int lvar, DataOutputStream out)
//      throws IOException
//  {
//      codeLocalLoadStore(lvar, opc_fstore, opc_fstore_0, out);
//  }

//  private void code_dstore(int lvar, DataOutputStream out)
//      throws IOException
//  {
//      codeLocalLoadStore(lvar, opc_dstore, opc_dstore_0, out);
//  }

    private void code_astore(int lvar, DataOutputStream out)
            throws IOException {
        codeLocalLoadStore(lvar, opc_astore, opc_astore_0, out);
    }

    /**
     * Generate code for a load or store instruction for the given local
     * variable.  The code is written to the supplied stream.
     * <p>
     * "opcode" indicates the opcode form of the desired load or store
     * instruction that takes an explicit local variable index, and
     * "opcode_0" indicates the corresponding form of the instruction
     * with the implicit index 0.
     */
    private void codeLocalLoadStore(int lvar, int opcode, int opcode_0,
                                    DataOutputStream out)
            throws IOException {
        assert lvar >= 0 && lvar <= 0xFFFF;
        if (lvar <= 3) {
            out.writeByte(opcode_0 + lvar);
        } else if (lvar <= 0xFF) {
            out.writeByte(opcode);
            out.writeByte(lvar & 0xFF);
        } else {
            /*
             * Use the "wide" instruction modifier for local variable
             * indexes that do not fit into an unsigned byte.
             */
            out.writeByte(opc_wide);
            out.writeByte(opcode);
            out.writeShort(lvar & 0xFFFF);
        }
    }

    /**
     * Generate code for an "ldc" instruction for the given constant pool
     * index (the "ldc_w" instruction is used if the index does not fit
     * into an unsigned byte).  The code is written to the supplied stream.
     */
    private void code_ldc(int index, DataOutputStream out)
            throws IOException {
        assert index >= 0 && index <= 0xFFFF;
        if (index <= 0xFF) {
            out.writeByte(opc_ldc);
            out.writeByte(index & 0xFF);
        } else {
            out.writeByte(opc_ldc_w);
            out.writeShort(index & 0xFFFF);
        }
    }

    /**
     * Generate code to push a constant integer value on to the operand
     * stack, using the "iconst_<i>", "bipush", or "sipush" instructions
     * depending on the size of the value.  The code is written to the
     * supplied stream.
     */
    private void code_ipush(int value, DataOutputStream out)
            throws IOException {
        if (value >= -1 && value <= 5) {
            out.writeByte(opc_iconst_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            out.writeByte(opc_bipush);
            out.writeByte(value & 0xFF);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            out.writeByte(opc_sipush);
            out.writeShort(value & 0xFFFF);
        } else {
            throw new AssertionError();
        }
    }

    /**
     * Generate code to invoke the Class.forName with the name of the given
     * class to get its Class object at runtime.  The code is written to
     * the supplied stream.  Note that the code generated by this method
     * may caused the checked ClassNotFoundException to be thrown.
     */
    private void codeClassForName(Class<?> cl, DataOutputStream out)
            throws IOException {
        code_ldc(cp.getString(cl.getName()), out);

        out.writeByte(opc_invokestatic);
        out.writeShort(cp.getMethodRef(
                "java/lang/Class",
                "forName", "(Ljava/lang/String;)Ljava/lang/Class;"));
    }


    /*
     * ==================== General Utility Methods ====================
     */

    /**
     * Convert a fully qualified class name that uses '.' as the package
     * separator, the external representation used by the Java language
     * and APIs, to a fully qualified class name that uses '/' as the
     * package separator, the representation used in the class file
     * format (see JVMS section 4.2).
     */
    private static String dotToSlash(String name) {
        return name.replace('.', '/');
    }

    /**
     * Return the "method descriptor" string for a method with the given
     * parameter types and return type.  See JVMS section 4.3.3.
     */
    private static String getMethodDescriptor(Class<?>[] parameterTypes,
                                              Class<?> returnType) {
        return getParameterDescriptors(parameterTypes) +
                ((returnType == void.class) ? "V" : getFieldType(returnType));
    }

    /**
     * Return the list of "parameter descriptor" strings enclosed in
     * parentheses corresponding to the given parameter types (in other
     * words, a method descriptor without a return descriptor).  This
     * string is useful for constructing string keys for methods without
     * regard to their return type.
     */
    private static String getParameterDescriptors(Class<?>[] parameterTypes) {
        StringBuilder desc = new StringBuilder("(");
        for (int i = 0; i < parameterTypes.length; i++) {
            desc.append(getFieldType(parameterTypes[i]));
        }
        desc.append(')');
        return desc.toString();
    }

    /**
     * Return the "field type" string for the given type, appropriate for
     * a field descriptor, a parameter descriptor, or a return descriptor
     * other than "void".  See JVMS section 4.3.2.
     */
    private static String getFieldType(Class<?> type) {
        if (type.isPrimitive()) {
            return PrimitiveTypeInfo.get(type).baseTypeString;
        } else if (type.isArray()) {
            /*
             * According to JLS 20.3.2, the getName() method on Class does
             * return the VM type descriptor format for array classes (only);
             * using that should be quicker than the otherwise obvious code:
             *
             *     return "[" + getTypeDescriptor(type.getComponentType());
             */
            return type.getName().replace('.', '/');
        } else {
            return "L" + dotToSlash(type.getName()) + ";";
        }
    }

    /**
     * Returns a human-readable string representing the signature of a
     * method with the given name and parameter types.
     */
    private static String getFriendlyMethodSignature(String name,
                                                     Class<?>[] parameterTypes) {
        StringBuilder sig = new StringBuilder(name);
        sig.append('(');
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                sig.append(',');
            }
            Class<?> parameterType = parameterTypes[i];
            int dimensions = 0;
            while (parameterType.isArray()) {
                parameterType = parameterType.getComponentType();
                dimensions++;
            }
            sig.append(parameterType.getName());
            while (dimensions-- > 0) {
                sig.append("[]");
            }
        }
        sig.append(')');
        return sig.toString();
    }

    /**
     * Return the number of abstract "words", or consecutive local variable
     * indexes, required to contain a value of the given type.  See JVMS
     * section 3.6.1.
     * <p>
     * Note that the original version of the JVMS contained a definition of
     * this abstract notion of a "word" in section 3.4, but that definition
     * was removed for the second edition.
     */
    private static int getWordsPerType(Class<?> type) {
        if (type == long.class || type == double.class) {
            return 2;
        } else {
            return 1;
        }
    }

    /**
     * Add to the given list all of the types in the "from" array that
     * are not already contained in the list and are assignable to at
     * least one of the types in the "with" array.
     * <p>
     * This method is useful for computing the greatest common set of
     * declared exceptions from duplicate methods inherited from
     * different interfaces.
     */
    private static void collectCompatibleTypes(Class<?>[] from,
                                               Class<?>[] with,
                                               List<Class<?>> list) {
        for (Class<?> fc : from) {
            if (!list.contains(fc)) {
                for (Class<?> wc : with) {
                    if (wc.isAssignableFrom(fc)) {
                        list.add(fc);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Given the exceptions declared in the throws clause of a proxy method,
     * compute the exceptions that need to be caught from the invocation
     * handler's invoke method and rethrown intact in the method's
     * implementation before catching other Throwables and wrapping them
     * in UndeclaredThrowableExceptions.
     * <p>
     * The exceptions to be caught are returned in a List object.  Each
     * exception in the returned list is guaranteed to not be a subclass of
     * any of the other exceptions in the list, so the catch blocks for
     * these exceptions may be generated in any order relative to each other.
     * <p>
     * Error and RuntimeException are each always contained by the returned
     * list (if none of their superclasses are contained), since those
     * unchecked exceptions should always be rethrown intact, and thus their
     * subclasses will never appear in the returned list.
     * <p>
     * The returned List will be empty if java.lang.Throwable is in the
     * given list of declared exceptions, indicating that no exceptions
     * need to be caught.
     */
    private static List<Class<?>> computeUniqueCatchList(Class<?>[] exceptions) {
        List<Class<?>> uniqueList = new ArrayList<>();
        // unique exceptions to catch

        uniqueList.add(Error.class);            // always catch/rethrow these
        uniqueList.add(RuntimeException.class);

        nextException:
        for (Class<?> ex : exceptions) {
            if (ex.isAssignableFrom(Throwable.class)) {
                /*
                 * If Throwable is declared to be thrown by the proxy method,
                 * then no catch blocks are necessary, because the invoke
                 * can, at most, throw Throwable anyway.
                 */
                uniqueList.clear();
                break;
            } else if (!Throwable.class.isAssignableFrom(ex)) {
                /*
                 * Ignore types that cannot be thrown by the invoke method.
                 */
                continue;
            }
            /*
             * Compare this exception against the current list of
             * exceptions that need to be caught:
             */
            for (int j = 0; j < uniqueList.size(); ) {
                Class<?> ex2 = uniqueList.get(j);
                if (ex2.isAssignableFrom(ex)) {
                    /*
                     * if a superclass of this exception is already on
                     * the list to catch, then ignore this one and continue;
                     */
                    continue nextException;
                } else if (ex.isAssignableFrom(ex2)) {
                    /*
                     * if a subclass of this exception is on the list
                     * to catch, then remove it;
                     */
                    uniqueList.remove(j);
                } else {
                    j++;        // else continue comparing.
                }
            }
            // This exception is unique (so far): add it to the list to catch.
            uniqueList.add(ex);
        }
        return uniqueList;
    }

    /**
     * A PrimitiveTypeInfo object contains assorted information about
     * a primitive type in its public fields.  The struct for a particular
     * primitive type can be obtained using the static "get" method.
     */
    private static class PrimitiveTypeInfo {

        /**
         * "base type" used in various descriptors (see JVMS section 4.3.2)
         */
        public String baseTypeString;

        /**
         * name of corresponding wrapper class
         */
        public String wrapperClassName;

        /**
         * method descriptor for wrapper class "valueOf" factory method
         */
        public String wrapperValueOfDesc;

        /**
         * name of wrapper class method for retrieving primitive value
         */
        public String unwrapMethodName;

        /**
         * descriptor of same method
         */
        public String unwrapMethodDesc;

        private static Map<Class<?>, PrimitiveTypeInfo> table = new HashMap<>();

        static {
            add(byte.class, Byte.class);
            add(char.class, Character.class);
            add(double.class, Double.class);
            add(float.class, Float.class);
            add(int.class, Integer.class);
            add(long.class, Long.class);
            add(short.class, Short.class);
            add(boolean.class, Boolean.class);
        }

        private static void add(Class<?> primitiveClass, Class<?> wrapperClass) {
            table.put(primitiveClass,
                    new PrimitiveTypeInfo(primitiveClass, wrapperClass));
        }

        private PrimitiveTypeInfo(Class<?> primitiveClass, Class<?> wrapperClass) {
            assert primitiveClass.isPrimitive();

            baseTypeString =
                    Array.newInstance(primitiveClass, 0)
                            .getClass().getName().substring(1);
            wrapperClassName = dotToSlash(wrapperClass.getName());
            wrapperValueOfDesc =
                    "(" + baseTypeString + ")L" + wrapperClassName + ";";
            unwrapMethodName = primitiveClass.getName() + "Value";
            unwrapMethodDesc = "()" + baseTypeString;
        }

        public static PrimitiveTypeInfo get(Class<?> cl) {
            return table.get(cl);
        }
    }


    /**
     * A ConstantPool object represents the constant pool of a class file
     * being generated.  This representation of a constant pool is designed
     * specifically for use by ProxyGenerator; in particular, it assumes
     * that constant pool entries will not need to be resorted (for example,
     * by their type, as the Java compiler does), so that the final index
     * value can be assigned and used when an entry is first created.
     * <p>
     * Note that new entries cannot be created after the constant pool has
     * been written to a class file.  To prevent such logic errors, a
     * ConstantPool instance can be marked "read only", so that further
     * attempts to add new entries will fail with a runtime exception.
     * <p>
     * See JVMS section 4.4 for more information about the constant pool
     * of a class file.
     */
    private static class ConstantPool {

        /**
         * 八股文 哪些字面量会进入常量池？
         * 1、final修饰 的8种基本类型的值会进入常量池
         * 2、非final修饰(包括static的) 的8种基本类型的值，只有 double、float、long 的值会进入常量池
         * 3、双引号引起来的字符串值
         */
        /**
         * 常量池
         *
         * list of constant pool entries, in constant pool index order.
         * <p>
         * This list is used when writing the constant pool to a stream
         * and for assigning the next index value.  Note that element 0
         * of this list corresponds to constant pool index 1.
         */
        private List<Entry> pool = new ArrayList<>(32);

        /**
         * maps constant pool data of all types to constant pool indexes.
         * <p>
         * This map is used to look up the index of an existing entry for
         * values of all types.
         */
        private Map<Object, Integer> map = new HashMap<>(16);

        /**
         * true if no new constant pool entries may be added
         */
        private boolean readOnly = false;

        /**
         * Get or assign the index for a CONSTANT_Utf8 entry.
         */
        public short getUtf8(String s) {
            // getUtf8()方法做了2件事情
            // 1.将值写入常量池
            // 2.返回该值在常量池中的索引
            if (s == null) {
                throw new NullPointerException();
            }
            return getValue(s);
        }

        /**
         * Get or assign the index for a CONSTANT_Integer entry.
         */
        public short getInteger(int i) {
            return getValue(i);
        }

        /**
         * Get or assign the index for a CONSTANT_Float entry.
         */
        public short getFloat(float f) {
            return getValue(f);
        }

        /**
         * Get or assign the index for a CONSTANT_Class entry.
         */
        public short getClass(String name) {
            // 常量：在常量池存在过了，元素就不会再去创建了

            short utf8Index = getUtf8(name); // 返回该元素在常量池里面的下标
            //
            return getIndirect(new IndirectEntry(CONSTANT_CLASS, utf8Index));
        }

        /**
         * Get or assign the index for a CONSTANT_String entry.
         */
        public short getString(String s) {
            short utf8Index = getUtf8(s);
            return getIndirect(new IndirectEntry(CONSTANT_STRING, utf8Index));
        }

        /**
         * Get or assign the index for a CONSTANT_FieldRef entry.
         */
        public short getFieldRef(String className,
                                 String name, String descriptor) {
            short classIndex = getClass(className);
            short nameAndTypeIndex = getNameAndType(name, descriptor);
            return getIndirect(new IndirectEntry(
                    CONSTANT_FIELD, classIndex, nameAndTypeIndex));
        }

        /**
         * Get or assign the index for a CONSTANT_MethodRef entry.
         */
        public short getMethodRef(String className,
                                  String name, String descriptor) {
            short classIndex = getClass(className);
            short nameAndTypeIndex = getNameAndType(name, descriptor);
            return getIndirect(new IndirectEntry(
                    CONSTANT_METHOD, classIndex, nameAndTypeIndex));
        }

        /**
         * Get or assign the index for a CONSTANT_InterfaceMethodRef entry.
         */
        public short getInterfaceMethodRef(String className, String name,
                                           String descriptor) {
            short classIndex = getClass(className);
            short nameAndTypeIndex = getNameAndType(name, descriptor);
            return getIndirect(new IndirectEntry(
                    CONSTANT_INTERFACEMETHOD, classIndex, nameAndTypeIndex));
        }

        /**
         * Get or assign the index for a CONSTANT_NameAndType entry.
         */
        public short getNameAndType(String name, String descriptor) {
            short nameIndex = getUtf8(name);
            short descriptorIndex = getUtf8(descriptor);
            return getIndirect(new IndirectEntry(
                    CONSTANT_NAMEANDTYPE, nameIndex, descriptorIndex));
        }

        /**
         * Set this ConstantPool instance to be "read only".
         * <p>
         * After this method has been called, further requests to get
         * an index for a non-existent entry will cause an InternalError
         * to be thrown instead of creating of the entry.
         */
        public void setReadOnly() {
            readOnly = true;
        }

        /**
         * Write this constant pool to a stream as part of
         * the class file format.
         * <p>
         * This consists of writing the "constant_pool_count" and
         * "constant_pool[]" items of the "ClassFile" structure, as
         * described in JVMS section 4.1.
         */
        public void write(OutputStream out) throws IOException {
            DataOutputStream dataOut = new DataOutputStream(out);

            // 4、常类词计数器
            // 这里写入cp的大小，注意size()+1，可以和之前Class结构中的constant_pool_count对应
            // constant_pool_count: number of entries plus one
            dataOut.writeShort(pool.size() + 1);

            // 遍历cp中的对象，写入详细信息，对应Class结构中的cp_info
            for (Entry e : pool) {
                e.write(dataOut);
            }
        }

        /**
         * Add a new constant pool entry and return its index.
         */
        private short addEntry(Entry entry) {
            pool.add(entry);
            /*
             * Note that this way of determining the index of the
             * added entry is wrong if this pool supports
             * CONSTANT_Long or CONSTANT_Double entries.
             */
            if (pool.size() >= 65535) {
                throw new IllegalArgumentException("constant pool size limit exceeded");
            }
            // 常量池的下标是从1开始的，也就是这里不-1的原因
            return (short) pool.size();
        }

        /**
         * Get or assign the index for an entry of a type that contains
         * a direct value.  The type of the given object determines the
         * type of the desired entry as follows:
         * <p>
         * java.lang.String        CONSTANT_Utf8
         * java.lang.Integer       CONSTANT_Integer
         * java.lang.Float         CONSTANT_Float
         * java.lang.Long          CONSTANT_Long
         * java.lang.Double        CONSTANT_DOUBLE
         */
        private short getValue(Object key) {
            // 这里用map做了一个缓存，key就是需要写入的字段，value就是索引值，如果命中了map，则直接返回value
            // 如果没有命中缓存，则需要addEntry，即将生成的entry添加入pool，并返回当前pool的大小，也就是该常量在池中的索引
            Integer index = map.get(key);
            if (index != null) {
                return index.shortValue();
            } else {
                if (readOnly) {
                    throw new InternalError("late constant pool addition: " + key);
                }
                // 累加
                short i = addEntry(new ValueEntry(key));
                // key是第几个加入的
                map.put(key, (int) i);
                return i;
            }
        }

        /**
         * Get or assign the index for an entry of a type that contains
         * references to other constant pool entries.
         */
        private short getIndirect(IndirectEntry e) {
            Integer index = map.get(e);
            if (index != null) {
                return index.shortValue();
            } else {
                if (readOnly) {
                    throw new InternalError("late constant pool addition");
                }
                short i = addEntry(e);
                map.put(e, (int) i);
                return i;
            }
        }

        /**
         * Entry is the abstact superclass of all constant pool entry types
         * that can be stored in the "pool" list; its purpose is to define a
         * common method for writing constant pool entries to a class file.
         */
        private abstract static class Entry {
            public abstract void write(DataOutputStream out) throws IOException;
        }

        /**
         * ValueEntry represents a constant pool entry of a type that
         * contains a direct value (see the comments for the "getValue"
         * method for a list of such types).
         * <p>
         * ValueEntry objects are not used as keys for their entries in the
         * Map "map", so no useful hashCode or equals methods are defined.
         */
        private static class ValueEntry extends Entry {
            private Object value;
            public ValueEntry(Object value) {
                this.value = value;
            }
            public void write(DataOutputStream out) throws IOException {
                if (value instanceof String) {
                    out.writeByte(CONSTANT_UTF8);
                    out.writeUTF((String) value);
                } else if (value instanceof Integer) {
                    out.writeByte(CONSTANT_INTEGER);
                    out.writeInt(((Integer) value).intValue());
                } else if (value instanceof Float) {
                    out.writeByte(CONSTANT_FLOAT);
                    out.writeFloat(((Float) value).floatValue());
                } else if (value instanceof Long) {
                    out.writeByte(CONSTANT_LONG);
                    out.writeLong(((Long) value).longValue());
                } else if (value instanceof Double) {
                    out.writeDouble(CONSTANT_DOUBLE);
                    out.writeDouble(((Double) value).doubleValue());
                } else {
                    throw new InternalError("bogus value entry: " + value);
                }
            }
        }

        /**
         * IndirectEntry represents a constant pool entry of a type that
         * references other constant pool entries, i.e., the following types:
         * <p>
         * CONSTANT_Class, CONSTANT_String, CONSTANT_Fieldref,
         * CONSTANT_Methodref, CONSTANT_InterfaceMethodref, and
         * CONSTANT_NameAndType.
         * <p>
         * Each of these entry types contains either one or two indexes of
         * other constant pool entries.
         * <p>
         * IndirectEntry objects are used as the keys for their entries in
         * the Map "map", so the hashCode and equals methods are overridden
         * to allow matching.
         */
        private static class IndirectEntry extends Entry {
            private int tag;
            private short index0;
            private short index1;

            /**
             * Construct an IndirectEntry for a constant pool entry type
             * that contains one index of another entry.
             */
            public IndirectEntry(int tag, short index) {
                this.tag = tag;
                this.index0 = index;
                this.index1 = 0;
            }

            /**
             * Construct an IndirectEntry for a constant pool entry type
             * that contains two indexes for other entries.
             */
            public IndirectEntry(int tag, short index0, short index1) {
                this.tag = tag;
                this.index0 = index0;
                this.index1 = index1;
            }

            public void write(DataOutputStream out) throws IOException {
                out.writeByte(tag);
                out.writeShort(index0);
                /*
                 * If this entry type contains two indexes, write
                 * out the second, too.
                 */
                if (tag == CONSTANT_FIELD ||
                        tag == CONSTANT_METHOD ||
                        tag == CONSTANT_INTERFACEMETHOD ||
                        tag == CONSTANT_NAMEANDTYPE) {
                    out.writeShort(index1);
                }
            }

            public int hashCode() {
                return tag + index0 + index1;
            }

            public boolean equals(Object obj) {
                if (obj instanceof IndirectEntry) {
                    IndirectEntry other = (IndirectEntry) obj;
                    if (tag == other.tag &&
                            index0 == other.index0 && index1 == other.index1) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
}
