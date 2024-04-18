package org.redis.lang.basis.zskip;

/**
 * @author zsy
 * 跳跃表节点
 */
public class SkipNode<T> {

    /** 存储排序分值 */
    public double score;

    /** 存储类型的值 */
    public T value;

    /**
     * 后退指针：
     * 只能指向当前节点最底层的前一个节点，
     * 头节点和第一个节点——backward指向NULL，
     * 从后向前遍历跳跃表时使用。
     */
    SkipNode<T> backward;

    // 跳跃表节点的层级结构
    public static class SkipListLevel<T> {

        /** 指向本层下一个节点，尾节点的forward指向NULL */
        public SkipNode<T> forward;

        /** forward指向的节点与本节点之间的元素个数。span值越大，跳过的节点个数越多 */
        public int span;

        public SkipListLevel(SkipNode<T> forward, int span) {
            this.forward = forward;
            this.span = span;
        }
    }

    /** 节点层级数组，柔性数组 */
    public SkipListLevel[] level;

    public SkipNode() {

    }

    public SkipNode(double score,T value) {
        this.score = score;
        this.value = value;
    }
}
