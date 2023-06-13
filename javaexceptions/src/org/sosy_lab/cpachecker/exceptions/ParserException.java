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

/**
 * Exception thrown if an error occurs during parsing step (e.g. because the parser library throws
 * an exception).
 */
public class ParserException extends Exception {

  private static final long serialVersionUID = 2377475523222364935L;

  private final Language language;

  protected ParserException(String msg, Language pLanguage) {
    super(msg);
    language = pLanguage;
  }

  protected ParserException(Throwable cause, Language pLanguage) {
    super(cause.getMessage(), cause);
    language = pLanguage;
  }

  protected ParserException(String msg, Throwable cause, Language pLanguage) {
    super(msg + ": " + cause.getMessage(), cause);
    language = pLanguage;
  }

  protected ParserException(String msg, CFAEdge edge, Language pLanguage) {
    super(UnrecognizedCodeException.createMessage(msg, null, edge, null));
    language = pLanguage;
  }

  public Language getLanguage() {
    return language;
  }
}
