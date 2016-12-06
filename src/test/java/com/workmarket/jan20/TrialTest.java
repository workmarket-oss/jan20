/*
 * Copyright 2016, WorkMarket, Inc. All Rights Reserved.
 */
package com.workmarket.jan20;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Callables;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.workmarket.jan20.Trial.WhichReturn;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action1;

import static com.workmarket.jan20.Trial.IDENTITY_WRAPPER;
import static com.workmarket.jan20.Trial.WhichReturn.CONTROL;
import static com.workmarket.jan20.Trial.WhichReturn.CONTROL_ONLY;
import static com.workmarket.jan20.Trial.WhichReturn.EXPERIMENT;
import static com.workmarket.jan20.Trial.WhichReturn.EXPERIMENT_ONLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test trial.
 * <p>
 * The ReturningExperiment variants pass EXPERIMENT and the counter checks should be the same as for the
 * non ReturningExperiment variant.
 */
@RunWith(MockitoJUnitRunner.class)
public final class TrialTest {

  private static final Integer ONE = 1;
  private static final Integer TWO = 2;

  private static final int WAIT_TIMEOUT = 1000;

  private static final String EXCEPTION_MESSAGE = "AIEEE";

  private static final ImmutableList<Integer> LIST_1_2 = ImmutableList.of(ONE, TWO);
  private static final ImmutableList<Integer> JUST_2 = ImmutableList.of(TWO);

  private static final Callable<Observable<Integer>> MAKE_1_2 = Callables.returning(Observable.from(LIST_1_2));
  private static final Callable<Observable<Integer>> MAKE_JUST_2 = Callables.returning(Observable.from(JUST_2));

  private static final Callable<Observable<Integer>> FAIL_TO_LAUNCH = new Callable<Observable<Integer>>() {
    @Override
    public Observable<Integer> call() throws Exception {
      throw new RuntimeException(EXCEPTION_MESSAGE);
    }
  };

  private static final IsEqual<TrialResult<Integer>> OK_AND_EQUAL = Trial.makeIsEqual(
      IsEqual.<Throwable>alwaysTrue().checkNullEquality(), IsEqual.<Iterable<Integer>>useDotEquals());

  private static final String ROOT = "root";

  @Mock
  private Meter attempts;
  @Mock
  private Meter successes;
  @Mock
  private Meter experimentFailures;
  @Mock
  private Meter controlFailures;
  @Mock
  private Meter compareFailures;
  @Mock
  private Meter rejectedControlFailures;
  @Mock
  private Meter rejectedExperimentFailures;
  @Mock
  private Meter isEqualFailures;
  @Mock
  private Timer controlTimer;
  @Mock
  private Timer experimentTimer;
  @Mock
  private Context controlContext;
  @Mock
  private Context experimentContext;
  @Mock
  private MetricRegistry registry;

  private ExecutorService executor;
  private Trial controlTrial;
  private Trial experimentTrial;

  /**
   * Set up things.
   */
  @Before
  public void setUp() {
    when(registry.meter("root.attempts")).thenReturn(attempts);
    when(registry.meter("root.isEqualFailures")).thenReturn(isEqualFailures);
    when(registry.meter("root.successes")).thenReturn(successes);
    when(registry.meter("root.experimentFailures")).thenReturn(experimentFailures);
    when(registry.meter("root.controlFailures")).thenReturn(controlFailures);
    when(registry.meter("root.compareFailures")).thenReturn(compareFailures);
    when(registry.meter("root.rejectedControlFailures"))
        .thenReturn(rejectedControlFailures);
    when(registry.meter("root.rejectedExperimentFailures"))
        .thenReturn(rejectedExperimentFailures);
    when(registry.timer("root.controlTimer")).thenReturn(controlTimer);
    when(registry.timer("root.experimentTimer")).thenReturn(experimentTimer);
    when(controlTimer.time()).thenReturn(controlContext);
    when(experimentTimer.time()).thenReturn(experimentContext);

    executor = new ThreadPoolExecutor(
        1, 1, 0, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>(1));
    controlTrial = new Trial(executor, registry, "", CONTROL);
    experimentTrial = new Trial(executor, registry, "", EXPERIMENT);
  }

