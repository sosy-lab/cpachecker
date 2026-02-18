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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslMemoryLocationSet;

/** This class is a representanion of Acsl modified memory locations. */
public final class AcslAssigns extends AAcslAnnotation {
  private final ImmutableSet<AcslMemoryLocationSet> locations;

  private AcslAssigns(FileLocation pFileLocation, ImmutableSet<AcslMemoryLocationSet> pLocations) {
    super(pFileLocation);
    locations = pLocations;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof AcslAssigns other && locations.equals(other.locations);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    int prime = 31;
    hash = prime * hash * Objects.hashCode(locations);
    return hash;
  }

  @Override
  public String toAstString() {
    StringBuilder astString = new StringBuilder("assigns ");
    for (AcslMemoryLocationSet l : locations) {
      astString.append(l.toASTString()).append(", ");
    }
    return astString.toString();
  }
}
