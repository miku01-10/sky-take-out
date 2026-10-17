package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 配置类，用于配置OSS
@Configuration
@Slf4j
public class OssConfiguration {
    // 用于创建AliOssUtil对象，并交给spring管理
    @Bean
    public AliOssUtil aliOssUtil(AliOssProperties aliossProperties) {
        log.info("创建AliOssUtil对象");
        AliOssUtil aliOssUtil = new AliOssUtil(aliossProperties.getEndpoint(),
                aliossProperties.getAccessKeyId(),
                aliossProperties.getAccessKeySecret(),
                aliossProperties.getBucketName());

        return aliOssUtil;
    }
}
