package com.ai.filter;

import com.ai.utils.CurrentHolder;
import com.ai.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;

@Component
@Slf4j
public class TokenFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String requestUri = request.getRequestURI();

        // ✅ 添加详细日志
        log.info("========== TokenFilter 开始 ==========");
        log.info("请求方法: {}", request.getMethod());
        log.info("请求URI: {}", requestUri);
        log.info("Remote Address: {}", request.getRemoteAddr());

        // ===================== 1. 公开接口：彻底放行 =====================
        if (requestUri.startsWith("/public/") ||
                requestUri.startsWith("/auth/")) {
            log.info("公开接口，直接放行: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 提取Token
        String token = request.getHeader("Authorization");
        log.info("Authorization Header: {}", token != null ? token.substring(0, Math.min(30, token.length())) + "..." : "null");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        } else {
            token = request.getHeader("token");
            log.info("尝试从 'token' header 获取: {}", token != null ? "有值" : "null");
        }

        // 3. 无Token：匿名放行（不清理上下文）
        if (token == null || token.isEmpty()) {
            log.warn("⚠️ 无Token，匿名放行: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        // 4. 有Token：校验
        try {
            log.info("开始解析Token...");
            Claims claims = JwtUtils.parseToken(token);
            Long userId = Long.valueOf(claims.get("id").toString());
            String role = claims.get("role") != null ? claims.get("role").toString() : "USER";

            log.info("✅ Token解析成功，userId: {}, name: {}, role: {}", userId, claims.get("name"), role);

            CurrentHolder.setCurrentId(userId);
            CurrentHolder.setCurrentRole(role);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("SecurityContext 已设置，继续过滤链");

            filterChain.doFilter(request, response);

            log.info("========== TokenFilter 结束 ==========");

        } catch (Exception e) {
            log.error("❌ Token验证失败: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"登录状态无效，请重新登录\"}");
        } finally {
            CurrentHolder.remove();
            SecurityContextHolder.clearContext();
            log.info("清理 ThreadLocal 和 SecurityContext");
        }
    }
}
