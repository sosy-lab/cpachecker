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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
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
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

@Options(prefix = "assumptions")
public class UCACollector {

  public static final String NAME_OF_WITNESS_AUTOMATON = "WitnessAutomaton";
  public static final String NAME_OF_TEMP_STATE = "__qTEMP";
  public static final String NAME_OF_ERROR_STATE = "__qERROR";

  @SuppressWarnings("unused")
  public static final String NAME_OF_FINAL_STATE = "__qFINAL";

  public static final String NAME_OF_NEWTESTINPUT_STATE = "__qNEWTEST";

  @SuppressWarnings("unused")
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

  @Option(
      secure = true,
      description = "Generate uca for refinement (usable in C-CEGAR for craig interpolation)")
  private boolean genUCA4Refinement = false;

  private final LogManager logger;

  @SuppressWarnings("unused")
  private final Algorithm innerAlgorithm;

  @SuppressWarnings("unused")
  private final FormulaManagerView formulaManager;

  @SuppressWarnings("unused")
  private final CFA cfa;

  private final ConfigurableProgramAnalysis cpa;

  @SuppressWarnings("unused")
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
    } else if (genUCA4Refinement) {
      produceUCA4Interpolant(output, reached);
    }
    return this.universalConditionAutomaton;
  }

  private void produceUCA4Interpolant(Appendable output, UnmodifiableReachedSet reached)
      throws IOException, CPAException {
    final AbstractState firstState = reached.getFirstState();
    if (!(firstState instanceof ARGState) || getWitnessAutomatonState(firstState).isEmpty()) {
      throw new CPAException("Cannot dump interpolant as automaton if ARGCPA is not used.");
    }
    @Nullable PredicateCPA predicateCPA = CPAs.retrieveCPA(this.cpa, PredicateCPA.class);
    if (Objects.isNull(predicateCPA)) {
      throw new CPAException("Cannot dump interpolant as automaton if no PredicateCPA is presetn.");
    }

    // check, if the uca that should be generated (e.g. for a violation witness)
    // matches the reached set (meaning that the reached set contians at least
    // location with a property violation according to the specification

    boolean hasViolation =
        reached.stream().anyMatch(s -> s instanceof ARGState && ((ARGState) s).isTarget());
    if (genUCA4Refinement && hasViolation) {
      throw new CPAException(
          "Cannot transform the UCA, as the reached set has a property violation!");
    }

    final ARGState argRoot = (ARGState) reached.getFirstState();
    Set<UCAARGStateEdge> relevantEdges = new HashSet<>();
    // Get all states that have some invariants, because for them the invariant will be printed in
    // the UCA
    ImmutableSet<ARGState> statesWithInvariants =
        reached.asCollection().stream()
            .filter(
                s -> {
                  @Nullable PredicateAbstractState pState =
                      AbstractStates.extractStateByType(s, PredicateAbstractState.class);
                  if (pState == null) return false;
                  if (!pState.isAbstractionState()) return false;
                  // Remove all non-abstract states and abstract states with true abstraction
                  // formula
                  return !formulaManager
                      .getBooleanFormulaManager()
                      .isTrue(pState.getAbstractionFormula().asFormula());
                })
            .map(s -> AbstractStates.extractStateByType(s, ARGState.class))
            .collect(ImmutableSet.toImmutableSet());

    // scan reached set for all relevant states with are AbstractionsStates that have a non-true
    // abstraction formula
    Set<ARGState> relevantStates = new TreeSet<>();
    for (AbstractState state : reached) {
      ARGState e = (ARGState) state;
      boolean isRelevant = statesWithInvariants.contains(e);

      if (e.isCovered()) {
        e = e.getCoveringState(); // replace with covering state
        assert !e.isCovered();
      }
      if (relevantStates.contains(e)) {
        continue;
      }
      if (isRelevant) {
        Deque<ARGState> toAdd = new ArrayDeque<>();
        toAdd.add(e);
        while (!toAdd.isEmpty()) {
          ARGState current = toAdd.pop();
          assert !current.isCovered();

          if (relevantStates.add(current)) {
            // current was not yet contained in parentSet,
            // so we need to handle its parents

            toAdd.addAll(current.getParents());

            // Create a new edge:
            Optional<AbstractionFormula> assumption =
                statesWithInvariants.contains(current)
                    ? Optional.ofNullable(
                        AbstractStates.extractStateByType(current, PredicateAbstractState.class)
                            .getAbstractionFormula())
                    : Optional.empty();
            for (ARGState parent : current.getParents()) {
              // Find out which is the last edge to the ARG State
              ARGPath path = ARGUtils.getOnePathFromTo((x) -> x.equals(parent), current);

              final CFAEdge edgeToCurrent = path.getFullPath().get(path.getFullPath().size() - 1);
              relevantEdges.add(new UCAARGStateEdge(parent, current, edgeToCurrent, assumption));
              // Now, add all edges leaving the path
              for (CFAEdge edge :
                  path.getFullPath().stream()
                      .filter(e1 -> e1 instanceof AssumeEdge)
                      .collect(ImmutableList.toImmutableList())) {
                for (CFAEdge notTakenEdge :
                    CFAUtils.allLeavingEdges(edge.getPredecessor()).stream()
                        .filter(e2 -> !e2.equals(edge))
                        .collect(ImmutableList.toImmutableList())) {
                  relevantEdges.add(new UCAARGStateEdge(parent, notTakenEdge));
                }
              }
            }
            for (ARGState coveredByCurrent : current.getCoveredByThis()) {
              toAdd.addAll(coveredByCurrent.getParents());
            }
          }
        }
      }
    }

    // Assert that the all states with invariant are present
    for (ARGState state : statesWithInvariants) {
      if (relevantEdges.stream().noneMatch(edge -> edge.getSource().equals(state))) {
        CFAUtils.leavingEdges(AbstractStates.extractLocation(state))
            .forEach(e -> relevantEdges.add(new UCAARGStateEdge(state, e)));
      }
    }

    universalConditionAutomaton +=
        writeUCAForInterpolant(
            output, argRoot, relevantEdges, predicateCPA.getSolver().getFormulaManager());
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
              .collect(ImmutableSet.toImmutableSet());
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
   *
   * @param pS the state to check
   * @param pRelevantStates the other states in the graph
   * @return true, if pS has no child in pRelevantStates.
   */
  private boolean hasNoSuccessor(ARGState pS, Set<ARGState> pRelevantStates) {
    return pS.getChildren().stream().noneMatch(child -> pRelevantStates.contains(child));
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
    AutomatonState rootState = getWitnessAutomatonState(argRoot).orElseThrow();

    Set<UCAAutomatonStateEdge> edgesToAdd = new HashSet<>();

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
        Optional<AutomatonState> parentAutomatonState = getWitnessAutomatonState(parent);
        // If parent node has a automaton state and this is differnt to the one of the
        // child, add the child to statesWithNewAutomatonState
        if (parentAutomatonState.isPresent()
            && !parentAutomatonState.orElseThrow().equals(currentAutomatonState)
            && // automaton state is not already present in  parentsWithOtherAutomatonState
            parentsWithOtherAutomatonState.stream()
                .map(pair -> pair.getSecond())
                .noneMatch(state -> state.equals(parentAutomatonState.orElseThrow()))) {
          parentsWithOtherAutomatonState.add(Pair.of(parent, parentAutomatonState.orElseThrow()));
        }
      }
      if (!parentsWithOtherAutomatonState.isEmpty()) {
        for (Pair<ARGState, AutomatonState> parentPair : parentsWithOtherAutomatonState) {
          // Create the edge
          CFAEdge edge = this.getEdge(parentPair, argState);
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

    universalConditionAutomaton +=
        writeUCAForViolationWitness(output, rootState, edgesToAdd, automatonIgnoreAssumptions);
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
    if (target.isPresent() && target.orElseThrow() instanceof AutomatonState) {
      return Optional.of((AutomatonState) target.orElseThrow());
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
   * Create a String containing the assumption automaton.
   *
   * @param sb Where to write the String into.
   * @param pInitialState The initial state of the automaton.
   * @param pLastStates the set of last states that should be replaced with the
   *     NAME_OF_NEWTESTINPUT_STATE
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
        sb.append(String.format("STATE USEALL %s :\n", NAME_OF_NEWTESTINPUT_STATE));
      } else {
        sb.append("STATE USEALL ARG" + s.getStateId() + " :\n");
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
                "STATE USEALL ARG" + s.getStateId() + "M" + multiEdgeID + " :\n");
            numProducedStates++;
            descriptionForInnerMultiEdges.append("    MATCH \"");
            AssumptionCollectorAlgorithm.escape(
                innerEdge.getRawStatement(), descriptionForInnerMultiEdges);
            descriptionForInnerMultiEdges.append("\" -> ");
          }

          AssumptionStorageState assumptionChild =
              AbstractStates.extractStateByType(child, AssumptionStorageState.class);
          AssumptionCollectorAlgorithm.addAssumption(
              descriptionForInnerMultiEdges,
              assumptionChild,
              false,
              AbstractStates.extractLocation(child));
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
          AssumptionCollectorAlgorithm.addAssumption(
              sb, assumptionChild, false, AbstractStates.extractLocation(child));
          if (pLastStates.contains(child)) {
            sb.append(String.format("GOTO %s", NAME_OF_NEWTESTINPUT_STATE));
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
    sb.append("    TRUE -> GOTO __FALSE;\n\n");

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
      Appendable sb,
      AutomatonState rootState,
      Set<UCAAutomatonStateEdge> edgesToAdd,
      boolean ignoreAssumptions)
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
            .sorted(Comparator.comparing(this::getName))
            .collect(ImmutableList.toImmutableList())) {

      sb.append(String.format("STATE USEALL %s :\n", getName(currentState)));
      numProducedStates++;

      for (UCAAutomatonStateEdge edge : nodesToEdges.get(currentState)) {

        sb.append("    MATCH \"");
        AssumptionCollectorAlgorithm.escape(getEdgeString(edge.getEdge()), sb);
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

  /**
   * Create an UCA for the given set of edges Beneth printing the edges, each node gets a self-loop
   * and a node to the temp-location
   *
   * @param sb the appendable to print to
   * @param rootState the root state of the automaton
   * @param edgesToAdd the edges between states to add
   * @param pFmgr the formula manager neeeded to print the assumptions
   * @throws IOException if the file cannot be accessed or does not exist
   */
  private int writeUCAForInterpolant(
      Appendable sb, ARGState rootState, Set<UCAARGStateEdge> edgesToAdd, FormulaManagerView pFmgr)
      throws IOException {
    int numProducedStates = 0;
    sb.append("OBSERVER AUTOMATON AssumptionAutomaton\n\n");

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
    sb.append(String.format("    TRUE -> GOTO %s;\n\n", NAME_OF_TEMP_STATE));

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
            .sorted(Comparator.comparing(this::getName))
            .collect(ImmutableList.toImmutableList())) {

      sb.append(String.format("STATE USEALL %s :\n", getName(currentState)));
      numProducedStates++;

      for (UCAARGStateEdge edge : nodesToEdges.get(currentState)) {

        sb.append("    MATCH \"");
        AssumptionCollectorAlgorithm.escape(edge.getEdge().getRawStatement(), sb);
        sb.append("\" -> ");
        sb.append(edge.getStringOfAssumption(pFmgr));
        sb.append(String.format("GOTO %s", edge.getTargetName()));
        sb.append(";\n");
      }
      sb.append("\n");
      // Self loops are not needed, as only a single path is descriebed!

    }
    sb.append("END AUTOMATON\n");

    return numProducedStates;
  }

  private String getName(AutomatonState s) {
    return s.isTarget() ? NAME_OF_ERROR_STATE : s.getInternalStateName();
  }

  private class UCAAutomatonStateEdge {
    private final AutomatonState source;
    private final Optional<AutomatonState> target;
    private final CFAEdge edge;

    public UCAAutomatonStateEdge(AutomatonState pSource, AutomatonState pTarget, CFAEdge pEdge) {
      this.source = pSource;
      this.target = Optional.of(pTarget);
      this.edge = pEdge;
    }

    public UCAAutomatonStateEdge(AutomatonState pSource, CFAEdge pEdge) {
      this.source = pSource;
      this.target = Optional.empty();
      this.edge = pEdge;
    }

    public String getSourceName() {
      return getName(source);
    }

    public String getTargetName() {
      return this.target.isPresent() ? getName(target.orElseThrow()) : NAME_OF_TEMP_STATE;
    }

    public AutomatonState getSource() {
      return source;
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (!(pO instanceof UCAAutomatonStateEdge)) {
        return false;
      }
      UCAAutomatonStateEdge ucaEdge = (UCAAutomatonStateEdge) pO;
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
          + getEdgeString(edge)
          + " ->"
          + getTargetName()
          + '}';
    }

    public CFAEdge getEdge() {
      return edge;
    }
  }

  private class UCAARGStateEdge {
    private final ARGState source;
    private final Optional<ARGState> target;
    private final CFAEdge edge;
    private final Optional<AbstractionFormula> assumption;

    public UCAARGStateEdge(
        ARGState pSource,
        ARGState pTarget,
        CFAEdge pEdge,
        Optional<AbstractionFormula> pAssumption) {
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
      return getName(source);
    }

    public String getTargetName() {
      return this.target.isPresent() ? getName(target.orElseThrow()) : NAME_OF_TEMP_STATE;
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
          + getEdgeString(edge)
          + " ->"
          + getTargetName()
          + '}';
    }

    public CFAEdge getEdge() {
      return edge;
    }
  }

  private String getName(ARGState pSource) {
    return String.format("ARG%d", +pSource.getStateId());
  }

  private String getEdgeString(CFAEdge pEdge) {
    if (pEdge instanceof BlankEdge && pEdge.getDescription().equals("Function start dummy edge")) {
      final String funcName = pEdge.getSuccessor().getFunction().toString();
      int indexSemicolon = funcName.indexOf(";");
      if (indexSemicolon > 0) {
        return funcName.substring(0, indexSemicolon);
      }
      return funcName;
    }
    return pEdge.getRawStatement();
  }
}
