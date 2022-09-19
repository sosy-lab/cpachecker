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

public class JParserException extends ParserException {

  private static final long serialVersionUID = 2377445523222164635L;

  public JParserException(String pMsg) {
    super(pMsg, Language.JAVA);
  }

  public JParserException(Throwable pCause) {
    super(pCause, Language.JAVA);
  }

  public JParserException(String pMsg, CFAEdge pEdge) {
    super(pMsg, pEdge, Language.JAVA);
  }
}
