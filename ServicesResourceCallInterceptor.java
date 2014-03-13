package com.be2.services.rs;

import com.be2.logging.ProtocolLogger;
import com.be2.logging.SystemLogger;
import com.be2.logging.MonitoringlLogger;
import com.be2.logging.UserId;
import com.be2.services.auth.*;
import com.be2.services.configuration.ServicesConf;
import com.be2.services.dto.AbstractDTO;
import com.be2.services.exception.ServicesException;
import com.be2.services.exception.ServicesServerException;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.notification.NotificationPublisher;
import org.springframework.jmx.export.notification.NotificationPublisherAware;
import org.springframework.stereotype.Component;

import javax.management.Notification;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;


@Aspect
@Component
@ManagedResource(objectName = "com.be2.services:name=REST")
public class ServicesResourceCallInterceptor implements NotificationPublisherAware {

    private static final String PROTOCOL = "REST";
    private static final String RESPONSE_FAIL = "fail";
    private static final String RESPONSE_SUCCESS = "success";

    private AtomicLong successful = new AtomicLong();
    private AtomicLong failed = new AtomicLong();
    private AtomicLong total = new AtomicLong();

    private AtomicLong minTime = new AtomicLong();
    private AtomicLong maxTime = new AtomicLong();
    private AtomicLong avgTime = new AtomicLong();
    private AtomicLong totalTime = new AtomicLong();

    private AtomicLong minSuccessfulTime = new AtomicLong();
    private AtomicLong maxSuccessfulTime = new AtomicLong();
    private AtomicLong avgSuccessfulTime = new AtomicLong();
    private AtomicLong totalSuccessfulTime = new AtomicLong();

    private AtomicLong minFailedTime = new AtomicLong();
    private AtomicLong maxFailedTime = new AtomicLong();
    private AtomicLong avgFailedTime = new AtomicLong();
    private AtomicLong totalFailedTime = new AtomicLong();

