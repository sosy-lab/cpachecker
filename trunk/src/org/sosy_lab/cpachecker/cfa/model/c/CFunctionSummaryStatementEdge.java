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
package org.sosy_lab.cpachecker.cfa.model.c;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class CFunctionSummaryStatementEdge extends CStatementEdge {
  private final String functionName;
  private final CFunctionCall fcall;

  public CFunctionSummaryStatementEdge(String pRawStatement,
      CStatement pStatement, FileLocation pFileLocation, CFANode pPredecessor,
      CFANode pSuccessor, CFunctionCall fcall, String functionName) {
    super(pRawStatement, pStatement, pFileLocation, pPredecessor, pSuccessor);
    this.functionName = checkNotNull(functionName);
    this.fcall = checkNotNull(fcall);
  }

  public String getFunctionName() {
    return functionName;
  }

  public CFunctionCall getFunctionCall() {
    return fcall;
  }
}