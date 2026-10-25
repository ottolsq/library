package com.libraryadmin.task;

import com.libraryadmin.service.BorrowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 逾期借阅标记任务
 *
 * <p>每 10 分钟扫描一次借出状态且已过应还时间的记录，落库为逾期。
 * 列表查询前也会调用同一方法，保证页面打开时状态立即正确。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueBorrowTask {

    private final BorrowService borrowService;

    /**
     * 启动完成后先执行一次，避免重启后第一批逾期记录要等下一个周期。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void markOverdueOnStartup() {
        markOverdue();
    }

    @Scheduled(cron = "0 */10 * * * ?")
    public void markOverdue() {
        int updated = borrowService.markOverdue();
        if (updated > 0) {
            log.info("逾期标记完成，更新记录数={}", updated);
        }
    }
}
