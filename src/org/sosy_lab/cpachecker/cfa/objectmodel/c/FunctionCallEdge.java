/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.objectmodel.c;

import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.objectmodel.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import com.google.common.collect.ImmutableList;

public class FunctionCallEdge extends AbstractCFAEdge {

	private final List<IASTExpression> arguments;
	private final IASTStatement rawAST;
	private final CallToReturnEdge summaryEdge;

  public FunctionCallEdge (String pRawStatement, IASTStatement pRawAST,
      int pLineNumber, CFANode pPredecessor, FunctionDefinitionNode pSuccessor,
      List<IASTExpression> pArguments, CallToReturnEdge pSummaryEdge) {

    super(pRawStatement, pLineNumber, pPredecessor, pSuccessor);
    arguments = ImmutableList.copyOf(pArguments);
    rawAST = pRawAST;
    summaryEdge = pSummaryEdge;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.FunctionCallEdge;
  }

  public CallToReturnEdge getSummaryEdge() {
    return summaryEdge;
  }

  public List<IASTExpression> getArguments() {
    return arguments;
  }

  @Override
  public IASTStatement getRawAST() {
    return rawAST;
  }

  @Override
  public FunctionDefinitionNode getSuccessor() {
    // the constructor enforces that the successor is always a CFAFunctionDefinitionNode
    return (FunctionDefinitionNode)super.getSuccessor();
  }
}
