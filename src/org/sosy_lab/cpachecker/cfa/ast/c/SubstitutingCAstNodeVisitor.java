// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class SubstitutingCAstNodeVisitor implements CAstNodeVisitor<CAstNode, NoException> {

  private final Function<CAstNode, CAstNode> findSubstitute;

  public SubstitutingCAstNodeVisitor(Function<CAstNode, CAstNode> function) {
    this.findSubstitute = function;
  }

  private <T> T firstNotNull(T pExpr1, T pExpr2) {
    if (pExpr1 != null) {
      return pExpr1;
    }
    if (pExpr2 != null) {
      return pExpr2;
    }
    return null;
  }

  @Override
  public CAstNode visit(CArrayDesignator pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    CExpression oldSubEx = pNode.getSubscriptExpression();
    CExpression newSubEx = (CExpression) oldSubEx.accept(this);
    if (oldSubEx != newSubEx) {
      return new CArrayDesignator(oldSubEx.getFileLocation(), newSubEx);
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CArrayRangeDesignator pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    CExpression oldCeil = pNode.getCeilExpression();
    CExpression newCeil = (CExpression) pNode.getCeilExpression().accept(this);

    CExpression oldFloor = pNode.getFloorExpression();
    CExpression newFloor = (CExpression) pNode.getFloorExpression().accept(this);
    if ((oldCeil != newCeil) || (newFloor != oldFloor)) {
      return new CArrayRangeDesignator(
          pNode.getFileLocation(),
          firstNotNull(newFloor, oldFloor),
          firstNotNull(newCeil, oldCeil));
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CFieldDesignator pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CInitializerList pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    boolean initChanged = false;
    List<CInitializer> newInitializerList = new ArrayList<>(pNode.getInitializers().size());
    for (CInitializer oldInit : pNode.getInitializers()) {
      CInitializer newInit = (CInitializer) oldInit.accept(this);
      if (newInit != oldInit) {
        initChanged = true;
      }
      newInitializerList.add(newInit);
    }
    if (initChanged) {
      return new CInitializerList(pNode.getFileLocation(), newInitializerList);
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CReturnStatement pNode) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public CAstNode visit(CDesignatedInitializer pNode) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public CAstNode visit(CInitializerExpression pNode) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public CAstNode visit(CFunctionCallExpression pNode) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public CAstNode visit(CBinaryExpression pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    CExpression oldOp1 = pNode.getOperand1();
    CExpression newOp1 = (CExpression) pNode.getOperand1().accept(this);

    CExpression oldOp2 = pNode.getOperand2();
    CExpression newOp2 = (CExpression) pNode.getOperand2().accept(this);
    if ((oldOp1 != newOp1) || (oldOp2 != newOp2)) {
      return new CBinaryExpression(
          pNode.getFileLocation(),
          pNode.getExpressionType(),
          pNode.getCalculationType(),
          firstNotNull(newOp1, oldOp1),
          firstNotNull(newOp2, oldOp2),
          pNode.getOperator());
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CCastExpression pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    CExpression oldOp1 = pNode.getOperand();
    CExpression newOp1 = (CExpression) pNode.getOperand().accept(this);
    if (oldOp1 != newOp1) {
      return new CCastExpression(pNode.getFileLocation(), pNode.getExpressionType(), newOp1);
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CTypeIdExpression pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CUnaryExpression pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    CExpression oldOp1 = pNode.getOperand();
    CExpression newOp1 = (CExpression) pNode.getOperand().accept(this);
    if (oldOp1 != newOp1) {
      return new CUnaryExpression(
          pNode.getFileLocation(),
          pNode.getExpressionType(),
          firstNotNull(newOp1, oldOp1),
          pNode.getOperator());
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CArraySubscriptExpression pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    CExpression oldOp1 = pNode.getArrayExpression();
    CExpression newOp1 = (CExpression) pNode.getArrayExpression().accept(this);

    CExpression oldOp2 = pNode.getSubscriptExpression();
    CExpression newOp2 = (CExpression) pNode.getSubscriptExpression().accept(this);
    if (oldOp1 != newOp1) {
      return new CArraySubscriptExpression(
          pNode.getFileLocation(),
          pNode.getExpressionType(),
          firstNotNull(newOp1, oldOp1),
          firstNotNull(newOp2, oldOp2));
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CComplexCastExpression pNode) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public CAstNode visit(CFieldReference pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CIdExpression pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CPointerExpression pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    CExpression oldOp1 = pNode.getOperand();
    CExpression newOp1 = (CExpression) pNode.getOperand().accept(this);
    if (oldOp1 != newOp1) {
      return new CPointerExpression(
          pNode.getFileLocation(), pNode.getExpressionType(), firstNotNull(newOp1, oldOp1));
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CCharLiteralExpression pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CFloatLiteralExpression pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CImaginaryLiteralExpression pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CIntegerLiteralExpression pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CStringLiteralExpression pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CAddressOfLabelExpression pNode) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public CAstNode visit(CParameterDeclaration pNode) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public CAstNode visit(CFunctionDeclaration pNode) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public CAstNode visit(CComplexTypeDeclaration pNode) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public CAstNode visit(CTypeDefDeclaration pNode) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public CAstNode visit(CVariableDeclaration pNode) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public CAstNode visit(CExpressionAssignmentStatement pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    CLeftHandSide oldLeft = pNode.getLeftHandSide();
    CLeftHandSide newLeft = (CLeftHandSide) pNode.getLeftHandSide().accept(this);

    CExpression oldRight = pNode.getRightHandSide();
    CExpression newRight = (CExpression) pNode.getRightHandSide().accept(this);
    if ((oldRight != newRight) || (oldLeft != newLeft)) {
      return new CExpressionAssignmentStatement(
          pNode.getFileLocation(),
          firstNotNull(newLeft, oldLeft),
          firstNotNull(newRight, oldRight));
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CExpressionStatement pNode) {
    CAstNode result = findSubstitute.apply(pNode);
    if (result != null) {
      return result;
    }
    CExpression oldRight = pNode.getExpression();
    CExpression newRight = (CExpression) pNode.getExpression().accept(this);
    if (oldRight != newRight) {
      return new CExpressionStatement(pNode.getFileLocation(), newRight);
    }
    return pNode;
  }

  @Override
  public CAstNode visit(CFunctionCallAssignmentStatement pNode) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public CAstNode visit(CFunctionCallStatement pNode) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public CAstNode visit(CEnumType.CEnumerator pCEnumerator) {
    throw new RuntimeException("Not yet implemented");
  }
}
