/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public final class AutomatonTransferException extends CPATransferException {

  private static final long serialVersionUID = 3470772694494993317L;

  AutomatonTransferException(String pMsg) {
    super(pMsg);
  }

  AutomatonTransferException(String pMsg, ResultValue<?> pResult) {
    super(pMsg + ": " + pResult.getFailureMessage());
  }

  AutomatonTransferException(String pMsg, Throwable pCause) {
    super(pMsg, pCause);
  }
}
