package com.codeinterview.auth;

import java.util.Locale;
import java.util.Set;

/**
 * 密码策略（02 §5.4.1 v3.4 放宽版）：
 * 6–32 位、不限字符类型；只拒全同字符、顺序递增串（≥7 位）与等于昵称。
 * <p>
 * 说明（见 02 §10 偏离登记）：06 T04 验收要求 `123456` 通过、`123456789` 被拒，
 * 故「顺序递增」按整串连续递增且长度 ≥7 判定，长度 6 的 `123456` 予以放行。
 *
 * <p>生产管理员使用更严格的口径（{@link #validateAdminForProd}）：
 * 至少 12 位，并拒绝常见弱口令；普通用户策略保持既有产品验收口径不变。
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 6;
    public static final int MAX_LENGTH = 32;
    public static final int ADMIN_MIN_LENGTH = 12;
    private static final int ASCENDING_MIN_LENGTH = 7;

    private static final Set<String> COMMON_WEAK_PASSWORDS = Set.of(
            "123456", "1234567", "12345678", "123456789", "password", "passw0rd",
            "qwerty", "qwerty123", "111111", "000000", "admin", "admin123",
            "abc123", "123123", "iloveyou", "letmein", "welcome"
    );

    private PasswordPolicy() {
    }

    /**
     * @return null 表示通过；否则返回可直接展示给用户的错误文案
     */
    public static String validate(String raw, String nickname) {
        if (raw == null || raw.isEmpty()) {
            return "密码不能为空";
        }
        if (raw.length() < MIN_LENGTH || raw.length() > MAX_LENGTH) {
            return "密码长度需为 6–32 位";
        }
        if (allSameChar(raw)) {
            return "密码太简单，请换一个";
        }
        if (isAscendingSequence(raw)) {
            return "密码太简单，请换一个";
        }
        if (nickname != null && raw.equalsIgnoreCase(nickname)) {
            return "密码不能与昵称相同";
        }
        return null;
    }

    public static boolean isStrongEnough(String raw, String nickname) {
        return validate(raw, nickname) == null;
    }

    /**
     * 生产管理员密码：至少 12 位，并拒绝常见弱口令。
     * 普通用户仍使用 {@link #validate(String, String)}，避免改变既有产品验收口径。
     */
    public static String validateAdminForProd(String raw, String nickname) {
        if (raw == null || raw.length() < ADMIN_MIN_LENGTH || raw.length() > MAX_LENGTH) {
            return "生产管理员密码需为 12–32 位";
        }
        String baseError = validate(raw, nickname);
        if (baseError != null) {
            return baseError;
        }
        if (COMMON_WEAK_PASSWORDS.contains(raw.toLowerCase(Locale.ROOT))) {
            return "密码太简单，请换一个";
        }
        return null;
    }

    private static boolean allSameChar(String raw) {
        char first = raw.charAt(0);
        for (int i = 1; i < raw.length(); i++) {
            if (raw.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAscendingSequence(String raw) {
        if (raw.length() < ASCENDING_MIN_LENGTH) {
            return false;
        }
        for (int i = 1; i < raw.length(); i++) {
            if (raw.charAt(i) != raw.charAt(i - 1) + 1) {
                return false;
            }
        }
        return true;
    }
}
