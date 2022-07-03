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
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;

abstract class ExportStatement {

  private static final UniqueIdGenerator labelIdGenerator = new UniqueIdGenerator();

  private boolean isGotoTarget = false;
  private String gotoLabel = null;

  protected final CFAEdge origin;

  private ExportStatement(final CFAEdge pOrigin) {
    origin = pOrigin;
  }

  private static String createNewLabelName() {
    return "label_" + labelIdGenerator.getFreshId();
  }

  Optional<String> getLabelIfGotoTarget() {
    if (!isGotoTarget) {
      return Optional.empty();
    } else {
      return Optional.of(gotoLabel);
    }
  }

  String getOrCreateLabel() {
    if (!isGotoTarget) {
      gotoLabel = createNewLabelName();
      isGotoTarget = true;
    }
    return gotoLabel;
  }

  abstract String exportToCCode();

  static class SimpleStatement extends ExportStatement {

    SimpleStatement(final CFAEdge pEdge) {
      super(pEdge);
    }

    @Override
    String exportToCCode() {
      return origin.getRawAST().isPresent() ? origin.getRawAST().orElseThrow().toASTString() : "";
    }
  }

  static class GlobalDeclaration extends SimpleStatement {

    GlobalDeclaration(final CDeclarationEdge pEdge) {
      super(pEdge);
      checkArgument(pEdge.getDeclaration().isGlobal());
    }
  }

  static class IfStatement extends ExportStatement {

    IfStatement(final AssumeEdge pEdge) {
      super(pEdge);
    }

    @Override
    String exportToCCode() {
      return "if (" + origin.getRawAST().orElseThrow().toASTString() + ") {";
    }
  }

  static class ElseStatement extends ExportStatement {

    ElseStatement(final AssumeEdge pEdge) {
      super(pEdge);
    }

    @Override
    String exportToCCode() {
      return "} else {";
    }
  }

  static class PlaceholderStatement extends ExportStatement {

    PlaceholderStatement(final CFAEdge pEdge) {
      super(pEdge);
    }

    @Override
    String exportToCCode() {
      return "";
    }
  }
}
