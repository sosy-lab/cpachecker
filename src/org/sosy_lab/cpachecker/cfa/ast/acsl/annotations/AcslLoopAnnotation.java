// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * This class is a representation of acsl loop annotations
 *
 * <p>A loop annotation can have an arbirtary number of loop invariants and loop-assigns. A loop
 * invariant is a loop-clause. (§2.4.2 Acsl standard v 1.23)
 */
public final class AcslLoopAnnotation extends AAcslAnnotation {

  private final ImmutableSet<AcslLoopInvariant> loopInvariants;
  private final ImmutableSet<AcslAssigns> loopAssigns;

  public AcslLoopAnnotation(
      FileLocation pFileLocation,
      ImmutableSet<AcslLoopInvariant> pLoopInvariants,
      ImmutableSet<AcslAssigns> pLoopAssigns) {
    super(pFileLocation);
    loopInvariants = checkNotNull(pLoopInvariants);
    loopAssigns = checkNotNull(pLoopAssigns);
    checkArgument(
        !loopInvariants.isEmpty() || !loopAssigns.isEmpty(), "A loop annotation has a clause");
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof AcslLoopAnnotation other
        && loopInvariants.equals(other.loopInvariants)
        && loopAssigns.equals(other.loopAssigns);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    int prime = 31;
    hash = prime * hash * loopInvariants.hashCode();
    hash = prime * hash * loopAssigns.hashCode();
    return hash;
  }

  public ImmutableSet<AcslLoopInvariant> getLoopInvariants() {
    return loopInvariants;
  }

  public ImmutableSet<AcslAssigns> getLoopAssigns() {
    return loopAssigns;
  }

  @Override
  public String toAstString() {
    return FluentIterable.concat(
            FluentIterable.from(loopInvariants).transform(AcslLoopInvariant::toAstString),
            FluentIterable.from(loopAssigns).transform(assigns -> "loop " + assigns.toAstString()))
        .join(Joiner.on(' '));
  }
}
