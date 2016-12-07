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

package com.workmarket.jan20.demo;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;

import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.Timer;

import java.util.HashMap;
import java.util.Map;

/**
 * Since we're not using a regular metric reporter, we make something we can use to get metric values.
 */
public class MetricListener implements MetricRegistryListener {
  private final Map<String, Meter> meters;

  MetricListener() {
    meters = new HashMap<>();
  }

  Map<String, Meter> getMeters() {
    return meters;
  }

  @Override
  public void onMeterAdded(final String name, final Meter meter) {
    meters.put(name, meter);
  }

  @Override
  public void onGaugeAdded(final String name, final Gauge<?> gauge) { }

  @Override
  public void onGaugeRemoved(final String name) { }

  @Override
  public void onCounterAdded(final String name, final Counter counter) { }

  @Override
  public void onCounterRemoved(final String name) { }

  @Override
  public void onHistogramAdded(final String name, final Histogram histogram) { }

  @Override
  public void onHistogramRemoved(final String name) { }

  @Override
  public void onMeterRemoved(final String name) { }

  @Override
  public void onTimerAdded(final String name, final Timer timer) { }

  @Override
  public void onTimerRemoved(final String name) { }
}
