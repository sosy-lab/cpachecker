// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqFunctionCallExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqFunctionDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;

/**
 * Taken from <a
 * href="https://sv-comp.sosy-lab.org/2025/rules.php">https://sv-comp.sosy-lab.org/2025/rules.php</a>
 */
public enum VerifierNondetFunctionType {
  INT("__VERIFIER_nondet_int"),
  UINT("__VERIFIER_nondet_uint");

  private final String name;

  VerifierNondetFunctionType(String pName) {
    name = pName;
  }

  public @NonNull String getName() {
    return name;
  }

  // Data Helpers ==================================================================================
  // cannot link these attributes to the enum directly, because they are not marked with @Immutable

  public @NonNull CType getReturnType() {
    return switch (this) {
      case INT -> CNumericTypes.INT;
      case UINT -> CNumericTypes.UNSIGNED_INT;
    };
  }

  public @NonNull CIdExpression getNameExpression() {
    return switch (this) {
      case INT -> SeqIdExpressions.VERIFIER_NONDET_INT;
      case UINT -> SeqIdExpressions.VERIFIER_NONDET_UINT;
    };
  }

  public @NonNull CFunctionDeclaration getFunctionDeclaration() {
    return switch (this) {
      case INT -> SeqFunctionDeclarations.VERIFIER_NONDET_INT;
      case UINT -> SeqFunctionDeclarations.VERIFIER_NONDET_UINT;
    };
  }

  public @NonNull CFunctionCallExpression getFunctionCallExpression() {
    return switch (this) {
      case INT -> SeqFunctionCallExpressions.VERIFIER_NONDET_INT;
      case UINT -> SeqFunctionCallExpressions.VERIFIER_NONDET_UINT;
    };
  }

  // Helpers =======================================================================================

  public static Optional<CFunctionCallExpression> tryBuildFunctionCallExpressionByType(
      CType pType) {

    for (VerifierNondetFunctionType nondetType : VerifierNondetFunctionType.values()) {
      if (nondetType.getReturnType().equals(pType)) {
        return Optional.of(nondetType.getFunctionCallExpression());
      }
    }
    return Optional.empty();
  }

  /**
   * Returns {@code next_thread = __VERIFIER_nondet_{u}int} with {@code uint} for unsigned, {@code
   * int} for signed.
   */
  public static CFunctionCallAssignmentStatement buildNondetIntegerAssignment(
      MPOROptions pOptions, CIdExpression pIdExpression) {

    return new CFunctionCallAssignmentStatement(
        FileLocation.DUMMY,
        pIdExpression,
        pOptions.nondeterminismSigned()
            ? INT.getFunctionCallExpression()
            : UINT.getFunctionCallExpression());
  }
}
