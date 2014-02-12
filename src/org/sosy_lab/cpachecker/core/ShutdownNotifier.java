/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core;

import static com.google.common.base.Preconditions.*;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.MapMaker;

/**
 * This class implements a central component that is used for interrupting
 * and stopping a CPAchecker run in case of a timeout or when the user
 * wants to cancel the analysis.
 *
 * It works passively, the running analysis will not be interrupted directly,
 * but instead it has to check every then and now whether it should shutdown.
 * This means that the CPAchecker only halts in "clean" states,
 * never in an intermediate unsafe state,
 * and we can safely do any post-processing (such as statistics printing).
 *
 * The check whether a shutdown was requested is cheap and should be done quite often
 * in order to ensure a timely response to a shutdown request.
 * As a rule of thumb, all operations that may take longer than 1s
 * should take care of calling {@link #shouldShutdown()} or {@link #shutdownIfNecessary()}
 * from time to time.
 *
 * This class supports a hierarchy of instances.
 * Setting the shutdown request on a higher-level instance will do the same in all
 * children instances (recursively), but not vice-versa.
 * This can be used for example to implement global and component-specific timeouts
 * at the same time, with the former overriding the latter if necessary.
 *
 * This class does not implement any timeout by itself.
 * It could do so by checking whether the timeout was reached in
 * {@link #shouldShutdown()}, but this could be expensive when called too often.
 * Instead, a separate component should be used that implements the timeout
 * more intelligently, and uses this class to request a shutdown after the timeout
 * was reached.
 *
 * This class is completely thread safe.
 */
@ParametersAreNonnullByDefault
public final class ShutdownNotifier {

  /*
   * Implementation details:
   * The check whether a shutdown was requested needs to be cheap and thread safe.
   * The cheapest way to do this in Java is to read from a volatile field,
   * and accessing an AtomicReference is the same.
   * We use an AtomicReference instead of a volatile field to be sure
   * that we get the correct reason for the shutdown.
   * The semantics of this field is that there was no shutdown requested
   * as long as reference is null.
   */
  private final AtomicReference<String> shutdownRequested = new AtomicReference<>();

  /**
   * For maintaining the list of listeners, the AtomicReference shutdownRequest
   * is not enough, so we synchronize on the list and have a second flag
   * whether they were already notified.
   */
  private final Set<ShutdownRequestListener> listeners =
      // This creates a set which is backed by a thread-safe map
      // with identity comparison (== instead of equals())
      // and weak references for the keys
      Collections.newSetFromMap(new MapMaker().concurrencyLevel(1).weakKeys().<ShutdownRequestListener, Boolean>makeMap());

  // Separate flag for notification of listeners
  // in order to prevent a race condition when registering a listener
  // and calling requestStop() at the same time.
  // This variable is not volatile and always needs to accessed from within
  // a synchronized (listeners) { } block!
  private boolean listenersNotified = false;

  // Do not remove this field, otherwise the listener will be garbage collected
  // and we could miss notifications.
  private final ShutdownRequestListener ourListener = new ShutdownRequestListener() {
    @Override
    public void shutdownRequested(String reason) {
      ShutdownNotifier.this.requestShutdown(reason);
    }
  };

  private ShutdownNotifier() { }

  /**
   * Create a fresh new instance of this class.
   * There are no listeners and stop has not been requested yet.
   */
  public static ShutdownNotifier create() {
    return new ShutdownNotifier();
  }

  /**
   * Create a fresh new instance of this class in a hierarchy.
   *
   * The new instance is considered to be a child of the given instance,
   * this means as soon as the parent has a shutdown requested,
   * the same is true for the child instance (but not vice-versa).
   * Note that if the parent instance already has shutdown requested,
   * the new instance is also immediately in the same state.
   *
   * @param parent A non-null ShutdownNotifier instance.
   */
  public static ShutdownNotifier createWithParent(final ShutdownNotifier parent) {
    final ShutdownNotifier child = create();
    parent.registerAndCheckImmediately(child.ourListener);
    return child;
  }

  /**
   * Request a shutdown of all components that check this instance,
   * by letting {@link #shouldShutdown()} return true in the future,
   * and by notifying all registered listeners.
   * Only the first call to this method has an effect.
   * When this method returns, it is guaranteed that all currently registered
   * listeners where notified and have been unregistered.
   *
   * @param pReason A non-null human-readable string that tells the user why a shutdown was requested.
   */
  public void requestShutdown(final String pReason) {
    checkNotNull(pReason);

    if (shutdownRequested.compareAndSet(null, pReason)) {
      // Shutdown was not requested before, only one thread ever enters this block.

      // Notify listeners
      // Additional synchronization necessary for registerAndCheckImmediately()
      // and for iterating over the list.
      synchronized (listeners) {
        assert !listenersNotified;
        listenersNotified = true;

        for (ShutdownRequestListener listener : listeners) {
          // TODO exception safety
          listener.shutdownRequested(pReason);
        }
        listeners.clear();
      }
    }
  }

