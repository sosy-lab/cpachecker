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
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessFactory;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class GIAARGStateEdge {
  protected final ARGState source;
  protected final Optional<ARGState> target;
  private final CFAEdge edge;
  private final Optional<AbstractionFormula> assumption;
  private final Optional<String> source_mulitEdgeIndex;
  private final Optional<String> target_mulitEdgeIndex;
  //  private final Optional<String> additionalAssumption;

  //  public GIAARGStateEdge(
  //      ARGState pSource,
  //      Optional<String> pSource_mulitEdgeIndex,
  //      ARGState pTarget,
  //      Optional<String> pTarget_mulitEdgeIndex,
  //      CFAEdge pEdge,
  //      Optional<AbstractionFormula> pAssumption,
  //      Optional<String> pAdditionalAssumption) {
  //    this.source = pSource;
  //    this.target = Optional.of(pTarget);
  //    this.edge = pEdge;
  //    this.assumption = pAssumption;
  //    this.source_mulitEdgeIndex = pSource_mulitEdgeIndex;
  //    this.target_mulitEdgeIndex = pTarget_mulitEdgeIndex;
  //    this.additionalAssumption = pAdditionalAssumption;
  //  }

  //  public GIAARGStateEdge(
  //      ARGState pSource,
  //      Optional<String> pSource_mulitEdgeIndex,
  //      ARGState pTarget,
  //      Optional<String> pTarget_mulitEdgeIndex,
  //      CFAEdge pEdge,
  //      Optional<AbstractionFormula> pAssumption) {
  //    this.source = pSource;
  //    this.target = Optional.of(pTarget);
  //    this.edge = pEdge;
  //    this.assumption = pAssumption;
  //    this.source_mulitEdgeIndex = pSource_mulitEdgeIndex;
  //    this.target_mulitEdgeIndex = pTarget_mulitEdgeIndex;
  //    this.additionalAssumption = Optional.empty();
  //  }

  //  public GIAARGStateEdge(
  //      ARGState pSource,
  //      Optional<String> pSource_mulitEdgeIndex,
  //      CFAEdge pEdge,
  //      Optional<AbstractionFormula> pAssumption) {
  //    this.source = pSource;
  //    this.target = Optional.empty();
  //    this.edge = pEdge;
  //    this.assumption = pAssumption;
  //    this.source_mulitEdgeIndex = pSource_mulitEdgeIndex;
  //    this.target_mulitEdgeIndex = Optional.empty();
  //    this.additionalAssumption = Optional.empty();
  //  }

  public GIAARGStateEdge(
      ARGState pSource,
      ARGState pTarget,
      CFAEdge pEdge,
      Optional<AbstractionFormula> pAssumption,
      Optional<String> pAdditionalAssumption) {
    this.source = pSource;
    this.target = Optional.of(pTarget);
    this.edge = pEdge;
    this.assumption = pAssumption;
    this.source_mulitEdgeIndex = Optional.empty();
    this.target_mulitEdgeIndex = Optional.empty();
    //    this.additionalAssumption = pAdditionalAssumption;
  }

  public GIAARGStateEdge(
      ARGState pSource, ARGState pTarget, CFAEdge pEdge, Optional<AbstractionFormula> pAssumption) {
    this.source = pSource;
    this.target = Optional.of(pTarget);
    this.edge = pEdge;
    this.assumption = pAssumption;
    this.source_mulitEdgeIndex = Optional.empty();
    this.target_mulitEdgeIndex = Optional.empty();
    //    this.additionalAssumption = Optional.empty();
  }

  public GIAARGStateEdge(ARGState pSource, CFAEdge pEdge) {
    this.source = pSource;
    this.target = Optional.empty();
    this.edge = pEdge;
    this.assumption = Optional.empty();
    this.source_mulitEdgeIndex = Optional.empty();
    this.target_mulitEdgeIndex = Optional.empty();
    //    this.additionalAssumption = Optional.empty();
  }

  public GIAARGStateEdge(ARGState pSource, ARGState pTarget, CFAEdge pEdge) {
    this.source = pSource;
    this.target = Optional.ofNullable(pTarget);
    this.edge = pEdge;
    this.assumption = Optional.empty();
    this.source_mulitEdgeIndex = Optional.empty();
    this.target_mulitEdgeIndex = Optional.empty();
    //    this.additionalAssumption = Optional.empty();
  }

  public String getSourceName() {
    return GIAGenerator.getNameOrError(source);
  }

  public String getTargetName() {
    return this.target.isPresent()
        ? GIAGenerator.getName(target.orElseThrow())
        : GIAGenerator.NAME_OF_TEMP_STATE;
  }

  public String getTargetName(
      Set<ARGState> pTargetStates, Set<ARGState> pNonTargetStates, Set<ARGState> pUnknownStates) {
    if (this.target.isPresent()) {
      final ARGState targetState = target.orElseThrow();
      if (pTargetStates.contains(targetState)) return GIAGenerator.NAME_OF_ERROR_STATE;
      if (pNonTargetStates.contains(targetState)) return GIAGenerator.NAME_OF_FINAL_STATE;
      if (pUnknownStates.contains(targetState)) return GIAGenerator.NAME_OF_UNKNOWN_STATE;
      return GIAGenerator.getName(targetState);
    }
    return GIAGenerator.NAME_OF_TEMP_STATE;
  }

  public ARGState getSource() {
    return source;
  }

  public Optional<ARGState> getTarget() {
    return target;
  }

  public String getStringOfAssumption(Optional<ARGState> pState)
      throws IOException, InterruptedException {
    if (this.assumption.isPresent() && pState.isPresent()) {
      StringBuilder sb = new StringBuilder();
      sb.append("ASSUME {");
      AssumptionCollectorAlgorithm.escape(
          WitnessFactory.getAssumptionAsCode(
              this.assumption.orElseThrow().asExpressionTree(AbstractStates.extractLocation(pState.orElseThrow())), Optional.empty()),
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
    String sourceSuffix =
        this.source_mulitEdgeIndex.isPresent() ? "_" + source_mulitEdgeIndex.orElseThrow() : "";
    String targetSuffix =
        this.target_mulitEdgeIndex.isPresent() ? "_" + target_mulitEdgeIndex.orElseThrow() : "";
    return "GIAEdge{"
        + getSourceName()
        + sourceSuffix
        + "-- "
        + GIAGenerator.getEdgeString(edge)
        + " ->"
        + getTargetName()
        + targetSuffix
        + '}';
  }

  public CFAEdge getEdge() {
    return edge;
  }
}
