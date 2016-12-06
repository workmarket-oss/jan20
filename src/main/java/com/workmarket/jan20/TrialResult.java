/*
 * Copyright 2016, WorkMarket, Inc. All Rights Reserved.
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
