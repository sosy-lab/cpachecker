// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.specification;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibAstNode;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibAstNodeVisitor;

public final class SvLibTrace implements SvLibAstNode {

  @Serial private static final long serialVersionUID = 6210509388439561283L;
  private final SmtLibModel model;
  private final ImmutableList<SvLibTraceSetGlobalVariable> setGlobalVariables;
  private final SvLibTraceEntryProcedure entryProc;
  private final ImmutableList<SvLibTraceStep> steps;
  private final SvLibViolatedProperty violatedProperty;
  private final ImmutableList<SvLibTraceUsingAnnotation> usingAnnotations;
  private final FileLocation fileLocation;

  public SvLibTrace(
      SmtLibModel pModel,
      List<SvLibTraceSetGlobalVariable> pSetGlobalVariables,
      SvLibTraceEntryProcedure pEntryProc,
      List<SvLibTraceStep> pSteps,
      SvLibViolatedProperty pViolatedProperty,
      List<SvLibTraceUsingAnnotation> pUsingAnnotations,
      FileLocation pFileLocation) {
    model = pModel;
    setGlobalVariables = ImmutableList.copyOf(pSetGlobalVariables);
    entryProc = pEntryProc;
    steps = ImmutableList.copyOf(pSteps);
    violatedProperty = pViolatedProperty;
    usingAnnotations = ImmutableList.copyOf(pUsingAnnotations);
    fileLocation = pFileLocation;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return Joiner.on(System.lineSeparator())
        .join(
            "(model " + model.toASTString(pAAstNodeRepresentation) + ")",
            "(init-global-vars "
                + Joiner.on(" ")
                    .join(setGlobalVariables.stream().map(SvLibAstNode::toASTString).toList())
                + ")",
            entryProc.toASTString(pAAstNodeRepresentation),
            "(steps "
                + Joiner.on(" ").join(steps.stream().map(SvLibAstNode::toASTString).toList())
                + ")",
            violatedProperty.toASTString(pAAstNodeRepresentation),
            Joiner.on(System.lineSeparator())
                .join(usingAnnotations.stream().map(SvLibAstNode::toASTString).toList()));
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  public ImmutableList<SvLibTraceSetGlobalVariable> getSetGlobalVariables() {
    return setGlobalVariables;
  }

  public SmtLibModel getModel() {
    return model;
  }

  public SvLibTraceEntryProcedure getEntryProc() {
    return entryProc;
  }

  public ImmutableList<SvLibTraceStep> getSteps() {
    return steps;
  }

  public SvLibViolatedProperty getViolatedProperty() {
    return violatedProperty;
  }

  public ImmutableList<SvLibTraceUsingAnnotation> getUsingAnnotations() {
    return usingAnnotations;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + model.hashCode();
    result = prime * result + setGlobalVariables.hashCode();
    result = prime * result + entryProc.hashCode();
    result = prime * result + steps.hashCode();
    result = prime * result + violatedProperty.hashCode();
    result = prime * result + usingAnnotations.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibTrace other
        && model.equals(other.model)
        && setGlobalVariables.equals(other.setGlobalVariables)
        && entryProc.equals(other.entryProc)
        && steps.equals(other.steps)
        && violatedProperty.equals(other.violatedProperty)
        && usingAnnotations.equals(other.usingAnnotations);
  }
}
