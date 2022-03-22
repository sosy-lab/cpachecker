// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.ucageneration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ucageneration.UCAGenerator.UCAGeneratorOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

public class UCAVioWitGenerator {

  public static final String DESC_OF_DUMMY_FUNC_START_EDGE = "Function start dummy edge";
  private final LogManager logger;
  private final ConfigurableProgramAnalysis cpa;
  private UCAGeneratorOptions optinons;

  public UCAVioWitGenerator(
      LogManager pLogger, UCAGeneratorOptions pOptions, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    this.logger = pLogger;
    this.optinons = pOptions;
    this.cpa = pCpa;
  }

  int produceUCA4ViolationWitness(Appendable output, UnmodifiableReachedSet reached)
      throws IOException, CPAException {
    final AbstractState firstState = reached.getFirstState();
    if (!(firstState instanceof ARGState)
        || UCAGenerator.getWitnessAutomatonState(firstState).isEmpty()) {
      output.append("Cannot dump assumption as automaton if ARGCPA is not used.");
    }

    // check, if the uca that should be generated (e.g. for a violation witness)
    // matches the reached set (meaning that the reached set contians at least
    // location with a property violation according to the specification

    boolean hasViolation =
        reached.stream().anyMatch(s -> s instanceof ARGState && ((ARGState) s).isTarget());
    if (!hasViolation) {
      throw new CPAException(
          "Cannot transform the UCA, as the reached set has no property violation");
    }

    // Goal: generate a set of the form (AutomatonState --EDGE--> AutomatonState)
    // and the root node

    final ARGState argRoot = (ARGState) reached.getFirstState();

    Optional<AutomatonState> automatonRootState = UCAGenerator.getWitnessAutomatonState(argRoot);
    if (automatonRootState.isEmpty()) {
      return produceUCAForARG(argRoot, output, reached);
    } else {
      return produceUCA4WitnessTransformation(output, reached, automatonRootState);
    }
  }

  private int produceUCAForARG(
      ARGState pArgRoot, Appendable pOutput, UnmodifiableReachedSet pReached) throws IOException {

    // scan reached set for all relevant states
    // Relevant state: Function enter, branching, loopHead or error states
    // We start at the root and iterate through the ARG
    Set<UCAARGStateEdge> relevantEdges = new HashSet<>();
    List<ARGState> toProcess = new ArrayList<>();
    toProcess.add(pArgRoot);
    List<ARGState> processed = new ArrayList<>();

    Set<ARGState> targetStates = getAllTargetStates(pReached);

    logger.logf(
        Level.INFO,
        String.format(
            "Target states found are %s",
            targetStates.stream()
                .map(a -> a.getStateId())
                .collect(ImmutableList.toImmutableList())));

    while (!toProcess.isEmpty()) {
      ARGState state = toProcess.remove(0);
      logger.logf(
          Level.INFO,
          "Taking %s from the list, processed %s, toProcess %s",
          state.getStateId(),
          processed.stream().map(a -> a.getStateId()).collect(ImmutableList.toImmutableList()),
          toProcess);

      for (ARGState child : state.getChildren()) {

        addAllRelevantEdges(state, child, targetStates, toProcess, relevantEdges, processed);
      }
      processed.add(state);
    }
    logger.log(
        Level.WARNING,
        relevantEdges.stream().map(e -> e.toString()).collect(ImmutableList.toImmutableList()));

    return writeUCAForViolationWitness(pOutput, pArgRoot, relevantEdges, false);
  }

