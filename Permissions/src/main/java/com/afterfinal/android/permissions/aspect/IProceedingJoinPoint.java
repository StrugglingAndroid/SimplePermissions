package com.afterfinal.android.permissions.aspect;

public interface IProceedingJoinPoint {
    Object proceed() throws Throwable;

    ISourceLocation getSourceLocation();
}
