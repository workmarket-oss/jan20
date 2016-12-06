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

import java.util.concurrent.Callable;

import rx.Observable;

/**
 * Interface for control and experiment wrappers for {@link Trial}s.
 * <p>
 * At some point I'd love to make these simpler to create, but typing gets in the way.
 */
public interface CallableObservableWrapper {
  /*
   * Returns a Callable&lt;Observable&lt;T&gt;&gt; in place of the original item.  Can be used to add pre-post
   * behavior
   *
   * @param item The item to wrap.
   * @return the wrapped item.
   */
  <T> Callable<Observable<T>> wrap(final Callable<Observable<T>> item);
}
