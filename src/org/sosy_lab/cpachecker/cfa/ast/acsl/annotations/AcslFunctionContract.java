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
 * This class is a representation of Acsl function contract ANSI/ISO C Specification Language
 * Version 1.23 §2.3.5 Default contracts, multiple contracts
 *
 * <p>A C function can be defined only once but declared several times. It is allowed to annotate
 * each of these declarations with contracts. Those contracts are seen as a single contract with the
 * union of the requires clauses and behaviors.
 *
 * <p>A function may have no contract at all, or a contract with missing clauses. Missing requires
 * and ensures clauses default to \true. Missing exits clauses default to \false. If no assigns
 * clause is given, it remains unspecified
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
