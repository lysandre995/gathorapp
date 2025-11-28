package com.alfano.gathorapp.testutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("test")
public class TestDatabaseCleaner {

    private static final Logger logger = LoggerFactory.getLogger(TestDatabaseCleaner.class);

    private final JdbcTemplate jdbcTemplate;

    public TestDatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void truncateAll() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

        // Truncate in reverse dependency order (children first, parents last)
        // This ensures FK constraints won't block truncation even with referential
        // integrity enabled
        List<String> tableOrder = List.of(
                "chat_messages", // depends on chats, users
                "chats", // depends on outings
                "reviews", // depends on outings, users
                "vouchers", // depends on rewards, outings
                "participations", // depends on outings, users
                "outing_participants", // depends on outings, users
                "notifications", // depends on users
                "reports", // depends on users, outings
                "refresh_tokens", // depends on users
                "outings", // depends on users, events
                "rewards", // depends on events, users
                "events", // depends on users
                "users" // no dependencies
        );

        // for (String table : tableOrder) {
        // try {
        // jdbcTemplate.execute("TRUNCATE TABLE " + table);
        // } catch (Exception e) {
        // // table might not exist, ignore
        // }
        // }

        // // Also handle any tables not in the explicit list
        // List<String> allTables = jdbcTemplate.queryForList(
        // "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'
        // AND TABLE_TYPE='TABLE'",
        // String.class);

        // for (String table : allTables) {
        // if (table == null || tableOrder.contains(table))
        // continue;
        // try {
        // jdbcTemplate.execute("TRUNCATE TABLE " + table);
        // } catch (Exception e) {
        // // ignore
        // }
        // }

        // jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        for (String table : tableOrder) {
            try {
                long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
                if (count > 0) {
                    logger.info("Truncating table: {} (had {} rows)", table, count);
                    jdbcTemplate.execute("TRUNCATE TABLE " + table);
                }
            } catch (Exception e) {
                logger.debug("Could not truncate {}: {}", table, e.getMessage());
            }
        }

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        logger.info("=== Database cleanup complete ===");
    }
}
