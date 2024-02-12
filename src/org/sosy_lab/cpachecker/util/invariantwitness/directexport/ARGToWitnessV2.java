// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.directexport;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
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
import org.sosy_lab.cpachecker.util.invariantwitness.directexport.DataTypes.ExpressionType;
import org.sosy_lab.cpachecker.util.invariantwitness.directexport.DataTypes.WitnessVersion;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantWitnessWriter.YamlWitnessExportException;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.InvariantSetEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InvariantRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InvariantRecord.InvariantRecordType;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.LocationRecord;

class ARGToWitnessV2 extends ARGToWitness {
  protected ARGToWitnessV2(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  private InvariantRecord createInvariant(Collection<ARGState> argStates, CFANode node, String type)
      throws InterruptedException, IOException, YamlWitnessExportException {
    ListMultimap<String, Integer> lineOffsetByLine = getlineOffsetsByFile();

    // We now conjunct all the overapproximations of the states and export them as loop invariants
    Optional<IterationStructure> iterationStructure =
        getASTStructure().getTightestIterationStructureForNode(node);
    if (iterationStructure.isEmpty()) {
      return null;
    }

    FileLocation fileLocation = iterationStructure.orElseThrow().getCompleteElement().location();
    ExpressionTree<Object> invariant = getOverapproximationOfStates(argStates, node);
    LocationRecord locationRecord =
        WitnessV2ExportUtils.createLocationRecordAtStart(
            fileLocation,
            lineOffsetByLine,
            node.getFunction().getFileLocation().getFileName().toString(),
            node.getFunctionName());

    InvariantRecord invariantRecord =
        new InvariantRecord(
            invariant.toString(), type, ExpressionType.C.toString(), locationRecord);

    return invariantRecord;
  }

  public void exportWitnesses(ARGState pRootState)
      throws YamlWitnessExportException, InterruptedException, IOException {
    // Collect the information about the states which contain the information about the invariants
    CollectedARGStates statesCollector = getRelevantStates(pRootState);

    Multimap<CFANode, ARGState> loopInvariants = statesCollector.loopInvariants;
    Multimap<CFANode, ARGState> functionCallInvariants = statesCollector.functionCallInvariants;

    // Use the collected states to generate invariants
    List<InvariantRecord> entries = new ArrayList<>();

    // First handle the loop invariants
    for (CFANode node : loopInvariants.keySet()) {
      Collection<ARGState> argStates = loopInvariants.get(node);
      InvariantRecord loopInvariant =
          createInvariant(argStates, node, InvariantRecordType.LOOP_INVARIANT.getKeyword());
      if (loopInvariant != null) {
        entries.add(loopInvariant);
      }
    }

    // Handle the location invariants
    for (CFANode node : functionCallInvariants.keySet()) {
      Collection<ARGState> argStates = functionCallInvariants.get(node);
      InvariantRecord locationInvariant =
          createInvariant(argStates, node, InvariantRecordType.LOCATION_INVARIANT.getKeyword());
      if (locationInvariant != null) {
        entries.add(locationInvariant);
      }
    }

    exportEntries(
        new InvariantSetEntry(getMetadata(WitnessVersion.V2), entries),
        getOutputFile(WitnessVersion.V2));
  }
}
