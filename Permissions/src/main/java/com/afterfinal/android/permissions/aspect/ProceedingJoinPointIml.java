package com.afterfinal.android.permissions.aspect;

import org.aspectj.lang.ProceedingJoinPoint;

class ProceedingJoinPointIml implements IProceedingJoinPoint{

    private final ProceedingJoinPoint mRealProceedingJoinPoint;

    public ProceedingJoinPointIml(ProceedingJoinPoint joinPoint) {
        mRealProceedingJoinPoint = joinPoint;
    }

    @Override
    public Object proceed() throws Throwable {
        return mRealProceedingJoinPoint.proceed(mRealProceedingJoinPoint.getArgs());
    }

    @Override
    public ISourceLocation getSourceLocation() {
        return new SourceLocationIml(mRealProceedingJoinPoint.getSourceLocation());
    }
}
