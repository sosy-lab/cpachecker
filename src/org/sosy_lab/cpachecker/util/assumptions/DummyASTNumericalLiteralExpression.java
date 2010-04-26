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

import java.math.BigDecimal;

import org.eclipse.cdt.core.dom.ast.ASTNodeProperty;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.parser.IToken;

/**
 * Hack!!
 * @author g.theoduloz
 */
public class DummyASTNumericalLiteralExpression implements
    IASTLiteralExpression, Comparable<DummyASTNumericalLiteralExpression> {

  private String representation;

  public DummyASTNumericalLiteralExpression(String r) {
    representation = r;
  }

  @Override
  public IASTLiteralExpression copy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getKind() {
    return IASTLiteralExpression.lk_integer_constant;
  }

  @Override
  public char[] getValue() {
    return representation.toCharArray();
  }

  @Override
  public void setKind(int pValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setValue(char[] pValue) {
    representation = new String(pValue);
  }

  @Override
  public void setValue(String pValue) {
    representation = pValue;
  }

  @Override
  public IType getExpressionType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean accept(ASTVisitor pVisitor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean contains(IASTNode pNode) {
    throw new UnsupportedOperationException();
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
    return representation;
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
  public int compareTo(DummyASTNumericalLiteralExpression other) {
    if (this == other) return 0;
    BigDecimal decThis = new BigDecimal(representation);
    BigDecimal decOther = new BigDecimal(other.representation);
    return decThis.compareTo(decOther);
  }

  @Override
  public String toString() {
    return getRawSignature();
  }

  /* Constants, assuming 32-bit machine */
  public static final DummyASTNumericalLiteralExpression ZERO = new DummyASTNumericalLiteralExpression("0");
  public static final DummyASTNumericalLiteralExpression FALSE = ZERO;
  public static final DummyASTNumericalLiteralExpression ONE = new DummyASTNumericalLiteralExpression("1");
  public static final DummyASTNumericalLiteralExpression TRUE = ONE;

  public static final DummyASTNumericalLiteralExpression INT_MAX = new DummyASTNumericalLiteralExpression("2147483647");
  public static final DummyASTNumericalLiteralExpression INT_MIN = new DummyASTNumericalLiteralExpression("-2147483648");
  public static final DummyASTNumericalLiteralExpression UINT_MIN = ZERO;
  public static final DummyASTNumericalLiteralExpression UINT_MAX = new DummyASTNumericalLiteralExpression("4294967295");

  public static final DummyASTNumericalLiteralExpression LONG_MAX = new DummyASTNumericalLiteralExpression("2147483647");
  public static final DummyASTNumericalLiteralExpression LONG_MIN = new DummyASTNumericalLiteralExpression("-2147483648");
  public static final DummyASTNumericalLiteralExpression ULONG_MIN = ZERO;
  public static final DummyASTNumericalLiteralExpression ULONG_MAX = new DummyASTNumericalLiteralExpression("4294967295");

  public static final DummyASTNumericalLiteralExpression SHRT_MAX = new DummyASTNumericalLiteralExpression("32767");
  public static final DummyASTNumericalLiteralExpression SHRT_MIN = new DummyASTNumericalLiteralExpression("-32768");
  public static final DummyASTNumericalLiteralExpression USHRT_MIN = ZERO;
  public static final DummyASTNumericalLiteralExpression USHRT_MAX = new DummyASTNumericalLiteralExpression("65535");

  public static final DummyASTNumericalLiteralExpression CHAR_MAX = new DummyASTNumericalLiteralExpression("127");
  public static final DummyASTNumericalLiteralExpression CHAR_MIN = new DummyASTNumericalLiteralExpression("-128");
  public static final DummyASTNumericalLiteralExpression UCHAR_MIN = ZERO;
  public static final DummyASTNumericalLiteralExpression UCHAR_MAX = new DummyASTNumericalLiteralExpression("255");

}
