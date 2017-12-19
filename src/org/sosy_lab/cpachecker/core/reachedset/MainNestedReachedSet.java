/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.reachedset;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class MainNestedReachedSet implements NestedReachedSet<AbstractState> {

  protected final LinkedHashMap<AbstractState, Precision> reached;
  protected final Set<AbstractState> unmodifiableReached;
  private @Nullable AbstractState lastState = null;
  private @Nullable AbstractState firstState = null;

  public MainNestedReachedSet() {
    reached = new LinkedHashMap<>();
    unmodifiableReached = Collections.unmodifiableSet(reached.keySet());
  }

  public boolean add(AbstractState state, Precision precision) throws IllegalArgumentException {
    Preconditions.checkNotNull(state);
    Preconditions.checkNotNull(precision);

    if (reached.isEmpty()) {
      firstState = state;
    }

    Precision previousPrecision = reached.put(state, precision);

    if (previousPrecision == null) {
      // State wasn't already in the reached set.
      lastState = state;
      return true;

    } else {
      // State was already in the reached set.
      // This happens only if the MergeOperator produces a state that is already there.

      // The state may or may not be currently in the waitlist.
      // In the first case, we are not allowed to add it to the waitlist,
      // otherwise it would be in there twice (this method is responsible for
      // enforcing the set semantics of the waitlist).
      // In the second case, we do not need
      // to add it to the waitlist, because it was already handled
      // (we assume that the CPA would always produce the same successors if we
      // give it the same state twice).

      // So do nothing here.

      // But check if the new and the old precisions are equal.
      if (!precision.equals(previousPrecision)) {

        // Restore previous state of reached set
        // (a method shouldn't change state if it throws an IAE).
        reached.put(state, previousPrecision);

        throw new IllegalArgumentException(
            "State added to reached set which is already contained, but with a different precision");
      }
      return false;
    }
  }

  public void updatePrecision(AbstractState s, Precision newPrecision) {
    Preconditions.checkNotNull(s);
    Preconditions.checkNotNull(newPrecision);

    Precision oldPrecision = reached.put(s, newPrecision);
    if (oldPrecision == null) {
      // State was not contained in the reached set.
      // Restore previous state and throw exception.
      reached.remove(s);
      throw new IllegalArgumentException(
          "State needs to be in the reached set in order to change the precision.");
    }
  }

  public boolean remove(AbstractState state) {
    Preconditions.checkNotNull(state);
    int hc = state.hashCode();
    if (firstState != null && hc == firstState.hashCode() && state.equals(firstState)) {
      firstState = null;
    }

    if (lastState != null && hc == lastState.hashCode() && state.equals(lastState)) {
      lastState = null;
    }
    return reached.remove(state) != null;
  }

  @Override
  public void clear() {
    firstState = null;
    lastState = null;
    reached.clear();
  }

  @Override
  public Set<AbstractState> asCollection() {
    return unmodifiableReached;
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return unmodifiableReached.iterator();
  }

  public Collection<Precision> getPrecisions() {
    return Collections.unmodifiableCollection(reached.values());
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState state) {
    return asCollection();
  }

  @Override
  public Collection<AbstractState> getReached(CFANode location) {
    return asCollection();
  }

  @Override
  public AbstractState getFirstState() {
    Preconditions.checkState(firstState != null);
    return firstState;
  }

  @Override
  public AbstractState getLastState() {
    return lastState;
  }

  public Precision getPrecision(AbstractState state) {
    Preconditions.checkNotNull(state);
    Precision prec = reached.get(state);
    Preconditions.checkArgument(prec != null, "State not in reached set:\n%s", state);
    return prec;
  }

  public void forEach(BiConsumer<? super AbstractState, ? super Precision> pAction) {
    reached.forEach(pAction);
  }

  @Override
  public boolean contains(AbstractState state) {
    Preconditions.checkNotNull(state);
    return reached.containsKey(state);
  }

  @Override
  public int size() {
    return reached.size();
  }

  @Override
  public boolean isEmpty() {
    return (size() == 0);
  }

  @Override
  public String toString() {
    return reached.keySet().toString();
  }
}
