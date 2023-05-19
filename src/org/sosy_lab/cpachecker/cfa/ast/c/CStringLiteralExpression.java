// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Verify.verify;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;

public final class CStringLiteralExpression extends AStringLiteralExpression
    implements CLiteralExpression {

  private static final long serialVersionUID = 2656216584704518185L;

  public CStringLiteralExpression(FileLocation pFileLocation, String pValue) {
    super(pFileLocation, computeType(pValue, pFileLocation), pValue);
  }

  /** Returns the type as <code>char[]</code> including the correct length. */
  @Override
  public CArrayType getExpressionType() {
    return (CArrayType) super.getExpressionType();
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

  /**
   * Use either {@link #toASTString()} (if you want the syntactic representation including quotes)
   * or {@link #getContentWithNullTerminator()} (if you want the actual content) or {@link
   * #getContentWithoutNullTerminator()} (if you want the actual content but without the including
   * null terminator).
   */
  @Override
  @Deprecated
  public String getValue() {
    return super.getValue();
  }

  /**
   * Returns the content of this string literal including the null terminator. This is the actual
   * semantics of a string literal according to the C standard.
   */
  public String getContentWithNullTerminator() {
    return getContentWithoutNullTerminator() + "\0";
  }

  /**
   * Returns the content of this string literal, but removes the null terminator first. Typically
   * one would use {@link #getContentWithNullTerminator()} instead, use this only in special cases.
   */
  public String getContentWithoutNullTerminator() {
    String literal = getValue();
    verify(literal.charAt(0) == '"');
    verify(literal.charAt(literal.length() - 1) == '"');
    return literal.substring(1, literal.length() - 1);
  }

  /** Returns the size in bytes of the represented string. */
  public int getSize() {
    // without quotes but plus null terminator
    int value = getValue().length() - 2 + 1;
    assert value == getContentWithNullTerminator().length();
    return value;
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
    // The string literal is is always NULL terminated, but a string in an array might be not.
    // We handle this below.
    final String s = getContentWithoutNullTerminator();
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

  private static CArrayType computeType(String astString, FileLocation pFileLocation) {
    CExpression length =
        new CIntegerLiteralExpression(
            pFileLocation, CNumericTypes.INT, BigInteger.valueOf(astString.length() - 2 + 1));
    return new CArrayType(false, false, CTypes.withConst(CNumericTypes.CHAR), length);
  }
}
