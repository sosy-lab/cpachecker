/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;

import java.util.List;

import javax.annotation.Nullable;

public class SubstitutingCAstNodeVisitor implements CAstNodeVisitor<CAstNode, UnsupportedOperationException> {

  public interface SubstituteProvider {
    @Nullable CAstNode findSubstitute(CAstNode pNode);
    @Nullable CAstNode adjustTypesAfterSubstitution(CAstNode pNode);
  }

  private SubstituteProvider sp;

  public SubstitutingCAstNodeVisitor(final SubstituteProvider pSubstituteProvider) {
    this.sp = Preconditions.checkNotNull(pSubstituteProvider);
  }

  private @Nullable CAstNode findSubstitute(final CAstNode pNode) {
    final CAstNode substResult = sp.findSubstitute(pNode);

    return fixTypes(substResult);
  }

  private CAstNode fixTypes(CAstNode pSubstResult) {
    if (pSubstResult == null) {
      return null;
    }

    final CAstNode postTypeCheck = sp.adjustTypesAfterSubstitution(pSubstResult);
    if (postTypeCheck != null) {
      return postTypeCheck;
    } else {
      return pSubstResult;
    }
  }

  private <T> T firstNotNull(final T pExpr1, final T pExpr2) {
    if (pExpr1 != null) {
      return pExpr1;
    }
    if (pExpr2 != null) {
      return pExpr2;
    }
    return null;
  }

  @Override
  public CAstNode visit(final CArrayDesignator pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    }

    final CExpression oldSubEx = pNode.getSubscriptExpression();
    final CExpression newSubEx = (CExpression) oldSubEx.accept(this);
    if (oldSubEx != newSubEx) {
      return new CArrayDesignator(
          oldSubEx.getFileLocation(),
          newSubEx);
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CArrayRangeDesignator pNode)  {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    }

    final CExpression oldCeil = pNode.getCeilExpression();
    final CExpression newCeil = (CExpression) pNode.getCeilExpression().accept(this);

    final CExpression oldFloor = pNode.getFloorExpression();
    final CExpression newFloor = (CExpression) pNode.getFloorExpression().accept(this);

    if (oldCeil != newCeil || newFloor != oldFloor) {
      return new CArrayRangeDesignator(
          pNode.getFileLocation(),
          firstNotNull(newFloor, oldFloor),
          firstNotNull(newCeil, oldCeil));
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CFieldDesignator pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CInitializerList pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    }

    boolean initChanged = false;
    List<CInitializer> newInitializerList = Lists.newArrayListWithExpectedSize(pNode.getInitializers().size());

    for (CInitializer oldInit: pNode.getInitializers()) {
      CInitializer newInit = (CInitializer) oldInit.accept(this);
      if (newInit != oldInit) {
        initChanged = true;
      }
      newInitializerList.add(newInit);
    }

    if (initChanged) {
      return new CInitializerList(pNode.getFileLocation(), newInitializerList);
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CReturnStatement pNode) {
    throw new UnsupportedOperationException("Not yet implemented! Implement me!");
  }

  @Override
  public CAstNode visit(final CDesignatedInitializer pNode) {
    throw new UnsupportedOperationException("Not yet implemented! Implement me!");
  }

  @Override
  public CAstNode visit(final CInitializerExpression pNode) {
    throw new UnsupportedOperationException("Not yet implemented! Implement me!");
  }

  @Override
  public CAstNode visit(final CFunctionCallExpression pNode) {
    throw new UnsupportedOperationException("Not yet implemented! Implement me!");
  }

  @Override
  public CAstNode visit(final CBinaryExpression pNode) {
    CAstNode subst = substituteOnBinExpr(pNode)
        ;
    if (subst == null) {
      return pNode;
    } else {
      return subst;
    }
  }

  private CAstNode substituteOnBinExpr(final CBinaryExpression pNode) {
    CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    }

    final CExpression oldOp1 = pNode.getOperand1();
    final CExpression newOp1 = (CExpression) pNode.getOperand1().accept(this);

    final CExpression oldOp2 = pNode.getOperand2();
    final CExpression newOp2 = (CExpression) pNode.getOperand2().accept(this);

    if (oldOp1 != newOp1 || oldOp2 != newOp2) {
      result = new CBinaryExpression(
          pNode.getFileLocation(),
          pNode.getExpressionType(),
          pNode.getCalculationType(),
          firstNotNull(newOp1, oldOp1),
          firstNotNull(newOp2, oldOp2),
          pNode.getOperator());

      return fixTypes(result);
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CCastExpression pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    }

    final CExpression oldOp1 = pNode.getOperand();
    final CExpression newOp1 = (CExpression) pNode.getOperand().accept(this);

    if (oldOp1 != newOp1) {
      return new CCastExpression(
          pNode.getFileLocation(),
          pNode.getExpressionType(),
          newOp1);
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CTypeIdExpression pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CUnaryExpression pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    }

    final CExpression oldOp1 = pNode.getOperand();
    final CExpression newOp1 = (CExpression) pNode.getOperand().accept(this);

