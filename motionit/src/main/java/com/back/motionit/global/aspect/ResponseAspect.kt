package com.back.motionit.global.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.back.motionit.global.respoonsedata.ResponseData;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class ResponseAspect {

	private final HttpServletResponse response;

	@Around("""
		(
			within
			(
				@org.springframework.web.bind.annotation.RestController *
			)
			&&
			(
				@annotation(org.springframework.web.bind.annotation.GetMapping)
				||
				@annotation(org.springframework.web.bind.annotation.PostMapping)
				||
				@annotation(org.springframework.web.bind.annotation.PutMapping)
				||
				@annotation(org.springframework.web.bind.annotation.DeleteMapping)
			)
		)
		||
		@annotation(org.springframework.web.bind.annotation.ResponseBody)
		""")
	public Object responseAspect(ProceedingJoinPoint joinPoint) throws Throwable {

		Object rst = joinPoint.proceed(); // 실제 수행 메서드

		if (rst instanceof ResponseData responseData) {
			response.setStatus(responseData.getStatusCode());
		}

		return rst;
	}

}
