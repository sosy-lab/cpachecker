// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

/**
 * Rewrites C atomic type specifiers {@code _Atomic ( type-name )} into the equivalent {@code
 * _Atomic}-qualifier form, which denotes the same type (cf. C23 § 6.7.3.5 and footnote 147, #1667).
 * Eclipse CDT cannot parse the specifier form, but the qualifier form is handled via the {@link
 * EclipseCdtWrapper#ATOMIC_ATTRIBUTE} macro (and, for pointers, via the recovery in {@code
 * ASTTypeConverter#findAtomicPointerOperators}, #1670).
 *
 * <ul>
 *   <li>a plain type name: {@code _Atomic(T)} becomes {@code _Atomic T} by blanking the
 *       parentheses;
 *   <li>a trailing-pointer type name: {@code _Atomic(T*)} becomes {@code T* _Atomic}, so the {@code
 *       _Atomic} applies to the pointer (the distinction is the point of the parentheses: {@code
 *       _Atomic(int) *} is a pointer to an atomic int, while {@code _Atomic(int*)} is an atomic
 *       pointer to int);
 *   <li>a parenthesized-pointer type name (function pointer or pointer to array): {@code
 *       _Atomic(int (*)(void)) f} becomes {@code typedef int (*NAME)(void); _Atomic NAME f} via a
 *       fresh typedef, because the qualifier form cannot otherwise be written without moving the
 *       declarator.
 * </ul>
 *
 * <p>The first two rewrites keep the total length so that all source offsets are preserved; the
 * typedef rewrite does not (but preserves the line structure). Array and function type names are
 * forbidden (C23 § 6.7.3.5 (3)) and left unchanged, as are occurrences inside strings, character
 * constants, or comments.
 *
 * <p>This is a lightweight, purpose-built scanner over the source text, not a full C parser.
 */
final class AtomicTypeSpecifierRewriter {

  private static final String ATOMIC_KEYWORD = "_Atomic";

  private AtomicTypeSpecifierRewriter() {}

  private enum AtomicTypeName {
    /** A plain type name such as {@code int}, {@code unsigned long}, or {@code struct s}. */
    PLAIN,
    /** A pointer type name whose {@code *}(s) are trailing, e.g. {@code int*} or {@code int **}. */
    TRAILING_POINTER,
    /**
     * A pointer type name with a parenthesized declarator, e.g. {@code int (*)(void)} (pointer to
     * function) or {@code int (*)[3]} (pointer to array).
     */
    PARENTHESIZED_POINTER,
    /** An unsupported type name: an array, function, or otherwise too complex type name. */
    UNSUPPORTED
  }

  /**
   * Rewrite all atomic type specifiers in the given code as described in the class documentation.
   */
  static String rewrite(final String pCode) {
    if (!pCode.contains(ATOMIC_KEYWORD)) {
      return pCode;
    }
    char[] code = pCode.toCharArray();
    StringBuilder out = new StringBuilder(code.length);
    int typedefCounter = 0;
    int i = 0;
    while (i < code.length) {
      char c = code[i];
      if (c == '"' || c == '\'') {
        int end = skipLiteral(code, i);
        out.append(code, i, end - i);
        i = end;
      } else if (c == '/' && i + 1 < code.length && code[i + 1] == '/') {
        int end = skipToLineEnd(code, i);
        out.append(code, i, end - i);
        i = end;
      } else if (c == '/' && i + 1 < code.length && code[i + 1] == '*') {
        int end = skipBlockComment(code, i);
        out.append(code, i, end - i);
        i = end;
      } else if (startsAtomicKeyword(code, i)) {
        int open = skipWhitespace(code, i + ATOMIC_KEYWORD.length());
        int close = open < code.length && code[open] == '(' ? matchingParenthesis(code, open) : -1;
        if (close < 0) {
          out.append(ATOMIC_KEYWORD);
          i += ATOMIC_KEYWORD.length();
          continue;
        }
        int start = open + 1;
        switch (classifyTypeName(code, start, close)) {
          case PLAIN ->
              // "_Atomic(T)" -> "_Atomic T " (blank the parentheses)
              out.append(code, i, open - i)
                  .append(' ')
                  .append(code, start, close - start)
                  .append(' ');
          case TRAILING_POINTER -> {
            // "_Atomic(T*)" -> "T* _Atomic" (append the qualifier after the pointer)
            int padding = (close + 1 - i) - (close - start) - ATOMIC_KEYWORD.length();
            out.append(code, start, close - start);
            appendRepeated(out, ' ', padding);
            out.append(ATOMIC_KEYWORD);
          }
          case PARENTHESIZED_POINTER -> {
            // introduce a fresh typedef for the type name and make that typedef atomic
            String name = "__CPAchecker_atomic_type_" + typedefCounter++;
            out.append("typedef ")
                .append(typedefDeclarator(code, start, close, name))
                .append("; ")
                .append(ATOMIC_KEYWORD)
                .append(' ')
                .append(name);
          }
          case UNSUPPORTED -> out.append(code, i, close + 1 - i); // leave unchanged
        }
        i = close + 1;
      } else {
        out.append(c);
        i++;
      }
    }
    return out.toString();
  }

