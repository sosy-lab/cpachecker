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

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.ThreadModularWaitlistElement;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.defaults.TauInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WaitlistElement;
import org.sosy_lab.cpachecker.core.waitlist.AbstractSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;

public class ThreadModularReachedSet extends AbstractReachedSet {

  protected final InferenceObjectReachedSet reachedInferenceObjects;

  ThreadModularReachedSet(WaitlistFactory factory, MainNestedReachedSet delegate) {
    super(factory, delegate);
    reachedInferenceObjects = new InferenceObjectReachedSet();
  }

  @Override
  public void add(AbstractState state, Precision precision) throws IllegalArgumentException {
    if (state instanceof InferenceObject) {
      reachedInferenceObjects.add((InferenceObject) state);
    } else {
      super.add(state, precision);
    }
  }

  @Override
  public void reAddToWaitlist(AbstractState pState) {
    Preconditions.checkArgument(!(pState instanceof InferenceObject));
    super.reAddToWaitlist(pState);
  }

  @Override
  protected void removeOnlyFromWaitlist(AbstractState pState, Precision pPrecision) {
    Preconditions.checkArgument(waitlist instanceof AbstractSortedWaitlist<?>);

    Iterator<WaitlistElement> iterator = ((AbstractSortedWaitlist<?>) waitlist).iterator();
    Predicate<ThreadModularWaitlistElement> check;

    if (pState instanceof InferenceObject) {
      check = w -> w.getInferenceObject().equals(pState);
    } else {
      check = w -> w.getState().equals(pState);
    }
    Set<WaitlistElement> toRemove = new HashSet<>();
    while (iterator.hasNext()) {
      ThreadModularWaitlistElement current = (ThreadModularWaitlistElement) iterator.next();

      if (check.apply(current)) {
        toRemove.add(current);
      }
    }

    toRemove.forEach(e -> waitlist.remove(e));
  }

  @Override
  public void updatePrecision(AbstractState s, Precision newPrecision) {
    Preconditions.checkArgument(!(s instanceof InferenceObject));
    super.updatePrecision(s, newPrecision);
  }

  @Override
  public Precision getPrecision(AbstractState pState)
      throws UnsupportedOperationException {
    Preconditions.checkArgument(!(pState instanceof InferenceObject));
    return super.getPrecision(pState);
  }

  @Override
  public boolean contains(AbstractState pState) {
    if (pState instanceof InferenceObject) {
      return reachedInferenceObjects.contains((InferenceObject) pState);
    } else {
      return super.contains(pState);
    }
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState pState)
      throws UnsupportedOperationException {
    if (pState instanceof InferenceObject) {
      return from(reachedInferenceObjects.getReached((InferenceObject) pState))
          .transform(s -> (AbstractState) s)
          .toSet();
    } else {
      return super.getReached(pState);
    }
  }

  @Override
  public Collection<AbstractState> getReached(CFANode pLocation) {
    Preconditions.checkArgument(false, "Not supported");
    return null;
  }

  @Override
  public void clear() {
    reachedInferenceObjects.clear();
    super.clear();
  }

  @Override
  protected void reAddToWaitlist(AbstractState pState, Precision pPrecision) {
    if (pState instanceof InferenceObject) {
      Preconditions.checkArgument(false, "Not supported");
    } else {
      reAddToWaitlist(pState, TauInferenceObject.getInstance(), pPrecision);
    }
  }

  public void reAddToWaitlist(AbstractState pState, InferenceObject object, Precision pPrecision) {
    if (pState instanceof InferenceObject) {
      Preconditions.checkArgument(false, "Not supported");
    } else if (object != EmptyInferenceObject.getInstance()) {
      ThreadModularWaitlistElement element =
          new ThreadModularWaitlistElement(pState, object, pPrecision);

      waitlist.add(element);
    }
  }

  public Collection<InferenceObject> getInferenceObjects() {
    return reachedInferenceObjects.asCollection();
  }

  public Collection<AbstractState> getStates() {
    return reached.asCollection();
  }

  @Override
  public void remove(AbstractState state) {
    if (state instanceof InferenceObject) {
      removeOnlyFromWaitlist(state, null);
      reachedInferenceObjects.remove((InferenceObject) state);
    } else {
      super.remove(state);
    }
  }

  @Override
  public void printStatistics(PrintStream pOut) {
    super.printStatistics(pOut);
    reachedInferenceObjects.printStatistics(pOut);
  }
}
