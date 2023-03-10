package com.socket.server.config;

import cn.hutool.http.ContentType;
import com.socket.core.constant.ChatConstants;
import com.socket.core.model.enums.HttpStatus;
import com.socket.core.util.Enums;
import com.socket.server.custom.CustomRealm;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticationFilter;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class ShiroConfig {
    private final ChatConstants constants;
    private final CustomRealm customRealm;

    @Bean
    public ShiroFilterFactoryBean getShiroFilterFactoryBean() {
        ShiroFilterFactoryBean bean = new ShiroFilterFactoryBean();
        bean.setSecurityManager(getDefaultWebSecurityManager());
        Map<String, Filter> filters = bean.getFilters();
        filters.put("user", new JsonRetuenFilter());
        Map<String, String> definitionMap = bean.getFilterChainDefinitionMap();
        definitionMap.put("/login/**", "anon");
        definitionMap.put("/qqlogin/**", "anon");
        definitionMap.put("/wxlogin/**", "anon");
        definitionMap.put("/secure/**", "anon");
        definitionMap.put("/**", "user");
        return bean;
    }

    @Bean
    public SecurityManager getDefaultWebSecurityManager() {
        DefaultWebSecurityManager security = new DefaultWebSecurityManager();
        security.setRememberMeManager(getCookieRememberMeManager());
        security.setRealm(customRealm);
        return security;
    }

    @Bean
    public CookieRememberMeManager getCookieRememberMeManager() {
        CookieRememberMeManager rememberMeManager = new CookieRememberMeManager();
        rememberMeManager.setCipherKey("SNVIUDEFWHDWIEJF".getBytes());
        SimpleCookie simpleCookie = new SimpleCookie("RSID");
        // ??????????????????30???
        simpleCookie.setMaxAge(60 * 60 * 24 * 30);
        rememberMeManager.setCookie(simpleCookie);
        return rememberMeManager;
    }

    class JsonRetuenFilter extends AuthenticationFilter {
        @Override
        protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
            Subject subject = SecurityUtils.getSubject();
            // ??????Cookie
            if (subject.isRemembered()) {
                return true;
            }
            // ??????????????????
            if (WebUtils.toHttp(request).getHeader(constants.getAuthServerHeader()) != null) {
                return true;
            }
            // ??????403
            WebUtils.toHttp(response).setStatus(403);
            response.setContentType(ContentType.JSON.toString(StandardCharsets.UTF_8));
            response.getWriter().write(Enums.toJSON(HttpStatus.UNAUTHORIZED.message("??????????????????")));
            return false;
        }
    }
}
