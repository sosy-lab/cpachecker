// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.ucageneration;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.arg.ARGUtils.getUncoveredChildrenView;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.assumptions.AssumptionWithLocation;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

@Options(prefix = "assumptions")
public class UCACollector {

  private static final String NAME_OF_WITNESS_AUTOMATON = "WitnessAutomaton";
  private static final String NAME_OF_TEMP_STATE = "__qTEMP";
  private static final String NAME_OF_ERROR_STATE = "__qERROR";

  @SuppressWarnings("unused")
  private static final String NAME_OF_FINAL_STATE = "__qFINAL";

  @SuppressWarnings("unused")
  private static final String NAME_OF_NEWTESTINPUT_STATE = "__qNEWTEST";

  private final ShutdownNotifier shutdownNotifier;

  @Option(
      secure = true,
      description =
          "If it is enabled, automaton does not add assumption which is considered to continue path with corresponding this edge.")
  private boolean automatonIgnoreAssumptions = false;

  @Option(secure = true, description = "Generate uca for violation witness")
  private boolean genUCA4ViolationWitness = false;

  @Option(secure = true, description = "Generate uca for testcomp testcase")
  private boolean genUCA4Testcase = false;

  private final LogManager logger;
  private final Algorithm innerAlgorithm;
  private final FormulaManagerView formulaManager;
  private final AssumptionWithLocation exceptionAssumptions;
  private final BooleanFormulaManager bfmgr;
  private final CFA cfa;
  private final ConfigurableProgramAnalysis cpa;
  private final Configuration config;

  private int universalConditionAutomaton = 0;

  public UCACollector(
      Algorithm algo,
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    this.logger = pLogger;
    this.innerAlgorithm = algo;
    shutdownNotifier = pShutdownNotifier;
    AssumptionStorageCPA asCpa =
        CPAs.retrieveCPAOrFail(pCpa, AssumptionStorageCPA.class, AssumptionStorageCPA.class);

    this.formulaManager = asCpa.getFormulaManager();
    this.bfmgr = formulaManager.getBooleanFormulaManager();
    this.exceptionAssumptions = new AssumptionWithLocation(formulaManager);
    this.cpa = pCpa;
    this.cfa = pCfa;
    this.config = pConfig;
  }

  public int produceUniversalConditionAutomaton(
      Appendable output, UnmodifiableReachedSet reached, Set<Integer> pExceptionStates)
      throws IOException, CPAException {

    if (genUCA4ViolationWitness) {
      produceUCA4ViolationWitness(output, reached);
    } else if (genUCA4Testcase) {
      produceUCA4Testcase(output, reached, pExceptionStates);
    }
    return this.universalConditionAutomaton;
  }

  private void produceUCA4Testcase(
      Appendable output, UnmodifiableReachedSet reached, Set<Integer> pExceptionStates)
      throws IOException {
    final AbstractState firstState = reached.getFirstState();
    if (!(firstState instanceof ARGState)) {
      output.append("Cannot dump assumption as automaton if ARGCPA is not used.");
    }
    if (!(reached.getLastState() instanceof ARGState)) {
      output.append("Cannot dump assumption as automaton if ARGCPA is not used.");
    }
    ARGState lastState = (ARGState) reached.getLastState();

    // check, if the uca that should be generated (e.g. for a violation witness)
    // matches the reached set (meaning that the reached set contians at least
    // location with a property violation according to the specification

    Set<AbstractState> falseAssumptionStates =
        AssumptionCollectorAlgorithm.getFalseAssumptionStates(reached, true, cpa);

    // scan reached set for all relevant states with an assumption
    // Invariant: relevantStates does not contain any covered state.
    // A covered state is always replaced by its covering state.
    Set<ARGState> relevantStates = new TreeSet<>();

    Set<ARGState> potentialLastStates = new HashSet<>();
    for (AbstractState state : reached) {
      ARGState e = (ARGState) state;
      AssumptionStorageState asmptState =
          AbstractStates.extractStateByType(e, AssumptionStorageState.class);

      boolean hasFalseAssumption =
          e.isTarget() || asmptState.isStop() || pExceptionStates.contains(e.getStateId());

      boolean isRelevant = !asmptState.isAssumptionTrue();

      if (e.isCovered()) {
        e = e.getCoveringState(); // replace with covering state
        assert !e.isCovered();
      }

      if (hasFalseAssumption) {
        falseAssumptionStates.add(e);
      }

      if (relevantStates.contains(e)) {
        continue;
      }

      if (isRelevant || falseAssumptionStates.contains(e)) {
        // now add e and all its transitive parents to the relevantStates set
        AssumptionCollectorAlgorithm.findAllParents(e, relevantStates);
        potentialLastStates.add(e);
      }
    }
    // Update the last state if needed
    Set<ARGState> lastStates;
    if (!relevantStates.contains(lastState)) {
      lastStates =
          potentialLastStates.stream()
              .filter(s -> hasNoSuccessor(s, relevantStates))
              .collect(Collectors.toSet());
    } else {
      lastStates = Sets.newHashSet((ARGState) reached.getLastState());
    }

    falseAssumptionStates.addAll(lastStates);

    writeUCAForTestcase(
        output, (ARGState) firstState, lastStates, relevantStates, falseAssumptionStates);
  }


