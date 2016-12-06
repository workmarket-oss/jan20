/*
 * Copyright 2016, WorkMarket, Inc. All Rights Reserved.
 */
package com.workmarket.jan20;

/**
 * A consumer interface for trial results.
 *
 * @param <T> The type arg of the observables in question.
 */
interface ResultConsumer<T> {
  /**
   * Consume the trial result and does something with it.
   */
  void consume(TrialResult<T> result);
}
