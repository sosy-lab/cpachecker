// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;

/** Handles problems during CFA generation */
class CFAGenerationRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 6850681425709171716L;

  public CFAGenerationRuntimeException(String msg) {
    super(msg);
  }

  public CFAGenerationRuntimeException(Throwable cause) {
    super(cause.getMessage(), cause);
  }

  public CFAGenerationRuntimeException(String msg, CAstNode astNode) {
    this(
        astNode == null
            ? msg
            : (astNode.getFileLocation() + ": " + msg + ": " + astNode.toASTString()));
  }
}