  private static void appendRepeated(final StringBuilder pOut, final char pChar, final int pCount) {
    for (int k = 0; k < pCount; k++) {
      pOut.append(pChar);
    }
  }

  private static AtomicTypeName classifyTypeName(
      final char[] pCode, final int pStart, final int pEnd) {
    if (pStart >= pEnd) {
      return AtomicTypeName.UNSUPPORTED;
    }
    int firstParenthesis = -1;
    int firstBracket = -1;
    boolean hasPointer = false;
    for (int i = pStart; i < pEnd; i++) {
      char c = pCode[i];
      if (c == '(' && firstParenthesis < 0) {
        firstParenthesis = i;
      } else if (c == '[' && firstBracket < 0) {
        firstBracket = i;
      } else if (c == '*') {
        hasPointer = true;
      }
    }
    if (firstParenthesis >= 0 && (firstBracket < 0 || firstParenthesis < firstBracket)) {
      // A parenthesized declarator such as "(*)" in "int (*)(void)"; supported only if these
      // parentheses contain nothing but "*"(s).
      int declaratorEnd = matchingParenthesis(pCode, firstParenthesis);
      if (declaratorEnd < 0 || declaratorEnd >= pEnd) {
        return AtomicTypeName.UNSUPPORTED;
      }
      boolean starsOnly = false;
      for (int i = firstParenthesis + 1; i < declaratorEnd; i++) {
        char c = pCode[i];
        if (c == '*') {
          starsOnly = true;
        } else if (!Character.isWhitespace(c)) {
          return AtomicTypeName.UNSUPPORTED;
        }
      }
      return starsOnly ? AtomicTypeName.PARENTHESIZED_POINTER : AtomicTypeName.UNSUPPORTED;
    }
    if (firstBracket >= 0) {
      return AtomicTypeName.UNSUPPORTED; // array type name
    }
    if (!hasPointer) {
      return AtomicTypeName.PLAIN;
    }
    // A trailing-pointer type name has its "*"(s) at the end; anything else (e.g. the qualified
    // pointer "int * const") is a constraint violation that we do not rewrite.
    int last = pEnd - 1;
    while (last >= pStart && Character.isWhitespace(pCode[last])) {
      last--;
    }
    return pCode[last] == '*' ? AtomicTypeName.TRAILING_POINTER : AtomicTypeName.UNSUPPORTED;
  }

  /**
   * Build the declarator for a typedef of the parenthesized-pointer type name {@code [pStart,
   * pEnd)}, giving it the name {@code pName}. For example {@code int (*)(void)} with name {@code N}
   * yields {@code int (*N)(void)}.
   */
  private static String typedefDeclarator(
      final char[] pCode, final int pStart, final int pEnd, final String pName) {
    int parenthesis = pStart;
    while (pCode[parenthesis] != '(') {
      parenthesis++;
    }
    int declaratorEnd = matchingParenthesis(pCode, parenthesis);
    return new String(pCode, pStart, declaratorEnd - pStart)
        + pName
        + new String(pCode, declaratorEnd, pEnd - declaratorEnd);
  }

  private static boolean startsAtomicKeyword(final char[] pCode, final int pIndex) {
    if (pIndex + ATOMIC_KEYWORD.length() > pCode.length) {
      return false;
    }
    for (int k = 0; k < ATOMIC_KEYWORD.length(); k++) {
      if (pCode[pIndex + k] != ATOMIC_KEYWORD.charAt(k)) {
        return false;
      }
    }
    if (pIndex > 0 && isIdentifierPart(pCode[pIndex - 1])) {
      return false;
    }
    int after = pIndex + ATOMIC_KEYWORD.length();
    return after >= pCode.length || !isIdentifierPart(pCode[after]);
  }

  private static boolean isIdentifierPart(final char pChar) {
    return Character.isLetterOrDigit(pChar) || pChar == '_';
  }

  private static int skipWhitespace(final char[] pCode, final int pIndex) {
    int i = pIndex;
    while (i < pCode.length && Character.isWhitespace(pCode[i])) {
      i++;
    }
    return i;
  }

  private static int skipLiteral(final char[] pCode, final int pIndex) {
    char quote = pCode[pIndex];
    int i = pIndex + 1;
    while (i < pCode.length) {
      if (pCode[i] == '\\') {
        i += 2;
      } else if (pCode[i] == quote) {
        return i + 1;
      } else {
        i++;
      }
    }
    return i;
  }

  private static int skipToLineEnd(final char[] pCode, final int pIndex) {
    int i = pIndex + 2;
    while (i < pCode.length && pCode[i] != '\n') {
      i++;
    }
    return i;
  }

  private static int skipBlockComment(final char[] pCode, final int pIndex) {
    int i = pIndex + 2;
    while (i + 1 < pCode.length && !(pCode[i] == '*' && pCode[i + 1] == '/')) {
      i++;
    }
    return Math.min(i + 2, pCode.length);
  }

  private static int matchingParenthesis(final char[] pCode, final int pOpen) {
    int depth = 0;
    for (int i = pOpen; i < pCode.length; i++) {
      if (pCode[i] == '(') {
        depth++;
      } else if (pCode[i] == ')') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    return -1;
  }
}
