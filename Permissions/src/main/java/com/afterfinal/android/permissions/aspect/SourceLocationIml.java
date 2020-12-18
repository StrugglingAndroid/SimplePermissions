package com.afterfinal.android.permissions.aspect;

import org.aspectj.lang.reflect.SourceLocation;

class SourceLocationIml implements ISourceLocation {
    private final SourceLocation mRealSourceLocation;

    public SourceLocationIml(SourceLocation sourceLocation) {
        mRealSourceLocation = sourceLocation;
    }

    @Override
    public Class getWithinType() {
        return mRealSourceLocation.getWithinType();
    }

    @Override
    public String getFileName() {
        return mRealSourceLocation.getFileName();
    }

    @Override
    public int getLine() {
        return mRealSourceLocation.getLine();
    }

    @Override
    @Deprecated
    public int getColumn() {
        return mRealSourceLocation.getColumn();
    }
}
