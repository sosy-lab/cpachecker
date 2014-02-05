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
package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;


public class CIdExpressionCollectingVisitor implements
  CStatementVisitor<Set<CIdExpression>, RuntimeException>,
  CRightHandSideVisitor<Set<CIdExpression>, RuntimeException>,
  CInitializerVisitor<Set<CIdExpression>, RuntimeException>,
  CDesignatorVisitor<Set<CIdExpression>, RuntimeException>,
  CExpressionVisitor<Set<CIdExpression>, RuntimeException> {

  @Override
  public Set<CIdExpression> visit(CArraySubscriptExpression pE) throws RuntimeException {
    return Sets.union(
        pE.getArrayExpression().accept(this),
        pE.getSubscriptExpression().accept(this));
  }

  @Override
  public Set<CIdExpression> visit(CBinaryExpression pE) throws RuntimeException {
    return Sets.union(
        pE.getOperand1().accept(this),
        pE.getOperand2().accept(this));
  }

  @Override
  public Set<CIdExpression> visit(CCastExpression pE) throws RuntimeException {
    return pE.getOperand().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CComplexCastExpression pE) throws RuntimeException {
    return pE.getOperand().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CFieldReference pE) throws RuntimeException {
    return pE.getFieldOwner().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CPointerExpression pE) throws RuntimeException {
    return pE.getOperand().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CUnaryExpression pE) throws RuntimeException {
    return pE.getOperand().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CInitializerExpression pE) throws RuntimeException {
    return pE.getExpression().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CInitializerList pI) throws RuntimeException {
    Set<CIdExpression> result = Collections.emptySet();
    for (CInitializer i: pI.getInitializers()) {
      result = Sets.union(result,  i.accept(this));
    }
    return result;
  }

  @Override
  public Set<CIdExpression> visit(CDesignatedInitializer pI) throws RuntimeException {
    Set<CIdExpression> result = Collections.emptySet();
    for (CDesignator d: pI.getDesignators()) {
      result = Sets.union(result, d.accept(this));
    }
    if (pI.getRightHandSide() != null) {
      result = Sets.union(result, pI.getRightHandSide().accept(this));
    }
    return result;
  }

  @Override
  public Set<CIdExpression> visit(CExpressionAssignmentStatement pS) throws RuntimeException {
    return Sets.union(
        pS.getLeftHandSide().accept(this),
        pS.getRightHandSide().accept(this));
  }

  @Override
  public Set<CIdExpression> visit(CExpressionStatement pS) throws RuntimeException {
    return pS.getExpression().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CFunctionCallAssignmentStatement pS) throws RuntimeException {
    Set<CIdExpression> result = Sets.union(
        pS.getLeftHandSide().accept(this),
        pS.getRightHandSide().getFunctionNameExpression().accept(this));

    for (CExpression expr : pS.getRightHandSide().getParameterExpressions()) {
      result = Sets.union(result, expr.accept(this));
    }

    return result;
  }

  @Override
  public Set<CIdExpression> visit(CFunctionCallStatement pS) throws RuntimeException {
    return pS.getFunctionCallExpression().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CIdExpression pE) throws RuntimeException {
    return Collections.singleton(pE);
  }

  @Override
  public Set<CIdExpression> visit(CCharLiteralExpression pE) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<CIdExpression> visit(CFloatLiteralExpression pE) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<CIdExpression> visit(CIntegerLiteralExpression pE) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<CIdExpression> visit(CStringLiteralExpression pE) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<CIdExpression> visit(CTypeIdExpression pE) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<CIdExpression> visit(CTypeIdInitializerExpression pE) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<CIdExpression> visit(CImaginaryLiteralExpression pE) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<CIdExpression> visit(CArrayDesignator pArrayDesignator) throws RuntimeException {
    return pArrayDesignator.getSubscriptExpression().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CArrayRangeDesignator pArrayRangeDesignator) throws RuntimeException {
    return Sets.union(
        pArrayRangeDesignator.getFloorExpression().accept(this),
        pArrayRangeDesignator.getCeilExpression().accept(this));
  }

  @Override
  public Set<CIdExpression> visit(CFieldDesignator pFieldDesignator) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<CIdExpression> visit(CFunctionCallExpression pIastFunctionCallExpression) throws RuntimeException {
    Set<CIdExpression> result = Collections.emptySet();
    for (CExpression e: pIastFunctionCallExpression.getParameterExpressions()) {
      Sets.union(result, e.accept(this));
    }
    return result;
  }


}
