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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.Path;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.explict.refiner")
abstract public class ExplicitRefiner implements IExplicitRefiner {

  protected final PathFormulaManager pathFormulaManager;

  protected List<Pair<ARGState, CFAEdge>> currentEdgePath = null;
  protected List<Pair<ARGState, CFANode>> currentNodePath = null;

  protected CounterexampleTraceInfo<Collection<AbstractionPredicate>> currentTraceInfo = null;

  private Set<String> pathHashes = new HashSet<String>();

  // statistics
  protected int numberOfExplicitRefinements                   = 0;

  @Option(description="whether or not to always use the inital node as starting point for the next iteration")
  boolean useInitialNodeAsRestartingPoint = true;

  protected ExplicitRefiner(Configuration config, PathFormulaManager pathFormulaManager) throws InvalidConfigurationException {
    config.inject(this, ExplicitRefiner.class);
    this.pathFormulaManager = pathFormulaManager;
  }

  @Override
  public final List<Pair<ARGState, CFANode>> transformPath(Path errorPath) {
    List<Pair<ARGState, CFANode>> result = Lists.newArrayList();

    for(ARGState ae : transform(errorPath, Pair.<ARGState>getProjectionToFirst())) {
        result.add(Pair.of(ae, AbstractStates.extractLocation(ae)));
    }

    assert errorPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
    return result;
  }

  @Override
  public List<Formula> getFormulasForPath(List<Pair<ARGState,
      CFANode>> errorPath,
      ARGState initialElement) throws CPATransferException {
    PathFormula currentPathFormula = pathFormulaManager.makeEmptyPathFormula();

    List<Formula> formulas = new ArrayList<Formula>(errorPath.size());

    // iterate over edges (not nodes)
    for (Pair<ARGState, CFAEdge> pathElement : this.currentEdgePath) {
      currentPathFormula = pathFormulaManager.makeAnd(currentPathFormula, pathElement.getSecond());

      formulas.add(currentPathFormula.getFormula());

      // reset the formula
      currentPathFormula = pathFormulaManager.makeEmptyPathFormula(currentPathFormula);
    }

    return formulas;
  }

  @Override
  public Pair<ARGState, Precision> performRefinement(
      UnmodifiableReachedSet reachedSet,
      Precision oldPrecision,
      List<Pair<ARGState, CFANode>> errorPath,
      CounterexampleTraceInfo<Collection<AbstractionPredicate>> traceInfo) throws CPAException {
    numberOfExplicitRefinements++;

    currentNodePath   = errorPath;
    currentTraceInfo  = traceInfo;

    Multimap<CFANode, String> precisionIncrement = determinePrecisionIncrement(
        reachedSet,
        extractExplicitPrecision(oldPrecision));

    ARGState interpolationPoint = determineInterpolationPoint(errorPath, precisionIncrement);

    if(interpolationPoint == null) {
        throw new RefinementFailedException(Reason.InterpolationFailed, null);
    }

    Precision precision = createExplicitPrecision(extractExplicitPrecision(oldPrecision), precisionIncrement);

    return Pair.of(interpolationPoint, precision);
  }

  abstract protected Multimap<CFANode, String> determinePrecisionIncrement(UnmodifiableReachedSet reachedSet, ExplicitPrecision oldPrecision) throws CPAException;

  /**
   * This method determines the new interpolation point.
   *
   * @param errorPath the error path from where to determine the interpolation point
   * @param precisionIncrement the current precision increment
   * @return the new interpolation point
   */
  protected ARGState determineInterpolationPoint(
      List<Pair<ARGState, CFANode>> errorPath,
      Multimap<CFANode, String> precisionIncrement) {

    // just use initial node of error path if the respective option is set
    if(useInitialNodeAsRestartingPoint) {
      return errorPath.get(1).getFirst();
    }

    // otherwise, use the first node where new information is present
    else {
      for(Pair<ARGState, CFANode> element : errorPath) {
        if(precisionIncrement.containsKey(element.getSecond())) {
          return element.getFirst();
        }
      }
    }

    return null;
  }

  /**
   * This method creates an explicit precision out of an old precision and an increment.
   *
   * @param oldPrecision the old precision to use as base
   * @param precisionIncrement the increment to add
   * @return the new explicit precision
   */
  protected ExplicitPrecision createExplicitPrecision(
      ExplicitPrecision oldPrecision,
      Multimap<CFANode, String> precisionIncrement) {

    ExplicitPrecision explicitPrecision = new ExplicitPrecision(oldPrecision);

    explicitPrecision.getCegarPrecision().addToMapping(precisionIncrement);

    return explicitPrecision;
  }

  /**
   * This method extracts the explicit precision.
   *
   * @param precision the current precision
   * @return the explicit precision
   */
  protected ExplicitPrecision extractExplicitPrecision(Precision precision) {
    ExplicitPrecision explicitPrecision = Precisions.extractPrecisionByType(precision, ExplicitPrecision.class);
    if(explicitPrecision == null) {
      throw new IllegalStateException("Could not find the ExplicitPrecision for the error element");
    }
    return explicitPrecision;
  }

  @Override
  public boolean hasMadeProgress(List<Pair<ARGState, CFAEdge>> currentErrorPath, Precision currentPrecision) {
    ExplicitPrecision currentExplicitPrecision = extractExplicitPrecision(currentPrecision);

    return pathHashes.add(currentExplicitPrecision.getCegarPrecision().toString().hashCode() +
        "_" +
        currentEdgePath.toString().hashCode());
  }

  @Override
  public void setCurrentErrorPath(List<Pair<ARGState, CFAEdge>> currentErrorPath) {
    this.currentEdgePath = currentErrorPath;
  }
}