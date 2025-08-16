// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.k3;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.exceptions.ParserException;

public class K3ParserException extends ParserException {

  @Serial private static final long serialVersionUID = 2377475523222354924L;

  public K3ParserException(String pMsg) {
    super(pMsg, Language.K3);
  }

  public K3ParserException(Throwable pCause) {
    super(pCause, Language.K3);
  }

  public K3ParserException(String pMsg, Throwable pCause) {
    super(pMsg, pCause, Language.K3);
  }
}