  /**
   * Tests what happens when everything works.
   *
   * @throws Exception
   */
  @Test
  public void testAllOk() throws Exception {
    assertIsOneTwo(controlTrial.doTrial(MAKE_1_2, MAKE_1_2, OK_AND_EQUAL, ROOT));
    verify(attempts).mark();
    verify(successes, timeout(WAIT_TIMEOUT)).mark();

    verify(compareFailures, times(0)).mark();
    verify(controlFailures, times(0)).mark();
    verify(experimentFailures, times(0)).mark();
    verify(rejectedExperimentFailures, times(0)).mark();
    verify(rejectedControlFailures, times(0)).mark();
    verifyTimersClosed();
  }

  /**
   * Tests what happens when everything works.
   *
   * @throws Exception
   */
  @Test
  public void testAllOkReturningExperiment() throws Exception {
    assertIsOneTwo(experimentTrial.doTrial(MAKE_1_2, MAKE_1_2, OK_AND_EQUAL, ROOT));

    verify(attempts).mark();
    verify(successes, timeout(WAIT_TIMEOUT)).mark();

    verify(compareFailures, times(0)).mark();
    verify(controlFailures, times(0)).mark();
    verify(experimentFailures, times(0)).mark();
    verify(rejectedExperimentFailures, times(0)).mark();
    verify(rejectedControlFailures, times(0)).mark();
    verifyTimersClosed();
  }

  /**
   * Test when everything works, but the values are not equal.
   */
  @Test
  public void testNotEqual() throws Exception {
    assertIsOneTwo(controlTrial.doTrial(MAKE_1_2, MAKE_JUST_2, OK_AND_EQUAL, ROOT));
    verify(attempts).mark();
    verify(compareFailures, timeout(WAIT_TIMEOUT)).mark();

    verify(successes, times(0)).mark();
    verify(controlFailures, times(0)).mark();
    verify(experimentFailures, times(0)).mark();
    verify(rejectedExperimentFailures, times(0)).mark();
    verify(rejectedControlFailures, times(0)).mark();
    verifyTimersClosed();
  }

  /**
   * Test when everything works, but the values are not equal.
   */
  @Test
  public void testNotEqualReturningExperiment() throws Exception {
    final Observable<Integer> result = experimentTrial.doTrial(MAKE_1_2, MAKE_JUST_2, OK_AND_EQUAL, ROOT);
    final ImmutableList<Integer> results = ImmutableList.copyOf(result.toBlocking().getIterator());
    assertEquals(JUST_2, results);
    verify(attempts).mark();
    verify(compareFailures, timeout(WAIT_TIMEOUT)).mark();

    verify(successes, times(0)).mark();
    verify(controlFailures, times(0)).mark();
    verify(experimentFailures, times(0)).mark();
    verify(rejectedExperimentFailures, times(0)).mark();
    verify(rejectedControlFailures, times(0)).mark();
    verifyTimersClosed();
  }

  /**
   * Test when the control experiment never gets off the ground.
   */
  @Test
  public void testControlBlowsException() throws Exception {
    try {
      controlTrial.doTrial(FAIL_TO_LAUNCH, MAKE_1_2, OK_AND_EQUAL, ROOT);
      fail("Should throw");
    } catch (final RuntimeException e) {
      assertEquals(EXCEPTION_MESSAGE, e.getMessage());
    }

    verify(attempts).mark();
    verify(controlFailures).mark();

    verify(successes, times(0)).mark();
    verify(experimentFailures, times(0)).mark();
    verify(compareFailures, times(0)).mark();
    verify(rejectedExperimentFailures, times(0)).mark();
    verify(rejectedControlFailures, times(0)).mark();
    verifyTimersClosed();
  }

  /**
   * Test when the control experiment never gets off the ground.
   */
  @Test
  public void testControlBlowsExceptionReturningExperiment() throws Exception {
    assertIsOneTwo(experimentTrial.doTrial(FAIL_TO_LAUNCH, MAKE_1_2, OK_AND_EQUAL, ROOT));

    verify(attempts).mark();
    verify(controlFailures, timeout(WAIT_TIMEOUT)).mark();

    verify(successes, times(0)).mark();
    verify(experimentFailures, times(0)).mark();
    verify(compareFailures, times(0)).mark();
    verify(rejectedExperimentFailures, times(0)).mark();
    verify(rejectedControlFailures, times(0)).mark();
    verifyTimersClosed();
  }

