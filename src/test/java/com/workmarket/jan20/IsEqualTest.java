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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import org.junit.Test;

import static com.workmarket.jan20.IsEqual.alwaysFalse;
import static com.workmarket.jan20.IsEqual.alwaysTrue;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks that IsEquals works.
 */
public class IsEqualTest {
  private static final ImmutableList<Boolean> TRUE_FALSE = ImmutableList.of(TRUE, FALSE);
  private static final ImmutableList<Boolean> TRUE_TRUE = ImmutableList.of(TRUE, TRUE);
  private static final IsEqual<Object> KABOOM_EQUALS = new IsEqual<Object>() {
    @Override
    public boolean apply(final Object control, final Object experiment) {
      throw new RuntimeException();
    }
  };

  private static final Item ITEM_1 = new Item("foo", "bar");
  private static final Item ITEM_2 = new Item("foo", "baz");

  private static final class Item {
    private final String key;
    @SuppressWarnings("unused") // mostly to make sure that identity isn't being used for .isEqual
    private final String value;

    private Item(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }

  /**
   * Is alwaysTrue *always* true?
   */
  @Test
  public void testTrue() {
    assertEquals(TRUE, alwaysTrue().apply(FALSE, FALSE));
    assertEquals(TRUE, alwaysTrue().apply(FALSE, TRUE));
    assertEquals(TRUE, alwaysTrue().apply(TRUE, FALSE));
    assertEquals(TRUE, alwaysTrue().apply(TRUE, TRUE));
  }

  /**
   * Is alwaysFalse *always* false?
   */
  @Test
  public void testFalse() {
    assertEquals(FALSE, alwaysFalse().apply(FALSE, FALSE));
    assertEquals(FALSE, alwaysFalse().apply(FALSE, TRUE));
    assertEquals(FALSE, alwaysFalse().apply(TRUE, FALSE));
    assertEquals(FALSE, alwaysFalse().apply(TRUE, TRUE));
  }

  /**
   * Check that or works.
   */
  @Test
  public void testOr() {
    assertFalse(alwaysFalse().or(alwaysFalse()).apply(TRUE, TRUE));
    assertTrue(alwaysFalse().or(alwaysTrue()).apply(TRUE, TRUE));
    assertTrue(alwaysTrue().or(alwaysFalse()).apply(TRUE, TRUE));
    assertTrue(alwaysTrue().or(alwaysTrue()).apply(TRUE, TRUE));
  }

  /**
   * Check that and works.
   */
  @Test
  public void testAnd() {
    assertFalse(alwaysFalse().and(alwaysFalse()).apply(TRUE, TRUE));
    assertFalse(alwaysFalse().and(alwaysTrue()).apply(TRUE, TRUE));
    assertFalse(alwaysTrue().and(alwaysFalse()).apply(TRUE, TRUE));
    assertTrue(alwaysTrue().and(alwaysTrue()).apply(TRUE, TRUE));
  }

  /**
   * Test multi-argument and and short circuiting.
   */
  @Test
  public void variadicAndShortCircuit() {
    assertTrue(alwaysTrue().and().apply(TRUE, TRUE));
    assertFalse(alwaysFalse().and().apply(TRUE, TRUE));

    assertFalse(alwaysFalse().and(KABOOM_EQUALS).apply(TRUE, TRUE));
    assertFalse(alwaysTrue().and(alwaysFalse(), KABOOM_EQUALS).apply(TRUE, TRUE));
    assertFalse(alwaysTrue().and(alwaysTrue(), alwaysFalse(), KABOOM_EQUALS).apply(TRUE, TRUE));
    assertTrue(alwaysTrue().and(alwaysTrue(), alwaysTrue()).apply(TRUE, TRUE));
  }

  /**
   * Test multi-argument or and short circuiting.
   */
  @Test
  public void variadicOrShortCircuit() {
    assertTrue(alwaysTrue().or().apply(TRUE, TRUE));
    assertFalse(alwaysFalse().or().apply(TRUE, TRUE));

    assertTrue(alwaysTrue().or(KABOOM_EQUALS).apply(TRUE, TRUE));
    assertTrue(alwaysFalse().or(alwaysTrue(), KABOOM_EQUALS).apply(TRUE, TRUE));
    assertTrue(alwaysFalse().or(alwaysFalse(), alwaysTrue(), KABOOM_EQUALS).apply(TRUE, TRUE));
    assertFalse(alwaysFalse().or(alwaysFalse(), alwaysFalse()).apply(TRUE, TRUE));
  }

