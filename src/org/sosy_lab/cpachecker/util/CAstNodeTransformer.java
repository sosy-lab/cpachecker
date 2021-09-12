// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.exceptions.NoException;

public final class CAstNodeTransformer<X extends Exception> {

  private final CAstNodeVisitor<CAstNode, X> transformingVisitor;

  private CAstNodeTransformer(CAstNodeVisitor<CAstNode, X> pTransformingVisitor) {
    transformingVisitor = pTransformingVisitor;
  }

  public static <X extends Exception> CAstNodeTransformer<X> of(
      CAstNodeVisitor<CAstNode, X> pTransformingVisitor) {

    Objects.requireNonNull(pTransformingVisitor, "pTransformingVisitor must not be null");

    return new CAstNodeTransformer<>(pTransformingVisitor);
  }

  public static CAstNodeTransformer<NoException> identity() {
    return new CAstNodeTransformer<>(new FastIdentityTransformer());
  }

  public CAstNode transform(CAstNode pCAstNode) throws X {
    if (pCAstNode != null) {
      return pCAstNode.accept(transformingVisitor);
    } else {
      return null;
    }
  }

  public abstract static class AbstractTransformingVisitor<X extends Exception>
      implements CAstNodeVisitor<CAstNode, X> {

    // required to prevent infinite recursive calls due to AST node cycles
    private CVariableDeclaration visitedVariableDeclaration = null;
    private CVariableDeclaration createdVariableDeclaration = null;

    @Override
    public CArrayDesignator visit(CArrayDesignator pCArrayDesignator) throws X {

      CExpression oldSubscriptExpression = pCArrayDesignator.getSubscriptExpression();
      CExpression newSubscriptExpression = (CExpression) oldSubscriptExpression.accept(this);

      if (oldSubscriptExpression != newSubscriptExpression) {
        return new CArrayDesignator(pCArrayDesignator.getFileLocation(), newSubscriptExpression);
      } else {
        return pCArrayDesignator;
      }
    }

    @Override
    public CArrayRangeDesignator visit(CArrayRangeDesignator pCArrayRangeDesignator) throws X {

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
    public CFieldDesignator visit(CFieldDesignator pCFieldDesignator) throws X {
      return pCFieldDesignator;
    }

    @Override
    public CInitializerExpression visit(CInitializerExpression pCInitializerExpression) throws X {

      CExpression oldExpression = pCInitializerExpression.getExpression();
      CExpression newExpression = (CExpression) oldExpression.accept(this);

      if (oldExpression != newExpression) {
        return new CInitializerExpression(pCInitializerExpression.getFileLocation(), newExpression);
      } else {
        return pCInitializerExpression;
      }
    }

    @Override
    public CInitializerList visit(CInitializerList pCInitializerList) throws X {

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
        return new CInitializerList(
            pCInitializerList.getFileLocation(), initializersBuilder.build());
      } else {
        return pCInitializerList;
      }
    }

    @Override
    public CDesignatedInitializer visit(CDesignatedInitializer pDesignatedInitializer) throws X {

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
    public CFunctionCallExpression visit(CFunctionCallExpression pCFunctionCallExpression)
        throws X {

      boolean parameterExpressionsChanged = false;
      ImmutableList.Builder<CExpression> parameterExpressionsBuilder =
          ImmutableList.builderWithExpectedSize(
              pCFunctionCallExpression.getParameterExpressions().size());

      for (CExpression oldParameterExpression :
          pCFunctionCallExpression.getParameterExpressions()) {
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
    public CExpression visit(CBinaryExpression pCBinaryExpression) throws X {

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
    public CExpression visit(CCastExpression pCCastExpression) throws X {

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
    public CExpression visit(CCharLiteralExpression pCCharLiteralExpression) throws X {
      return pCCharLiteralExpression;
    }

    @Override
    public CExpression visit(CFloatLiteralExpression pCFloatLiteralExpression) throws X {
      return pCFloatLiteralExpression;
    }

    @Override
    public CExpression visit(CIntegerLiteralExpression pCIntegerLiteralExpression) throws X {
      return pCIntegerLiteralExpression;
    }

    @Override
    public CExpression visit(CStringLiteralExpression pCStringLiteralExpression) throws X {
      return pCStringLiteralExpression;
    }

    @Override
    public CExpression visit(CTypeIdExpression pCTypeIdExpression) throws X {
      return pCTypeIdExpression;
    }

    @Override
    public CExpression visit(CUnaryExpression pCUnaryExpression) throws X {

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
    public CExpression visit(CImaginaryLiteralExpression pCImaginaryLiteralExpression) throws X {

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
    public CExpression visit(CAddressOfLabelExpression pCAddressOfLabelExpression) throws X {
      return pCAddressOfLabelExpression;
    }

    @Override
    public CExpression visit(CArraySubscriptExpression pCArraySubscriptExpression) throws X {

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
    public CExpression visit(CFieldReference pCFieldReference) throws X {

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
    public CExpression visit(CIdExpression pCIdExpression) throws X {

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
    public CExpression visit(CPointerExpression pCPointerExpression) throws X {

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
    public CExpression visit(CComplexCastExpression pCComplexCastExpression) throws X {

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
    public CFunctionDeclaration visit(CFunctionDeclaration pCFunctionDeclaration) throws X {

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
            parametersBuilder.build());
      } else {
        return pCFunctionDeclaration;
      }
    }

    @Override
    public CComplexTypeDeclaration visit(CComplexTypeDeclaration pCComplexTypeDeclaration)
        throws X {
      return pCComplexTypeDeclaration;
    }

    @Override
    public CTypeDefDeclaration visit(CTypeDefDeclaration pCTypeDefDeclaration) throws X {
      return pCTypeDefDeclaration;
    }

    @Override
    public CVariableDeclaration visit(CVariableDeclaration pCVariableDeclaration) throws X {

      // required to prevent infinite recursive calls due to AST node cycles
      if (visitedVariableDeclaration == pCVariableDeclaration) {
        return createdVariableDeclaration;
      }

      CVariableDeclaration variableDeclaration =
          new CVariableDeclaration(
              pCVariableDeclaration.getFileLocation(),
              pCVariableDeclaration.isGlobal(),
              pCVariableDeclaration.getCStorageClass(),
              pCVariableDeclaration.getType(),
              pCVariableDeclaration.getName(),
              pCVariableDeclaration.getOrigName(),
              pCVariableDeclaration.getQualifiedName(),
              null);

      visitedVariableDeclaration = pCVariableDeclaration;
      createdVariableDeclaration = variableDeclaration;

      CInitializer oldInitializer = pCVariableDeclaration.getInitializer();
      CInitializer newInitializer = null;
      if (oldInitializer != null) {
        newInitializer = (CInitializer) oldInitializer.accept(this);
        variableDeclaration.addInitializer(newInitializer);
      }

      visitedVariableDeclaration = null;
      createdVariableDeclaration = null;

      if (oldInitializer != newInitializer) {
        return variableDeclaration;
      } else {
        return pCVariableDeclaration;
      }
    }

    @Override
    public CParameterDeclaration visit(CParameterDeclaration pCParameterDeclaration) throws X {
      return pCParameterDeclaration;
    }

    @Override
    public CEnumerator visit(CEnumerator pCEnumerator) throws X {
      return pCEnumerator;
    }

    @Override
    public CStatement visit(CExpressionStatement pCExpressionStatement) throws X {

      CExpression oldExpression = pCExpressionStatement.getExpression();
      CExpression newExpression = (CExpression) oldExpression.accept(this);

      if (oldExpression != newExpression) {
        return new CExpressionStatement(pCExpressionStatement.getFileLocation(), newExpression);
      } else {
        return pCExpressionStatement;
      }
    }

    @Override
    public CStatement visit(CExpressionAssignmentStatement pCExpressionAssignmentStatement)
        throws X {

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
    public CStatement visit(CFunctionCallAssignmentStatement pCFunctionCallAssignmentStatement)
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
    public CStatement visit(CFunctionCallStatement pCFunctionCallStatement) throws X {

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
    public CReturnStatement visit(CReturnStatement pCReturnStatement) throws X {

      boolean changed = false;

      Optional<? extends CExpression> oldOptionalReturnValue = pCReturnStatement.getReturnValue();
      Optional<CExpression> newOptionalReturnValue = Optional.absent();
      if (oldOptionalReturnValue.isPresent()) {
        CExpression oldReturnValue = oldOptionalReturnValue.orNull();
        CExpression newReturnValue = (CExpression) oldReturnValue.accept(this);
        newOptionalReturnValue = Optional.of(newReturnValue);
        if (oldReturnValue != newReturnValue) {
          changed = true;
        }
      }

      Optional<? extends CAssignment> oldOptionalAssignment = pCReturnStatement.asAssignment();
      Optional<CAssignment> newOptionalAssignment = Optional.absent();
      if (oldOptionalAssignment.isPresent()) {
        CAssignment oldAssignment = oldOptionalAssignment.orNull();
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

  private static final class FastIdentityTransformer
      implements CAstNodeVisitor<CAstNode, NoException> {

    @Override
    public CArrayDesignator visit(CArrayDesignator pCArrayDesignator) {
      return pCArrayDesignator;
    }

    @Override
    public CArrayRangeDesignator visit(CArrayRangeDesignator pCArrayRangeDesignator) {
      return pCArrayRangeDesignator;
    }

    @Override
    public CFieldDesignator visit(CFieldDesignator pCFieldDesignator) {
      return pCFieldDesignator;
    }

    @Override
    public CInitializerExpression visit(CInitializerExpression pCInitializerExpression) {
      return pCInitializerExpression;
    }

    @Override
    public CInitializerList visit(CInitializerList pCInitializerList) {
      return pCInitializerList;
    }

    @Override
    public CDesignatedInitializer visit(CDesignatedInitializer pDesignatedInitializer) {
      return pDesignatedInitializer;
    }

    @Override
    public CFunctionCallExpression visit(CFunctionCallExpression pCFunctionCallExpression) {
      return pCFunctionCallExpression;
    }

    @Override
    public CBinaryExpression visit(CBinaryExpression pCBinaryExpression) {
      return pCBinaryExpression;
    }

    @Override
    public CCastExpression visit(CCastExpression pCCastExpression) {
      return pCCastExpression;
    }

    @Override
    public CCharLiteralExpression visit(CCharLiteralExpression pCCharLiteralExpression) {
      return pCCharLiteralExpression;
    }

    @Override
    public CFloatLiteralExpression visit(CFloatLiteralExpression pCFloatLiteralExpression) {
      return pCFloatLiteralExpression;
    }

    @Override
    public CIntegerLiteralExpression visit(CIntegerLiteralExpression pCIntegerLiteralExpression) {
      return pCIntegerLiteralExpression;
    }

    @Override
    public CStringLiteralExpression visit(CStringLiteralExpression pCStringLiteralExpression) {
      return pCStringLiteralExpression;
    }

    @Override
    public CTypeIdExpression visit(CTypeIdExpression pCTypeIdExpression) {
      return pCTypeIdExpression;
    }

    @Override
    public CUnaryExpression visit(CUnaryExpression pCUnaryExpression) {
      return pCUnaryExpression;
    }

    @Override
    public CImaginaryLiteralExpression visit(
        CImaginaryLiteralExpression pCImaginaryLiteralExpression) {
      return pCImaginaryLiteralExpression;
    }

    @Override
    public CAddressOfLabelExpression visit(CAddressOfLabelExpression pCAddressOfLabelExpression) {
      return pCAddressOfLabelExpression;
    }

    @Override
    public CArraySubscriptExpression visit(CArraySubscriptExpression pCArraySubscriptExpression) {
      return pCArraySubscriptExpression;
    }

    @Override
    public CFieldReference visit(CFieldReference pCFieldReference) {
      return pCFieldReference;
    }

    @Override
    public CIdExpression visit(CIdExpression pCIdExpression) {
      return pCIdExpression;
    }

    @Override
    public CPointerExpression visit(CPointerExpression pCPointerExpression) {
      return pCPointerExpression;
    }

    @Override
    public CComplexCastExpression visit(CComplexCastExpression pCComplexCastExpression) {
      return pCComplexCastExpression;
    }

    @Override
    public CFunctionDeclaration visit(CFunctionDeclaration pCFunctionDeclaration) {
      return pCFunctionDeclaration;
    }

    @Override
    public CComplexTypeDeclaration visit(CComplexTypeDeclaration pCComplexTypeDeclaration) {
      return pCComplexTypeDeclaration;
    }

    @Override
    public CTypeDefDeclaration visit(CTypeDefDeclaration pCTypeDefDeclaration) {
      return pCTypeDefDeclaration;
    }

    @Override
    public CVariableDeclaration visit(CVariableDeclaration pCVariableDeclaration) {
      return pCVariableDeclaration;
    }

    @Override
    public CParameterDeclaration visit(CParameterDeclaration pCParameterDeclaration) {
      return pCParameterDeclaration;
    }

    @Override
    public CEnumerator visit(CEnumerator pCEnumerator) {
      return pCEnumerator;
    }

    @Override
    public CExpressionStatement visit(CExpressionStatement pCExpressionStatement) {
      return pCExpressionStatement;
    }

    @Override
    public CExpressionAssignmentStatement visit(
        CExpressionAssignmentStatement pCExpressionAssignmentStatement) {
      return pCExpressionAssignmentStatement;
    }

    @Override
    public CFunctionCallAssignmentStatement visit(
        CFunctionCallAssignmentStatement pCFunctionCallAssignmentStatement) {
      return pCFunctionCallAssignmentStatement;
    }

    @Override
    public CFunctionCallStatement visit(CFunctionCallStatement pCFunctionCallStatement) {
      return pCFunctionCallStatement;
    }

    @Override
    public CReturnStatement visit(CReturnStatement pCReturnStatement) {
      return pCReturnStatement;
    }
  }
}
