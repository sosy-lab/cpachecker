/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.exceptions;

import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Exception thrown if an error occurs during parsing step (e.g. because the
 * parser library throws an exception).
 */
public  class ParserException extends Exception {

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
