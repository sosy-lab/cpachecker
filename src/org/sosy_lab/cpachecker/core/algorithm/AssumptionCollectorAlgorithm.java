/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Outer algorithm to collect all invariants generated during
 * the analysis, and report them to the user
 *
 * @author g.theoduloz
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
  
  @Option(name="export")
  private boolean exportAssumptions = true;

  @Option(name="file", type=Option.Type.OUTPUT_FILE)
  private File assumptionsFile = new File("assumptions.txt");
  
  @Option(name="automatonFile", type=Option.Type.OUTPUT_FILE)
  private File assumptionAutomatonFile = new File("AssumptionAutomaton.txt");

  private final LogManager logger;
  private final Algorithm innerAlgorithm;
  private final FormulaManager formulaManager;
  private final AssumptionWithLocation exceptionAssumptions;
  private final AssumptionStorageCPA cpa;
  
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
  public boolean run(ReachedSet reached) throws CPAException {
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
        addAssumptionsForFailedRefinement(exceptionAssumptions, failedRefinement);

        // remove element and it's parent from reached set
        // parent needs to be removed, because CPAAlgorithm re-added it
        // to the waitlist just before refinement
        ARTElement ae = (ARTElement)reached.getLastElement();
        for (ARTElement p : ImmutableSet.copyOf(ae.getParents())) {
          reached.remove(p);
          p.removeFromART();
        }
        reached.remove(ae);
        ae.removeFromART();

        restartCPA = true;
      } catch (CPAException e) {
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
      CFANode location = extractLocation(element);
      assert location != null;
      
      AssumptionStorageElement e = AbstractElements.extractElementByType(element, AssumptionStorageElement.class);
      Formula assumption = formulaManager.makeAnd(e.getAssumption(), e.getStopFormula());
      if (!assumption.isTrue()) {
        Formula dataRegion = ReportingUtils.extractReportedFormulas(formulaManager, element);
      
        result.add(location, formulaManager.makeOr(assumption, formulaManager.makeNot(dataRegion)));
      }
    }
   
    // dump invariants to prevent going further with nodes in
    // the waitlist
    if (reached.hasWaitingElement()) {
      logger.log(Level.FINER, "Dumping assumptions resulting from unprocessed elements");
      addAssumptionsForWaitlist(result, reached.getWaitlist());
    }
    
    return result;
  }
  
  private String produceAssumptionAutomaton(ReachedSet reached) {
    AbstractElement firstElement = reached.getFirstElement();
    if (!(firstElement instanceof ARTElement)) {
      return "Cannot dump assumption as automaton if ARTCPA is not used.";
    }
    
    Set<ARTElement> artNodes = new HashSet<ARTElement>();
    Set<ARTElement> trueAssumptions = new HashSet<ARTElement>();
    Set<AbstractElement> falseAssumptions = Sets.newHashSet(reached.getWaitlist());
    getTrueAssumptionElements((ARTElement)firstElement, artNodes, trueAssumptions, falseAssumptions);
    
    StringBuilder sb = new StringBuilder();
    sb.append("OBSERVER AUTOMATON AssumptionAutomaton\n\n");
    sb.append("INITIAL STATE ART" + ((ARTElement)reached.getFirstElement()).getElementId() + ";\n\n");
    sb.append("STATE __TRUE :\n");
    sb.append("    TRUE -> ASSUME \"true\" GOTO __TRUE;\n\n");
    sb.append("STATE __FALSE :\n");
    sb.append("    TRUE -> ASSUME \"false\" GOTO __FALSE;\n\n");
    
    for (ARTElement e : artNodes) {
      if (falseAssumptions.contains(e)
        || (!e.getParents().isEmpty() && trueAssumptions.containsAll(e.getParents()))) {
        continue;
      }
      
      CFANode loc = AbstractElements.extractLocation(e);
      sb.append("STATE USEFIRST ART" + e.getElementId() + " :\n");
      if (trueAssumptions.contains(e)) {
        sb.append("   TRUE -> GOTO __TRUE;\n\n");
      
      } else {
        for (ARTElement child : e.getChildren()) {
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
        sb.append("    TRUE -> ERROR;\n\n");
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
    
    for (ARTElement child : e.getChildren()) {
      getTrueAssumptionElements(child, visited, trueAssumptions, falseAssumptions);
    }
    
    AssumptionStorageElement asmptElement = AbstractElements.extractElementByType(e, AssumptionStorageElement.class);
    if (asmptElement.getAssumption().isTrue() 
        && asmptElement.getAssumption().isTrue()
        && !falseAssumptions.contains(e)
        && trueAssumptions.containsAll(e.getChildren())) {
      
      trueAssumptions.add(e);
    }
  }
  
  /**
   * Add to the given map the invariant required to
   * avoid the given refinement failure
   */
  private void addAssumptionsForFailedRefinement(
      AssumptionWithLocation invariant,
      RefinementFailedException failedRefinement) {
    Path path = failedRefinement.getErrorPath();

    int pos = failedRefinement.getFailurePoint();

    if (pos == -1)
      pos = path.size() - 2; // the node before the error node

    ARTElement e = path.get(pos).getFirst();
    Formula dataRegion = ReportingUtils.extractReportedFormulas(formulaManager, e);
    invariant.add(extractLocation(e), formulaManager.makeNot(dataRegion));
  }

  /**
   * Add to the given map the invariant required to
   * avoid nodes in the given set of states
   */
  private void addAssumptionsForWaitlist(
      AssumptionWithLocation invariant,
      Iterable<AbstractElement> waitlist) {
    for (AbstractElement element : waitlist) {
      Formula dataRegion = ReportingUtils.extractReportedFormulas(formulaManager, element);
      invariant.add(extractLocation(element), formulaManager.makeNot(dataRegion));
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
