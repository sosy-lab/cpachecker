/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm;

import static org.sosy_lab.cpachecker.cpa.arg.ARGUtils.getUncoveredChildrenView;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.assumptions.AssumptionWithLocation;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * Outer algorithm to collect all invariants generated during
 * the analysis, and report them to the user
 */
@Options(prefix="assumptions")
public class AssumptionCollectorAlgorithm implements Algorithm, StatisticsProvider {

  @Option(name="export", description="write collected assumptions to file")
  private boolean exportAssumptions = true;

  @Option(name="file", description="write collected assumptions to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path assumptionsFile = Paths.get("assumptions.txt");

  @Option(name="automatonFile", description="write collected assumptions as automaton to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path assumptionAutomatonFile = Paths.get("AssumptionAutomaton.txt");

  @Option(description="Add a threshold to the automaton, after so many branches on a path the automaton will be ignored (0 to disable)")
  @IntegerOption(min=0)
  private int automatonBranchingThreshold = 0;

  private final LogManager logger;
  private final Algorithm innerAlgorithm;
  private final FormulaManagerView formulaManager;
  private final AssumptionWithLocation exceptionAssumptions;
  private final AssumptionStorageCPA cpa;
  private final BooleanFormulaManager bfmgr;

  // store only the ids, not the states in order to prevent memory leaks
  private final Set<Integer> exceptionStates = new HashSet<>();

  // statistics
  private int automatonStates = 0;

  public AssumptionCollectorAlgorithm(Algorithm algo, ConfigurableProgramAnalysis pCpa, Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);

    this.logger = logger;
    this.innerAlgorithm = algo;
    this.cpa = ((WrapperCPA)pCpa).retrieveWrappedCpa(AssumptionStorageCPA.class);
    if (this.cpa == null) {
      throw new InvalidConfigurationException("AssumptionStorageCPA needed for AssumptionCollectionAlgorithm");
    }
    this.formulaManager = cpa.getFormulaManager();
    this.bfmgr = formulaManager.getBooleanFormulaManager();
    this.exceptionAssumptions = new AssumptionWithLocation(formulaManager);
  }

  @Override
  public boolean run(ReachedSet reached) throws CPAException, InterruptedException {
    boolean isComplete = true;
    boolean restartCPA = false;

    // loop if restartCPA is set to false
    do {
      restartCPA = false;
      try {
        // run the inner algorithm to fill the reached set
        isComplete &= innerAlgorithm.run(reached);

      } catch (RefinementFailedException failedRefinement) {
        logger.log(Level.FINER, "Dumping assumptions due to:", failedRefinement);

        ARGPath path = failedRefinement.getErrorPath();
        ARGState errorState = path.getLast().getFirst();
        assert errorState == reached.getLastState();

        // old code, perhaps we can use the information from getFailurePoint()
        //        int pos = failedRefinement.getFailurePoint();
        //
        //        if (pos == -1)
        //          pos = path.size() - 2; // the node before the error node
        //
        //        ARGState state = path.get(pos).getFirst();
        //        addAvoidingAssumptions(exceptionAssumptions, state);
        //        exceptionStates.add(state.getStatetId());

        // remove state
        // remove it's parents from waitlist (CPAAlgorithm re-added them)
        // and create assumptions for the parents

        // we have to do this for the parents and not for the errorState itself,
        // because the parents might have other potential successors that were
        // ignored by CPAAlgorithm due to the signaled break

        ARGState parent = Iterables.getOnlyElement(errorState.getParents());
        reached.removeOnlyFromWaitlist(parent);
        exceptionStates.add(parent.getStateId());
        addAvoidingAssumptions(exceptionAssumptions, parent);

        reached.remove(errorState);
        errorState.removeFromARG();

        restartCPA = true;
        isComplete = false;

        // TODO: handle CounterexampleAnalysisFailed similar to RefinementFailedException
        // TODO: handle other kinds of CPAException?

//      } catch (CPAException e) {
//        // TODO is it really wise to swallow exceptions here?
//        logger.log(Level.FINER, "Dumping assumptions due to: " + e.toString());
      }
    } while (restartCPA);

    return isComplete;
  }

  private AssumptionWithLocation collectLocationAssumptions(ReachedSet reached, AssumptionWithLocation exceptionAssumptions) {
    AssumptionWithLocation result = AssumptionWithLocation.copyOf(exceptionAssumptions);

    // collect and dump all assumptions stored in abstract states
    logger.log(Level.FINER, "Dumping assumptions resulting from tool assumptions");
    for (AbstractState state : reached) {

      if (AbstractStates.isTargetState(state)) {
        // create assumptions for target state
        addAvoidingAssumptions(result, state);

      } else {
        // get stored assumption

        AssumptionStorageState e = AbstractStates.extractStateByType(state, AssumptionStorageState.class);
        BooleanFormula assumption = bfmgr.and(e.getAssumption(), e.getStopFormula());

        if (!bfmgr.isTrue(assumption)) {
          addAssumption(result, assumption, state);
        }
      }
    }

    // dump invariants to prevent going further with nodes in the waitlist
    logger.log(Level.FINER, "Dumping assumptions resulting from waitlist states");
    for (AbstractState state : reached.getWaitlist()) {
      addAvoidingAssumptions(result, state);
    }

    return result;
  }

  /**
   * Add a given assumption for the location and state of a state.
   */
  private void addAssumption(AssumptionWithLocation invariant, BooleanFormula assumption, AbstractState state) {
    BooleanFormula dataRegion = AbstractStates.extractReportedFormulas(formulaManager, state);

    CFANode loc = extractLocation(state);
    assert loc != null;
    invariant.add(loc, bfmgr.or(assumption, bfmgr.not(dataRegion)));
  }

  /**
   * Create an assumption that is sufficient to exclude an abstract state
   */
  private void addAvoidingAssumptions(AssumptionWithLocation invariant, AbstractState state) {
    addAssumption(invariant, bfmgr.makeBoolean(false), state);
  }

  private void produceAssumptionAutomaton(Appendable output, ReachedSet reached) throws IOException {
    final AbstractState firstState = reached.getFirstState();
    if (!(firstState instanceof ARGState)) {
      output.append("Cannot dump assumption as automaton if ARGCPA is not used.");
    }

    Set<AbstractState> falseAssumptionStates = Sets.newHashSet(reached.getWaitlist());

    // scan reached set for all relevant states with an assumption
    // Invariant: relevantStates does not contain any covered state.
    // A covered state is always replaced by its covering state.
    Set<ARGState> relevantStates = new TreeSet<>();
    for (AbstractState state : reached) {
      ARGState e = (ARGState)state;
      AssumptionStorageState asmptState = AbstractStates.extractStateByType(e, AssumptionStorageState.class);

      boolean hasFalseAssumption =
             e.isTarget()
          || asmptState.isStop()
          || exceptionStates.contains(e.getStateId());

      boolean isRelevant = !asmptState.isAssumptionTrue();

      if (e.isCovered()) {
        e = e.getCoveringState(); // replace with covering state
        assert !e.isCovered();
        asmptState = null; // just to prevent accidental misuse
      }

      if (hasFalseAssumption) {
        falseAssumptionStates.add(e);
      }

      if (relevantStates.contains(e)) {
        continue;
      }

      if (isRelevant || falseAssumptionStates.contains(e)) {
        // now add e and all its transitive parents to the relevantStates set
        findAllParents(e, relevantStates);
      }
    }

    writeAutomaton(output, (ARGState)firstState, relevantStates, falseAssumptionStates);
  }

  /**
   * Create a String containing the assumption automaton.
   * @param sb Where to write the String into.
   * @param initialState The initial state of the automaton.
   * @param relevantStates A set with all states with non-trivial assumptions (all others will have assumption TRUE).
   * @param falseAssumptionStates A set with all states with the assumption FALSE
   * @throws IOException
   */
  private void writeAutomaton(Appendable sb, ARGState initialState,
      Set<ARGState> relevantStates, Set<AbstractState> falseAssumptionStates) throws IOException {
    sb.append("OBSERVER AUTOMATON AssumptionAutomaton\n\n");

    String actionOnFinalEdges = "";
    if (automatonBranchingThreshold > 0) {
      sb.append("LOCAL int branchingThreshold = " + automatonBranchingThreshold + ";\n");
      sb.append("LOCAL int branchingCount = 0;\n\n");

      // Reset automaton variable on all edges like "GOTO __FALSE"
      // to allow merging of states.
      actionOnFinalEdges = "DO branchingCount = 0 ";
    }

    sb.append("INITIAL STATE ARG" + initialState.getStateId() + ";\n\n");
    sb.append("STATE __TRUE :\n");
    sb.append("    TRUE -> ASSUME {true} GOTO __TRUE;\n\n");

    if (!falseAssumptionStates.isEmpty()) {
      sb.append("STATE __FALSE :\n");
      sb.append("    TRUE -> ASSUME {false} GOTO __FALSE;\n\n");
    }

    for (final ARGState s : relevantStates) {
      assert !s.isCovered();

      if (falseAssumptionStates.contains(s)) {
        continue;
      }

      sb.append("STATE USEFIRST ARG" + s.getStateId() + " :\n");
      automatonStates++;

      boolean branching = false;
      if ((automatonBranchingThreshold > 0) && (s.getChildren().size() > 1)) {
        branching = true;
        sb.append("    branchingCount == branchingThreshold -> " + actionOnFinalEdges + "GOTO __FALSE;\n");
      }

      final CFANode loc = AbstractStates.extractLocation(s);
      for (final ARGState child : getUncoveredChildrenView(s)) {
        assert !child.isCovered();

        CFAEdge edge = loc.getEdgeTo(extractLocation(child));
        sb.append("    MATCH \"");
        escape(edge.getRawStatement(), sb);
        sb.append("\" -> ");

        AssumptionStorageState assumptionChild = AbstractStates.extractStateByType(child, AssumptionStorageState.class);
        BooleanFormula assumption = bfmgr.and(assumptionChild.getAssumption(), assumptionChild.getStopFormula());
        sb.append("ASSUME {");
        escape(assumption.toString(), sb);
        sb.append("} ");

        if (falseAssumptionStates.contains(child)) {
          sb.append(actionOnFinalEdges + "GOTO __FALSE");

        } else if (relevantStates.contains(child)) {
          if (branching) {
            sb.append("DO branchingCount = branchingCount+1 ");
          }
          sb.append("GOTO ARG" + child.getStateId());

        } else {
          sb.append(actionOnFinalEdges + "GOTO __TRUE");
        }

        sb.append(";\n");
      }
      sb.append("    TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");

    }
    sb.append("END AUTOMATON\n");
  }

  /**
   * This method transitively finds all parents of a given state and adds
   * them to a given set.
   * Covering nodes are considered to be parents of the covered nodes.
   * @param s
   * @param parentSet
   */
  private void findAllParents(ARGState s, Set<ARGState> parentSet) {
    Deque<ARGState> toAdd = new ArrayDeque<>();
    toAdd.add(s);
    while (!toAdd.isEmpty()) {
      ARGState current = toAdd.pop();
      assert !current.isCovered();

      if (parentSet.add(current)) {
        // current was not yet contained in parentSet,
        // so we need to handle its parents

        toAdd.addAll(current.getParents());

        for (ARGState coveredByCurrent : current.getCoveredByThis()) {
          toAdd.addAll(coveredByCurrent.getParents());
        }
      }
    }
  }

  private static void escape(String s, Appendable appendTo) throws IOException {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
      case '\n':
        appendTo.append("\\n");
        break;
      case '\"':
        appendTo.append("\\\"");
        break;
      case '\\':
        appendTo.append("\\\\");
        break;
      case '`':
        break;
      default:
        appendTo.append(c);
        break;
      }
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (innerAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)innerAlgorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(new AssumptionCollectionStatistics());
  }

  private class AssumptionCollectionStatistics extends AbstractStatistics {
    @Override
    public String getName() {
      return "Assumption Collection algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      AssumptionWithLocation resultAssumption = collectLocationAssumptions(pReached, exceptionAssumptions);

      put(out, "Number of locations with assumptions", resultAssumption.getNumberOfLocations());

      if (exportAssumptions) {
        if (assumptionsFile != null) {
          try {
            Files.writeFile(assumptionsFile, resultAssumption);
          } catch (IOException e) {
            logger.logUserException(Level.WARNING, e, "Could not write assumptions to file");
          }
        }

        if (assumptionAutomatonFile != null) {
          try (Writer w = Files.openOutputFile(assumptionAutomatonFile)) {
            produceAssumptionAutomaton(w, pReached);
          } catch (IOException e) {
            logger.logUserException(Level.WARNING, e, "Could not write assumptions to file");
          }
          put(out, "Number of states in automaton", automatonStates);
        }
      }
    }
  }
}
