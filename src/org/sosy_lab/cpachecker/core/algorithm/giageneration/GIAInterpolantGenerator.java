//// This file is part of CPAchecker,
//// a tool for configurable software verification:
//// https://cpachecker.sosy-lab.org
////
//// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
////
//// SPDX-License-Identifier: Apache-2.0
//
// package org.sosy_lab.cpachecker.core.algorithm.giageneration;
//
// import com.google.common.collect.ImmutableList;
// import com.google.common.collect.ImmutableSet;
// import com.google.common.collect.Sets;
// import java.io.IOException;
// import java.util.ArrayDeque;
// import java.util.Comparator;
// import java.util.Deque;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.Map;
// import java.util.Objects;
// import java.util.Optional;
// import java.util.Set;
// import java.util.TreeSet;
// import org.checkerframework.checker.nullness.qual.Nullable;
// import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
// import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
// import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
// import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
// import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
// import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
// import org.sosy_lab.cpachecker.cpa.arg.ARGState;
// import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
// import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
// import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
// import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
// import org.sosy_lab.cpachecker.exceptions.CPAException;
// import org.sosy_lab.cpachecker.util.AbstractStates;
// import org.sosy_lab.cpachecker.util.CFAUtils;
// import org.sosy_lab.cpachecker.util.CPAs;
// import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
// import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
//
// public class GIAInterpolantGenerator {
//  private final ConfigurableProgramAnalysis cpa;
//
//  private final FormulaManagerView formulaManager;
//
//  public GIAInterpolantGenerator(
//      ConfigurableProgramAnalysis pCpa, FormulaManagerView pFormlaManger) {
//    this.cpa = pCpa;
//    this.formulaManager = pFormlaManger;
//  }
//
//  int produceGIA4Interpolant(Appendable output, UnmodifiableReachedSet reached)
//      throws IOException, CPAException {
//    final AbstractState firstState = reached.getFirstState();
//    if (!(firstState instanceof ARGState)) {
//      throw new CPAException("Cannot dump interpolant as automaton if ARGCPA is not used.");
//    }
//    @Nullable PredicateCPA predicateCPA = CPAs.retrieveCPA(this.cpa, PredicateCPA.class);
//    if (Objects.isNull(predicateCPA)) {
//      throw new CPAException("Cannot dump interpolant as automaton if no PredicateCPA is
// preset.");
//    }
//
//    // check, if the GIA that should be generated (e.g. for a violation witness)
//    // matches the reached set (meaning that the reached set contians at least
//    // location with a property violation according to the specification
//
//    boolean hasViolation =
//        reached.stream().anyMatch(s -> s instanceof ARGState && ((ARGState) s).isTarget());
//    if (hasViolation) {
//      throw new CPAException(
//          "Cannot transform the GIA, as the reached set has a property violation!");
//    }
//
//    final ARGState argRoot = (ARGState) reached.getFirstState();
//    Set<GIAARGStateEdge> relevantEdges = new HashSet<>();
//    // Get all states that have some invariants, because for them the invariant will be printed in
//    // the GIA
//    ImmutableSet<ARGState> statesWithInvariants =
//        reached.asCollection().stream()
//            .filter(
//                s -> {
//                  @Nullable PredicateAbstractState pState =
//                      AbstractStates.extractStateByType(s, PredicateAbstractState.class);
//                  if (pState == null) return false;
//                  if (!pState.isAbstractionState()) return false;
//                  // Remove all non-abstract states and abstract states with true abstraction
//                  // formula
//                  return !formulaManager
//                      .getBooleanFormulaManager()
//                      .isTrue(pState.getAbstractionFormula().asFormula());
//                })
//            .map(s -> AbstractStates.extractStateByType(s, ARGState.class))
//            .collect(ImmutableSet.toImmutableSet());
//
//    // scan reached set for all relevant states with are AbstractionsStates that have a non-true
//    // abstraction formula
//    Set<ARGState> relevantStates = new TreeSet<>();
//    for (AbstractState state : reached) {
//      ARGState e = (ARGState) state;
//      boolean isRelevant = statesWithInvariants.contains(e);
//
//      if (e.isCovered()) {
//        e = e.getCoveringState(); // replace with covering state
//        assert !e.isCovered();
//      }
//      if (relevantStates.contains(e)) {
//        continue;
//      }
//      if (isRelevant) {
//        Deque<ARGState> toAdd = new ArrayDeque<>();
//        toAdd.add(e);
//        while (!toAdd.isEmpty()) {
//          ARGState current = toAdd.pop();
//          assert !current.isCovered();
//
//          if (relevantStates.add(current)) {
//            // current was not yet contained in parentSet,
//            // so we need to handle its parents
//
//            toAdd.addAll(current.getParents());
//
//            // Create a new edge:
//            // Store the interpolant as assumption
//            Optional<AbstractionFormula> assumption =
//                statesWithInvariants.contains(current)
//                    ? Optional.ofNullable(
//                        AbstractStates.extractStateByType(current, PredicateAbstractState.class)
//                            .getAbstractionFormula())
//                    : Optional.empty();
//            for (ARGState parent : current.getParents()) {
//              // Find out which is the last edge to the ARG State
//              ARGPath path = ARGUtils.getOnePathFromTo((x) -> x.equals(parent), current);
//
//              final CFAEdge edgeToCurrent = path.getFullPath().get(path.getFullPath().size() - 1);
//              relevantEdges.add(new GIAARGStateEdge(parent, current, edgeToCurrent, assumption));
//              // Now, add all edges leaving the path
//              for (CFAEdge edge :
//                  path.getFullPath().stream()
//                      .filter(e1 -> e1 instanceof AssumeEdge)
//                      .collect(ImmutableList.toImmutableList())) {
//                for (CFAEdge notTakenEdge :
//                    CFAUtils.allLeavingEdges(edge.getPredecessor()).stream()
//                        .filter(e2 -> !e2.equals(edge))
//                        .collect(ImmutableList.toImmutableList())) {
//                  relevantEdges.add(new GIAARGStateEdge(parent, notTakenEdge));
//                }
//              }
//            }
//            for (ARGState coveredByCurrent : current.getCoveredByThis()) {
//              toAdd.addAll(coveredByCurrent.getParents());
//            }
//          }
//        }
//      }
//    }
//
//    // Assert that the all states with invariant are present
//    for (ARGState state : statesWithInvariants) {
//      if (relevantEdges.stream().noneMatch(edge -> edge.getSource().equals(state))) {
//        CFAUtils.leavingEdges(AbstractStates.extractLocation(state))
//            .forEach(e -> relevantEdges.add(new GIAARGStateEdge(state, e)));
//      }
//    }
//    try {
//      return writeGIAForInterpolant(
//          output, argRoot, relevantEdges, predicateCPA.getSolver().getFormulaManager());
//    } catch (InterruptedException pE) {
//      throw new CPAException("Failed to write GIA to file due to :", pE);
//    }
//  }
//
//  /**
//   * Create an GIA for the given set of edges Beneth printing the edges, each node gets a
// self-loop
//   * and a node to the temp-location
//   *
//   * @param sb the appendable to print to
//   * @param rootState the root state of the automaton
//   * @param edgesToAdd the edges between states to add
//   * @param pFmgr the formula manager neeeded to print the assumptions
//   * @throws IOException if the file cannot be accessed or does not exist
//   */
//  private int writeGIAForInterpolant(
//      Appendable sb, ARGState rootState, Set<GIAARGStateEdge> edgesToAdd, FormulaManagerView
// pFmgr)
//      throws IOException, InterruptedException {
//    int numProducedStates = 0;
//    sb.append(GIAGenerator.AUTOMATON_HEADER);
//
//    GIAGenerator.storeInitialNode(sb, edgesToAdd.isEmpty(),
// GIAGenerator.getNameOrError(rootState));
//    sb.append(String.format("    TRUE -> GOTO %s;\n\n", GIAGenerator.NAME_OF_TEMP_STATE));
//
//    // Fill the map to be able to iterate over the nodes
//    Map<ARGState, Set<GIAARGStateEdge>> nodesToEdges = new HashMap<>();
//    edgesToAdd.forEach(
//        e -> {
//          if (nodesToEdges.containsKey(e.getSource())) {
//            nodesToEdges.get(e.getSource()).add(e);
//          } else {
//            nodesToEdges.put(e.getSource(), Sets.newHashSet(e));
//          }
//        });
//
//    for (final ARGState currentState :
//        nodesToEdges.keySet().stream()
//            .sorted(Comparator.comparing(GIAGenerator::getNameOrError))
//            .collect(ImmutableList.toImmutableList())) {
//
//      sb.append(String.format("STATE USEALL %s :\n", GIAGenerator.getNameOrError(currentState)));
//      numProducedStates++;
//
//      for (GIAARGStateEdge edge : nodesToEdges.get(currentState)) {
//
//        sb.append("    MATCH \"");
//        AssumptionCollectorAlgorithm.escape(edge.getEdge().getRawStatement(), sb);
//        sb.append("\" -> ");
//        sb.append(edge.getStringOfAssumption(edge.getTarget()));
//        sb.append(String.format("GOTO %s", edge.getTargetName()));
//        sb.append(";\n");
//      }
//      sb.append("\n");
//      // Self loops are not needed, as only a single path is descriebed!
//
//    }
//    sb.append("END AUTOMATON\n");
//
//    return numProducedStates;
//  }
// }
