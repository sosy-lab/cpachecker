// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.base.Verify;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.graph.SuccessorsFunction;
import com.google.common.graph.Traverser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
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
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.ReportingMethodNotImplementedException;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.TranslationToExpressionTreeFailedException;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingCPA;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.expressions.RemovingStructuresVisitor;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.ARGToYAMLWitness.CollectedARGStates.ARGStatePair;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionContractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;

class ARGToYAMLWitness extends AbstractYAMLWitnessExporter {

  private final Map<ARGState, CollectedARGStates> stateToStatesCollector = new HashMap<>();

  public ARGToYAMLWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  /**
   * A class to keep track of parent child relations between abstract states which enter a function
   * and those which exit it
   */
  protected record FunctionEntryExitPair(ARGState entry, ARGState exit) {}

  /**
   * A class to keep track of the result of the witness export, in particular to inform the caller
   * about some internals of the translation and export.
   *
   * @param translationAlwaysSuccessful if the translation from internal ARG states to strings was
   *     always successful
   */
  public record WitnessExportResult(boolean translationAlwaysSuccessful) {}

  /**
   * A class to keep track of the result of the creation of an invariant, in particular to inform
   * the caller about some internals of the translation and export.
   *
   * @param invariantEntry the invariant entry which was created
   * @param translationSuccessful if the translation from internal ARG states to strings was
   *     successful
   */
  record InvariantCreationResult(InvariantEntry invariantEntry, boolean translationSuccessful) {}

  /**
   * A class to keep track of the result of the creation of a function contract, in particular to
   * inform the caller about some internals of the translation and export.
   *
   * @param functionContractEntry the function contract entry which was created
   * @param translationSuccessful if the translation from internal ARG states to strings was
   *     successful
   */
  record FunctionContractCreationResult(
      FunctionContractEntry functionContractEntry, boolean translationSuccessful) {}

  /**
   * A class to keep track of the result of the creation of an expression tree, in particular to
   * keep track if this expression tree was successfully generated or not.
   *
   * @param expressionTree the expression tree which was created
   * @param backTranslationSuccessful if the back translation from the abstract state to an
   *     ExpressionTree was successful or if a fallback is being used
   */
  record ExpressionTreeResult(
      ExpressionTree<Object> expressionTree, boolean backTranslationSuccessful) {}

  /** A data structure for collecting the relevant information for a witness from an ARG */
  protected static class CollectedARGStates {
    public Multimap<CFANode, ARGState> loopInvariants = HashMultimap.create();
    public Multimap<CFANode, ARGState> functionCallInvariants = HashMultimap.create();
    public Multimap<FunctionEntryNode, ARGState> functionContractRequires = HashMultimap.create();
    public Multimap<FunctionExitNode, FunctionEntryExitPair> functionContractEnsures =
        HashMultimap.create();

    /**
     * Maps {@link FileLocation}s (to prevent duplicates from cloned functions) to {@link ARGState}
     * pairs from {@link ThreadingCPA} that are connected through lock operations.
     */
    public BiMap<FileLocation, ARGStatePair> lockUpdates = HashBiMap.create();

    /**
     * Maps {@link FileLocation}s (to prevent duplicates from cloned functions) to {@link ARGState}
     * pairs from {@link ThreadingCPA} that are connected through unlock operations.
     */
    public BiMap<FileLocation, ARGStatePair> unlockUpdates = HashBiMap.create();

    public record ARGStatePair(@NonNull ARGState parent, @NonNull ARGState child) {}
  }

  /**
   * Analyzes the ARG during its traversal by collecting the states relevant to exporting a witness
   */
  private static class RelevantARGStateCollector {

    private final CollectedARGStates collectedStates = new CollectedARGStates();

    // TODO: This needs to be improved once we implement setjump/longjump
    /** The callstack of the order in which the function entry points where traversed */
    private ListMultimap<AFunctionDeclaration, ARGState> functionEntryStatesCallStack =
        ArrayListMultimap.create();

    /** Enables the recovery of the callstack when an ARGState has multiple children */
    private final Map<ARGState, ListMultimap<AFunctionDeclaration, ARGState>> callStackRecovery =
        new HashMap<>();

    protected void analyze(ARGState pSuccessor) {
      if (!pSuccessor.getParents().isEmpty()) {
        ARGState parent = pSuccessor.getParents().stream().findFirst().orElseThrow();
        if (callStackRecovery.containsKey(parent)) {
          // Copy the saved callstack, since we want to return to the state we had before the
          // branching
          functionEntryStatesCallStack = ArrayListMultimap.create(callStackRecovery.get(parent));
        }
      }

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
          functionEntryStatesCallStack.put(functionEntryNode.getFunctionDefinition(), pSuccessor);
          collectedStates.functionContractRequires.put(functionEntryNode, pSuccessor);
        } else if (node instanceof FunctionExitNode functionExitNode) {
          List<ARGState> functionEntryNodes = functionEntryStatesCallStack.get(node.getFunction());
          Verify.verify(!functionEntryNodes.isEmpty());
          collectedStates.functionContractEnsures.put(
              functionExitNode,
              new FunctionEntryExitPair(
                  functionEntryNodes.remove(functionEntryNodes.size() - 1), pSuccessor));
        }

