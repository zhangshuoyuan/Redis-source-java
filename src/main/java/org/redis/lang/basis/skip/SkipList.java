package org.redis.lang.basis.skip;

import java.util.Random;

/**
 * @author zsy
 * 跳表
 */
public class SkipList
{

    /** Redis5允许的跳表最大层数为 64 */
    private static final int SKIP_MAX_LEVEL = 64;
    /**  定义概率 P，用于控制节点层高的生成 */
    private static final double SKIP_LIST_P = 0.25;

    /** 当前跳表的索引层数 */
    private int level;

    /** 跳跃表高度 */
    private int length;

    /**
     * 头节点
     *  backward 指向 NULL
     *  forward 指向 下一节点
     */
    private SkipNode header;

    /**
     * 尾节点：倒序便利跳表时使用
     * forward 指向 NULL
     */
    private SkipNode tail;

    /**
     * 初始化
     */
    public SkipList() {
        // 初始层级为1层，初始长度为0
        level = 1;
        length = 0;
        // 头节点初始化: SCORE权重最小
        header = new SkipNode(Double.MIN_VALUE,null);
        // 尾节点为NULL
        tail = null;
    }

    /**
     * 生成随机节点层高
     */
    private static int randomLevel() {
        Random random = new Random();

        // 初始化节点层高 = 1
        int level = 1;
        // 循环生成节点层高，直到达到最大层高或者随机数超过概率阈值
        while (random.nextDouble() < SKIP_LIST_P && level < SKIP_MAX_LEVEL) {
            // 如果随机数小于概率阈值且当前层高未达到最大层高，则增加节点层高
            level += 1;
        }

        return level;
    }


    /**
     * 创建跳表的Node节点
     */
    public static SkipNode createZslNode(int level,double score,Integer value) {
        SkipNode node = new SkipNode();
        node.level = new SkipNode.SkipNodeLevel[level];
        node.score = score;
        node.value = value;

        return node;
    }

    /**
     * 创建跳表
     * 1. 创建跳跃表的结构体对象 zsl
     * 2. 将zsl的头节点指向新创建的节点
     * 3. 跳跃表层高初始化为1，长度初始化为0，尾节点指向NULL
     */
    public static SkipList createZslList() {
        int j;
        SkipList zsl = new SkipList(); // 创建跳跃表对象

        // 初始化跳跃表的层级，长度以及头节点
        zsl.level = 1;
        zsl.length = 0;
        zsl.header = createZslNode(SKIP_MAX_LEVEL,0,null);

        // 初始化头节点的层级结构
        for (j = 0; j<SKIP_MAX_LEVEL; j++) {
            zsl.header.level[j] = new SkipNode.SkipNodeLevel();
            zsl.header.level[j].forward = null;
            zsl.header.level[j].span = 0;
        }

        // 设置头节点的后退指针以及尾节点
        zsl.header.backward = null;
        zsl.tail = null;

        return zsl;
    }

    /**
     * 跳表节点
     */
    public static class SkipNode
    {

        /** 存储排序分值:权重 */
        private double score;

        /** 存储类型的值: 默认存储Int类型*/
        private Integer value;

        private SkipNode () {

        }

        private SkipNode (double score,Integer value) {
           this.score = score;
           this.value = value;
        }

        /**
         * 后退指针：
         * 只能指向当前节点最底层的前一个节点，
         * 头节点和第一个节点——backward指向NULL，
         * 从后向前遍历跳跃表时使用。
         */
        private SkipNode backward;

        /** 节点层级数组，柔性数组 */
        private SkipNodeLevel[] level;

        /**
         * 跳跃表节点的层级结构
         */
        private static class SkipNodeLevel
        {

            /** 指向本层下一个节点，尾节点的forward指向NULL */
            private SkipNode forward;

            /** forward指向的节点与本节点之间的元素个数。span值越大，跳过的节点个数越多 */
            private int span;
        }
    }


