package org.redis.lang.basis.skip;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author zsy
 * @Description 跳表功能测试Demo
 * @ClassName SkipListDemo
 * @date 2024/4/19 14:13
 * @Version 1.0
 */
public class SkipListDemo {


    public static void main(String[] args) {

        // 跳表
        Map<Integer,String> skipMap = new ConcurrentSkipListMap<>();

        skipMap.put(13,"add");
        skipMap.put(11,"delete");
        skipMap.put(15,"find");

        System.out.println(skipMap);

    }

}
