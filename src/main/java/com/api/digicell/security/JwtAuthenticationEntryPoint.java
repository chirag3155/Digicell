package com.api.digicell.security;

import java.io.IOException;

import com.api.digicell.utils.Constants;
import com.google.gson.Gson;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	// private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

	private Gson gson;

	public JwtAuthenticationEntryPoint(Gson gson) {
		this.gson = gson;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {

		// log.error("-- ERROR : [JwtAuthenticationEntryPoint]: token: {}, msg: {}", request.getHeader("Authorization"),
		// 		authException.getMessage());
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		// String defaultLangCode = GeneralConfig.globalVariables.get(Constants.DEFAULT_LANGUAGE);
		// String msg = GeneralConfig.globalizedMessages.get(Constants.INVALID_JWT_REQUEST).get(defaultLangCode);
		String msg = "Invalid JWT Token";
		GenericRes<String> res = new GenericRes<>(Constants.UNAUTHORIZED_JWT_CODE, Constants.UNSUCCESSFUL, msg);
		response.getOutputStream().println(gson.toJson(res));
	}
}
