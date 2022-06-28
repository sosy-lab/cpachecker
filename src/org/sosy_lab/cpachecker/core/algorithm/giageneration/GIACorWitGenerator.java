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
// import com.google.common.collect.Sets;
// import java.io.IOException;
// import java.util.Comparator;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.Map;
// import java.util.Objects;
// import java.util.Optional;
// import java.util.Set;
// import java.util.logging.Level;
// import java.util.stream.Collectors;
// import org.checkerframework.checker.nullness.qual.Nullable;
// import org.sosy_lab.common.log.LogManager;
// import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
// import org.sosy_lab.cpachecker.cfa.model.CFANode;
// import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
// import org.sosy_lab.cpachecker.core.algorithm.giageneration.GIAGenerator.GIAGeneratorOptions;
// import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
// import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
// import org.sosy_lab.cpachecker.cpa.arg.ARGState;
// import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
// import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
// import org.sosy_lab.cpachecker.exceptions.CPAException;
// import org.sosy_lab.cpachecker.util.AbstractStates;
// import org.sosy_lab.cpachecker.util.CFAUtils;
// import org.sosy_lab.cpachecker.util.Pair;
// import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
// import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
// import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
// import org.sosy_lab.java_smt.api.BooleanFormula;
//
// public class GIACorWitGenerator {
//
//  private final LogManager logger;
//  private GIAGeneratorOptions optinons;
//
//  public GIACorWitGenerator(LogManager pLogger, GIAGeneratorOptions pOptions) {
//    this.logger = pLogger;
//    this.optinons = pOptions;
//  }
//
//  int produceGIA4CorrectnessWitness(Appendable output, UnmodifiableReachedSet reached)
//      throws IOException, CPAException {
//    final AbstractState firstState = reached.getFirstState();
//    if (!(firstState instanceof ARGState)
//        || GIAGenerator.getWitnessAutomatonState(firstState).isEmpty()) {
//      output.append("Cannot dump assumption as automaton if ARGCPA is not used.");
//      return 0;
//    }
//
//    // check, if the GIA that should be generated (e.g. for a violation witness)
//    // matches the reached set (meaning that the reached set contians at least
//    // location with a property violation according to the specification
//
//    // We do not need to care about error states!
//
//    // Goal: generate a set of the form (AutomatonState --EDGE--> AutomatonState)
//    // and the root node
//
//    final ARGState argRoot = (ARGState) reached.getFirstState();
//    //    AutomatonState rootState = GIAGenerator.getWitnessAutomatonState(argRoot).orElseThrow();
//
//    Set<GIAAutomatonStateEdge> edgesInAutomatonStates = new HashSet<>();
//    Set<GIAARGStateEdgeWithAssumptionNaming> edgesToAdd = new HashSet<>();
//
//    // Next, filter the reached set fo all states, that have a different automaton
//    // state compared to their predecessors, as these are the states that need to be stored in
//    // the  GIA
//
//    for (AbstractState s : reached.asCollection()) {
//      Optional<AutomatonState> automatonStateOpt = GIAGenerator.getWitnessAutomatonState(s);
//      if (automatonStateOpt.isEmpty()) {
//        logger.log(
//            Level.WARNING,
//            String.format("Cannot export state %s, as no AutomatonState is present", s));
//        continue;
//      }
//      AutomatonState currentAutomatonState = automatonStateOpt.orElseThrow();
//      logger.log(Level.INFO, currentAutomatonState);
//      @Nullable ARGState argState = AbstractStates.extractStateByType(s, ARGState.class);
//      if (Objects.isNull(argState)) {
//        logger.log(
//            Level.WARNING, String.format("Cannot export state %s, as it is not an ARG State", s));
//        continue;
//      }
//
//      Set<Pair<ARGState, AutomatonState>> parentsWithOtherAutomatonState =
//          Sets.newConcurrentHashSet();
//
//      for (ARGState parent : argState.getParents()) {
//        Optional<AutomatonState> parentAutomatonState =
//            GIAGenerator.getWitnessAutomatonState(parent);
//        // If parent node has a automaton state and this is differnt to the one of the
//        // child, add the child to statesWithNewAutomatonState
//        if (parentAutomatonState.isPresent()
//            && !parentAutomatonState.orElseThrow().equals(currentAutomatonState)
//            && // automaton state is not already present in  parentsWithOtherAutomatonState
//            parentsWithOtherAutomatonState.stream()
//                .map(pair -> pair.getSecond())
//                .noneMatch(state -> parentAutomatonState.orElseThrow().equals(state))) {
//          parentsWithOtherAutomatonState.add(Pair.of(parent, parentAutomatonState.orElseThrow()));
//        } else {
//          logger.logf(
//              Level.INFO,
//              "Ignoring state %s, as already present ",
//              parentAutomatonState.orElseThrow());
//        }
//      }
//      if (!parentsWithOtherAutomatonState.isEmpty()) {
//        for (Pair<ARGState, AutomatonState> parentPair : parentsWithOtherAutomatonState) {
//          // Create the edge
//          CFAEdge edge = GIAGenerator.getEdge(parentPair, argState);
//          edgesInAutomatonStates.add(
//              new GIAAutomatonStateEdge(parentPair.getSecond(), currentAutomatonState, edge));
//          edgesToAdd.add(
//              new GIAARGStateEdgeWithAssumptionNaming(
//                  parentPair.getFirst(),
//                  AbstractStates.extractStateByType(s, ARGState.class),
//                  edge));
//
//          // Check, if the parent node has any other outgoing edges, they have to be added aswell
//          for (CFAEdge otherEdge :
//              CFAUtils.leavingEdges(AbstractStates.extractLocation(parentPair.getFirst()))) {
//            if (!otherEdge.equals(edge)) {
//              edgesInAutomatonStates.add(
//                  new GIAAutomatonStateEdge(parentPair.getSecond(), otherEdge));
//            }
//          }
//        }
//      }
//    }
//
//    logger.log(
//        Level.FINE,
//        edgesInAutomatonStates.stream().map(e -> e.toString()).collect(Collectors.joining("\n")));
//
//    try {
//      return writeGAIForCorrectnessWitness(
//          output, argRoot, edgesToAdd, optinons.isAutomatonIgnoreAssumptions());
//    } catch (InterruptedException pE) {
//      throw new CPAException("Parsing faled due to", pE);
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
//   * @throws IOException if the file cannot be accessed or does not exist
//   */
//  private int writeGAIForCorrectnessWitness(
//      Appendable sb,
//      ARGState rootState,
//      Set<GIAARGStateEdgeWithAssumptionNaming> edgesToAdd,
//      boolean ignoreAssumptions)
//      throws IOException, InterruptedException {
//    int numProducedStates = 0;
//    sb.append(GIAGenerator.AUTOMATON_HEADER);
//
//    String actionOnFinalEdges = "";
//
//    Optional<AutomatonState> autoRootState = GIAGenerator.getWitnessAutomatonState(rootState);
//    if (autoRootState.isPresent()) {
//      GIAGenerator.storeInitialNode(
//          sb, edgesToAdd.isEmpty(), GIAGenerator.getNameOrError(autoRootState.get()));
//    } else {
//      GIAGenerator.storeInitialNode(
//          sb, edgesToAdd.isEmpty(), GIAGenerator.getNameOrError(rootState));
//    }
//    sb.append(String.format("STATE %s :\n", GIAGenerator.NAME_OF_FINAL_STATE));
//    sb.append(String.format("    TRUE -> GOTO %s;\n\n", GIAGenerator.NAME_OF_FINAL_STATE));
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
//      Optional<AutomatonState> curAssumptionState =
//          GIAGenerator.getWitnessAutomatonState(currentState);
//      if (curAssumptionState.isPresent()) {
//        sb.append(
//            String.format(
//                "STATE USEALL %s :\n",
//                GIAGenerator.getNameOrError(curAssumptionState.orElseThrow())));
//      } else {
//        sb.append(String.format("STATE USEALL %s :\n",
// GIAGenerator.getNameOrError(currentState)));
//      }
//      numProducedStates++;
//
//      for (GIAARGStateEdge edge : nodesToEdges.get(currentState)) {
//
//        sb.append("    MATCH \"");
//        AssumptionCollectorAlgorithm.escape(GIAGenerator.getEdgeString(edge.getEdge()), sb);
//        sb.append("\" -> ");
//        sb.append(String.format("GOTO %s", edge.getTargetName()));
//        sb.append(";\n");
//      }
//
//      if (!currentState.isTarget()) {
//        if (curAssumptionState.isPresent()) {
//          sb.append(
//              String.format(
//                  "    MATCH OTHERWISE -> " + actionOnFinalEdges + "GOTO %s;\n",
//                  GIAGenerator.getNameOrError(curAssumptionState.orElseThrow())));
//        } else {
//          sb.append(
//              String.format(
//                  "    MATCH OTHERWISE -> " + actionOnFinalEdges + "GOTO %s;\n",
//                  GIAGenerator.getNameOrError(currentState)));
//        }
//      }
//      // Add a edge to __TRUE, as all states are accepting
//      sb.append(String.format("    TRUE -> " + "GOTO %s;\n", GIAGenerator.NAME_OF_FINAL_STATE));
//
//      @Nullable AssumptionStorageState assumptionState =
//          AbstractStates.extractStateByType(currentState, AssumptionStorageState.class);
//      if (Objects.nonNull(assumptionState) && !assumptionState.isAssumptionTrue()) {
//        // Add indent to be able to use util mehtods
//        // TODO: Find out, why Modulo operations are exported that strange: (Happens already
// during
//        // add in AssunptionTransferRelation
//        // Example: ( ( ( ( ( y % 2 ) < 2 ) && ( ! ( y < ( y % 2 ) ) ) ) && ( ( y % 2 ) == ( ( y %
// 2
//        // ) % 2 ) ) ) && ( ( y % 2 ) == 1 ) ) for ( ( y % 2 ) == 1 )
//        @Nullable CFANode cfaNode = AbstractStates.extractLocation(currentState);
//        if (!ignoreAssumptions && Objects.nonNull(assumptionState) && Objects.nonNull(cfaNode)) {
//          FormulaManagerView fmgr = assumptionState.getFormulaManager();
//          final BooleanFormulaManagerView bmgr =
//              assumptionState.getFormulaManager().getBooleanFormulaManager();
//          BooleanFormula assumption =
//              bmgr.and(assumptionState.getAssumption(), assumptionState.getStopFormula());
//          if (!bmgr.isTrue(assumption)) {
//            sb.append("    INVARIANT {");
//            AssumptionCollectorAlgorithm.escape(
//                ExpressionTrees.fromFormula(assumption, fmgr, cfaNode).toString(), sb);
//            sb.append("};\n");
//          }
//        }
//      }
//      sb.append("\n");
//    }
//
//    sb.append("END AUTOMATON\n");
//
//    return numProducedStates;
//  }
// }
