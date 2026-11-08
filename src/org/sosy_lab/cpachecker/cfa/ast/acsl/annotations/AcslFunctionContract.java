// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
 *
 * <p>Note, that because of the order in the grammar rule for funciton contracts the requires
 * clauses of the contract neeed to be listed before assigns and ensures clauses:
 *
 * <p>functionContract : requiresClause* simpleClause* namedBehavior*;
 */
public final class AcslFunctionContract extends AAcslAnnotation {

  private final ImmutableSet<AcslRequires> requiresClauses;
  private final ImmutableSet<AcslEnsures> ensuresClauses;
  private final ImmutableSet<AcslAssigns> assignsClauses;
  private final ImmutableList<AcslBehavior> behaviors;
  private final ImmutableList<AcslCompletenessClause> completenessClauses;

  public AcslFunctionContract(
      FileLocation pFileLocation,
      ImmutableSet<AcslRequires> pRequiresClauses,
      ImmutableSet<AcslEnsures> pEnsuresClauses,
      ImmutableSet<AcslAssigns> pAssignsClauses) {
    this(
        pFileLocation,
        pRequiresClauses,
        pEnsuresClauses,
        pAssignsClauses,
        ImmutableList.of(),
        ImmutableList.of());
  }

  public AcslFunctionContract(
      FileLocation pFileLocation,
      ImmutableSet<AcslRequires> pRequiresClauses,
      ImmutableSet<AcslEnsures> pEnsuresClauses,
      ImmutableSet<AcslAssigns> pAssignsClauses,
      ImmutableList<AcslBehavior> pBehaviors,
      ImmutableList<AcslCompletenessClause> pCompletenessClauses) {
    super(pFileLocation);
    requiresClauses = checkNotNull(pRequiresClauses);
    ensuresClauses = checkNotNull(pEnsuresClauses);
    assignsClauses = checkNotNull(pAssignsClauses);
    behaviors = checkNotNull(pBehaviors);
    completenessClauses = checkNotNull(pCompletenessClauses);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof AcslFunctionContract other
        && assignsClauses.equals(other.assignsClauses)
        && ensuresClauses.equals(other.ensuresClauses)
        && requiresClauses.equals(other.requiresClauses)
        && behaviors.equals(other.behaviors)
        && completenessClauses.equals(other.completenessClauses);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    int prime = 31;
    hash = prime * hash + ensuresClauses.hashCode();
    hash = prime * hash + assignsClauses.hashCode();
    hash = prime * hash + requiresClauses.hashCode();
    hash = prime * hash + behaviors.hashCode();
    hash = prime * hash + completenessClauses.hashCode();
    return hash;
  }

  @Override
  public String toAstString() {
    StringBuilder astString = new StringBuilder();

    for (AcslRequires r : requiresClauses) {
      astString.append(r.toAstString()).append(System.lineSeparator());
    }

    for (AcslEnsures e : ensuresClauses) {
      astString.append(e.toAstString()).append(System.lineSeparator());
    }

    for (AcslAssigns a : assignsClauses) {
      astString.append(a.toAstString()).append(System.lineSeparator());
    }

    for (AcslBehavior b : behaviors) {
      astString.append(b.toAstString()).append(System.lineSeparator());
    }

    for (AcslCompletenessClause c : completenessClauses) {
      astString.append(c.toAstString()).append(System.lineSeparator());
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

  public ImmutableList<AcslBehavior> getBehaviors() {
    return behaviors;
  }

  public ImmutableList<AcslCompletenessClause> getCompletenessClauses() {
    return completenessClauses;
  }
}
