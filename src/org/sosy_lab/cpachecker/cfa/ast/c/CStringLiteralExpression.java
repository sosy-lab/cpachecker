// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public final class CStringLiteralExpression extends AStringLiteralExpression
    implements CLiteralExpression {

  private static final long serialVersionUID = 2656216584704518185L;

  public CStringLiteralExpression(FileLocation pFileLocation, CType pType, String pValue) {
    super(pFileLocation, pType, pValue);
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public String toASTString() {
    return getValue();
  }

  public String getContentString() {
    String literal = getValue();
    return literal.substring(1, literal.length() - 1);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CStringLiteralExpression)) {
      return false;
    }

    return super.equals(obj);
  }

  /**
   * Expand a string literal to an array of characters.
   *
   * <p>http://stackoverflow.com/a/6915917 As the C99 Draft Specification's 32nd Example in §6.7.8
   * (p. 130) states char s[] = "abc", t[3] = "abc"; is identical to: char s[] = { 'a', 'b', 'c',
   * '\0' }, t[] = { 'a', 'b', 'c' };
   *
   * @param type The type of the character array.
   * @return List of character-literal expressions
   */
  public List<CCharLiteralExpression> expandStringLiteral(final CArrayType type) {
    // The string is either NULL terminated, or not.
    // If the length is not provided explicitly, NULL termination is used
    final String s = getContentString();
    final int length = type.getLengthAsInt().orElse(s.length() + 1);
    assert length >= s.length();

    // create one CharLiteralExpression for each character of the string
    final List<CCharLiteralExpression> result = new ArrayList<>();
    for (int i = 0; i < s.length(); i++) {
      result.add(
          new CCharLiteralExpression(getFileLocation(), CNumericTypes.SIGNED_CHAR, s.charAt(i)));
    }

    // http://stackoverflow.com/questions/10828294/c-and-c-partial-initialization-of-automatic-structure
    // C99 Standard 6.7.8.21
    // If there are ... fewer characters in a string literal
    // used to initialize an array of known size than there are elements in the array,
    // the remainder of the aggregate shall be initialized implicitly ...
    for (int i = s.length(); i < length; i++) {
      result.add(new CCharLiteralExpression(getFileLocation(), CNumericTypes.SIGNED_CHAR, '\0'));
    }

    return result;
  }

  public CArrayType transformTypeToArrayType() throws UnrecognizedCodeException {

    CExpression length =
        new CIntegerLiteralExpression(
            getFileLocation(),
            new CSimpleType(
                false, false, CBasicType.INT, false, false, false, false, false, false, false),
            BigInteger.valueOf(getValue().length() - 1));

    if (getExpressionType() instanceof CArrayType) {
      CArrayType arrayType = (CArrayType) getExpressionType();
      if (arrayType.getLengthAsInt().orElse(0) == 0) {
        arrayType = new CArrayType(false, false, arrayType.getType(), length);
      }
      return arrayType;
    } else if (getExpressionType() instanceof CPointerType) {
      return new CArrayType(false, false, ((CPointerType) getExpressionType()).getType(), length);
    } else {
      throw new UnrecognizedCodeException(
          "Assigning string literal to " + getExpressionType(), this);
    }
  }
}
