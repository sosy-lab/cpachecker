// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslFunctionContract extends AAcslAnnotation {

  @Nullable private final ImmutableList<AcslRequiresClause> requiresClauses;
  @Nullable private final AcslTerminatesClause terminatesClause;
  @Nullable private final AcslDecreasesClause decreasesClause;
  @Nullable private final ImmutableList<AAcslSimpleClause> simpleClauses;

  AcslFunctionContract(
      FileLocation pFileLocation,
      @Nullable ImmutableList<AcslRequiresClause> pRequiresClauses,
      @Nullable AcslTerminatesClause pTerminatesClause1,
      @Nullable AcslDecreasesClause pDecreasesClause,
      @Nullable ImmutableList<AAcslSimpleClause> pSimpleClauses) {
    super(pFileLocation);
    requiresClauses = pRequiresClauses;
    terminatesClause = pTerminatesClause1;
    decreasesClause = pDecreasesClause;
    simpleClauses = pSimpleClauses;
  }

  @Override
  public boolean equals(Object pO) {
    return false;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  String toAstString() {
    return "";
  }

  public @Nullable ImmutableList<AcslRequiresClause> getRequiresClauses() {
    return requiresClauses;
  }

  public @Nullable AcslTerminatesClause getTerminatesClause() {
    return terminatesClause;
  }

  public @Nullable AcslDecreasesClause getDecreasesClause() {
    return decreasesClause;
  }

  public @Nullable ImmutableList<AAcslSimpleClause> getSimpleClauses() {
    return simpleClauses;
  }
}
