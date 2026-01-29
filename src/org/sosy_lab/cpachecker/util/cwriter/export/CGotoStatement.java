// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;

/** Represents a goto statement in C. Example: {@code goto label;}. */
public record CGotoStatement(CLabelStatement label) implements CExportStatement {

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "goto " + label.name() + ";";
  }
}
