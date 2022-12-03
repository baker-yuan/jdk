package cn.baker.threadlocal;

import java.text.SimpleDateFormat;

/**
 * 生产出线程安全的日期格式化工具
 */
public class ThreadSafeFormatter {
    public static ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };
}