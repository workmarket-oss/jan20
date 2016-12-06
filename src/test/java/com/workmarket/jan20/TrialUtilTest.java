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
