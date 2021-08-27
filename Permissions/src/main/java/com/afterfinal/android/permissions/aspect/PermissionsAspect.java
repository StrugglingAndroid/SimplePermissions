package com.afterfinal.android.permissions.aspect;

import com.afterfinal.android.permissions.Options;
import com.afterfinal.android.permissions.PermissionRequestCallback;
import com.afterfinal.android.permissions.Permissions;
import com.afterfinal.android.permissions.RequestPermissions;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.runtime.internal.AroundClosure;

import java.lang.reflect.Method;
import java.util.List;

@Aspect
@SuppressWarnings("all")
public class PermissionsAspect {

    @Pointcut("execution(@com.afterfinal.android.permissions.RequestPermissions * **(..))")
    public void withinAnnotatedMethods() {
    }

    @Around("withinAnnotatedMethods()")
    public void aroundRequestPermissionsPoint(final ProceedingJoinPoint joinPoint) throws Throwable {
        if (joinPoint.getSignature() instanceof MethodSignature) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            final RequestPermissions requestPermissions = method.getAnnotation(RequestPermissions.class);
            if (null == requestPermissions) {
                joinPoint.proceed();
                return;
            }
            String[] permissionsArr = requestPermissions.value();
            if (null == permissionsArr || 0 == permissionsArr.length) {
                joinPoint.proceed();
                return;
            }
            final Options options = new Options(requestPermissions.execWhenRejected(), requestPermissions.exitWhenRejected(), requestPermissions.tipMode());
            Permissions.request(Permissions.getContext(), options, new Permissions.Callback() {
                @Override
                public void onResult(Options options, List<String> granted, List<String> rejected) {
                    if (joinPoint.getThis() instanceof PermissionRequestCallback) {
                        if (0 == rejected.size() || options.execWhenRejected) {
                            PermissionRequestCallback callback = (PermissionRequestCallback) joinPoint.getThis();
                            callback.onPermissionRequestResult(options, granted, rejected, new ProceedingJoinPointIml(joinPoint));
                        }
                    } else {
                        if (0 == rejected.size() || options.execWhenRejected) {
                            try {
                                joinPoint.proceed(joinPoint.getArgs());
                            } catch (Throwable throwable) {
                                throwable.printStackTrace();
                            }
                        }
                    }
                }
            }, permissionsArr);
        } else {
            joinPoint.proceed();
        }
    }
}