  /**
   * Test when the experiment never gets off the ground.
   */
  @Test
  public void testExperimentBlowsException() throws Exception {
    assertIsOneTwo(controlTrial.doTrial(MAKE_1_2, FAIL_TO_LAUNCH, OK_AND_EQUAL, ROOT));

    verify(attempts).mark();
    verify(experimentFailures, timeout(WAIT_TIMEOUT)).mark();

    verify(successes, times(0)).mark();
    verify(controlFailures, times(0)).mark();
    verify(compareFailures, times(0)).mark();
    verify(rejectedExperimentFailures, times(0)).mark();
    verify(rejectedControlFailures, times(0)).mark();
    verifyTimersClosed();
  }

  /**
   * Test when the experiment never gets off the ground.
   */
  @Test
  public void testExperimentBlowsExceptionReturningExperiment() throws Exception {
    try {
      experimentTrial.doTrial(MAKE_1_2, FAIL_TO_LAUNCH, OK_AND_EQUAL, ROOT);
      fail("Should throw");
    } catch (final RuntimeException e) {
      assertEquals(EXCEPTION_MESSAGE, e.getMessage());
    }

    verify(attempts).mark();
    verify(experimentFailures, timeout(WAIT_TIMEOUT)).mark();

    verify(successes, times(0)).mark();
    verify(controlFailures, times(0)).mark();
    verify(compareFailures, times(0)).mark();
    verify(rejectedExperimentFailures, times(0)).mark();
    verify(rejectedControlFailures, times(0)).mark();
    verifyTimersClosed();
  }

  /**
   * Test when both control and experiment never leave the launch pad.
   */
  @Test
  public void testBothExperimentsDie() throws Exception {
    try {
      controlTrial.doTrial(FAIL_TO_LAUNCH, FAIL_TO_LAUNCH, OK_AND_EQUAL, ROOT);
      fail("Should throw");
    } catch (final RuntimeException e) {
      assertEquals(EXCEPTION_MESSAGE, e.getMessage());
    }

    verify(attempts).mark();
    verify(controlFailures).mark();
    verify(experimentFailures, timeout(WAIT_TIMEOUT)).mark();

    verify(successes, times(0)).mark();
    verify(compareFailures, times(0)).mark();
    verify(rejectedExperimentFailures, times(0)).mark();
    verify(rejectedControlFailures, times(0)).mark();
    verifyTimersClosed();
  }

  /**
   * Test when both control and experiment never leave the launch pad.
   */
  @Test
  public void testBothExperimentsDieReturningExperiment() throws Exception {
    try {
      experimentTrial.doTrial(FAIL_TO_LAUNCH, FAIL_TO_LAUNCH, OK_AND_EQUAL, ROOT);
      fail("Should throw");
    } catch (final RuntimeException e) {
      assertEquals(EXCEPTION_MESSAGE, e.getMessage());
    }

    verify(attempts).mark();
    verify(controlFailures, timeout(WAIT_TIMEOUT)).mark();
    verify(experimentFailures, timeout(WAIT_TIMEOUT)).mark();

    verify(successes, times(0)).mark();
    verify(compareFailures, times(0)).mark();
    verify(rejectedExperimentFailures, times(0)).mark();
    verify(rejectedControlFailures, times(0)).mark();
    verifyTimersClosed();
  }

  /**
   * Overwhelm our pool and queue of 1 and make sure we don't block.
   */
  @Test
  public void testExecutorThreadPoolExhaustion() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    runExhaustionTest(controlTrial, rejectedExperimentFailures, rejectedControlFailures, latch,
        MAKE_1_2, makeBlocking12(latch));