  /**
   * Checks, if the Node pS in the path has no successors in pRelevantStates, and hence is an
   * assumption state without successors.
   * @param pS the state to check
   * @param pRelevantStates the other states in the graph
   * @return true, if pS has no child in pRelevantStates.
   */
  private boolean hasNoSuccessor(ARGState pS, Set<ARGState> pRelevantStates) {
    return pS.getChildren().stream().noneMatch(child -> pRelevantStates.contains(child));
  }

  /**
   * Create a String containing the assumption automaton.
   *
   * @param sb Where to write the String into.
   * @param pInitialState The initial state of the automaton.
   * @param pLastState
   * @param relevantStates A set with all states with non-trivial assumptions (all others will have
   *     assumption TRUE).
   * @param falseAssumptionStates A set with all states with the assumption FALSE
   * @return the number of states contained in the written automaton
   */
  public static int writeUCAForTestcase(
      Appendable sb,
      ARGState pInitialState,
      Set<ARGState> pLastStates,
      Set<ARGState> relevantStates,
      Set<AbstractState> falseAssumptionStates)
      throws IOException {

    if (relevantStates.isEmpty()) {
      return 0;
    }

    int numProducedStates = 0;
    sb.append("OBSERVER AUTOMATON AssumptionAutomaton\n\n");

    String actionOnFinalEdges = "";
    String initialStateName;

    initialStateName = "ARG" + pInitialState.getStateId();

    sb.append("INITIAL STATE ").append(initialStateName).append(";\n\n");
    sb.append("STATE __TRUE :\n");
    sb.append("    TRUE -> GOTO __TRUE;\n\n");

    if (!falseAssumptionStates.isEmpty()) {
      sb.append("STATE __FALSE :\n");
      sb.append("    TRUE -> ASSUME {false} GOTO __FALSE;\n\n");
    }

    for (final ARGState s : relevantStates) {
      assert !s.isCovered();

      if (falseAssumptionStates.contains(s)) {
        continue;
      }
      if (pLastStates.contains(s)) {
        sb.append(String.format("STATE USEFIRST %s :\n", NAME_OF_NEWTESTINPUT_STATE));
      } else {
        sb.append("STATE USEFIRST ARG" + s.getStateId() + " :\n");
      }
      numProducedStates++;

      boolean branching = false;

      final StringBuilder descriptionForInnerMultiEdges = new StringBuilder();
      int multiEdgeID = 0;

      for (final ARGState child : getUncoveredChildrenView(s)) {
        assert !child.isCovered();

        List<CFAEdge> edges = s.getEdgesToChild(child);

        if (edges.size() > 1) {
          sb.append("    MATCH \"");
          AssumptionCollectorAlgorithm.escape(edges.get(0).getRawStatement(), sb);
          sb.append("\" -> ");
          sb.append("GOTO ARG" + s.getStateId() + "M" + multiEdgeID);

          boolean first = true;
          for (CFAEdge innerEdge : from(edges).skip(1)) {

            if (!first) {
              multiEdgeID++;
              descriptionForInnerMultiEdges.append(
                  "GOTO ARG" + s.getStateId() + "M" + multiEdgeID + ";\n\n");
              //              descriptionForInnerMultiEdges.append(
              //                  "    TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");
            } else {
              first = false;
            }

            descriptionForInnerMultiEdges.append(
                "STATE USEFIRST ARG" + s.getStateId() + "M" + multiEdgeID + " :\n");
            numProducedStates++;
            descriptionForInnerMultiEdges.append("    MATCH \"");
            AssumptionCollectorAlgorithm.escape(
                innerEdge.getRawStatement(), descriptionForInnerMultiEdges);
            descriptionForInnerMultiEdges.append("\" -> ");
          }

          AssumptionStorageState assumptionChild =
              AbstractStates.extractStateByType(child, AssumptionStorageState.class);
          AssumptionCollectorAlgorithm.addAssumption(
              descriptionForInnerMultiEdges, assumptionChild, false);
          AssumptionCollectorAlgorithm.finishTransition(
              descriptionForInnerMultiEdges,
              child,
              relevantStates,
              falseAssumptionStates,
              actionOnFinalEdges,
              branching);
          descriptionForInnerMultiEdges.append(";\n");

          //          descriptionForInnerMultiEdges.append(
          //              "    TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");

        } else {

          sb.append("    MATCH \"");
          AssumptionCollectorAlgorithm.escape(
              Iterables.getOnlyElement(edges).getRawStatement(), sb);
          sb.append("\" -> ");

          AssumptionStorageState assumptionChild =
              AbstractStates.extractStateByType(child, AssumptionStorageState.class);
          AssumptionCollectorAlgorithm.addAssumption(sb, assumptionChild, false);
          if (pLastStates.contains(child)) {
            sb.append(NAME_OF_NEWTESTINPUT_STATE);
          } else {
            AssumptionCollectorAlgorithm.finishTransition(
                sb, child, relevantStates, falseAssumptionStates, actionOnFinalEdges, branching);
          }
        }
        sb.append(";\n\n");
      }
      //      sb.append("    TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");
      sb.append(descriptionForInnerMultiEdges);
    }
    sb.append(String.format("STATE %s :\n", NAME_OF_NEWTESTINPUT_STATE));
    sb.append("    TRUE -> ASSUME GOTO __FALSE;\n\n");

    sb.append("END AUTOMATON\n");

    return numProducedStates;
  }

