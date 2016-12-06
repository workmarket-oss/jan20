/*
 * Copyright 2016, WorkMarket, Inc. All Rights Reserved.
 */
package com.workmarket.jan20;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;

/**
 * Run a trial of the experiment.  Reader note: The control experiment part uses a {@link
 * java.util.concurrent.Callable}
 * rather than taking the straight {@link Observable} because we wish to ensure that the experimental version is done
 * in parallel with the control.  If the API is synchronous, the API might have done all the work already and just
 * wrapped the response it had in an {@link Observable} rather than returning an {@link Observable} that's linked to
 * some running task in a thread-pool.
 * <p>
 * If you want to disable the parallelism, you can use
 * {@link com.google.common.util.concurrent.Callables#returning(Object)}} to make a
 * {@link java.util.concurrent.Callable} from an existing {@link Observable}, but really consider why you'd do it
 * first.
 * <p>
 * The timer metrics this generates begin when calling the callable to get the value and end either
 * when onCompleted/onError are called from the {@link Observable}, or when the callable throws an
 * exception.
 */
public final class Trial {
  private static final Logger logger = LoggerFactory.getLogger(Trial.class);

  /**
   * An enum telling the trial which of the two results to return and, by extension, which one is used only for
   * comparison.
   */
  public enum WhichReturn {
    /**
     * Return the value of control experiment.
     */
    CONTROL,

    /**
     * Don't even run the experiment, this causes doTrial to just return control.call(), and no metrics are updated.
     */
    CONTROL_ONLY,

    /**
     * Return the experimental result.
     */
    EXPERIMENT,

    /**
     * Don't even run the control, this causes doTrial to just return experiment.call(), and no metrics are updated.
     */
    EXPERIMENT_ONLY;

    /**
     * Since WhichReturn is a boolean, make it easy to switch based on it.
     *
     * @param ifControl    The value to return if this is CONTROL.
     * @param ifExperiment The value to return if this is EXPERIMENT.
     * @return a T
     */
    public <T> T ifControl(final T ifControl, final T ifExperiment) {
      if (this == CONTROL) {
        return ifControl;
      } else {
        return ifExperiment;
      }
    }
  }

  /**
   * If for some reason you require to manually pass in the IDENTITY_WRAPPER to the constructor in lieu of using the
   * 4 argument constructor, this is here so you don't have to.
   */
  public static final CallableObservableWrapper IDENTITY_WRAPPER = new CallableObservableWrapper() {
    @Override
    public <T> Callable<Observable<T>> wrap(final Callable<Observable<T>> original) {
      return original;
    }
  };

  private final ExecutorService executor;
  private final String metricRootName;
  private final Supplier<WhichReturn> whichReturnSupplier;
  private final MetricRegistry registry;
  private final CallableObservableWrapper returnWrapper;
  private final CallableObservableWrapper compareWrapper;

