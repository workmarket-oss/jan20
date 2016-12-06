/*
 * Copyright 2016, WorkMarket, Inc. All Rights Reserved.
 */
package com.workmarket.jan20;

/**
 * An exception thrown when duplicate values are encountered.
 */
public class DuplicateValueException extends RuntimeException {
  /**
   * Create with a message.
   */
  public DuplicateValueException(final String message) {
    super(message);
  }
}
