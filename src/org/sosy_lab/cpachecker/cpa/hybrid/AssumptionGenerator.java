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
package org.sosy_lab.cpachecker.cpa.hybrid;

import javax.annotation.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.HybridValueProvider;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.HybridValueTransformer;
import org.sosy_lab.cpachecker.cpa.hybrid.exception.InvalidAssumptionException;
import org.sosy_lab.cpachecker.cpa.hybrid.visitor.HybridValueArraySubscriptExpressionTransformer;
import org.sosy_lab.cpachecker.cpa.hybrid.visitor.HybridValueDeclarationTransformer;
import org.sosy_lab.cpachecker.cpa.hybrid.visitor.HybridValueExpressionTransformer;
import org.sosy_lab.cpachecker.cpa.hybrid.visitor.HybridValueIdExpressionTransformer;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

public class AssumptionGenerator {

  private final HybridValueTransformer<CExpression, CDeclaration> declarationTransformer;
  private final HybridValueExpressionTransformer<CIdExpression> idExpressionTransformer;
  private final HybridValueExpressionTransformer<CArraySubscriptExpression> arraySubscriptExpressionTransformer;
  private final HybridValueProvider valueProvider;

  public AssumptionGenerator(
      MachineModel pMachineModel,
      LogManager pLogger,
      HybridValueProvider pValueProvider) {

    declarationTransformer = new HybridValueDeclarationTransformer(pMachineModel, pLogger);
    idExpressionTransformer = new HybridValueIdExpressionTransformer(pMachineModel, pLogger);
    arraySubscriptExpressionTransformer
        = new HybridValueArraySubscriptExpressionTransformer(pMachineModel, pLogger);
    valueProvider = pValueProvider;
  }

  /**
   * Tries to generate a assumption for the ast node
   * Provides a hybrid value for the given type
   * @param pCAstNode
   * @return
   */
  @Nullable
  public CExpression generateAssumption(CAstNode pCAstNode)
      throws InvalidAssumptionException {

    if(pCAstNode instanceof CDeclaration) {
      return handleDeclaration((CDeclaration) pCAstNode);
    }
    if(pCAstNode instanceof CIdExpression) {
      return handleIdExpression((CIdExpression) pCAstNode);
    }
    if(pCAstNode instanceof CArraySubscriptExpression) {
      handleArraySubscript((CArraySubscriptExpression) pCAstNode);
    }

    // ast node cannot be handled
    return null;
  }

  private CExpression handleDeclaration(CDeclaration pCDeclaration)
      throws InvalidAssumptionException {

    Value value = valueProvider.delegateVisit(pCDeclaration.getType());
    return declarationTransformer.transform(value, pCDeclaration, BinaryOperator.EQUALS);
  }

  private CExpression handleIdExpression(CIdExpression pCIdExpression)
      throws InvalidAssumptionException {

    Value value = valueProvider.delegateVisit(pCIdExpression.getExpressionType());
    return idExpressionTransformer.transform(value, pCIdExpression, BinaryOperator.EQUALS);
  }

  private CExpression handleArraySubscript(CArraySubscriptExpression pCArraySubscriptExpression)
      throws InvalidAssumptionException {

    Value value = valueProvider.delegateVisit(pCArraySubscriptExpression.getExpressionType());
    return arraySubscriptExpressionTransformer.transform(value, pCArraySubscriptExpression, BinaryOperator.EQUALS);
  }
}
