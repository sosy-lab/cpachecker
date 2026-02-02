// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public interface SeqASTNode {

  default String toASTString() throws UnrecognizedCodeException {
    return toASTString(AAstNodeRepresentation.DEFAULT);
  }

  /**
   * It is generally preferred to lazily (i.e. inside this function implementation) initialize
   * objects such as {@link CStatement}s even if they are only dependent on {@code final} variables.
   */
  String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException;
}
