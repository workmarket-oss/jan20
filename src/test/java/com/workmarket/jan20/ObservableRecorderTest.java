/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
