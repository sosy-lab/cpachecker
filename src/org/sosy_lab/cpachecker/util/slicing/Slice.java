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
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class Slice {

  private final CFA cfa;
  private final ImmutableSet<CFAEdge> relevantEdges;
  private final ImmutableCollection<CFAEdge> criteria;

  Slice(final CFA pCfa, final Collection<CFAEdge> pRelevantEdges, Collection<CFAEdge> pCriteria) {
    cfa = pCfa;
    relevantEdges = ImmutableSet.copyOf(pRelevantEdges);
    criteria = ImmutableList.copyOf(pCriteria);
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
}
