// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

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

  public AcslLoopAnnotation(
      FileLocation pFileLocation, ImmutableSet<AcslLoopInvariant> pLoopInvariants) {
    super(pFileLocation);
    loopInvariants = pLoopInvariants;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof AcslLoopAnnotation other
        && Objects.equals(loopInvariants, other.loopInvariants);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    int prime = 31;
    hash = prime * hash * Objects.hashCode(loopInvariants);
    return hash;
  }

  public int numOfLoopInvariants() {
    return loopInvariants.size();
  }

  @Override
  public String toAstString() {
    StringBuilder astString = new StringBuilder();
    if (loopInvariants != null) {
      for (AcslLoopInvariant l : loopInvariants) {
        astString.append(l.toAstString()).append("\n");
      }
    }
    return astString.toString();
  }
}