  /**
   * Traverse allpath from parent to its child node, until a relevant edge is found
   *
   * @param pParent the parent
   * @param pChild its child to check for beeing relevant (i.e. any edge on the path leading to that
   *     edge )
   * @param pTargetStates the target states
   * @param pToProcess the states to process
   * @param pRelevantEdges the relevent ades to add the result to
   * @param pProcessed the edges already processed
   */
  private void addAllRelevantEdges(
      ARGState pParent,
      ARGState pChild,
      Set<ARGState> pTargetStates,
      List<ARGState> pToProcess,
      Set<UCAARGStateEdge> pRelevantEdges,
      List<ARGState> pProcessed) {

    // Check, if the pChild is relevant
    Optional<Pair<CFAEdge, Optional<ARGState>>> relevantEdge =
        gedEdgeIfIsRelevant(pChild, pParent, pTargetStates);
    boolean edgesAdded = false;
    if (relevantEdge.isPresent()) {
      // now, create a new edge.
      Pair<CFAEdge, Optional<ARGState>> pair = relevantEdge.orElseThrow();
      if (pair.getSecond().isEmpty()) {
        pRelevantEdges.add(new UCAARGStateEdge(pParent, pair.getFirst()));
      } else {
        pRelevantEdges.add(
            new UCAARGStateEdge(pParent, pair.getSecond().orElseThrow(), pair.getFirst()));
        if (!pProcessed.contains(pChild) && !pTargetStates.contains(pair.getSecond().get())) {
          logger.logf(Level.INFO, "Adding %s", pChild.getStateId());
          pToProcess.add(pChild);
        }
      }
      edgesAdded = true;
    } else {

      List<CFAEdge> pahtToChild = Lists.newArrayList(pParent.getFirstPathToChild(pChild));
      List<UCAARGStateEdge> edgesToAdd = new ArrayList<>();
      ARGState lastNodeUsed = pChild;
      // Check if there are any edges on the path that are relevant and if so, create for
      // each relevant edge an edge
      if (pahtToChild.size() > 1) {
        logger.logf(Level.INFO, "Processing a Multi-node");
        while (!pahtToChild.isEmpty()) {
          CFAEdge currentEdge = pahtToChild.get(pahtToChild.size() - 1);
          pahtToChild.remove(currentEdge);
          if (isRelevantEdge(currentEdge)) {
            ARGState intermediateState = new ARGState(null, null);
            edgesToAdd.add(new UCAARGStateEdge(intermediateState, lastNodeUsed, currentEdge));
            //            lastNodeUsed.addParent(intermediateState);
            lastNodeUsed = intermediateState;
          }
        }
        // remove the lastNodeused and replace it by parent
        ARGState finalLastNodeUsed = lastNodeUsed;
        ImmutableList<UCAARGStateEdge> edgesToUpdate =
            edgesToAdd.stream()
                .filter(e -> e.getSource().equals(finalLastNodeUsed))
                .collect(ImmutableList.toImmutableList());
        for (UCAARGStateEdge edge : edgesToUpdate) {
          edgesToAdd.remove(edge);
          if (edge.getTarget().isPresent())
            edgesToAdd.add(
                new UCAARGStateEdge(pParent, edge.getTarget().orElseThrow(), edge.getEdge()));
        }
        if (!edgesToAdd.isEmpty()) {
          pRelevantEdges.addAll(edgesToAdd);
          if (!pProcessed.contains(pChild)) {
            logger.logf(Level.INFO, "Adding %s", pChild.getStateId());
            pToProcess.add(pChild);
          }
          edgesAdded = true;
        }
      }
    }
    if (!edgesAdded) {
      for (ARGState grandChild : pChild.getChildren()) {
        logger.logf(
            Level.INFO,
            "No match found for parent %s and child %s, coninue with grandchild %s",
            pParent.getStateId(),
            pChild.getStateId(),
            grandChild.getStateId());
        // As there might be cycles with not-rpcoessed nodes, only continue with nodes that are
        // already expanded
        if (grandChild.wasExpanded()) {
          addAllRelevantEdges(
              pParent, grandChild, pTargetStates, pToProcess, pRelevantEdges, pProcessed);
        } else {
          // Add an edge to qtemp if needed
          Optional<UCAARGStateEdge> additionalEdgeToQtemp =
              getEdgeForNotExpandedNode(pParent, grandChild, pTargetStates);
          if (additionalEdgeToQtemp.isPresent()) {
            pRelevantEdges.add(additionalEdgeToQtemp.orElseThrow());
          }
        }
      }
    }
  }

