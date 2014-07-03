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
package org.sosy_lab.cpachecker.cpa.octagon.refiner;

import static com.google.common.collect.FluentIterable.from;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.IAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.octagon.refiner.util.OctagonInterpolator;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolationBasedRefiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ErrorPathClassifier;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.octagon.refiner")
public class OctagonInterpolationBasedRefiner implements Statistics {
  /**
   * whether or not to do lazy-abstraction, i.e., when true, the re-starting node
   * for the re-exploration of the ARG will be the node closest to the root
   * where new information is made available through the current refinement
   */
  @Option(description="whether or not to do lazy-abstraction")
  private boolean doLazyAbstraction = true;

  @Option(description="whether or not to avoid restarting at assume edges after a refinement")
  private boolean avoidAssumes = false;

  @Option(description="whether or not to interpolate the shortest infeasible prefix rather than the whole error path")
  private boolean interpolateInfeasiblePrefix = false;

  /**
   * the offset in the path from where to cut-off the subtree, and restart the analysis
   */
  private int interpolationOffset = -1;

  /**
   * a reference to the assignment-counting state, to make the precision increment aware of thresholds
   */
  private UniqueAssignmentsInPathConditionState assignments = null;

  // statistics
  private int totalInterpolations       = 0;
  private int totalInterpolationQueries = 0;
  private Timer timerInterpolation      = new Timer();

  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final OctagonInterpolator interpolator;

  protected OctagonInterpolationBasedRefiner(Configuration config,
      final LogManager pLogger, final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa)
      throws InvalidConfigurationException {
    config.inject(this);

    logger           = pLogger;
    cfa              = pCfa;
    shutdownNotifier = pShutdownNotifier;
    interpolator     = new OctagonInterpolator(config, logger, shutdownNotifier, cfa);
  }

  protected Map<ARGState, ValueAnalysisInterpolant> performInterpolation(ARGPath errorPath,
      ValueAnalysisInterpolant interpolant) throws CPAException, InterruptedException {
    totalInterpolations++;
    timerInterpolation.start();

    interpolationOffset = -1;

    errorPath = obtainErrorPathPrefix(errorPath, interpolant);

    List<CFAEdge> errorTrace = obtainErrorTrace(errorPath);

    Map<ARGState, ValueAnalysisInterpolant> pathInterpolants = new LinkedHashMap<>(errorPath.size());

    for (int i = 0; i < errorPath.size() - 1; i++) {
      shutdownNotifier.shutdownIfNecessary();

      if(!interpolant.isFalse()) {
        interpolant = interpolator.deriveInterpolant(errorTrace, i, interpolant);
      }

      totalInterpolationQueries = totalInterpolationQueries + interpolator.getNumberOfInterpolationQueries();

      if(!interpolant.isTrivial() && interpolationOffset == -1) {
        interpolationOffset = i + 1;
      }

      pathInterpolants.put(errorPath.get(i + 1).getFirst(), interpolant);
    }

    assert interpolant.isFalse() : "final interpolant is not false";

    timerInterpolation.stop();
    return pathInterpolants;
  }

  /**
   * This method obtains, from the error path, the list of CFA edges to be interpolated.
   *
   * @param errorPath the error path
   * @return the list of CFA edges to be interpolated
   */
  private List<CFAEdge> obtainErrorTrace(ARGPath errorPath) {
    return from(errorPath).transform(Pair.<CFAEdge>getProjectionToSecond()).toList();
  }

  /**
   * This path obtains a (sub)path of the error path which is given to the interpolation procedure.
   *
   * @param errorPath the original error path
   * @param interpolant the initial interpolant, i.e. the initial state, with which to check the error path.
   * @return a (sub)path of the error path which is given to the interpolation procedure
   * @throws CPAException
   * @throws InterruptedException
   */
  private ARGPath obtainErrorPathPrefix(ARGPath errorPath, ValueAnalysisInterpolant interpolant)
          throws CPAException, InterruptedException {
    if(interpolateInfeasiblePrefix) {
      try {
        ValueAnalysisFeasibilityChecker checker = new ValueAnalysisFeasibilityChecker(logger, cfa);

        List<ARGPath> prefixes = checker.getInfeasilbePrefixes(errorPath, interpolant.createValueAnalysisState());

        errorPath = new ErrorPathClassifier(cfa.getVarClassification()).obtainPrefixWithLowestScore(prefixes);

      } catch (InvalidConfigurationException e) {
        throw new CPAException("Configuring ValueAnalysisFeasibilityChecker failed: " + e.getMessage(), e);
      }
    }

    return errorPath;
  }

  protected Multimap<CFANode, MemoryLocation> determinePrecisionIncrement(ARGPath errorPath)
      throws CPAException, InterruptedException {

    assignments = AbstractStates.extractStateByType(errorPath.getLast().getFirst(),
        UniqueAssignmentsInPathConditionState.class);

    Multimap<CFANode, MemoryLocation> increment = HashMultimap.create();

    Map<ARGState, ValueAnalysisInterpolant> itps = performInterpolation(errorPath, ValueAnalysisInterpolant.createInitial());

    for(Map.Entry<ARGState, ValueAnalysisInterpolant> itp : itps.entrySet()) {
      addToPrecisionIncrement(increment, AbstractStates.extractLocation(itp.getKey()), itp.getValue());
    }

    return increment;
  }

