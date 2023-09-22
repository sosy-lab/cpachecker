// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class ASTElement {
  private final FileLocation location;
  private ImmutableSet<CFAEdge> allEdges;
  private Set<CFAEdge> edges = null;

  public ASTElement(FileLocation pLocation, ImmutableSet<CFAEdge> pAllEdges) {
    location = pLocation;
    allEdges = pAllEdges;
  }

  public FileLocation location() {
    return location;
  }

  /**
   * @return the set of CFA edges belonging to this ASTElement.
   */
  public Set<CFAEdge> edges() {
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
}
