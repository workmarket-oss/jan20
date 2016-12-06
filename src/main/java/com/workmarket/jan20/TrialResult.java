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

import java.util.Collections;
import java.util.List;

/**
 * The result of running an experiment.
 *
 * @param <T> The Observable type parameter.
 */
public final class TrialResult<T> {
  private final List<T> result;
  private final Throwable exception;

  /**
   * The result of an experiment is the (possibly empty) list of result items from the observable, and possibly
   * an exception if the observable ran into trouble.  If exception == null, the observables onCompleted() was
   * called.
   *
   * @param result    The result list of items from the observable.  This is always non-null, but possibly empty.
   * @param exception The exception, if any, that was sent to onError().
   */
  public TrialResult(final List<T> result, final Throwable exception) {
    // using unmodifiable ArrayList because ImmutableList don't support nulls.
    this.result = Collections.unmodifiableList(result);
    this.exception = exception;
  }

  /**
   * Return the result list.
   */
  public List<T> getResult() {
    return result;
  }

  /**
   * Return the exception that was thrown, if any.  Otherwise returns null.
   */
  public Throwable getException() {
    return exception;
  }
}
