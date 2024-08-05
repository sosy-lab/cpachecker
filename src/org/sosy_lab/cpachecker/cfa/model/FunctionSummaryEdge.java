// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class FunctionSummaryEdge extends AbstractCFAEdge {

  private static final long serialVersionUID = -99016347135694310L;
  private final AFunctionCall expression;
  private final FunctionEntryNode functionEntry;

  protected FunctionSummaryEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      AFunctionCall pExpression,
      FunctionEntryNode pFunctionEntry) {
    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
    expression = pExpression;
    functionEntry = checkNotNull(pFunctionEntry);
  }

  public AFunctionCall getExpression() {
    return expression;
  }

  public FunctionEntryNode getFunctionEntry() {
    return functionEntry;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.CallToReturnEdge;
  }

  @Override
  public String getCode() {
    return expression.toASTString();
  }
}
