// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.giageneration;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.arg.ARGUtils.getUncoveredChildrenView;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class GIATestcaseGenerator {

  private final ConfigurableProgramAnalysis cpa;

  public GIATestcaseGenerator(ConfigurableProgramAnalysis pCpa) {
    this.cpa = pCpa;
  }

  int produceGIA4Testcase(
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

    // check, if the GIA that should be generated (e.g. for a violation witness)
    // matches the reached set (meaning that the reached set contians at least
    // location with a property violation according to the specification

    Set<AbstractState> falseAssumptionStates =
        AssumptionCollectorAlgorithm.getFalseAssumptionStates(reached, true, this.cpa);

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
          e.isTarget()
              || (Objects.nonNull(asmptState) && asmptState.isStop())
              || pExceptionStates.contains(e.getStateId());

      boolean isRelevant = Objects.nonNull(asmptState) && !asmptState.isAssumptionTrue();

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
              .filter(s -> GIAGenerator.hasNoSuccessor(s, relevantStates))
              .collect(ImmutableSet.toImmutableSet());
    } else {
      lastStates = Sets.newHashSet((ARGState) reached.getLastState());
    }

    falseAssumptionStates.addAll(lastStates);
    assert firstState instanceof ARGState;
    return writeGIAForTestcase(
        output, (ARGState) firstState, lastStates, relevantStates, falseAssumptionStates);
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
  private static int writeGIAForTestcase(
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
    sb.append(GIAGenerator.AUTOMATON_HEADER);

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
        sb.append(String.format("STATE USEALL %s :%n", GIAGenerator.NAME_OF_NEWTESTINPUT_STATE));
      } else {
        sb.append("STATE USEALL ARG").append(String.valueOf(s.getStateId())).append(" :\n");
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
          sb.append("GOTO ARG")
              .append(String.valueOf(s.getStateId()))
              .append("M")
              .append(String.valueOf(multiEdgeID));

          boolean first = true;
          for (CFAEdge innerEdge : from(edges).skip(1)) {

            if (!first) {
              multiEdgeID++;
              descriptionForInnerMultiEdges
                  .append("GOTO ARG")
                  .append(s.getStateId())
                  .append("M")
                  .append(multiEdgeID)
                  .append(";\n\n");
            } else {
              first = false;
            }

            descriptionForInnerMultiEdges
                .append("STATE USEALL ARG")
                .append(s.getStateId())
                .append("M")
                .append(multiEdgeID)
                .append(" :\n");
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
            sb.append(String.format("GOTO %s", GIAGenerator.NAME_OF_NEWTESTINPUT_STATE));
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
    sb.append(String.format("STATE %s :%n", GIAGenerator.NAME_OF_NEWTESTINPUT_STATE));
    sb.append("    TRUE -> GOTO __FALSE;\n\n");

    sb.append("END AUTOMATON\n");

    return numProducedStates;
  }
}
