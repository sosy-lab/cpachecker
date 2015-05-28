/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Verify;
import com.google.common.collect.Sets;

public class BMCAlgorithmForInvariantGeneration extends AbstractBMCAlgorithm {

  private InvariantSupplier locationInvariantsProvider = InvariantSupplier.TrivialInvariantSupplier.INSTANCE;

  public BMCAlgorithmForInvariantGeneration(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCPA,
                      Configuration pConfig, LogManager pLogger,
                      ReachedSetFactory pReachedSetFactory,
                      ShutdownNotifier pShutdownNotifier, CFA pCFA,
                      BMCStatistics pBMCStatistics)
                      throws InvalidConfigurationException, CPAException {
    super(pAlgorithm, pCPA, pConfig, pLogger, pReachedSetFactory, pShutdownNotifier, pCFA,
        pBMCStatistics,
        true /* invariant generator */ );
    Verify.verify(checkIfInductionIsPossible(pCFA, pLogger));
  }

  public InvariantSupplier getCurrentInvariants() {
    return locationInvariantsProvider;
  }

  public boolean isProgramSafe() {
    return invariantGenerator.isProgramSafe();
  }

  @Override
  protected CandidateGenerator getCandidateInvariants(CFA pCFA,
      Collection<CFANode> pTargetLocations) {
    final Set<CandidateInvariant> candidates = Sets.newLinkedHashSet();

    for (AssumeEdge assumeEdge : getRelevantAssumeEdges(pTargetLocations)) {
      candidates.add(new EdgeFormulaNegation(pCFA.getLoopStructure().get().getAllLoopHeads(), assumeEdge));
    }

    return new StaticCandidateProvider(candidates);
  }

  /**
   * Gets the relevant assume edges.
   *
   * @param pTargetLocations the predetermined target locations.
   *
   * @return the relevant assume edges.
   */
  private Set<AssumeEdge> getRelevantAssumeEdges(Collection<CFANode> pTargetLocations) {
    final Set<AssumeEdge> assumeEdges = Sets.newLinkedHashSet();
    Set<CFANode> visited = Sets.newHashSet(pTargetLocations);
    Queue<CFANode> waitlist = new ArrayDeque<>(pTargetLocations);
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      for (CFAEdge enteringEdge : CFAUtils.enteringEdges(current)) {
        CFANode predecessor = enteringEdge.getPredecessor();
        if (enteringEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
          assumeEdges.add((AssumeEdge)enteringEdge);
        } else if (visited.add(predecessor)) {
          waitlist.add(predecessor);
        }
      }
    }
    return assumeEdges;
  }

  @Override
  protected KInductionProver createInductionProver() {
    final KInductionProver prover = super.createInductionProver();

    if (prover != null) {
      locationInvariantsProvider = new InvariantSupplier() {

        @Override
        public BooleanFormula getInvariantFor(CFANode location, FormulaManagerView fmgr, PathFormulaManager pfmgr) {
          try {
            return prover.getCurrentLocationInvariants(location, fmgr, pfmgr);
          } catch (InterruptedException | CPAException e) {
            return fmgr.getBooleanFormulaManager().makeBoolean(true);
          }
        }
      };
    }

    return prover;
  }
}
