// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;

public record CWrapperStatement(CStatement statement) implements CAstStatement {

  @Override
  public String toASTString() {
    return statement.toASTString();
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return statement.toASTString(pAAstNodeRepresentation);
  }
}
