// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.giageneration;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessFactory;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.MatchOtherwise;
import org.sosy_lab.cpachecker.cpa.giacombiner.GIATransition;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;

public class GIAARGStateEdge<T extends AbstractState> {
  protected final T source;
  protected final Optional<T> target;
  private final CFAEdge edge;
  private final Optional<AbstractionFormula> assumption;
  private final Optional<String> source_mulitEdgeIndex;
  private final Optional<String> target_mulitEdgeIndex;
  private final Optional<GIATransition> giaTransition;
  private boolean edgesPresentAsCFAEdge = true;
  private  Optional<String> additionalAssumption = Optional.empty();

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
      T pSource,
      T pTarget,
      CFAEdge pEdge,
      Optional<AbstractionFormula> pAssumption,
      Optional<String> pAdditionalAssumption) {
    this.source = pSource;
    this.target = Optional.of(pTarget);
    this.edge = pEdge;
    this.assumption = pAssumption;
    this.source_mulitEdgeIndex = Optional.empty();
    this.target_mulitEdgeIndex = Optional.empty();
    this.giaTransition = Optional.empty();
    //TODO: Use this information
    this.additionalAssumption = pAdditionalAssumption;
  }

  public GIAARGStateEdge(
      T pSource, T pTarget, CFAEdge pEdge, Optional<AbstractionFormula> pAssumption) {
    this.source = pSource;
    this.target = Optional.of(pTarget);
    this.edge = pEdge;
    this.assumption = pAssumption;
    this.source_mulitEdgeIndex = Optional.empty();
    this.target_mulitEdgeIndex = Optional.empty();
    this.giaTransition = Optional.empty();
    //    this.additionalAssumption = Optional.empty();
  }

  public GIAARGStateEdge(T pSource, CFAEdge pEdge) {
    this.source = pSource;
    this.target = Optional.empty();
    this.edge = pEdge;
    this.assumption = Optional.empty();
    this.source_mulitEdgeIndex = Optional.empty();
    this.target_mulitEdgeIndex = Optional.empty();
    this.giaTransition = Optional.empty();
    //    this.additionalAssumption = Optional.empty();
  }

  public GIAARGStateEdge(T pSource, T pTarget, CFAEdge pEdge) {
    this.source = pSource;
    this.target = Optional.ofNullable(pTarget);
    this.edge = pEdge;
    this.assumption = Optional.empty();
    this.source_mulitEdgeIndex = Optional.empty();
    this.target_mulitEdgeIndex = Optional.empty();
    //    this.additionalAssumption = Optional.empty();
    this.giaTransition = Optional.empty();

  }

  public GIAARGStateEdge(T pState, Entry<GIATransition, T> pEdge) {
    this.source = pState;
    this.target = Optional.ofNullable(pEdge.getValue());
    this.edge = null;
    this.assumption = Optional.empty();
    this.source_mulitEdgeIndex = Optional.empty();
    this.target_mulitEdgeIndex = Optional.empty();
    this.giaTransition = Optional.ofNullable(pEdge.getKey());   edgesPresentAsCFAEdge = false;
  }

  public String getTargetName() {
    return this.target.isPresent()
        ? GIAGenerator.getName(target.orElseThrow())
        : GIAGenerator.NAME_OF_TEMP_STATE;
  }

  public String getTargetName(
      Set<T> pTargetStates, Set<T> pNonTargetStates, Set<T> pUnknownStates) {
    if (this.target.isPresent()) {
      final T targetState = target.orElseThrow();
      if (pTargetStates.contains(targetState)) return GIAGenerator.NAME_OF_ERROR_STATE;
      if (pNonTargetStates.contains(targetState)) return GIAGenerator.NAME_OF_FINAL_STATE;
      if (pUnknownStates.contains(targetState)) return GIAGenerator.NAME_OF_UNKNOWN_STATE;
      return GIAGenerator.getName(targetState);
    }
    return GIAGenerator.NAME_OF_TEMP_STATE;
  }

  public T getSource() {
    return source;
  }

  public Optional<T> getTarget() {
    return target;
  }

  public String getStringOfAssumption(Optional<T> pState) throws IOException, InterruptedException {
    if (this.assumption.isPresent() && pState.isPresent()) {
      StringBuilder sb = new StringBuilder();
      sb.append("ASSUME {");
      AssumptionCollectorAlgorithm.escape(
          WitnessFactory.getAssumptionAsCode(
              this.assumption
                  .orElseThrow()
                  .asExpressionTree(AbstractStates.extractLocation(pState.orElseThrow())),
              Optional.empty()),
          sb);
      sb.append("} ");
      return sb.toString();
    }
    return "";
  }

  public String getStringOfAssumption(Optional<T> pState, String pAdditionalAssumption) throws IOException, InterruptedException {
    if (this.assumption.isPresent() && pState.isPresent()) {
      StringBuilder sb = new StringBuilder();
      sb.append("ASSUME {");
      AssumptionCollectorAlgorithm.escape(
          WitnessFactory.getAssumptionAsCode(
              this.assumption
                  .orElseThrow()
                  .asExpressionTree(AbstractStates.extractLocation(pState.orElseThrow())),
              Optional.empty()),
          sb);
      sb.append(";" + pAdditionalAssumption);
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
    @SuppressWarnings("unchecked")
    GIAARGStateEdge<T> giaEdge = (GIAARGStateEdge<T>) pO;
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
        + GIAGenerator.getName(source)
        + sourceSuffix
        + "-- "
        + GIAGenerator.getEdgeString(edge)
        + " ->"
        + getTargetName()
        + targetSuffix
        + '}';
  }

  /**
   *
   * @return true if the edge added is an otherwise edge, false otherwise
   */
  public boolean generateTransition(
      Appendable sb, Set<T> pTargetStates, Set<T> pNonTargetStates, Set<T> pUnknownStates)
      throws IOException, InterruptedException {
    if (this.edgesPresentAsCFAEdge || this.giaTransition.isEmpty()) {
      sb.append("    MATCH ");

      sb.append(GIAGenerator.getEdgeString(this.getEdge()));
      sb.append(" -> ");
      if (additionalAssumption.isPresent()) {sb.append(getStringOfAssumption(getTarget(), additionalAssumption.orElseThrow()));
        }else{
        sb.append(getStringOfAssumption(getTarget()));}
      sb.append(
          String.format("GOTO %s", getTargetName(pTargetStates, pNonTargetStates, pUnknownStates)));
      sb.append(";\n");
      return false;
    } else {
      GIATransition transition = giaTransition.orElseThrow();
      sb.append("    ");
      sb.append(AssumptionCollectorAlgorithm.escapeSpacingChars(transition.getTrigger().toString()));
      sb.append(" -> ");
      if (!transition.getAssumptions().isEmpty()) {
        ExpressionTreeFactory<AExpression> fac = ExpressionTrees.newFactory();
        ExpressionTree<AExpression> assumptions = ExpressionTrees.getTrue();
        for (AExpression c : transition.getAssumptions()) {
          assumptions = fac.and(assumptions, fac.leaf(c));
        }
        sb.append("ASSUME {");
        AssumptionCollectorAlgorithm.escape(
            WitnessFactory.getAssumptionAsCode(ExpressionTrees.cast(assumptions), Optional.empty()),
            sb);
        if (additionalAssumption.isPresent()) {
          sb.append(";" + this.additionalAssumption.orElseThrow());}
        sb.append("} ");
      }
      sb.append(
          String.format("GOTO %s", getTargetName(pTargetStates, pNonTargetStates, pUnknownStates)));
      sb.append(";\n");
      return transition.getTrigger() instanceof MatchOtherwise;
    }
  }

  @Nullable
  public CFAEdge getEdge() {
    return edge;
  }

  public Optional<GIATransition> getGiaTransition() {
    return giaTransition;
  }
}
