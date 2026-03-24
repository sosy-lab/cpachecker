// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
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
    Preconditions.checkNotNull(pLoopInvariants);
    Preconditions.checkNotNull(pLoopAssigns);
    loopInvariants = pLoopInvariants;
    loopAssigns = pLoopAssigns;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof AcslLoopAnnotation other
        && Objects.equals(loopInvariants, other.loopInvariants)
        && Objects.equals(loopAssigns,other.loopAssigns);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    int prime = 31;
    hash = prime * hash * Objects.hashCode(loopInvariants);
    hash = prime * hash * Objects.hashCode(loopAssigns);
    return hash;
  }

  public ImmutableSet<AcslLoopInvariant> getLoopInvariants() {
    return loopInvariants;
  }
  public ImmutableSet<AcslAssigns> getLoopAssigns(){return loopAssigns;}

  @Override
  public String toAstString() {
    StringBuilder astString = new StringBuilder();
      for (AcslLoopInvariant l : loopInvariants) {
        astString.append(l.toAstString()).append(System.lineSeparator());
      }
      for (AcslAssigns a : loopAssigns){
        astString.append("loop ").append(a.toAstString()).append(System.lineSeparator());
      }
    return astString.toString();
  }
}
