// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class SeqStringUtil {

  /** Matches both Windows (\r\n) and Unix-like (\n) newline conventions. */
  private static final Splitter newlineSplitter = Splitter.onPattern("\\r?\\n");

  /** Returns the number of lines, i.e. the amount of \n + 1 in pString. */
  public static int countLines(String pString) {
    if (isNullOrEmpty(pString)) {
      return 0;
    }
    return newlineSplitter.splitToList(pString).size();
  }

  public static ImmutableList<String> splitOnNewline(String pString) {
    return ImmutableList.copyOf(newlineSplitter.split(pString));
  }

  // AST Nodes =====================================================================================

  /** {@link CType#toString()} yields a trailing white space, this function strips it. */
  public static String getTypeName(CType pType) {
    return pType.toString().strip();
  }

  /**
   * If {@link CVariableDeclaration#toASTString()} yields {@code int x = 42;} then this method
   * yields {@code int x;}.
   */
  public static String getVariableDeclarationASTStringWithoutInitializer(
      CVariableDeclaration pVariableDeclaration, AAstNodeRepresentation pAAstNodeRepresentation) {

    return buildStorageClassNameAndTypeASTString(pVariableDeclaration, pAAstNodeRepresentation)
        + ";";
  }

  /**
   * If {@link CVariableDeclaration#toASTString()} yields {@code extern int x = 42;} then this
   * method yields {@code x = 42;}. Note that the initializer does not have to be present.
   */
  public static String getVariableDeclarationASTStringWithoutStorageClassAndType(
      CVariableDeclaration pVariableDeclaration, AAstNodeRepresentation pAAstNodeRepresentation) {

    return buildNameASTString(pVariableDeclaration, pAAstNodeRepresentation)
        + buildInitializerASTString(pVariableDeclaration, pAAstNodeRepresentation)
        + ";";
  }

  private static String buildStorageClassNameAndTypeASTString(
      CVariableDeclaration pVariableDeclaration, AAstNodeRepresentation pAAstNodeRepresentation) {

    return pVariableDeclaration.getCStorageClass().toASTString()
        + pVariableDeclaration
            .getType()
            .toASTString(buildNameASTString(pVariableDeclaration, pAAstNodeRepresentation));
  }

  private static String buildNameASTString(
      CVariableDeclaration pVariableDeclaration, AAstNodeRepresentation pAAstNodeRepresentation) {

    return switch (pAAstNodeRepresentation) {
      case DEFAULT -> pVariableDeclaration.getName();
      case QUALIFIED -> pVariableDeclaration.getQualifiedName().replace("::", "__");
      case ORIGINAL_NAMES -> pVariableDeclaration.getOrigName();
    };
  }

  private static String buildInitializerASTString(
      CVariableDeclaration pVariableDeclaration, AAstNodeRepresentation pAAstNodeRepresentation) {

    if (pVariableDeclaration.getInitializer() != null) {
      return " = " + pVariableDeclaration.getInitializer().toASTString(pAAstNodeRepresentation);
    }
    return "";
  }

  // Hexadecimal Format ============================================================================

  public static String hexFormat(int pLength, BigInteger pBigInteger) {
    return String.format("%0" + pLength + "x", pBigInteger);
  }
}
