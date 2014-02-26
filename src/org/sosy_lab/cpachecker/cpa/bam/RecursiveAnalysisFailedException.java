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
package org.sosy_lab.cpachecker.cpa.bam;

import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

class RecursiveAnalysisFailedException extends CPATransferException {

  private static final long serialVersionUID = 3822584071233172171L;

  private int depth;

  public RecursiveAnalysisFailedException(CPAException e) {
    super(createMessage(e));

    if (e instanceof RecursiveAnalysisFailedException) {
      RecursiveAnalysisFailedException recursiveException = (RecursiveAnalysisFailedException)e;
      initCause(recursiveException.getCause());
      depth = recursiveException.depth + 1;
    } else {
      initCause(e);
      depth = 1;
    }
  }

  private static String createMessage(CPAException e) {
    if (e instanceof RecursiveAnalysisFailedException) {
      RecursiveAnalysisFailedException r = (RecursiveAnalysisFailedException)e;
      return "Error in recursive analysis at depth " + r.depth + ": " + r.getCause().getMessage();
    } else {
      return "Error in recursive analysis at depth 1: " + e.getMessage();
    }
  }

  @Override
  public CPAException getCause() {
    return (CPAException)super.getCause();
  }
}