  /**
   * Create the trial.  For the executor, you want something that a) is a limited size pool and b) will reject
   * submissions if the pool is full, rather than blocking.  For example, the following will create a pool with one
   * thread ever that has a queue that holds one item, if you try to submit more tasks than can be handled by the
   * queue and pool, it will reject the execution.
   * <pre>
   * // change the 1's in the code below to real values! The first two ones, for a fixed pool should be the same value
   * // as each other whichever you choose.  The argument to LinkedBlockingQueue is arguable, but small values
   * // (like 1) are probably good choices.
   * {@code new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1));}
   * </pre>
   * <p>
   * Why do executors this way?  If the code that returns results that is only compared (normally the experiment, but
   * if whichReturn is EXPERIMENT, it would be the control code) is getting stuck or taking too long to run relative
   * to the main line code, we don't want to wedge our clients.  Given this only covers the execution of the callable,
   * and the thing which manages the Observable bits may run into it's own issues, but either way, we want the
   * whichReturn'd code to return unimpeded no matter what the non-whichReturn'd code does.
   * <p>
   * CAVEAT: the Trial framework here assumes that the control and experiment {@link Observable}s will only be
   * subscribed to a single time.  At the current time, this cannot check the case of multiple subscriptions, and
   * probably has assumptions on cold observables.  So rather than having weird things happen, subscribing a second
   * time to the result of doTrial will cause an exception to be thrown.  This is also to make it so that, with the
   * above exception, the subscription semantics of the original {@link Observable} is preserved.
   *
   * @param executor       The executor on which to put the callable that will only be used for comparisons.
   * @param registry       The metrics registry in which to create counters.
   * @param metricRootName The root name from which to make metrics with.
   * @param whichReturn    Which value do you wish to be returned, CONTROL/CONTROL_ONLY or EXPERIMENT?
   */
  public Trial(final ExecutorService executor, final MetricRegistry registry, final String metricRootName,
               final WhichReturn whichReturn) {
    this(executor, registry, metricRootName, Suppliers.ofInstance(whichReturn), IDENTITY_WRAPPER, IDENTITY_WRAPPER);
  }

  /**
   * Create the trial.  Like the other constructor, but provides the ability to pass in wrappers that will
   * be called to wrap the Callable&lt;Observable&gt; of the control and experiment.  Useful in cases like where you
   * need things to behave differently due to {@link ThreadLocal}s and such depending on whether they execute inline
   * or in a background thread.
   *
   * @param executor       The executor on which to put the callable that will only be used for comparisons.
   * @param registry       The metrics registry in which to create counters.
   * @param metricRootName The root name from which to make metrics with.
   * @param whichReturn    Supplier of which value you wish to be returned, CONTROL/CONTROL_ONLY or EXPERIMENT?
   */
  public Trial(final ExecutorService executor, final MetricRegistry registry, final String metricRootName,
               final Supplier<WhichReturn> whichReturn) {
    this(executor, registry, metricRootName, whichReturn, IDENTITY_WRAPPER, IDENTITY_WRAPPER);
  }

  /**
   * Create the trial.  Like the other constructor, but provides the ability to pass in wrappers that will
   * be called to wrap the Callable&lt;Observable&gt; of the control and experiment.  Useful in cases like where you
   * need things to behave differently due to {@link ThreadLocal}s and such depending on whether they execute inline
   * or in a background thread.
   *
   * @param executor       The executor on which to put the callable that will only be used for comparisons.
   * @param registry       The metrics registry in which to create counters.
   * @param metricRootName The root name from which to make metrics with.
   * @param whichReturn    Which value you wish to be returned, CONTROL/CONTROL_ONLY or EXPERIMENT?
   * @param returnWrapper  Wrapper that can be used to wrap whichever of control or experiment is used to produce
   *                       the return value.
   * @param compareWrapper Wrapper that can be used to wrap whichever of control or experiment is only
   *                       used for comparison.
   */
  public Trial(final ExecutorService executor,
               final MetricRegistry registry,
               final String metricRootName,
               final WhichReturn whichReturn,
               final CallableObservableWrapper returnWrapper,
               final CallableObservableWrapper compareWrapper) {
    this(executor, registry, metricRootName, Suppliers.ofInstance(whichReturn), returnWrapper, compareWrapper);
  }

