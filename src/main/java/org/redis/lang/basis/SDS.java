package org.redis.lang.basis;

/**
 * @author zsy
 * @Description 动态字符串
 *
 * 1. 此设计有单独的统计变量len和free(头部),可以很方便的得到字符串的长度;
 * 2. 内容存放在柔性数组buf中，SDS对上层暴露的指针不是指向结构体SDS的指针，
 *    而是直接指向柔性数组buf的指针。上层可像读取C字符串一样读取SDS的内容，
 *    兼容C语言处理字符串的各种函数。
 * 3. 由于有长度统计变量len的存在，读写字符串时不依赖“\0”终止符，保证了二进制安全。
 *
 * @ClassName Sds
 * @Version 1.0
 */
public class SDS {

    /** buf中已占用的字节 */
    int len; // Redis觉得int类型占用四个字符还是太长了，于是采用更加节省空间修饰

    /** buf所剩余的字节 */
    int free;

    /**
     * 柔性数组成员（flexible array member），也叫伸缩性数组成员，
     * 只能被放在结构体的末尾。包含柔性数组成员的结构体，
     * 通过malloc函数为柔性数组动态分配内存。
     * */
    byte[] buf;  // 数据空间

    /** 类型：此处只列出TYPE8和TYPE5版本 */
    private static final char SDS_TYPE_8 = 'c';
    private static final char SDS_TYPE_5 = 'e';


    /**
     * 创建SDS的大致流程如下：
     *      1. 创建空字符串时，SDS_TYPE_5被强制转换为SDS_TYPE_8。
     *      2. 长度计算时有“+1”操作，是为了算上结束符“\0”。
     *      3. 返回值是指向sds结构buf字段的指针。
     */
    public static SDS sdsNewLen(byte[] init,int initLen)  {
        byte[] sh;
        SDS sds;

        // 根据字符串长度,选择合适的类型
        char type = sdsRedType(initLen);
        // 如果字符串为空，且类型为5时，将会强制转换为TYPE8类型;
        // 原因可能是创建空字符串后，其内容可能会频繁更新而引发扩容，故创建时直接创建为sdshdr8
        if (type == SDS_TYPE_5 && initLen == 0) {
            type = SDS_TYPE_8;
        }

        // 计算不同类型TYPE的头部所需长度
        int hdrLen = sdsHdrSize(type);
        byte[] fp; // 指向flags的指针

        // 长度计算时有+1操作，是为了算上结束符 '/0'
        sh = sMalloc(hdrLen+initLen+1);

        // ...

        sds = new SDS();
        sds.buf = new byte[initLen];
        // s是指向buf的指针
        System.arraycopy(sh,hdrLen,sds.buf,0,initLen);
        fp = new byte[]{sh[0]}; // fp是柔性数组

        // ...

        // 添加末尾结束符
        sds.buf[initLen] = '\0';
        return sds;
    }

    // 根据字符串长度选择合适的类型
    private static char sdsRedType(int lenSize){
        // 此处为伪代码
        return lenSize > 5 ? SDS_TYPE_8:SDS_TYPE_5;
    }
    // 计算不同类型TYPE的头部所需长度
    private static int sdsHdrSize(char type) {
        // 伪代码
        switch (type) {
            case SDS_TYPE_5:return 2;
            case SDS_TYPE_8:return 1;
        }
        // 默认头部长度为4
        return 4;
    }
    // 生成sh指针
    private static byte[] sMalloc(int byteSize) {
        // 伪代码
        return new byte[byteSize];
    }


    /**
     * SDS对外提供了释放性能的方法
     *     该方法通过对sds的偏移，定位到SDS结构体的首部，然后调用s_free()释放内存
     *
     * 此过程有内存申请的带来的开销。
     */
    public static void sdsFree(SDS sds){
        if (sds == null) return;
        // 释放销毁
    }

    /**
     * 为节省内存性能开销，此方法不直接释放内存；
     *      而是由重置统计值达到清空内存的目的;
     *
     * 此方法将SDS的len归零，此处存在的buf并没有真正的被清除，
     * 新的操作可以覆盖写，而不用重新申请内存;
     */
    public static void sdsClear(SDS sds){
        sdsSetLen(sds,0);
        sds.buf[0] = '\0';
    }

