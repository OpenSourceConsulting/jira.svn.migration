package kr.osc.jira.svn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Main class with Spring Boot
 * 
 * - required java option : -Dspring.config.name=sg -Dspring.profiles.active=[local|dev|prd]
 * 
 * @author Tran Ho
 * 
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = { "kr.osc.jira.svn.rest" })
@PropertySource(value = { "classpath:config-${spring.profiles.active:local}.properties" })
public class MigrationBoot extends WebMvcConfigurerAdapter {

	public static void main(String[] args) {
		SpringApplication.run(MigrationBoot.class, args);
	}

	@Configuration
	// @Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
	@EnableWebSecurity
	protected static class ApplicationSecurity extends WebSecurityConfigurerAdapter {

		@Autowired
		private SecurityProperties security;

		@Override
		public void configure(WebSecurity web) throws Exception {
			web.ignoring().antMatchers("/", "/index.html", "/app.js", "/auth/notLogin*", "/auth/loginFail*", "/auth/accessDenied*", "/auth/onAfterLogout*");
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {

			http.anonymous();
		}
	}
}
