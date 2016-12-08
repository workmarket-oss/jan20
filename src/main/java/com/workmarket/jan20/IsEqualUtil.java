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

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

/**
 * Utility functions to make writing good IsEqual classes.
 * <p>
 * Note! These do not short circuit when one returns false, they all continue to go.
 */
public final class IsEqualUtil {

  private IsEqualUtil() { }


  /**
   * Interface for things to consume the mismatches the compares may spit out.
   */
  public interface MismatchConsumer {
    /**
     * Consume a mismatch string.
     *
     * @param item the item to consume
     */
    void accept(final String item);
  }

  /**
   * Check if the null-ness of control and experiment are the same.
   *
   * @param control    the first thing to compare
   * @param experiment the second thing to compare
   * @param consumer   the mismatch consumer
   * @return true if their nullness is equal
   */
  public static boolean checkNullity(final Object control, final Object experiment, final MismatchConsumer consumer) {
    final boolean result = (control == null) == (experiment == null);
    if (result) {
      return true;
    }
    consumer.accept("nullity");
    return false;
  }

  /**
   * Creates a MismatchConsumer that will consume it to a list.
   *
   * @param list the list to consume mismatches to
   * @return the consumer.
   */
  public static MismatchConsumer consumeToList(final List<String> list) {
    return new MismatchConsumer() {
      @Override
      public void accept(final String item) {
        list.add(item);
      }
    };
  }

  /**
   * Start a comparison chain.
   *
   * @param consumer the mismatch consumer
   * @return the chain
   */
  public static EqualChain startCompare(final MismatchConsumer consumer) {
    return new EqualChain(consumer, true);
  }

  /**
   * A class to allow for chaining of comparisons.
   */
  public static final class EqualChain {
    private final MismatchConsumer consumer;
    private final boolean result;

    private EqualChain(final MismatchConsumer consumer, final boolean previousResult) {
      this.consumer = consumer;
      this.result = previousResult;
    }

    private EqualChain continueChain(final boolean newResult, final String name) {
      if (newResult) {
        return this;
      }
      consumer.accept(name);
      if (!result) {
        return this; // already was false
      } else { // was a true chain, make it false
        return new EqualChain(consumer, false);
      }
    }

    /**
     * Compare control and experiment using ==.
     *
     * @param control    the first thing to compare
     * @param experiment the second thing to compare
     * @param name       the name to put into the errors list when they don't match
     * @return a new equal chain
     */
    public EqualChain doubleEquals(final Object control, final Object experiment, final String name) {
      return continueChain(control == experiment, name);
    }


    /**
     * Compare Doubles.
     *
     * @param control    the first thing to compare
     * @param experiment the second thing to compare
     * @param name       the name to put into the errors list when they don't match
     * @deprecated use {@link #doubleEquals(Double, Double, Double, String)}
     */
    @Deprecated
    public void doubleEquals(final Double control, final Double experiment, final String name) {
      throw new RuntimeException("BUG: will not doubleEquals a Double without a delta");
    }

    /**
     * Compare doubles.
     *
     * @param control    the first thing to compare
     * @param experiment the second thing to compare
     * @param delta      how close the values need to be
     * @param name       the name to put into the errors list when they don't match
     */
    public EqualChain doubleEquals(
        final Double control,
        final Double experiment,
        final Double delta,
        final String name) {
      return continueChain(Math.abs(control - experiment) < delta, name);
    }

    /**
     * Compare floats.
     *
     * @param control    the first thing to compare
     * @param experiment the second thing to compare
     * @param name       the name to put into the errors list when they don't match
     * @deprecated use {@link #doubleEquals(Float, Float, Float, String)}
     */
    @Deprecated
    public void doubleEquals(final Float control, final Float experiment, final String name) {
      throw new RuntimeException("BUG: will not doubleEquals a Float without a delta");
    }

    /**
     * Compare floats.
     *
     * @param control    the first thing to compare
     * @param experiment the second thing to compare
     * @param delta      how close the values need to be
     * @param name       the name to put into the errors list when they don't match
     */
    public EqualChain doubleEquals(
        final Float control,
        final Float experiment,
        final Float delta,
        final String name) {
      return continueChain(Math.abs(control - experiment) < delta, name);
    }

