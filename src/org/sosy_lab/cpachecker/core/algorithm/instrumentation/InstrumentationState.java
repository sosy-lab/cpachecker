// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import static com.google.common.base.Preconditions.checkNotNull;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.instrumentation.InstrumentationAutomaton.StateAnnotation;

public class InstrumentationState {
  private final String name;
  private final StateAnnotation stateAnnotation;
  // Null only for the dummy state
  private final @Nullable InstrumentationAutomaton automatonOfTheState;

  public InstrumentationState(
      String pName,
      StateAnnotation pStateAnnotation,
      InstrumentationAutomaton pInstrumentationAutomaton) {
    this.name = pName;
    this.stateAnnotation = pStateAnnotation;
    this.automatonOfTheState = pInstrumentationAutomaton;
  }

  private InstrumentationState() {
    name = "DUMMY";
    stateAnnotation = StateAnnotation.TRUE;
    automatonOfTheState = null;
  }

  /**
   * Creates a dummy state, which does not belong to any automaton. It is used for the CFA nodes at
   * which no automaton has been started yet.
   */
  public static InstrumentationState createDummyState() {
    return new InstrumentationState();
  }

  public boolean isDummy() {
    return automatonOfTheState == null;
  }

  public InstrumentationAutomaton getAutomatonOfTheState() {
    return checkNotNull(automatonOfTheState);
  }

  public boolean isInitialAnnotation() {
    return stateAnnotation == StateAnnotation.INIT;
  }

  public boolean isFunctionHeadAnnotation() {
    return stateAnnotation == StateAnnotation.FUNCTIONHEADFORLOOP;
  }

  public boolean isLoopHeadAnnotation() {
    return stateAnnotation == StateAnnotation.LOOPHEAD;
  }

  public boolean stateMatchesCfaNode(CFANode pCFANode) {
    if (stateAnnotation == StateAnnotation.MAINFUNCTIONHEAD
        && pCFANode.getFunctionName().equals("main")) {
      for (int i = 0; i < pCFANode.getNumEnteringEdges(); i++) {
        if (pCFANode.getEnteringEdge(i).getDescription().contains("Function start")) {
          return true;
        }
      }
      return false;
    }
    return ((stateAnnotation == StateAnnotation.INIT || stateAnnotation == StateAnnotation.TRUE)
            && !isDummy())
        || ((stateAnnotation == StateAnnotation.LOOPHEAD && pCFANode.isLoopStart())
            || (stateAnnotation == StateAnnotation.FUNCTIONHEADFORLOOP && pCFANode.isLoopStart()));
  }

  @Override
  public String toString() {
    return name;
  }
}
