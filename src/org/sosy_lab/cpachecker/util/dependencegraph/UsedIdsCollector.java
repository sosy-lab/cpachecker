/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.base.Optional;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.expressions.IdExpressionCollector;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Returns all identifiers used in the visited tree. */
public class UsedIdsCollector
    implements CAstNodeVisitor<Set<MemoryLocation>, CPATransferException> {

  private final IdExpressionCollector idCollector = new IdExpressionCollector();

  @Override
  public Set<MemoryLocation> visit(CExpressionStatement pIastExpressionStatement)
      throws CPATransferException {
    return pIastExpressionStatement.getExpression().accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CExpressionAssignmentStatement pExp)
      throws CPATransferException {
    Set<MemoryLocation> lhs = handleLeftHandSide(pExp.getLeftHandSide());
    Set<MemoryLocation> rhs = pExp.getRightHandSide().accept(this);
    return combine(lhs, rhs);
  }

  private Set<MemoryLocation> combine(
      final Set<MemoryLocation> pLhs, final Set<MemoryLocation> pRhs) {
    Set<MemoryLocation> combined = new HashSet<>(pLhs);
    combined.addAll(pRhs);
    return combined;
  }

  @Override
  public Set<MemoryLocation> visit(CFunctionCallAssignmentStatement pAssignment)
      throws CPATransferException {
    Set<MemoryLocation> lhs = handleLeftHandSide(pAssignment.getLeftHandSide());
    Set<MemoryLocation> rhs = pAssignment.getRightHandSide().accept(this);

    return combine(lhs, rhs);
  }

  private Set<MemoryLocation> handleLeftHandSide(CLeftHandSide pLeftHandSide)
      throws CPATransferException {

    if (pLeftHandSide instanceof CIdExpression) {
      return Collections.emptySet();
    } else {
      return pLeftHandSide.accept(this);
    }
  }

  @Override
  public Set<MemoryLocation> visit(CFunctionCallStatement pCall) throws CPATransferException {
    Set<MemoryLocation> used = new HashSet<>();
    for (CExpression p : pCall.getFunctionCallExpression().getParameterExpressions()) {
      used.addAll(p.accept(idCollector));
    }
    return used;
  }

  @Override
  public Set<MemoryLocation> visit(CArrayDesignator pArrayDesignator) throws CPATransferException {
    return pArrayDesignator.getSubscriptExpression().accept(this);
  }

  @Override
  public Set<MemoryLocation> visit(CArrayRangeDesignator pArrayRangeDesignator)
      throws CPATransferException {
    Set<MemoryLocation> fst = pArrayRangeDesignator.getCeilExpression().accept(idCollector);
    Set<MemoryLocation> snd = pArrayRangeDesignator.getFloorExpression().accept(idCollector);
    return combine(fst, snd);
  }

  @Override
  public Set<MemoryLocation> visit(CFieldDesignator pFieldDesignator) throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<MemoryLocation> visit(CArraySubscriptExpression pExp) throws CPATransferException {
    Set<MemoryLocation> fst = pExp.getArrayExpression().accept(idCollector);
    Set<MemoryLocation> snd = pExp.getSubscriptExpression().accept(idCollector);

    return combine(fst, snd);
  }

  @Override
  public Set<MemoryLocation> visit(CFieldReference pIastFieldReference)
      throws CPATransferException {
    return pIastFieldReference.getFieldOwner().accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CIdExpression pIastIdExpression) throws CPATransferException {
    return pIastIdExpression.accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CPointerExpression pointerExpression)
      throws CPATransferException {
    return pointerExpression.getOperand().accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CComplexCastExpression complexCastExpression)
      throws CPATransferException {
    return complexCastExpression.getOperand().accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CInitializerExpression pInitializerExpression)
      throws CPATransferException {
    return pInitializerExpression.getExpression().accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CInitializerList pInitializerList) throws CPATransferException {
    Set<MemoryLocation> used = new HashSet<>();
    for (CInitializer i : pInitializerList.getInitializers()) {
      used.addAll(i.accept(this));
    }
    return used;
  }

  @Override
  public Set<MemoryLocation> visit(CDesignatedInitializer pCStructInitializerPart)
      throws CPATransferException {
    Set<MemoryLocation> used = new HashSet<>();
    used.addAll(pCStructInitializerPart.getRightHandSide().accept(this));
    for (CDesignator d : pCStructInitializerPart.getDesignators()) {
      used.addAll(d.accept(this));
    }

    return used;
  }

  @Override
  public Set<MemoryLocation> visit(CFunctionCallExpression pExp) throws CPATransferException {
    Set<MemoryLocation> used = new HashSet<>();
    used.addAll(pExp.getFunctionNameExpression().accept(idCollector));
    for (CExpression e : pExp.getParameterExpressions()) {
      used.addAll(e.accept(idCollector));
    }
    return used;
  }

  @Override
  public Set<MemoryLocation> visit(CBinaryExpression pExp) throws CPATransferException {
    return pExp.accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CCastExpression pExp) throws CPATransferException {
    return pExp.accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CCharLiteralExpression pExp) throws CPATransferException {
    return pExp.accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CFloatLiteralExpression pExp) throws CPATransferException {
    return pExp.accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CIntegerLiteralExpression pExp) throws CPATransferException {
    return pExp.accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CStringLiteralExpression pExp) throws CPATransferException {
    return pExp.accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CTypeIdExpression pExp) throws CPATransferException {
    return pExp.accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CUnaryExpression pExp) throws CPATransferException {
    return pExp.accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CImaginaryLiteralExpression pExp) throws CPATransferException {
    return pExp.accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CAddressOfLabelExpression pExp) throws CPATransferException {
    return pExp.accept(idCollector);
  }

  @Override
  public Set<MemoryLocation> visit(CFunctionDeclaration pDecl) throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<MemoryLocation> visit(CComplexTypeDeclaration pDecl) throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<MemoryLocation> visit(CTypeDefDeclaration pDecl) throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<MemoryLocation> visit(CVariableDeclaration pDecl) throws CPATransferException {
    return pDecl.getInitializer().accept(this);
  }

  @Override
  public Set<MemoryLocation> visit(CParameterDeclaration pDecl) throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<MemoryLocation> visit(CEnumerator pDecl) throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Set<MemoryLocation> visit(CReturnStatement pNode) throws CPATransferException {
    Optional<CExpression> maybeExp = pNode.getReturnValue();
    if (maybeExp.isPresent()) {
      return maybeExp.get().accept(idCollector);
    } else {
      return Collections.emptySet();
    }
  }
}
