// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.Language;

public class SvLibParserException extends ParserException {

  @Serial private static final long serialVersionUID = 2377475523222354924L;

  public SvLibParserException(String pMsg) {
    super(pMsg, Language.SVLIB);
  }

  public SvLibParserException(Throwable pCause) {
    super(pCause, Language.SVLIB);
  }

  public SvLibParserException(String pMsg, Throwable pCause) {
    super(pMsg, pCause, Language.SVLIB);
  }
}
