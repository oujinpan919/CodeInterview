package com.codeinterview.common.storage;

import java.util.Locale;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.codeinterview.common.exception.BusinessException;
import com.codeinterview.common.result.Result;
import com.codeinterview.common.result.ResultCode;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 图片上传（v4.12）。登录即可上传——头像和笔记配图面向所有用户；
 * 题目配图只给管理员用，但复用的是同一个接口（管理端页面本身有 /admin/** 权限保护，
 * 上传本身只是把文件写进 OSS，不需要再按业务场景分权限）。
 *
 * <p>为什么单独一个 /uploads 而不是各业务各写一个：
 * 嗅探、大小限制、key 规则、删除策略都只应有一份实现，业务侧只关心「我拿到 URL 了」。
 */
@Tag(name = "图片上传")
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;
    private final UploadRateGuard uploadRateGuard;

    /**
     * 上传一张图片。folder 决定 OSS 里的业务子目录（avatar / question / note）。
     * 返回 { key, url, size }：url 可直接写进 Markdown 或 avatar 字段，key 留给删除用。
     */
    @Operation(summary = "上传图片")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<StorageService.UploadResult> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "misc") String folder) {
        Long userId = StpUtil.getLoginIdAsLong();
        String normalizedFolder = folder == null ? "" : folder.trim().toLowerCase(Locale.ROOT);
        if ("question".equals(normalizedFolder) && !StpUtil.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有管理员可以上传题目配图");
        }
        if (!uploadRateGuard.tryAcquire(userId)) {
            throw new BusinessException(ResultCode.RATE_LIMITED, "上传太频繁，请稍后再试");
        }
        return Result.success(storageService.upload(file, folder, userId));
    }
}
