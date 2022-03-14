// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class ClangParserException extends ParserException {

  private static final long serialVersionUID = 623683591746357905L;

  public ClangParserException(final String pMsg) {
    super(pMsg, Language.LLVM);
  }

  public ClangParserException(final Throwable pCause) {
    super(pCause, Language.LLVM);
  }

  public ClangParserException(final String pMsg, Throwable pCause) {
    super(pMsg, pCause, Language.LLVM);
  }

  public ClangParserException(final String pMsg, CFAEdge pEdge) {
    super(pMsg, pEdge, Language.LLVM);
  }
}
