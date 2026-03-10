// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
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
      ImmutableSet<AcslRequires> pRequiresClauses) {
    super(pFileLocation);
    Preconditions.checkNotNull(pEnsuresClauses);
    Preconditions.checkNotNull(pAssignsClauses);
    Preconditions.checkNotNull(pRequiresClauses);
    ensuresClauses = pEnsuresClauses;
    assignsClauses = pAssignsClauses;
    requiresClauses = pRequiresClauses;
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
      astString.append(e.toAstString()).append(System.lineSeparator());
    }

    for (AcslAssigns a : assignsClauses) {
      astString.append(a.toAstString()).append(System.lineSeparator());
    }
    for (AcslRequires r : requiresClauses) {
      astString.append(r.toAstString()).append(System.lineSeparator());
    }

    return astString.toString();
  }

  public ImmutableSet<AcslEnsures> getEnsuresClauses() {
    return ensuresClauses;
  }

  public ImmutableSet<AcslRequires> getRequiresClauses() {
    return requiresClauses;
  }

  public ImmutableSet<AcslAssigns> getAssignsClauses() {
    return assignsClauses;
  }
}
