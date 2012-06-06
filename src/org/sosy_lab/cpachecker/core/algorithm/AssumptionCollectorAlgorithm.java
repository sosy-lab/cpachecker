/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.Path;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.assumptions.AssumptionWithLocation;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * Outer algorithm to collect all invariants generated during
 * the analysis, and report them to the user
 */
@Options(prefix="assumptions")
public class AssumptionCollectorAlgorithm implements Algorithm, StatisticsProvider {

  private class AssumptionCollectionStatistics implements Statistics {
    @Override
    public String getName() {
      return "Assumption Collection algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,
        ReachedSet pReached) {

      AssumptionWithLocation resultAssumption = collectLocationAssumptions(pReached, exceptionAssumptions);

      out.println("Number of locations with assumptions: " + resultAssumption.getNumberOfLocations());

      if (exportAssumptions) {
        if (assumptionsFile != null) {
          try {
            Files.writeFile(assumptionsFile, resultAssumption);
          } catch (IOException e) {
            logger.logUserException(Level.WARNING, e, "Could not write assumptions to file");
          }
        }

        if (assumptionAutomatonFile != null) {
          try {
            Files.writeFile(assumptionAutomatonFile, produceAssumptionAutomaton(pReached));
          } catch (IOException e) {
            logger.logUserException(Level.WARNING, e, "Could not write assumptions to file");
          }
          out.println("Number of states in automaton:        " + automatonStates);
        }
      }
    }
  }

  // statistics
  private int automatonStates = 0;


  @Option(name="export", description="write collected assumptions to file")
  private boolean exportAssumptions = true;

