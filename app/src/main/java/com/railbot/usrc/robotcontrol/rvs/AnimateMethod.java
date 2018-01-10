package com.railbot.usrc.robotcontrol.rvs;

/**
 * Created by usrc on 18. 1. 10.
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface AnimateMethod {
}
