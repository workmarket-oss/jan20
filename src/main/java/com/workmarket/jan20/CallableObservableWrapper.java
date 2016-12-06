/*
 * Copyright 2016, WorkMarket, Inc. All Rights Reserved.
 */
package com.workmarket.jan20;

import java.util.concurrent.Callable;

import rx.Observable;

/**
 * Interface for control and experiment wrappers for {@link Trial}s.
 * <p>
 * At some point I'd love to make these simpler to create, but typing gets in the way.
 */
public interface CallableObservableWrapper {
  /**
   * Returns a Callable&lt;Observable&lt;T&gt;&gt; in place of the original item.  Can be used to add pre-post
   * behavior
   *
   * @param item The item to wrap.
   * @return the wrapped item.
   */
  <T> Callable<Observable<T>> wrap(final Callable<Observable<T>> item);
}
