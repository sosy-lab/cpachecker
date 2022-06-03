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

public class CParserException extends ParserException {

  private static final long serialVersionUID = 2377475523222354924L;

  public CParserException(String pMsg) {
    super(pMsg, Language.C);
  }

  public CParserException(Throwable pCause) {
    super(pCause, Language.C);
  }

  public CParserException(String pMsg, Throwable pCause) {
    super(pMsg, pCause, Language.C);
  }

  public CParserException(String pMsg, CFAEdge pEdge) {
    super(pMsg, pEdge, Language.C);
  }
}