  /**
   * Test that negation negates.
   */
  @Test
  public void testNot() {
    assertFalse(alwaysTrue().negate().apply(TRUE, TRUE));
    assertTrue(alwaysFalse().negate().apply(TRUE, TRUE));
  }

  /**
   * Test that null checking bits work.
   */
  @Test
  public void testNullEquality() {
    final IsEqual<Boolean> kaboomOnNull = new IsEqual<Boolean>() {
      @Override
      public boolean apply(final Boolean control, final Boolean experiment) {
        assertNotNull(control);
        assertNotNull(experiment);
        return control.equals(experiment);
      }
    };

    assertFalse(kaboomOnNull.checkNullEquality().apply(FALSE, TRUE));
    assertTrue(kaboomOnNull.checkNullEquality().apply(TRUE, TRUE));
    assertFalse(kaboomOnNull.checkNullEquality().apply(TRUE, null));
    assertFalse(kaboomOnNull.checkNullEquality().apply(null, TRUE));
    assertTrue(kaboomOnNull.checkNullEquality().apply(null, null));
  }

  /**
   * Test that pairwise equality checking works.
   */
  @Test
  public void testPairwiseEquality() {
    final IsEqual<Iterable<Boolean>> equal = IsEqual.<Boolean>useDotEquals().pairwiseEqual();

    // check with an empty iterable
    assertFalse(equal.apply(ImmutableList.<Boolean>of(), TRUE_TRUE));
    assertFalse(equal.apply(ImmutableList.of(TRUE), ImmutableList.<Boolean>of()));

    // check with different sizes
    assertFalse(equal.apply(ImmutableList.of(TRUE), TRUE_TRUE));
    assertFalse(equal.apply(TRUE_TRUE, ImmutableList.of(TRUE)));

    // check it actually compares
    assertTrue(equal.apply(TRUE_TRUE, TRUE_TRUE));
    assertFalse(equal.apply(TRUE_TRUE, TRUE_FALSE));
  }

  /**
   * Test that using dotEquals works.
   */
  @Test
  public void testDotEquals() {
    final IsEqual<Integer> equal = IsEqual.<Integer>useDotEquals();
    assertFalse(equal.apply(1, 2));
    assertTrue(equal.apply(1, 1));
    assertFalse(equal.apply(2, 1));
  }

  /**
   * Test onExtractionOf.
   */
  @Test
  public void testOnExtractionOf() {
    final IsEqual<Item> equals = IsEqual
        .<String>useDotEquals()
        .onExtractionOf(new Function<Item, String>() {
          @Override
          public String apply(final Item input) {
            return input.key;
          }
        });

    assertTrue(equals.apply(ITEM_1, ITEM_2));
  }

  /**
   * Test given a Comparator.
   */
  @Test
  public void comparatorUse() {
    final IsEqual<Boolean> eq = IsEqual.useComparator(Ordering.natural());
    assertTrue(eq.apply(TRUE, TRUE));
    assertFalse(eq.apply(TRUE, FALSE));
  }

  /**
   * Test ==.
   */
  @Test
  public void testDoubleEquals() {
    assertTrue(IsEqual.useDoubleEquals().apply(ITEM_1, ITEM_1));
    assertFalse(IsEqual.useDoubleEquals().apply(ITEM_1, ITEM_2));
  }

  /**
   * Test that instantiating from a lambda works.  Or what a lambda would be if we were in java 8.
   */
  @Test
  public void testFromLambda() {
    final IsEqual<Item> eq = IsEqual.use(
        new IsEqualPredicate<Item>() {
          @Override
          public boolean apply(final Item control, final Item experiment) {
            return control == experiment;
          }
        });
    assertTrue(eq.apply(ITEM_1, ITEM_1));
    assertFalse(eq.apply(ITEM_1, ITEM_2));
  }
}