  /**
   * An Edge is relevant, if: <br>
   * 1. The child is a loophead <br>
   * 2. The child is the direct successor of the parent and the edge is an assumeEdge or function
   * call edge <br>
   * 3. The child is a target state <br>
   * 4. The last edge on the path from parent to child is a dummy function call edge <br>
   * 5. The child node has a grandchild node that is a target state <br>
   *
   * @param pChild the child
   * @param pParent the parent
   * @param pTargetStates the list of target states
   * @return the last edge on the path from parent to child, if the edge is relevant, otherwise an
   *     empty optional and the target node to use for the edge
   */
  private Optional<Pair<CFAEdge, Optional<ARGState>>> gedEdgeIfIsRelevant(
      ARGState pChild, ARGState pParent, Set<ARGState> pTargetStates) {
    List<CFAEdge> pathToChild = pParent.getFirstPathToChild(pChild);
    @Nullable CFANode child = AbstractStates.extractLocation(pChild);

    // CAse 1 is irrelevant
    // Case 1:
    //    if (child != null && child.isLoopStart()) {
    //      if (pathToChild.isEmpty()) {
    //        return Optional.empty(); // To avoid NPE
    //      } else {
    //        return Optional.ofNullable(pathToChild.get(pathToChild.size() - 1));
    //      }
    //    }

    if (pathToChild.isEmpty()) {
      return Optional.empty();
    }

    final CFAEdge lastEdge = pathToChild.get(pathToChild.size() - 1);
    // Case 2:
    boolean case2 = lastEdge instanceof AssumeEdge;
    // Case 3:
    boolean case3 = (pTargetStates.contains(pChild));
    // Case 4:
    boolean case4 =
        ((lastEdge instanceof BlankEdge
            && lastEdge.getDescription().contains(DESC_OF_DUMMY_FUNC_START_EDGE)));
    // Case 5:
    boolean case5 = (pChild.getChildren().stream().anyMatch(gc -> pTargetStates.contains(gc)));

    // If the pChild cannot reach the error state, do not add it to the toProcessed
    // and let the edge goto the qTemp State (as not relevant for the path)
    // If it can reach the error state, add the edge and the child to the toProcess
    if (case2 || case3 || case4) {
      if (!canReachError(pChild, pTargetStates)) {

        return Optional.of(Pair.of(lastEdge, Optional.empty()));
      } else {
        return Optional.of(Pair.of(lastEdge, Optional.of(pChild)));
      }
    } else if (case5) {
      // If case 5 applies, we want to return the error state
      if (!canReachError(pChild, pTargetStates)) {
        return Optional.of(Pair.of(lastEdge, Optional.empty()));
      } else {
        return Optional.of(
            Pair.of(
                lastEdge,
                Optional.of(
                    pChild.getChildren().stream()
                        .filter(gc -> pTargetStates.contains(gc))
                        .findFirst()
                        .orElseThrow())));
      }
    }

    return Optional.empty();
  }

  private Optional<UCAARGStateEdge> getEdgeForNotExpandedNode(
      ARGState pParent, ARGState pGrandChild, Set<ARGState> pTargetStates) {
    Optional<Pair<CFAEdge, Optional<ARGState>>> relevantEdge =
        gedEdgeIfIsRelevant(pGrandChild, pParent, pTargetStates);
    if (relevantEdge.isPresent()) {
      return Optional.of(new UCAARGStateEdge(pParent, relevantEdge.orElseThrow().getFirst()));
    } else {
      return Optional.empty();
    }
  }

  private boolean isRelevantEdge(CFAEdge pCFAEdge) {
    return pCFAEdge instanceof FunctionCallEdge
        || (pCFAEdge instanceof BlankEdge
            && pCFAEdge.getDescription().contains(DESC_OF_DUMMY_FUNC_START_EDGE));
  }

  /**
   * Return true, if there is a children state that is the a target state
   *
   * @param pState the state to check the children for
   * @param pTargetStates the target states
   * @return Return true, if there is a children state that is the a target state
   */
  private boolean canReachError(ARGState pState, Set<ARGState> pTargetStates) {
    for (ARGState child : pState.getChildren()) {
      if (pTargetStates.contains(child)) return true;
      else {
        if (canReachError(child, pTargetStates)) {
          return true;
        } else {
          continue;
        }
      }
    }
    return false;
  }

