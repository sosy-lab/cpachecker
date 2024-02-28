// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

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
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.ast.IterationStructure;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
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

  private InvariantEntry createInvariant(Collection<ARGState> argStates, CFANode node, String type)
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
        LocationRecord.createLocationRecordAtStart(
            fileLocation,
            node.getFunction().getFileLocation().getFileName().toString(),
            node.getFunctionName());

    InvariantEntry invariantEntry =
        new InvariantEntry(
            invariant.toString(), type, YAMLWitnessExpressionType.C.toString(), locationRecord);

    return invariantEntry;
  }

  void exportWitnesses(ARGState pRootState, Path pPath)
      throws YamlWitnessExportException, InterruptedException, IOException {
    // Collect the information about the states which contain the information about the invariants
    CollectedARGStates statesCollector = getRelevantStates(pRootState);

    Multimap<CFANode, ARGState> loopInvariants = statesCollector.loopInvariants;
    Multimap<CFANode, ARGState> functionCallInvariants = statesCollector.functionCallInvariants;

    // Use the collected states to generate invariants
    List<InvariantEntry> entries = new ArrayList<>();

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

    exportEntries(new InvariantSetEntry(getMetadata(YAMLWitnessVersion.V2), entries), pPath);
  }
}