    /**
     * 查询 数据是否存在 (外部业务，如果是其他类型有序存储，那么必定会存储在外部数据库，根据对应数据VALUE获取 Score，然后通过Score)
     *
     * 查询流程：设置一个临时节点 team = head。当 team ！= null
     *      1. 从 team 节点出发，如果当前节点的 score 与查询的 score 相等，那么返回当前节点 (如果是修改操作，那么一直向下进行修改值)
     *      2. 如果 score 不相等，且 forward 未 null，那么只能进入下一层（结果只能出现在下右方），此时 team = team.down
     *      3. 如果 score 不相等，且 forward 不为 null, 且右侧节点的 score 小于 待查询的 score，说明同层还可以继续 forward ，
     *         此时 team = team.forward
     *      4. 如果 score 不相等，且 forward 不为 null, 且右侧节点的 score 大于 待查询的 score，那么说明如果有结果，
     *         那么这个结果就在(下层中的此索引与下个节点索引之间)，此时 team = team.down
     *
     * @param score 如果value未其他类型，那么有序列表需要用户给其他类型定义一个权重，跳表根据权重进行排序
     * @return 是否存在 true 存在  false 不存在
     */
    public boolean doesItExist(double score) {
        // 设置一个临时节点 team = head。team == null
        SkipNode team = header;
        if (team == null) {
            return false;
        }

        // 编辑层级,从最高层开始
        for (int i = this.level-1; i>0; i--) {
            // 查询本层，大于等于该值的节点的，或最后一个节点
            team = findClosest(team,i,score);

            // 如果相等直接返回
            if (team.score == score) {
                return true;
            }

            // 如果当前节点forward为空, team.score < targetScore,则直接进入下一层
            if (team.level[i].forward == null && team.level[i].forward == null) {
                continue; // 进入下一层
            }

            // 如果当前节点的forward不为空，则另team = team.backward,然后再进入下一层
            // 如果当前节点forward未空，且team.score > targetScore 则 team = team.backward, 然后再进入下一层
            team = team.backward;
        }

        return false;
    }

    /**
     * 添加，插入
     * 1. 查找要插入的位置 (节点位置、层级位置)
     * 2. 调整跳跃表高度 Length
     * 3. 插入节点
     * 4. 调整 backward 后节点的 backward
     *
     * @param score 分数、排序权重
     * @param value 值
     */
    public SkipNode add(double score, Integer value) {
        // 1. 查找要插入的位置 (节点位置以及层级位置)
        SkipNode[] update = new SkipNode[SKIP_MAX_LEVEL]; // 用于保存每层需要更新的节点
        long[] rank = new long[SKIP_MAX_LEVEL];
        int level;

        assert !Double.isNaN(score); // 断言score不为空

        SkipNode temp = this.header;
        for (int i = this.level - 1; i >= 0; i--) {
            // 记录达到插入位置时，跨越的节点数量
            rank[i] = (i == this.level - 1) ? 0 : rank[i + 1];

            // 在当前层级查找插入位置
            while (temp.level[i].forward != null &&
                    (temp.level[i].forward.score < score ||
                            (temp.level[i].forward.score == score &&
                                    temp.level[i].forward.score < score))) // 分数权重比较
            {
                rank[i] += temp.level[i].span;
                temp = temp.level[i].forward;
            }
            update[i] = temp;
        }


        // 确定新的节点层级
        level = randomLevel();
        if (level > this.level) {
            for (int i = this.level; i < level; i++) {
                rank[i] = 0;
                update[i] = this.header;
                update[i].level[i].span = this.length;
            }
            this.level = level;
        }

        // 创建新节点
        temp = createZslNode(level, score, value);
        for (int i = 0; i < level; i++) {
            if (temp.level[i] == null) {
                continue;
            }

            temp.level[i].forward = update[i].level[i].forward;
            update[i].level[i].forward = temp;

            // 更新跨越的节点数量
            temp.level[i].span = (int) (update[i].level[i].span - (rank[0] - rank[i]));
            update[i].level[i].span = (int) ((rank[0] - rank[i]) + 1);
        }

        // 更新未被触及的层级的跨越数量
        for (int i = level; i < this.level; i++) {
            update[i].level[i].span++;
        }

        // 更新新节点的 backward 指针
        temp.backward = (update[0] == this.header) ? null : update[0];
        if (temp.level[0] != null && temp.level[0].forward != null) {
            temp.level[0].forward.backward = temp;
        } else {
            this.tail = temp;
        }

        // 增加跳表的长度
        this.length++;

        return temp;
    }


    /**
     * 查询跳表本层中，大于等于该节点的最近节点，如果没有返回当前层级最后一个节点
     * (从当前节点开始，一次进入下一 节点)
     * @param node 需要处理的跳表
     * @param levelIndex 当前层数
     * @param score 分值，权重
     * @return 查询结果
     */
    private SkipNode findClosest(SkipNode node, int levelIndex, double score) {

        // 当node不为当前层级的最后一个节点，且查询score大于跳表中score
        while (node.level[levelIndex] != null && score > node.score) {
            // 进入下一 节点
            node = node.level[levelIndex].forward;
        }
        // 如果当前节点未本层最后一个节点，或当前节点值大于score，直接返回
        return node;
    }
}
