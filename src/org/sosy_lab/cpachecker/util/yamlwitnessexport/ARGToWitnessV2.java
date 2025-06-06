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
import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.ReportingMethodNotImplementedException;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;

class ARGToWitnessV2 extends ARGToYAMLWitness {
  protected ARGToWitnessV2(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  /**
   * Create an invariant in the format for witnesses version 2.0 for the abstractions encoded by the
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

    // We now conjunct all the overapproximations of the states and export them as loop invariants
    Optional<IterationElement> iterationStructure =
        getASTStructure().getTightestIterationStructureForNode(node);
    if (iterationStructure.isEmpty()) {
      return null;
    }

    FileLocation fileLocation = iterationStructure.orElseThrow().getCompleteElement().location();
    // TODO: The original name of the variables should be used here. This requires a visitor to
    // rename them
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

  WitnessExportResult exportWitnesses(ARGState pRootState, Path pPath)
      throws InterruptedException, IOException, ReportingMethodNotImplementedException {
    // Collect the information about the states which contain the information about the invariants
    CollectedARGStates statesCollector = getRelevantStates(pRootState);

    Multimap<CFANode, ARGState> loopInvariants = statesCollector.loopInvariants;
    Multimap<CFANode, ARGState> functionCallInvariants = statesCollector.functionCallInvariants;

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

    // Handle the location invariants
    for (CFANode node : functionCallInvariants.keySet()) {
      Collection<ARGState> argStates = functionCallInvariants.get(node);
      InvariantCreationResult locationInvariant =
          createInvariant(argStates, node, InvariantRecordType.LOCATION_INVARIANT.getKeyword());
      if (locationInvariant != null) {
        entries.add(locationInvariant.invariantEntry());
        translationAlwaysSuccessful &= locationInvariant.translationSuccessful();
      }
    }

    exportEntries(
        new InvariantSetEntry(getMetadata(YAMLWitnessVersion.V2), entries.build()), pPath);

    return new WitnessExportResult(translationAlwaysSuccessful);
  }
}
