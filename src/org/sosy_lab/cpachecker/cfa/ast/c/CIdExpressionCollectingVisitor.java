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
    extends DefaultCExpressionVisitor<Set<CIdExpression>, RuntimeException> {

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
  public Set<CIdExpression> visit(CIdExpression pE) {
    return Collections.singleton(pE);
  }
}
