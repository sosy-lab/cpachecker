/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.speci;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;


public class SpecInferenceState implements AbstractState, Graphable {

  private final Automaton automaton;
  private final String handle;

  public SpecInferenceState() {
    automaton = new Automaton();
    handle = null;
  }

  public SpecInferenceState(final Automaton pAutomaton, final String pHandle) {
    checkNotNull(pAutomaton);
    automaton = pAutomaton;
    handle = pHandle;
  }

  public SpecInferenceState addAutomatonState(String pState) {
   checkNotNull(pState);
   return new SpecInferenceState(automaton.addStatement(pState), this.handle);
  }

  public SpecInferenceState startTracking(String pHandle, String pState) {
    checkNotNull(pState);
    checkNotNull(pHandle);

    return new SpecInferenceState(automaton.addStatement(pState), pHandle);
  }

  public SpecInferenceState stopTracking(String pState) {
    checkNotNull(pState);

    return new SpecInferenceState(automaton.addStatement(pState), null);
  }

  @Override
  public String toDOTLabel() {
    return "[ " + (handle != null ? handle : "") + " - " + automaton.toString() + " ]";
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public String toString() {
    return automaton.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(automaton);
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) { return true; }

    if (!(pObj instanceof SpecInferenceState)) { return false; }

    SpecInferenceState other = (SpecInferenceState) pObj;

    return automaton.equals(other.automaton);
  }

  /**
   * Returns true if this state is less or equal than the state provided as an argument.
   *
   * @param other is the state to compare to
   * @return (this <= other)
   */
  public boolean isLessOrEqual(SpecInferenceState other) {

    // only merge on exact matches (loops) No merges for ifs (yet)
    // may introduce bugs, if there is a loop at a point where the handle is unknown
    if (handle == null) {
      return false;
    } else {
      return automaton.toString().equals(other.automaton.toString());
    }

  }

  public SpecInferenceState join(SpecInferenceState other) {
    return new SpecInferenceState(); // TODO implement
  }


  public String getHandle() {
    return handle;
  }

  public Automaton getAutomaton() {
    return automaton;
  }

  public boolean isEmpty() {
    return automaton.getRoot() == automaton.getSink();
  }
}
