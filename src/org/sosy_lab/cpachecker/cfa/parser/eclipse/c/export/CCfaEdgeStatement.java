// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;

/** Statement representing a {@link CFAEdge} in a C-{@link org.sosy_lab.cpachecker.cfa.CFA}. */
abstract class CCfaEdgeStatement {

  private static final UniqueIdGenerator labelIdGenerator = new UniqueIdGenerator();

  private boolean isLabeled = false;
  private String label = null;

  private boolean isGoto = false;
  private String gotoTarget = null;

  private final Optional<CFAEdge> origin;

  private CCfaEdgeStatement(final CFAEdge pOrigin) {
    origin = Optional.ofNullable(pOrigin);

    if (origin.isEmpty()) {
      return;
    }

    // only label the IfStatement at branching points
    if (!(this instanceof ElseStatement) && pOrigin.getPredecessor() instanceof CFALabelNode) {
      isLabeled = true;
      label = ((CFALabelNode) pOrigin.getPredecessor()).getLabel();
    }
  }

  static String createNewLabelName() {
    return "label_" + labelIdGenerator.getFreshId();
  }

  Optional<String> getLabelIfLabeled() {
    if (!isLabeled) {
      return Optional.empty();
    } else {
      return Optional.of(label);
    }
  }

  String getOrCreateLabel() {
    if (!isLabeled) {
      label = createNewLabelName();
      isLabeled = true;
    }
    return label;
  }

  void addGotoTo(final String pLabel) {
    assert !isGoto : "An edge can not be a goto to two targets";
    gotoTarget = pLabel;
    isGoto = true;
  }

  String exportPotentialLabel() {
    if (isLabeled) {
      return label + ":;\n";
    }
    return "";
  }

  String exportPotentialGoto() {
    if (isGoto) {
      return "goto " + gotoTarget + ";\n";
    }
    return "";
  }

  abstract String exportToCCode();

  Optional<CFAEdge> getOrigin() {
    return origin;
  }

  @Override
  public String toString() {
    return "`" + exportToCCode() + "`";
  }

  static class SimpleCCfaEdgeStatement extends CCfaEdgeStatement {

    SimpleCCfaEdgeStatement(final CFAEdge pEdge) {
      super(pEdge);
      checkNotNull(pEdge);
    }

    @Override
    String exportToCCode() {
      final CFAEdge origin = getOrigin().orElseThrow();
      final String statement =
          origin.getRawAST().isPresent()
              ? origin.getRawAST().orElseThrow().toASTString() + "\n"
              : "";
      return exportPotentialLabel() + statement + exportPotentialGoto();
    }
  }

  static class GlobalDeclaration extends SimpleCCfaEdgeStatement {

    GlobalDeclaration(final CDeclarationEdge pEdge) {
      super(pEdge);
      checkNotNull(pEdge);
      checkArgument(pEdge.getDeclaration().isGlobal());
    }

    @Override
    String getOrCreateLabel() {
      throw new AssertionError("Global declarations can not be labeled.");
    }

    @Override
    void addGotoTo(final String pLabel) {
      throw new AssertionError("Global declarations can not be followed by gotos.");
    }

    @Override
    String exportToCCode() {
      assert exportPotentialGoto().isEmpty() : "Global declarations can not be labeled.";
      assert exportPotentialGoto().isEmpty() : "Global declarations can not be followed by gotos.";
      return super.exportToCCode();
    }
  }

  static class IfStatement extends CCfaEdgeStatement {

    IfStatement(final AssumeEdge pEdge) {
      super(pEdge);
      checkNotNull(pEdge);
    }

    @Override
    String exportToCCode() {
      final String condition = getOrigin().orElseThrow().getRawAST().orElseThrow().toASTString();
      return exportPotentialLabel() + "if (" + condition + ") {\n" + exportPotentialGoto();
    }
  }

  static class ElseStatement extends CCfaEdgeStatement {

    ElseStatement(final AssumeEdge pEdge) {
      super(pEdge);
      checkNotNull(pEdge);
    }

    @Override
    String getOrCreateLabel() {
      throw new AssertionError("The corresponding IfStatement should have been labeled.");
    }

    @Override
    String exportToCCode() {
      assert exportPotentialLabel().isEmpty()
          : "The corresponding IfStatement should have been labeled.";
      return "} else {\n" + exportPotentialGoto();
    }
  }

  static class ClosingBraceStatement extends CCfaEdgeStatement {

    ClosingBraceStatement() {
      super(null);
    }

    @Override
    String getOrCreateLabel() {
      throw new AssertionError(
          "ClosingBraceStatement can not be labeled, because it does not correspond to any"
              + " CFAEdge.");
    }

    @Override
    void addGotoTo(final String pLabel) {
      throw new AssertionError(
          "ClosingBraceStatement can not be followed by a goto, because it does not correspond"
              + " to any CFAEdge.");
    }

    @Override
    String exportToCCode() {
      return "}\n";
    }
  }

  static class EmptyCCfaEdgeStatement extends CCfaEdgeStatement {

    EmptyCCfaEdgeStatement(final CFAEdge pEdge) {
      super(pEdge);
      checkNotNull(pEdge);
    }

    @Override
    String exportToCCode() {
      return exportPotentialLabel() + exportPotentialGoto();
    }
  }
}
