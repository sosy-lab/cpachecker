// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/** This class is a representation of Acsl function_contract */
public final class AcslFunctionContract extends AAcslAnnotation {

  private final @Nullable ImmutableSet<AcslEnsures> ensuresClauses;
  private final @Nullable ImmutableSet<AcslAssigns> assignsClauses;
  private final @Nullable ImmutableSet<AcslRequires> requiresClauses;

  public AcslFunctionContract(
      FileLocation pFileLocation,
      @Nullable ImmutableSet<AcslEnsures> pEnsuresClauses,
      @Nullable ImmutableSet<AcslAssigns> pAssignsClauses,
      @Nullable ImmutableSet<AcslRequires> pRequiresClauses1) {
    super(pFileLocation);
    ensuresClauses = pEnsuresClauses;
    assignsClauses = pAssignsClauses;
    requiresClauses = pRequiresClauses1;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof AcslFunctionContract other
        && Objects.equals(ensuresClauses, other.ensuresClauses)
        && Objects.equals(requiresClauses, other.requiresClauses);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    int prime = 31;
    hash = prime * hash * Objects.hashCode(ensuresClauses);
    hash = prime * hash * Objects.hashCode(requiresClauses);
    return hash;
  }

  @Override
  public String toAstString() {
    StringBuilder astString = new StringBuilder();
    if (ensuresClauses != null) {
      for (AcslEnsures e : ensuresClauses) {
        astString.append(e.toAstString()).append("\n");
      }
    }
    if (assignsClauses != null) {
      for (AcslAssigns a : assignsClauses) {
        astString.append(a.toAstString()).append("\n");
      }
    }
    if (requiresClauses != null) {
      for (AcslRequires r : requiresClauses) {
        astString.append(r.toAstString()).append("\n");
      }
    }

    return astString.toString();
  }

  public @Nullable ImmutableSet<AcslEnsures> getEnsuresClauses() {
    return ensuresClauses;
  }

  public @Nullable ImmutableSet<AcslRequires> getRequiresClauses() {
    return requiresClauses;
  }
}
