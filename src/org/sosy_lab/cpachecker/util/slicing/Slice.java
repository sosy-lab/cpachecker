// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Represents a program slice. */
public interface Slice {

  /**
   * Returns the {@link CFA} from which this program slice was created.
   *
   * @return the {@link CFA} from which this program slice was created
   */
  CFA getOriginalCfa();

  /**
   * Returns the slicing criteria that were used to create this program slice.
   *
   * @return the slicing criteria that were used to create this program slice
   */
  ImmutableCollection<CFAEdge> getSlicingCriteria();

  /**
   * Returns a set of all edges contained in this program slice.
   *
   * <p>The edges contained in a program slice are called relevant edges. Some edges are only
   * partially relevant to a program slice (e.g., not all function parameters are relevant). These
   * partially relevant edges are, as well as the fully relevant edges, contained in the returned
   * set.
   *
   * @return a set of all edges contained in this program slice
   */
  ImmutableSet<CFAEdge> getRelevantEdges();

  /**
   * Returns a set of all declarations in this program slice.
   *
   * @return a set of all declarations in this program slice
   */
  ImmutableSet<ASimpleDeclaration> getRelevantDeclarations();

  /**
   * Returns whether the write to the specified memory location at the specified edge is relevant to
   * this program slice.
   *
   * <p>If the specified memory location is not actually written to at the specified edge, either
   * {@code true} or {@code false} is returned.
   *
   * @param pEdge the {@link CFAEdge} at which the memory location is written to
   * @param pMemoryLocation the {@link MemoryLocation} that is written to
   * @return whether the write to the specified memory location at the specified edge is relevant to
   *     this program slice
   * @throws IllegalArgumentException if {@code pEdge} is not a relevant edge to this program slice
   * @throws NullPointerException if any parameter is {@code null}
   */
  boolean isRelevantDef(CFAEdge pEdge, MemoryLocation pMemoryLocation);

  /**
   * Returns whether the read of the specified memory location at the specified edge is relevant to
   * this program slice.
   *
   * <p>If the specified memory location is not actually read at the specified edge, either {@code
   * true} or {@code false} is returned.
   *
   * @param pEdge the {@link CFAEdge} at which the memory location is read
   * @param pMemoryLocation the {@link MemoryLocation} that is read
   * @return whether the read of the specified memory location at the specified edge is relevant to
   *     this program slice
   * @throws IllegalArgumentException if {@code pEdge} is not a relevant edge to this program slice
   * @throws NullPointerException if any parameter is {@code null}
   */
  boolean isRelevantUse(CFAEdge pEdge, MemoryLocation pMemoryLocation);
}
