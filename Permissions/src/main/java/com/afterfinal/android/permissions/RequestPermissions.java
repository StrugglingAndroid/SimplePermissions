package com.afterfinal.android.permissions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestPermissions {

    String[] value();

    boolean execWhenRejected() default true;

    boolean exitWhenRejected() default false;

    TipMode tipMode() default TipMode.Toast;
}