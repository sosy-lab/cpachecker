// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import com.google.common.annotations.VisibleForTesting;
import java.io.Serial;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;

public final class CStringLiteralExpression extends AStringLiteralExpression
    implements CLiteralExpression {

  @Serial private static final long serialVersionUID = 2656216584704518185L;

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
    return decodeContent(getValue());
  }

  /** Returns the size in bytes of the represented string. */
  public int getSize() {
    return getContentWithNullTerminator().length();
  }

  private static String decodeContent(String literal) {
    verify(literal.charAt(0) == '"');
    verify(literal.charAt(literal.length() - 1) == '"');
    return decodeEscapeSequences(literal.substring(1, literal.length() - 1));
  }

  /**
   * Replaces the escape sequences (C11 § 6.4.4.4) in the given body of a string literal by the
   * characters they denote. Characters from escape sequences are bytes (universal character names
   * are encoded in UTF-8).
   *
   * @throws IllegalArgumentException if an escape sequence is invalid
   */
  @VisibleForTesting
  static String decodeEscapeSequences(String s) {
    if (s.indexOf('\\') < 0) {
      return s;
    }
    StringBuilder result = new StringBuilder(s.length());
    int i = 0;
    while (i < s.length()) {
      char c = s.charAt(i++);
      if (c != '\\') {
        result.append(c);
        continue;
      }
      checkArgument(i < s.length(), "Incomplete escape sequence in string literal \"%s\"", s);
      c = s.charAt(i++);
      switch (c) {
        case 'a' -> result.append((char) 7);
        case 'b' -> result.append('\b');
        case 'e', 'E' -> result.append((char) 27); // GNU extension
        case 'f' -> result.append('\f');
        case 'n' -> result.append('\n');
        case 'r' -> result.append('\r');
        case 't' -> result.append('\t');
        case 'v' -> result.append((char) 11);
        case '0', '1', '2', '3', '4', '5', '6', '7' -> {
          int start = i - 1;
          while (i < s.length() && i - start < 3 && s.charAt(i) >= '0' && s.charAt(i) <= '7') {
            i++;
          }
          int value = Integer.parseInt(s.substring(start, i), 8);
          checkArgument(value <= 0xFF, "Octal escape sequence out of range in \"%s\"", s);
          result.append((char) value);
        }
        case 'x' -> {
          int start = i;
          while (i < s.length() && Character.digit(s.charAt(i), 16) >= 0) {
            i++;
          }
          checkArgument(i > start, "Hex escape sequence without digits in \"%s\"", s);
          BigInteger value = new BigInteger(s.substring(start, i), 16);
          checkArgument(
              value.compareTo(BigInteger.valueOf(0xFF)) <= 0,
              "Hex escape sequence out of range in \"%s\"",
              s);
          result.append((char) value.intValue());
        }
        case 'u', 'U' -> {
          int digits = c == 'u' ? 4 : 8;
          checkArgument(
              i + digits <= s.length(), "Incomplete universal character name in \"%s\"", s);
          int codePoint = Integer.parseUnsignedInt(s.substring(i, i + digits), 16);
          checkArgument(
              Character.isValidCodePoint(codePoint), "Invalid universal character in \"%s\"", s);
          i += digits;
          for (byte b : Character.toString(codePoint).getBytes(StandardCharsets.UTF_8)) {
            result.append((char) (b & 0xFF));
          }
        }
        // \', \", \?, \\, and unknown escape sequences (for which GCC keeps the character)
        default -> result.append(c);
      }
    }
    return result.toString();
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

    return obj instanceof CStringLiteralExpression && super.equals(obj);
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
    // The string literal is always NULL terminated, but a string in an array might be not.
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
    // literals with an encoding prefix like L"..." are not supported, keep their raw length
    int size =
        astString.startsWith("\"")
            ? decodeContent(astString).length() + 1
            : astString.length() - 2 + 1;
    CExpression length =
        new CIntegerLiteralExpression(pFileLocation, CNumericTypes.INT, BigInteger.valueOf(size));
    return new CArrayType(CTypeQualifiers.NONE, CNumericTypes.CONST_CHAR, length);
  }
}
