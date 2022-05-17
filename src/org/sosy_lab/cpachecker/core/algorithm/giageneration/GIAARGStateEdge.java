// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.giageneration;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class GIAARGStateEdge {
  protected final ARGState source;
  protected final Optional<ARGState> target;
  private final CFAEdge edge;
  private final Optional<AbstractionFormula> assumption;

  public GIAARGStateEdge(
      ARGState pSource, ARGState pTarget, CFAEdge pEdge, Optional<AbstractionFormula> pAssumption) {
    this.source = pSource;
    this.target = Optional.of(pTarget);
    this.edge = pEdge;
    this.assumption = pAssumption;
  }

  public GIAARGStateEdge(ARGState pSource, CFAEdge pEdge) {
    this.source = pSource;
    this.target = Optional.empty();
    this.edge = pEdge;
    this.assumption = Optional.empty();
  }

  public GIAARGStateEdge(ARGState pSource, ARGState pTarget, CFAEdge pEdge) {
    this.source = pSource;
    this.target = Optional.ofNullable(pTarget);
    this.edge = pEdge;
    this.assumption = Optional.empty();
  }

  public String getSourceName() {
    return GIAGenerator.getName(source);
  }

  public String getTargetName() {
    return this.target.isPresent()
        ? GIAGenerator.getName(target.orElseThrow())
        : GIAGenerator.NAME_OF_TEMP_STATE;
  }

  public ARGState getSource() {
    return source;
  }

  public Optional<ARGState> getTarget() {
    return target;
  }

  public String getStringOfAssumption(FormulaManagerView pFMgr)
      throws IOException, InterruptedException {
    if (this.assumption.isPresent()) {
      StringBuilder sb = new StringBuilder();
      sb.append("ASSUME {");
      AssumptionCollectorAlgorithm.escape(
          ExpressionTrees.fromFormula(
                  this.assumption.orElseThrow().asFormula(), pFMgr, this.edge.getSuccessor())
              .toString(),
          sb);
      sb.append("} ");
      return sb.toString();
    }
    return "";
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof GIAARGStateEdge)) {
      return false;
    }
    GIAARGStateEdge giaEdge = (GIAARGStateEdge) pO;
    return Objects.equals(source, giaEdge.source)
        && Objects.equals(target, giaEdge.target)
        && Objects.equals(edge, giaEdge.edge);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, target, edge);
  }

  @Override
  public String toString() {
    return "GIAEdge{"
        + getSourceName()
        + "-- "
        + GIAGenerator.getEdgeString(edge)
        + " ->"
        + getTargetName()
        + '}';
  }

  public CFAEdge getEdge() {
    return edge;
  }
}
