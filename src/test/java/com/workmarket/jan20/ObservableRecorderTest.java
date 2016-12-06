/*
 * Copyright 2016, WorkMarket, Inc. All Rights Reserved.
 */
package com.workmarket.jan20;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test that the ObservableRecorder works.
 */
public final class ObservableRecorderTest {

  private AtomicReference<TrialResult<Integer>> result;
  private ObservableRecorder<Integer> recorder;

  /**
   * Set up common bits.
   */
  @Before
  public void setUp() {
    result = new AtomicReference<TrialResult<Integer>>();
    recorder = new ObservableRecorder<Integer>(new ResultConsumer<Integer>() {
      @Override
      public void consume(final TrialResult<Integer> innerResult) {
        result.set(innerResult);
      }
    });
  }

  /**
   * Test that success works.
   */
  @Test
  public void testSuccess() {
    recorder.onNext(1);
    recorder.onNext(2);
    recorder.onCompleted();

    assertEquals(ImmutableList.of((Integer) 1, 2), result.get().getResult());
    assertNull(result.get().getException());
  }

  /**
   * Test that failure works.
   */
  @Test
  public void testFailureWithValues() {
    recorder.onNext(1);
    recorder.onNext(2);
    recorder.onError(new RuntimeException());

    assertEquals(ImmutableList.of((Integer) 1, 2), result.get().getResult());
    assertEquals(RuntimeException.class, result.get().getException().getClass());
  }

}
