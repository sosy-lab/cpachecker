/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractElements.filterTargetElements;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.assumptions.AssumptionWithLocation;
import org.sosy_lab.cpachecker.util.assumptions.ReportingUtils;
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
            logger.log(Level.WARNING,
                "Could not write assumptions to file ", assumptionsFile.getAbsolutePath(),
                ", (", e.getMessage(), ")");
          }
        }

        if (assumptionAutomatonFile != null) {
          try {
            Files.writeFile(assumptionAutomatonFile, produceAssumptionAutomaton(pReached));
          } catch (IOException e) {
            logger.log(Level.WARNING,
                "Could not write assumptions to file ", assumptionAutomatonFile.getAbsolutePath(),
                ", (", e.getMessage(), ")");
          }
        }
      }
    }
  }

  @Option(name="export", description="write collected assumptions to file")
  private boolean exportAssumptions = true;

  @Option(name="file", type=Option.Type.OUTPUT_FILE,
      description="write collected assumptions to file")
      private File assumptionsFile = new File("assumptions.txt");

  @Option(name="automatonFile", type=Option.Type.OUTPUT_FILE,
      description="write collected assumptions as automaton to file")
      private File assumptionAutomatonFile = new File("AssumptionAutomaton.txt");

  private final LogManager logger;
  private final Algorithm innerAlgorithm;
  private final FormulaManager formulaManager;
  private final AssumptionWithLocation exceptionAssumptions;
  private final AssumptionStorageCPA cpa;

  // store only the ids, not the elements in order to prevent memory leaks
  private final Set<Integer> exceptionElements = new HashSet<Integer>();

  public AssumptionCollectorAlgorithm(Algorithm algo, Configuration config, LogManager logger) throws InvalidConfigurationException
  {
    config.inject(this);

    this.logger = logger;
    innerAlgorithm = algo;
    cpa = ((WrapperCPA)getCPA()).retrieveWrappedCpa(AssumptionStorageCPA.class);
    if (cpa == null) {
      throw new InvalidConfigurationException("AssumptionStorageCPA needed for AssumptionCollectionAlgorithm");
    }
    formulaManager = cpa.getFormulaManager();
    exceptionAssumptions = new AssumptionWithLocation(formulaManager);
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return innerAlgorithm.getCPA();
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
        ARTElement errorElement = path.getLast().getFirst();
        assert errorElement == reached.getLastElement();

        // old code, perhaps we can use the information from getFailurePoint()
        //        int pos = failedRefinement.getFailurePoint();
        //
        //        if (pos == -1)
        //          pos = path.size() - 2; // the node before the error node
        //
        //        ARTElement element = path.get(pos).getFirst();
        //        addAvoidingAssumptions(exceptionAssumptions, element);
        //        exceptionElements.add(element.getElementId());

        // remove element
        // remove it's parents from waitlist (CPAAlgorithm re-added them)
        // and create assumptions for the parents

        // we have to do this for the parents and not for the errorElement itself,
        // because the parents might have other potential successors that were
        // ignored by CPAAlgorithm due to the signaled break

        ARTElement parent = Iterables.getOnlyElement(errorElement.getLocalParents());
        reached.removeOnlyFromWaitlist(parent);
        exceptionElements.add(parent.getElementId());
        addAvoidingAssumptions(exceptionAssumptions, parent);

        reached.remove(errorElement);
        errorElement.removeFromART();

        restartCPA = true;

        if(failedRefinement.doesItHurtSoundness()) sound = false;

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
    for (AbstractElement element : reached) {
      AssumptionStorageElement e = AbstractElements.extractElementByType(element, AssumptionStorageElement.class);

      Formula assumption = formulaManager.makeAnd(e.getAssumption(), e.getStopFormula());

      addAssumption(result, assumption, element);
    }

    // create assumptions for target elements
    logger.log(Level.FINER, "Dumping assumptions resulting from target elements");
    for (AbstractElement element : filterTargetElements(reached)) {
      addAvoidingAssumptions(result, element);
    }

    // dump invariants to prevent going further with nodes in the waitlist
    logger.log(Level.FINER, "Dumping assumptions resulting from waitlist elements");
    for (AbstractElement element : reached.getWaitlist()) {
      addAvoidingAssumptions(result, element);
    }

    return result;
  }

  /**
   * Add a given assumption for the location and state of an element.
   */
  private void addAssumption(AssumptionWithLocation invariant, Formula assumption, AbstractElement state) {
    Formula dataRegion = ReportingUtils.extractReportedFormulas(formulaManager, state);

    CFANode loc = extractLocation(state);
    assert loc != null;
    invariant.add(loc, formulaManager.makeOr(assumption, formulaManager.makeNot(dataRegion)));
  }

  /**
   * Create an assumption that is sufficient to exclude an abstract state
   */
  private void addAvoidingAssumptions(AssumptionWithLocation invariant, AbstractElement element) {
    addAssumption(invariant, formulaManager.makeFalse(), element);
  }

  private String produceAssumptionAutomaton(ReachedSet reached) {
    AbstractElement firstElement = reached.getFirstElement();
    if (!(firstElement instanceof ARTElement)) {
      return "Cannot dump assumption as automaton if ARTCPA is not used.";
    }

    Set<ARTElement> artNodes = new HashSet<ARTElement>();

    Set<AbstractElement> falseAssumptions = Sets.newHashSet(reached.getWaitlist());
    Iterables.addAll(falseAssumptions, filterTargetElements(reached));

    if (!exceptionElements.isEmpty()) {
      for (AbstractElement element : reached) {
        if (exceptionElements.contains(((ARTElement)element).getElementId())) {
          falseAssumptions.add(element);
        }
      }
    }

    Set<ARTElement> trueAssumptions = new HashSet<ARTElement>();
    getTrueAssumptionElements((ARTElement)firstElement, artNodes, trueAssumptions, falseAssumptions);

    StringBuilder sb = new StringBuilder();
    sb.append("OBSERVER AUTOMATON AssumptionAutomaton\n\n");
    sb.append("INITIAL STATE ART" + ((ARTElement)reached.getFirstElement()).getElementId() + ";\n\n");
    sb.append("STATE __TRUE :\n");
    sb.append("    TRUE -> ASSUME \"true\" GOTO __TRUE;\n\n");
    sb.append("STATE __FALSE :\n");
    sb.append("    TRUE -> ASSUME \"false\" GOTO __FALSE;\n\n");

    for (ARTElement e : artNodes) {
      if (e.isCovered() || falseAssumptions.contains(e) ||
          (!e.getLocalParents().isEmpty() && trueAssumptions.containsAll(e.getLocalParents()))){
        continue;
      }

      CFANode loc = AbstractElements.extractLocation(e);
      sb.append("STATE USEFIRST ART" + e.getElementId() + " :\n");
      if (trueAssumptions.contains(e)) {
        sb.append("   TRUE -> GOTO __TRUE;\n\n");

      } else {
        for (ARTElement child : e.getLocalChildren()) {
          if (child.isCovered()) {
            child = child.getCoveringElement();
            assert !child.isCovered();
          }

          if (artNodes.contains(child)) {
            CFANode childLoc = AbstractElements.extractLocation(child);
            CFAEdge edge = loc.getEdgeTo(childLoc);
            sb.append("    MATCH \"");
            escape(edge.getRawStatement(), sb);
            sb.append("\" -> ");

            AssumptionStorageElement assumptionChild = AbstractElements.extractElementByType(child, AssumptionStorageElement.class);
            Formula assumption = formulaManager.makeAnd(assumptionChild.getAssumption(), assumptionChild.getStopFormula());
            sb.append("ASSUME \"");
            escape(assumption.toString(), sb);
            sb.append("\" ");

            if (!assumptionChild.getStopFormula().isTrue() || falseAssumptions.contains(child)) {
              sb.append("GOTO __FALSE");
            } else {
              sb.append("GOTO ART" + child.getElementId());
            }
            sb.append(";\n");
          }
        }
        sb.append("   TRUE -> GOTO __TRUE;\n\n");
      }
    }
    sb.append("END AUTOMATON\n");
    return sb.toString();
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

  private static void getTrueAssumptionElements(ARTElement e, Set<ARTElement> visited, Set<ARTElement> trueAssumptions, Set<AbstractElement> falseAssumptions) {
    if (!visited.add(e)) {
      return;
    }

    List<ARTElement> childrenAndCoveredList = new ArrayList<ARTElement>();
    childrenAndCoveredList.addAll(e.getLocalChildren());
    if(e.isCovered()){
      childrenAndCoveredList.add(e.getCoveringElement());
    }

    for (ARTElement child : childrenAndCoveredList) {
      getTrueAssumptionElements(child, visited, trueAssumptions, falseAssumptions);
    }

    AssumptionStorageElement asmptElement = AbstractElements.extractElementByType(e, AssumptionStorageElement.class);

    List<ARTElement> tempChildrenAndCoveredList = new ArrayList<ARTElement>();
    tempChildrenAndCoveredList.addAll(e.getLocalChildren());
    if(e.isCovered()){
      tempChildrenAndCoveredList.add(e.getCoveringElement());
    }

    if (asmptElement.getAssumption().isTrue()
        && asmptElement.getStopFormula().isTrue()
        && !falseAssumptions.contains(e)
        && trueAssumptions.containsAll(tempChildrenAndCoveredList)){
      trueAssumptions.add(e);
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
