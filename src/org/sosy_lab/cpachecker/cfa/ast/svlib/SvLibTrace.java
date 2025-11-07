// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.base.Joiner;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class SvLibTrace extends SvLibSelectTraceComponent {

  @Serial private static final long serialVersionUID = 6210509388439561283L;
  private final List<SvLibTraceSetGlobalVariable> setGlobalVariables;
  private final SvLibTraceEntryCall entryCall;
  private final List<SvLibTraceStep> steps;
  private final SvLibViolatedProperty violatedProperty;
  private final List<SvLibTraceSetTag> setTags;

  public SvLibTrace(
      List<SvLibTraceSetGlobalVariable> pSetGlobalVariables,
      SvLibTraceEntryCall pEntryCall,
      List<SvLibTraceStep> pSteps,
      SvLibViolatedProperty pViolatedProperty,
      List<SvLibTraceSetTag> pSetTags,
      FileLocation pFileLocation) {
    super(pFileLocation);
    setGlobalVariables = pSetGlobalVariables;
    entryCall = pEntryCall;
    steps = pSteps;
    violatedProperty = pViolatedProperty;
    setTags = pSetTags;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  <R, X extends Exception> R accept(SvLibTraceElementVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return Joiner.on(System.lineSeparator())
        .join(
            Joiner.on(" ")
                .join(setGlobalVariables.stream().map(SvLibAstNode::toASTString).toList()),
            entryCall.toASTString(pAAstNodeRepresentation),
            Joiner.on(" ").join(steps.stream().map(SvLibAstNode::toASTString).toList()),
            violatedProperty.toASTString(pAAstNodeRepresentation),
            Joiner.on(" ").join(setTags.stream().map(SvLibAstNode::toASTString).toList()));
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  public List<SvLibTraceSetGlobalVariable> getSetGlobalVariables() {
    return setGlobalVariables;
  }

  public SvLibTraceEntryCall getEntryCall() {
    return entryCall;
  }

  public List<SvLibTraceStep> getSteps() {
    return steps;
  }

  public SvLibViolatedProperty getViolatedProperty() {
    return violatedProperty;
  }

  public List<SvLibTraceSetTag> getSetTags() {
    return setTags;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + setGlobalVariables.hashCode();
    result = prime * result + entryCall.hashCode();
    result = prime * result + steps.hashCode();
    result = prime * result + violatedProperty.hashCode();
    result = prime * result + setTags.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibTrace other
        && setGlobalVariables.equals(other.setGlobalVariables)
        && entryCall.equals(other.entryCall)
        && steps.equals(other.steps)
        && violatedProperty.equals(other.violatedProperty)
        && setTags.equals(other.setTags);
  }
}