    @Autowired
    private ServicesConf servicesConf;
    @Autowired
    private DecryptAnnotationManager decryptAnnotationManager;
    @Autowired
    private HexAnnotationManager hexAnnotationManager;
    @Autowired
    private EncryptAnnotationManager encryptAnnotationManager;
    @Autowired
    private SubjectAnnotationManager subjectAnnotationManager;
    @Autowired
    private ProductAnnotationManager productAnnotationManager;
    @Autowired
    private ResourceRoleAnnotationManager resourceRoleAnnotationManager;
    private final AtomicLong localSequence = new AtomicLong();
    private NotificationPublisher notificationPublisher;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNotificationPublisher(NotificationPublisher notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    /**
     *
     * @return
     */
    @ManagedAttribute
    public long getMinFailedTime() {
        return minFailedTime.get();
    }

    /**
     *
     * @return
     */
    @ManagedAttribute
    public long getMaxFailedTime() {
        return maxFailedTime.get();
    }

    /**
     *
     * @return
     */
    @ManagedAttribute
    public long getAvgFailedTime() {
        return avgFailedTime.get();
    }

    /**
     *
     * @return
     */
    @ManagedAttribute
    public long getTotalFailedTime() {
        return totalFailedTime.get();
    }

    /**
     *
     * @return
     */
    @ManagedAttribute
    public long getMinSuccessfulTime() {
        return minSuccessfulTime.get();
    }

    /**
     *
     * @return
     */
    @ManagedAttribute
    public long getMaxSuccessfulTime() {
        return maxSuccessfulTime.get();
    }

    /**
     *
     * @return
     */
    @ManagedAttribute
    public long getAvgSuccessfulTime() {
        return avgSuccessfulTime.get();
    }

    /**
     *
     * @return
     */
    @ManagedAttribute
    public long getTotalSuccessfulTime() {
        return totalSuccessfulTime.get();
    }

    /**
     *
     * @return
     */
    @ManagedAttribute
    public long getMinTime() {
        return minTime.get();
    }

    /**
     *
     * @return
     */
    @ManagedAttribute
    public long getMaxTime() {
        return maxTime.get();
    }

    /**
     *
     * @return
     */
    @ManagedAttribute
    public long getAvgTime() {
        return avgTime.get();
    }

    /**
     *
     * @return
     */
    @ManagedAttribute
    public long getTotalTime() {
        return totalTime.get();
    }

    /**
     *
     * @return
     */
    @ManagedAttribute
    public long getSuccessful() {
        return successful.get();
    }

    /**
     *
     * @return
     */
    @ManagedAttribute
    public long getFailed() {
        return failed.get();
    }

    /**
     *
     * @return
     */
    @ManagedAttribute
    public long getTotal() {
        return total.get();
    }

    /**
     *
     */
    @Pointcut("execution(* com.be2.services.rs..*.*(..))")
    public void handleAccess() {
    }

    /**
     *
     * @param pjp
     * @return
     * @throws Throwable
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Around("handleAccess()")
    public Object restCallErrorHandling(ProceedingJoinPoint pjp)
    throws Throwable {
        long start = System.currentTimeMillis();
        boolean error = false;
        Method method = null;
        try {
            Object[] preProcessed = preProcess(pjp);
            method = (Method) preProcessed[0];
            Object obj = pjp.proceed((Object[]) preProcessed[1]);
            obj = postProcess(method, obj);
            successful.incrementAndGet();
            return obj;
        } catch (ServicesException e) {
            failed.incrementAndGet();
            error = true;
            SystemLogger.error(e);
            if (method == null) {
                method = getMethod(pjp);
            }
            Class returnType = null;
            if (method != null) {
                returnType = method.getReturnType();
            }
            if (returnType != null && AbstractDTO.class.isAssignableFrom(returnType)) {
                AbstractDTO dto = (AbstractDTO) returnType.newInstance();
                dto.setResultCode(e.getErrorCode());
                dto.setResultMessages(e.getErrorMessages());
                return dto;
            } else {
                throw e;
            }
        } catch (Throwable e) {
            failed.incrementAndGet();
            error = true;
            SystemLogger.error(e);
            throw new ServicesServerException(e);
        } finally {
            long time = System.currentTimeMillis() - start;
            total.incrementAndGet();
            if (method == null) {
                method = getMethod(pjp);
            }
            String source = method == null ? "N/A" : method.getDeclaringClass().getName() + "." + method.getName();
            ProtocolLogger.log(
                    UserId.SYSTEM,
                    PROTOCOL,
                    source,
                    error ? RESPONSE_FAIL : RESPONSE_SUCCESS,
                    "" + time
            );
            calculateTime(time, this.total.get(), this.minTime, this.maxTime, this.avgTime, this.totalTime);
            if (error) {
                calculateTime(time, this.failed.get(), this.minFailedTime, this.maxFailedTime, this.avgFailedTime, this.totalFailedTime);
            } else {
                calculateTime(time, this.successful.get(), this.minSuccessfulTime, this.maxSuccessfulTime, this.avgSuccessfulTime, this.totalSuccessfulTime);
            }
            alert(error, time, source);
        }
    }

    /**
     *
     * @param error
     * @param time
     */
    public void alert(boolean error, long time, String source) {
        if (this.notificationPublisher != null) {
            if (error && servicesConf.isMonitorAlertEnabledRestFailed()) {
                long alertSequence = localSequence.getAndIncrement();
                final Notification notification = new Notification(
                        "REST alert",
                        source,
                        alertSequence,
                        "Failed"
                );
                notificationPublisher.sendNotification(notification);
                if(servicesConf.isMonitorAlertEnabledRESTLog()) {
                    MonitoringlLogger.log(
                            alertSequence,
                            notification.getType(),
                            notification.getSource().toString(),
                            notification.getMessage()
                    );
                }
            }
            if (time > servicesConf.getMonitorAlertThresholdRestTime() && servicesConf.isMonitorAlertEnabledRestTime()) {
                long alertSequence = localSequence.getAndIncrement();
                final Notification notification = new Notification(
                        "REST alert",
                        source,
                        alertSequence,
                        "Time exceeded (" + servicesConf.getMonitorAlertThresholdRestTime() + "): " + time
                );
                notificationPublisher.sendNotification(notification);
                if(servicesConf.isMonitorAlertEnabledRESTLog()) {
                    MonitoringlLogger.log(
                            alertSequence,
                            notification.getType(),
                            notification.getSource().toString(),
                            notification.getMessage()
                    );
                }
            }
        }
    }

    /**
     *
     * @param time
     * @param total
     * @param minTime
     * @param maxTime
     * @param avgTime
     * @param totalTime
     */
    private void calculateTime(long time, long total, AtomicLong minTime, AtomicLong maxTime, AtomicLong avgTime, AtomicLong totalTime) {
        // here the values can be inaccurate due to lost atomicity,
        // but it is acceptable from statistical perspective
        long min = minTime.get();
        if (time < min || min == 0) {
            minTime.set(time);
        }
        // here the values can be inaccurate due to lost atomicity,
        // but it is acceptable from statistical perspective
        if (time > maxTime.get()) {
            maxTime.set(time);
        }
        long totalTimeValue = totalTime.addAndGet(time);
        avgTime.set(total == 0 ? 0 : totalTimeValue / total);
    }

    /**
     *
     * @param pjp
     * @throws OAuthServiceException
     */
    @SuppressWarnings("all")
    private Object[] preProcess(ProceedingJoinPoint pjp) throws OAuthServiceException, NoSuchMethodException {
        Method method = getMethod(pjp);
        Object[] args = pjp.getArgs();
        hexAnnotationManager.execute(method.getDeclaringClass(), method, args);
        decryptAnnotationManager.execute(method.getDeclaringClass(), method, args);
        subjectAnnotationManager.execute(method.getDeclaringClass(), method, args);
        productAnnotationManager.execute(method.getDeclaringClass(), method, args);
        resourceRoleAnnotationManager.execute(method.getDeclaringClass(), method, args);
        return new Object[] {method, args};
    }

    /**
     *
     * @param method
     * @param result
     * @return
     * @throws OAuthServiceException
     * @throws NoSuchMethodException
     */
    @SuppressWarnings("all")
    private Object postProcess(Method method, Object result) throws OAuthServiceException, NoSuchMethodException {
        Object[] objs = new Object[] {result};
        encryptAnnotationManager.execute(method.getDeclaringClass(), method, objs);
        return objs[0];
    }

    /**
     *
     * @param pjp
     * @return
     * @throws NoSuchMethodException
     */
    @SuppressWarnings("all")
    private Method getMethod(ProceedingJoinPoint pjp) throws NoSuchMethodException {
        Method method = null;
        if (pjp.getSignature() instanceof MethodSignature) {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            method = signature.getMethod();
        } else {
            Object[] args = pjp.getArgs();
            Class[] classes = null;
            if (args != null) {
                classes = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    classes[i] = args[i] == null ? null : args[i].getClass();
                }
            }
            if (classes == null) {
                classes = new Class[] {};
            }
            Signature sig = pjp.getSignature();
            Class targetClass = sig.getDeclaringType();
            method = targetClass.getMethod(sig.getName(), classes);
        }
        return method;
    }
}