    // 设置SDS字符串长度
    private static void sdsSetLen(SDS s,int len) {
        s.len = len;
    }

    // 给free属性赋值
    private static void sdsSetFree(SDS s,int free){
        s.free = free;
    }


    /**
     * 字符串拼接
     *
     *  sdsCatSda是暴露给上层的方法，其最终调用sdsCatLen方法；
     *
     *  其中可能会涉及SDS的扩容，在sdsCatLen中sdsMakeRoomFor对带拼接的字符串s容量进行检查
     *  若无需扩容则直接返回s，如果需要扩容，则返回扩容好的新字符串s;
     */
    public static SDS sdsCatSds(SDS s,SDS t) {
        return sdsCatLen(s,t,sdsLen(t));
    }

    private static SDS sdsCatLen(SDS s,SDS t,int len){
        // s以存储的长度
        int curLen = sdsLen(s);
        // SDS扩容机制
        s = sdsMarkRoomFor(s,len);
        if (s == null) return null;

        // 直接拼接 保证二进制安全  result其实就是S的数组
        System.arraycopy(s, 0, s.buf, 0, curLen); // 将s中的内容复制到结果数组中
        System.arraycopy(t, 0, s.buf, curLen, len); // 将t中的内容复制到结果数组中的末尾位置

        // 设置结束符
        s.buf[curLen+len] = '\0';
        s.len = curLen+len;
        return s;
    }

    /** 1MB能够容纳的存储长度 */
    public static  final int SDS_MAX_PREALLOC = 1024 * 1024;

    /** 类型 */
    public static char type = ' ';

    /**
     * SDS的扩容机制
     */
    private static SDS sdsMarkRoomFor(SDS s,int addLen){

        // 获取SDS的可用空间
        int avail = sdsAvail(s);
        // 原始长度
        int len = sdsLen(s);

        // 策略1：如果剩余空间avail 大于等于 所需要空间长度，无需扩容，返回SDS;
        if (avail >= addLen) return s;

        // 总长度
        int newLen = len + addLen;

        // 策略2：如果avail<len; 且 len + addLen 占用存储小于 1MB，则扩容两倍
        // 策略3：如果avail<len; 且 len + addLen 占用存储大于 1MB，则扩容+1MB;
        if (newLen < SDS_MAX_PREALLOC) {
            newLen = newLen * 2;
        }else {
            newLen += newLen;
        }

        // 最后，根据长度重新选择存储类型，并分配空间
        char oldType = type;

        type = sdsRedType(newLen);
        if (type == SDS_TYPE_5) type = SDS_TYPE_8;
        //计算头部长度
        int hdrLen = sdsHdrSize(type);

        SDS sh = null;
        SDS newSh = null;

        if (oldType == type){
            // 无需更改类型，直接扩大柔性数组
            sh = realloc(s,sh,hdrLen,newLen + 1); // 无需重新开辟内存
            if (sh == null) return null;
            // 拼接扩容buf[]
            System.arraycopy(sh, hdrLen, s, 0, s.len);
        }else {
            // 类型以及头部长度发生了更改，此时不再进行realloc,而是直接内存开辟
            newSh  = malloc(hdrLen+newLen+1); // 根据长度重新开辟内存
            if (newSh == null) return null;
            System.arraycopy(newSh, hdrLen, s, 0, s.len);
            // 释放旧指针
            sdsFree(s);
            // 给len赋值
            sdsSetLen(s,len);
        }
        //给free属性赋值
        sdsSetFree(s,newLen);
        return s;
    }

    // 无需重新开辟内存，扩容buf[] (伪代码，因为java中不具备控制内存的方法)
    private static SDS realloc(SDS oldSh,SDS sh,int hdrLen,int len){
        byte[] newBuf = new byte[hdrLen + len];
        System.arraycopy(oldSh.buf, 0, newBuf, 0, oldSh.buf.length);
        sh = new SDS();
        sh.buf = newBuf;
        return sh;
    }

    // 需要根据长度，开辟新的内存
    private static SDS malloc(int len){
        byte[] bytes = new byte[len];
        SDS sh = new SDS();
        sh.buf = bytes;
        return sh;
    }



    // 返回SDS的可用空间
    private static int sdsAvail(SDS s) {
        return s.free;
    }


    // 返回SDS字符长度
    private static int sdsLen(SDS s){
        return s.len;
    }
}
