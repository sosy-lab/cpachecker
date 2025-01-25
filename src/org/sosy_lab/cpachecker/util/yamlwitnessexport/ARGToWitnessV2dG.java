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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.ReportingMethodNotImplementedException;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
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

  private static final Comparator<GhostUpdateRecord> ghostUpdateComparator =
      Comparator.comparingInt((GhostUpdateRecord ghostUpdate) -> ghostUpdate.location().getLine())
          .thenComparingInt(ghostUpdate -> ghostUpdate.location().getColumn());

  public ARGToWitnessV2dG(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  WitnessExportResult exportWitness(ARGState pRootState, Path pPath)
      throws IOException, ReportingMethodNotImplementedException, InterruptedException {
    // collect the information about the states relevant for ghost variables
    CollectedARGStates statesCollector = getRelevantStates(pRootState);

    // first create ghost variables and their respective ghost updates
    ImmutableList<GhostUpdateRecord> ghostUpdates =
        getGhostUpdatesFromStateCollector(statesCollector);
    GhostInstrumentationContentRecord record =
        new GhostInstrumentationContentRecord(
            getGhostVariablesFromGhostUpdates(ghostUpdates), ghostUpdates);

    // second use collected states to generate invariants
    Multimap<CFANode, ARGState> loopInvariants = statesCollector.loopInvariants;
    Multimap<CFANode, ARGState> functionCallInvariants = statesCollector.functionCallInvariants;

    // TODO: also include ghost variables in the invariants
    ImmutableList.Builder<AbstractInvariantEntry> invariantEntries = new ImmutableList.Builder<>();
    boolean translationAlwaysSuccessful = true;

    // handle the loop invariants
    for (CFANode node : loopInvariants.keySet()) {
      Collection<ARGState> argStates = loopInvariants.get(node);
      InvariantCreationResult loopInvariant =
          createInvariant(argStates, node, InvariantRecordType.LOOP_INVARIANT.getKeyword());
      if (loopInvariant != null) {
        invariantEntries.add(loopInvariant.invariantEntry());
        translationAlwaysSuccessful &= loopInvariant.translationSuccessful();
      }
    }

    // handle the location invariants
    for (CFANode node : functionCallInvariants.keySet()) {
      Collection<ARGState> argStates = functionCallInvariants.get(node);
      InvariantCreationResult locationInvariant =
          createInvariant(argStates, node, InvariantRecordType.LOCATION_INVARIANT.getKeyword());
      if (locationInvariant != null) {
        invariantEntries.add(locationInvariant.invariantEntry());
        translationAlwaysSuccessful &= locationInvariant.translationSuccessful();
      }
    }

    exportEntries(
        ImmutableList.of(
            new InvariantSetEntry(getMetadata(YAMLWitnessVersion.V2), invariantEntries.build()),
            new GhostInstrumentationEntry(getMetadata(YAMLWitnessVersion.V2dG), record)),
        pPath);

    return new WitnessExportResult(translationAlwaysSuccessful);
  }

  /**
   * Creates {@link GhostUpdateRecord}s from the collected {@link ARGState} and sorts them by line
   * then column.
   */
  private ImmutableList<GhostUpdateRecord> getGhostUpdatesFromStateCollector(
      @NonNull CollectedARGStates pStatesCollector) {

    checkNotNull(pStatesCollector);
    ImmutableList.Builder<GhostUpdateRecord> ghostUpdates = ImmutableList.builder();
    // handle ghost updates through locks
    for (var entry : getUniqueEdgeEntries(pStatesCollector.lockUpdates).entries()) {
      ghostUpdates.add(createGhostUpdate(entry.getKey(), entry.getValue(), 1));
    }
    // handle ghost updates through unlocks
    for (var entry : getUniqueEdgeEntries(pStatesCollector.unlockUpdates).entries()) {
      ghostUpdates.add(createGhostUpdate(entry.getKey(), entry.getValue(), 0));
    }
    return FluentIterable.from(ghostUpdates.build()).toSortedList(ghostUpdateComparator);
  }

  /** Create a {@link GhostUpdateRecord} for a lock/unlock operation between pParent and pChild. */
  private GhostUpdateRecord createGhostUpdate(
      @NonNull ARGState pParent, @NonNull ARGState pChild, int pValue) {

    checkNotNull(pParent);
    checkNotNull(pChild);
    CFAEdge lockEdge = pParent.getEdgeToChild(pChild);
    checkArgument(lockEdge != null, "no edge connects pParent and pChild");
    // ghost updates always commute with the lock statement -> can put it at end / start
    LocationRecord locationRecord =
        LocationRecord.createLocationRecordAtStart(
            lockEdge.getFileLocation(),
            // using original name instead of cloned function name
            lockEdge.getSuccessor().getFunction().getOrigName());
    // the format of ghost updates is currently always c_expression
    UpdateRecord updateRecord =
        new UpdateRecord(getLockId(pParent, pChild), pValue, YAMLWitnessExpressionType.C);
    return new GhostUpdateRecord(locationRecord, ImmutableList.of(updateRecord));
  }

  /**
   * Extracts the ghost variables from pGhostUpdates. Every variable is present once even with
   * multiple {@link GhostUpdateRecord}s to it.
   */
  private ImmutableList<GhostVariableRecord> getGhostVariablesFromGhostUpdates(
      @NonNull ImmutableList<GhostUpdateRecord> pGhostUpdates) {

    checkNotNull(pGhostUpdates);
    // extract ghost variable names from ghostUpdates
    ImmutableSet<String> ghostVarNames =
        FluentIterable.from(pGhostUpdates)
            .transformAndConcat(GhostUpdateRecord::updates)
            .transform(UpdateRecord::variable)
            .toSet();
    return FluentIterable.from(ghostVarNames)
        .transform(
            varName -> {
              // TODO initial value always 0? (yes for locks)
              // the format of an initial is currently always c_expression
              InitialRecord initial = new InitialRecord(0, YAMLWitnessExpressionType.C);
              // the scope of a ghost variable is always global
              return new GhostVariableRecord(
                  varName, CBasicType.INT.toASTString(), "global", initial);
            })
        .toList();
  }

  /**
   * Returns the subset of entries in pUpdates where the {@link CFAEdge}s linking {@link ARGState}s
   * are present exactly once.
   *
   * <p>E.g. pUpdates := {@code {(argA, argB), (argC, argD)}} where both are connected through
   * {@code edgeE}, then return just {@code {(argA, argB)}} or {@code {(argC, argD)}} depending on
   * the order.
   *
   * <p>Also ensures that the lockUpdates for all parent / child pairs of {@link ARGState}s
   * connected through the same {@link CFAEdge} are equal so that they are equal w.r.t. {@link
   * GhostUpdateRecord} semantics.
   */
  private Multimap<ARGState, ARGState> getUniqueEdgeEntries(
      @NonNull Multimap<ARGState, ARGState> pUpdates) {

    checkNotNull(pUpdates);
    Multimap<ARGState, ARGState> rEntries = HashMultimap.create();
    Map<CFAEdge, Multimap<ARGState, ARGState>> visitedEdges = new HashMap<>();
    for (var entry : pUpdates.entries()) {
      ARGState argParent = entry.getKey();
      ARGState argChild = entry.getValue();
      CFAEdge edge = argParent.getEdgeToChild(argChild);
      if (!visitedEdges.containsKey(edge)) {
        visitedEdges.put(edge, HashMultimap.create());
        visitedEdges.get(edge).put(argParent, argChild);
        rEntries.put(argParent, argChild);
      } else {
        visitedEdges.get(edge).put(argParent, argChild);
      }
    }
    return rEntries;
  }

  /**
   * Returns the single lock id as used in {@link ThreadingState} that is updated between pParent
   * and pChild.
   */
  private @NonNull String getLockId(@NonNull ARGState pParent, @NonNull ARGState pChild) {
    checkNotNull(pParent);
    checkNotNull(pChild);
    ThreadingState parent = ARGUtils.tryExtractThreadingState(pParent).orElseThrow();
    ThreadingState child = ARGUtils.tryExtractThreadingState(pChild).orElseThrow();
    SetView<String> symmetricDifference =
        Sets.symmetricDifference(parent.getGlobalLockIds(), child.getGlobalLockIds());
    Verify.verify(
        symmetricDifference.size() == 1,
        "there must be exactly one lock update between pParent and pChild");
    String rLockId = symmetricDifference.iterator().next();
    Verify.verify(rLockId != null, "the updated lock cannot be null");
    return rLockId;
  }

  // TODO this function is imported as is from ARGToWitnessV2
  /**
   * Create an invariant in the format for witnesses version 2.0 for the abstractions encoded by the
   * arg states
   *
   * @param argStates the arg states encoding abstractions of the state
   * @param node the node at whose location the state should be over approximated
   * @param type the type of the invariant. Currently only `loop_invariant` and `location_invariant`
   *     are supported
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
