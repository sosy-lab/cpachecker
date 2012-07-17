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
package org.sosy_lab.cpachecker.cpa.explicit.refiner;

import static com.google.common.collect.Lists.transform;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.Path;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.PredicateMap;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ReferencedVariablesCollector;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefinementManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.explict.refiner")
public class SmtBasedExplicitRefiner extends ExplicitRefiner {

  private final ExtendedFormulaManager formulaManager;

  protected final PathFormulaManager pathFormulaManager;

  protected final PredicateRefinementManager interpolationManager;

  protected int numberOfRefinements = 0;

  // statistics
  private Timer timerSyntacticalPathAnalysis = new Timer();

  protected SmtBasedExplicitRefiner(
      Configuration config,
      PathFormulaManager pathFormulaManager,
      ExtendedFormulaManager formulaManager,
      PredicateRefinementManager interpolationManager) throws InvalidConfigurationException {
    config.inject(this);
    this.formulaManager       = formulaManager;
    this.pathFormulaManager   = pathFormulaManager;
    this.interpolationManager = interpolationManager;
  }

  protected CounterexampleTraceInfo<Collection<AbstractionPredicate>> buildCounterexampleTrace(final Path errorPath)
      throws CPATransferException, CPAException, InterruptedException {

    return interpolationManager.buildCounterexampleTrace(
        getFormulasForPath(errorPath, errorPath.getFirst().getFirst()),
        ARGUtils.getAllStatesOnPathsTo(errorPath.getLast().getFirst()));
  }

  @Override
  protected Multimap<CFANode, String> determinePrecisionIncrement(
      final UnmodifiableReachedSet reachedSet,
      final Path errorPath) throws CPAException, InterruptedException {

    numberOfRefinements++;

    firstInterpolationPoint = null;

    Multimap<CFANode, String> precisionIncrement = HashMultimap.create();
    // in case the path is spurious
    if(isPathFeasable(errorPath, HashMultimap.<CFANode, String>create())) {
      return precisionIncrement;
    }

    CounterexampleTraceInfo<Collection<AbstractionPredicate>> cti = buildCounterexampleTrace(errorPath);

    ExplicitPrecision currentPrecision = Precisions.extractPrecisionByType(reachedSet.getPrecision(reachedSet.getLastState()), ExplicitPrecision.class);

    // create the mapping of CFA nodes to predicates, based on the counter example trace info
    PredicateMap predicateMap = new PredicateMap(cti.getPredicatesForRefinement(), transformPath(errorPath));

    // determine the precision increment
    precisionIncrement = predicateMap.determinePrecisionIncrement(formulaManager);
    System.out.println("1: " + precisionIncrement);
    // also add variables occurring on the error path and referenced by variables in precision increment
    precisionIncrement.putAll(determineReferencedVariablesInPath(currentPrecision, precisionIncrement, errorPath));
    System.out.println("2: " + precisionIncrement);
    firstInterpolationPoint = determineInterpolationPoint(errorPath, precisionIncrement);

    precisionIncrement = determineSingularIncrement(
        precisionIncrement,
        extractExplicitPrecision(reachedSet.getPrecision(firstInterpolationPoint)));
    System.out.println("3: " + precisionIncrement);
    // create the new precision
    return precisionIncrement;
  }

  /**
   * This method determines the new interpolation point.
   *
   * @param errorPath the error path from where to determine the interpolation point
   * @param precisionIncrement the current precision increment
   * @return the new interpolation point
   */
  private ARGState determineInterpolationPoint(Path errorPath, Multimap<CFANode, String> precisionIncrement) {
    for(Pair<ARGState, CFAEdge> element : errorPath) {
      if(precisionIncrement.containsKey(element.getSecond().getSuccessor())) {
        return element.getFirst();
      }
    }

    return null;
  }

  /**
   * This method ensures that no redundant entries are in the precision when combining the precision increment with the
   * current precision.
   *
   * @param currentIncrement the precision increment
   * @param precision the current precision
   * @return the currentIncrement, with all entries removed that are already in the precision
   */
  private Multimap<CFANode, String> determineSingularIncrement(
      final Multimap<CFANode, String> currentIncrement,
      final ExplicitPrecision precision) {

    Multimap<CFANode, String> singularIncrement = HashMultimap.create();

    for(Map.Entry<CFANode, String> entry : currentIncrement.entries()) {
      // only add the entry if the precision does not already allow
      // the tracking of the current identifier at the current location
      if(!precision.getCegarPrecision().allowsTrackingAt(entry.getKey(), entry.getValue())) {
        singularIncrement.put(entry.getKey(), entry.getValue());
      }
    }

    return singularIncrement;
  }

  private Multimap<CFANode, String> determineReferencedVariablesInPath(
      ExplicitPrecision precision,
      Multimap<CFANode, String> precisionIncrement,
      final Path errorPath) {

    List<CFAEdge> cfaTrace = new ArrayList<CFAEdge>();
    for(int i = 0; i < errorPath.size(); i++) {
      cfaTrace.add(errorPath.get(i).getSecond());
    }

    // the referenced-variable-analysis has the done on basis of all variables in the precision plus the current increment
    Collection<String> referencingVariables = precision.getCegarPrecision().getVariablesInPrecision();
    referencingVariables.addAll(precisionIncrement.values());

    ReferencedVariablesCollector collector = new ReferencedVariablesCollector(referencingVariables);
    Multimap<CFANode, String> referencedVariables = collector.collectVariables(cfaTrace);

    return referencedVariables;
  }

  private List<Formula> getFormulasForPath(List<Pair<ARGState, CFAEdge>> errorPath, ARGState initialElement) throws CPATransferException {
    PathFormula currentPathFormula = pathFormulaManager.makeEmptyPathFormula();

    List<Formula> formulas = new ArrayList<Formula>(errorPath.size());

    // iterate over edges (not nodes)
    for (Pair<ARGState, CFAEdge> pathElement : errorPath) {
      currentPathFormula = pathFormulaManager.makeAnd(currentPathFormula, pathElement.getSecond());

      formulas.add(currentPathFormula.getFormula());

      // reset the formula
      currentPathFormula = pathFormulaManager.makeEmptyPathFormula(currentPathFormula);
    }

    return formulas;
  }

  private final List<Pair<ARGState, CFANode>> transformPath(Path errorPath) {
    List<Pair<ARGState, CFANode>> result = Lists.newArrayList();

    for(ARGState ae : transform(errorPath, Pair.<ARGState>getProjectionToFirst())) {
        result.add(Pair.of(ae, AbstractStates.extractLocation(ae)));
    }

    assert errorPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
    return result;
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println(this.getClass().getSimpleName() + ":");
    out.println("  number of SMT-interpolation-based refinements: " + numberOfRefinements);
    out.println("  max. time for syntactical path analysis:       " + timerSyntacticalPathAnalysis.printMaxTime());
    out.println("  total time for syntactical path analysis:      " + timerSyntacticalPathAnalysis);
  }
}