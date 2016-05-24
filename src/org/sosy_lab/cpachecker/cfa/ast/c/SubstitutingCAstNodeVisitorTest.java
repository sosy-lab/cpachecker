/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.SubstitutingCAstNodeVisitor.SubstituteProvider;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class SubstitutingCAstNodeVisitorTest {

  /**
   * A substitute provider returning {@code null} for every method call.
   */
  private class NullSubstituteProvider implements SubstituteProvider {

    @Nullable
    @Override
    public CAstNode findSubstitute(final CAstNode pNode) {
      return null;
    }

    @Nullable
    @Override
    public CAstNode adjustTypesAfterSubstitution(final CAstNode pNode) {
      return null;
    }
  }

  /**
   * A substitute provider returning the exact same node it got as a parameter.
   */
  private class IdentitySubstituteProvider implements SubstituteProvider {

    @Nullable
    @Override
    public CAstNode findSubstitute(final CAstNode pNode) {
      return pNode;
    }

    @Nullable
    @Override
    public CAstNode adjustTypesAfterSubstitution(final CAstNode pNode) {
      return pNode;
    }
  }

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private SubstitutingCAstNodeVisitor nullVisitor;
  private SubstitutingCAstNodeVisitor identityVisitor;

  private CArrayDesignator arrayDesignator;
  private CArrayRangeDesignator arrayRangeDesignator;
  private CFieldDesignator fieldDesignator;
  private CInitializerList initializerList;
  private CReturnStatement returnStatement;
  private CDesignatedInitializer designatedInitializer;
  private CInitializerExpression initializerExpression;
  private CFunctionCallExpression functionCallExpression;
  private CBinaryExpression binaryExpression;
  private CCastExpression castExpression;
  private CTypeIdExpression typeIdExpression;
  private CUnaryExpression unaryExpression;
  private CArraySubscriptExpression arraySubscriptExpression;
  private CComplexCastExpression complexCastExpression;
  private CFieldReference fieldReference;
  private CIdExpression idExpression;
  private CPointerExpression pointerExpression;
  private CCharLiteralExpression charLiteralExpression;
  private CFloatLiteralExpression floatLiteralExpression;
  private CImaginaryLiteralExpression imaginaryLiteralExpression;
  private CIntegerLiteralExpression integerLiteralExpression;
  private CStringLiteralExpression stringLiteralExpression;
  private CAddressOfLabelExpression addressOfLabelExpression;
  private CParameterDeclaration parameterDeclaration;
  private CFunctionDeclaration functionDeclaration;
  private CComplexTypeDeclaration complexTypeDeclaration;
  private CTypeDefDeclaration typeDefDeclaration;
  private CVariableDeclaration variableDeclaration;
  private CExpressionAssignmentStatement expressionAssignmentStatement;
  private CExpressionStatement expressionStatement;
  private CFunctionCallAssignmentStatement functionCallAssignmentStatement;
  private CFunctionCallStatement functionCallStatement;
  private CEnumerator enumerator;

  @Before
  public void setUp() {
    nullVisitor = new SubstitutingCAstNodeVisitor(new NullSubstituteProvider());
    identityVisitor = new SubstitutingCAstNodeVisitor(new IdentitySubstituteProvider());

    // {@code visit} methods for these types are currently not implemented. We check only, if the
    // expected exception is thrown in the test. Therefore we set these types to {@code null} as
    // they are not needed.
    returnStatement = null;
    designatedInitializer = null;
    initializerExpression = null;
    functionCallExpression = null;
    complexCastExpression = null;
    addressOfLabelExpression = null;
    parameterDeclaration = null;
    functionDeclaration = null;
    complexTypeDeclaration = null;
    typeDefDeclaration = null;
    functionCallAssignmentStatement = null;
    functionCallStatement = null;
    enumerator = null;

    // Initialization for other types
    variableDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            true,
            CStorageClass.AUTO,
            CNumericTypes.INT,
            "tmp",
            "tmp",
            "tmp",
            new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ZERO));
    idExpression = new CIdExpression(FileLocation.DUMMY, variableDeclaration);
    charLiteralExpression = new CCharLiteralExpression(FileLocation.DUMMY, CNumericTypes.CHAR, 'x');
    floatLiteralExpression =
        new CFloatLiteralExpression(FileLocation.DUMMY, CNumericTypes.FLOAT, new BigDecimal(42.0));
    imaginaryLiteralExpression =
        new CImaginaryLiteralExpression(
            FileLocation.DUMMY, CNumericTypes.FLOAT, floatLiteralExpression);
    integerLiteralExpression = CIntegerLiteralExpression.ONE;
    expressionAssignmentStatement =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, idExpression, CIntegerLiteralExpression.ONE);
    expressionStatement = new CExpressionStatement(FileLocation.DUMMY, idExpression);
    arrayDesignator = new CArrayDesignator(FileLocation.DUMMY, idExpression);
    arrayRangeDesignator =
        new CArrayRangeDesignator(
            FileLocation.DUMMY, CIntegerLiteralExpression.ZERO, CIntegerLiteralExpression.ONE);
    fieldDesignator = new CFieldDesignator(FileLocation.DUMMY, "dummy");
    List<CInitializer> tmp = new ArrayList<>();
    tmp.add(new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ZERO));
    tmp.add(new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ONE));
    initializerList = new CInitializerList(FileLocation.DUMMY, tmp);
    binaryExpression =
        new CBinaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            CNumericTypes.INT,
            CIntegerLiteralExpression.ZERO,
            CIntegerLiteralExpression.ONE,
            BinaryOperator.LESS_THAN);
    castExpression =
        new CCastExpression(FileLocation.DUMMY, CNumericTypes.INT, CIntegerLiteralExpression.ZERO);
    typeIdExpression =
        new CTypeIdExpression(
            FileLocation.DUMMY, CNumericTypes.INT, TypeIdOperator.SIZEOF, CNumericTypes.INT);
    unaryExpression =
        new CUnaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            CIntegerLiteralExpression.ONE,
            UnaryOperator.MINUS);
    arraySubscriptExpression = null;
    fieldReference = null;
    pointerExpression = null;
    stringLiteralExpression =
        new CStringLiteralExpression(FileLocation.DUMMY, CNumericTypes.UNSIGNED_CHAR, "test");
  }

  @Test
  public void visitCArrayDesignator() {
    assertThat(nullVisitor.visit(arrayDesignator)).isEqualTo(arrayDesignator);
    assertThat(identityVisitor.visit(arrayDesignator)).isEqualTo(arrayDesignator);
  }

  @Test
  public void visitCArrayRangeDesignator() {
    assertThat(nullVisitor.visit(arrayRangeDesignator)).isEqualTo(arrayRangeDesignator);
    assertThat(identityVisitor.visit(arrayRangeDesignator)).isEqualTo(arrayRangeDesignator);
  }

  @Test
  public void visitCFieldDesignator() {
    assertThat(nullVisitor.visit(fieldDesignator)).isEqualTo(fieldDesignator);
    assertThat(identityVisitor.visit(fieldDesignator)).isEqualTo(fieldDesignator);
  }

  @Test
  public void visitCInitializerList() {
    exception.expect(UnsupportedOperationException.class);
    nullVisitor.visit(initializerList);
  }

  @Test
  public void visitCReturnStatement() {
    exception.expect(UnsupportedOperationException.class);
    nullVisitor.visit(returnStatement);
  }

  @Test
  public void visitCDesignatedInitializer() {
    exception.expect(UnsupportedOperationException.class);
    nullVisitor.visit(designatedInitializer);
  }

  @Test
  public void visitCInitializerExpression() {
    exception.expect(UnsupportedOperationException.class);
    nullVisitor.visit(initializerExpression);
  }

  @Test
  public void visitCFunctionCallExpression() {
    exception.expect(UnsupportedOperationException.class);
    nullVisitor.visit(functionCallExpression);
  }

  @Test
  public void visitCBinaryExpression() {
    assertThat(nullVisitor.visit(binaryExpression)).isEqualTo(binaryExpression);
    assertThat(identityVisitor.visit(binaryExpression)).isEqualTo(binaryExpression);
  }

  @Test
  public void visitCCastExpression() {
    assertThat(nullVisitor.visit(castExpression)).isEqualTo(castExpression);
    assertThat(identityVisitor.visit(castExpression)).isEqualTo(castExpression);
  }

  @Test
  public void visitCTypeIdExpression() {
    assertThat(nullVisitor.visit(typeIdExpression)).isEqualTo(typeIdExpression);
    assertThat(identityVisitor.visit(typeIdExpression)).isEqualTo(typeIdExpression);
  }

  @Test
  public void visitCUnaryExpression() {
    assertThat(nullVisitor.visit(unaryExpression)).isEqualTo(unaryExpression);
    assertThat(identityVisitor.visit(unaryExpression)).isEqualTo(unaryExpression);
  }

  @Ignore
  @Test
  public void visitCArraySubscriptExpression() {}

  @Test
  public void visitCComplexCastException() {
    exception.expect(UnsupportedOperationException.class);
    nullVisitor.visit(complexCastExpression);
  }

  @Ignore
  @Test
  public void visitCFieldReference() {}

  @Test
  public void visitCIdExpression() {
    assertThat(nullVisitor.visit(idExpression)).isEqualTo(idExpression);
    assertThat(identityVisitor.visit(idExpression)).isEqualTo(idExpression);
  }

  @Ignore
  @Test
  public void visitCPointerExpression() {}

  @Test
  public void visitCCharLiteralExpression() {
    assertThat(nullVisitor.visit(charLiteralExpression)).isEqualTo(charLiteralExpression);
    assertThat(identityVisitor.visit(charLiteralExpression)).isEqualTo(charLiteralExpression);
  }

  @Test
  public void visitCFloatLiteralExpression() {
    assertThat(nullVisitor.visit(floatLiteralExpression)).isEqualTo(floatLiteralExpression);
    assertThat(identityVisitor.visit(floatLiteralExpression)).isEqualTo(floatLiteralExpression);
  }

  @Test
  public void visitCImaginaryLiteralExpression() {
    assertThat(nullVisitor.visit(imaginaryLiteralExpression)).isEqualTo(imaginaryLiteralExpression);
    assertThat(identityVisitor.visit(imaginaryLiteralExpression))
        .isEqualTo(imaginaryLiteralExpression);
  }

  @Test
  public void visitCIntegerLiteralExpression() {
    assertThat(nullVisitor.visit(integerLiteralExpression)).isEqualTo(integerLiteralExpression);
    assertThat(identityVisitor.visit(integerLiteralExpression)).isEqualTo(integerLiteralExpression);
  }

  @Test
  public void visitStringLiteralExpression() {
    assertThat(nullVisitor.visit(stringLiteralExpression)).isEqualTo(stringLiteralExpression);
    assertThat(identityVisitor.visit(stringLiteralExpression)).isEqualTo(stringLiteralExpression);
  }

  @Test
  public void visitCAddressOfLabelExpression() {
    exception.expect(UnsupportedOperationException.class);
    nullVisitor.visit(addressOfLabelExpression);
  }

  @Test
  public void visitCParameterDeclaration() {
    exception.expect(UnsupportedOperationException.class);
    nullVisitor.visit(parameterDeclaration);
  }

  @Test
  public void visitCFunctionDeclaration() {
    exception.expect(UnsupportedOperationException.class);
    nullVisitor.visit(functionDeclaration);
  }

  @Test
  public void visitCComplexTypeDeclaration() {
    exception.expect(UnsupportedOperationException.class);
    nullVisitor.visit(complexTypeDeclaration);
  }

  @Test
  public void visitCTypeDefDeclaration() {
    exception.expect(UnsupportedOperationException.class);
    nullVisitor.visit(typeDefDeclaration);
  }

  @Test
  public void visitCVariableDeclaration() {
    assertThat(nullVisitor.visit(variableDeclaration)).isEqualTo(variableDeclaration);
    assertThat(identityVisitor.visit(variableDeclaration)).isEqualTo(variableDeclaration);
  }

  @Test
  public void visitCExpressionAssignmentStatement() {
    assertThat(nullVisitor.visit(expressionAssignmentStatement))
        .isEqualTo(expressionAssignmentStatement);
    assertThat(identityVisitor.visit(expressionAssignmentStatement))
        .isEqualTo(expressionAssignmentStatement);
  }

  @Test
  public void visitCExpressionStatement() {
    assertThat(nullVisitor.visit(expressionStatement)).isEqualTo(expressionStatement);
    assertThat(identityVisitor.visit(expressionStatement)).isEqualTo(expressionStatement);
  }

  @Test
  public void visitCFunctionCallAssignmentStatement() {
    exception.expect(UnsupportedOperationException.class);
    nullVisitor.visit(functionCallAssignmentStatement);
  }

  @Test
  public void visitCFunctionCallStatement() {
    exception.expect(UnsupportedOperationException.class);
    nullVisitor.visit(functionCallStatement);
  }

  @Test
  public void visitCEnumerator() {
    exception.expect(UnsupportedOperationException.class);
    nullVisitor.visit(enumerator);
  }

}
