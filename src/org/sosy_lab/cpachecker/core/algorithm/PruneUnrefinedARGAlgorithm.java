/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;


public class PruneUnrefinedARGAlgorithm implements Algorithm {

  private final Algorithm algorithm;
  @SuppressWarnings("unused")
  private final LogManager logger;
  private final ReachedSetFactory reachedSetFactory;
  private final CFA cfa;
  private final ConfigurableProgramAnalysis cpa;

  public PruneUnrefinedARGAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa, LogManager pLogger, ReachedSetFactory pReachedSetFactory, CFA pCfa) {
    algorithm = pAlgorithm;
    logger = pLogger;
    reachedSetFactory = pReachedSetFactory;
    cfa = pCfa;
    cpa = pCpa;
  }

  @Override
  public boolean run(ReachedSet reachedOrig) throws CPAException, InterruptedException {
    ReachedSet reached = reachedSetFactory.create();
    FunctionEntryNode mainFunction = cfa.getMainFunction();
    AbstractState initialElement = cpa.getInitialState(mainFunction);
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction);
    reached.add(initialElement, initialPrecision);

    // compute explicit version of ART
    boolean sound = algorithm.run(reachedOrig);

    if (!algorithm.reset())
      throw new CPAException("Reset in PruneUnrefinedARGAlgorithm did not work");

    // compute predicate version of ART
    sound = algorithm.run(reached);

    // compute all error locations that are reachable
    Set<CFANode> locations = new HashSet<CFANode>();
    for (AbstractState e : reached) {
      ARGState artEle = (ARGState) e;
      LocationState le = AbstractStates.extractStateByType(artEle, LocationState.class);
      if (artEle.isTarget()) {
        locations.add(le.getLocationNode());
      }
    }

    Deque<ARGState> leaves = new ArrayDeque<ARGState>();

    // remove all error nodes from reachedOrig that are not reachable in reached
    for (AbstractState e : reachedOrig) {
      ARGState artEle = (ARGState) e;
      LocationState le = AbstractStates.extractStateByType(artEle, LocationState.class);
      if (artEle.isTarget()) {
        System.out.print("Found target " + le.getLocationNode().getLineNumber());
        if (artEle.isCovered())
          System.out.print(" COV");
        if (!locations.contains(le.getLocationNode())) {
          leaves.add(artEle);
          for (ARGState e2 : artEle.getCoveredByThis()) {
            System.out.print(" (added " + AbstractStates.extractStateByType(e2, LocationState.class).getLocationNode().getLineNumber() + " )");
            leaves.add(e2);
          }
          System.out.println(" RM");
        } else {
          System.out.println(" IGN");
        }
      }
    }
    AbstractARGBasedRefiner.checkART(reachedOrig);
    while (!leaves.isEmpty()) {
      ARGState leaf = leaves.pop();

      Collection<ARGState> parents = new ArrayList<ARGState>();
      parents.addAll(leaf.getParents());

      reachedOrig.remove(leaf);
      leaf.removeFromARG();

      for (ARGState parent : parents) {
        if (parent.getChildren().size() == 0) {
          leaves.push(parent);
        }
      }
    }
    AbstractARGBasedRefiner.checkART(reachedOrig);

    return sound;
  }

  @Override
  public boolean reset() {
    return false;
  }
}
