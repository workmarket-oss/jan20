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

import java.util.Comparator;
import java.util.Iterator;

/**
 * While there is Comparator, there was not interface I knew of that was basically IsEqual.  Guava's Equivalence is
 * closest but is both more and less than I want.  It would be nice at some point to be able to extract a BiPredicate
 * and move applicable methods there, but making it so that alwaysFalse and such would return the type you wanted
 * appears to step into some JLS gray areas between JDK 7 and 8 where it fails depending on which one you're using
 * which
 * for this use case is bad.
 * <p>
 * If/when I don't have to worry about Java 7, this will be turned into an interface with default methods.
 *
 * @param <T> The type of the thing that will eventually be compared.
 */
public abstract class IsEqual<T> implements IsEqualPredicate<T> {
  /**
   * Makes an IsEqual from a comparator.
   *
   * @param comparator the comparator to make IsEqual from.
   */
  public static <T> IsEqual<T> useComparator(final Comparator<? super T> comparator) {
    Preconditions.checkNotNull(comparator);
    return new IsEqual<T>() {
      @Override
      public boolean apply(final T control, final T experiment) {
        return comparator.compare(control, experiment) == 0;
      }
    };
  }

  /**
   * Makes an IsEqual based on a BiPredicate.
   *
   * @param predicate The predicate to evaluate.
   */
  public static <T> IsEqual<T> use(final IsEqualPredicate<T> predicate) {
    Preconditions.checkNotNull(predicate);
    return new IsEqual<T>() {
      @Override
      public boolean apply(final T control, final T experiment) {
        return predicate.apply(control, experiment);
      }
    };
  }

  /**
   * Makes an IsEqual from the normal .equals() method, and is null-safe.
   */
  public static <T> IsEqual<T> useDotEquals() {
    return new IsEqual<T>() {
      @Override
      public boolean apply(final T control, final T experiment) {
        return control == experiment || (control != null && control.equals(experiment));
      }
    };
  }

  /**
   * Makes an IsEqual from ==.  Is null-safe.
   */
  public static <T> IsEqual<T> useDoubleEquals() {
    return new IsEqual<T>() {
      @Override
      public boolean apply(final T control, final T experiment) {
        return control == experiment;
      }
    };
  }

  /**
   * Returns an IsEqual that always returns true.
   */
  public static <T> IsEqual<T> alwaysTrue() {
    return new IsEqual<T>() {
      @Override
      public boolean apply(final T control, final T experiment) {
        return true;
      }
    };
  }

  /**
   * Returns an IsEqual that always returns false.
   */
  public static <T> IsEqual<T> alwaysFalse() {
    return new IsEqual<T>() {
      @Override
      public boolean apply(final T control, final T experiment) {
        return false;
      }
    };
  }

  /**
   * Short circuit OR operation -- evaluates this first and if it's false, evaluates next.
   *
   * @param rest The next equality operations to check.
   */
  @SafeVarargs
  public final IsEqual<T> or(final IsEqualPredicate<? super T>... rest) {
    return new ShortCircuitBoolean<T>(true, this, rest);
  }

  /**
   * Short circuit AND operation -- evaluates this first and if true, returns the value of next, otherwise returns
   * false.
   *
   * @param rest The next equality operations to check.
   */
  @SafeVarargs
  public final IsEqual<T> and(final IsEqualPredicate<? super T>... rest) {
    return new ShortCircuitBoolean<T>(false, this, rest);
  }

  private static final class ShortCircuitBoolean<T> extends IsEqual<T> {
    private final boolean shortCircuitOn;
    private final IsEqual<? super T> first;
    private final IsEqualPredicate<? super T>[] rest;

    @SafeVarargs
    public ShortCircuitBoolean(
        final boolean shortCircuitOn,
        final IsEqual<? super T> first,
        final IsEqualPredicate<? super T>... rest) {
      Preconditions.checkNotNull(rest);
      for (final IsEqualPredicate<? super T> item : rest) {
        Preconditions.checkNotNull(item);
      }
      this.shortCircuitOn = shortCircuitOn;
      this.first = first;
      this.rest = rest;
    }

    @Override
    public boolean apply(final T control, final T experiment) {
      if (first.apply(control, experiment) == shortCircuitOn) {
        return shortCircuitOn;
      }
      for (final IsEqualPredicate<? super T> item : rest) {
        if (item.apply(control, experiment) == shortCircuitOn) {
          return shortCircuitOn;
        }
      }
      return !shortCircuitOn;
    }
  }

  /**
   * Returns a negation of the current IsEqual.
   */
  public final IsEqual<T> negate() {
    return new IsEqual<T>() {
      @Override
      public boolean apply(final T control, final T experiment) {
        return !IsEqual.this.apply(control, experiment);
      }
    };
  }

  /**
   * Returns an IsEqual that checks that the null-status of both is the same.  If it's different, it returns false,
   * otherwise if they are both null, it returns true.  Put on an existing IsEqual, it removes the need for it to
   * handle null values.
   */
  public final IsEqual<T> checkNullEquality() {
    return new IsEqual<T>() {
      @Override
      public boolean apply(final T control, final T experiment) {
        return ((control == null) == (experiment == null)) // if null status is different, false
            && (control == null || IsEqual.this.apply(control, experiment)); // a==null implies b==null, so
        // true, or defer to this
      }
    };
  }

  /**
   * Given the current IsEqual, return an IsEqual that performs the current IsEqual on the extraction
   * of a T from a value of U.
   *
   * @param function The function to use to extract values.
   */
  public final <U> IsEqual<U> onExtractionOf(final Function<? super U, ? extends T> function) {
    Preconditions.checkNotNull(function);
    return new IsEqual<U>() {
      @Override
      public boolean apply(final U control, final U experiment) {
        return IsEqual.this.apply(function.apply(control), function.apply(experiment));
      }
    };
  }

  /**
   * Returns an IsEqual that operates on two iterables of T and checks to see that they are equal element by element.
   */
  public final IterableIsEqual<T> pairwiseEqual() {
    return new Pairwise<T>(this);
  }

  private static class Pairwise<T> extends IterableIsEqual<T> {
    private final IsEqual<T> original;

    public Pairwise(final IsEqual<T> original) {
      this.original = original;
    }

    @Override
    public boolean apply(final Iterable<T> control, final Iterable<T> experiment) {
      final Iterator<T> aIt = control.iterator();
      final Iterator<T> bIt = experiment.iterator();

      while (aIt.hasNext() && bIt.hasNext()) {
        if (!original.apply(aIt.next(), bIt.next())) {
          return false;
        }
      }

      return !aIt.hasNext() && !bIt.hasNext();
    }
  }
}
