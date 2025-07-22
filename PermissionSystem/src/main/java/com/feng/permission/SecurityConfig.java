package com.feng.permission;

import javax.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * TODO
 *
 * @since 2025/3/12
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final UrlPermissionFilter urlFilter;

    @Autowired
    public SecurityConfig(UrlPermissionFilter urlFilter) {
        this.urlFilter = urlFilter;
    }
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
               .antMatchers("/admin/**").hasRole("ADMIN")
               .antMatchers("/user/**").hasRole("USER")
               .anyRequest().authenticated()
               .and()
               .formLogin()
               .loginPage("/login")
               .permitAll()
               .and()
               .logout()
               .permitAll();
        http.addFilterBefore(urlFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
