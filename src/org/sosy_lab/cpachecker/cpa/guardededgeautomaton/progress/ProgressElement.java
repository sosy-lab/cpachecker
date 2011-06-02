/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.progress;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStateElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.IGuardedEdgeAutomatonStateElement;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton.State;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;

public class ProgressElement implements Targetable, AbstractElement, IGuardedEdgeAutomatonStateElement {

  private final GuardedEdgeAutomatonStateElement mAutomatonElement;
  private final NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge mTransition;

  public ProgressElement(GuardedEdgeAutomatonStateElement pAutomatonElement, NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge pTransition) {
    mAutomatonElement = pAutomatonElement;
    mTransition = pTransition;
  }

  public GuardedEdgeAutomatonElement getWrappedElement() {
    return mAutomatonElement;
  }

  public NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge getTransition() {
    return mTransition;
  }

  @Override
  public boolean isFinalState() {
    return mAutomatonElement.isFinalState();
  }

  @Override
  public State getAutomatonState() {
    return mAutomatonElement.getAutomatonState();
  }

  @Override
  public String toString() {
    return "ProgressElement[" + mAutomatonElement.toString() + "]";
  }

  @Override
  public boolean isTarget() {
    return isFinalState();
  }

}
