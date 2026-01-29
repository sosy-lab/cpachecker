// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.FluentIterable;
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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
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
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionContractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;

class ARGToYAMLWitness extends AbstractYAMLWitnessExporter {

  private final Map<ARGState, CollectedARGStates> stateToStatesCollector = new HashMap<>();

  public ARGToYAMLWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
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
   * A class to keep track of the result of the witness export, in particular to inform the caller
   * about some internals of the translation and export.
   *
   * @param translationAlwaysSuccessful if the translation from internal ARG states to strings was
   *     always successful
   */
  public record WitnessExportResult(boolean translationAlwaysSuccessful) {}

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
   * Cache the information collected when traversing the ARG starting at the given state.
   *
   * @param pRootState the state for where the traversal of the ARG should start for the collection
   *     of the information
   * @return the collected information about the ARG
   */
  CollectedARGStates getRelevantStates(ARGState pRootState) {
    if (!stateToStatesCollector.containsKey(pRootState)) {
      stateToStatesCollector.put(
          pRootState, RelevantArgStatesCollector.getRelevantStates(pRootState));
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
      // Currently, we do not export witnesses for other programming languages than C, therefore
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
}
