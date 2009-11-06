/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.invariant.util;

import org.eclipse.cdt.core.dom.ast.ASTNodeProperty;
import org.eclipse.cdt.core.dom.ast.ASTSignatureUtil;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.parser.IToken;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Hack!!!
 * @author g.theoduloz
 */
public class DummyASTUnaryExpression implements IASTUnaryExpression {

  private int operator;
  private IASTExpression operand;
  
  public DummyASTUnaryExpression(int op, IASTExpression op1)
  {
    operator = op;
    op1 = operand;
  }
  
  @Override
  public IASTUnaryExpression copy() {
    throw new NotImplementedException();
  }

  @Override
  public IASTExpression getOperand() {
    return operand;
  }

  @Override
  public int getOperator() {
    return operator;
  }

  @Override
  public void setOperand(IASTExpression pExpression) {
    operand = pExpression;
  }

  @Override
  public void setOperator(int pValue) {
    operator = pValue;
  }

  @Override
  public IType getExpressionType() {
    throw new NotImplementedException();
  }

  @Override
  public boolean accept(ASTVisitor pVisitor) {
    throw new NotImplementedException();
  }

  @Override
  public boolean contains(IASTNode pNode) {
    throw new NotImplementedException();
  }

  @Override
  public IASTNode[] getChildren() {
    throw new NotImplementedException();
  }

  @Override
  public String getContainingFilename() {
    throw new NotImplementedException();
  }

  @Override
  public IASTFileLocation getFileLocation() {
    throw new NotImplementedException();
  }

  @Override
  public IToken getLeadingSyntax() throws ExpansionOverlapsBoundaryException,
      UnsupportedOperationException {
    throw new NotImplementedException();
  }

  @Override
  public IASTNodeLocation[] getNodeLocations() {
    throw new NotImplementedException();
  }

  @Override
  public IASTNode getParent() {
    throw new NotImplementedException();
  }

  @Override
  public ASTNodeProperty getPropertyInParent() {
    throw new NotImplementedException();
  }

  @Override
  public String getRawSignature() {
    return ASTSignatureUtil.getUnaryOperatorString(this) + operand.getRawSignature();
  }

  @Override
  public IToken getSyntax() throws ExpansionOverlapsBoundaryException {
    throw new NotImplementedException();
  }

  @Override
  public IToken getTrailingSyntax() throws ExpansionOverlapsBoundaryException,
      UnsupportedOperationException {
    throw new NotImplementedException();
  }

  @Override
  public IASTTranslationUnit getTranslationUnit() {
    throw new NotImplementedException();
  }

  @Override
  public boolean isActive() {
    throw new NotImplementedException();
  }

  @Override
  public boolean isFrozen() {
    throw new NotImplementedException();
  }

  @Override
  public boolean isPartOfTranslationUnitFile() {
    throw new NotImplementedException();
  }

  @Override
  public void setParent(IASTNode pNode) {
    throw new NotImplementedException();
  }

  @Override
  public void setPropertyInParent(ASTNodeProperty pProperty) {
    throw new NotImplementedException();
  }

  @Override
  public String toString() {
    return getRawSignature();
  }
  
}
