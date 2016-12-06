/*
 * Copyright 2016, WorkMarket, Inc. All Rights Reserved.
 */
package com.workmarket.jan20;

import java.util.ArrayList;
import java.util.List;

import rx.Observer;

/**
 * An {@link Observer} that records what an {@link Observer} sees.
 *
 * @param <T> The type parameter to the {@link Observer}.
 */
final class ObservableRecorder<T> implements Observer<T> {
  private final List<T> values;
  private final ResultConsumer<T> onTerminate;

  /**
   * Create the recorder and call onTerminate with the recorded bits.
   *
   * @param onTerminate the {@link ResultConsumer} to call with the recorded value.
   */
  public ObservableRecorder(final ResultConsumer<T> onTerminate) {
    this.values = new ArrayList<>();
    this.onTerminate = onTerminate;
  }

  @Override
  public void onCompleted() {
    onTerminate.consume(new TrialResult<T>(values, null));
  }

  @Override
  public void onError(final Throwable e) {
    onTerminate.consume(new TrialResult<T>(values, e));
  }

  @Override
  public void onNext(final T t) {
    values.add(t);
  }
}
