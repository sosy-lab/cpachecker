// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.instrumentation.InstrumentationAutomaton.InstrumentationProperty;
import org.sosy_lab.cpachecker.core.algorithm.instrumentation.InstrumentationAutomaton.StateAnnotation;

public class InstrumentationState {
  private String name;
  private StateAnnotation stateAnnotation;
  private InstrumentationAutomaton automatonOfTheState;

  public InstrumentationState(
      String pName,
      StateAnnotation pStateAnnotation,
      InstrumentationAutomaton pInstrumentationAutomaton) {
    this.name = pName;
    this.stateAnnotation = pStateAnnotation;
    this.automatonOfTheState = pInstrumentationAutomaton;
  }

  // Create a dummy state for the instrumentation operator, when no IA needs to be used.
  public InstrumentationState() {
    this.name = "DUMMY";
    this.stateAnnotation = StateAnnotation.TRUE;
    this.automatonOfTheState =
        new InstrumentationAutomaton(
            InstrumentationProperty.TERMINATION, ImmutableMap.of(), ImmutableMap.of(), 0);
  }

  public InstrumentationAutomaton getAutomatonOfTheState() {
    return automatonOfTheState;
  }

  public boolean isInitialAnnotation() {
    return stateAnnotation == StateAnnotation.INIT;
  }

  public boolean isFunctionHeadAnnotation() {
    return stateAnnotation == StateAnnotation.FUNCTIONHEADFORLOOP;
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
            && !name.equals("DUMMY"))
        || ((stateAnnotation == StateAnnotation.LOOPHEAD && pCFANode.isLoopStart())
            || (stateAnnotation == StateAnnotation.FUNCTIONHEADFORLOOP && pCFANode.isLoopStart()));
  }

  @Override
  public String toString() {
    return name;
  }
}
