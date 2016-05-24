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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sosy_lab.cpachecker.cfa.ast.c.SubstitutingCAstNodeVisitor.SubstituteProvider;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;

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
    arrayDesignator = null;
    arrayRangeDesignator = null;
    fieldDesignator = null;
    initializerList = null;
    binaryExpression = null;
    castExpression = null;
    typeIdExpression = null;
    unaryExpression = null;
    arraySubscriptExpression = null;
    fieldReference = null;
    idExpression = null;
    pointerExpression = null;
    charLiteralExpression = null;
    floatLiteralExpression = null;
    imaginaryLiteralExpression = null;
    integerLiteralExpression = null;
    stringLiteralExpression = null;
    variableDeclaration = null;
    expressionAssignmentStatement = null;
    expressionStatement = null;
  }

  @Test
  public void visitCArrayDesignator() {

  }

  @Test
  public void visitCArrayRangeDesignator() {

  }

  @Test
  public void visitCFieldDesignator() {

  }

  @Test
  public void visitCInitializerList() {

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

  }

  @Test
  public void visitCCastExpression() {

  }

  @Test
  public void visitCTypeIdExpression() {

  }

  @Test
  public void visitCUnaryExpression() {

  }

  @Test
  public void visitCArraySubscriptExpression() {

  }

  @Test
  public void visitCComplexCastException() {
    exception.expect(UnsupportedOperationException.class);
    nullVisitor.visit(complexCastExpression);
  }

  @Test
  public void visitCFieldReference() {

  }

  @Test
  public void visitCIdExpression() {

  }

  @Test
  public void visitCPointerExpression() {

  }

  @Test
  public void visitCCharLiteralExpression() {

  }

  @Test
  public void visitCFloatLiteralExpression() {

  }

  @Test
  public void visitCImaginaryLiteralExpression() {

  }

  @Test
  public void visitCIntegerLiteralExpression() {

  }

  @Test
  public void visitStringLiteralExpression() {

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

  }

  @Test
  public void visitCExpressionAssignmentStatement() {

  }

  @Test
  public void visitCExpressionStatement() {

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
