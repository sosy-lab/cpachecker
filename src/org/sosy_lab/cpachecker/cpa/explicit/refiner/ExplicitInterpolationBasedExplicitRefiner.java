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

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.IAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpressionCollectorVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.AssumptionClosureCollector;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ExplicitInterpolator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.explicit.refiner")
public class ExplicitInterpolationBasedExplicitRefiner implements Statistics {
  /**
   * whether or not to do lazy-abstraction, i.e., when true, the re-starting node
   * for the re-exploration of the ARG will be the node closest to the root
   * where new information is made available through the current refinement
   */
  @Option(description="whether or not to do lazy-abstraction")
  private boolean doLazyAbstraction = true;

  @Option(description="whether or not to avoid restarting at assume edges after a refinement")
  private boolean avoidAssumes = false;

  @Option(description="whether or not to ignore the semantics of loop-leaving-assume-edges during interpolation - "
      + "this avoids to have loop-counters in the interpolant")
  private boolean ignoreAssumptionsInLoops = true;

  @Option(description="whether or not to use use-definition information from the error paths to optimize the "
      + "interpolation process")
  private boolean applyUseDefInformationForItp = true;

  @Option(description="whether or not the obtain a precision from use-definition information instead of interpolation")
  private boolean generateUseDefBasedPrecision = false;

  /**
   * the offset in the path from where to cut-off the subtree, and restart the analysis
   */
  private int interpolationOffset = -1;

  /**
   * a reference to the assignment-counting state, to make the precision increment aware of thresholds
   */
  private UniqueAssignmentsInPathConditionState assignments = null;

  /**
   * the set of assume-edges that leave loop structures
   */
  private Set<CAssumeEdge> loopLeavingAssumes = new HashSet<>();

  /**
   * the set of memory locations appearing in the loop-leaving-assume-edges
   */
  private Set<MemoryLocation> loopLeavingMemoryLocations = new HashSet<>();

  // statistics
  private int numberOfInterpolations        = 0;
  private Timer timerInterpolation          = new Timer();

  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  protected ExplicitInterpolationBasedExplicitRefiner(Configuration config,
      final LogManager pLogger, final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa)
      throws InvalidConfigurationException {
    config.inject(this);

    logger           = pLogger;
    cfa              = pCfa;
    shutdownNotifier = pShutdownNotifier;

    initializeLoopInformation();
  }

  /**
   * This method initializes the loop-information which is used during interpolation.
   */
  private void initializeLoopInformation() {
    for(Loop l : cfa.getLoopStructure().get().values()) {
      for(CFAEdge currentEdge : l.getOutgoingEdges()) {
        if(currentEdge instanceof CAssumeEdge) {
          loopLeavingAssumes.add((CAssumeEdge)currentEdge);
        }
      }
    }

    for(CAssumeEdge assumeEdge : loopLeavingAssumes) {
      CIdExpressionCollectorVisitor collector = new CIdExpressionCollectorVisitor();
      assumeEdge.getExpression().accept(collector);

      for (CIdExpression id : collector.getReferencedIdExpressions()) {
        String scope = ForwardingTransferRelation.isGlobal(id) ? null : assumeEdge.getPredecessor().getFunctionName();

        if(scope == null) {
          loopLeavingMemoryLocations.add(MemoryLocation.valueOf(id.getName()));
        } else {
          loopLeavingMemoryLocations.add(MemoryLocation.valueOf(scope, id.getName(), 0));
        }
      }
    }

    // clear the set of assume edges if the respective option is not set
    if(!ignoreAssumptionsInLoops) {
      loopLeavingAssumes.clear();
    }
  }

