package com.fsa.aicenter.infrastructure.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * IP工具类
 *
 * @author FSA AI Center
 */
@Slf4j
public class IpUtil {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final int IP_MAX_LENGTH = 15;

    /**
     * 获取客户端真实IP地址
     *
     * @param request HTTP请求
     * @return IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            // 多次反向代理后会有多个IP值，第一个IP才是真实IP
            int index = ip.indexOf(",");
            if (index != -1) {
                ip = ip.substring(0, index);
            }
        }

        if (!isValidIp(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (!isValidIp(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        if (!isValidIp(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        if (!isValidIp(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }

        if (!isValidIp(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }

        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        // IPv6本地地址转换为IPv4
        if (LOCALHOST_IPV6.equals(ip)) {
            ip = LOCALHOST_IPV4;
        }

        return ip;
    }

    /**
     * 判断IP是否有效
     *
     * @param ip IP地址
     * @return 是否有效
     */
    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip);
    }

    /**
     * 判断是否为内网IP
     *
     * @param ip IP地址
     * @return 是否为内网IP
     */
    public static boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        if (LOCALHOST_IPV4.equals(ip) || LOCALHOST_IPV6.equals(ip)) {
            return true;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            int firstPart = Integer.parseInt(parts[0]);
            int secondPart = Integer.parseInt(parts[1]);

            // 10.0.0.0 - 10.255.255.255
            if (firstPart == 10) {
                return true;
            }

            // 172.16.0.0 - 172.31.255.255
            if (firstPart == 172 && secondPart >= 16 && secondPart <= 31) {
                return true;
            }

            // 192.168.0.0 - 192.168.255.255
            if (firstPart == 192 && secondPart == 168) {
                return true;
            }

            return false;
        } catch (NumberFormatException e) {
            log.warn("Invalid IP address: {}", ip);
            return false;
        }
    }
}