  /**
   * Check whether a shutdown was previously requested.
   * This method returns false immediately after this instance was constructed,
   * and may return true later on.
   * After it returned true once it will always keep returning true,
   * and never return false again.
   * Calling this method is very cheap.
   */
  public boolean shouldShutdown() {
    return shutdownRequested.get() != null;
  }

  /**
   * Check whether a shutdown was previously requested,
   * and throw an {@link InterruptedException} in this case.
   * Once a shutdown was requested, every call to this method will throw an exception.
   * In the common case that no shutdown was yet requested,
   * calling this method is very cheap.
   * @throws InterruptedException If a shutdown was requested.
   */
  public void shutdownIfNecessary() throws InterruptedException {
    if (shouldShutdown()) {
      throw new InterruptedException(getReason());
    }
  }

  /**
   * Return the reason for the shutdown request on this instance.
   * @return A non-null human-readable string.
   * @throws IllegalStateException If there was no shutdown request on this instance.
   */
  public String getReason() {
    String reason = shutdownRequested.get();
    checkState(reason != null, "Cannot call getReason() on an instance with no shutdown request.");
    return reason;
  }

  /**
   * Register a listener that will be notified once when {@link #requestShutdown(String)}
   * is called for the first time.
   *
   * Listeners registered after {@link #requestShutdown(String)} was already called
   * will never be notified (so calling this method at that time has no effect).
   *
   * This class keeps only weak reference to the listener to allow the GC
   * to collect them, so make sure to keep a strong reference to your instance
   * as long as you won't to be notified.
   *
   * @param listener A non-null and not already registered listener.
   */
  public void register(ShutdownRequestListener listener) {
    checkNotNull(listener);

    synchronized (listeners) {
      if (!listenersNotified) {
        boolean freshListener = listeners.add(listener);
        checkArgument(freshListener, "Not allowed to register listeners twice");
      }
      // else do nothing because its irrelevant
    }
  }

  /**
   * Register a listener that will be notified once when {@link #requestShutdown(String)}
   * is called for the first time, or immediately, if the {@link #requestShutdown(String)}
   * method has been called already.
   *
   * Use this method to avoid a race condition when registering the listener
   * and checking for a requested shutdown at the same time
   * (you could loose a notification).
   *
   * This class keeps only weak reference to the listener to allow the GC
   * to collect them, so make sure to keep a strong reference to your instance
   * as long as you won't to be notified.
   *
   * @param listener A non-null and not already registered listener.
   */
  public void registerAndCheckImmediately(ShutdownRequestListener listener) {
    synchronized (listeners) { // synchronized block to have atomic "register-or-notify"
      register(listener); // does nothing if listenersNotified==true

      if (listenersNotified) {
        String reason = getReason();
        // Listeners were already notified previously,
        // this listener would not get called.
        listener.shutdownRequested(reason);
      }
    }
  }

  /**
   * Unregister a listener.
   * This listener will not be notified in the future.
   * It is safe to call this method twice with the same listener.
   * It is not necessary to call this method for a listener that was already notified.
   *
   * @param listener A previously registered listener.
   */
  public void unregister(ShutdownRequestListener listener) {
    checkNotNull(listener);

    // listeners is thread-safe
    listeners.remove(listener);
  }

  /**
   * Utility method for creating a {@link ShutdownRequestListener}
   * that interrupts the current thread (that calls this method) on a shutdown.
   * Note that this method does not actually do anything, you need to
   * register the returned listener with an instance of this class.
   * @return
   */
  public static ShutdownRequestListener interruptCurrentThreadOnShutdown() {
    final Thread currentThread = Thread.currentThread();
    return new ShutdownRequestListener() {
        @Override
        public void shutdownRequested(String pReason) {
          currentThread.interrupt();
        }
      };
  }

  public static interface ShutdownRequestListener  {

    /**
     * This method is called on registered listeners the first time
     * {@link ShutdownNotifier#requestShutdown(String)} was called.
     *
     * Implementations of this method should be reasonably quick
     * and never throw an exception.
     *
     * Note that it is usually not necessary to use a listener when
     * all you want to do in this method is to set some boolean flag.
     * Instead, just call {@link ShutdownRequestListener#shouldShutdown()}
     * whenever you would check the flag (this is similarly cheap).
     *
     * @param reason A non-null human-readable string that tells the user why a shutdown was requested.
     */
    void shutdownRequested(String reason);
  }
}
