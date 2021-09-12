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
      return new CArrayDesignator(
          pCArrayDesignator.getFileLocation(),
          (CExpression) pCArrayDesignator.getSubscriptExpression().accept(this));
    }

    @Override
    public CArrayRangeDesignator visit(CArrayRangeDesignator pCArrayRangeDesignator) throws X {
      return new CArrayRangeDesignator(
          pCArrayRangeDesignator.getFileLocation(),
          (CExpression) pCArrayRangeDesignator.getFloorExpression().accept(this),
          (CExpression) pCArrayRangeDesignator.getCeilExpression().accept(this));
    }

    @Override
    public CFieldDesignator visit(CFieldDesignator pCFieldDesignator) throws X {
      return pCFieldDesignator;
    }

    @Override
    public CInitializerExpression visit(CInitializerExpression pCInitializerExpression) throws X {
      return new CInitializerExpression(
          pCInitializerExpression.getFileLocation(),
          (CExpression) pCInitializerExpression.getExpression().accept(this));
    }

    @Override
    public CInitializerList visit(CInitializerList pCInitializerList) throws X {

      ImmutableList.Builder<CInitializer> initializersBuilder =
          ImmutableList.builderWithExpectedSize(pCInitializerList.getInitializers().size());

      for (CInitializer initializer : pCInitializerList.getInitializers()) {
        initializersBuilder.add((CInitializer) initializer.accept(this));
      }

      return new CInitializerList(pCInitializerList.getFileLocation(), initializersBuilder.build());
    }

    @Override
    public CDesignatedInitializer visit(CDesignatedInitializer pDesignatedInitializer) throws X {

      ImmutableList.Builder<CDesignator> designatorsBuilder =
          ImmutableList.builderWithExpectedSize(pDesignatedInitializer.getDesignators().size());

      for (CDesignator designator : pDesignatedInitializer.getDesignators()) {
        designatorsBuilder.add((CDesignator) designator.accept(this));
      }

      return new CDesignatedInitializer(
          pDesignatedInitializer.getFileLocation(),
          designatorsBuilder.build(),
          (CInitializer) pDesignatedInitializer.getRightHandSide().accept(this));
    }

    @Override
    public CFunctionCallExpression visit(CFunctionCallExpression pCFunctionCallExpression)
        throws X {

      ImmutableList.Builder<CExpression> parameterExpressionsBuilder =
          ImmutableList.builderWithExpectedSize(
              pCFunctionCallExpression.getParameterExpressions().size());

      for (CExpression parameterExpression : pCFunctionCallExpression.getParameterExpressions()) {
        parameterExpressionsBuilder.add((CExpression) parameterExpression.accept(this));
      }

      CFunctionDeclaration functionDeclaration = pCFunctionCallExpression.getDeclaration();
      if (functionDeclaration != null) {
        functionDeclaration = (CFunctionDeclaration) functionDeclaration.accept(this);
      }

      return new CFunctionCallExpression(
          pCFunctionCallExpression.getFileLocation(),
          pCFunctionCallExpression.getExpressionType(),
          (CExpression) pCFunctionCallExpression.getFunctionNameExpression().accept(this),
          parameterExpressionsBuilder.build(),
          functionDeclaration);
    }

    @Override
    public CExpression visit(CBinaryExpression pCBinaryExpression) throws X {
      return new CBinaryExpression(
          pCBinaryExpression.getFileLocation(),
          pCBinaryExpression.getExpressionType(),
          pCBinaryExpression.getCalculationType(),
          (CExpression) pCBinaryExpression.getOperand1().accept(this),
          (CExpression) pCBinaryExpression.getOperand2().accept(this),
          pCBinaryExpression.getOperator());
    }

    @Override
    public CExpression visit(CCastExpression pCCastExpression) throws X {
      return new CCastExpression(
          pCCastExpression.getFileLocation(),
          pCCastExpression.getExpressionType(),
          (CExpression) pCCastExpression.getOperand().accept(this));
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
      return new CUnaryExpression(
          pCUnaryExpression.getFileLocation(),
          pCUnaryExpression.getExpressionType(),
          (CExpression) pCUnaryExpression.getOperand().accept(this),
          pCUnaryExpression.getOperator());
    }

    @Override
    public CExpression visit(CImaginaryLiteralExpression pCImaginaryLiteralExpression) throws X {
      return new CImaginaryLiteralExpression(
          pCImaginaryLiteralExpression.getFileLocation(),
          pCImaginaryLiteralExpression.getExpressionType(),
          (CLiteralExpression) pCImaginaryLiteralExpression.getValue().accept(this));
    }

    @Override
    public CExpression visit(CAddressOfLabelExpression pCAddressOfLabelExpression) throws X {
      return pCAddressOfLabelExpression;
    }

    @Override
    public CExpression visit(CArraySubscriptExpression pCArraySubscriptExpression) throws X {
      return new CArraySubscriptExpression(
          pCArraySubscriptExpression.getFileLocation(),
          pCArraySubscriptExpression.getExpressionType(),
          (CExpression) pCArraySubscriptExpression.getArrayExpression().accept(this),
          (CExpression) pCArraySubscriptExpression.getSubscriptExpression().accept(this));
    }

    @Override
    public CExpression visit(CFieldReference pCFieldReference) throws X {
      return new CFieldReference(
          pCFieldReference.getFileLocation(),
          pCFieldReference.getExpressionType(),
          pCFieldReference.getFieldName(),
          (CExpression) pCFieldReference.getFieldOwner().accept(this),
          pCFieldReference.isPointerDereference());
    }

    @Override
    public CExpression visit(CIdExpression pCIdExpression) throws X {

      CSimpleDeclaration declaration = pCIdExpression.getDeclaration();
      if (declaration != null) {
        declaration = (CSimpleDeclaration) declaration.accept(this);
      }

      return new CIdExpression(
          pCIdExpression.getFileLocation(),
          pCIdExpression.getExpressionType(),
          pCIdExpression.getName(),
          declaration);
    }

    @Override
    public CExpression visit(CPointerExpression pCPointerExpression) throws X {
      return new CPointerExpression(
          pCPointerExpression.getFileLocation(),
          pCPointerExpression.getExpressionType(),
          (CExpression) pCPointerExpression.getOperand().accept(this));
    }

    @Override
    public CExpression visit(CComplexCastExpression pCComplexCastExpression) throws X {
      return new CComplexCastExpression(
          pCComplexCastExpression.getFileLocation(),
          pCComplexCastExpression.getExpressionType(),
          (CExpression) pCComplexCastExpression.getOperand().accept(this),
          pCComplexCastExpression.getType(),
          pCComplexCastExpression.isRealCast());
    }

    @Override
    public CFunctionDeclaration visit(CFunctionDeclaration pCFunctionDeclaration) throws X {

      ImmutableList.Builder<CParameterDeclaration> parametersBuilder =
          ImmutableList.builderWithExpectedSize(pCFunctionDeclaration.getParameters().size());

      for (CParameterDeclaration parameterDeclaration : pCFunctionDeclaration.getParameters()) {
        parametersBuilder.add((CParameterDeclaration) parameterDeclaration.accept(this));
      }

      return new CFunctionDeclaration(
          pCFunctionDeclaration.getFileLocation(),
          pCFunctionDeclaration.getType(),
          pCFunctionDeclaration.getName(),
          pCFunctionDeclaration.getOrigName(),
          parametersBuilder.build());
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

      CInitializer initializer = pCVariableDeclaration.getInitializer();
      if (initializer != null) {
        variableDeclaration.addInitializer((CInitializer) initializer.accept(this));
      }

      visitedVariableDeclaration = null;
      createdVariableDeclaration = null;

      return variableDeclaration;
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
      return new CExpressionStatement(
          pCExpressionStatement.getFileLocation(),
          (CExpression) pCExpressionStatement.getExpression().accept(this));
    }

    @Override
    public CStatement visit(CExpressionAssignmentStatement pCExpressionAssignmentStatement)
        throws X {
      return new CExpressionAssignmentStatement(
          pCExpressionAssignmentStatement.getFileLocation(),
          (CLeftHandSide) pCExpressionAssignmentStatement.getLeftHandSide().accept(this),
          (CExpression) pCExpressionAssignmentStatement.getRightHandSide().accept(this));
    }

    @Override
    public CStatement visit(CFunctionCallAssignmentStatement pCFunctionCallAssignmentStatement)
        throws X {
      return new CFunctionCallAssignmentStatement(
          pCFunctionCallAssignmentStatement.getFileLocation(),
          (CLeftHandSide) pCFunctionCallAssignmentStatement.getLeftHandSide().accept(this),
          (CFunctionCallExpression)
              pCFunctionCallAssignmentStatement.getFunctionCallExpression().accept(this));
    }

    @Override
    public CStatement visit(CFunctionCallStatement pCFunctionCallStatement) throws X {
      return new CFunctionCallStatement(
          pCFunctionCallStatement.getFileLocation(),
          (CFunctionCallExpression)
              pCFunctionCallStatement.getFunctionCallExpression().accept(this));
    }

    @Override
    public CReturnStatement visit(CReturnStatement pCReturnStatement) throws X {

      Optional<? extends CExpression> oldReturnValue = pCReturnStatement.getReturnValue();
      Optional<CExpression> newReturnValue = Optional.absent();
      if (oldReturnValue.isPresent()) {
        newReturnValue = Optional.of((CExpression) oldReturnValue.orNull().accept(this));
      }

      Optional<? extends CAssignment> oldAssignment = pCReturnStatement.asAssignment();
      Optional<CAssignment> newAssignment = Optional.absent();
      if (oldAssignment.isPresent()) {
        newAssignment = Optional.of((CAssignment) oldAssignment.orNull().accept(this));
      }

      return new CReturnStatement(
          pCReturnStatement.getFileLocation(), newReturnValue, newAssignment);
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
