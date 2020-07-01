/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
