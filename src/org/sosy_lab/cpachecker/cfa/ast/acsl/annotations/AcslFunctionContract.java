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

  @Nullable private final ImmutableList<AcslEnsures> ensuresClauses;
  @Nullable private final ImmutableList<AcslRequires> requiresClauses;

  AcslFunctionContract(
      FileLocation pFileLocation,
      ImmutableList<AcslEnsures> pEnsuresClauses,
      ImmutableList<AcslRequires> pRequiresClauses1) {
    super(pFileLocation);
    ensuresClauses = pEnsuresClauses;
    requiresClauses = pRequiresClauses1;
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

  public ImmutableList<AcslEnsures> getEnsuresClauses() {
    return ensuresClauses;
  }

  public ImmutableList<AcslRequires> getRequiresClauses() {
    return requiresClauses;
  }
}
