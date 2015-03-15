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

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

import com.google.common.collect.Sets;


public class FileLocationCollectingVisitor implements CAstNodeVisitor<Set<FileLocation>, RuntimeException>{

  @Override
  public Set<FileLocation> visit(CArraySubscriptExpression pE) {
    return Sets.union(Collections.singleton(pE.getFileLocation()),
              Sets.union(pE.getArrayExpression().accept(this),
                  pE.getSubscriptExpression().accept(this)));
  }

  @Override
  public Set<FileLocation> visit(CBinaryExpression pE) {
    return Sets.union(Collections.singleton(pE.getFileLocation()),
        Sets.union(pE.getOperand1().accept(this),
            pE.getOperand2().accept(this)));
  }

  @Override
  public Set<FileLocation> visit(CCastExpression pE) {
    return Sets.union(Collections.singleton(pE.getFileLocation()), pE.getOperand().accept(this));
  }

  @Override
  public Set<FileLocation> visit(CComplexCastExpression pE) {
    return Sets.union(Collections.singleton(pE.getFileLocation()), pE.getOperand().accept(this));
  }

  @Override
  public Set<FileLocation> visit(CFieldReference pE) {
    return Sets.union(Collections.singleton(pE.getFileLocation()), pE.getFieldOwner().accept(this));
  }

  @Override
  public Set<FileLocation> visit(CPointerExpression pE) {
    return Sets.union(Collections.singleton(pE.getFileLocation()), pE.getOperand().accept(this));
  }

  @Override
  public Set<FileLocation> visit(CUnaryExpression pE) {
    return Sets.union(Collections.singleton(pE.getFileLocation()), pE.getOperand().accept(this));
  }

  @Override
  public Set<FileLocation> visit(CInitializerExpression pE) {
    return Sets.union(Collections.singleton(pE.getFileLocation()), pE.getExpression().accept(this));
  }

  @Override
  public Set<FileLocation> visit(CInitializerList pI) {
    Set<FileLocation> result = Sets.newHashSet();
    result.add(pI.getFileLocation());

    for (CInitializer i: pI.getInitializers()) {
      result.add(i.getFileLocation());
      result.addAll(i.accept(this));
    }

    return result;
  }

  @Override
  public Set<FileLocation> visit(CDesignatedInitializer pI) {
    return Collections.singleton(pI.getFileLocation());
  }

  @Override
  public Set<FileLocation> visit(CExpressionAssignmentStatement pS) {
    return Sets.union(Collections.singleton(pS.getFileLocation()),
        Sets.union(pS.getLeftHandSide().accept(this), pS.getRightHandSide().accept(this)));
  }

  @Override
  public Set<FileLocation> visit(CExpressionStatement pS) {
    return Sets.union(Collections.singleton(pS.getFileLocation()), pS.getExpression().accept(this));
  }

  @Override
  public Set<FileLocation> visit(CFunctionCallAssignmentStatement pS) {
    Set<FileLocation> result = Sets.newHashSet();
    result.add(pS.getFileLocation());
    result.addAll(pS.getLeftHandSide().accept(this));
    result.addAll(pS.getRightHandSide().accept(this));
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
      result = Sets.union(result, pNode.getSubscriptExpression().accept(this));
    }
    return result;
  }

  @Override
  public Set<FileLocation> visit(CArrayRangeDesignator pNode) {
    Set<FileLocation> result = Collections.singleton(pNode.getFileLocation());
    if (pNode.getCeilExpression() != null) {
      result = Sets.union(result, pNode.getCeilExpression().accept(this));
    }
    if (pNode.getFloorExpression() != null) {
      result = Sets.union(result, pNode.getFloorExpression().accept(this));
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
      result = Sets.union(result, pNode.getReturnValue().get().accept(this));
    }
    return result;
  }

  @Override
  public Set<FileLocation> visit(CFunctionCallExpression pNode) {
    Set<FileLocation> result = Sets.newHashSet();
    result.add(pNode.getFileLocation());
    result.addAll(pNode.getFunctionNameExpression().accept(this));
    for (CExpression expr : pNode.getParameterExpressions()) {
      result.addAll(expr.accept(this));
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