    if (oldOp1 != newOp1) {
      return new CUnaryExpression(
          pNode.getFileLocation(),
          pNode.getExpressionType(),
          firstNotNull(newOp1, oldOp1),
          pNode.getOperator());
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CArraySubscriptExpression pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    }

    final CExpression oldOp1 = pNode.getArrayExpression();
    final CExpression newOp1 = (CExpression) pNode.getArrayExpression().accept(this);

    final CExpression oldOp2 = pNode.getSubscriptExpression();
    final CExpression newOp2 = (CExpression) pNode.getSubscriptExpression().accept(this);

    if (oldOp1 != newOp1) {
      return new CArraySubscriptExpression(
          pNode.getFileLocation(),
          pNode.getExpressionType(),
          firstNotNull(newOp1, oldOp1),
          firstNotNull(newOp2, oldOp2));
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CComplexCastExpression pNode) {
    throw new UnsupportedOperationException("Not yet implemented! Implement me!");
  }

  @Override
  public CAstNode visit(final CFieldReference pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CIdExpression pNode) {
    CAstNode subst = findSubstitute(pNode);
    if (subst == null) {
      return pNode;
    } else {
      return subst;
    }
  }

  @Override
  public CAstNode visit(final CPointerExpression pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    }

    final CExpression oldOp1 = pNode.getOperand();
    final CExpression newOp1 = (CExpression) pNode.getOperand().accept(this);

    if (oldOp1 != newOp1) {
      // Replace a '&(*ptr)' by a 'ptr'
      if (newOp1 instanceof CUnaryExpression) {
        CUnaryExpression unaryOperand = (CUnaryExpression)newOp1;
        if (unaryOperand.getOperator().equals(UnaryOperator.AMPER)) {
          return unaryOperand.getOperand();
        }
      }

      CExpression op = firstNotNull(newOp1, oldOp1);
      return new CPointerExpression(
          pNode.getFileLocation(),
          op.getExpressionType(),
          op);
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CCharLiteralExpression pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CFloatLiteralExpression pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CImaginaryLiteralExpression pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CIntegerLiteralExpression pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CStringLiteralExpression pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CAddressOfLabelExpression pNode) {
    throw new UnsupportedOperationException("Not yet implemented! Implement me!");
  }

  @Override
  public CAstNode visit(final CParameterDeclaration pNode) {
    throw new UnsupportedOperationException("Not yet implemented! Implement me!");
  }

  @Override
  public CAstNode visit(final CFunctionDeclaration pNode) {
    throw new UnsupportedOperationException("Not yet implemented! Implement me!");
  }

  @Override
  public CAstNode visit(final CComplexTypeDeclaration pNode) {
    throw new UnsupportedOperationException("Not yet implemented! Implement me!");
  }

  @Override
  public CAstNode visit(final CTypeDefDeclaration pNode) {
    throw new UnsupportedOperationException("Not yet implemented! Implement me!");
  }

  @Override
  public CAstNode visit(final CVariableDeclaration pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    }

    CInitializer init = (CInitializer) findSubstitute(pNode.getInitializer());
    if (init != null && init != pNode.getInitializer()) {
      return new CVariableDeclaration(pNode.getFileLocation(), pNode.isGlobal(),
          pNode.getCStorageClass(), pNode.getType(), pNode.getName(),
          pNode.getOrigName(), pNode.getQualifiedName(),
          init);
    }

    return pNode;
  }

  @Override
  public CAstNode visit(final CExpressionAssignmentStatement pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    }

    final CLeftHandSide oldLeft = pNode.getLeftHandSide();
    final CLeftHandSide newLeft = (CLeftHandSide) pNode.getLeftHandSide().accept(this);

    final CExpression oldRight = pNode.getRightHandSide();
    final CExpression newRight = (CExpression) pNode.getRightHandSide().accept(this);

    if (oldRight != newRight || oldLeft != newLeft) {
      return new CExpressionAssignmentStatement(
          pNode.getFileLocation(),
          firstNotNull(newLeft, oldLeft),
          firstNotNull(newRight, oldRight));
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CExpressionStatement pNode) {
    final CAstNode result = findSubstitute(pNode);
    if (result != null) {
      return result;
    }

    final CExpression oldRight = pNode.getExpression();
    final CExpression newRight = (CExpression) pNode.getExpression().accept(this);

    if (oldRight != newRight) {
      return new CExpressionStatement(
          pNode.getFileLocation(),
          newRight);
    } else {
      return pNode;
    }
  }

  @Override
  public CAstNode visit(final CFunctionCallAssignmentStatement pNode) {
    throw new UnsupportedOperationException("Not yet implemented! Implement me!");
  }

  @Override
  public CAstNode visit(final CFunctionCallStatement pNode) {
    throw new UnsupportedOperationException("Not yet implemented! Implement me!");
  }

  @Override
  public CAstNode visit(final CEnumerator pCEnumerator) {
    throw new UnsupportedOperationException("Not yet implemented! Implement me!");
  }


}
