package org.redis.utils;

/**
 * @author zsy
 * @Description 比较工具类
 * @ClassName ComparatorUtil
 * @date 2024/4/18 17:28
 * @Version 1.0
 */
public class ComparatorUtil {


    /**
         如果 o1 等于 o2，返回值为 0。
         如果 o1 小于 o2，返回值为负数。
         如果 o1 大于 o2，返回值为正数。
     */
    public static int comparator(Object o1,Object o2) {
// 判断对象是否为空
        if (o1 == null && o2 == null) {
            return 0; // 两个对象都为空，视为相等
        } else if (o1 == null) {
            return -1; // o1为空，o2不为空，视为o1小于o2
        } else if (o2 == null) {
            return 1; // o2为空，o1不为空，视为o1大于o2
        }

        // 判断对象类型是否相同
        if (!o1.getClass().equals(o2.getClass())) {
            throw new IllegalArgumentException("Objects are not of the same type");
        }


        // 如果对象为基本类型，直接比较值
        if (o1 instanceof Integer || o1 instanceof Long || o1 instanceof Double || o1 instanceof Float || o1 instanceof Short || o1 instanceof Byte || o1 instanceof Character || o1 instanceof Boolean) {
            return Double.compare(((Number) o1).doubleValue(), ((Number) o2).doubleValue());
        }

        // 如果对象为引用类型，比较其引用
        if (o1 instanceof String || o1 instanceof StringBuilder || o1 instanceof StringBuffer) {
            return ((CharSequence) o1).toString().compareTo(((CharSequence) o2).toString());
        }

        // 其他情况，无法比较，抛出异常
        throw new UnsupportedOperationException("Cannot compare objects of type " + o1.getClass().getName());
    }


}
