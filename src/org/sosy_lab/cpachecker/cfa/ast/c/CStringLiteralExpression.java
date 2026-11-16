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

import java.io.Serial;
import java.math.BigInteger;
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

  /**
   * Creates a string literal from its representation in C source code, i.e. with the surrounding
   * quotes and with escape sequences (C11 6.4.4.4) for special characters, e.g. {@code "a\n"}.
   */
  public CStringLiteralExpression(FileLocation pFileLocation, String pValue) {
    super(pFileLocation, computeType(pValue, pFileLocation), pValue);
  }

  /**
   * Creates a string literal with the given content (without the null terminator), i.e. special
   * characters of the content are escaped in the representation in C source code.
   */
  public static CStringLiteralExpression ofContent(FileLocation pFileLocation, String pContent) {
    StringBuilder literal = new StringBuilder(pContent.length() + 2).append('"');
    for (int i = 0; i < pContent.length(); i++) {
      char c = pContent.charAt(i);
      switch (c) {
        case '\\' -> literal.append("\\\\");
        case '"' -> literal.append("\\\"");
        default -> {
          if (c < 0x20 || c >= 0x7F) {
            // Three octal digits, such that a following digit is not part of the escape sequence
            checkArgument(c <= 0xFF, "Character %s can not be stored in a char", (int) c);
            literal.append(String.format("\\%03o", (int) c));
          } else {
            literal.append(c);
          }
        }
      }
    }
    return new CStringLiteralExpression(pFileLocation, literal.append('"').toString());
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
    return getContent(getValue());
  }

  /** Returns the size in bytes of the represented string. */
  public int getSize() {
    return getContentWithNullTerminator().length();
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
    // The content plus the null terminator
    CExpression length =
        new CIntegerLiteralExpression(
            pFileLocation,
            CNumericTypes.INT,
            BigInteger.valueOf(getContent(astString).length() + 1));
    return new CArrayType(CTypeQualifiers.NONE, CNumericTypes.CONST_CHAR, length);
  }

  /**
   * Returns the characters that the given string literal in C source code denotes (without the null
   * terminator), i.e. without the surrounding quotes and with the escape sequences replaced by the
   * characters they denote.
   */
  private static String getContent(String pLiteral) {
    verify(pLiteral.charAt(0) == '"');
    verify(pLiteral.charAt(pLiteral.length() - 1) == '"');
    return decodeEscapeSequences(pLiteral.substring(1, pLiteral.length() - 1));
  }

  /**
   * Replaces the escape sequences (C11 6.4.4.4) in the given content of a string literal by the
   * characters they denote. Besides the escape sequences of C, the GNU extension {@code \e} for the
   * escape character is supported.
   */
  private static String decodeEscapeSequences(String pContent) {
    StringBuilder result = new StringBuilder(pContent.length());
    int i = 0;
    while (i < pContent.length()) {
      char c = pContent.charAt(i++);
      if (c != '\\') {
        result.append(c);
        continue;
      }
      checkArgument(i < pContent.length(), "Incomplete escape sequence in \"%s\"", pContent);
      char escaped = pContent.charAt(i++);
      if (escaped == 'x') {
        // Hexadecimal escape sequences consist of all following hexadecimal digits
        int start = i;
        while (i < pContent.length() && isHexDigit(pContent.charAt(i))) {
          i++;
        }
        result.append(parseEscapedCharacter(pContent, start, i, 16));
      } else if (escaped >= '0' && escaped <= '7') {
        // Octal escape sequences consist of up to 3 octal digits
        int start = i - 1;
        while (i < pContent.length()
            && i - start < 3
            && pContent.charAt(i) >= '0'
            && pContent.charAt(i) <= '7') {
          i++;
        }
        result.append(parseEscapedCharacter(pContent, start, i, 8));
      } else {
        result.append(
            switch (escaped) {
              case '\'', '"', '?', '\\' -> escaped;
              case 'a' -> (char) 7;
              case 'b' -> '\b';
              case 'e' -> (char) 27;
              case 'f' -> '\f';
              case 'n' -> '\n';
              case 'r' -> '\r';
              case 't' -> '\t';
              case 'v' -> (char) 11;
              default ->
                  throw new IllegalArgumentException(
                      "Unknown escape sequence \\" + escaped + " in \"" + pContent + "\"");
            });
      }
    }
    return result.toString();
  }

  private static boolean isHexDigit(char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  /** Parses the digits of a numeric escape sequence into the character it denotes. */
  private static char parseEscapedCharacter(String pContent, int pStart, int pEnd, int pRadix) {
    checkArgument(pStart < pEnd, "Escape sequence without digits in \"%s\"", pContent);
    BigInteger value = new BigInteger(pContent.substring(pStart, pEnd), pRadix);
    checkArgument(
        value.compareTo(BigInteger.valueOf(0xFF)) <= 0,
        "Escape sequence out of range in \"%s\"",
        pContent);
    return (char) value.intValueExact();
  }
}
