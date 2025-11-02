// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import com.google.common.base.Joiner;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3Trace extends K3SelectTraceComponent {

  @Serial private static final long serialVersionUID = 6210509388439561283L;
  private final List<K3TraceSetGlobalVariable> setGlobalVariables;
  private final K3TraceEntryCall entryCall;
  private final List<K3TraceStep> steps;
  private final K3ViolatedProperty violatedProperty;

  public K3Trace(
      List<K3TraceSetGlobalVariable> pSetGlobalVariables,
      K3TraceEntryCall pEntryCall,
      List<K3TraceStep> pSteps,
      K3ViolatedProperty pViolatedProperty,
      FileLocation pFileLocation) {
    super(pFileLocation);
    setGlobalVariables = pSetGlobalVariables;
    entryCall = pEntryCall;
    steps = pSteps;
    violatedProperty = pViolatedProperty;
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  <R, X extends Exception> R accept(K3TraceElementVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return Joiner.on(" ")
        .join(
            Joiner.on(" ").join(setGlobalVariables.stream().map(K3AstNode::toASTString).toList()),
            entryCall.toASTString(pAAstNodeRepresentation),
            Joiner.on(" ").join(steps.stream().map(K3AstNode::toASTString).toList()),
            violatedProperty.toASTString(pAAstNodeRepresentation));
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  public List<K3TraceSetGlobalVariable> getSetGlobalVariables() {
    return setGlobalVariables;
  }

  public K3TraceEntryCall getEntryCall() {
    return entryCall;
  }

  public List<K3TraceStep> getSteps() {
    return steps;
  }

  public K3ViolatedProperty getViolatedProperty() {
    return violatedProperty;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + setGlobalVariables.hashCode();
    result = prime * result + entryCall.hashCode();
    result = prime * result + steps.hashCode();
    result = prime * result + violatedProperty.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3Trace other
        && setGlobalVariables.equals(other.setGlobalVariables)
        && entryCall.equals(other.entryCall)
        && steps.equals(other.steps)
        && violatedProperty.equals(other.violatedProperty);
  }
}
