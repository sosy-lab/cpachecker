// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.timedautomata;

import com.google.common.base.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class TCFAEdge extends AbstractCFAEdge {

  private final Set<String> variablesToReset;
  private final Optional<TaVariableCondition> guard;
  private final Optional<String> action;

  private static final long serialVersionUID = 5472749446453717391L;

  public TCFAEdge(
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      Optional<TaVariableCondition> pGuard,
      Set<String> pVariablesToReset,
      Optional<String> pAction) {
    super("", pFileLocation, pPredecessor, pSuccessor);
    variablesToReset = pVariablesToReset;
    guard = pGuard;
    action = pAction;
  }

  public Optional<String> getAction() {
    return action;
  }

  public Set<String> getVariablesToReset() {
    return variablesToReset;
  }

  public Optional<TaVariableCondition> getGuard() {
    return guard;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.TimedAutomatonEdge;
  }

  @Override
  public String getCode() {
    return "";
  }
}
