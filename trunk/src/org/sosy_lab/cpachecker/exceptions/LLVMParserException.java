// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class LLVMParserException extends ParserException {

  public static final long serialVersionUID = 0;

  public LLVMParserException(final String pMsg) {
    super(pMsg, Language.LLVM);
  }

  public LLVMParserException(final Throwable pCause) {
    super(pCause, Language.LLVM);
  }

  public LLVMParserException(final String pMsg, Throwable pCause) {
    super(pMsg, pCause, Language.LLVM);
  }

  public LLVMParserException(final String pMsg, CFAEdge pEdge) {
    super(pMsg, pEdge, Language.LLVM);
  }
}
