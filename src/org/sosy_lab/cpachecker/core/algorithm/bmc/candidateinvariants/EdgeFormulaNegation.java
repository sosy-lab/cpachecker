// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class EdgeFormulaNegation extends SingleLocationFormulaInvariant
    implements ExpressionTreeCandidateInvariant {

  private final AssumeEdge edge;

  public EdgeFormulaNegation(CFANode pLocation, AssumeEdge pEdge) {
    super(pLocation);
    Preconditions.checkNotNull(pEdge);
    edge = pEdge;
  }

  @Override
  public BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext)
      throws CPATransferException, InterruptedException {
    PathFormula clearContext = pPFMGR.makeEmptyPathFormulaWithContextFrom(pContext);
    PathFormula invariantPathFormula = pPFMGR.makeAnd(clearContext, edge);
    return pFMGR
        .getBooleanFormulaManager()
        .not(pFMGR.uninstantiate(invariantPathFormula.getFormula()));
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof EdgeFormulaNegation) {
      EdgeFormulaNegation other = (EdgeFormulaNegation) pO;
      return getLocation().equals(other.getLocation())
          && edge.getTruthAssumption() == other.edge.getTruthAssumption()
          && edge.getExpression().equals(other.edge.getExpression());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLocation(), edge.getTruthAssumption(), edge.getExpression());
  }

  @Override
  public String toString() {
    return asExpressionTree().toString();
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    if (appliesTo(edge.getPredecessor())) {
      Iterable<AbstractState> infeasibleStates =
          ImmutableList.copyOf(AbstractStates.filterLocation(pReachedSet, edge.getSuccessor()));
      pReachedSet.removeAll(infeasibleStates);
      for (ARGState s : from(infeasibleStates).filter(ARGState.class)) {
        s.removeFromARG();
      }
    }
  }

  @Override
  public ExpressionTree<Object> asExpressionTree() {
    return LeafExpression.of(edge.getExpression(), !edge.getTruthAssumption());
  }
}
