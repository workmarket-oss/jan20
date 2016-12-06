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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An IsEqual extended with things that make sense for comparing lists of things.
 *
 * @param <T> The type of thing contained in the iterable.
 */
public abstract class IterableIsEqual<T> extends IsEqual<Iterable<T>> {
  /**
   * Should rarely be necessary, but there are cases where the type information may have been stripped or things got
   * built up in a unique way, but this allows you access to the methods here if/when that happens.
   * <p>
   * You may think you want this to be able to handle original values of IsEqual&lt;List&lt;T&gt;&gt; or other
   * Iterable subclasses; you don't.  You only need IterableIsEqual for the correlation methods, which if you've
   * written an IsEquals for some other specialized collection, either just have it extend
   * IsEqual&lt;Iterable&lt;T&gt;&gt; or you don't need the correlations to begin with.  Read the source code
   * comment for this method for more details if you disagree.
   *
   * @param original the original IsEqual to convert to IterableIsEqual
   */
  public static <T> IterableIsEqual<T> fromIsEqualIterable(final IsEqual<Iterable<T>> original) {
    // I would really like to be able to pass in IsEqual<List<T>> or similar, but because of the
    // way things work in practice, it won't [and I believe cannot] work.  Please stop trying.
    // In short, IterableIsEqual extends IsEqual<Iterable<T>>. It's because of that inner
    // Iterable that you can't swap it out.  Really, believe me.  Even if you try to have an I parameter
    // that is I extends Iterable<T>.  All starts going well until you get to original.isEqual().
    // There (assuming you tried things with List), the isEqual wants List, but you're passing in
    // iterable, and therefore lose.  It may be possible to have a converter function that converts from
    // random iterable to the iterable type that original deals with, but it's probably best to just
    // leave it alone.  You have been warned.
    //     Number of failed attempts -> 3
    // That said, I did write one that took original, and a conversion function that converted Iterable<T>
    // to SubClassOfIterable<T>, but the more I thought of it, it really more smelled like "you're doing it wrong,"
    // but if I'm later proved to be wrong, let me (Drew Csillag) know.
    Preconditions.checkNotNull(original);
    if (original instanceof IterableIsEqual) {
      return (IterableIsEqual<T>) original;
    }
    return new IterableIsEqual<T>() {
      @Override
      public boolean apply(final Iterable<T> control, final Iterable<T> experiment) {
        return original.apply(control, experiment);
      }
    };
  }

  /**
   * For doing comparisons of iterables where things may be in different orders, correlate them by
   * the output of a key function.  Things like primary keys work well here.  If you expect duplicate
   * items within one of the iterables, you shouldn't use this, use {@link #correlateByOrdering(Comparator)} instead.
   *
   * @param keyFunction The function used to correlate items.
   * @throws DuplicateValueException if your key function is spitting out the same key for more than one value in a
   *                                 given iterable.
   */
  public final <K> IterableIsEqual<T> correlateByKey(
      final Function<? super T, ? extends K> keyFunction) {
    Preconditions.checkNotNull(keyFunction);
    return new Correlator<T, K>(keyFunction, this);
  }

  private static final class Correlator<T, K> extends IterableIsEqual<T> {
    private final Function<? super T, ? extends K> keyFunction;
    private final IterableIsEqual<T> next;

    private Correlator(final Function<? super T, ? extends K> keyFunction, final IterableIsEqual<T> next) {
      this.next = next;
      this.keyFunction = keyFunction;
    }

    @Override
    public boolean apply(final Iterable<T> control, final Iterable<T> experiment) {
      Preconditions.checkNotNull(control);
      Preconditions.checkNotNull(experiment);
      final Map<K, T> keyMap = Maps.newHashMap();

      // Index the left iterable
      for (final T item : control) {
        final K key = keyFunction.apply(item);
        if (keyMap.containsKey(key)) {
          throw new DuplicateValueException("Left iterable generates duplicate keys during correlateByKey");
        }
        keyMap.put(key, item);
      }

      final List<T> experimentList = ImmutableList.copyOf(experiment);
      final List<T> controlList = new ArrayList<T>();
      final Set<K> usedKeys = new HashSet<K>();
      // Look up map items by keying on experiment, and make the lists
      for (final T item : experiment) {
        final K experimentKey = keyFunction.apply(item);
        if (usedKeys.contains(experimentKey)) {
          throw new DuplicateValueException("Right iterable generates duplicate keys during correlateByKey");
        }
        usedKeys.add(experimentKey);
        if (!keyMap.containsKey(experimentKey)) {
          return false;
        }
        controlList.add(keyMap.get(experimentKey));
      }

      // If b has fewer items than a, this will be the case.
      if (controlList.size() < keyMap.size()) {
        return false;
      }

      return next.apply(controlList, experimentList);
    }
  }

  /**
   * For doing comparisons of iterables where things may be in different orders, correlate them by
   * sorting both iterables first.
   *
   * @param comparator The comparator used to sort.
   */
  public IterableIsEqual<T> correlateByOrdering(final Comparator<? super T> comparator) {
    Preconditions.checkNotNull(comparator);
    return new Sorting<T>(comparator, this);
  }

  private static final class Sorting<T> extends IterableIsEqual<T> {
    private final Ordering<? super T> ordering;
    private final IsEqual<Iterable<T>> next;

    private Sorting(final Comparator<? super T> comparator, final IsEqual<Iterable<T>> next) {
      this.next = next;
      this.ordering = Ordering.from(comparator);
    }

    @Override
    public boolean apply(final Iterable<T> control, final Iterable<T> experiment) {
      Preconditions.checkNotNull(control);
      Preconditions.checkNotNull(experiment);
      return next.apply(ordering.sortedCopy(control), ordering.sortedCopy(experiment));
    }
  }
}
