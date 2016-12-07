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

import com.google.common.base.Joiner;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.workmarket.jan20.IsEqual;
import com.workmarket.jan20.IsEqualUtil;
import com.workmarket.jan20.Trial;
import com.workmarket.jan20.Trial.WhichReturn;
import com.workmarket.jan20.TrialResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import rx.Observable;

import static com.workmarket.jan20.IsEqualUtil.checkNullity;
import static com.workmarket.jan20.IsEqualUtil.startCompare;

/**
 * A simple demo showing how the various cases get triggered.
 */
public final class Demo {
  private final Trial trial;
  private final MetricListener listener;

  private static final IsEqual<Throwable> BOTH_OR_NEITHER_THROW = new IsEqual<Throwable>() {
    @Override
    public boolean apply(final Throwable control, final Throwable experiment) {
      final boolean result = ((control == null) == (experiment == null));
      if (!result) {
        System.out.println("BOTH_OR_NEITHER_THROW mismatch: control -> " + control + "  experiment -> " + experiment);
      }
      return result;
    }
  };

  private static final IsEqual<Boolean> BOOL_ISEQUAL = new IsEqual<Boolean>() {
    @Override
    public boolean apply(final Boolean control, final Boolean experiment) {
        final List<String> mismatches = new ArrayList<>();
        final IsEqualUtil.MismatchConsumer consumer = IsEqualUtil.consumeToList(
            mismatches);
        final boolean success = checkNullity(control, experiment, consumer)
            && startCompare(consumer)
            .dotEquals(control, experiment, "checkmatches")
            .get();
        if (success) {
          return true;
        }
        System.out.println("BOOL_ISEQUAL mismatch: " + Joiner.on(", ").join(mismatches)
            + "\ncontrol was " + control + " experiment was " + experiment);
        return false;
      };
  };

  private static final IsEqual<TrialResult<Boolean>> BOOL_TRIAL_ISEQUAL = Trial.makeIsEqual(
      BOTH_OR_NEITHER_THROW, BOOL_ISEQUAL.pairwiseEqual());

  private Demo() {
    final ThreadPoolExecutor experimentExecutor = new ThreadPoolExecutor(
        1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1));

    final MetricRegistry metricRegistry = new MetricRegistry();

    listener = new MetricListener();
    metricRegistry.addListener(listener);

