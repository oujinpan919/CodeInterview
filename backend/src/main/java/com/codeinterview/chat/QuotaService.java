package com.codeinterview.chat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.codeinterview.ai.AiProperties;
import com.codeinterview.chat.vo.AiQuotaResp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 账号/全站配额原子扣减与回滚（05 §5.1）。
 * 日期由调用方传 Asia/Shanghai 的 LocalDate，保证「今日」口径唯一。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    public static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final QuotaMapper quotaMapper;
    private final AiProperties props;

    /** 05 §5.1：统计日统一取 Asia/Shanghai，不用 new Date()。 */
    public static LocalDate today() {
        return LocalDate.now(SHANGHAI);
    }

    /** 04 A3.7 #24：额度条数据；resetAt 为次日 00:00:00。 */
    public AiQuotaResp quotaInfo(Long userId) {
        LocalDate date = today();
        int used = countUsed(userId, date);
        int dailyLimit = userDailyLimit();
        AiQuotaResp resp = new AiQuotaResp();
        resp.setDailyLimit(dailyLimit);
        resp.setUsed(used);
        resp.setRemaining(Math.max(0, dailyLimit - used));
        resp.setSiteRemaining(Math.max(0, siteDailyLimit() - countSiteUsed(date)));
        resp.setResetAt(date.plusDays(1).atStartOfDay().format(DATE_TIME_FORMATTER));
        return resp;
    }

    /** 账号额度：确保当日行存在 + 条件自增；返回 true 表示扣减成功。 */
    public boolean tryConsumeUser(Long userId, LocalDate date, int limit) {
        quotaMapper.ensureUserQuota(userId, date);
        return quotaMapper.consumeUser(userId, date, limit) == 1;
    }

    /** 全站预算：同上。 */
    public boolean tryConsumeSite(LocalDate date, int limit) {
        quotaMapper.ensureSiteUsage(date);
        return quotaMapper.consumeSite(date, limit) == 1;
    }

    public void rollbackUser(Long userId, LocalDate date) {
        int rows = quotaMapper.rollbackUser(userId, date);
        if (rows > 0) {
            log.info("[ai] rollback user quota, userId={} date={}", userId, date);
        }
    }

    public void rollbackSite(LocalDate date) {
        int rows = quotaMapper.rollbackSite(date);
        if (rows > 0) {
            log.info("[ai] rollback site quota, date={}", date);
        }
    }

    public void addSiteTokens(LocalDate date, long tokens) {
        if (tokens <= 0) {
            return;
        }
        quotaMapper.addSiteTokens(date, tokens);
    }

    public int countUsed(Long userId, LocalDate date) {
        Integer used = quotaMapper.selectUserUsed(userId, date);
        return used == null ? 0 : used;
    }

    public int countSiteUsed(LocalDate date) {
        Integer used = quotaMapper.selectSiteUsed(date);
        return used == null ? 0 : used;
    }

    public int userDailyLimit() {
        return props.getQuota().getUserDaily();
    }

    public int siteDailyLimit() {
        return props.getQuota().getSiteDaily();
    }
}