  /**
   * Create the trial.  Like the other constructor, but provides the ability to pass in wrappers that will
   * be called to wrap the Callable&lt;Observable&gt; of the control and experiment.  Useful in cases like where you
   * need things to behave differently due to {@link ThreadLocal}s and such depending on whether they execute inline
   * or in a background thread.
   *
   * @param executor       The executor on which to put the callable that will only be used for comparisons.
   * @param registry       The metrics registry in which to create counters.
   * @param metricRootName The root name from which to make metrics with.
   * @param whichReturn    Supplier of which value you wish to be returned, CONTROL/CONTROL_ONLY or EXPERIMENT?
   * @param returnWrapper  Wrapper that can be used to wrap whichever of control or experiment is used to produce
   *                       the return value.
   * @param compareWrapper Wrapper that can be used to wrap whichever of control or experiment is only
   *                       used for comparison.
   */
  public Trial(final ExecutorService executor,
               final MetricRegistry registry,
               final String metricRootName,
               final Supplier<WhichReturn> whichReturn,
               final CallableObservableWrapper returnWrapper,
               final CallableObservableWrapper compareWrapper) {
    this.executor = executor;
    this.whichReturnSupplier = whichReturn;
    this.metricRootName = metricRootName;
    this.registry = registry;
    this.returnWrapper = returnWrapper;
    this.compareWrapper = compareWrapper;
  }

  /**
   * Do a trial.
   *
   * @param control    A Callable that returns an {@code Observable<T>} for the control part of the experiment.
   * @param experiment A Callable that returns an {@code Observable<T>} for the experimental part of the experiment.
   * @param isEqual    An instance used to check equality of the results.
   * @return The result of the control callable.
   * @throws Exception the exception that control would throw.
   */
  public <T> Observable<T> doTrial(final Callable<Observable<T>> control, final Callable<Observable<T>> experiment,
                                   final IsEqual<TrialResult<T>> isEqual, final String metricName) throws Exception {
    final WhichReturn whichReturn = whichReturnSupplier.get();
    if (whichReturn == WhichReturn.CONTROL_ONLY) {
      return returnWrapper.wrap(control).call();
    }

    if (whichReturn == WhichReturn.EXPERIMENT_ONLY) {
      return returnWrapper.wrap(experiment).call();
    }

    final Callable<Observable<T>> returnValueCallable = returnWrapper
        .wrap(whichReturn.ifControl(control, experiment));
    final Callable<Observable<T>> compareValueCallable = compareWrapper.wrap(
        whichReturn.ifControl(experiment, control));

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<TrialResult<T>> returnValue = new AtomicReference<>(null);
    final AtomicReference<Context> compareContext = new AtomicReference<>(null);
    final AtomicReference<Context> returnContext = new AtomicReference<>(null);

    /* Recorder for the value we will eventually return. */
    final ObservableRecorder<T> returnValueRecorder = new ObservableRecorder<T>(
        new ResultConsumer<T>() {
          @Override
          public void consume(final TrialResult<T> trialResult) {
            returnContext.get().close();
            returnValue.set(trialResult);
            // Tell the compareValueRecorder that this part has completed
            latch.countDown();
          }
        });

    // Get/create all metrics up front, so graphs don't have different visually rendered sizes because certain events
    // haven't happened on all experiments.
    final Meter isEqualFailures = getMetric(metricName, "isEqualFailures");
    final Meter successes = getMetric(metricName, "successes");
    final Meter compareFailures = getMetric(metricName, "compareFailures");
    final Meter experimentFailures = getMetric(metricName, "experimentFailures");
    final Meter controlFailures = getMetric(metricName, "controlFailures");
    final Timer controlTimer = getTimer(metricName, "controlTimer");
    final Timer experimentTimer = getTimer(metricName, "experimentTimer");
    final Meter rejectedExperimentFailures = getMetric(metricName, "rejectedExperimentFailures");
    final Meter rejectedControlFailures = getMetric(metricName, "rejectedControlFailures");
    final Meter attempts = getMetric(metricName, "attempts");

    /* Recorder for the value that we only use for comparison at the end. */
    final ObservableRecorder<T> comparedValueRecorder = new ObservableRecorder<T>(
        new ResultConsumer<T>() {
          @Override
          public void consume(final TrialResult<T> trialResult) {
            compareContext.get().close();
            try {
              latch.await(); // wait for the control to finish
            } catch (final Exception e) {
              logger.error("LIKELY BUG! Error awaiting {} result", whichReturn, e);
            }
            /* If the control callable blows up without producing the Observable, we'll get null here and the
             * experiment was basically for nothing, but oh well. */
            if (returnValue.get() == null) {
              return;
            }
            final boolean trialSucceeded;
            try {
              trialSucceeded = isEqual.apply(
                  whichReturn.ifControl(returnValue.get(), trialResult),
                  whichReturn.ifControl(trialResult, returnValue.get()));
            } catch (final RuntimeException e) {
              logger.error("IsEqual threw an exception", e);
              isEqualFailures.mark();
              return;
            }
            if (trialSucceeded) {
              successes.mark();
            } else {
              compareFailures.mark();
            }
          }
        });


    // Spin up the compare callable bits in a thread
    try {
      executor.submit(new Runnable() {
        @Override
        public void run() {
          final Observable<T> compareObservable;
          try {
            compareContext.set(whichReturn.ifControl(experimentTimer, controlTimer).time());
            compareObservable = compareValueCallable.call();
          } catch (final Exception e) {
            compareContext.get().close();
            logger.error("Error calling {} to even get the observable",
                whichReturn.ifControl("experiment", "control"), e);
            whichReturn.ifControl(experimentFailures, controlFailures).mark();
            return;
          }
          compareObservable.subscribe(comparedValueRecorder);
        }
      });
    } catch (final RejectedExecutionException e) {
      logger.error("Execution of {} rejected on executor (presumably due to pool exhaustion)",
          whichReturn.ifControl("experiment", "control"), e);
      whichReturn.ifControl(rejectedExperimentFailures, rejectedControlFailures).mark();
    }

    // This is where the return stuff actually runs inline.
    attempts.mark();
    final Observable<T> retval;
    returnContext.set(whichReturn.ifControl(controlTimer, experimentTimer).time());
    try {
      retval = returnValueCallable.call();
    } catch (final Exception e) {
      returnContext.get().close();
      latch.countDown(); // make sure that the compareCallable can stop waiting
      whichReturn.ifControl(controlFailures, experimentFailures).mark();
      throw e;
    }

    final Observable<T> cachedRetVal = retval.cache();
    cachedRetVal.subscribe(returnValueRecorder);
    return cachedRetVal;
  }

