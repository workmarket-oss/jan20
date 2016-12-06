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
