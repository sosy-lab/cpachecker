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

/**
 * This class is a representation of Acsl function_contract
 *
 * <p>A loop annotation can have an arbirtary number of loop invariants and loop-assigns. A loop
 * invariant is a loop-clause. (§2.4.2 Acsl standard v 1.23)
 */
public final class AcslFunctionContract extends AAcslAnnotation {

  private final ImmutableSet<AcslEnsures> ensuresClauses;
  private final ImmutableSet<AcslAssigns> assignsClauses;
  private final ImmutableSet<AcslRequires> requiresClauses;

  public AcslFunctionContract(
      FileLocation pFileLocation,
      ImmutableSet<AcslEnsures> pEnsuresClauses,
      ImmutableSet<AcslAssigns> pAssignsClauses,
      ImmutableSet<AcslRequires> pRequiresClauses1) {
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
    for (AcslEnsures e : ensuresClauses) {
      astString.append(e.toAstString()).append("\n");
    }

    for (AcslAssigns a : assignsClauses) {
      astString.append(a.toAstString()).append("\n");
    }
    for (AcslRequires r : requiresClauses) {
      astString.append(r.toAstString()).append("\n");
    }

    return astString.toString();
  }

  public int numOfEnsures() {
    return ensuresClauses.size();
  }

  public int numOfRequires() {
    return requiresClauses.size();
  }

  public int numOfAssigns() {
    return assignsClauses.size();
  }

  public int numOfAnnotations() {
    return ensuresClauses.size() + requiresClauses.size() + assignsClauses.size();
  }

  public @Nullable ImmutableSet<AcslEnsures> getEnsuresClauses() {
    return ensuresClauses;
  }

  public @Nullable ImmutableSet<AcslRequires> getRequiresClauses() {
    return requiresClauses;
  }
}
