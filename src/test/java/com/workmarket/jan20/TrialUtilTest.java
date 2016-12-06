/*
 * Copyright 2016, WorkMarket, Inc. All Rights Reserved.
 */
package com.workmarket.jan20;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test Trial Utilities.
 */
public class TrialUtilTest {
  /**
   * Test that makeObservable executes the callable when it ought to.
   */
  @Test
  public void testMakeObservable() throws Exception {
    final AtomicBoolean called = new AtomicBoolean(false);
    final Callable<Integer> original = new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        called.set(true);
        return 1;
      }
    };
    final Callable<Observable<Integer>> callableObservable = TrialUtil.makeObservable(original);
    assertFalse(called.get());
    final Observable<Integer> observable = callableObservable.call();
    assertFalse(called.get());
    assertEquals((Integer) 1, observable.toBlocking().single());
    assertTrue(called.get());
  }
}
