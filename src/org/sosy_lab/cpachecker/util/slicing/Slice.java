// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class Slice {

  private final CFA cfa;
  private final ImmutableSet<CFAEdge> relevantEdges;
  private final ImmutableCollection<CFAEdge> criteria;

  // A def is relevant for an edge if its memory location is contained in the value collection for
  // this specific edge. Otherwise, the def is irrelevant.
  private final ImmutableMultimap<CFAEdge, MemoryLocation> relevantEdgeDefs;

  Slice(
      final CFA pCfa,
      final Collection<CFAEdge> pRelevantEdges,
      Collection<CFAEdge> pCriteria,
      Multimap<CFAEdge, MemoryLocation> pRelevantEdgeDefs) {
    cfa = pCfa;
    relevantEdges = ImmutableSet.copyOf(pRelevantEdges);
    criteria = ImmutableList.copyOf(pCriteria);
    relevantEdgeDefs = ImmutableListMultimap.copyOf(pRelevantEdgeDefs);
  }

  public ImmutableSet<CFAEdge> getRelevantEdges() {
    return relevantEdges;
  }

  public ImmutableCollection<CFAEdge> getUsedCriteria() {
    return criteria;
  }

  public CFA getOriginalCfa() {
    return cfa;
  }

  public boolean isRelevantDef(CFAEdge pEdge, MemoryLocation pMemoryLocation) {

    if (relevantEdgeDefs.containsKey(pEdge)) {
      return relevantEdgeDefs.get(pEdge).contains(pMemoryLocation);
    } else {
      return false;
    }
  }
}