        if (pSuccessor.getChildren().size() > 1 && !callStackRecovery.containsKey(pSuccessor)) {
          callStackRecovery.put(pSuccessor, ArrayListMultimap.create(functionEntryStatesCallStack));
        }
      }

      Optional<ThreadingState> child = ARGUtils.tryExtractThreadingState(pSuccessor);
      if (child.isPresent()) {
        for (ARGState argParent : pSuccessor.getParents()) {
          ThreadingState parent = ARGUtils.tryExtractThreadingState(argParent).orElseThrow();
          ImmutableSet<String> parentLocks = parent.getLockIdsFromInputProgram();
          ImmutableSet<String> childLocks = child.orElseThrow().getLockIdsFromInputProgram();
          // locks unequal -> a lock / unlock operation was performed between states
          if (!parentLocks.equals(childLocks)) {
            // we later need the edge for the location and function
            CFAEdge edge = argParent.getEdgeToChild(pSuccessor);
            assert edge != null : "no edge found connecting parent and child";
            FileLocation location = edge.getFileLocation();
            ARGStatePair pair = new ARGStatePair(argParent, pSuccessor);
            int parentLocksNum = parentLocks.size();
            int childLocksNum = childLocks.size();
            if (parentLocksNum + 1 == childLocksNum) {
              // more locks in child -> lock was locked
              if (!collectedStates.lockUpdates.containsKey(location)) {
                collectedStates.lockUpdates.put(location, pair);
              }
            } else if (parentLocksNum == childLocksNum + 1) {
              // more locks in parent -> lock was unlocked
              if (!collectedStates.unlockUpdates.containsKey(location)) {
                collectedStates.unlockUpdates.put(location, pair);
              }
            } else {
              throw new AssertionError("multiple global locks within one operation");
            }
          }
        }
      }
    }

    public CollectedARGStates getCollectedStates() {
      return collectedStates;
    }
  }

  /** How to traverse the ARG */
  private static class ARGSuccessorFunction implements SuccessorsFunction<ARGState> {

    @Override
    public Iterable<ARGState> successors(ARGState node) {
      return node.getChildren();
    }
  }

  /**
   * Cache the information collected when traversing the ARG starting at the given state.
   *
   * @param pRootState the state for where the traversal of the ARG should start for the collection
   *     of the information
   * @return the collected information about the ARG
   */
  CollectedARGStates getRelevantStates(ARGState pRootState) {
    if (!stateToStatesCollector.containsKey(pRootState)) {
      RelevantARGStateCollector statesCollector = new RelevantARGStateCollector();
      for (ARGState state :
          Traverser.forGraph(new ARGSuccessorFunction()).depthFirstPreOrder(pRootState)) {
        statesCollector.analyze(state);
      }

      stateToStatesCollector.put(pRootState, statesCollector.getCollectedStates());
    }

    return stateToStatesCollector.get(pRootState);
  }

  /**
   * This is a wrapper for the function type to also throw {@link InterruptedException} and {@link
   * ReportingMethodNotImplementedException}. This is inspired by: <a
   * href="https://stackoverflow.com/questions/18198176/java-8-lambda-function-that-throws-exception">https://stackoverflow.com/questions/18198176/java-8-lambda-function-that-throws-exception</a>
   *
   * @param <T> the type of the input parameter
   * @param <R> the type of the return value
   */
  @FunctionalInterface
  public interface NotImplementedThrowingFunction<T, R> {
    R apply(T t)
        throws InterruptedException,
            ReportingMethodNotImplementedException,
            TranslationToExpressionTreeFailedException;
  }

  protected ExpressionTreeResult getOverapproximationOfStatesIgnoringReturnVariables(
      Collection<ARGState> argStates, CFANode node, boolean useOldKeywordForVariables)
      throws InterruptedException, ReportingMethodNotImplementedException {
    FunctionEntryNode entryNode = cfa.getFunctionHead(node.getFunctionName());
    return getOverapproximationOfStates(
        argStates,
        (ExpressionTreeReportingState x) ->
            x.getFormulaApproximationInputProgramInScopeVariables(
                entryNode, node, cfa.getAstCfaRelation(), useOldKeywordForVariables));
  }

  protected ExpressionTreeResult getOverapproximationOfStatesWithOnlyReturnVariables(
      Collection<ARGState> argStates, CFANode node)
      throws InterruptedException, ReportingMethodNotImplementedException {
    AIdExpression returnVariable;
    if (node.getFunction().getType().getReturnType() instanceof CType cType) {
      if (cType instanceof CVoidType) {
        return new ExpressionTreeResult(ExpressionTrees.getTrue(), true);
      }
      returnVariable =
          new CIdExpression(
              FileLocation.DUMMY,
              new CVariableDeclaration(
                  FileLocation.DUMMY,
                  false,
                  CStorageClass.AUTO,
                  cType,
                  "\\result",
                  "\\result",
                  node.getFunctionName() + "::\\result",
                  null));
    } else {
      // Currently we do not export witnesses for other programming languages than C, therefore
      // everything else is currently not supported.
      throw new UnsupportedOperationException();
    }

    FunctionEntryNode entryNode = cfa.getFunctionHead(node.getFunctionName());
    return getOverapproximationOfStates(
        argStates,
        (ExpressionTreeReportingState x) ->
            x.getFormulaApproximationFunctionReturnVariableOnly(entryNode, returnVariable));
  }

  /**
   * Provides an overapproximation of the abstractions encoded by the arg states at the location of
   * the node.
   *
   * @param pArgStates the arg states encoding abstractions of the state
   * @return an over approximation of the abstraction at the state
   * @throws InterruptedException if the call to this function is interrupted
   */
  private ExpressionTreeResult getOverapproximationOfStates(
      Collection<ARGState> pArgStates,
      NotImplementedThrowingFunction<ExpressionTreeReportingState, ExpressionTree<Object>>
          pStateToAbstraction)
      throws InterruptedException, ReportingMethodNotImplementedException {
    FluentIterable<ExpressionTreeReportingState> reportingStates =
        FluentIterable.from(pArgStates)
            .transformAndConcat(AbstractStates::asIterable)
            .filter(ExpressionTreeReportingState.class);
    List<List<ExpressionTreeResult>> expressionsPerClass = new ArrayList<>();

    for (Class<?> stateClass : reportingStates.transform(AbstractState::getClass).toSet()) {
      List<ExpressionTreeResult> expressionsMatchingClass = new ArrayList<>();
      for (ExpressionTreeReportingState state : reportingStates) {
        if (stateClass.isAssignableFrom(state.getClass())) {
          ExpressionTreeResult expressionTreeResult;
          try {
            expressionTreeResult = new ExpressionTreeResult(pStateToAbstraction.apply(state), true);
          } catch (TranslationToExpressionTreeFailedException e) {
            logger.logDebugException(e, "Could not translate state to expression tree");
            expressionTreeResult = new ExpressionTreeResult(ExpressionTrees.getTrue(), false);
          }
          expressionsMatchingClass.add(expressionTreeResult);
        }
      }
      expressionsPerClass.add(expressionsMatchingClass);
    }

    ExpressionTree<Object> overapproximationOfState =
        And.of(
            FluentIterable.from(expressionsPerClass)
                .transform(
                    elementsForClass ->
                        FluentIterable.from(elementsForClass)
                            .transform(ExpressionTreeResult::expressionTree))
                .transform(Or::of));
    boolean backTranslationSuccessful =
        expressionsPerClass.stream()
            .allMatch(
                elementsForClass ->
                    elementsForClass.stream()
                        .allMatch(ExpressionTreeResult::backTranslationSuccessful));

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
      logger.log(Level.FINE, "Could not remove CPAchecker internal variables from invariant");
    }

    return new ExpressionTreeResult(overapproximationOfState, backTranslationSuccessful);
  }

  /**
   * Create an invariant for the abstractions encoded by the {@link ARGState}s
   *
   * @param argStates the arg states encoding abstractions of the state
   * @param node the node at whose location the state should be over approximated
   * @param type the type of the invariant. Currently only `loop_invariant` and `location_invariant`
   *     are supported
   * @return an invariant over approximating the abstraction at the state
   * @throws InterruptedException if the execution is interrupted
   */
  protected InvariantCreationResult createInvariant(
      Collection<ARGState> argStates, CFANode node, String type)
      throws InterruptedException, ReportingMethodNotImplementedException {

    // We now conjunct all the overapproximations of the states and export them as loop invariants
    Optional<IterationElement> iterationStructure =
        getASTStructure().getTightestIterationStructureForNode(node);
    if (iterationStructure.isEmpty()) {
      return null;
    }

    FileLocation fileLocation = iterationStructure.orElseThrow().getCompleteElement().location();
    ExpressionTreeResult invariantResult =
        getOverapproximationOfStatesIgnoringReturnVariables(
            argStates, node, /* useOldKeywordForVariables= */ false);
    LocationRecord locationRecord =
        LocationRecord.createLocationRecordAtStart(
            fileLocation,
            node.getFunction().getFileLocation().getFileName().toString(),
            node.getFunctionName());

    InvariantEntry invariantEntry =
        new InvariantEntry(
            invariantResult.expressionTree().toString(),
            type,
            YAMLWitnessExpressionType.C,
            locationRecord);

    return new InvariantCreationResult(invariantEntry, invariantResult.backTranslationSuccessful());
  }
}
