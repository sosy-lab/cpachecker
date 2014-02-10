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
package org.sosy_lab.cpachecker.cpa.assume;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

public class AssumeTransferRelation implements TransferRelation {

  private static Collection<? extends AbstractState> sUnconstrainedSingleton = Collections.singleton(UnconstrainedAssumeState.getInstance());

  private String mFunctionName;

  public AssumeTransferRelation(String pFunctionName) {
    mFunctionName = pFunctionName;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    if (pCfaEdge.getEdgeType().equals(CFAEdgeType.StatementEdge)) {
      CStatementEdge lEdge = (CStatementEdge)pCfaEdge;

      CStatement lExpression = lEdge.getStatement();

      if (lExpression instanceof CFunctionCallStatement) {
        CFunctionCallExpression lCallExpression = ((CFunctionCallStatement)lExpression).getFunctionCallExpression();

        if (lCallExpression.getFunctionNameExpression().toASTString().equals(mFunctionName)) {
          List<CExpression> lParameterExpressions = lCallExpression.getParameterExpressions();
          if (lParameterExpressions.size() != 1) {
            throw new UnrecognizedCCodeException("Function " + mFunctionName + " called with wrong number of arguments",
                                                 pCfaEdge, lCallExpression);
          }
          AssumeState lElement = new ConstrainedAssumeState(lParameterExpressions.get(0));

          return Collections.singleton(lElement);
        }
      }
    }

    return sUnconstrainedSingleton;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    return null;
  }

}
