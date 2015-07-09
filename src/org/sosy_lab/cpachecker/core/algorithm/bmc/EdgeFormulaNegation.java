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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.*;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.getOnlyElement;

import java.util.Collections;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Preconditions;

public class EdgeFormulaNegation extends LocationFormulaInvariant {

  private final AssumeEdge edge;

  private final Set<CFANode> locations;

  public EdgeFormulaNegation(Set<CFANode> pLocations, AssumeEdge pEdge) {
    super(pLocations);
    Preconditions.checkNotNull(pEdge);
    this.locations = checkNotNull(pLocations);
    this.edge = pEdge;
  }

  private AssumeEdge getNegatedAssumeEdge() {
    CFANode predecessor = edge.getPredecessor();
    return getOnlyElement(CFAUtils.leavingEdges(predecessor).filter(AssumeEdge.class).filter(not(equalTo(edge))));
  }

  @Override
  public BooleanFormula getFormula(FormulaManagerView pFMGR, PathFormulaManager pPFMGR) throws CPATransferException, InterruptedException {
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
    return edge.hashCode();
  }

  @Override
  public String toString() {
    return getNegatedAssumeEdge().toString();
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
    for (CFANode location : locations) {
      pInvariantGenerator.injectInvariant(location, getNegatedAssumeEdge());
    }
  }
}