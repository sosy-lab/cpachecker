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

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

import java.util.Collections;
import java.util.Set;


public class FileLocationCollectingVisitor implements CAstNodeVisitor<Set<FileLocation>, RuntimeException>{

  @Override
  public Set<FileLocation> visit(CArraySubscriptExpression pE) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Sets.union(
        Collections.singleton(pE.getFileLocation()),
        Sets.union(
            pE.getArrayExpression().<Set<FileLocation>, RuntimeException>accept(this),
            pE.getSubscriptExpression().<Set<FileLocation>, RuntimeException>accept(this)));
  }

  @Override
  public Set<FileLocation> visit(CBinaryExpression pE) {
    return Sets.union(
        Collections.singleton(pE.getFileLocation()),
        // Do not remove explicit type inference, otherwise build fails with IntelliJ
        Sets.union(
            pE.getOperand1().<Set<FileLocation>, RuntimeException>accept(this),
            pE.getOperand2().<Set<FileLocation>, RuntimeException>accept(this)));
  }

  @Override
  public Set<FileLocation> visit(CCastExpression pE) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Sets.union(
        Collections.singleton(pE.getFileLocation()),
        pE.getOperand().<Set<FileLocation>, RuntimeException>accept(this));
  }

  @Override
  public Set<FileLocation> visit(CComplexCastExpression pE) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Sets.union(
        Collections.singleton(pE.getFileLocation()),
        pE.getOperand().<Set<FileLocation>, RuntimeException>accept(this));
  }

  @Override
  public Set<FileLocation> visit(CFieldReference pE) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Sets.union(
        Collections.singleton(pE.getFileLocation()),
        pE.getFieldOwner().<Set<FileLocation>, RuntimeException>accept(this));
  }

  @Override
  public Set<FileLocation> visit(CPointerExpression pE) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Sets.union(
        Collections.singleton(pE.getFileLocation()),
        pE.getOperand().<Set<FileLocation>, RuntimeException>accept(this));
  }

  @Override
  public Set<FileLocation> visit(CUnaryExpression pE) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Sets.union(
        Collections.singleton(pE.getFileLocation()),
        pE.getOperand().<Set<FileLocation>, RuntimeException>accept(this));
  }

  @Override
  public Set<FileLocation> visit(CInitializerExpression pE) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Sets.union(
        Collections.singleton(pE.getFileLocation()),
        pE.getExpression().<Set<FileLocation>, RuntimeException>accept(this));
  }

  @Override
  public Set<FileLocation> visit(CInitializerList pI) {
    Set<FileLocation> result = Sets.newHashSet();
    result.add(pI.getFileLocation());

    for (CInitializer i: pI.getInitializers()) {
      result.add(i.getFileLocation());
      // Do not remove explicit type inference, otherwise build fails with IntelliJ
      result.addAll(i.<Set<FileLocation>, RuntimeException>accept(this));
    }

    return result;
  }

  @Override
  public Set<FileLocation> visit(CDesignatedInitializer pI) {
    return Collections.singleton(pI.getFileLocation());
  }

  @Override
  public Set<FileLocation> visit(CExpressionAssignmentStatement pS) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Sets.union(
        Collections.singleton(pS.getFileLocation()),
        Sets.union(
            pS.getLeftHandSide().<Set<FileLocation>, RuntimeException>accept(this),
            pS.getRightHandSide().<Set<FileLocation>, RuntimeException>accept(this)));
  }

  @Override
  public Set<FileLocation> visit(CExpressionStatement pS) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Sets.union(
        Collections.singleton(pS.getFileLocation()),
        pS.getExpression().<Set<FileLocation>, RuntimeException>accept(this));
  }

  @Override
  public Set<FileLocation> visit(CFunctionCallAssignmentStatement pS) {
    Set<FileLocation> result = Sets.newHashSet();
    result.add(pS.getFileLocation());
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    result.addAll(pS.getLeftHandSide().<Set<FileLocation>, RuntimeException>accept(this));
    result.addAll(pS.getRightHandSide().<Set<FileLocation>, RuntimeException>accept(this));
    return result;
  }

  @Override
  public Set<FileLocation> visit(CFunctionCallStatement pS) {
    return Collections.singleton(pS.getFileLocation());
  }

  @Override
  public Set<FileLocation> visit(CIdExpression pE) {
    return Collections.singleton(pE.getFileLocation());
  }

  @Override
  public Set<FileLocation> visit(CCharLiteralExpression pE) {
    return Collections.singleton(pE.getFileLocation());
  }

  @Override
  public Set<FileLocation> visit(CFloatLiteralExpression pE) {
    if (pE.getFileLocation() != null) {
      return Collections.singleton(pE.getFileLocation());
    }
    return Collections.emptySet();
  }

  @Override
  public Set<FileLocation> visit(CIntegerLiteralExpression pE) {
    if (pE.getFileLocation() != null) {
      return Collections.singleton(pE.getFileLocation());
    }
    return Collections.emptySet();
  }

  @Override
  public Set<FileLocation> visit(CStringLiteralExpression pE) {
    return Collections.singleton(pE.getFileLocation());
  }

  @Override
  public Set<FileLocation> visit(CTypeIdExpression pE) {
    return Collections.singleton(pE.getFileLocation());
  }

  @Override
  public Set<FileLocation> visit(CImaginaryLiteralExpression pE) {
    return Collections.singleton(pE.getFileLocation());
  }

  public Set<FileLocation> collectLocationsFrom(CExpression astNode) {
    return astNode.accept(this);
  }

  public Set<FileLocation> collectLocationsFrom(CDeclaration astNode) {
    return astNode.accept(this);
  }

  public Set<FileLocation> collectLocationsFrom(CStatement astNode) {
    return astNode.accept(this);
  }

  public Set<FileLocation> collectLocationsFrom(CInitializer astNode) {
    return astNode.accept(this);
  }

  @Override
  public Set<FileLocation> visit(CFunctionDeclaration astNode) {
    Set<FileLocation> result = Sets.newHashSet();
    result.add(astNode.getFileLocation());
    for (CParameterDeclaration expr : astNode.getParameters()) {
      result.addAll(expr.accept(this));
    }
    return result;
  }

  @Override
  public Set<FileLocation> visit(CVariableDeclaration astNode) {
    Set<FileLocation> result = Sets.newHashSet();
    result.add(astNode.getFileLocation());
    if (astNode.getInitializer() != null) {
      result.addAll(astNode.getInitializer().accept(this));
    }
    return result;
  }

  @Override
  public Set<FileLocation> visit(CComplexTypeDeclaration pDecl) {
    return Collections.singleton(pDecl.getFileLocation());
  }

  @Override
  public Set<FileLocation> visit(CParameterDeclaration pDecl) {
    return Collections.singleton(pDecl.getFileLocation());
  }

  @Override
  public Set<FileLocation> visit(org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator pDecl)
      {
    return Collections.singleton(pDecl.getFileLocation());
  }

  @Override
  public Set<FileLocation> visit(CArrayDesignator pNode) {
    Set<FileLocation> result = Collections.singleton(pNode.getFileLocation());
    if (pNode.getSubscriptExpression() != null) {
      // Do not remove explicit type inference, otherwise build fails with IntelliJ
      result =
          Sets.union(
              result,
              pNode.getSubscriptExpression().<Set<FileLocation>, RuntimeException>accept(this));
    }
    return result;
  }

  @Override
  public Set<FileLocation> visit(CArrayRangeDesignator pNode) {
    Set<FileLocation> result = Collections.singleton(pNode.getFileLocation());
    if (pNode.getCeilExpression() != null) {
      // Do not remove explicit type inference, otherwise build fails with IntelliJ
      result =
          Sets.union(
              result, pNode.getCeilExpression().<Set<FileLocation>, RuntimeException>accept(this));
    }
    return result;
  }

  @Override
  public Set<FileLocation> visit(CFieldDesignator pNode) {
    return Collections.singleton(pNode.getFileLocation());
  }

  @Override
  public Set<FileLocation> visit(CReturnStatement pNode) {
    Set<FileLocation> result = Collections.singleton(pNode.getFileLocation());
    if (pNode.getReturnValue().isPresent()) {
      // Do not remove explicit type inference, otherwise build fails with IntelliJ
      result =
          Sets.union(
              result,
              pNode.getReturnValue().get().<Set<FileLocation>, RuntimeException>accept(this));
    }
    return result;
  }

  @Override
  public Set<FileLocation> visit(CFunctionCallExpression pNode) {
    Set<FileLocation> result = Sets.newHashSet();
    result.add(pNode.getFileLocation());
    result.addAll(pNode.getFunctionNameExpression().accept(this));
    for (CExpression expr : pNode.getParameterExpressions()) {
      // Do not remove explicit type inference, otherwise build fails with IntelliJ
      result.addAll(expr.<Set<FileLocation>, RuntimeException>accept(this));
    }
    return result;
  }

  @Override
  public Set<FileLocation> visit(CTypeDefDeclaration pNode) {
    return Collections.singleton(pNode.getFileLocation());
  }

  @Override
  public Set<FileLocation> visit(CAddressOfLabelExpression pNode) {
    return Collections.singleton(pNode.getFileLocation());
  }
}
