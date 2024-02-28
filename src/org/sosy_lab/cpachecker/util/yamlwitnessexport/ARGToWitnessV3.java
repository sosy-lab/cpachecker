// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.ast.IterationStructure;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.CorrectnessWitnessSetElementEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.EnsuresRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionContractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntryV3;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.RequiresRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.CorrectnessWitnessSetEntry;

class ARGToWitnessV3 extends ARGToYAMLWitness {
  protected ARGToWitnessV3(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  private InvariantEntryV3 createInvariant(
      Collection<ARGState> argStates, CFANode node, String type)
      throws InterruptedException, YamlWitnessExportException {

    // We now conjunct all the overapproximations of the states and export them as loop invariants
    Optional<IterationStructure> iterationStructure =
        getASTStructure().getTightestIterationStructureForNode(node);
    if (iterationStructure.isEmpty()) {
      return null;
    }

    FileLocation fileLocation = iterationStructure.orElseThrow().getCompleteElement().location();
    ExpressionTree<Object> invariant = getOverapproximationOfStates(argStates, node);
    LocationRecord locationRecord =
        YAMLWitnessesExportUtils.createLocationRecordAtStart(
            fileLocation,
            node.getFunction().getFileLocation().getFileName().toString(),
            node.getFunctionName());

    InvariantEntryV3 invariantRecord =
        new InvariantEntryV3(
            invariant.toString(), type, YAMLWitnessExpressionType.C.toString(), locationRecord);

    return invariantRecord;
  }

  private List<FunctionContractEntry> handleFunctionContract(
      Multimap<FunctionEntryNode, ARGState> functionContractRequires,
      Multimap<FunctionExitNode, ARGState> functionContractEnsures)
      throws InterruptedException {
    List<FunctionContractEntry> functionContractRecords = new ArrayList<>();
    for (FunctionEntryNode node : functionContractRequires.keySet()) {
      Collection<ARGState> requiresArgStates = functionContractRequires.get(node);

      FileLocation location = node.getFileLocation();
      String requiresClause =
          getOverapproximationOfStates(requiresArgStates, node, true).toString();
      String ensuresClause = "1";
      if (node.getExitNode().isPresent()
          && functionContractEnsures.containsKey(node.getExitNode().orElseThrow())) {
        Collection<ARGState> ensuresArgStates =
            functionContractEnsures.get(node.getExitNode().orElseThrow());
        ensuresClause = getOverapproximationOfStates(ensuresArgStates, node, true).toString();
      }
      functionContractRecords.add(
          new FunctionContractEntry(
              new EnsuresRecord(ImmutableList.of(ensuresClause)),
              new RequiresRecord(ImmutableList.of(requiresClause)),
              YAMLWitnessExpressionType.C,
              YAMLWitnessesExportUtils.createLocationRecordAtStart(
                  location, node.getFunctionName())));
    }

    return functionContractRecords;
  }

  void exportWitness(ARGState pRootState, Path pOutputFile)
      throws YamlWitnessExportException, InterruptedException, IOException {
    // Collect the information about the states which contain the information about the invariants
    CollectedARGStates statesCollector = getRelevantStates(pRootState);

    Multimap<CFANode, ARGState> loopInvariants = statesCollector.loopInvariants;
    Multimap<CFANode, ARGState> functionCallInvariants = statesCollector.functionCallInvariants;

    // Use the collected states to generate invariants
    List<CorrectnessWitnessSetElementEntry> entries = new ArrayList<>();

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
        new CorrectnessWitnessSetEntry(getMetadata(YAMLWitnessVersion.V3), entries), pOutputFile);
  }
}
