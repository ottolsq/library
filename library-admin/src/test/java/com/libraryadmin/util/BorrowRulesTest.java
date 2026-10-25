package com.libraryadmin.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 借阅规则纯函数测试：逾期识别、逾期天数、默认应还时间。
 */
class BorrowRulesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 25, 10, 0, 0);

    @Test
    @DisplayName("借出状态且已过应还时间，识别为逾期")
    void effectiveStatus_borrowedAndPastDue_returnsOverdue() {
        int status = BorrowRules.effectiveStatus(
                BorrowRules.STATUS_BORROWED,
                NOW.minusDays(1),
                NOW);

        assertThat(status).isEqualTo(BorrowRules.STATUS_OVERDUE);
    }

    @Test
    @DisplayName("借出状态且未到应还时间，保持借出")
    void effectiveStatus_borrowedAndNotDue_returnsBorrowed() {
        int status = BorrowRules.effectiveStatus(
                BorrowRules.STATUS_BORROWED,
                NOW.plusDays(1),
                NOW);

        assertThat(status).isEqualTo(BorrowRules.STATUS_BORROWED);
    }

    @Test
    @DisplayName("已归还记录即使超过应还时间也不改为逾期")
    void effectiveStatus_returnedAndPastDue_returnsReturned() {
        int status = BorrowRules.effectiveStatus(
                BorrowRules.STATUS_RETURNED,
                NOW.minusDays(10),
                NOW);

        assertThat(status).isEqualTo(BorrowRules.STATUS_RETURNED);
    }

    @Test
    @DisplayName("默认应还时间为当前时间后 30 天")
    void defaultShouldReturnTime_addsThirtyDays() {
        LocalDateTime due = BorrowRules.defaultShouldReturnTime(NOW);

        assertThat(due).isEqualTo(NOW.plusDays(BorrowRules.DEFAULT_BORROW_DAYS));
    }

    @Test
    @DisplayName("逾期天数按整天计算")
    void overdueDays_pastDueByThreeDays_returnsThree() {
        long days = BorrowRules.overdueDays(
                NOW.minusDays(3),
                NOW);

        assertThat(days).isEqualTo(3);
    }

    @Test
    @DisplayName("未逾期时逾期天数为 0")
    void overdueDays_notDue_returnsZero() {
        long days = BorrowRules.overdueDays(
                NOW.plusDays(2),
                NOW);

        assertThat(days).isZero();
    }

    @Test
    @DisplayName("逾期不足一天时按 1 天展示，避免出现逾期标签配 0 天")
    void overdueDays_lessThanOneDay_returnsOne() {
        long days = BorrowRules.overdueDays(
                NOW.minusHours(5),
                NOW);

        assertThat(days).isEqualTo(1);
    }

    @Test
    @DisplayName("借出与逾期记录都占用读者可借额度")
    void countsTowardLimit_borrowedAndOverdue_returnsTrue() {
        assertThat(BorrowRules.countsTowardLimit(BorrowRules.STATUS_BORROWED)).isTrue();
        assertThat(BorrowRules.countsTowardLimit(BorrowRules.STATUS_OVERDUE)).isTrue();
    }

    @Test
    @DisplayName("已归还记录不占用可借额度")
    void countsTowardLimit_returned_returnsFalse() {
        assertThat(BorrowRules.countsTowardLimit(BorrowRules.STATUS_RETURNED)).isFalse();
    }
}
