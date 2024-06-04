// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
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
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
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
   * arg states
   *
   * @param argStates the arg states encoding abstractions of the state
   * @param node the node at whose location the state should be over approximated
   * @param type the type of the invariant. Currently only `loop_invariant` and `location_invariant`
   *     are supported
   * @return an invariant over approximating the abstraction at the state
   * @throws InterruptedException if the execution is interrupted
   */
  private InvariantEntry createInvariant(Collection<ARGState> argStates, CFANode node, String type)
      throws InterruptedException, ReportingMethodNotImplementedException {

    // We now conjunct all the over approximations of the states and export them as loop invariants
    Optional<IterationElement> iterationStructure =
        getASTStructure().getTightestIterationStructureForNode(node);
    if (iterationStructure.isEmpty()) {
      return null;
    }

    FileLocation fileLocation = iterationStructure.orElseThrow().getCompleteElement().location();
    ExpressionTree<Object> invariant =
        getOverapproximationOfStatesIgnoringReturnVariables(argStates, node);
    LocationRecord locationRecord =
        LocationRecord.createLocationRecordAtStart(
            fileLocation,
            node.getFunction().getFileLocation().getFileName().toString(),
            node.getFunctionName());

    InvariantEntry invariantRecord =
        new InvariantEntry(invariant.toString(), type, YAMLWitnessExpressionType.C, locationRecord);

    return invariantRecord;
  }

  /**
   * Create function contracts for each of the functions whose entry nodes have been given
   *
   * @param functionContractRequires a mapping from function entry nodes to arg states encoding the
   *     abstractions at that location
   * @param functionContractEnsures a mapping from function exit nodes to arg states encoding the *
   *     abstractions at that location
   * @return a list of function contracts, one for each of the functions whose entry nodes have been
   *     given
   * @throws InterruptedException if the execution is interrupted
   */
  private ImmutableList<FunctionContractEntry> handleFunctionContract(
      Multimap<FunctionEntryNode, ARGState> functionContractRequires,
      Multimap<FunctionExitNode, FunctionEntryExitPair> functionContractEnsures)
      throws InterruptedException, ReportingMethodNotImplementedException {
    Builder<FunctionContractEntry> functionContractRecords = new Builder<>();
    for (FunctionEntryNode functionEntryNode : functionContractRequires.keySet()) {
      Collection<ARGState> requiresArgStates = functionContractRequires.get(functionEntryNode);

      FileLocation location = functionEntryNode.getFileLocation();
      String requiresClause =
          getOverapproximationOfStatesIgnoringReturnVariables(requiresArgStates, functionEntryNode)
              .toString();
      ImmutableSet.Builder<String> ensuresClause = new ImmutableSet.Builder<>();
      if (functionEntryNode.getExitNode().isPresent()
          && functionContractEnsures.containsKey(functionEntryNode.getExitNode().orElseThrow())) {
        Collection<FunctionEntryExitPair> ensuresArgStates =
            functionContractEnsures.get(functionEntryNode.getExitNode().orElseThrow());
        for (FunctionEntryExitPair pair : ensuresArgStates) {
          String stateOfTheInput =
              getOverapproximationOfStatesIgnoringReturnVariables(
                      ImmutableSet.of(pair.entry()), functionEntryNode)
                  .toString();
          String stateOfTheOutput =
              getOverapproximationOfStatesWithOnlyReturnVariables(
                      ImmutableSet.of(pair.exit()), functionEntryNode)
                  .toString();
          String implication = "(!(" + stateOfTheInput + ") || (" + stateOfTheOutput + "))";
          ensuresClause.add(implication);
        }
      } else {
        // If we do not have an exit node then we do not have any ensures clause
        ensuresClause.add("1");
      }
      functionContractRecords.add(
          new FunctionContractEntry(
              String.join(" && ", ensuresClause.build()),
              requiresClause,
              YAMLWitnessExpressionType.C,
              LocationRecord.createLocationRecordAtStart(
                  location, functionEntryNode.getFunctionName())));
    }

    return functionContractRecords.build();
  }

  void exportWitness(ARGState pRootState, Path pOutputFile)
      throws InterruptedException, IOException, ReportingMethodNotImplementedException {
    // Collect the information about the states which contain the information about the invariants
    CollectedARGStates statesCollector = getRelevantStates(pRootState);

    Multimap<CFANode, ARGState> loopInvariants = statesCollector.loopInvariants;
    Multimap<CFANode, ARGState> functionCallInvariants = statesCollector.functionCallInvariants;

    // Use the collected states to generate invariants
    ImmutableList.Builder<AbstractInvariantEntry> entries = new ImmutableList.Builder<>();

    // First handle the loop invariants
    for (CFANode node : loopInvariants.keySet()) {
      Collection<ARGState> argStates = loopInvariants.get(node);
      InvariantEntry loopInvariant =
          createInvariant(argStates, node, InvariantRecordType.LOOP_INVARIANT.getKeyword());
      if (loopInvariant != null) {
        entries.add(loopInvariant);
      }
    }

    // Handle the location invariants
    for (CFANode node : functionCallInvariants.keySet()) {
      Collection<ARGState> argStates = functionCallInvariants.get(node);
      InvariantEntry locationInvariant =
          createInvariant(argStates, node, InvariantRecordType.LOCATION_INVARIANT.getKeyword());
      if (locationInvariant != null) {
        entries.add(locationInvariant);
      }
    }

    // If we are exporting to witness version 3.0 then we want to include function contracts
    entries.addAll(
        handleFunctionContract(
            statesCollector.functionContractRequires, statesCollector.functionContractEnsures));

    exportEntries(
        new InvariantSetEntry(getMetadata(YAMLWitnessVersion.V2d1), entries.build()), pOutputFile);
  }
}
