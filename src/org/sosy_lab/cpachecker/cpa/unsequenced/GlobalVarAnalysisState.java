// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

public class GlobalVarAnalysisState implements AbstractState, Graphable {
  private final Set<String> globalVars;
  private final boolean waitReturn;      // Flag to detect if function return is awaited (x = f())
  private final String waitingVar;
  private final List<String> detectedAssignedVars;

  public GlobalVarAnalysisState(
      Set<String> pGlobalVars,
      boolean pWaitReturn,
      String pWaitingVar,
      List<String> pDetectedAssignedVars) {
    globalVars = pGlobalVars;
    waitReturn = pWaitReturn;
    waitingVar = pWaitingVar;
    detectedAssignedVars = pDetectedAssignedVars;
  }

  public GlobalVarAnalysisState() {
    globalVars = new HashSet<>();
    waitReturn = false;
    waitingVar = null;
    detectedAssignedVars = new ArrayList<>();
  }

  public Set<String> getGlobalVars() {
    return globalVars;
  }

  public boolean isWaitReturn() {
    return waitReturn;
  }

  public String getWaitingVar() {
    return waitingVar;
  }

  public List<String> getDetectedAssignedVars() {
    return detectedAssignedVars;
  }

  @Override
  public String toDOTLabel() {
    return "global vars = " + globalVars.toString()
        + ", waitReturn = " + waitReturn
        + ", waitingVar = " + waitingVar
        + ", detectedAssignedVars = " + detectedAssignedVars.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }



}