// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.expressions.RemovingStructuresVisitor;
import org.sosy_lab.cpachecker.util.witnesses.RelevantArgStatesCollector;
import org.sosy_lab.cpachecker.util.witnesses.RelevantArgStatesCollector.CollectedARGStates;
import org.sosy_lab.cpachecker.util.witnesses.RelevantArgStatesCollector.FunctionEntryExitPair;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionContractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;

final class ARGToYAMLWitness extends AbstractYAMLWitnessExporter {

  private final RelevantArgStatesCollector argStatesCollector;

  public ARGToYAMLWitness(
      Configuration pConfig,
      CFA pCfa,
      Specification pSpecification,
      LogManager pLogger,
      RelevantArgStatesCollector pArgStatesCollector)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
    argStatesCollector = pArgStatesCollector;
  }

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
   * A class to keep track of the result of the creation of an invariant, in particular to inform
   * the caller about some internals of the translation and export.
   *
   * @param invariantEntry the invariant entry which was created
   * @param translationSuccessful if the translation from internal ARG states to strings was
   *     successful
   */
  record InvariantCreationResult(InvariantEntry invariantEntry, boolean translationSuccessful) {}

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

  private ExpressionTreeResult getOverapproximationOfStatesIgnoringReturnVariables(
      Collection<ARGState> argStates, CFANode node, boolean useOldKeywordForVariables)
      throws InterruptedException, ReportingMethodNotImplementedException {
    FunctionEntryNode entryNode = cfa.getFunctionHead(node.getFunctionName());
    return getOverapproximationOfStates(
        argStates,
        (ExpressionTreeReportingState x) ->
            x.getFormulaApproximationInputProgramInScopeVariables(
                entryNode,
                node,
                cfa.getAstCfaRelation(),
                useOldKeywordForVariables,
                cfa.getMachineModel()));
  }

  private ExpressionTreeResult getOverapproximationOfStatesWithOnlyReturnVariables(
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
      // Currently, we do not export witnesses for other programming languages than C, therefore
      // everything else is currently not supported.
      throw new UnsupportedOperationException();
    }

    FunctionEntryNode entryNode = cfa.getFunctionHead(node.getFunctionName());
    return getOverapproximationOfStates(
        argStates,
        (ExpressionTreeReportingState x) ->
            x.getFormulaApproximationFunctionReturnVariableOnly(
                entryNode, returnVariable, cfa.getMachineModel()));
  }

  /**
   * Provides an overapproximation of the abstractions encoded by the ARG states at the location of
   * the node.
   *
   * @param pArgStates the ARG states encoding abstractions of the state
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
   * The entries created for one ARG, grouped by the kind of information they contain.
   *
   * @param entriesPerKind the created entries for each kind
   * @param kindsWithFailedTranslation the kinds for which at least one translation from internal
   *     ARG states to strings was not successful
   */
  record CollectedInvariants(
      ImmutableListMultimap<WitnessInvariantKind, AbstractInvariantEntry> entriesPerKind,
      ImmutableSet<WitnessInvariantKind> kindsWithFailedTranslation) {

    /** The entries of the given kinds, in the declaration order of {@link WitnessInvariantKind}. */
    ImmutableList<AbstractInvariantEntry> entriesFor(Set<WitnessInvariantKind> pKinds) {
      ImmutableList.Builder<AbstractInvariantEntry> entries = ImmutableList.builder();
      for (WitnessInvariantKind kind : WitnessInvariantKind.values()) {
        if (pKinds.contains(kind)) {
          entries.addAll(entriesPerKind.get(kind));
        }
      }
      return entries.build();
    }

    boolean translationAlwaysSuccessfulFor(Set<WitnessInvariantKind> pKinds) {
      return Collections.disjoint(kindsWithFailedTranslation, pKinds);
    }
  }

  /**
   * Traverse the ARG and create the entries for the requested kinds of information.
   *
   * <p>The ARG is traversed only once, independently of how many witness versions are exported from
   * the result.
   *
   * @param pRootState the root state of the ARG
   * @param pKinds the kinds of information which should be created
   * @return the created entries
   * @throws InterruptedException if the execution is interrupted
   */
  CollectedInvariants createInvariantEntries(ARGState pRootState, Set<WitnessInvariantKind> pKinds)
      throws InterruptedException, ReportingMethodNotImplementedException {
    CollectedARGStates statesCollector = argStatesCollector.getRelevantStates(pRootState);

    ImmutableListMultimap.Builder<WitnessInvariantKind, AbstractInvariantEntry> entries =
        ImmutableListMultimap.builder();
    ImmutableSet.Builder<WitnessInvariantKind> kindsWithFailedTranslation = ImmutableSet.builder();

    if (pKinds.contains(WitnessInvariantKind.LOOP_INVARIANT)) {
      collectInvariants(
          statesCollector.loopInvariants(),
          InvariantRecordType.LOOP_INVARIANT,
          entries,
          kindsWithFailedTranslation);
    }

    if (pKinds.contains(WitnessInvariantKind.LOCATION_INVARIANT)) {
      collectInvariants(
          statesCollector.functionCallInvariants(),
          InvariantRecordType.LOCATION_INVARIANT,
          entries,
          kindsWithFailedTranslation);
    }

    if (pKinds.contains(WitnessInvariantKind.FUNCTION_CONTRACT)) {
      ImmutableList<FunctionContractCreationResult> contracts =
          createFunctionContracts(
              statesCollector.functionContractRequires(),
              statesCollector.functionContractEnsures());
      entries.putAll(
          WitnessInvariantKind.FUNCTION_CONTRACT,
          FluentIterable.from(contracts)
              .transform(FunctionContractCreationResult::functionContractEntry));
      if (!FluentIterable.from(contracts)
          .allMatch(FunctionContractCreationResult::translationSuccessful)) {
        kindsWithFailedTranslation.add(WitnessInvariantKind.FUNCTION_CONTRACT);
      }
    }

    return new CollectedInvariants(entries.build(), kindsWithFailedTranslation.build());
  }

  /**
   * Create the invariants of the given type for the states relevant to each of the given nodes, and
   * add them to the entries.
   *
   * @param pStates the ARG states to over approximate, per node
   * @param pType the type of the invariants to create
   * @param pEntries where to add the created invariants
   * @param pKindsWithFailedTranslation where to note the kind if a translation was not successful
   * @throws InterruptedException if the execution is interrupted
   */
  private void collectInvariants(
      Multimap<CFANode, ARGState> pStates,
      InvariantRecordType pType,
      ImmutableListMultimap.Builder<WitnessInvariantKind, AbstractInvariantEntry> pEntries,
      ImmutableSet.Builder<WitnessInvariantKind> pKindsWithFailedTranslation)
      throws InterruptedException, ReportingMethodNotImplementedException {
    WitnessInvariantKind kind = WitnessInvariantKind.of(pType).orElseThrow();
    boolean translationSuccessful = true;
    for (CFANode node : pStates.keySet()) {
      Optional<FileLocation> location = locationOfInvariant(node, pType);
      if (location.isEmpty()) {
        logger.logf(
            Level.FINE, "Could not determine the location of node %s, skipping its %s", node, kind);
        continue;
      }
      InvariantCreationResult invariant =
          createInvariant(pStates.get(node), node, pType, location.orElseThrow());
      pEntries.put(kind, invariant.invariantEntry());
      translationSuccessful &= invariant.translationSuccessful();
    }
    if (!translationSuccessful) {
      pKindsWithFailedTranslation.add(kind);
    }
  }

  /**
   * The location in the input program an invariant of the given type at the given node belongs to.
   * A loop invariant belongs to its loop, a location invariant to the statement containing the
   * node, which is also the statement the validator resolves such an invariant to.
   */
  private Optional<FileLocation> locationOfInvariant(CFANode pNode, InvariantRecordType pType) {
    return switch (pType) {
      case LOOP_INVARIANT ->
          getASTStructure()
              .getTightestIterationStructureForNode(pNode)
              .map(iteration -> iteration.getCompleteElement().location());
      case LOCATION_INVARIANT -> getASTStructure().getStatementFileLocationForNode(pNode);
      default -> throw new AssertionError("Cannot export invariants of type " + pType);
    };
  }

  /**
   * Create an invariant for the abstractions encoded by the ARG states.
   *
   * @param pArgStates the ARG states encoding abstractions of the state
   * @param pNode the node at whose location the states should be over approximated
   * @param pType the type of the invariant
   * @param pLocation the location in the input program the invariant belongs to
   * @return an invariant over approximating the abstraction at the state
   * @throws InterruptedException if the execution is interrupted
   */
  private InvariantCreationResult createInvariant(
      Collection<ARGState> pArgStates,
      CFANode pNode,
      InvariantRecordType pType,
      FileLocation pLocation)
      throws InterruptedException, ReportingMethodNotImplementedException {
    // TODO: The original name of the variables should be used here. This requires a visitor to
    // rename them
    ExpressionTreeResult invariantResult =
        getOverapproximationOfStatesIgnoringReturnVariables(
            pArgStates, pNode, /* useOldKeywordForVariables= */ false);
    LocationRecord locationRecord =
        LocationRecord.createLocationRecordAtStart(
            pLocation,
            pNode.getFunction().getFileLocation().getFileName().toString(),
            pNode.getFunction().getOrigName());

    return new InvariantCreationResult(
        new InvariantEntry(
            invariantResult.expressionTree().toString(),
            pType.getKeyword(),
            YAMLWitnessExpressionType.C,
            locationRecord),
        invariantResult.backTranslationSuccessful());
  }

  /**
   * Create function contracts for each of the functions whose entry nodes have been given
   *
   * @param pFunctionContractRequires a mapping from function entry nodes to ARG states encoding the
   *     abstractions at that location
   * @param pFunctionContractEnsures a mapping from function exit nodes to ARG states encoding the
   *     abstractions at that location
   * @return a list of function contracts, one for each of the functions whose entry nodes have been
   *     given
   * @throws InterruptedException if the execution is interrupted
   */
  private ImmutableList<FunctionContractCreationResult> createFunctionContracts(
      Multimap<FunctionEntryNode, ARGState> pFunctionContractRequires,
      Multimap<FunctionExitNode, FunctionEntryExitPair> pFunctionContractEnsures)
      throws InterruptedException, ReportingMethodNotImplementedException {
    ImmutableList.Builder<FunctionContractCreationResult> functionContractRecords =
        new ImmutableList.Builder<>();

    for (FunctionEntryNode functionEntryNode : pFunctionContractRequires.keySet()) {
      Collection<ARGState> requiresArgStates = pFunctionContractRequires.get(functionEntryNode);
      boolean translationSuccessful = true;

      FileLocation location = functionEntryNode.getFileLocation();
      ExpressionTreeResult requiresClauseResult =
          getOverapproximationOfStatesIgnoringReturnVariables(
              requiresArgStates, functionEntryNode, /* useOldKeywordForVariables= */ false);
      String requiresClause = requiresClauseResult.expressionTree().toString();
      translationSuccessful &= requiresClauseResult.backTranslationSuccessful();

      ImmutableSet.Builder<String> ensuresClause = new ImmutableSet.Builder<>();
      if (functionEntryNode.getExitNode().isPresent()
          && pFunctionContractEnsures.containsKey(functionEntryNode.getExitNode().orElseThrow())) {
        Collection<FunctionEntryExitPair> ensuresArgStates =
            pFunctionContractEnsures.get(functionEntryNode.getExitNode().orElseThrow());
        for (FunctionEntryExitPair pair : ensuresArgStates) {
          // Get the state of the input of the function
          ExpressionTreeResult stateOfTheInputResult =
              getOverapproximationOfStatesIgnoringReturnVariables(
                  ImmutableSet.of(pair.entry()),
                  functionEntryNode,
                  // we need to use the old keyword to reference the variables in the input.
                  /* useOldKeywordForVariables= */ true);

          String stateOfTheInput = stateOfTheInputResult.expressionTree().toString();
          translationSuccessful &= stateOfTheInputResult.backTranslationSuccessful();

          // Get the state of the output of the function
          ExpressionTreeResult stateOfTheOutputResult =
              getOverapproximationOfStatesWithOnlyReturnVariables(
                  ImmutableSet.of(pair.exit()), functionEntryNode);
          String stateOfTheOutput = stateOfTheOutputResult.expressionTree().toString();
          translationSuccessful &= stateOfTheOutputResult.backTranslationSuccessful();

          // Create a relation between the input and the output of the function
          String implication = "(!(" + stateOfTheInput + ") || (" + stateOfTheOutput + "))";
          ensuresClause.add(implication);
        }
      } else {
        // If we do not have an exit node then we do not have any ensures clause
        ensuresClause.add("1");
      }
      functionContractRecords.add(
          new FunctionContractCreationResult(
              new FunctionContractEntry(
                  String.join(" && ", ensuresClause.build()),
                  requiresClause,
                  // The format of contract expressions is always ext_c_expression
                  YAMLWitnessExpressionType.EXT_C,
                  LocationRecord.createLocationRecordAtStart(
                      location, functionEntryNode.getFunction().getOrigName())),
              translationSuccessful));
    }

    return functionContractRecords.build();
  }
}
