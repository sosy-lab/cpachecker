// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.ucageneration;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class UCAARGStateEdge {
  private final ARGState source;
  private final Optional<ARGState> target;
  private final CFAEdge edge;
  private final Optional<AbstractionFormula> assumption;

  public UCAARGStateEdge(
      ARGState pSource, ARGState pTarget, CFAEdge pEdge, Optional<AbstractionFormula> pAssumption) {
    this.source = pSource;
    this.target = Optional.of(pTarget);
    this.edge = pEdge;
    this.assumption = pAssumption;
  }

  public UCAARGStateEdge(ARGState pSource, CFAEdge pEdge) {
    this.source = pSource;
    this.target = Optional.empty();
    this.edge = pEdge;
    this.assumption = Optional.empty();
  }

  public String getSourceName() {
    return UCAGenerator.getName(source);
  }

  public String getTargetName() {
    return this.target.isPresent()
        ? UCAGenerator.getName(target.orElseThrow())
        : UCAGenerator.NAME_OF_TEMP_STATE;
  }

  public ARGState getSource() {
    return source;
  }

  public String getStringOfAssumption(FormulaManagerView pFMgr) throws IOException {
    if (this.assumption.isPresent()) {
      StringBuilder sb = new StringBuilder();
      sb.append("ASSUME {");
      AssumptionCollectorAlgorithm.escape(
          AssumptionCollectorAlgorithm.parseAssumptionToString(
              this.assumption.orElseThrow().asFormula(), pFMgr, this.edge.getSuccessor()),
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
    if (!(pO instanceof UCAARGStateEdge)) {
      return false;
    }
    UCAARGStateEdge ucaEdge = (UCAARGStateEdge) pO;
    return Objects.equals(source, ucaEdge.source)
        && Objects.equals(target, ucaEdge.target)
        && Objects.equals(edge, ucaEdge.edge);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, target, edge);
  }

  @Override
  public String toString() {
    return "UCAEdge{"
        + getSourceName()
        + "-- "
        + UCAGenerator.getEdgeString(edge)
        + " ->"
        + getTargetName()
        + '}';
  }

  public CFAEdge getEdge() {
    return edge;
  }
}
