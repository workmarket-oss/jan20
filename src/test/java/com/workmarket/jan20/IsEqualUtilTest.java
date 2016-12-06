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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.workmarket.jan20.IsEqualUtil.consumeToList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test IsEqualUtil.
 */
public class IsEqualUtilTest {

  /**
   * Test that chaining works as we expect.
   */
  @Test
  public void chaining() {
    final List<String> mismatches = Lists.newArrayList();
    assertFalse(IsEqualUtil
        .startCompare(consumeToList(mismatches))
        .doubleEquals(1, 1, "first")
        .doubleEquals(1, 2, "second")
        .dotEquals("foo", "foo", "third")
        .isEquals("foo", "FOO", IsEqual.useDotEquals(), "fourth")
        .compareTo(BigDecimal.valueOf(10L), BigDecimal.valueOf(10.1), "sixth")
        .compareTo(BigDecimal.valueOf(10L), BigDecimal.valueOf(10.0), "fifth")
        .doubleEquals(1.2, 1.1, 0.1, "seventh")
        .dotEquals(1.2, 1.1, 0.1, "eighth")
        .doubleEquals(Float.valueOf("1.2"), Float.valueOf("1.1"), Float.valueOf("0.2"), "9th")
        .dotEquals(Float.valueOf("1.2"), Float.valueOf("1.1"), Float.valueOf("0.2"), "10th")
        .get());
    assertEquals(ImmutableList.of("second", "fourth", "sixth"), mismatches);
  }

  /**
   * Test another case where everything matches.
   */
  @Test
  public void shouldBeTrue() {
    final List<String> mismatches = Lists.newArrayList();
    assertTrue(IsEqualUtil
        .startCompare(consumeToList(mismatches))
        .doubleEquals(1, 1, "first")
        .dotEquals("FOO", "FOO", "second")
        .get());

    assertTrue(mismatches.isEmpty());
  }

  /**
   * Test nullity pattern.
   */
  @Test
  public void nullityFailure() {
    final List<String> mismatches = Lists.newArrayList();
    IsEqualUtil.checkNullity(null, (Long) 1L, consumeToList(mismatches));
    assertEquals(1, mismatches.size());
    assertEquals("nullity", mismatches.get(0));
  }

  /**
   * Test nullity pattern.
   */
  @Test
  public void nullityOk() {
    final List<String> mismatches = Lists.newArrayList();
    IsEqualUtil.checkNullity(null, null, consumeToList(mismatches));
    assertEquals(0, mismatches.size());
    IsEqualUtil.checkNullity((Long) 1L, (Long) 1L, consumeToList(mismatches));
    assertEquals(0, mismatches.size());
  }
}
