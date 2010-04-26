/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.assumptions;

import org.eclipse.cdt.core.dom.ast.ASTNodeProperty;
import org.eclipse.cdt.core.dom.ast.ASTSignatureUtil;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.parser.IToken;

/**
 * Hack!!!
 * @author g.theoduloz
 */
public class DummyASTBinaryExpression implements IASTBinaryExpression {

  private int operator;
  private IASTExpression operand1;
  private IASTExpression operand2;

  public DummyASTBinaryExpression(int op, IASTExpression op1, IASTExpression op2)
  {
    operator = op;
    operand1 = op1;
    operand2 = op2;
  }

  @Override
  public IASTBinaryExpression copy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTExpression getOperand1() {
    return operand1;
  }

  @Override
  public IASTExpression getOperand2() {
    return operand2;
  }

  @Override
  public int getOperator() {
    return operator;
  }

  @Override
  public void setOperand1(IASTExpression pExpression) {
    operand1 = pExpression;
  }

  @Override
  public void setOperand2(IASTExpression pExpression) {
    operand2 = pExpression;
  }

  @Override
  public void setOperator(int pOp) {
    operator = pOp;
  }

  @Override
  public IType getExpressionType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean accept(ASTVisitor pVisitor) {
    return false;
  }

  @Override
  public boolean contains(IASTNode pNode) {
    return false;
  }

  @Override
  public IASTNode[] getChildren() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getContainingFilename() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTFileLocation getFileLocation() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IToken getLeadingSyntax() throws ExpansionOverlapsBoundaryException,
      UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTNodeLocation[] getNodeLocations() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTNode getParent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ASTNodeProperty getPropertyInParent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getRawSignature() {
    return
      "(" + operand1.getRawSignature()
      + ASTSignatureUtil.getBinaryOperatorString(this)
      + operand2.getRawSignature() + ")";
  }

  @Override
  public IToken getSyntax() throws ExpansionOverlapsBoundaryException {
    throw new UnsupportedOperationException();
  }

  @Override
  public IToken getTrailingSyntax() throws ExpansionOverlapsBoundaryException,
      UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTTranslationUnit getTranslationUnit() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isActive() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isFrozen() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isPartOfTranslationUnitFile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setParent(IASTNode pNode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setPropertyInParent(ASTNodeProperty pProperty) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return getRawSignature();
  }

}
