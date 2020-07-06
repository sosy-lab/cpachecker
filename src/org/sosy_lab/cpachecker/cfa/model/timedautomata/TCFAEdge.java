// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.timedautomata;

import com.google.common.base.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class TCFAEdge extends AbstractCFAEdge {

  private final Set<TaVariable> variablesToReset;
  private final Optional<TaVariableCondition> guard;
  private final Optional<TaVariable> action;

  private static final long serialVersionUID = 5472749446453717391L;

  public TCFAEdge(
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      Optional<TaVariableCondition> pGuard,
      Set<TaVariable> pVariablesToReset,
      Optional<TaVariable> pAction) {
    super("", pFileLocation, pPredecessor, pSuccessor);
    variablesToReset = pVariablesToReset;
    guard = pGuard;
    action = pAction;
  }

  public Optional<TaVariable> getAction() {
    return action;
  }

  public Set<TaVariable> getVariablesToReset() {
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

  @Override
  public String getDescription() {
    var guardString = guard.transform(g -> g.toString()).or("-");
    var actionString = action.transform(a -> a.getShortName()).or("-");
    var resetString =
        String.join(
            ", ", variablesToReset.stream().map(v -> v.getShortName()).collect(Collectors.toSet()));
    if (resetString.isBlank()) {
      resetString = "-";
    }

    return guardString + " | " + actionString + " | " + resetString;
  }

  @Override
  public int hashCode() {
    return Objects.hash(variablesToReset, guard, action, getPredecessor(), getSuccessor());
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof TCFAEdge)) {
      return false;
    }
    TCFAEdge tCFAEdge = (TCFAEdge) o;
    return Objects.equals(variablesToReset, tCFAEdge.variablesToReset)
        && Objects.equals(guard, tCFAEdge.guard)
        && Objects.equals(action, tCFAEdge.action)
        && getPredecessor().equals(tCFAEdge.getPredecessor())
        && getSuccessor().equals(tCFAEdge.getSuccessor());
  }
}
