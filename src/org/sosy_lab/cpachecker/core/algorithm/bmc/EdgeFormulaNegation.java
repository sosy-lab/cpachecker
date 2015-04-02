/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.FluentIterable.from;

import java.util.Collections;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.invariants.CPAInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;

public class EdgeFormulaNegation implements CandidateInvariant {

  private final CFAEdge edge;

  private final Set<CFANode> locations;

  public EdgeFormulaNegation(Set<CFANode> pLocations, CFAEdge pEdge) {
    Preconditions.checkNotNull(pLocations);
    Preconditions.checkNotNull(pEdge);
    this.locations = pLocations;
    this.edge = pEdge;
  }

  private Optional<AssumeEdge> getAssumeEdge() {
    if (edge instanceof AssumeEdge) {
      AssumeEdge assumeEdge = (AssumeEdge) edge;
      CFANode predecessor = assumeEdge.getPredecessor();
      AssumeEdge otherEdge = CFAUtils.leavingEdges(predecessor).filter(not(equalTo(edge))).filter(AssumeEdge.class).iterator().next();
      return Optional.of(otherEdge);
    }
    return Optional.absent();
  }

  public BooleanFormula getCandidate(FormulaManagerView pFMGR, PathFormulaManager pPFMGR) throws CPATransferException, InterruptedException {
    PathFormula invariantPathFormula = pPFMGR.makeFormulaForPath(Collections.<CFAEdge>singletonList(edge));
    return pFMGR.getBooleanFormulaManager().not(pFMGR.uninstantiate(invariantPathFormula.getFormula()));
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof EdgeFormulaNegation) {
      EdgeFormulaNegation other = (EdgeFormulaNegation) pO;
      return edge.equals(other.edge);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getAssumeEdge());
  }

  @Override
  public String toString() {
    Optional<AssumeEdge> assumeEdge = getAssumeEdge();
    if (assumeEdge.isPresent()) {
      return assumeEdge.get().toString();
    }
    return String.format("not (%s)", edge);
  }

  @Override
  public BooleanFormula getAssertion(ReachedSet pReachedSet, FormulaManagerView pFMGR, PathFormulaManager pPFMGR) throws CPATransferException, InterruptedException {
    Iterable<AbstractState> locationStates = AbstractStates.filterLocations(pReachedSet, locations);
    FluentIterable<BooleanFormula> assertions = FluentIterable.from(
        BMCHelper.assertAt(locationStates, getCandidate(pFMGR, pPFMGR), pFMGR));
    return pFMGR.getBooleanFormulaManager().and(assertions.toList());
  }

  @Override
  public boolean violationIndicatesError() {
    return false;
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    if (locations.contains(edge.getPredecessor())) {
      Iterable<AbstractState> infeasibleStates = from(AbstractStates.filterLocation(pReachedSet, edge.getSuccessor())).toList();
      pReachedSet.removeAll(infeasibleStates);
      for (ARGState s : from(infeasibleStates).filter(ARGState.class)) {
        s.removeFromARG();
      }
    }
  }

  @Override
  public void attemptInjection(InvariantGenerator pInvariantGenerator) throws UnrecognizedCodeException {
    if (pInvariantGenerator instanceof CPAInvariantGenerator) {
      CPAInvariantGenerator invGen = (CPAInvariantGenerator) pInvariantGenerator;
      InvariantsCPA invariantsCPA = CPAs.retrieveCPA(invGen.getCPAs(), InvariantsCPA.class);
      if (invariantsCPA != null) {
        Optional<AssumeEdge> assumption = getAssumeEdge();
        if (assumption.isPresent()) {
          for (CFANode location : locations) {
            invariantsCPA.injectInvariant(location, assumption.get());
          }
        }
      }
    }
  }


}