package com.suning.snfe.util;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * Spring工具类 Created by 16080536 on 2016/12/3.
 */
public class SpringUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringUtil.class);

    // 保持Spring上下文
    private static ApplicationContext ctx = null;
    // 保持Servlet上下文
    private static ServletContext sct = null;

    private SpringUtil() {
    }

    /**
     * 初始化上下文
     *
     * @param newSct Servlet上下文
     * @param newCtx 系统启动时传入的上下文
     */
    public static void initial(ServletContext newSct, ApplicationContext newCtx) {
        ctx = newCtx;
        sct = newSct;
        LOGGER.info("SpringUtil initial successful!");
    }

    /**
     * 根据名称和类型获取Bean实例
     *
     * @param clazz Bean类
     * @param <T> 指定类型
     * @return Bean实例
     */
    public static <T> T getServiceBean(Class<T> clazz) {
        if (ctx == null) {
            LOGGER.warn("SpringUtil has not been initialed!");
            return null;
        }
        return ctx.getBean(clazz);
    }

    /**
     * 获取应用上下文
     */
    public static ApplicationContext getCtx() {
        return ctx;
    }

    /**
     * 获取Servlet上下文
     */
    public static ServletContext getSct() {
        return sct;
    }
}
