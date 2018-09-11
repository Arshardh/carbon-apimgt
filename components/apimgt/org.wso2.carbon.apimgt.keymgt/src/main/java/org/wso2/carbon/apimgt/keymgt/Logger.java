package org.wso2.carbon.apimgt.keymgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.utils.CarbonUtils;

import java.util.List;

@Aspect
public class Logger
{
    private static final Log log = LogFactory.getLog("timing");

    @Pointcut("(execution(* org.wso2.carbon.apimgt.keymgt.service..*(..)) ||" +
            " execution(* org.wso2.carbon.apimgt.keymgt.token..*(..))) && if()")
    public static boolean pointCut() {
        boolean enabled = false;
        String config = CarbonUtils.getServerConfiguration().getFirstProperty("EnableTimingLogs");
        if (config != null && !config.equals("")) {
            enabled = Boolean.parseBoolean(config);
        }
        return enabled;
    }

    @Around("pointCut()")
    public Object log(ProceedingJoinPoint point) throws Throwable
    {
        long start = System.currentTimeMillis();
        Object result = point.proceed();
        log.info("className="+MethodSignature.class.cast(point.getSignature()).getDeclaringTypeName()+
                ", methodName="+MethodSignature.class.cast(point.getSignature()).getMethod().getName()+
                ",threadId="+Thread.currentThread().getId() + ", timeMs="+ (System.currentTimeMillis() - start) );
        return result;
    }
}
