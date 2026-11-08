// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * A named behavior of a function or statement contract. Its requires and ensures clauses only need
 * to hold if its assumes clauses hold in the pre-state.
 */
public final class AcslBehavior extends AAcslAnnotation {

  private final String name;
  private final ImmutableSet<AcslAssumes> assumesClauses;
  private final ImmutableSet<AcslRequires> requiresClauses;
  private final ImmutableSet<AcslEnsures> ensuresClauses;
  private final ImmutableSet<AcslAssigns> assignsClauses;

  public AcslBehavior(
      FileLocation pFileLocation,
      String pName,
      ImmutableSet<AcslAssumes> pAssumesClauses,
      ImmutableSet<AcslRequires> pRequiresClauses,
      ImmutableSet<AcslEnsures> pEnsuresClauses,
      ImmutableSet<AcslAssigns> pAssignsClauses) {
    super(pFileLocation);
    name = checkNotNull(pName);
    assumesClauses = checkNotNull(pAssumesClauses);
    requiresClauses = checkNotNull(pRequiresClauses);
    ensuresClauses = checkNotNull(pEnsuresClauses);
    assignsClauses = checkNotNull(pAssignsClauses);
  }

  public String getName() {
    return name;
  }

  public ImmutableSet<AcslAssumes> getAssumesClauses() {
    return assumesClauses;
  }

  public ImmutableSet<AcslRequires> getRequiresClauses() {
    return requiresClauses;
  }

  public ImmutableSet<AcslEnsures> getEnsuresClauses() {
    return ensuresClauses;
  }

  public ImmutableSet<AcslAssigns> getAssignsClauses() {
    return assignsClauses;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof AcslBehavior other
        && name.equals(other.name)
        && assumesClauses.equals(other.assumesClauses)
        && requiresClauses.equals(other.requiresClauses)
        && ensuresClauses.equals(other.ensuresClauses)
        && assignsClauses.equals(other.assignsClauses);
  }

  @Override
  public int hashCode() {
    int hash = name.hashCode();
    hash = 31 * hash + assumesClauses.hashCode();
    hash = 31 * hash + requiresClauses.hashCode();
    hash = 31 * hash + ensuresClauses.hashCode();
    return 31 * hash + assignsClauses.hashCode();
  }

  @Override
  public String toAstString() {
    return "behavior "
        + name
        + ":"
        + System.lineSeparator()
        + Joiner.on(System.lineSeparator())
            .join(
                FluentIterable.<AAcslAnnotation>concat(
                        assumesClauses, requiresClauses, ensuresClauses, assignsClauses)
                    .transform(AAcslAnnotation::toAstString));
  }
}