    /**
     * Compare control and experiment using .equals().
     *
     * @param control    the first thing to compare
     * @param experiment the second thing to compare
     * @param name       the name to put into the errors list when they don't match
     * @return a new equal chain
     */
    public EqualChain dotEquals(final Object control, final Object experiment, final String name) {
      return continueChain(Objects.equal(control, experiment), name);
    }

    /**
     * Compare Doubles.
     *
     * @param control    the first thing to compare
     * @param experiment the second thing to compare
     * @param name       the name to put into the errors list when they don't match
     * @deprecated use {@link #dotEquals(Double, Double, Double, String)}
     */
    @Deprecated
    public void dotEquals(final Double control, final Double experiment, final String name) {
      throw new RuntimeException("BUG: will not dotEquals a Double without a delta");
    }

    /**
     * Compare doubles.
     *
     * @param control    the first thing to compare
     * @param experiment the second thing to compare
     * @param delta      how close the values need to be
     * @param name       the name to put into the errors list when they don't match
     */
    public EqualChain dotEquals(
        final Double control,
        final Double experiment,
        final Double delta,
        final String name) {
      return continueChain(Math.abs(control - experiment) < delta, name);
    }

    /**
     * Compare floats.
     *
     * @param control    the first thing to compare
     * @param experiment the second thing to compare
     * @param name       the name to put into the errors list when they don't match
     * @deprecated use {@link #dotEquals(Float, Float, Float, String)}
     */
    @Deprecated
    public void dotEquals(final Float control, final Float experiment, final String name) {
      throw new RuntimeException("BUG: will not dotEquals a Float without a delta");
    }

    /**
     * Compare floats.
     *
     * @param control    the first thing to compare
     * @param experiment the second thing to compare
     * @param delta      how close the values need to be
     * @param name       the name to put into the errors list when they don't match
     */
    public EqualChain dotEquals(final Float control, final Float experiment, final Float delta, final String name) {
      final float abs = Math.abs(control - experiment);
      return continueChain(abs < delta, name);
    }

    /**
     * Compare control and experiment using an IsEqual.
     *
     * @param control    the first thing to compare
     * @param experiment the second thing to compare
     * @param isEqual    an isEqual instance to do the comparison
     * @param name       the name to put into the errors list when they don't match
     * @return a new equal chain
     */
    public <T> EqualChain isEquals(
        final T control,
        final T experiment,
        final IsEqual<T> isEqual,
        final String name) {
      return continueChain(isEqual.apply(control, experiment), name);
    }

    /**
     * Perform a comparison on Comparables using compareTo.  Useful for things like BigDecimal.
     *
     * @param control    the first thing to compare
     * @param experiment the second thing to compare
     * @param name       the name to put into the errors list when they don't match
     * @return equal chain
     */
    public <T extends Comparable<T>> EqualChain compareTo(final T control, final T experiment, final String name) {
      if ((control == null) != (experiment == null)) {
        return continueChain(false, name);
      }
      if (control == null) {
        return continueChain(true, name);
      }
      return continueChain(control.compareTo(experiment) == 0, name);
    }

    /**
     * Retrieve the logically ANDed result of the comparisons.
     *
     * @return the result
     */
    public boolean get() {
      return result;
    }
  }

  /**
   * Create a map suitable for reporting IsEqual differences.
   *
   * @param control    The first object
   * @param experiment The second object
   * @param method     Generally, the name of the method calling the trial
   * @param name       Generally, a name to correlate to the IsEqual class.
   * @param mismatches A list of mismatches between the control and experiment
   * @return the map
   */
  public static Map<String, Object> makeReportMap(
      final Object control,
      final Object experiment,
      final String method,
      final String name,
      final List<String> mismatches) {
      final Map<String, Object> map = Maps.newHashMap();
    map.put("method", method);
    map.put("name", name);
    map.put("control", control);
    map.put("experiment", experiment);
    map.put("mismatches", mismatches);
    map.put("timestamp", new DateTime().toString());
    return map;
  }
}
