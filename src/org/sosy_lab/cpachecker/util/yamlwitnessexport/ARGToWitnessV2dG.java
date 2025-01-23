// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.ReportingMethodNotImplementedException;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost.GhostInstrumentationContentRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost.GhostInstrumentationEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost.GhostUpdateRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost.GhostVariableRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost.InitialRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost.UpdateRecord;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
class ARGToWitnessV2dG extends ARGToYAMLWitness {

  public ARGToWitnessV2dG(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  /** Create a {@link GhostUpdateRecord} for a lock/unlock operation between pParent and pChild. */
  private GhostUpdateRecord createGhostUpdate(
      @NonNull ARGState pParent, @NonNull ARGState pChild, int pValue) {

    checkNotNull(pParent);
    checkNotNull(pChild);
    CFAEdge lockEdge = pParent.getEdgeToChild(pChild);
    checkArgument(lockEdge != null, "no edge connects pParent and pChild");
    LocationRecord locationRecord =
        LocationRecord.createLocationRecordAfterLocation(
            lockEdge.getFileLocation(),
            // using original name instead of cloned function name
            lockEdge.getSuccessor().getFunction().getOrigName(),
            cfa.getAstCfaRelation());
    // the format of ghost updates is currently always c_expression
    UpdateRecord updateRecord =
        new UpdateRecord(getLockId(pParent, pChild), pValue, YAMLWitnessExpressionType.C);
    return new GhostUpdateRecord(locationRecord, ImmutableList.of(updateRecord));
  }

  WitnessExportResult exportWitness(ARGState pRootState, Path pPath)
      throws IOException, ReportingMethodNotImplementedException, InterruptedException {
    // Collect the information about the states relevant for ghost variables
    CollectedARGStates statesCollector = getRelevantStates(pRootState);

    ImmutableList.Builder<GhostUpdateRecord> ghostUpdatesB = ImmutableList.builder();
    // Handle ghost updates through locks
    for (var entry : statesCollector.lockUpdates.entries()) {
      ghostUpdatesB.add(createGhostUpdate(entry.getKey(), entry.getValue(), 1));
    }
    // Handle ghost updates through unlocks
    for (var entry : statesCollector.unlockUpdates.entries()) {
      ghostUpdatesB.add(createGhostUpdate(entry.getKey(), entry.getValue(), 0));
    }
    ImmutableList<GhostUpdateRecord> ghostUpdates = ghostUpdatesB.build();

    // extract ghost variable names from ghostUpdates
    ImmutableSet<String> ghostVarNames =
        FluentIterable.from(ghostUpdates)
            .transformAndConcat(GhostUpdateRecord::updates)
            .transform(UpdateRecord::variable)
            .toSet();
    ImmutableList.Builder<GhostVariableRecord> ghostVariables = ImmutableList.builder();
    for (String varName : ghostVarNames) {
      // TODO initial value always 0? (yes for locks)
      // the format of an initial is currently always c_expression
      InitialRecord initial = new InitialRecord(0, YAMLWitnessExpressionType.C);
      // the scope of a ghost variable is always global
      ghostVariables.add(
          new GhostVariableRecord(varName, CBasicType.INT.toASTString(), "global", initial));
    }

    GhostInstrumentationContentRecord record =
        new GhostInstrumentationContentRecord(ghostVariables.build(), ghostUpdates);

    Multimap<CFANode, ARGState> loopInvariants = statesCollector.loopInvariants;
    Multimap<CFANode, ARGState> functionCallInvariants = statesCollector.functionCallInvariants;

    // TODO: use the collected states and ghost variables to generate invariants
    ImmutableList.Builder<AbstractInvariantEntry> invariantEntries = new ImmutableList.Builder<>();

    exportEntries(
        ImmutableList.of(
            new InvariantSetEntry(getMetadata(YAMLWitnessVersion.V2), invariantEntries.build()),
            new GhostInstrumentationEntry(getMetadata(YAMLWitnessVersion.V2dG), record)),
        pPath);

    return new WitnessExportResult(true);
  }

  /**
   * Returns the single lock id as used in {@link ThreadingState} that is updated between pParent
   * and pChild.
   */
  private @NonNull String getLockId(@NonNull ARGState pParent, @NonNull ARGState pChild) {
    checkNotNull(pParent);
    checkNotNull(pChild);
    ThreadingState parent = ARGUtils.extractSingleThreadingState(pParent);
    ThreadingState child = ARGUtils.extractSingleThreadingState(pChild);
    SetView<String> symmetricDifference =
        Sets.symmetricDifference(parent.getGlobalLockIds(), child.getGlobalLockIds());
    Verify.verify(
        symmetricDifference.size() == 1,
        "there must be exactly one lock update between pParent and pChild");
    String rLockId = symmetricDifference.iterator().next();
    Verify.verify(rLockId != null, "the updated lock cannot be null");
    return rLockId;
  }
}
