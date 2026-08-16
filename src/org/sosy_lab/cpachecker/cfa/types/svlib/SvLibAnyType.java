// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.svlib;

import java.io.Serial;
import org.sosy_lab.java_smt.api.FormulaType;

/**
 * This class represents an internal SV-LIB type which can be used whenever any type is acceptable.
 * For example, it can be used as a placeholder type for internal functions that can operate on any
 * type, like the return type of the __VERIFIER_nondet function.
 *
 * <p>It can also be used in situations where an annotation is not resolved yet, for example at a
 * `annotate-tag` command.
 *
 * <p>TODO: Actually only relevant for parsing, consider moving to AST package. But currently we
 * leave it here to have a sealed type hierarchy for SvLibType.
 */
public final class SvLibAnyType implements SvLibSmtLibType {
  @Serial private static final long serialVersionUID = -2771387240943229018L;

  @Override
  public String toASTString() {
    return "any";
  }

  @Override
  public String toASTString(String declarator) {
    return "any";
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibAnyType;
  }

  @Override
  public FormulaType<?> toFormulaType() {
    throw new UnsupportedOperationException("SvLibAnyType cannot be converted to a FormulaType");
  }
}
