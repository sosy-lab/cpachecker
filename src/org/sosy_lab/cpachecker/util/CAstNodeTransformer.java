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
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;

/** An instance of this abstract class can be used to modify specific parts of an AST. */
public abstract class CAstNodeTransformer<X extends Exception>
    implements CAstNodeVisitor<CAstNode, X> {

  public static CAstNodeTransformer<ImpossibleException> createIdentityTransformer() {
    return new FastIdentityTransformer<>();
  }

  public CAstNode transform(CAstNode pCAstNode) throws X {
    return pCAstNode.accept(this);
  }

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
  public CFunctionCallExpression visit(CFunctionCallExpression pCFunctionCallExpression) throws X {

    ImmutableList.Builder<CExpression> parameterExpressionsBuilder =
        ImmutableList.builderWithExpectedSize(
            pCFunctionCallExpression.getParameterExpressions().size());

    for (CExpression parameterExpression : pCFunctionCallExpression.getParameterExpressions()) {
      parameterExpressionsBuilder.add((CExpression) parameterExpression.accept(this));
    }

    return new CFunctionCallExpression(
        pCFunctionCallExpression.getFileLocation(),
        pCFunctionCallExpression.getExpressionType(),
        (CExpression) pCFunctionCallExpression.getFunctionNameExpression().accept(this),
        parameterExpressionsBuilder.build(),
        (CFunctionDeclaration) pCFunctionCallExpression.getDeclaration().accept(this));
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
  public CLiteralExpression visit(CCharLiteralExpression pCCharLiteralExpression) throws X {
    return pCCharLiteralExpression;
  }

  @Override
  public CLiteralExpression visit(CFloatLiteralExpression pCFloatLiteralExpression) throws X {
    return pCFloatLiteralExpression;
  }

  @Override
  public CLiteralExpression visit(CIntegerLiteralExpression pCIntegerLiteralExpression) throws X {
    return pCIntegerLiteralExpression;
  }

  @Override
  public CLiteralExpression visit(CStringLiteralExpression pCStringLiteralExpression) throws X {
    return pCStringLiteralExpression;
  }

  @Override
  public CTypeIdExpression visit(CTypeIdExpression pCTypeIdExpression) throws X {
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
  public CLiteralExpression visit(CImaginaryLiteralExpression pCImaginaryLiteralExpression)
      throws X {
    return new CImaginaryLiteralExpression(
        pCImaginaryLiteralExpression.getFileLocation(),
        pCImaginaryLiteralExpression.getExpressionType(),
        (CLiteralExpression) pCImaginaryLiteralExpression.getValue().accept(this));
  }

  @Override
  public CAddressOfLabelExpression visit(CAddressOfLabelExpression pCAddressOfLabelExpression)
      throws X {
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
  public CFieldReference visit(CFieldReference pCFieldReference) throws X {
    return new CFieldReference(
        pCFieldReference.getFileLocation(),
        pCFieldReference.getExpressionType(),
        pCFieldReference.getFieldName(),
        (CExpression) pCFieldReference.getFieldOwner().accept(this),
        pCFieldReference.isPointerDereference());
  }

  @Override
  public CIdExpression visit(CIdExpression pCIdExpression) throws X {
    return new CIdExpression(
        pCIdExpression.getFileLocation(),
        pCIdExpression.getExpressionType(),
        pCIdExpression.getName(),
        (CSimpleDeclaration) pCIdExpression.getDeclaration().accept(this));
  }

  @Override
  public CPointerExpression visit(CPointerExpression pCPointerExpression) throws X {
    return new CPointerExpression(
        pCPointerExpression.getFileLocation(),
        pCPointerExpression.getExpressionType(),
        (CExpression) pCPointerExpression.getOperand().accept(this));
  }

  @Override
  public CComplexCastExpression visit(CComplexCastExpression pCComplexCastExpression) throws X {
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
  public CComplexTypeDeclaration visit(CComplexTypeDeclaration pCComplexTypeDeclaration) throws X {
    return pCComplexTypeDeclaration;
  }

  @Override
  public CTypeDefDeclaration visit(CTypeDefDeclaration pCTypeDefDeclaration) throws X {
    return pCTypeDefDeclaration;
  }

  @Override
  public CVariableDeclaration visit(CVariableDeclaration pCVariableDeclaration) throws X {
    CInitializer initializer = pCVariableDeclaration.getInitializer();
    if (initializer != null) {
      return new CVariableDeclaration(
          pCVariableDeclaration.getFileLocation(),
          pCVariableDeclaration.isGlobal(),
          pCVariableDeclaration.getCStorageClass(),
          pCVariableDeclaration.getType(),
          pCVariableDeclaration.getName(),
          pCVariableDeclaration.getOrigName(),
          pCVariableDeclaration.getQualifiedName(),
          (CInitializer) pCVariableDeclaration.getInitializer().accept(this));
    } else {
      return new CVariableDeclaration(
          pCVariableDeclaration.getFileLocation(),
          pCVariableDeclaration.isGlobal(),
          pCVariableDeclaration.getCStorageClass(),
          pCVariableDeclaration.getType(),
          pCVariableDeclaration.getName(),
          pCVariableDeclaration.getOrigName(),
          pCVariableDeclaration.getQualifiedName(),
          null);
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
  public CExpressionStatement visit(CExpressionStatement pCExpressionStatement) throws X {
    return new CExpressionStatement(
        pCExpressionStatement.getFileLocation(),
        (CExpression) pCExpressionStatement.getExpression().accept(this));
  }

  @Override
  public CExpressionAssignmentStatement visit(
      CExpressionAssignmentStatement pCExpressionAssignmentStatement) throws X {
    return new CExpressionAssignmentStatement(
        pCExpressionAssignmentStatement.getFileLocation(),
        (CLeftHandSide) pCExpressionAssignmentStatement.getLeftHandSide().accept(this),
        (CExpression) pCExpressionAssignmentStatement.getRightHandSide().accept(this));
  }

  @Override
  public CFunctionCallAssignmentStatement visit(
      CFunctionCallAssignmentStatement pCFunctionCallAssignmentStatement) throws X {
    return new CFunctionCallAssignmentStatement(
        pCFunctionCallAssignmentStatement.getFileLocation(),
        (CLeftHandSide) pCFunctionCallAssignmentStatement.getLeftHandSide().accept(this),
        (CFunctionCallExpression)
            pCFunctionCallAssignmentStatement.getFunctionCallExpression().accept(this));
  }

  @Override
  public CFunctionCallStatement visit(CFunctionCallStatement pCFunctionCallStatement) throws X {
    return new CFunctionCallStatement(
        pCFunctionCallStatement.getFileLocation(),
        (CFunctionCallExpression) pCFunctionCallStatement.getFunctionCallExpression().accept(this));
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

    return new CReturnStatement(pCReturnStatement.getFileLocation(), newReturnValue, newAssignment);
  }

  public static final class ImpossibleException extends RuntimeException {

    private static final long serialVersionUID = 2348284925894663519L;

    private ImpossibleException() {}
  }

  private static final class FastIdentityTransformer<X extends Exception>
      extends CAstNodeTransformer<X> {

    @Override
    public CArrayDesignator visit(CArrayDesignator pCArrayDesignator) throws X {
      return pCArrayDesignator;
    }

    @Override
    public CArrayRangeDesignator visit(CArrayRangeDesignator pCArrayRangeDesignator) throws X {
      return pCArrayRangeDesignator;
    }

    @Override
    public CFieldDesignator visit(CFieldDesignator pCFieldDesignator) throws X {
      return pCFieldDesignator;
    }

    @Override
    public CInitializerExpression visit(CInitializerExpression pCInitializerExpression) throws X {
      return pCInitializerExpression;
    }

    @Override
    public CInitializerList visit(CInitializerList pCInitializerList) throws X {
      return pCInitializerList;
    }

    @Override
    public CDesignatedInitializer visit(CDesignatedInitializer pDesignatedInitializer) throws X {
      return pDesignatedInitializer;
    }

    @Override
    public CFunctionCallExpression visit(CFunctionCallExpression pCFunctionCallExpression)
        throws X {
      return pCFunctionCallExpression;
    }

    @Override
    public CBinaryExpression visit(CBinaryExpression pCBinaryExpression) throws X {
      return pCBinaryExpression;
    }

    @Override
    public CCastExpression visit(CCastExpression pCCastExpression) throws X {
      return pCCastExpression;
    }

    @Override
    public CCharLiteralExpression visit(CCharLiteralExpression pCCharLiteralExpression) throws X {
      return pCCharLiteralExpression;
    }

    @Override
    public CFloatLiteralExpression visit(CFloatLiteralExpression pCFloatLiteralExpression)
        throws X {
      return pCFloatLiteralExpression;
    }

    @Override
    public CIntegerLiteralExpression visit(CIntegerLiteralExpression pCIntegerLiteralExpression)
        throws X {
      return pCIntegerLiteralExpression;
    }

    @Override
    public CStringLiteralExpression visit(CStringLiteralExpression pCStringLiteralExpression)
        throws X {
      return pCStringLiteralExpression;
    }

    @Override
    public CTypeIdExpression visit(CTypeIdExpression pCTypeIdExpression) throws X {
      return pCTypeIdExpression;
    }

    @Override
    public CUnaryExpression visit(CUnaryExpression pCUnaryExpression) throws X {
      return pCUnaryExpression;
    }

    @Override
    public CImaginaryLiteralExpression visit(
        CImaginaryLiteralExpression pCImaginaryLiteralExpression) throws X {
      return pCImaginaryLiteralExpression;
    }

    @Override
    public CAddressOfLabelExpression visit(CAddressOfLabelExpression pCAddressOfLabelExpression)
        throws X {
      return pCAddressOfLabelExpression;
    }

    @Override
    public CArraySubscriptExpression visit(CArraySubscriptExpression pCArraySubscriptExpression)
        throws X {
      return pCArraySubscriptExpression;
    }

    @Override
    public CFieldReference visit(CFieldReference pCFieldReference) throws X {
      return pCFieldReference;
    }

    @Override
    public CIdExpression visit(CIdExpression pCIdExpression) throws X {
      return pCIdExpression;
    }

    @Override
    public CPointerExpression visit(CPointerExpression pCPointerExpression) throws X {
      return pCPointerExpression;
    }

    @Override
    public CComplexCastExpression visit(CComplexCastExpression pCComplexCastExpression) throws X {
      return pCComplexCastExpression;
    }

    @Override
    public CFunctionDeclaration visit(CFunctionDeclaration pCFunctionDeclaration) throws X {
      return pCFunctionDeclaration;
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
      return pCVariableDeclaration;
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
    public CExpressionStatement visit(CExpressionStatement pCExpressionStatement) throws X {
      return pCExpressionStatement;
    }

    @Override
    public CExpressionAssignmentStatement visit(
        CExpressionAssignmentStatement pCExpressionAssignmentStatement) throws X {
      return pCExpressionAssignmentStatement;
    }

    @Override
    public CFunctionCallAssignmentStatement visit(
        CFunctionCallAssignmentStatement pCFunctionCallAssignmentStatement) throws X {
      return pCFunctionCallAssignmentStatement;
    }

    @Override
    public CFunctionCallStatement visit(CFunctionCallStatement pCFunctionCallStatement) throws X {
      return pCFunctionCallStatement;
    }

    @Override
    public CReturnStatement visit(CReturnStatement pCReturnStatement) throws X {
      return pCReturnStatement;
    }
  }
}
