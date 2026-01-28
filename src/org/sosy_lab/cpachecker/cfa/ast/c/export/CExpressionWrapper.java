// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c.export;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;

public record CExpressionWrapper(CExpression expression) implements CExportExpression {

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return expression.toASTString(pAAstNodeRepresentation);
  }
}
