package com.ai.utils;

/**
 * 线程本地变量持有者工具类
 * 用于在同一线程内隔离存储 Long 类型数据
 */
public class CurrentHolder {

    private static final ThreadLocal<Long> CURRENT_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE_LOCAL = new ThreadLocal<>();

    public static void setCurrentId(Long employeeId) {
        CURRENT_LOCAL.set(employeeId);
    }

    public static Long getCurrentId() {
        return CURRENT_LOCAL.get();
    }

    public static void setCurrentRole(String role) {
        ROLE_LOCAL.set(role);
    }

    public static String getCurrentRole() {
        return ROLE_LOCAL.get();
    }

    public static void remove() {
        CURRENT_LOCAL.remove();
        ROLE_LOCAL.remove();
    }
}