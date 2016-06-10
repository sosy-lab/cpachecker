/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.termination;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

import javax.annotation.concurrent.Immutable;

@Immutable
public class TerminationState extends AbstractSingleWrapperState implements Graphable {

  private static final long serialVersionUID = 2L;

  private final boolean loop;

  /**
   * Creates a new {@link TerminationState} that is part of the lasso's stem.
   *
   * @param pWrappedState
   *          the {@link AbstractState} to wrap
   * @return the created {@link TerminationState}
   */
  public static TerminationState createStemState(AbstractState pWrappedState) {
    return new TerminationState(pWrappedState, false);
  }

  private TerminationState(AbstractState pWrappedState, boolean pLoop) {
    super(checkNotNull(pWrappedState));
    loop = pLoop;
  }

  /**
   * Creates a new {@link TerminationState} that is part of the lasso's loop iff this
   * {@link TerminationState} is part of the lasso's loop.
   *
   * @param pWrappedState
   *            the {@link AbstractState} to wrap
   * @return the created {@link TerminationState}
   */
  public TerminationState withWrappedState(AbstractState pWrappedState) {
    return new TerminationState(pWrappedState, loop);
  }

  /**
   * Creates a new {@link TerminationState} that is the first state of the lasso's loop.
   *
   * @return the created {@link TerminationState}
   */
  public TerminationState enterLoop() {
    Preconditions.checkArgument(!loop, "% is already part of the lasso's loop", this);
    return new TerminationState(getWrappedState(), true);
  }

  /**
   * @return <code>true</code> iff this {@link TerminationState} is part of the lasso's loop.
   */
  public boolean isPartOfLoop() {
    return loop;
  }

  /**
   * @return <code>true</code> iff this {@link TerminationState} is part of the lasso's stem.
   */
  public boolean isPartOfStem() {
    return !loop;
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();
    if (loop) {
      sb.append("loop");
    } else {
      sb.append("stem");
    }

    if (getWrappedState() instanceof Graphable) {
      sb.append("\n");
      sb.append(((Graphable)getWrappedState()).toDOTLabel());
    }

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    if (getWrappedState() instanceof Graphable) {
      return ((Graphable)getWrappedState()).shouldBeHighlighted();
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(TerminationState.class.getSimpleName());
    if (loop) {
      sb.append("(loop)");
    } else {
      sb.append("(stem)");
    }

    sb.append(" ");
    sb.append(getWrappedState());

   return sb.toString();
  }
}
