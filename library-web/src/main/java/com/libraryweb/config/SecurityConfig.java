package com.libraryweb.config;

import com.libraryweb.filter.JwtAuthenticationFilter;
import com.libraryweb.handler.AccessDeniedHandlerImpl;
import com.libraryweb.handler.AuthenticationEntryPointImpl;
import com.libraryweb.security.RequestAuthorizationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 配置
 *
 * <p>注册 {@link SecurityFilterChain} 与 {@link AuthenticationManager}。
 *
 * <p>配置项说明（前后端分离 + JWT 模式）：
 * <ul>
 *   <li>关闭 CSRF：基于令牌认证，不依赖会话</li>
 *   <li>关闭表单登录 / HTTP Basic：避免未认证请求被重定向或弹原生登录框</li>
 *   <li>关闭框架自带退出接口：行为固定，需自行实现</li>
 *   <li>开启方法级安全注解：{@code @PreAuthorize} / {@code @PostAuthorize} 等生效</li>
 *   <li>跨域配置迁移到此处：由安全框架统一接管</li>
 *   <li>预检请求（OPTIONS）放行：避免跨域失败</li>
 *   <li>白名单放行：登录、注册、健康检查、错误页转发</li>
 *   <li>其余请求要求已认证</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;
    private final RequestAuthorizationManager requestAuthorizationManager;

    /**
     * 白名单：无需认证即可访问
     */
    private static final String[] WHITE_LIST = {
            // 登录注册
            "/user/login",
            "/user/register",
            // 健康检查
            "/health",
            "/health/**",
            // 开发调试
            "/test/**",
            // Druid 监控
            "/druid/**",
            // 静态资源
            "/favicon.ico",
            // 错误页转发
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 关闭 CSRF（前后端分离，使用 JWT）
                .csrf(AbstractHttpConfigurer::disable)
                // 关闭表单登录（避免未认证请求被重定向到登录页）
                .formLogin(AbstractHttpConfigurer::disable)
                // 关闭 HTTP Basic（避免浏览器弹原生登录框）
                .httpBasic(AbstractHttpConfigurer::disable)
                // 关闭框架自带退出接口（行为固定，需自行实现）
                .logout(AbstractHttpConfigurer::disable)
                // 跨域配置由安全框架统一接管
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 禁用 Session（前后端分离，使用无状态 JWT）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 路径授权：预检请求放行 + 白名单放行 + 其余请求按权限映射放行/拒绝
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        .requestMatchers(WHITE_LIST).permitAll()
                        .anyRequest().access(requestAuthorizationManager))
                // 添加 JWT 过滤器（必须在用户名密码过滤器之前，否则后续授权读不到身份）
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 自定义未认证/无权限处理器
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    /**
     * 密码编码器：BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器：供登录接口调用
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS 配置
     *
     * <p>允许任意来源、任意头、任意方法（开发期默认配置）；
     * 生产环境应替换为具体允许的来源列表。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        // setAllowCredentials(true) 时不允许使用 addAllowedOrigin("*")，必须用 OriginPattern
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