  protected Multimap<CFANode, MemoryLocation> determinePrecisionIncrement(ARGPath errorPath)
      throws CPAException, InterruptedException {
    timerInterpolation.start();
    interpolationOffset                   = -1;
    assignments                           = AbstractStates.extractStateByType(errorPath.getLast().getFirst(),
        UniqueAssignmentsInPathConditionState.class);

    ExplicitInterpolator interpolator             = new ExplicitInterpolator(logger, shutdownNotifier, cfa,
                                                      loopLeavingAssumes, loopLeavingMemoryLocations);
    Map<MemoryLocation, Long> currentInterpolant  = new HashMap<>();
    Multimap<CFANode, MemoryLocation> increment   = HashMultimap.create();

    List<CFAEdge> cfaTrace = Lists.newArrayList();
    for(Pair<ARGState, CFAEdge> elem : errorPath) {
      cfaTrace.add(elem.getSecond());
    }

    AssumptionClosureCollector coll = new AssumptionClosureCollector();
    Set<String> relevantVars = null;
    if(applyUseDefInformationForItp) {
      relevantVars = coll.collectVariables(cfaTrace);

      if(generateUseDefBasedPrecision) {
        for(String var : relevantVars) {
          String[] s = var.split("::");

          if(s.length == 1) {
            addToPrecisionIncrement(increment, cfaTrace.get(0), MemoryLocation.valueOf(s[0]));
          } else {
            addToPrecisionIncrement(increment, cfaTrace.get(0), MemoryLocation.valueOf(s[0], s[1], 0));
          }
        }

        timerInterpolation.stop();
        return increment;
      }
    }

    for (int i = 0; i < errorPath.size(); i++) {
      shutdownNotifier.shutdownIfNecessary();
      CFAEdge currentEdge = errorPath.get(i).getSecond();

      if (currentEdge instanceof BlankEdge) {
        // add the current interpolant to the increment
        for (MemoryLocation variableName : currentInterpolant.keySet()) {
          addToPrecisionIncrement(increment, currentEdge, variableName);
        }
        continue;
      }
      else if (currentEdge instanceof CFunctionReturnEdge) {
        currentEdge = ((CFunctionReturnEdge)currentEdge).getSummaryEdge();
      }

      // do interpolation
      Map<MemoryLocation, Long> inputInterpolant = new HashMap<>(currentInterpolant);
      Map<MemoryLocation, Long> interpolant = interpolator.deriveInterpolant(cfaTrace, i, inputInterpolant, relevantVars);
      numberOfInterpolations += interpolator.getNumberOfInterpolations();

      // early stop once we are past the first statement that made a path feasible for the first time
      if (interpolant == null) {
        timerInterpolation.stop();
        return increment;
      }
      for (Map.Entry<MemoryLocation, Long> element : interpolant.entrySet()) {
        if (element.getValue() == null) {
          currentInterpolant.remove(element.getKey());
        } else {
          currentInterpolant.put(element.getKey(), element.getValue());
        }
      }

      // remove variables from the interpolant that belong to the scope of the returning function
      // this is done one iteration after returning from the function, as the special FUNCTION_RETURN_VAR is needed that long
      if (i > 0 && errorPath.get(i - 1).getSecond().getEdgeType() == CFAEdgeType.ReturnStatementEdge) {
        currentInterpolant = clearInterpolant(currentInterpolant, errorPath.get(i - 1).getSecond().getSuccessor().getFunctionName());
      }

      // add the current interpolant to the increment
      for (MemoryLocation variableName : currentInterpolant.keySet()) {
        if (interpolationOffset == -1) {
          interpolationOffset = i + 1;
        }

        addToPrecisionIncrement(increment, currentEdge, variableName);
      }
    }

    timerInterpolation.stop();

    return increment;
  }

  /**
   * This method adds the given variable at the given edge/location to the increment.
   *
   * @param increment the current increment
   * @param currentEdge the current edge for which to add a new variable
   * @param memoryLocation the name of the variable to add to the increment at the given edge
   */
  private void addToPrecisionIncrement(Multimap<CFANode, MemoryLocation> increment, CFAEdge currentEdge, MemoryLocation memoryLocation) {
    if(assignments == null || !assignments.exceedsHardThreshold(memoryLocation)) {
      increment.put(currentEdge.getSuccessor(), memoryLocation);
    }
  }

  /**
   * This method removes variables from the interpolant that belong to the scope of the given function.
   *
   * @param currentInterpolant the current interpolant
   * @param functionName the name of the function for which to remove variables
   * @return the current interpolant with the respective variables removed
   */
  private Map<MemoryLocation, Long> clearInterpolant(Map<MemoryLocation, Long> currentInterpolant, String functionName) {
    for (Iterator<MemoryLocation> variableNames = currentInterpolant.keySet().iterator(); variableNames.hasNext(); ) {
      if (variableNames.next().isOnFunctionStack(functionName)) {
        variableNames.remove();
      }
    }

    return currentInterpolant;
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
      if(!generateUseDefBasedPrecision) {
        throw new RefinementFailedException(Reason.InterpolationFailed, errorPath);
      }
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
    return "Explicit-Interpolation-Based Refiner";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println("  number of explicit interpolations:                   " + numberOfInterpolations);
    out.println("  max. time for singe interpolation:                   " + timerInterpolation.getMaxTime().formatAs(TimeUnit.SECONDS));
    out.println("  total time for interpolation:                        " + timerInterpolation);
  }

  public int getInterpolationOffset() {
    return interpolationOffset;
  }
}