  private int produceUCA4WitnessTransformation(
      Appendable output,
      UnmodifiableReachedSet reached,
      Optional<AutomatonState> automatonRootState)
      throws IOException {
    AutomatonState rootState = automatonRootState.orElseThrow();

    Set<UCAAutomatonStateEdge> edgesToAdd = new HashSet<>();

    // Next, filter the reached set fo all states, that have a different automaton
    // state compared to their predecessors, as these are the states that need to be stored in
    // the  uca

    for (AbstractState s : reached.asCollection()) {
      Optional<AutomatonState> automatonStateOpt = UCAGenerator.getWitnessAutomatonState(s);
      if (automatonStateOpt.isEmpty()) {
        logger.log(
            Level.WARNING,
            String.format("Cannot export state %s, as no AutomatonState is present", s));
        continue;
      }
      AutomatonState currentAutomatonState = automatonStateOpt.orElseThrow();
      @Nullable ARGState argState = AbstractStates.extractStateByType(s, ARGState.class);
      if (Objects.isNull(argState)) {
        logger.log(
            Level.WARNING, String.format("Cannot export state %s, as it is not an ARG State", s));
        continue;
      }

      Set<Pair<ARGState, AutomatonState>> parentsWithOtherAutomatonState =
          Sets.newConcurrentHashSet();

      for (ARGState parent : argState.getParents()) {
        Optional<AutomatonState> parentAutomatonState =
            UCAGenerator.getWitnessAutomatonState(parent);
        // If parent node has a automaton state and this is differnt to the one of the
        // child, add the child to statesWithNewAutomatonState
        if (parentAutomatonState.isPresent()
            && !parentAutomatonState.orElseThrow().equals(currentAutomatonState)
            && // automaton state is not already present in  parentsWithOtherAutomatonState
            parentsWithOtherAutomatonState.stream()
                .map(pair -> pair.getSecond())
                .noneMatch(state -> parentAutomatonState.orElseThrow().equals(state))) {
          parentsWithOtherAutomatonState.add(Pair.of(parent, parentAutomatonState.orElseThrow()));
        }
      }
      if (!parentsWithOtherAutomatonState.isEmpty()) {
        for (Pair<ARGState, AutomatonState> parentPair : parentsWithOtherAutomatonState) {
          // Create the edge
          CFAEdge edge = UCAGenerator.getEdge(parentPair, argState);
          edgesToAdd.add(
              new UCAAutomatonStateEdge(parentPair.getSecond(), currentAutomatonState, edge));
          // Check, if the parent node has any other outgoing edges, they have to be added aswell
          for (CFAEdge otherEdge :
              CFAUtils.leavingEdges(AbstractStates.extractLocation(parentPair.getFirst()))) {
            if (!otherEdge.equals(edge)) {
              edgesToAdd.add(new UCAAutomatonStateEdge(parentPair.getSecond(), otherEdge));
            }
          }
        }
      }
    }

    logger.log(
        Level.FINE, edgesToAdd.stream().map(e -> e.toString()).collect(Collectors.joining("\n")));

    return writeUCAForViolationWitness(
        output, rootState, edgesToAdd, optinons.isAutomatonIgnoreAssumptions());
  }

  /**
   * Create an UCA for the given set of edges Beneth printing the edges, each node gets a self-loop
   * and a node to the temp-location
   *
   * @param sb the appendable to print to
   * @param rootState the root state of the automaton
   * @param edgesToAdd the edges between states to add
   * @throws IOException if the file cannot be accessed or does not exist
   */
  private int writeUCAForViolationWitness(
      Appendable sb,
      AutomatonState rootState,
      Set<UCAAutomatonStateEdge> edgesToAdd,
      boolean ignoreAssumptions)
      throws IOException {
    int numProducedStates = 0;
    sb.append("OBSERVER AUTOMATON AssumptionAutomaton\n\n");

    String actionOnFinalEdges = "";

    UCAGenerator.storeInitialNode(sb, edgesToAdd.isEmpty(), UCAGenerator.getName(rootState));
    if (ignoreAssumptions) {
      sb.append(String.format("    TRUE -> GOTO %s;\n\n", UCAGenerator.NAME_OF_TEMP_STATE));
    } else {
      sb.append(
          String.format(
              "    TRUE -> ASSUME {false} GOTO %s;\n\n", UCAGenerator.NAME_OF_TEMP_STATE));
    }

    sb.append(String.format("STATE %s :\n", UCAGenerator.NAME_OF_ERROR_STATE));
    if (ignoreAssumptions) {
      sb.append(String.format("    TRUE -> GOTO %s;\n\n", UCAGenerator.NAME_OF_ERROR_STATE));
    } else {
      sb.append(
          String.format(
              "    TRUE -> ASSUME {true} GOTO %s;\n\n", UCAGenerator.NAME_OF_ERROR_STATE));
    }

    // Fill the map to be able to iterate over the nodes
    Map<AutomatonState, Set<UCAAutomatonStateEdge>> nodesToEdges = new HashMap<>();
    edgesToAdd.forEach(
        e -> {
          if (nodesToEdges.containsKey(e.getSource())) {
            nodesToEdges.get(e.getSource()).add(e);
          } else {
            nodesToEdges.put(e.getSource(), Sets.newHashSet(e));
          }
        });

    for (final AutomatonState currentState :
        nodesToEdges.keySet().stream()
            .sorted(Comparator.comparing(UCAGenerator::getName))
            .collect(ImmutableList.toImmutableList())) {

      sb.append(String.format("STATE USEALL %s :\n", UCAGenerator.getName(currentState)));
      numProducedStates++;

      for (UCAAutomatonStateEdge edge : nodesToEdges.get(currentState)) {

        sb.append("    MATCH \"");
        AssumptionCollectorAlgorithm.escape(UCAGenerator.getEdgeString(edge.getEdge()), sb);
        sb.append("\" -> ");
        sb.append(String.format("GOTO %s", edge.getTargetName()));
        sb.append(";\n");
      }
      if (!currentState.isTarget()) {
        sb.append(
            String.format(
                "    TRUE -> " + actionOnFinalEdges + "GOTO %s;\n\n",
                UCAGenerator.getName(currentState)));
        //        sb.append("    TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");
      }
    }
    sb.append("END AUTOMATON\n");

    return numProducedStates;
  }

