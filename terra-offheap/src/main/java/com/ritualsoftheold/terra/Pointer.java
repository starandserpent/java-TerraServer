package com.ritualsoftheold.terra;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When applied to a field, local variable or parameter, @Pointer indicates
 * that they are used as a memory address. Otherwise, the return value of
 * annotated method is a memory address.
 *
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.PARAMETER})
public @interface Pointer {

}
