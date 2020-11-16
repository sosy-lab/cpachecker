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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public interface Slice {

  CFA getOriginalCfa();

  ImmutableCollection<CFAEdge> getUsedCriteria();

  ImmutableSet<CFAEdge> getRelevantEdges();

  /**
   * Returns whether the definition of {@code pMemoryLocation} at {@code pEdge} is relevant for this
   * slice.
   *
   * <p>If the definition is relevant or the relevancy of the definition unknown, true is returned.
   * Otherwise, if this definition is not relevant, false is returned. It's possible that for every
   * possible combination of {@code pEdge} and {@code pMemoryLocation}, true is returned.
   *
   * @param pEdge the {@link CFAEdge} that defines {@code pMemoryLocation}.
   * @param pMemoryLocation the defined {@link MemoryLocation}.
   * @return true if the definition of {@code pMemoryLocation} at {@code pEdge} is relevant for the
   *     slice or if the relevancy is unknown; otherwise, false is returned.
   */
  boolean isRelevantDef(CFAEdge pEdge, MemoryLocation pMemoryLocation);
}
