// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.instrumentation.InstrumentationAutomaton.InstrumentationProperty;
import org.sosy_lab.cpachecker.core.algorithm.instrumentation.InstrumentationAutomaton.StateAnnotation;

public class InstrumentationState {
  private String name;
  private StateAnnotation stateAnnotation;
  private InstrumentationAutomaton automatonOfTheState;

  public InstrumentationState(String pName,
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
    this.automatonOfTheState = new InstrumentationAutomaton(InstrumentationProperty.TERMINATION,
                                                            ImmutableMap.of());
  }

  public InstrumentationAutomaton getAutomatonOfTheState() {
    return automatonOfTheState;
  }

  public boolean stateMatchesCfaNode(CFANode pCFANode, CFA pCFA) {
    return (stateAnnotation == StateAnnotation.TRUE && !name.equals("DUMMY")) ||
        (stateAnnotation == StateAnnotation.INIT &&
            pCFANode.equals(pCFA.getMetadata().getMainFunctionEntry())) ||
            (stateAnnotation == StateAnnotation.LOOPHEAD && pCFANode.isLoopStart());
  }

  @Override
  public String toString() {
    return name;
  }
}
