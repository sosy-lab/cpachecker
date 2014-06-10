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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import static com.google.common.collect.FluentIterable.from;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
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
import org.sosy_lab.cpachecker.cpa.value.Value;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.InitialAssumptionUseDefinitionCollector;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisInterpolator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.value.refiner")
public class ValueAnalysisInterpolationBasedRefiner implements Statistics {
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

  private final ValueAnalysisInterpolator interpolator;

  protected ValueAnalysisInterpolationBasedRefiner(Configuration pConfig,
      final LogManager pLogger, final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    logger           = pLogger;
    cfa              = pCfa;
    shutdownNotifier = pShutdownNotifier;
    interpolator     = new ValueAnalysisInterpolator(pConfig, logger, shutdownNotifier, cfa);
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
   * This method adds the given variable at the given location to the increment.
   *
   * @param increment the current increment
   * @param currentNode the current node for which to add a new variable
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
   * This method obtains, from the error path, the list of CFA edges to be interpolated.
   *
   * @param errorPath the error path
   * @return the list of CFA edges to be interpolated
   */
  private List<CFAEdge> obtainErrorTrace(ARGPath errorPath) {
    return from(errorPath).transform(Pair.<CFAEdge>getProjectionToSecond()).toList();
  }

  /**
   *
   * @param errorPath
   * @param interpolant
   * @return
   * @throws CPAException
   * @throws InterruptedException
   */
  private ARGPath obtainErrorPathPrefix(ARGPath errorPath, ValueAnalysisInterpolant interpolant)
          throws CPAException, InterruptedException {
    if(interpolateInfeasiblePrefix) {
      try {
        ValueAnalysisFeasibilityChecker checker = new ValueAnalysisFeasibilityChecker(logger, cfa);

        List<ARGPath> prefixes = checker.getInfeasilbePrefixes(errorPath,
            new ValueAnalysisPrecision("",
                Configuration.builder().build(),
                Optional.<VariableClassification>absent(),
                new ValueAnalysisPrecision.FullPrecision()),
            interpolant.createValueAnalysisState());

        errorPath = obtainPrefixWithLowestScore(prefixes);

      } catch (InvalidConfigurationException e) {
        throw new CPAException("Configuring ValueAnalysisFeasibilityChecker failed: " + e.getMessage(), e);
      }
    }

    return errorPath;
  }

  /**
   * This method obtains, heuristically, from the list of infeasible prefixes the one with the lowest score, i.e., the
   * one that leads, given the heuristic performs well, to the best interpolants.
   *
   * @param pPrefixes the list of infeasible prefixes of the original error path
   * @return the ARG path with the lowest score
   */
  private ARGPath obtainPrefixWithLowestScore(List<ARGPath> pPrefixes) {

    final int BOOLEAN_VAR   = 2;
    final int INTEQUAL_VAR  = 4;
    final int UNKNOWN_VAR   = 16;

    ARGPath fullArgPath   = new ARGPath();
    long lowestScore      = Long.MAX_VALUE;
    int lowestScoreIndex  = 0;

    for(ARGPath prefix : pPrefixes) {
      assert(prefix.getLast().getSecond().getEdgeType() == CFAEdgeType.AssumeEdge);

      fullArgPath.addAll(prefix);

      InitialAssumptionUseDefinitionCollector collector = new InitialAssumptionUseDefinitionCollector();
      Set<String> useDefinitionInformation = collector.obtainUseDefInformation(fullArgPath);

      long score = 1;
      for(String variableName : useDefinitionInformation) {
        int factor = UNKNOWN_VAR;
        if(cfa.getVarClassification().get().getIntBoolVars().contains(variableName)) {
          factor = BOOLEAN_VAR;
        }
        else if(cfa.getVarClassification().get().getIntEqualVars().contains(variableName)) {
          factor = INTEQUAL_VAR;
        }

        score = score * factor;
      }

      if(score <= lowestScore) {
        lowestScore = score;
        lowestScoreIndex = pPrefixes.indexOf(prefix);
      }
    }

    ARGPath errorPath = new ARGPath();
    for(int j = 0; j <= lowestScoreIndex; j++) {
      if(j != lowestScoreIndex) {
        errorPath.addAll(pPrefixes.get(j).subList(0, pPrefixes.get(j).size() - 1));
      }

      else {
        errorPath.addAll(pPrefixes.get(j).subList(0, pPrefixes.get(j).size()));
      }
    }

    return errorPath;
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

  /**
   * This class represents a Value-Analysis interpolant, itself, just a mere wrapper around a map
   * from memory locations to values, representing a variable assignment.
   */
  public static class ValueAnalysisInterpolant {
    /**
     * the variable assignment of the interpolant
     */
    private final Map<MemoryLocation, Value> assignment;

    /**
     * the interpolant representing "true"
     */
    public static final ValueAnalysisInterpolant TRUE  = new ValueAnalysisInterpolant();

    /**
     * the interpolant representing "false"
     */
    public static final ValueAnalysisInterpolant FALSE = new ValueAnalysisInterpolant((Map<MemoryLocation, Value>)null);

    /**
     * Constructor for a new, empty interpolant, i.e. the interpolant representing "true"
     */
    private ValueAnalysisInterpolant() {
      assignment = new HashMap<>();
    }

    /**
     * Constructor for a new interpolant representing the given variable assignment
     *
     * @param pAssignment the variable assignment to be represented by the interpolant
     */
    public ValueAnalysisInterpolant(Map<MemoryLocation, Value> pAssignment) {
      assignment = pAssignment;
    }

    /**
     * This method serves as factory method for an initial, i.e. an interpolant representing "true"
     *
     * @return
     */
    static ValueAnalysisInterpolant createInitial() {
      return new ValueAnalysisInterpolant();
    }

    Set<MemoryLocation> getMemoryLocations() {
      return isFalse()
          ? Collections.<MemoryLocation>emptySet()
          : Collections.unmodifiableSet(assignment.keySet());
    }

    /**
     * This method joins to value-analysis interpolants. If the underlying map contains different values for a key
     * contained in both maps, the behaviour is undefined.
     *
     * @param other the value-analysis interpolant to join with this one
     * @return a new value-analysis interpolant containing the joined mapping of this and the other value-analysis
     * interpolant
     */
    public ValueAnalysisInterpolant join(ValueAnalysisInterpolant other) {

      if(assignment == null || other.assignment == null) {
        return ValueAnalysisInterpolant.FALSE;
      }

      Map<MemoryLocation, Value> newAssignment = new HashMap<>(assignment);

      // add other itp mapping - one by one for now, to check for correctness
      // newAssignment.putAll(other.assignment);
      for(Map.Entry<MemoryLocation, Value> entry : other.assignment.entrySet()) {
        if(newAssignment.containsKey(entry.getKey())) {
          assert(entry.getValue().equals(other.assignment.get(entry.getKey()))) : "interpolants mismatch in " + entry.getKey();
        }

        newAssignment.put(entry.getKey(), entry.getValue());
      }


      return new ValueAnalysisInterpolant(newAssignment);
    }

    @Override
    public int hashCode() {
      return (assignment == null) ? 0 : assignment.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      ValueAnalysisInterpolant other = (ValueAnalysisInterpolant) obj;
      if ((assignment == null && other.assignment != null) || (assignment != null && other.assignment == null)) {
        return false;
      }

      else if (!assignment.equals(other.assignment)) {
        return false;
      }

      return true;
    }

    /**
     * The method checks for trueness of the interpolant.
     *
     * @return true, if the interpolant represents "true", else false
     */
    private boolean isTrue() {
      return assignment.isEmpty();
    }

    /**
     * The method checks for falseness of the interpolant.
     *
     * @return true, if the interpolant represents "false", else true
     */
    boolean isFalse() {
      return assignment == null;
    }

    /**
     * The method checks if the interpolant is a trivial one, i.e. if it represents either true or false
     *
     * @return true, if the interpolant is trivial, else false
     */
    boolean isTrivial() {
      return isFalse() || isTrue();
    }

    /**
     * This method clears memory locations from the assignment that belong to the given function.
     *
     * @param functionName the name of the function for which to remove assignments
     */
    private void clearScope(String functionName) {
      if(isTrivial()) {
        return;
      }

      for (Iterator<MemoryLocation> variableNames = assignment.keySet().iterator(); variableNames.hasNext(); ) {
        if (variableNames.next().isOnFunctionStack(functionName)) {
          variableNames.remove();
        }
      }
    }

    /**
     * This method serves as factory method to create a value-analysis state from the interpolant
     *
     * @return a value-analysis state that represents the same variable assignment as the interpolant
     */
    public ValueAnalysisState createValueAnalysisState() {
      return new ValueAnalysisState(PathCopyingPersistentTreeMap.copyOf(assignment));
    }

    @Override
    public String toString() {
      if(isFalse()) {
        return "FALSE";
      }

      if(isTrue()) {
        return "TRUE";
      }

      return assignment.toString();
    }

    public boolean strengthen(ValueAnalysisState valueState, ARGState argState) {
      if (isTrivial()) {
        return false;
      }

      boolean strengthened = false;

      for (Map.Entry<MemoryLocation, Value> itp : assignment.entrySet()) {
        if(!valueState.contains(itp.getKey())) {
          valueState.assignConstant(itp.getKey(), itp.getValue());
          strengthened = true;
        }

        else if(valueState.contains(itp.getKey()) && valueState.getValueFor(itp.getKey()).asNumericValue().longValue() != itp.getValue().asNumericValue().longValue()) {
          assert false : "state and interpolant do not match in value for variable " + itp.getKey() + "[state = " + valueState.getValueFor(itp.getKey()).asNumericValue().longValue() + " != " + itp.getValue() + " = itp] for state " + argState.getStateId();
        }
      }

      return strengthened;
    }
  }
}
