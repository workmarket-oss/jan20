/*
 * Copyright 2016, WorkMarket, Inc. All Rights Reserved.
 */
package com.workmarket.jan20;

/**
 * Presumably will be used until such time as we can use Java 8's BiPredicate.
 *
 * @param <T> Type of the argument.
 */
public interface IsEqualPredicate<T> {
  /**
   * Apply the predicate.
   */
  boolean apply(T control, T experiment);
}
