/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.testtargets;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class TestTargetProvider {

  private static TestTargetProvider instance;

  private CFA cfa;
  private ImmutableSet<CFAEdge> initialTestTargets;
  private Set<CFAEdge> testTargets;
  private Set<CFAEdge> coveredTestTargets;

  private TestTargetProvider() {
    cfa = null;
    initialTestTargets = null;
    testTargets = null;
    coveredTestTargets = null;
  }

  public static TestTargetProvider getInstance() {
    if (instance == null) {
      instance = new TestTargetProvider();
    }
    return instance;
  }

  public void initializeOnceWithAssumeEdges(CFA pCfa) {
    if (pCfa != cfa) {
      cfa = pCfa;
      testTargets = extractAssumeEdges(pCfa);
      initialTestTargets = ImmutableSet.copyOf(testTargets);
      coveredTestTargets = new HashSet<>();
    }
  }

  private Set<CFAEdge> extractAssumeEdges(CFA pCfa) {
    Set<CFAEdge> edges = new HashSet<>();
    for (CFANode node : pCfa.getAllNodes()) {
      edges.addAll(CFAUtils.allLeavingEdges(node).filter(AssumeEdge.class).toSet());
    }
    return edges;
  }

  public ImmutableSet<CFAEdge> getInitialTestTargets() {
    return initialTestTargets;
  }

  public Set<CFAEdge> getTestTargets() {
    return testTargets;
  }

  public Set<CFAEdge> getCoveredTestTargets() {
    return coveredTestTargets;
  }
}
