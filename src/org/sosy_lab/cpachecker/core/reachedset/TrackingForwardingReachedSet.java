// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * Extension of {@link ForwardingReachedSet} that records which states are added to and removed from
 * the reached set and exposes them as {@link ReachedSetDelta}.
 *
 * <p>The recorded changes and the possibility to reset them belong to the component that defines
 * what a tracking window is, which obtains them as a {@link TrackingSession} from {@link
 * TrackingSessionFactory}. To every other component an instance of this class is indistinguishable
 * from a plain {@link ReachedSet}, so no component can invalidate the view of the owner.
 */
public final class TrackingForwardingReachedSet extends ForwardingReachedSet {

  /**
   * Immutable snapshot of the changes of a reached set during one tracking window. Produced by a
   * {@link TrackingSession} and safe to hand to arbitrary consumers, such as refiners or heuristics
   * evaluating refinement progress.
   *
   * @param addedStates states added during the window, in the order in which they were added
   * @param removedStates states removed during the window, in the order in which they were removed
   */
  public record ReachedSetDelta(
      ImmutableSet<AbstractState> addedStates, ImmutableSet<AbstractState> removedStates) {

    private static final ReachedSetDelta EMPTY =
        new ReachedSetDelta(ImmutableSet.of(), ImmutableSet.of());

    public ReachedSetDelta {
      checkNotNull(addedStates, "addedStates must not be null.");
      checkNotNull(removedStates, "removedStates must not be null.");
    }

    public static ReachedSetDelta empty() {
      return EMPTY;
    }

    public boolean isEmpty() {
      return addedStates.isEmpty() && removedStates.isEmpty();
    }
  }

  /**
   * Owner handle for a reached set whose changes are tracked.
   *
   * <p>A session is created by {@link TrackingSessionFactory} and must not be shared: whoever holds
   * it decides where one tracking window ends and the next begins. The reached set returned by
   * {@link #reachedSet()} does not expose the tracking.
   *
   * <p>The session keeps a bounded history of closed windows to prevent the history from retaining
   * every state that was ever removed from the reached set.
   */
  public static final class TrackingSession {

    private final TrackingForwardingReachedSet trackingReachedSet;
    private final Queue<ReachedSetDelta> history;

    private TrackingSession(TrackingForwardingReachedSet pTrackingReachedSet, int pHistorySize) {
      trackingReachedSet = checkNotNull(pTrackingReachedSet);
      history = EvictingQueue.create(pHistorySize);
    }

    /** Returns the reached set to be used by the analysis. */
    public ReachedSet reachedSet() {
      return trackingReachedSet;
    }

    /**
     * Closes the current tracking window, appends its delta to the history, and opens the next
     * window.
     */
    public void closeWindow() {
      history.add(trackingReachedSet.closeWindow());
    }

    /**
     * Returns the deltas of the most recently closed windows, oldest first. At most as many entries
     * are returned as the history size configured for this session.
     */
    public ImmutableList<ReachedSetDelta> getHistory() {
      return ImmutableList.copyOf(history);
    }
  }

  private final Set<AbstractState> addedStates = new LinkedHashSet<>();
  private final Set<AbstractState> removedStates = new LinkedHashSet<>();

  private TrackingForwardingReachedSet(ReachedSet pDelegate) {
    super(pDelegate);
  }

  /** Creates a session together with the reached set it owns. */
  static TrackingSession createSession(ReachedSet pDelegate, int pHistorySize) {
    return new TrackingSession(new TrackingForwardingReachedSet(pDelegate), pHistorySize);
  }

  /**
   * Closes the current tracking window and immediately opens the next one.
   *
   * @return the changes recorded during the window closed by this call
   */
  private ReachedSetDelta closeWindow() {
    ReachedSetDelta delta =
        new ReachedSetDelta(ImmutableSet.copyOf(addedStates), ImmutableSet.copyOf(removedStates));
    addedStates.clear();
    removedStates.clear();
    return delta;
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) {
    addedStates.add(checkNotNull(pState));
    super.add(pState, pPrecision);
  }

  @Override
  public void addAll(Iterable<Pair<AbstractState, Precision>> pToAdd) {
    for (Pair<AbstractState, Precision> pair : pToAdd) {
      addedStates.add(checkNotNull(pair.getFirst()));
    }
    super.addAll(pToAdd);
  }

  @Override
  public void remove(AbstractState pState) {
    removedStates.add(checkNotNull(pState));
    super.remove(pState);
  }

  @Override
  public void removeAll(Iterable<? extends AbstractState> pToRemove) {
    for (AbstractState state : pToRemove) {
      removedStates.add(checkNotNull(state));
    }
    super.removeAll(pToRemove);
  }

  @Override
  public void clear() {
    addedStates.clear();
    removedStates.clear();
    super.clear();
  }
}
