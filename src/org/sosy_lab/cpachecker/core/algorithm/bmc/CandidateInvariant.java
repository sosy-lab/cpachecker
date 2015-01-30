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

import java.util.Collections;

import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

class CandidateInvariant {

  private final CFAEdge edge;

  public CandidateInvariant(CFAEdge pEdge) throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(pEdge);
    this.edge = pEdge;
  }

  public Optional<AssumeEdge> getAssumeEdge() {
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
    if (pO instanceof CandidateInvariant) {
      CandidateInvariant other = (CandidateInvariant) pO;
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

}