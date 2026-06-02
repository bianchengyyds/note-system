package com.ai.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {
    /** 操作类型，如 CREATE_NOTE、EDIT_NOTE、DELETE_NOTE 等 */
    String operation();

    /** 目标类型，如 NOTE、KB、COMMENT 等 */
    String targetType() default "";

    /** 目标ID的 SpEL 表达式，如 "#noteId" 或 "#request.id" */
    String targetId() default "";
}