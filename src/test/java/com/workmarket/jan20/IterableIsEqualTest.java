/*
 * Copyright 2016, WorkMarket, Inc. All Rights Reserved.
 */
package com.workmarket.jan20;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Testing IterableIsEqual.
 */
public class IterableIsEqualTest {
  private static final List<Integer> LIST_1 = ImmutableList.of(1, 2, 3);
  private static final List<Integer> LIST_2 = ImmutableList.of(2, 3, 1);
  private static final List<Integer> LIST_3 = ImmutableList.of(2, 3);
  private static final List<Integer> WITH_DUPS1 = ImmutableList.of(1, 3, 1, 2, 1);
  private static final List<Integer> WITH_DUPS2 = ImmutableList.of(3, 2, 1, 1, 1);
  private static final IterableIsEqual<Integer> BASE_INT_EQUAL = IsEqual.<Integer>useDoubleEquals().pairwiseEqual();

  /**
   * Test upconversion.
   */
  @Test
  public void fromIsEq() {
    final IsEqual<Iterable<Integer>> iteq = IsEqual.<Iterable<Integer>>useDoubleEquals();
    final IterableIsEqual<Integer> iie = IterableIsEqual.<Integer>fromIsEqualIterable(iteq);

    assertThat(iie, not(sameInstance(iteq)));
    assertThat(iie, sameInstance(IterableIsEqual.fromIsEqualIterable(iie)));
  }

  /**
   * Test that sorting on pairwise works.
   */
  @Test
  public void testSort() {
    // check failure without ordering
    assertFalse(BASE_INT_EQUAL.apply(LIST_1, LIST_2));
    // order the lists before comparing
    final IterableIsEqual<Integer> sorted = BASE_INT_EQUAL.correlateByOrdering(Ordering.natural());
    assertTrue(sorted.apply(LIST_1, LIST_2));
    assertTrue(sorted.apply(WITH_DUPS1, WITH_DUPS2));

    assertFalse(sorted.apply(LIST_1, LIST_3));
    assertFalse(sorted.apply(LIST_3, LIST_1));

    assertTrue(
        new IterableIsEqual<Integer>() {
          @Override
          public boolean apply(final Iterable<Integer> control, final Iterable<Integer> experiment) {
            return control.equals(LIST_1) && experiment.equals(LIST_1)
                && (control != LIST_1 && experiment != LIST_1);
          }
        }
            .correlateByOrdering(Ordering.natural())
            .apply(LIST_1, LIST_2));
  }

  /**
   * Test that keying stuff works.
   */
  @Test
  public void testCorrelateByKey() {
    final IterableIsEqual<Integer> correlateByKey = BASE_INT_EQUAL.correlateByKey(Functions.identity());
    assertTrue(correlateByKey.apply(LIST_1, LIST_2));
    assertFalse(correlateByKey.apply(LIST_3, LIST_1));
    assertFalse(correlateByKey.apply(LIST_1, LIST_3));

    try {
      correlateByKey.apply(WITH_DUPS1, LIST_1);
      fail("Should throw");
    } catch (final DuplicateValueException e) {
      assertThat(e.getMessage(), containsString("Left"));
    }

    try {
      correlateByKey.apply(LIST_1, WITH_DUPS1);
      fail("Should throw");
    } catch (final DuplicateValueException e) {
      assertThat(e.getMessage(), containsString("Right"));
    }
    assertTrue(
        new IterableIsEqual<Integer>() {
          @Override
          public boolean apply(final Iterable<Integer> control, final Iterable<Integer> experiment) {
            assertEquals(LIST_1, Ordering.natural().sortedCopy(control));
            assertEquals(LIST_1, Ordering.natural().sortedCopy(experiment));
            return (control != LIST_1 && experiment != LIST_1);
          }
        }
            .correlateByKey(Functions.identity())
            .apply(LIST_1, LIST_2));
  }
}
