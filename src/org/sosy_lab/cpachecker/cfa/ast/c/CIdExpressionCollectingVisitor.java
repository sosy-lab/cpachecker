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

import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Set;


public class CIdExpressionCollectingVisitor
    extends DefaultCExpressionVisitor<Set<CIdExpression>, RuntimeException>
    implements CStatementVisitor<Set<CIdExpression>, RuntimeException>,
               CRightHandSideVisitor<Set<CIdExpression>, RuntimeException>,
               CInitializerVisitor<Set<CIdExpression>, RuntimeException>,
               CDesignatorVisitor<Set<CIdExpression>, RuntimeException> {

  @Override
  protected Set<CIdExpression> visitDefault(CExpression pExp) {
    return Collections.emptySet();
  }

  @Override
  public Set<CIdExpression> visit(CArraySubscriptExpression pE) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Sets.union(
        pE.getArrayExpression().<Set<CIdExpression>, RuntimeException>accept(this),
        pE.getSubscriptExpression().<Set<CIdExpression>, RuntimeException>accept(this));
  }

  @Override
  public Set<CIdExpression> visit(CBinaryExpression pE) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Sets.union(
        pE.getOperand1().<Set<CIdExpression>, RuntimeException>accept(this),
        pE.getOperand2().<Set<CIdExpression>, RuntimeException>accept(this));
  }

  @Override
  public Set<CIdExpression> visit(CCastExpression pE) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return pE.getOperand().<Set<CIdExpression>, RuntimeException>accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CComplexCastExpression pE) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return pE.getOperand().<Set<CIdExpression>, RuntimeException>accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CFieldReference pE) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return pE.getFieldOwner().<Set<CIdExpression>, RuntimeException>accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CPointerExpression pE) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return pE.getOperand().<Set<CIdExpression>, RuntimeException>accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CUnaryExpression pE) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return pE.getOperand().<Set<CIdExpression>, RuntimeException>accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CInitializerExpression pE) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return pE.getExpression().<Set<CIdExpression>, RuntimeException>accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CInitializerList pI) {
    Set<CIdExpression> result = Collections.emptySet();
    for (CInitializer i: pI.getInitializers()) {
      // Do not remove explicit type inference, otherwise build fails with IntelliJ
      result = Sets.union(result, i.<Set<CIdExpression>, RuntimeException>accept(this));
    }
    return result;
  }

  @Override
  public Set<CIdExpression> visit(CDesignatedInitializer pI) {
    Set<CIdExpression> result = Collections.emptySet();
    for (CDesignator d: pI.getDesignators()) {
      // Do not remove explicit type inference, otherwise build fails with IntelliJ
      result = Sets.union(result, d.<Set<CIdExpression>, RuntimeException>accept(this));
    }
    return result;
  }

  @Override
  public Set<CIdExpression> visit(CExpressionAssignmentStatement pS) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Sets.union(
        pS.getLeftHandSide().<Set<CIdExpression>, RuntimeException>accept(this),
        pS.getRightHandSide().<Set<CIdExpression>, RuntimeException>accept(this));
  }

  @Override
  public Set<CIdExpression> visit(CExpressionStatement pS) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return pS.getExpression().<Set<CIdExpression>, RuntimeException>accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CFunctionCallAssignmentStatement pS) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    Set<CIdExpression> result =
        Sets.union(
            pS.getLeftHandSide().<Set<CIdExpression>, RuntimeException>accept(this),
            pS.getRightHandSide()
                .getFunctionNameExpression()
                .<Set<CIdExpression>, RuntimeException>accept(this));

    for (CExpression expr : pS.getRightHandSide().getParameterExpressions()) {
      result = Sets.union(result, expr.<Set<CIdExpression>, RuntimeException>accept(this));
    }
    return result;
  }

  @Override
  public Set<CIdExpression> visit(CFunctionCallStatement pS) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return pS.getFunctionCallExpression().<Set<CIdExpression>, RuntimeException>accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CIdExpression pE) {
    return Collections.singleton(pE);
  }

  @Override
  public Set<CIdExpression> visit(CArrayDesignator pArrayDesignator) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return pArrayDesignator
        .getSubscriptExpression()
        .<Set<CIdExpression>, RuntimeException>accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CArrayRangeDesignator pArrayRangeDesignator) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Sets.union(
        pArrayRangeDesignator
            .getFloorExpression()
            .<Set<CIdExpression>, RuntimeException>accept(this),
        pArrayRangeDesignator
            .getCeilExpression()
            .<Set<CIdExpression>, RuntimeException>accept(this));
  }

  @Override
  public Set<CIdExpression> visit(CFieldDesignator pFieldDesignator) {
    return Collections.emptySet();
  }

  @Override
  public Set<CIdExpression> visit(CFunctionCallExpression pIastFunctionCallExpression) {
    Set<CIdExpression> result = Collections.emptySet();
    for (CExpression e: pIastFunctionCallExpression.getParameterExpressions()) {
      // Do not remove explicit type inference, otherwise build fails with IntelliJ
      result = Sets.union(result, e.<Set<CIdExpression>, RuntimeException>accept(this));
    }
    return result;
  }
}
