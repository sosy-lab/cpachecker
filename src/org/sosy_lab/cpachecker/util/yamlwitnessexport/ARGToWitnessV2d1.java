// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.ReportingMethodNotImplementedException;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionContractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;

class ARGToWitnessV2d1 extends ARGToYAMLWitness {
  protected ARGToWitnessV2d1(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  /**
   * Create an invariant in the format for witnesses version 3.0 for the abstractions encoded by the
   * ARG states
   *
   * @param argStates the ARG states encoding abstractions of the state
   * @param node the node at whose location the state should be over approximated
   * @param type the type of the invariant. Currently, only `loop_invariant` and
   *     `location_invariant` are supported
   * @return an invariant over approximating the abstraction at the state
   * @throws InterruptedException if the execution is interrupted
   */
  private InvariantCreationResult createInvariant(
      Collection<ARGState> argStates, CFANode node, String type)
      throws InterruptedException, ReportingMethodNotImplementedException {

    // We now conjunct all the over approximations of the states and export them as loop invariants
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

    InvariantEntry invariantRecord =
        new InvariantEntry(
            invariantResult.expressionTree().toString(),
            type,
            YAMLWitnessExpressionType.C,
            locationRecord);

    return new InvariantCreationResult(
        invariantRecord, invariantResult.backTranslationSuccessful());
  }

  /**
   * Create function contracts for each of the functions whose entry nodes have been given
   *
   * @param functionContractRequires a mapping from function entry nodes to ARG states encoding the
   *     abstractions at that location
   * @param functionContractEnsures a mapping from function exit nodes to ARG states encoding the *
   *     abstractions at that location
   * @return a list of function contracts, one for each of the functions whose entry nodes have been
   *     given
   * @throws InterruptedException if the execution is interrupted
   */
  private ImmutableList<FunctionContractCreationResult> handleFunctionContract(
      Multimap<FunctionEntryNode, ARGState> functionContractRequires,
      Multimap<FunctionExitNode, FunctionEntryExitPair> functionContractEnsures)
      throws InterruptedException, ReportingMethodNotImplementedException {
    ImmutableList.Builder<FunctionContractCreationResult> functionContractRecords =
        new ImmutableList.Builder<>();

    for (FunctionEntryNode functionEntryNode : functionContractRequires.keySet()) {
      Collection<ARGState> requiresArgStates = functionContractRequires.get(functionEntryNode);
      boolean translationSuccessful = true;

      FileLocation location = functionEntryNode.getFileLocation();
      ExpressionTreeResult requiresClauseResult =
          getOverapproximationOfStatesIgnoringReturnVariables(
              requiresArgStates, functionEntryNode, /* useOldKeywordForVariables= */ false);
      String requiresClause = requiresClauseResult.expressionTree().toString();
      translationSuccessful &= requiresClauseResult.backTranslationSuccessful();

      ImmutableSet.Builder<String> ensuresClause = new ImmutableSet.Builder<>();
      if (functionEntryNode.getExitNode().isPresent()
          && functionContractEnsures.containsKey(functionEntryNode.getExitNode().orElseThrow())) {
        Collection<FunctionEntryExitPair> ensuresArgStates =
            functionContractEnsures.get(functionEntryNode.getExitNode().orElseThrow());
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
                  YAMLWitnessExpressionType.ACSL,
                  LocationRecord.createLocationRecordAtStart(
                      location, functionEntryNode.getFunctionName())),
              translationSuccessful));
    }

    return functionContractRecords.build();
  }

  WitnessExportResult exportWitness(ARGState pRootState, Path pOutputFile)
      throws InterruptedException, IOException, ReportingMethodNotImplementedException {
    // Collect the information about the states which contain the information about the invariants
    CollectedARGStates statesCollector = getRelevantStates(pRootState);

    Multimap<CFANode, ARGState> loopInvariants = statesCollector.loopInvariants;

    // Use the collected states to generate invariants
    ImmutableList.Builder<AbstractInvariantEntry> entries = new ImmutableList.Builder<>();
    boolean translationAlwaysSuccessful = true;

    // First handle the loop invariants
    for (CFANode node : loopInvariants.keySet()) {
      Collection<ARGState> argStates = loopInvariants.get(node);
      InvariantCreationResult loopInvariant =
          createInvariant(argStates, node, InvariantRecordType.LOOP_INVARIANT.getKeyword());
      if (loopInvariant != null) {
        entries.add(loopInvariant.invariantEntry());
        translationAlwaysSuccessful &= loopInvariant.translationSuccessful();
      }
    }

    // If we are exporting to witness version 3.0 then we want to include function contracts
    ImmutableList<FunctionContractCreationResult> functionContractCreationResult =
        handleFunctionContract(
            statesCollector.functionContractRequires, statesCollector.functionContractEnsures);
    entries.addAll(
        FluentIterable.from(functionContractCreationResult)
            .transform(FunctionContractCreationResult::functionContractEntry));

    translationAlwaysSuccessful &=
        FluentIterable.from(functionContractCreationResult)
            .allMatch(FunctionContractCreationResult::translationSuccessful);

    exportEntries(
        new InvariantSetEntry(getMetadata(YAMLWitnessVersion.V2d1), entries.build()), pOutputFile);

    return new WitnessExportResult(translationAlwaysSuccessful);
  }
}