    trial = new Trial(
        experimentExecutor,
        metricRegistry,
        "succession",
        WhichReturn.CONTROL);  // no wrappers, and a static choice of whichReturn
    run();
    experimentExecutor.shutdown();
  }

  private void run() {
    try {
      runSuccessfulTrial();
      logMetrics("successful");
      runMismatchTrial();
      logMetrics("mismatch");
      runMismatchOnException();
      logMetrics("mismatch-exc");
      runExceptionExperiment();
      logMetrics("exc-experiment");
      runExceptionControl();
      logMetrics("exc-control");
      runIsEqualKabooms();
      logMetrics("isequal-kaboom");
      runRejectedExperiment();
      logMetrics("rejected-experiment");
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  private void logMetrics(final String trialName) {
    System.out.println("\nMetrics for " + trialName);
    final ArrayList<Map.Entry<String, Meter>> entryList = new ArrayList<>(listener.getMeters().entrySet());
    entryList.sort(Comparator.comparing(Map.Entry::getKey));
    for (final Map.Entry<String, Meter> entry : entryList) {
      if (entry.getKey().contains(trialName)) {
        System.out.println(entry.getKey() + " : " + entry.getValue().getCount());
      }
    }
  }

  private void runSuccessfulTrial() throws Exception {
    final Callable<Observable<Boolean>> path = () -> Observable.just(true);
    trial.doTrial(path, path, BOOL_TRIAL_ISEQUAL, "successful").toBlocking().single();
  }

  private void runMismatchTrial() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final Callable<Observable<Boolean>> control = () -> Observable.just(true);
    final Callable<Observable<Boolean>> experiment = () -> Observable.create(
        subscriber -> {
          subscriber.onNext(false);
          subscriber.onCompleted();
          latch.countDown(); // or else the experiment might not have completed by the time the metrics are printed.
        });
    trial.doTrial(control, experiment, BOOL_TRIAL_ISEQUAL, "mismatch").toBlocking().single();

    latch.await();
  }

  private void runMismatchOnException() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final Callable<Observable<Boolean>> control = () -> Observable.just(true);
    final Callable<Observable<Boolean>> experiment = () -> Observable.create(
        subscriber -> {
          subscriber.onError(new RuntimeException("AIEEE"));
          subscriber.onCompleted();
          latch.countDown(); // or else the experiment might not have completed by the time the metrics are printed.
        });
    trial.doTrial(control, experiment, BOOL_TRIAL_ISEQUAL, "mismatch-exc").toBlocking().single();

    latch.await();
  }

  // That is, the callable that produces the Observable went bad.  The Observable may itself return an error, and
  // that's managed by the normal IsEqual<TrialResult<T>>.
  private void runExceptionExperiment() throws Exception {
    final Callable<Observable<Boolean>> control = () -> Observable.just(true);
    final Callable<Observable<Boolean>> experiment = () -> { throw new RuntimeException("FAIL!"); };
    trial.doTrial(control, experiment, BOOL_TRIAL_ISEQUAL, "exc-experiment").toBlocking().single();
  }

  // That is, the callable that produces the Observable went bad.  The Observable may itself return an error, and
  // that's managed by the normal IsEqual<TrialResult<T>>.
  private void runExceptionControl() throws Exception {
    final Callable<Observable<Boolean>> control = () -> { throw new RuntimeException("FAIL!"); };
    final Callable<Observable<Boolean>> experiment = () -> Observable.just(true);
    try {
      trial.doTrial(control, experiment, BOOL_TRIAL_ISEQUAL, "exc-control").toBlocking().single();
    } catch (final RuntimeException e) {
      // expected
    }
  }

  // Yup, sometimes you'll have bugs in your IsEqual, and you'll want a way to know.
  private void runIsEqualKabooms() throws Exception {
    final Callable<Observable<Boolean>> path = () -> Observable.just(true);
    trial.doTrial(path, path, new IsEqual<TrialResult<Boolean>>() {
      @Override
      public boolean apply(final TrialResult<Boolean> control, final TrialResult<Boolean> experiment) {
        throw new RuntimeException("Kaboom!");
      }
    }, "isequal-kaboom");
    // Note: only the IsEqual failed, the value of the control is still returned here.
  }

  // Rejected control is the same thing, just reversed when whichReturn=EXPERIMENT
  private void runRejectedExperiment() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final Callable<Observable<Boolean>> control = () -> Observable.just(true);
    final Callable<Observable<Boolean>> experiment = () -> Observable.create(
        subscriber -> {
          try {
            latch.await();
          } catch (final InterruptedException e) {
            throw new RuntimeException(e); // shouldn't actually happen in a demo thing like this
          }
          subscriber.onNext(true);
          subscriber.onCompleted();
          latch.countDown(); // or else the experiment might not have completed by the time the metrics are printed.
        });

    // Do a trial to gum up the works
    trial.doTrial(control, experiment, BOOL_TRIAL_ISEQUAL, "rejected-experiment").toBlocking().single();
    // Put one into the queue
    trial.doTrial(control, control, BOOL_TRIAL_ISEQUAL, "rejected-experiment").toBlocking().single();
    // This one's experiment will be rejected from the thread pool
    trial.doTrial(control, control, BOOL_TRIAL_ISEQUAL, "rejected-experiment").toBlocking().single();

    latch.countDown();
  }

  public static void main(final String[] ignored) throws Exception {
    new Demo();
  }
}
