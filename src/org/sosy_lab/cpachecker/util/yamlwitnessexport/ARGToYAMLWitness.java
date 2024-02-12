// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.expressions.RemovingStructuresVisitor;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantWitnessWriter.GraphTraverser;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantWitnessWriter.YamlWitnessExportException;

class ARGToYAMLWitness extends AbstractYAMLWitnessExporter {

  private final Map<ARGState, CollectedARGStates> stateToStatesCollector = new HashMap<>();

  public ARGToYAMLWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  protected static class CollectedARGStates {
    public Multimap<CFANode, ARGState> loopInvariants = HashMultimap.create();
    public Multimap<CFANode, ARGState> functionCallInvariants = HashMultimap.create();
    public Multimap<FunctionEntryNode, ARGState> functionContractRequires = HashMultimap.create();
    public Multimap<FunctionExitNode, ARGState> functionContractEnsures = HashMultimap.create();
  }

  private static class CollectRelevantARGStates
      extends GraphTraverser<ARGState, YamlWitnessExportException> {

    private final CollectedARGStates collectedStates = new CollectedARGStates();

    protected CollectRelevantARGStates(ARGState startNode) {
      super(startNode);
    }

    @Override
    protected ARGState visit(ARGState pSuccessor) throws YamlWitnessExportException {
      for (LocationState state :
          AbstractStates.asIterable(pSuccessor).filter(LocationState.class)) {
        CFANode node = state.getLocationNode();
        FluentIterable<CFAEdge> leavingEdges = CFAUtils.leavingEdges(node);
        if (node.isLoopStart()) {
          collectedStates.loopInvariants.put(node, pSuccessor);
        } else if (leavingEdges.size() == 1
            && leavingEdges.anyMatch(e -> e instanceof FunctionCallEdge)) {
          collectedStates.functionCallInvariants.put(node, pSuccessor);
        } else if (node instanceof FunctionEntryNode functionEntryNode) {
          collectedStates.functionContractRequires.put(functionEntryNode, pSuccessor);
        } else if (node instanceof FunctionExitNode functionExitNode) {
          collectedStates.functionContractEnsures.put(functionExitNode, pSuccessor);
        }
      }

      return pSuccessor;
    }

    @Override
    protected Iterable<ARGState> getSuccessors(ARGState pCurrent)
        throws YamlWitnessExportException {
      return pCurrent.getChildren();
    }
  }

  public CollectedARGStates getRelevantStates(ARGState pRootState)
      throws YamlWitnessExportException {
    if (!stateToStatesCollector.containsKey(pRootState)) {
      CollectRelevantARGStates statesCollector = new CollectRelevantARGStates(pRootState);
      statesCollector.traverse();
      stateToStatesCollector.put(pRootState, statesCollector.collectedStates);
    }

    return stateToStatesCollector.get(pRootState);
  }

  protected ExpressionTree<Object> getOverapproximationOfStates(
      Collection<ARGState> argStates, CFANode node) throws InterruptedException {
    return getOverapproximationOfStates(argStates, node, false);
  }

  protected ExpressionTree<Object> getOverapproximationOfStates(
      Collection<ARGState> argStates, CFANode node, boolean pReplaceOutputVariable)
      throws InterruptedException {
    FunctionEntryNode entryNode = cfa.getFunctionHead(node.getFunctionName());

    FluentIterable<ExpressionTreeReportingState> reportingStates =
        FluentIterable.from(argStates)
            .transformAndConcat(AbstractStates::asIterable)
            .filter(ExpressionTreeReportingState.class);
    List<List<ExpressionTree<Object>>> expressionsPerClass = new ArrayList<>();

    // TODO: Extend this to also include java Variables
    AIdExpression returnVariable;
    if (pReplaceOutputVariable
        && node.getFunction().getType().getReturnType() instanceof CType cType
        && !(cType instanceof CVoidType)) {
      returnVariable =
          new CIdExpression(
              FileLocation.DUMMY,
              new CVariableDeclaration(
                  FileLocation.DUMMY,
                  false,
                  CStorageClass.AUTO,
                  cType,
                  "\\return",
                  "\\return",
                  node.getFunctionName() + "::\\return",
                  null));
    } else {
      returnVariable = null;
    }

    for (Class<?> stateClass : reportingStates.transform(AbstractState::getClass).toSet()) {
      List<ExpressionTree<Object>> expressionsMatchingClass = new ArrayList<>();
      for (ExpressionTreeReportingState state : reportingStates) {
        if (stateClass.isAssignableFrom(state.getClass())) {
          expressionsMatchingClass.add(
              state.getFormulaApproximation(entryNode, node, returnVariable));
        }
      }
      expressionsPerClass.add(expressionsMatchingClass);
    }

    ExpressionTree<Object> overapproximationOfState =
        And.of(FluentIterable.from(expressionsPerClass).transform(Or::of));

    // Filter out CPAchecker internal variables from the over-approximation of the states
    // This transformation is NOT correct for all possible cases, since if multiple internal
    // variables are in relation to each other and this is relevant for the invariant, then this
    // will not work. A more sophisticated approach may consider all these dependencies and do an
    // actual replacement of CPAchecker internal variables
    // TODO: Improve this
    RemovingStructuresVisitor<Object, Exception> visitor =
        new RemovingStructuresVisitor<>(x -> x.toString().contains("__CPAchecker_TMP"));
    try {
      overapproximationOfState = overapproximationOfState.accept(visitor);
    } catch (Exception e) {
      logger.logException(
          Level.INFO, e, "Could not remove CPAchecker internal variables from invariant");
    }

    return overapproximationOfState;
  }
}
