// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;

/**
 * A {@link CAstNodeVisitor} that is used to create modified copies of abstract syntax tree (AST)
 * nodes.
 *
 * <p>Creating modified copies of AST-nodes is achieved by overriding visit-methods and returning
 * new AST-nodes that differ from the original AST-nodes. The default implementation of a
 * visit-method calls (other) visit-methods recursively to create modified copies for AST-nodes
 * referenced by an original AST-node. If no visit-method is overridden, the returned AST-node is
 * equal to the original AST-node. The implementation may return the same instance for AST-nodes
 * that are equal.
 *
 * <p>Modified AST-nodes are created by calling {@link CAstNode#accept(CAstNodeVisitor)} with an
 * {@code TransformingCAstNodeVisitor}:
 *
 * <pre>{@code
 * CAstNode transformedAstNode = originalAstNode.accept(transformingVisitor);
 * }</pre>
 *
 * <p>Note: for some AST-nodes, it's required to prevent infinite recursive visit-calls due to
 * cyclic references between variable declarations and their initializer expressions. The
 * implementing class must either handle this case itself or extend {@link
 * AbstractTransformingCAstNodeVisitor} which already prevents these infinite recursive calls.
 *
 * @param <X> the type of exception thrown by the visitor
 */
public interface TransformingCAstNodeVisitor<X extends Exception>
    extends CAstNodeVisitor<CAstNode, X> {

  @Override
  default CAstNode visit(CArrayDesignator pCArrayDesignator) throws X {

    CExpression oldSubscriptExpression = pCArrayDesignator.getSubscriptExpression();
    CExpression newSubscriptExpression = (CExpression) oldSubscriptExpression.accept(this);

    if (oldSubscriptExpression != newSubscriptExpression) {
      return new CArrayDesignator(pCArrayDesignator.getFileLocation(), newSubscriptExpression);
    } else {
      return pCArrayDesignator;
    }
  }

  @Override
  default CAstNode visit(CArrayRangeDesignator pCArrayRangeDesignator) throws X {

    CExpression oldFloorExpression = pCArrayRangeDesignator.getFloorExpression();
    CExpression newFloorExpression = (CExpression) oldFloorExpression.accept(this);

    CExpression oldCeilExpression = pCArrayRangeDesignator.getCeilExpression();
    CExpression newCeilExpression = (CExpression) oldCeilExpression.accept(this);

    if (oldFloorExpression != newFloorExpression || oldCeilExpression != newCeilExpression) {
      return new CArrayRangeDesignator(
          pCArrayRangeDesignator.getFileLocation(), newFloorExpression, newCeilExpression);
    } else {
      return pCArrayRangeDesignator;
    }
  }

  @Override
  default CAstNode visit(CFieldDesignator pCFieldDesignator) throws X {
    return pCFieldDesignator;
  }

  @Override
  default CAstNode visit(CInitializerExpression pCInitializerExpression) throws X {

    CExpression oldExpression = pCInitializerExpression.getExpression();
    CExpression newExpression = (CExpression) oldExpression.accept(this);

    if (oldExpression != newExpression) {
      return new CInitializerExpression(pCInitializerExpression.getFileLocation(), newExpression);
    } else {
      return pCInitializerExpression;
    }
  }

  @Override
  default CAstNode visit(CInitializerList pCInitializerList) throws X {

    boolean initializersChanged = false;
    ImmutableList.Builder<CInitializer> initializersBuilder =
        ImmutableList.builderWithExpectedSize(pCInitializerList.getInitializers().size());

    for (CInitializer oldInitializer : pCInitializerList.getInitializers()) {
      CInitializer newInitializer = (CInitializer) oldInitializer.accept(this);
      initializersBuilder.add(newInitializer);
      if (oldInitializer != newInitializer) {
        initializersChanged = true;
      }
    }

    if (initializersChanged) {
      return new CInitializerList(pCInitializerList.getFileLocation(), initializersBuilder.build());
    } else {
      return pCInitializerList;
    }
  }

  @Override
  default CAstNode visit(CDesignatedInitializer pDesignatedInitializer) throws X {

    boolean designatorsChanged = false;
    ImmutableList.Builder<CDesignator> designatorsBuilder =
        ImmutableList.builderWithExpectedSize(pDesignatedInitializer.getDesignators().size());

    for (CDesignator oldDesignator : pDesignatedInitializer.getDesignators()) {
      CDesignator newDesignator = (CDesignator) oldDesignator.accept(this);
      designatorsBuilder.add(newDesignator);
      if (oldDesignator != newDesignator) {
        designatorsChanged = true;
      }
    }

    CInitializer oldRightHandSide = pDesignatedInitializer.getRightHandSide();
    CInitializer newRightHandSide = (CInitializer) oldRightHandSide.accept(this);

    if (designatorsChanged || oldRightHandSide != newRightHandSide) {
      return new CDesignatedInitializer(
          pDesignatedInitializer.getFileLocation(), designatorsBuilder.build(), newRightHandSide);
    } else {
      return pDesignatedInitializer;
    }
  }

  @Override
  default CAstNode visit(CFunctionCallExpression pCFunctionCallExpression) throws X {

    boolean parameterExpressionsChanged = false;
    ImmutableList.Builder<CExpression> parameterExpressionsBuilder =
        ImmutableList.builderWithExpectedSize(
            pCFunctionCallExpression.getParameterExpressions().size());

    for (CExpression oldParameterExpression : pCFunctionCallExpression.getParameterExpressions()) {
      CExpression newParameterExpression = (CExpression) oldParameterExpression.accept(this);
      parameterExpressionsBuilder.add(newParameterExpression);
      if (oldParameterExpression != newParameterExpression) {
        parameterExpressionsChanged = true;
      }
    }

    CFunctionDeclaration oldFunctionDeclaration = pCFunctionCallExpression.getDeclaration();
    CFunctionDeclaration newFunctionDeclaration = null;
    if (oldFunctionDeclaration != null) {
      newFunctionDeclaration = (CFunctionDeclaration) oldFunctionDeclaration.accept(this);
    }

    CExpression oldFunctionNameExpression = pCFunctionCallExpression.getFunctionNameExpression();
    CExpression newFunctionNameExpression = (CExpression) oldFunctionNameExpression.accept(this);

    if (parameterExpressionsChanged
        || oldFunctionDeclaration != newFunctionDeclaration
        || oldFunctionNameExpression != newFunctionNameExpression) {
      return new CFunctionCallExpression(
          pCFunctionCallExpression.getFileLocation(),
          pCFunctionCallExpression.getExpressionType(),
          newFunctionNameExpression,
          parameterExpressionsBuilder.build(),
          newFunctionDeclaration);
    } else {
      return pCFunctionCallExpression;
    }
  }

  @Override
  default CAstNode visit(CBinaryExpression pCBinaryExpression) throws X {

    CExpression oldOperand1Expression = pCBinaryExpression.getOperand1();
    CExpression newOperand1Expression = (CExpression) oldOperand1Expression.accept(this);

    CExpression oldOperand2Expression = pCBinaryExpression.getOperand2();
    CExpression newOperand2Expression = (CExpression) oldOperand2Expression.accept(this);

    if (oldOperand1Expression != newOperand1Expression
        || oldOperand2Expression != newOperand2Expression) {
      return new CBinaryExpression(
          pCBinaryExpression.getFileLocation(),
          pCBinaryExpression.getExpressionType(),
          pCBinaryExpression.getCalculationType(),
          newOperand1Expression,
          newOperand2Expression,
          pCBinaryExpression.getOperator());
    } else {
      return pCBinaryExpression;
    }
  }

  @Override
  default CAstNode visit(CCastExpression pCCastExpression) throws X {

    CExpression oldOperandExpression = pCCastExpression.getOperand();
    CExpression newOperandExpression = (CExpression) oldOperandExpression.accept(this);

    if (oldOperandExpression != newOperandExpression) {
      return new CCastExpression(
          pCCastExpression.getFileLocation(),
          pCCastExpression.getExpressionType(),
          newOperandExpression);
    } else {
      return pCCastExpression;
    }
  }

  @Override
  default CAstNode visit(CCharLiteralExpression pCCharLiteralExpression) throws X {
    return pCCharLiteralExpression;
  }

  @Override
  default CAstNode visit(CFloatLiteralExpression pCFloatLiteralExpression) throws X {
    return pCFloatLiteralExpression;
  }

  @Override
  default CAstNode visit(CIntegerLiteralExpression pCIntegerLiteralExpression) throws X {
    return pCIntegerLiteralExpression;
  }

  @Override
  default CAstNode visit(CStringLiteralExpression pCStringLiteralExpression) throws X {
    return pCStringLiteralExpression;
  }

  @Override
  default CAstNode visit(CTypeIdExpression pCTypeIdExpression) throws X {
    return pCTypeIdExpression;
  }

  @Override
  default CAstNode visit(CUnaryExpression pCUnaryExpression) throws X {

    CExpression oldOperandExpression = pCUnaryExpression.getOperand();
    CExpression newOperandExpression = (CExpression) oldOperandExpression.accept(this);

    if (oldOperandExpression != newOperandExpression) {
      return new CUnaryExpression(
          pCUnaryExpression.getFileLocation(),
          pCUnaryExpression.getExpressionType(),
          newOperandExpression,
          pCUnaryExpression.getOperator());
    } else {
      return pCUnaryExpression;
    }
  }

  @Override
  default CAstNode visit(CImaginaryLiteralExpression pCImaginaryLiteralExpression) throws X {

    CLiteralExpression oldValueExpression = pCImaginaryLiteralExpression.getValue();
    CLiteralExpression newValueExpression = (CLiteralExpression) oldValueExpression.accept(this);

    if (oldValueExpression != newValueExpression) {
      return new CImaginaryLiteralExpression(
          pCImaginaryLiteralExpression.getFileLocation(),
          pCImaginaryLiteralExpression.getExpressionType(),
          newValueExpression);
    } else {
      return pCImaginaryLiteralExpression;
    }
  }

  @Override
  default CAstNode visit(CAddressOfLabelExpression pCAddressOfLabelExpression) throws X {
    return pCAddressOfLabelExpression;
  }

  @Override
  default CAstNode visit(CArraySubscriptExpression pCArraySubscriptExpression) throws X {

    CExpression oldArrayExpression = pCArraySubscriptExpression.getArrayExpression();
    CExpression newArrayExpression = (CExpression) oldArrayExpression.accept(this);

    CExpression oldSubscriptExpression = pCArraySubscriptExpression.getSubscriptExpression();
    CExpression newSubscriptExpression = (CExpression) oldSubscriptExpression.accept(this);

    if (oldArrayExpression != newArrayExpression
        || oldSubscriptExpression != newSubscriptExpression) {
      return new CArraySubscriptExpression(
          pCArraySubscriptExpression.getFileLocation(),
          pCArraySubscriptExpression.getExpressionType(),
          newArrayExpression,
          newSubscriptExpression);
    } else {
      return pCArraySubscriptExpression;
    }
  }

  @Override
  default CAstNode visit(CFieldReference pCFieldReference) throws X {

    CExpression oldFieldOwnerExpression = pCFieldReference.getFieldOwner();
    CExpression newFieldOwnerExpression = (CExpression) oldFieldOwnerExpression.accept(this);

    if (oldFieldOwnerExpression != newFieldOwnerExpression) {
      return new CFieldReference(
          pCFieldReference.getFileLocation(),
          pCFieldReference.getExpressionType(),
          pCFieldReference.getFieldName(),
          newFieldOwnerExpression,
          pCFieldReference.isPointerDereference());
    } else {
      return pCFieldReference;
    }
  }

  @Override
  default CAstNode visit(CIdExpression pCIdExpression) throws X {

    CSimpleDeclaration oldDeclaration = pCIdExpression.getDeclaration();
    CSimpleDeclaration newDeclaration = null;
    if (oldDeclaration != null) {
      newDeclaration = (CSimpleDeclaration) oldDeclaration.accept(this);
    }

    if (oldDeclaration != newDeclaration) {
      return new CIdExpression(
          pCIdExpression.getFileLocation(),
          pCIdExpression.getExpressionType(),
          pCIdExpression.getName(),
          newDeclaration);
    } else {
      return pCIdExpression;
    }
  }

  @Override
  default CAstNode visit(CPointerExpression pCPointerExpression) throws X {

    CExpression oldOperandExpression = pCPointerExpression.getOperand();
    CExpression newOperandExpression = (CExpression) oldOperandExpression.accept(this);

    if (oldOperandExpression != newOperandExpression) {
      return new CPointerExpression(
          pCPointerExpression.getFileLocation(),
          pCPointerExpression.getExpressionType(),
          newOperandExpression);
    } else {
      return pCPointerExpression;
    }
  }

  @Override
  default CAstNode visit(CComplexCastExpression pCComplexCastExpression) throws X {

    CExpression oldOperandExpression = pCComplexCastExpression.getOperand();
    CExpression newOperandExpression = (CExpression) oldOperandExpression.accept(this);

    if (oldOperandExpression != newOperandExpression) {
      return new CComplexCastExpression(
          pCComplexCastExpression.getFileLocation(),
          pCComplexCastExpression.getExpressionType(),
          newOperandExpression,
          pCComplexCastExpression.getType(),
          pCComplexCastExpression.isRealCast());
    } else {
      return pCComplexCastExpression;
    }
  }

  @Override
  default CAstNode visit(CFunctionDeclaration pCFunctionDeclaration) throws X {

    boolean parametersChanged = false;
    ImmutableList.Builder<CParameterDeclaration> parametersBuilder =
        ImmutableList.builderWithExpectedSize(pCFunctionDeclaration.getParameters().size());

    for (CParameterDeclaration oldParameterDeclaration : pCFunctionDeclaration.getParameters()) {
      CParameterDeclaration newParameterDeclaration =
          (CParameterDeclaration) oldParameterDeclaration.accept(this);
      parametersBuilder.add(newParameterDeclaration);
      if (oldParameterDeclaration != newParameterDeclaration) {
        parametersChanged = true;
      }
    }

    if (parametersChanged) {
      return new CFunctionDeclaration(
          pCFunctionDeclaration.getFileLocation(),
          pCFunctionDeclaration.getType(),
          pCFunctionDeclaration.getName(),
          pCFunctionDeclaration.getOrigName(),
          parametersBuilder.build(),
          pCFunctionDeclaration.getAttributes());
    } else {
      return pCFunctionDeclaration;
    }
  }

  @Override
  default CAstNode visit(CComplexTypeDeclaration pCComplexTypeDeclaration) throws X {
    return pCComplexTypeDeclaration;
  }

  @Override
  default CAstNode visit(CTypeDefDeclaration pCTypeDefDeclaration) throws X {
    return pCTypeDefDeclaration;
  }

  @Override
  default CAstNode visit(CVariableDeclaration pCVariableDeclaration) throws X {

    CInitializer oldInitializer = pCVariableDeclaration.getInitializer();
    CInitializer newInitializer = null;
    if (oldInitializer != null) {
      newInitializer = (CInitializer) oldInitializer.accept(this);
    }

    if (oldInitializer != newInitializer) {
      return new CVariableDeclaration(
          pCVariableDeclaration.getFileLocation(),
          pCVariableDeclaration.isGlobal(),
          pCVariableDeclaration.getCStorageClass(),
          pCVariableDeclaration.getType(),
          pCVariableDeclaration.getName(),
          pCVariableDeclaration.getOrigName(),
          pCVariableDeclaration.getQualifiedName(),
          newInitializer);
    } else {
      return pCVariableDeclaration;
    }
  }

  @Override
  default CAstNode visit(CParameterDeclaration pCParameterDeclaration) throws X {
    return pCParameterDeclaration;
  }

  @Override
  default CAstNode visit(CEnumerator pCEnumerator) throws X {
    return pCEnumerator;
  }

  @Override
  default CAstNode visit(CExpressionStatement pCExpressionStatement) throws X {

    CExpression oldExpression = pCExpressionStatement.getExpression();
    CExpression newExpression = (CExpression) oldExpression.accept(this);

    if (oldExpression != newExpression) {
      return new CExpressionStatement(pCExpressionStatement.getFileLocation(), newExpression);
    } else {
      return pCExpressionStatement;
    }
  }

  @Override
  default CAstNode visit(CExpressionAssignmentStatement pCExpressionAssignmentStatement) throws X {

    CLeftHandSide oldLeftHandSide = pCExpressionAssignmentStatement.getLeftHandSide();
    CLeftHandSide newLeftHandSide = (CLeftHandSide) oldLeftHandSide.accept(this);

    CExpression oldRightHandSide = pCExpressionAssignmentStatement.getRightHandSide();
    CExpression newRightHandSide = (CExpression) oldRightHandSide.accept(this);

    if (oldLeftHandSide != newLeftHandSide || oldRightHandSide != newRightHandSide) {
      return new CExpressionAssignmentStatement(
          pCExpressionAssignmentStatement.getFileLocation(), newLeftHandSide, newRightHandSide);
    } else {
      return pCExpressionAssignmentStatement;
    }
  }

  @Override
  default CAstNode visit(CFunctionCallAssignmentStatement pCFunctionCallAssignmentStatement)
      throws X {

    CLeftHandSide oldLeftHandSide = pCFunctionCallAssignmentStatement.getLeftHandSide();
    CLeftHandSide newLeftHandSide = (CLeftHandSide) oldLeftHandSide.accept(this);

    CFunctionCallExpression oldFunctionCallExpression =
        pCFunctionCallAssignmentStatement.getFunctionCallExpression();
    CFunctionCallExpression newFunctionCallExpression =
        (CFunctionCallExpression) oldFunctionCallExpression.accept(this);

    if (oldLeftHandSide != newLeftHandSide
        || oldFunctionCallExpression != newFunctionCallExpression) {
      return new CFunctionCallAssignmentStatement(
          pCFunctionCallAssignmentStatement.getFileLocation(),
          newLeftHandSide,
          newFunctionCallExpression);
    } else {
      return pCFunctionCallAssignmentStatement;
    }
  }

  @Override
  default CAstNode visit(CFunctionCallStatement pCFunctionCallStatement) throws X {

    CFunctionCallExpression oldFunctionCallExpression =
        pCFunctionCallStatement.getFunctionCallExpression();
    CFunctionCallExpression newFunctionCallExpression =
        (CFunctionCallExpression) oldFunctionCallExpression.accept(this);

    if (oldFunctionCallExpression != newFunctionCallExpression) {
      return new CFunctionCallStatement(
          pCFunctionCallStatement.getFileLocation(), newFunctionCallExpression);
    } else {
      return pCFunctionCallStatement;
    }
  }

  @Override
  default CAstNode visit(CReturnStatement pCReturnStatement) throws X {

    boolean changed = false;

    Optional<CExpression> oldOptionalReturnValue = pCReturnStatement.getReturnValue();
    Optional<CExpression> newOptionalReturnValue = Optional.empty();
    if (oldOptionalReturnValue.isPresent()) {
      CExpression oldReturnValue = oldOptionalReturnValue.orElseThrow();
      CExpression newReturnValue = (CExpression) oldReturnValue.accept(this);
      newOptionalReturnValue = Optional.of(newReturnValue);
      if (oldReturnValue != newReturnValue) {
        changed = true;
      }
    }

    Optional<CAssignment> oldOptionalAssignment = pCReturnStatement.asAssignment();
    Optional<CAssignment> newOptionalAssignment = Optional.empty();
    if (oldOptionalAssignment.isPresent()) {
      CAssignment oldAssignment = oldOptionalAssignment.orElseThrow();
      CAssignment newAssignment = (CAssignment) oldAssignment.accept(this);
      newOptionalAssignment = Optional.of(newAssignment);
      if (oldAssignment != newAssignment) {
        changed = true;
      }
    }

    if (changed) {
      return new CReturnStatement(
          pCReturnStatement.getFileLocation(), newOptionalReturnValue, newOptionalAssignment);
    } else {
      return pCReturnStatement;
    }
  }
}