  private void produceUCA4ViolationWitness(Appendable output, UnmodifiableReachedSet reached)
      throws IOException, CPAException {
    final AbstractState firstState = reached.getFirstState();
    if (!(firstState instanceof ARGState) || getWitnessAutomatonState(firstState).isEmpty()) {
      output.append("Cannot dump assumption as automaton if ARGCPA is not used.");
    }

    // check, if the uca that should be generated (e.g. for a violation witness)
    // matches the reached set (meaning that the reached set contians at least
    // location with a property violation according to the specification

    boolean hasViolation =
        reached.stream().anyMatch(s -> s instanceof ARGState && ((ARGState) s).isTarget());
    if (genUCA4ViolationWitness && !hasViolation) {
      throw new CPAException(
          "Cannot transform the UCA, as the reached set has no property violation");
    }

    // Goal: generate a set of the form (AutomatonState --EDGE--> AutomatonState)
    // and the root node

    final ARGState argRoot = (ARGState) reached.getFirstState();
    AutomatonState rootState = getWitnessAutomatonState(argRoot).get();

    Set<UCAEdge> edgesToAdd = Sets.newHashSet();

    // Next, filter the reached set fo all states, that have a different automaton
    // state compared to their predecessors, as these are the states that need to be stored in
    // the  uca

    for (AbstractState s : reached.asCollection()) {
      Optional<AutomatonState> automatonStateOpt = getWitnessAutomatonState(s);
      if (automatonStateOpt.isEmpty()) {
        logger.log(
            Level.WARNING,
            String.format("Cannot export state %s, as no AutomatonState is present", s));
        continue;
      }
      AutomatonState currentAutomatonState = automatonStateOpt.get();
      @Nullable ARGState argState = AbstractStates.extractStateByType(s, ARGState.class);
      if (Objects.isNull(argState)) {
        logger.log(
            Level.WARNING, String.format("Cannot export state %s, as it is not an ARG State", s));
        continue;
      }

      Set<Pair<ARGState, AutomatonState>> parentsWithOtherAutomatonState =
          Sets.newConcurrentHashSet();

      for (ARGState parent : argState.getParents()) {
        Optional<AutomatonState> parentAutomatonState = getWitnessAutomatonState(parent);
        // If parent node has a automaton state and this is differnt to the one of the
        // child, add the child to statesWithNewAutomatonState
        if (parentAutomatonState.isPresent()
            && !parentAutomatonState.get().equals(currentAutomatonState)
            && // automaton state is not already present in  parentsWithOtherAutomatonState
            parentsWithOtherAutomatonState.stream()
                .map(pair -> pair.getSecond())
                .noneMatch(state -> state.equals(parentAutomatonState.get()))) {
          parentsWithOtherAutomatonState.add(Pair.of(parent, parentAutomatonState.get()));
        }
      }
      if (!parentsWithOtherAutomatonState.isEmpty()) {
        for (Pair<ARGState, AutomatonState> parentPair : parentsWithOtherAutomatonState) {
          // Create the edge
          CFAEdge edge = getEdge(parentPair, argState);
          edgesToAdd.add(new UCAEdge(parentPair.getSecond(), currentAutomatonState, edge));
          // Check, if the parent node has any other outgoing edges, they have to be added aswell
          for (CFAEdge otherEdge :
              CFAUtils.leavingEdges(AbstractStates.extractLocation(parentPair.getFirst()))) {
            if (!otherEdge.equals(edge)) {
              edgesToAdd.add(new UCAEdge(parentPair.getSecond(), otherEdge));
            }
          }
        }
      }
    }

    logger.log(
        Level.FINE, edgesToAdd.stream().map(e -> e.toString()).collect(Collectors.joining("\n")));

    universalConditionAutomaton +=
        writeUniversalConditionAutomatonForViolationWitness(
            output, rootState, edgesToAdd, automatonIgnoreAssumptions);
  }