  /**
   * Create an UCA for the given set of edges Beneth printing the edges, each node gets a self-loop
   * and a node to the temp-location
   *
   * @param sb the appendable to print to
   * @param rootState the root state of the automaton
   * @param edgesToAdd the edges between states to add
   * @throws IOException if the file cannot be accessed or does not exist
   */
  private int writeUCAForViolationWitness(
      Appendable sb, ARGState rootState, Set<UCAARGStateEdge> edgesToAdd, boolean ignoreAssumptions)
      throws IOException {
    // TODO Refactor this method and the copied to one
    int numProducedStates = 0;
    sb.append("OBSERVER AUTOMATON AssumptionAutomaton\n\n");

    String actionOnFinalEdges = "";

    UCAGenerator.storeInitialNode(sb, edgesToAdd.isEmpty(), UCAGenerator.getName(rootState));
    if (ignoreAssumptions) {
      sb.append(String.format("    TRUE -> GOTO %s;\n\n", UCAGenerator.NAME_OF_TEMP_STATE));
    } else {
      sb.append(
          String.format(
              "    TRUE -> ASSUME {false} GOTO %s;\n\n", UCAGenerator.NAME_OF_TEMP_STATE));
    }

    sb.append(String.format("STATE %s :\n", UCAGenerator.NAME_OF_ERROR_STATE));
    if (ignoreAssumptions) {
      sb.append(String.format("    TRUE -> GOTO %s;\n\n", UCAGenerator.NAME_OF_ERROR_STATE));
    } else {
      sb.append(
          String.format(
              "    TRUE -> ASSUME {true} GOTO %s;\n\n", UCAGenerator.NAME_OF_ERROR_STATE));
    }

    // Fill the map to be able to iterate over the nodes
    Map<ARGState, Set<UCAARGStateEdge>> nodesToEdges = new HashMap<>();
    edgesToAdd.forEach(
        e -> {
          if (nodesToEdges.containsKey(e.getSource())) {
            nodesToEdges.get(e.getSource()).add(e);
          } else {
            nodesToEdges.put(e.getSource(), Sets.newHashSet(e));
          }
        });

    for (final ARGState currentState :
        nodesToEdges.keySet().stream()
            .sorted(Comparator.comparing(UCAGenerator::getName))
            .collect(ImmutableList.toImmutableList())) {

      sb.append(String.format("STATE USEALL %s :\n", UCAGenerator.getName(currentState)));
      numProducedStates++;

      for (UCAARGStateEdge edge : nodesToEdges.get(currentState)) {

        sb.append("    MATCH \"");
        AssumptionCollectorAlgorithm.escape(UCAGenerator.getEdgeString(edge.getEdge()), sb);
        sb.append("\" -> ");
        sb.append(String.format("GOTO %s", edge.getTargetName()));
        sb.append(";\n");
      }
      if (!currentState.isTarget()) {
        sb.append(
            String.format(
                "    TRUE -> " + actionOnFinalEdges + "GOTO %s;\n\n",
                UCAGenerator.getName(currentState)));
        //        sb.append("    TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");
      }
    }
    sb.append("END AUTOMATON\n");

    return numProducedStates;
  }

  private Set<ARGState> getAllTargetStates(UnmodifiableReachedSet pReached) {
    Set<ARGState> targetStates = new HashSet<>();
    for (ARGState errorState :
        pReached.asCollection().stream()
            .filter(s -> AbstractStates.isTargetState(s))
            .map(s -> AbstractStates.extractStateByType(s, ARGState.class))
            .collect(ImmutableList.toImmutableList())) {
      assert !errorState.isCovered();
      targetStates.add(errorState);
    }
    targetStates.addAll(pReached.asCollection().stream()
        .filter(
            state ->
                AbstractStates.extractStateByType(state, AssumptionStorageState.class).isStop())
        .map(s -> AbstractStates.extractStateByType(s, ARGState.class))
        .collect(ImmutableList.toImmutableList()));

    return targetStates;
  }
}
