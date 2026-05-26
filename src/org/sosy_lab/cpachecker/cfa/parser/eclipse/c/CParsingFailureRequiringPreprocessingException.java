// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import java.io.Serial;
import org.sosy_lab.cpachecker.exceptions.CParserException;

public class CParsingFailureRequiringPreprocessingException extends CParserException {
  @Serial private static final long serialVersionUID = 3163884437173824573L;

  public CParsingFailureRequiringPreprocessingException(String pMsg) {
    super(pMsg);
  }
}