  private Timer getTimer(final String metricName, final String subMetric) {
    return registry.timer(getMetricName(metricName, subMetric));
  }

  private String getMetricName(final String metricName, final String subMetric) {
    final StringBuilder name = new StringBuilder().append(metricName).append(".").append(subMetric);
    if (metricRootName == null || "".equals(metricRootName)) {
      return name.toString();
    }
    return metricRootName + "." + name;
  }

  private Meter getMetric(final String metricName, final String subMetric) {
    return registry.meter(getMetricName(metricName, subMetric));
  }

  /**
   * Given {@link IsEqual}s for the throwable and result, return an IsEqual for the whole trial.
   *
   * @param throwableIsEqual IsEqual for the throwable
   * @param resultIsEqual    IsEqual for the result.
   */
  public static <T> IsEqual<TrialResult<T>> makeIsEqual(
      final IsEqual<Throwable> throwableIsEqual,
      final IsEqual<Iterable<T>> resultIsEqual) {

    // In Java 8 terms...
    //   return throwableIsEqual.onExtractionOf((Function<TrialResult<T>, Throwable>) TrialResult::getException)
    //       .and(resultIsEqual.onExtractionOf(TrialResult::getResult));
    // or even:
    //   return IsEqual.use((a, b) -> throwableIsEqual.apply(a.getException(), b.getException())
    //       && resultIsEqual.apply(a.getResult(), b.getResult()));
    // Buuuuut... we're in Java 7, and so this:
    return new IsEqual<TrialResult<T>>() {
      @Override
      public boolean apply(final TrialResult<T> control, final TrialResult<T> experiment) {
        return throwableIsEqual.apply(control.getException(), experiment.getException())
            && resultIsEqual.apply(control.getResult(), experiment.getResult());
      }
    };
  }
}