  private Optional<AutomatonState> getWitnessAutomatonState(AbstractState s) {
    Optional<AbstractState> target =
        AbstractStates.asIterable(s)
            .filter(
                cs -> {
                  if (cs instanceof AutomatonState) {
                    return ((AutomatonState) cs)
                        .getOwningAutomatonName()
                        .equals(NAME_OF_WITNESS_AUTOMATON);
                  }
                  return false;
                })
            .stream()
            .findFirst();
    if (target.isPresent() && target.get() instanceof AutomatonState) {
      return Optional.of((AutomatonState) target.get());
    }
    return Optional.empty();
  }

  private CFAEdge getEdge(Pair<ARGState, AutomatonState> parentPair, ARGState argState) {
    return ARGUtils.getOnePathFromTo(
            (x) ->
                Objects.nonNull(x)
                    && Objects.nonNull(parentPair.getFirst())
                    && x.equals(parentPair.getFirst()),
            argState)
        .getFullPath()
        .get(0);
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
  private int writeUniversalConditionAutomatonForViolationWitness(
      Appendable sb, AutomatonState rootState, Set<UCAEdge> edgesToAdd, boolean ignoreAssumptions)
      throws IOException {
    int numProducedStates = 0;
    sb.append("OBSERVER AUTOMATON AssumptionAutomaton\n\n");

    String actionOnFinalEdges = "";

    String initialStateName;
    if (edgesToAdd.isEmpty()) {
      initialStateName = "__TRUE";
    } else {
      initialStateName = getName(rootState);
    }

    sb.append("INITIAL STATE ").append(initialStateName).append(";\n\n");
    sb.append("STATE __TRUE :\n");
    sb.append("    TRUE -> GOTO __TRUE;\n\n");

    sb.append(String.format("STATE %s :\n", NAME_OF_TEMP_STATE));
    if (ignoreAssumptions) {
      sb.append(String.format("    TRUE -> GOTO %s;\n\n", NAME_OF_TEMP_STATE));
    } else {
      sb.append(String.format("    TRUE -> ASSUME {false} GOTO %s;\n\n", NAME_OF_TEMP_STATE));
    }

    sb.append(String.format("STATE %s :\n", NAME_OF_ERROR_STATE));
    if (ignoreAssumptions) {
      sb.append(String.format("    TRUE -> GOTO %s;\n\n", NAME_OF_ERROR_STATE));
    } else {
      sb.append(String.format("    TRUE -> ASSUME {true} GOTO %s;\n\n", NAME_OF_ERROR_STATE));
    }

    // Fill the map to be able to iterate over the nodes
    Map<AutomatonState, Set<UCAEdge>> nodesToEdges = Maps.newHashMap();
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
            .sorted(Comparator.comparing(this::getName))
            .collect(Collectors.toList())) {

      sb.append(String.format("STATE USEFIRST %s :\n", getName(currentState)));
      numProducedStates++;

      for (UCAEdge edge : nodesToEdges.get(currentState)) {

        sb.append("    MATCH \"");
        AssumptionCollectorAlgorithm.escape(edge.getEdgeString(), sb);
        sb.append("\" -> ");
        sb.append(String.format("GOTO %s", edge.getTargetName()));
        sb.append(";\n");
      }
      if (!currentState.isTarget()) {
        sb.append(
            String.format(
                "    TRUE -> " + actionOnFinalEdges + "GOTO %s;\n\n", getName(currentState)));
        //        sb.append("    TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");
      }
    }
    sb.append("END AUTOMATON\n");

    return numProducedStates;
  }