    verify(controlContext, times(3)).close();
    verify(experimentContext, times(2)).close();
  }

  /**
   * Overwhelm our pool and queue of 1 and make sure we don't block.
   */
  @Test
  public void testExecutorThreadPoolExhaustionReturningExperiment() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    runExhaustionTest(experimentTrial, rejectedControlFailures, rejectedExperimentFailures, latch,
        makeBlocking12(latch), MAKE_1_2);

    verify(controlContext, times(2)).close();
    verify(experimentContext, times(3)).close();
  }

  private void assertIsOneTwo(final Observable<Integer> result) {
    assertEquals(LIST_1_2, ImmutableList.copyOf(result.toBlocking().getIterator()));
  }

  private void verifyTimersClosed() {
    verify(controlContext, timeout(WAIT_TIMEOUT)).close();
    verify(experimentContext, timeout(WAIT_TIMEOUT)).close();
  }

  /**
   * A delayed callable that blocks until the latch finishes, and then returns 1,2.
   *
   * @param latch The latch to wait on.
   */
  private Callable<Observable<Integer>> makeBlocking12(final CountDownLatch latch) {
    return new Callable<Observable<Integer>>() {
      @Override
      public Observable<Integer> call() throws Exception {
        latch.await();
        return Observable.from(LIST_1_2);
      }
    };
  }

  private void runExhaustionTest(final Trial trial,
                                 final Meter compareValueRejectionCounter,
                                 final Meter returnValueRejectionCounter,
                                 final CountDownLatch latch,
                                 final Callable<Observable<Integer>> control,
                                 final Callable<Observable<Integer>> experiment) throws Exception {
    // First two will clog the pool and queue, the third should trigger a rejection by the executor.
    assertIsOneTwo(trial.doTrial(control, experiment, OK_AND_EQUAL, ROOT));
    assertIsOneTwo(trial.doTrial(control, experiment, OK_AND_EQUAL, ROOT));
    assertIsOneTwo(trial.doTrial(control, experiment, OK_AND_EQUAL, ROOT));

    verify(attempts, times(3)).mark();

    // except for the rejected execution
    verify(compareValueRejectionCounter).mark();

    // nothing else happened at this point
    verify(successes, times(0)).mark();
    verify(controlFailures, times(0)).mark();
    verify(experimentFailures, times(0)).mark();
    verify(compareFailures, times(0)).mark();
    verify(returnValueRejectionCounter, times(0)).mark();

    // Release the Kraken!
    latch.countDown();

    verify(successes, new Timeout(WAIT_TIMEOUT, times(2))).mark();
    // Two did succeed, but nothing else happened
    verify(controlFailures, times(0)).mark();
    verify(experimentFailures, times(0)).mark();
    verify(compareFailures, times(0)).mark();
    verify(returnValueRejectionCounter, times(0)).mark();
  }

  /**
   * Test trial's makeIsEqual.
   */
  @Test
  public void testMakeIsEqual() {
    final IsEqual<Throwable> throwable = IsEqual.<Throwable>alwaysTrue().checkNullEquality();
    final IsEqual<Iterable<Integer>> ints = IsEqual.<Integer>useDotEquals().checkNullEquality().pairwiseEqual();

    final IsEqual<TrialResult<Integer>> trialEq = Trial.makeIsEqual(throwable, ints);
    final List<Integer> listWithNull = new ArrayList<Integer>();
    listWithNull.add(null);
    final TrialResult<Integer> nullTrial = new TrialResult<Integer>(listWithNull, null);
    final ImmutableList<Integer> listWithOne = ImmutableList.of(1);
    final TrialResult<Integer> one = new TrialResult<Integer>(listWithOne, null);
    final TrialResult<Integer> two = new TrialResult<Integer>(ImmutableList.of(2), null);

    assertTrue(trialEq.apply(one, one));
    assertFalse(trialEq.apply(one, two));
    assertFalse(trialEq.apply(two, one));
    assertFalse(trialEq.apply(two, nullTrial));
    assertFalse(trialEq.apply(nullTrial, one));
    assertTrue(trialEq.apply(nullTrial, nullTrial));

    final Throwable exc = new RuntimeException();
    assertFalse(trialEq.apply(
        new TrialResult<Integer>(listWithNull, exc),
        new TrialResult<Integer>(listWithNull, null)));

    assertTrue(trialEq.apply(
        new TrialResult<Integer>(listWithNull, exc),
        new TrialResult<Integer>(listWithNull, exc)));

    assertFalse(trialEq.apply(
        new TrialResult<Integer>(listWithNull, null),
        new TrialResult<Integer>(listWithNull, exc)));
  }

  /**
   * Test that we don't subscribe, etc. more than once to the underlying observable.
   *
   * @throws Exception
   */
  @Test
  public void preservingSemanticsOfUnderlyingObservable() throws Exception {
    final AtomicInteger subscriptionCounter = new AtomicInteger(0);
    // Count underlying subscriptions
    final Observable<Integer> count = Observable.create(new OnSubscribe<Integer>() {
      @Override
      public void call(final Subscriber<? super Integer> subscriber) {
        subscriptionCounter.incrementAndGet();
        subscriber.onCompleted();
      }
    });

    // don't care about actual equality of results, we're just testing observable semantics.
    final Observable<Integer> result = controlTrial.doTrial(
        Callables.returning(count), MAKE_1_2, IsEqual.<TrialResult<Integer>>alwaysTrue(), ROOT);

    final Action1<Integer> subscriber = new Action1<Integer>() {
      @Override
      public void call(final Integer value) { }
    };
    result.subscribe(subscriber);
    // Underlying observable should not be subscribed to twice.
    assertEquals(1, subscriptionCounter.get());
  }

  /**
   * Test wrapping.
   */
  @Test
  public void checkWrapping() throws Exception {
    final ThreadLocal<Integer> local = new ThreadLocal<Integer>();
    local.set(ONE);
    final CallableObservableWrapper setThreadLocal = new CallableObservableWrapper() {
      @Override
      public <T> Callable<Observable<T>> wrap(final Callable<Observable<T>> item) {
        return Callables.returning(Observable.create(new OnSubscribe<T>() {
          @Override
          public void call(final Subscriber<? super T> subscriber) {
            try {
              local.set(TWO);
              item.call().subscribe(subscriber);
            } catch (final Throwable t) {
              subscriber.onError(t);
            } finally {
              local.set(null);
            }
          }
        }));
      }
    };

    final Callable<Observable<Integer>> getFromThreadLocal = Callables.returning(Observable.create(
        new OnSubscribe<Integer>() {
          @Override
          public void call(final Subscriber<? super Integer> subscriber) {
            subscriber.onNext(local.get());
            subscriber.onCompleted();
          }
        }));

    // check no wrapping
    // Should not match, and should return 1, the experiment should see NULL
    final Observable<Integer> rv1 = runWrappedTrial(local,
        IDENTITY_WRAPPER, IDENTITY_WRAPPER, getFromThreadLocal, getFromThreadLocal);
    assertEquals(ONE, rv1.toBlocking().single());
    assertEquals(ONE, local.get());
    verify(successes, times(0)).mark();

    // Test control wrapper
    // Should not match, and return 2, experiment sees null
    final Observable<Integer> rv3 = runWrappedTrial(local,
        setThreadLocal, IDENTITY_WRAPPER, MAKE_JUST_2, getFromThreadLocal);
    assertEquals(TWO, rv3.toBlocking().single());
    assertNull(local.get());
    verify(successes, times(0)).mark();

    // Test experiment wrapper
    // Should match, and return 2
    final Observable<Integer> rv2 = runWrappedTrial(local,
        IDENTITY_WRAPPER, setThreadLocal, MAKE_JUST_2, getFromThreadLocal);
    assertEquals(TWO, rv2.toBlocking().single());
    assertEquals(ONE, local.get());
    verify(successes, timeout(WAIT_TIMEOUT)).mark();

    // Test both wrappers
    // Should match as both will pull 2 from the thread local
    final Observable<Integer> rv4 = runWrappedTrial(local,
        setThreadLocal, setThreadLocal, getFromThreadLocal, getFromThreadLocal);
    assertEquals(TWO, rv4.toBlocking().single());
    assertNull(local.get());
    verify(successes, timeout(WAIT_TIMEOUT)).mark();
  }

  private Observable<Integer> runWrappedTrial(final ThreadLocal<Integer> local,
                                              final CallableObservableWrapper controlWrapper,
                                              final CallableObservableWrapper experimentWrapper,
                                              final Callable<Observable<Integer>> control,
                                              final Callable<Observable<Integer>> experiment) throws Exception {
    reset(successes);
    local.set(ONE);
    // All this machination is to make *sure* by the time everything is looked at, that all background threads
    // have done everything.
    final ThreadPoolExecutor wrappedExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>(1));
    final Observable<Integer> rv = new Trial(wrappedExecutor, registry, "", CONTROL,
        controlWrapper, experimentWrapper)
        .doTrial(control, experiment, OK_AND_EQUAL, ROOT);
    rv.subscribe(); // triggers execution if it hadn't
    wrappedExecutor.shutdown();
    assertTrue(wrappedExecutor.awaitTermination(1, TimeUnit.SECONDS));
    return rv;
  }

  /**
   * Make sure control only trials don't run the experiment.
   */
  @Test
  public void doControlOnlyExperiment() throws Exception {
    final Trial trial = new Trial(executor, registry, "", CONTROL_ONLY);
    final CountingNoOpOnSubscribe shouldNotRun = new CountingNoOpOnSubscribe();

    final Observable<Integer> result = trial.doTrial(
        MAKE_1_2, shouldNotRun, IsEqual.<TrialResult<Integer>>alwaysTrue(), ROOT);
    result.toList().toBlocking().first();
    executor.shutdown();
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    assertEquals(0, shouldNotRun.getCount());
  }

  /**
   * Make sure control only trials don't run the experiment.
   */
  @Test
  public void doExperimentOnlyExperiment() throws Exception {
    final Trial trial = new Trial(executor, registry, "", EXPERIMENT_ONLY);
    final CountingNoOpOnSubscribe shouldNotRun = new CountingNoOpOnSubscribe();

    final Observable<Integer> result = trial.doTrial(
        shouldNotRun, MAKE_1_2, IsEqual.<TrialResult<Integer>>alwaysTrue(), ROOT);
    result.toList().toBlocking().first();
    executor.shutdown();
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    assertEquals(0, shouldNotRun.getCount());
  }

  private static class CountingNoOpOnSubscribe implements OnSubscribe<Integer>, Callable<Observable<Integer>> {
    private final AtomicInteger count = new AtomicInteger(0);

    @Override
    public void call(Subscriber<? super Integer> subscriber) {
      count.incrementAndGet();
      subscriber.onCompleted();
    }

    @Override
    public Observable<Integer> call() throws Exception {
      return Observable.create(this);
    }

    public int getCount() {
      return count.get();
    }
  }

  /**
   * Test changing the whichReturn value on the fly.
   */
  @Test
  public void useSupplier() throws Exception {
    final CountingNoOpOnSubscribe experiment = new CountingNoOpOnSubscribe();
    final CountingNoOpOnSubscribe control = new CountingNoOpOnSubscribe();

    final AtomicReference<WhichReturn> whichReturn = new AtomicReference<>(CONTROL_ONLY);
    final Trial trial = new Trial(executor, registry, "", new Supplier<WhichReturn>() {
      @Override
      public WhichReturn get() {
        return whichReturn.get();
      }
    });

    final IsEqual<TrialResult<Integer>> alwaysTrue = IsEqual.<TrialResult<Integer>>alwaysTrue();
    trial.doTrial(control, experiment, alwaysTrue, ROOT).toList().toBlocking().first();
    whichReturn.set(CONTROL);
    trial.doTrial(control, experiment, alwaysTrue, ROOT).toList().toBlocking().first();

    executor.shutdown();
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    assertEquals(2, control.getCount());
    assertEquals(1, experiment.getCount());
  }

  /**
   * Test when isEqual throws.
   */
  @Test
  public void isEqualFailure() throws Exception {
    controlTrial.doTrial(MAKE_1_2, MAKE_1_2, new IsEqual<TrialResult<Integer>>() {
      @Override
      public boolean apply(TrialResult<Integer> control, TrialResult<Integer> experiment) {
        throw new RuntimeException("GRORORO");
      }
    }, ROOT);

    verify(attempts).mark();
    verify(isEqualFailures, timeout(WAIT_TIMEOUT)).mark();

    verify(controlFailures, times(0)).mark();
    verify(successes, times(0)).mark();
    verify(rejectedExperimentFailures, times(0)).mark();
    verify(rejectedControlFailures, times(0)).mark();
    verify(experimentFailures, times(0)).mark();
    verify(compareFailures, times(0)).mark();
    verifyTimersClosed();
  }

  /**
   * Test that control is always the first item in calls to isEqual and that the experiment is always the second.
   */
  @Test
  public void isEqualControlThenExperiment() throws Exception {
    final AtomicReference<List<Integer>> aBox = new AtomicReference<>();
    final AtomicReference<List<Integer>> bBox = new AtomicReference<>();
    final CyclicBarrier latch = new CyclicBarrier(2);
    final IsEqual<TrialResult<Integer>> orderingCheck = new IsEqual<TrialResult<Integer>>() {
      @Override
      public boolean apply(final TrialResult<Integer> control, final TrialResult<Integer> experiment) {
        aBox.set(control.getResult());
        bBox.set(experiment.getResult());
        try {
          latch.await(WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException | BrokenBarrierException | TimeoutException e) {
          System.err.println("waiting in IsEqual");
          e.printStackTrace();
        }
        return true; // ignored
      }
    };

    controlTrial.doTrial(MAKE_1_2, MAKE_JUST_2, orderingCheck, ROOT)
        .toList().toBlocking().single();
    latch.await(WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
    assertEquals(LIST_1_2, aBox.get());
    assertEquals(JUST_2, bBox.get());

    experimentTrial.doTrial(MAKE_1_2, MAKE_JUST_2, orderingCheck, ROOT)
        .toList().toBlocking().single();
    latch.await(WAIT_TIMEOUT, TimeUnit.MILLISECONDS);

    assertEquals(LIST_1_2, aBox.get());
    assertEquals(JUST_2, bBox.get());
  }
}
