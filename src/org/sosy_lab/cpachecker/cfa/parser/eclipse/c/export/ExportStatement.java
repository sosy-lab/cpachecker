// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;

abstract class ExportStatement {

  private static final UniqueIdGenerator labelIdGenerator = new UniqueIdGenerator();

  private boolean isLabeled = false;
  private String label = null;

  private boolean isGoto = false;
  private String gotoTarget = null;

  private final CFAEdge origin;

  private ExportStatement(final CFAEdge pOrigin) {
    origin = pOrigin;

    // only label the IfStatement at branching points
    if (!(this instanceof ElseStatement) && origin.getPredecessor() instanceof CFALabelNode) {
      isLabeled = true;
      label = ((CFALabelNode) origin.getPredecessor()).getLabel();
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

  CFAEdge getOrigin() {
    return origin;
  }

  static class SimpleStatement extends ExportStatement {

    SimpleStatement(final CFAEdge pEdge) {
      super(pEdge);
    }

    @Override
    String exportToCCode() {
      final String statement =
          getOrigin().getRawAST().isPresent()
              ? getOrigin().getRawAST().orElseThrow().toASTString() + "\n"
              : "";
      return exportPotentialLabel() + statement + exportPotentialGoto();
    }
  }

  static class GlobalDeclaration extends SimpleStatement {

    GlobalDeclaration(final CDeclarationEdge pEdge) {
      super(pEdge);
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

  static class IfStatement extends ExportStatement {

    IfStatement(final AssumeEdge pEdge) {
      super(pEdge);
    }

    @Override
    String exportToCCode() {
      final String condition = getOrigin().getRawAST().orElseThrow().toASTString();
      return exportPotentialLabel() + "if (" + condition + ") {\n" + exportPotentialGoto();
    }
  }

  static class ElseStatement extends ExportStatement {

    ElseStatement(final AssumeEdge pEdge) {
      super(pEdge);
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

  static class PlaceholderStatement extends ExportStatement {

    PlaceholderStatement(final CFAEdge pEdge) {
      super(pEdge);
    }

    @Override
    String exportToCCode() {
      return exportPotentialLabel() + exportPotentialGoto();
    }
  }
}
