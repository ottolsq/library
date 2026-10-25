package com.libraryadmin.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 借阅规则纯函数集合。
 *
 * <p>把「逾期判断」「逾期天数」「默认应还时间」从 Service 中抽出，
 * 便于单元测试，也保证列表展示与定时标记使用同一份规则。
 */
public final class BorrowRules {

    /** 借出 */
    public static final int STATUS_BORROWED = 0;
    /** 已归还 */
    public static final int STATUS_RETURNED = 1;
    /** 逾期 */
    public static final int STATUS_OVERDUE = 2;

    /** 默认借阅期限：30 天 */
    public static final int DEFAULT_BORROW_DAYS = 30;

    private BorrowRules() {
    }

    /**
     * 默认应还时间：当前时间 + 30 天
     */
    public static LocalDateTime defaultShouldReturnTime(LocalDateTime now) {
        return now.plusDays(DEFAULT_BORROW_DAYS);
    }

    /**
     * 计算展示/判定用的有效状态。
     *
     * <p>已归还记录永远保持已归还；借出状态若已过应还时间，即使定时任务尚未回写，
     * 也按逾期展示，避免列表与数据库短时间不一致。
     */
    public static int effectiveStatus(Integer status,
                                      LocalDateTime shouldReturnTime,
                                      LocalDateTime now) {
        if (status == null) {
            return STATUS_BORROWED;
        }
        if (status != STATUS_BORROWED) {
            return status;
        }
        if (shouldReturnTime != null && shouldReturnTime.isBefore(now)) {
            return STATUS_OVERDUE;
        }
        return STATUS_BORROWED;
    }

    /**
     * 逾期天数：按整天计算，未逾期返回 0。
     *
     * @param shouldReturnTime 应还时间
     * @param end              计算截止时间（未归还传当前时间，已归还传实际归还时间）
     */
    public static long overdueDays(LocalDateTime shouldReturnTime, LocalDateTime end) {
        if (shouldReturnTime == null || end == null || !end.isAfter(shouldReturnTime)) {
            return 0;
        }
        // 逾期不足一天也按 1 天展示，避免「逾期」标签与「0 天」同时出现
        return Math.max(ChronoUnit.DAYS.between(shouldReturnTime, end), 1L);
    }

    /**
     * 是否占用读者可借额度：借出与逾期都占用，已归还释放。
     */
    public static boolean countsTowardLimit(Integer status) {
        return status != null && status != STATUS_RETURNED;
    }
}
