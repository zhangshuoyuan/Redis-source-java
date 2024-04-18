package org.redis.lang.basis.zskip;

import org.redis.utils.ComparatorUtil;

import java.util.Random;

/**
 * @author zsy
 * 跳跃表
 */
public class SkipList<T> {

    /** 当前跳表的索引层数 */
    public int level;

    /** 跳跃表高度 */
    public int length;

    /**  头节点 */
    public SkipNode<T> header;

    /** 指向跳跃表中的最后一个节点 */
    public SkipNode<T> tail;

    /**
     * 初始化
     */
    public SkipList () {
        level = 1;
        length = 0;
        header = new SkipNode<T>(Double.MIN_VALUE,null);
        tail = null;

        random = new Random();
    }

    /** Redis5允许的跳表最大层数为 64 */
    private static final int MAX_LEVEL = 64;
    /**  定义概率 P，用于控制节点层高的生成 */
    private static final double SKIP_LIST_P = 0.25;
    /** 创建一个随机数生成器实例 */
    private Random random;

    /**
     * 生成随机节点层高
     */
    private int randomLevel() {
        // 初始化节点层高 = 1
        int level = 1;
        // 循环生成节点层高，直到达到最大层高或者随机数超过概率阈值
        while (random.nextDouble() < SKIP_LIST_P && level < MAX_LEVEL) {
            // 如果随机数小于概率阈值且当前层高未达到最大层高，则增加节点层高
            level += 1;
        }

        return level;
    }

    /**
     * 创建节点
     */
    public SkipNode<T> zslCreateNode(int level,double score,T value){
        SkipNode<T> zslNode = new SkipNode<>();
        zslNode.score = score;
        zslNode.value = value;
        zslNode.level = new SkipNode.SkipListLevel[level];

        return zslNode;
    }

    /**
     * 构建跳表
     * 1. 创建跳跃表的结构体对象 zsl
     * 2. 将zsl的头节点指向新创建的节点
     * 3. 跳跃表层高初始化为1，长度初始化为0，尾节点指向NULL
     */
    public SkipList<T> zslCreate() {
        int j;
        SkipList<T> zsl = new SkipList<>(); // 创建跳跃表对象

        // 初始化跳跃表的层级，长度以及头节点
        zsl.level = 1;
        zsl.length = 0;
        zsl.header = zslCreateNode(MAX_LEVEL,0,null);

        // 初始化头节点的层级结构
        for (j = 0; j<MAX_LEVEL; j++) {
            zsl.header.level[j].forward = null;
            zsl.header.level[j].span = 0;
        }

        // 设置头节点的后退指针以及尾节点
        zsl.header.backward = null;
        zsl.tail = null;

        return zsl;
    }


    /**
     * 插入节点：
     * 1. 查找要插入的位置
     * 2. 调整跳跃表高度
     * 3. 插入节点
     * 4. 调整backward
     *
     */
    public <T> SkipNode<T> insert(SkipList<T> zsl,double score,T value) {
        //1. 查找需要插入的位置

        // 存储每个层级被跳过的节点数量
        long[] rank = new long[MAX_LEVEL];
        // 存储每个层级需要更新的节点
        SkipNode[] update = new SkipNode[MAX_LEVEL];
        // 当前的节点层级
        int level;

        //遍历跳跃表，查找插入位置
        SkipNode x = zsl.header;
        for (int i = zsl.level - 1; i > 0; i--) {
            rank[i] = (i == zsl.level - 1) ? 0 : rank[i + 1];
            while (x.level[i].forward != null &&
                    x.level[i].forward.score < score ||
                        (x.level[i].forward.score == score &&
                                // 跳表节点 x 在第 i 层的下一个节点的元素值比参数 value 小。
                                ComparatorUtil.comparator(x.level[i].forward.value, value)  < 0 ))

            {
                rank[i] += x.level[i].span;
                x = x.level[i].forward;
            }
            update[i] = x;
        }

        // 随机生成新节点的层级
        level = randomLevel();
        if (level > zsl.level) {
            for (int i = zsl.level; i < level; i++) {
                rank[i] = 0;
                update[i] = zsl.header;
                update[i].level[i].span = zsl.length;
            }
            zsl.level = level;
        }

        // 创建新节点
        x = zsl.zslCreateNode(level,score, value);
        for (int i = 0; i < level; i++) {
            x.level[i].forward = update[i].level[i].forward;
            update[i].level[i].forward = x;
            x.level[i].span = (int) (update[i].level[i].span - (rank[0] - rank[i]));
            update[i].level[i].span = (int) ((rank[0] - rank[i]) + 1);
        }

        // 更新未改变层级的节点的 span
        for (int i = level; i < zsl.level; i++) {
            update[i].level[i].span++;
        }

        // 设置新节点的后退指针
        x.backward = (update[0] == zsl.header) ? null : update[0];
        if (x.level[0].forward != null) {
            x.level[0].forward.backward = x;
        } else {
            zsl.tail = x;
        }

        // 增加跳跃表的长度
        zsl.length++;

        return x;
    }




}
