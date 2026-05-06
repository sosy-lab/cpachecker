// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 CPAchecker contributors
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class EdgeFormula extends SingleLocationFormulaInvariant
    implements ExpressionTreeCandidateInvariant {

  private final AssumeEdge edge;

  public EdgeFormula(CFANode pLocation, AssumeEdge pEdge) {
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
    return pFMGR.uninstantiate(invariantPathFormula.getFormula());
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    // Keep non-termination candidates observational only.
  }

  @Override
  public ExpressionTree<Object> asExpressionTree() {
    return LeafExpression.of(edge.getExpression(), edge.getTruthAssumption());
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof EdgeFormula other
        && getLocation().equals(other.getLocation())
        && edge.getTruthAssumption() == other.edge.getTruthAssumption()
        && edge.getExpression().equals(other.edge.getExpression());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLocation(), edge.getTruthAssumption(), edge.getExpression());
  }

  @Override
  public String toString() {
    return asExpressionTree().toString();
  }
}
