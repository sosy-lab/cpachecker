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
package org.sosy_lab.cpachecker.cpa.assume;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

public class AssumeTransferRelation implements TransferRelation {

  private static Collection<? extends AbstractElement> sUnconstrainedSingleton = Collections.singleton(UnconstrainedAssumeElement.getInstance());

  private String mFunctionName;

  public AssumeTransferRelation(String pFunctionName) {
    mFunctionName = pFunctionName;
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    if (pCfaEdge.getEdgeType().equals(CFAEdgeType.StatementEdge)) {
      StatementEdge lEdge = (StatementEdge)pCfaEdge;

      IASTStatement lExpression = lEdge.getStatement();

      if (lExpression instanceof IASTFunctionCallStatement) {
        IASTFunctionCallExpression lCallExpression = ((IASTFunctionCallStatement)lExpression).getFunctionCallExpression();

        if (lCallExpression.getFunctionNameExpression().getRawSignature().equals(mFunctionName)) {
          List<IASTExpression> lParameterExpressions = lCallExpression.getParameterExpressions();
          if (lParameterExpressions.size() != 1) {
            throw new UnrecognizedCCodeException("Function " + mFunctionName + " called with wrong number of arguments",
                                                 pCfaEdge, lCallExpression);
          }
          AssumeElement lElement = new ConstrainedAssumeElement(lParameterExpressions.get(0));

          return Collections.singleton(lElement);
        }
      }
    }

    return sUnconstrainedSingleton;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    return null;
  }

}