  private String getName(AutomatonState s) {
    return s.isTarget() ? NAME_OF_ERROR_STATE : s.getInternalStateName();
  }

  private class UCAEdge {
    AutomatonState source;
    Optional<AutomatonState> target;
    CFAEdge edge;

    public UCAEdge(AutomatonState pSource, AutomatonState pTarget, CFAEdge pEdge) {
      this.source = pSource;
      this.target = Optional.of(pTarget);
      this.edge = pEdge;
    }

    public UCAEdge(AutomatonState pSource, CFAEdge pEdge) {
      this.source = pSource;
      this.target = Optional.empty();
      this.edge = pEdge;
    }

    public String getSourceName() {
      return getName(source);
    }

    public String getTargetName() {
      return this.target.isPresent() ? getName(target.get()) : NAME_OF_TEMP_STATE;
    }

    public AutomatonState getSource() {
      return source;
    }

    public Optional<AutomatonState> getTarget() {
      return target;
    }

    public CFAEdge getEdge() {
      return edge;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || !(o instanceof UCAEdge)) {
        return false;
      }
      UCAEdge ucaEdge = (UCAEdge) o;
      return com.google.common.base.Objects.equal(source, ucaEdge.source)
          && com.google.common.base.Objects.equal(target, ucaEdge.target)
          && com.google.common.base.Objects.equal(edge, ucaEdge.edge);
    }

    @Override
    public int hashCode() {
      return com.google.common.base.Objects.hashCode(source, target, edge);
    }

    @Override
    public String toString() {
      return "UCAEdge{" + getSourceName() + "-- " + getEdgeString() + " ->" + getTargetName() + '}';
    }

    private String getEdgeString() {
      if (edge instanceof BlankEdge && edge.getDescription().equals("Function start dummy edge")) {
        final String funcName = edge.getSuccessor().getFunction().toString();
        int indexSemicolon = funcName.indexOf(";");
        if (indexSemicolon > 0) {
          return funcName.substring(0, indexSemicolon);
        }
        return funcName;
      }
      return edge.getRawStatement();
    }
  }
}
