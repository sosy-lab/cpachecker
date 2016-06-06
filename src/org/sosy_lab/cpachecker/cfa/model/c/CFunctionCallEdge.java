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

import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;

import java.util.Optional;

public class CFunctionCallEdge extends FunctionCallEdge {



  public CFunctionCallEdge(String pRawStatement,
      FileLocation pFileLocation, CFANode pPredecessor, CFunctionEntryNode pSuccessor,
      CFunctionCall pFunctionCall, CFunctionSummaryEdge pSummaryEdge) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pFunctionCall, pSummaryEdge);

  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.FunctionCallEdge;
  }

  @Override
  public CFunctionSummaryEdge getSummaryEdge() {
    return (CFunctionSummaryEdge) summaryEdge;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<CExpression> getArguments() {
    return (List<CExpression>) functionCall.getFunctionCallExpression().getParameterExpressions();
  }

  @Override
  public String getCode() {
    return functionCall.getFunctionCallExpression().toASTString();
  }

  @Override
  public Optional<CFunctionCall> getRawAST() {
    return Optional.of((CFunctionCall)functionCall);
  }

  @Override
  public CFunctionEntryNode getSuccessor() {
    // the constructor enforces that the successor is always a FunctionEntryNode
    return (CFunctionEntryNode)super.getSuccessor();
  }
}