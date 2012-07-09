package org.motechproject.ananya.kilkari.web;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class GatewayLogger {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Pointcut("execution(public * org.springframework.web.client.RestTemplate.*(..))")
    public void allExternalHttpCalls() {
    }

    @Before("allExternalHttpCalls()")
    public void beforeRESTCall(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0) {
            logger.info("Accessing external url: {}", Arrays.toString(args));
        }
    }
}
