// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.concurrent.LazyInit;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public final class ASTElement {
  private final FileLocation location;
  private ImmutableSet<CFAEdge> allEdges;
  @LazyInit private ImmutableSet<CFAEdge> edges = null;

  public ASTElement(FileLocation pLocation, ImmutableSet<CFAEdge> pAllEdges) {
    location = pLocation;
    allEdges = pAllEdges;
  }

  public FileLocation location() {
    return location;
  }

  /** Returns the set of CFA edges belonging to this ASTElement. */
  public ImmutableSet<CFAEdge> edges() {
    // we calculate this set lazily upon the first invocation
    if (edges == null) {
      edges =
          allEdges.stream()
              .filter(x -> FileLocationUtils.entails(location, x.getFileLocation()))
              .collect(ImmutableSet.toImmutableSet());
      allEdges = null; // free reference
    }
    return edges;
  }

  @Override
  public int hashCode() {
    return location.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    return other instanceof ASTElement a && location.equals(a.location);
  }

  @Override
  public String toString() {
    return "ASTElement at [ " + location + " ]";
  }
}
