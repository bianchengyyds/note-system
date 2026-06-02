package com.ai.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UserStatRefreshTask {

    private final JdbcTemplate jdbcTemplate;

    public UserStatRefreshTask(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 每30分钟全量刷新 user_stat 表
     * cron: 秒 分 时 日 月 星期
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void refreshUserStatistics() {
        String sql = """
                INSERT INTO user_stat (
                    user_id,
                    created_notes,
                    created_kbs,
                    total_views,
                    total_likes,
                    received_comments,
                    total_words,
                    active_days,
                    last_active_time,
                    updated_at
                )
                SELECT
                    u.id,
                    COALESCE(doc_stats.note_count, 0),
                    COALESCE(kb_stats.kb_count, 0),
                    COALESCE(doc_stats.total_views, 0),
                    COALESCE(doc_stats.total_likes, 0),
                    COALESCE(comm_stats.comment_count, 0),
                    COALESCE(doc_stats.total_words, 0),
                    COALESCE(log_stats.active_days, 0),
                    COALESCE(log_stats.last_time, u.last_login_time),
                    NOW()
                FROM user u
                LEFT JOIN (
                    SELECT
                        creator_id,
                        COUNT(*) AS note_count,
                        SUM(view_count) AS total_views,
                        SUM(like_count) AS total_likes,
                        SUM(CASE WHEN type = 'doc' THEN CHAR_LENGTH(content) ELSE 0 END) AS total_words
                    FROM document
                    WHERE deleted_at IS NULL
                    GROUP BY creator_id
                ) doc_stats ON u.id = doc_stats.creator_id
                LEFT JOIN (
                    SELECT
                        creator_id,
                        COUNT(*) AS kb_count
                    FROM knowledge_base
                    WHERE deleted = 0
                    GROUP BY creator_id
                ) kb_stats ON u.id = kb_stats.creator_id
                LEFT JOIN (
                    SELECT
                        d.creator_id,
                        COUNT(*) AS comment_count
                    FROM comment c
                    JOIN document d ON c.note_id = d.id
                    WHERE d.deleted_at IS NULL
                    GROUP BY d.creator_id
                ) comm_stats ON u.id = comm_stats.creator_id
                LEFT JOIN (
                    SELECT
                        user_id,
                        COUNT(DISTINCT DATE(created_at)) AS active_days,
                        MAX(created_at) AS last_time
                    FROM operation_log
                    GROUP BY user_id
                ) log_stats ON u.id = log_stats.user_id
                ON DUPLICATE KEY UPDATE
                    created_notes = VALUES(created_notes),
                    created_kbs = VALUES(created_kbs),
                    total_views = VALUES(total_views),
                    total_likes = VALUES(total_likes),
                    received_comments = VALUES(received_comments),
                    total_words = VALUES(total_words),
                    active_days = VALUES(active_days),
                    last_active_time = VALUES(last_active_time),
                    updated_at = VALUES(updated_at);
                """;

        try {
            jdbcTemplate.execute(sql);
            log.info("user_stat 表刷新完成");
        } catch (Exception e) {
            log.error("刷新 user_stat 失败", e);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(UserStatRefreshTask.class);
}