package cn.baker.strean;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// http://m.classinstance.cn/detail/175.html
// https://www.cnblogs.com/foreverstudy/p/16009259.html
// https://www.codenong.com/cs106569674/
// https://blog.csdn.net/abments/article/details/126680141
public class Main {


    public static void main(String[] args) {
        List<XxxVO> list = new ArrayList<>();
        list.add(new XxxVO(111, "article"));
        list.add(new XxxVO(222, "comment"));
        list.add(new XxxVO(111, "article"));

        List<XxxVO> unique1 = list.stream().collect(
                Collectors.collectingAndThen(
                        Collectors.toCollection(
                                () -> new TreeSet<>(Comparator.comparing(obj->obj.getBizId() + ";" + obj.getBizType()))
                        ),
                        ArrayList::new
                )
        );


        List<Integer> list2 = Arrays.asList(1, 2, 3, 4);
        Double result = list2.stream().collect(
                Collectors.collectingAndThen(Collectors.averagingLong(v -> {
                    System.out.println("v--" + v + "--> " + v * 2);
                    return v * 2;
                }),
                s -> {
                    System.out.println("s--" + s + "--> " + s * s);
                    return s * s;
                })
        );
        System.out.println(result);

    }



    public static void main2(String[] args) {
        List<XxxVO> list = new ArrayList<>();
        list.add(new XxxVO(111, "article"));
        list.add(new XxxVO(222, "comment"));
        list.add(new XxxVO(111, "article"));
        Predicate<XxxVO> predicate = new Predicate<XxxVO>() {
            /**
             * 成员变量，只会初始化一次
             */
            final Set<String> set = new CopyOnWriteArraySet<>(); // parallelStream
            /**
             * 是否保留当前元素，每个元素都会调用test方法判断
             * @param obj 判断的元素
             * @return true-保留 false-不保留
             */
            @Override
            public boolean test(XxxVO obj) {
                if (set.contains(obj.getUk())) {
                    // 以存在的元素不要了
                    return false;
                }
                // 不存在的元素加入集合，并且保留这个元素
                set.add(obj.getUk());
                return true;
            }
        };
        List<Object> result = list.parallelStream()
                .filter(predicate)
                .collect(Collectors.toList());
        System.out.println(result.size());

    }
}
