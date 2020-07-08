package com.atlassian.plugins.slack.util;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

public class DigestUtil {
    public static long crc32(final String input) {
        long hash = 0;
        if (StringUtils.isNotBlank(input)) {
            CRC32 hasher = new CRC32();
            hasher.update(input.getBytes(StandardCharsets.UTF_8));
            hash = hasher.getValue();
        }
        return hash;
    }
}