  @Option(name="file",
      description="write collected assumptions to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
      private File assumptionsFile = new File("assumptions.txt");

  @Option(name="automatonFile",
      description="write collected assumptions as automaton to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
      private File assumptionAutomatonFile = new File("AssumptionAutomaton.txt");

  @Option(description="Add a threshold to the automaton, "
      + "after so many branches on a path the automaton will be ignored (0 to disable)")
  @IntegerOption(min=0)
  private int automatonBranchingThreshold = 0;

  private final LogManager logger;
  private final Algorithm innerAlgorithm;
  private final FormulaManager formulaManager;
  private final AssumptionWithLocation exceptionAssumptions;
  private final AssumptionStorageCPA cpa;

  // store only the ids, not the elements in order to prevent memory leaks
  private final Set<Integer> exceptionElements = new HashSet<Integer>();

  public AssumptionCollectorAlgorithm(Algorithm algo, ConfigurableProgramAnalysis pCpa, Configuration config, LogManager logger) throws InvalidConfigurationException
  {
    config.inject(this);

    this.logger = logger;
    innerAlgorithm = algo;
    cpa = ((WrapperCPA)pCpa).retrieveWrappedCpa(AssumptionStorageCPA.class);
    if (cpa == null) {
      throw new InvalidConfigurationException("AssumptionStorageCPA needed for AssumptionCollectionAlgorithm");
    }
    formulaManager = cpa.getFormulaManager();
    exceptionAssumptions = new AssumptionWithLocation(formulaManager);
  }

  @Override
  public boolean run(ReachedSet reached) throws CPAException, InterruptedException {
    boolean sound = true;

    boolean restartCPA = false;

    // loop if restartCPA is set to false
    do {
      restartCPA = false;
      try {
        // run the inner algorithm to fill the reached set
        sound &= innerAlgorithm.run(reached);
      } catch (RefinementFailedException failedRefinement) {
        logger.log(Level.FINER, "Dumping assumptions due to:", failedRefinement);

        Path path = failedRefinement.getErrorPath();
        ARGState errorElement = path.getLast().getFirst();
        assert errorElement == reached.getLastState();

        // old code, perhaps we can use the information from getFailurePoint()
        //        int pos = failedRefinement.getFailurePoint();
        //
        //        if (pos == -1)
        //          pos = path.size() - 2; // the node before the error node
        //
        //        ARGState element = path.get(pos).getFirst();
        //        addAvoidingAssumptions(exceptionAssumptions, element);
        //        exceptionElements.add(element.getElementId());

        // remove element
        // remove it's parents from waitlist (CPAAlgorithm re-added them)
        // and create assumptions for the parents

        // we have to do this for the parents and not for the errorElement itself,
        // because the parents might have other potential successors that were
        // ignored by CPAAlgorithm due to the signaled break

        ARGState parent = Iterables.getOnlyElement(errorElement.getParents());
        reached.removeOnlyFromWaitlist(parent);
        exceptionElements.add(parent.getStateId());
        addAvoidingAssumptions(exceptionAssumptions, parent);

        reached.remove(errorElement);
        errorElement.removeFromART();

        restartCPA = true;

        sound = false;

      } catch (CPAException e) {
        // TODO is it really wise to swallow exceptions here?
        logger.log(Level.FINER, "Dumping assumptions due to: " + e.toString());
      }
    } while (restartCPA);

    return sound;
  }

  private AssumptionWithLocation collectLocationAssumptions(ReachedSet reached, AssumptionWithLocation exceptionAssumptions) {
    AssumptionWithLocation result = AssumptionWithLocation.copyOf(exceptionAssumptions);

    // collect and dump all assumptions stored in abstract states
    logger.log(Level.FINER, "Dumping assumptions resulting from tool assumptions");
    for (AbstractState element : reached) {

      if (AbstractStates.isTargetState(element)) {
        // create assumptions for target element
        addAvoidingAssumptions(result, element);

      } else {
        // get stored assumption

        AssumptionStorageState e = AbstractStates.extractStateByType(element, AssumptionStorageState.class);

        Formula assumption = formulaManager.makeAnd(e.getAssumption(), e.getStopFormula());

        if (!assumption.isTrue()) {
          addAssumption(result, assumption, element);
        }
      }
    }

    // dump invariants to prevent going further with nodes in the waitlist
    logger.log(Level.FINER, "Dumping assumptions resulting from waitlist elements");
    for (AbstractState element : reached.getWaitlist()) {
      addAvoidingAssumptions(result, element);
    }

    return result;
  }

  /**
   * Add a given assumption for the location and state of an element.
   */
  private void addAssumption(AssumptionWithLocation invariant, Formula assumption, AbstractState state) {
    Formula dataRegion = AbstractStates.extractReportedFormulas(formulaManager, state);

    CFANode loc = extractLocation(state);
    assert loc != null;
    invariant.add(loc, formulaManager.makeOr(assumption, formulaManager.makeNot(dataRegion)));
  }

  /**
   * Create an assumption that is sufficient to exclude an abstract state
   */
  private void addAvoidingAssumptions(AssumptionWithLocation invariant, AbstractState element) {
    addAssumption(invariant, formulaManager.makeFalse(), element);
  }

  private String produceAssumptionAutomaton(ReachedSet reached) {
    AbstractState firstElement = reached.getFirstState();
    if (!(firstElement instanceof ARGState)) {
      return "Cannot dump assumption as automaton if ARGCPA is not used.";
    }

    Set<AbstractState> falseAssumptionElements = Sets.newHashSet(reached.getWaitlist());

    // scan reached set for all relevant elements with an assumption
    Set<ARGState> relevantElements = new HashSet<ARGState>();
    for (AbstractState element : reached) {
      ARGState e = (ARGState)element;
      AssumptionStorageState asmptElement = AbstractStates.extractStateByType(e, AssumptionStorageState.class);

      if (e.isTarget()
          || asmptElement.isStop()
          || exceptionElements.contains(e.getStateId())) {
        falseAssumptionElements.add(e);
      }

      if (relevantElements.contains(e)) {
        continue;
      }

      if (!asmptElement.getAssumption().isTrue()
          || falseAssumptionElements.contains(e)) {

        // now add e and all its transitive parents to the relevantElements set
        findAllParents(e, relevantElements);
      }
    }

    ARGState rootElement = (ARGState)reached.getFirstState();

    Set<ARGState> childrenOfRelevantElements = new TreeSet<ARGState>(relevantElements);
    childrenOfRelevantElements.add(rootElement);
    for (ARGState e : relevantElements) {
      childrenOfRelevantElements.addAll(e.getChildren());
      if (e.isCovered()) {
        childrenOfRelevantElements.add(e.getCoveringState());
      }
    }

    return writeAutomaton(rootElement, childrenOfRelevantElements, relevantElements, falseAssumptionElements);
  }

  /**
   * Create a String containing the assumption automaton.
   * @param initialState The initial element of the automaton.
   * @param allElements A set with all elements which should appear as states in the automaton.
   * @param relevantElements A set with all elements with non-trivial assumptions (all others and their children will have assumption TRUE).
   * @param falseAssumptionElements A set with all elements with the assumption FALSE
   */
  private String writeAutomaton(ARGState initialState, Set<ARGState> allElements,
      Set<ARGState> relevantElements, Set<AbstractState> falseAssumptionElements) {
    StringBuilder sb = new StringBuilder();
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
    sb.append("    TRUE -> ASSUME \"true\" GOTO __TRUE;\n\n");

    if (!falseAssumptionElements.isEmpty()) {
      sb.append("STATE __FALSE :\n");
      sb.append("    TRUE -> ASSUME \"false\" GOTO __FALSE;\n\n");
    }

    for (ARGState e : allElements) {
      if (e.isCovered() || falseAssumptionElements.contains(e)) {
        continue;
      }

      sb.append("STATE USEFIRST ARG" + e.getStateId() + " :\n");
      automatonStates++;

      if (!relevantElements.contains(e)) {
        sb.append("   TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");

      } else {
        boolean branching = false;
        if ((automatonBranchingThreshold > 0) && (e.getChildren().size() > 1)) {
          branching = true;
          sb.append("    branchingCount == branchingThreshold -> " + actionOnFinalEdges + "GOTO __FALSE;\n");
        }

        CFANode loc = AbstractStates.extractLocation(e);
        for (ARGState child : e.getChildren()) {
          if (child.isCovered()) {
            child = child.getCoveringState();
            assert !child.isCovered();
          }

          CFANode childLoc = AbstractStates.extractLocation(child);
          CFAEdge edge = loc.getEdgeTo(childLoc);
          sb.append("    MATCH \"");
          escape(edge.getRawStatement(), sb);
          sb.append("\" -> ");

          AssumptionStorageState assumptionChild = AbstractStates.extractStateByType(child, AssumptionStorageState.class);
          Formula assumption = formulaManager.makeAnd(assumptionChild.getAssumption(), assumptionChild.getStopFormula());
          sb.append("ASSUME \"");
          escape(assumption.toString(), sb);
          sb.append("\" ");

          if (branching) {
            sb.append("DO branchingCount = branchingCount+1 ");
          }

          if (falseAssumptionElements.contains(child)) {
            sb.append(actionOnFinalEdges + "GOTO __FALSE");
          } else {
            sb.append("GOTO ARG" + child.getStateId());
          }
          sb.append(";\n");
        }
        sb.append("    TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");
      }
    }
    sb.append("END AUTOMATON\n");
    return sb.toString();
  }

  /**
   * This method transitively finds all parents of a given element and adds
   * them to a given set.
   * Covering nodes are considered to be parents of the covered nodes.
   * @param e
   * @param parentSet
   */
  private void findAllParents(ARGState e, Set<ARGState> parentSet) {
    Deque<ARGState> toAdd = new ArrayDeque<ARGState>();
    toAdd.add(e);
    while (!toAdd.isEmpty()) {
      ARGState current = toAdd.pop();

      if (parentSet.add(current)) {
        // current was not yet contained in parentSet,
        // so we need to handle its parents

        toAdd.addAll(current.getParents());
        toAdd.addAll(current.getCoveredByThis());
      }
    }
  }

  private static void escape(String s, StringBuilder appendTo) {
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
}