  /**
   * This method adds the given variable at the given edge/location to the increment.
   *
   * @param increment the current increment
   * @param currentEdge the current edge for which to add a new variable
   * @param memoryLocation the name of the variable to add to the increment at the given edge
   */
  private void addToPrecisionIncrement(Multimap<CFANode, MemoryLocation> increment,
      CFANode currentNode,
      ValueAnalysisInterpolant itp) {
    for(MemoryLocation memoryLocation : itp.getMemoryLocations()) {
      if(assignments == null || !assignments.exceedsHardThreshold(memoryLocation)) {
        increment.put(currentNode, memoryLocation);
      }
    }
  }

  /**
   * This method determines the new refinement root.
   *
   * @param errorPath the error path from where to determine the refinement root
   * @param increment the current precision increment
   * @param isRepeatedRefinement the flag to determine whether or not this is a repeated refinement
   * @return the new refinement root
   * @throws RefinementFailedException if no refinement root can be determined
   */
  Pair<ARGState, CFAEdge> determineRefinementRoot(ARGPath errorPath, Multimap<CFANode, MemoryLocation> increment,
      boolean isRepeatedRefinement) throws RefinementFailedException {

    if(interpolationOffset == -1) {
      throw new RefinementFailedException(Reason.InterpolationFailed, errorPath);
    }

    // if doing lazy abstraction, use the node closest to the root node where new information is present
    if (doLazyAbstraction) {
      // try to find a more suitable cut-off point when we deal with a repeated refinement or
      // cut-off is an assume edge, and this should be avoided
      if (isRepeatedRefinement || (avoidAssumes && cutOffIsAssumeEdge(errorPath))) {
        List<Pair<ARGState, CFAEdge>> trace = errorPath.subList(0, interpolationOffset - 1);

        // check in reverse order only when avoiding assumes
        if(avoidAssumes && cutOffIsAssumeEdge(errorPath)) {
          trace = Lists.reverse(trace);
        }

        // check each edge, if it assigns a "relevant" variable, if so, use that as new refinement root
        Collection<String> releventVariables = convertToIdentifiers(increment.values());
        for (Pair<ARGState, CFAEdge> currentElement : trace) {
          if(edgeAssignsVariable(currentElement.getSecond(), releventVariables)) {
            return errorPath.get(errorPath.indexOf(currentElement) + 1);
          }
        }
      }

      return errorPath.get(interpolationOffset);
    }

    // otherwise, just use the successor of the root node
    else {
      return errorPath.get(1);
    }
  }

  /**
   * This method translates a collection of memory locations to a collection of variable identifiers.
   *
   * @param memoryLocations the collection of memory locations
   * @return the set of variable identifiers
   */
  private Collection<String> convertToIdentifiers(Collection<MemoryLocation> memoryLocations) {
    Set<String> identifiers = new HashSet<>();

    for(MemoryLocation memoryLocation : memoryLocations) {
      identifiers.add(memoryLocation.getAsSimpleString());
    }

    return identifiers;
  }

  /**
   * This method checks whether or not the current cut-off point is at an assume edge.
   *
   * @param errorPath the error path
   * @return true, if the current cut-off point is at an assume edge, else false
   */
  private boolean cutOffIsAssumeEdge(ARGPath errorPath) {
    return errorPath.get(Math.max(1, interpolationOffset - 1)).getSecond().getEdgeType() == CFAEdgeType.AssumeEdge;
  }

  /**
   * This method determines whether or not the current edge is assigning any of the given variables.
   *
   * @param currentEdge the current edge to inspect
   * @param variableNames the collection of variables to check for
   * @return true, if any of the given variables is assigned in the given edge
   */
  private boolean edgeAssignsVariable(CFAEdge currentEdge, Collection<String> variableNames) {
    switch (currentEdge.getEdgeType()) {
      case StatementEdge:
      case DeclarationEdge:
        return isAssigningEdge(currentEdge, variableNames);

      case MultiEdge:
        for (CFAEdge singleEdge : ((MultiEdge)currentEdge)) {
          if (isAssigningEdge(singleEdge, variableNames)) {
            return true;
          }
        }
        break;

      default:
        break;
    }

    return false;
  }

  /**
   * This method determines whether or not the current edge is assigning any of the given variables.
   *
   * @param currentEdge the current edge to inspect
   * @param variableNames the collection of variables to check for
   * @return true, if any of the given variables is assigned in the given edge
   */
  private boolean isAssigningEdge(CFAEdge currentEdge, Collection<String> variableNames) {
    if (currentEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      IAStatement statement = ((AStatementEdge)currentEdge).getStatement();

      if (statement instanceof IAssignment) {
        IAExpression assignedVariable = ((IAssignment)statement).getLeftHandSide();

        if (assignedVariable instanceof AIdExpression) {
          IASimpleDeclaration declaration = ((AIdExpression)assignedVariable).getDeclaration();

          if(declaration instanceof CVariableDeclaration) {
            return variableNames.contains(((CVariableDeclaration)declaration).getQualifiedName());
          }
        }
      }
    }

    else if (currentEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      ADeclarationEdge declEdge = ((ADeclarationEdge)currentEdge);
      if (declEdge.getDeclaration() instanceof CVariableDeclaration) {
        return variableNames.contains(((CVariableDeclaration)declEdge.getDeclaration()).getQualifiedName());
      }
    }

    return false;
  }

  @Override
  public String getName() {
    return "ValueAnalysisInterpolationBasedRefiner";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println("  Number of interpolations:          " + String.format(Locale.US, "%9d",totalInterpolations));
    out.println("  Number of interpolation queries:   " + String.format(Locale.US, "%9d",totalInterpolationQueries));
    out.println("  Max. time for singe interpolation:     " + timerInterpolation.getMaxTime().formatAs(TimeUnit.SECONDS));
    out.println("  Total time for interpolation:          " + timerInterpolation);
  }

  public int getInterpolationOffset() {
    return interpolationOffset;
  }
}
