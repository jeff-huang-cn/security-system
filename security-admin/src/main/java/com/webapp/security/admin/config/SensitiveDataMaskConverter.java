package com.webapp.security.admin.config;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * 敏感数据掩码转换�?- 委托�?web-core-sdk 中的实现
 * 
 * @author webapp-auth-system
 * @since 1.0.0
 */
public class SensitiveDataMaskConverter extends com.webapp.core.utils.SensitiveDataMaskConverter {

    @Override
    protected String transform(ILoggingEvent event, String in) {
        return super.transform(event, in);
    }
}

