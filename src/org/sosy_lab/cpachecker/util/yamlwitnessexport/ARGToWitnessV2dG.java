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
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
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
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.ReportingMethodNotImplementedException;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.ARGToYAMLWitness.CollectedARGStates.ARGStatePair;
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
        getGhostUpdates(statesCollector.lockUpdates, statesCollector.unlockUpdates);
    ImmutableList<GhostVariableRecord> ghostVariables = getGhostVariables(ghostUpdates);
    GhostInstrumentationContentRecord record =
        new GhostInstrumentationContentRecord(ghostVariables, ghostUpdates);

    // second use collected states to generate invariants
    Multimap<CFANode, ARGState> loopInvariants = statesCollector.loopInvariants;
    Multimap<CFANode, ARGState> functionCallInvariants = statesCollector.functionCallInvariants;

    // TODO: also include ghost variables in the invariants
    ImmutableList.Builder<AbstractInvariantEntry> invariantEntries = new ImmutableList.Builder<>();
    boolean translationAlwaysSuccessful = true;

    ImmutableList<String> ghostVars =
        FluentIterable.from(ghostVariables).transform(GhostVariableRecord::name).toList();

    // handle the loop invariants
    for (CFANode node : loopInvariants.keySet()) {
      Collection<ARGState> argStates = loopInvariants.get(node);
      InvariantCreationResult loopInvariant =
          createInvariant(argStates, node, InvariantRecordType.LOOP_INVARIANT.getKeyword());
      if (loopInvariant != null) {
        // add ghost variable information for all ARGStates
        // TODO the ghostInvariant should only be created with ThreadingStates present?
        invariantEntries.add(
            createGhostInvariantEntry(loopInvariant, ImmutableList.copyOf(argStates), ghostVars));
        translationAlwaysSuccessful &= loopInvariant.translationSuccessful();
      }
    }

    // handle the location invariants
    for (CFANode node : functionCallInvariants.keySet()) {
      Collection<ARGState> argStates = functionCallInvariants.get(node);
      InvariantCreationResult locationInvariant =
          createInvariant(argStates, node, InvariantRecordType.LOCATION_INVARIANT.getKeyword());
      if (locationInvariant != null) {
        // add ghost variable information for all ARGStates
        // TODO the ghostInvariant should only be created with ThreadingStates present?
        invariantEntries.add(
            createGhostInvariantEntry(
                locationInvariant, ImmutableList.copyOf(argStates), ghostVars));
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

  /** Creates {@link GhostUpdateRecord}s from the collected {@link ARGState}. */
  private ImmutableList<GhostUpdateRecord> getGhostUpdates(
      @NonNull BiMap<FileLocation, ARGStatePair> pLockUpdates,
      @NonNull BiMap<FileLocation, ARGStatePair> pUnlockUpdates) {

    checkNotNull(pLockUpdates);
    checkNotNull(pUnlockUpdates);
    ImmutableList.Builder<GhostUpdateRecord> ghostUpdates = ImmutableList.builder();
    // handle ghost updates through locks
    for (ARGStatePair pair : pLockUpdates.values()) {
      ghostUpdates.add(createGhostUpdate(pair.parent(), pair.child(), 1));
    }
    // handle ghost updates through unlocks
    for (ARGStatePair pair : pUnlockUpdates.values()) {
      ghostUpdates.add(createGhostUpdate(pair.parent(), pair.child(), 0));
    }
    // sort ghost updates by line then column to improve readability for users
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
  private ImmutableList<GhostVariableRecord> getGhostVariables(
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
   * Returns the single lock id as used in {@link ThreadingState} that is updated between pParent
   * and pChild.
   */
  private @NonNull String getLockId(@NonNull ARGState pParent, @NonNull ARGState pChild) {
    checkNotNull(pParent);
    checkNotNull(pChild);
    ThreadingState parent = ARGUtils.tryExtractThreadingState(pParent).orElseThrow();
    ThreadingState child = ARGUtils.tryExtractThreadingState(pChild).orElseThrow();
    SetView<String> symmetricDifference =
        Sets.symmetricDifference(
            parent.getLockIdsFromInputProgram(), child.getLockIdsFromInputProgram());
    Verify.verify(
        symmetricDifference.size() == 1,
        "there must be exactly one lock update between pParent and pChild");
    String rLockId = symmetricDifference.iterator().next();
    Verify.verify(rLockId != null, "the updated lock cannot be null");
    return rLockId;
  }

  /** Adds the additional ghost variable information to the obtained invariant. */
  private @NonNull InvariantEntry createGhostInvariantEntry(
      @NonNull InvariantCreationResult pInvariantCreationResult,
      @NonNull ImmutableList<ARGState> pARGStates,
      @NonNull ImmutableList<String> pGhostVars) {

    // TODO use ExpressionTree here instead of Strings
    String ghostLhs =
        createLeftHandSideGhostInvariant(ImmutableList.copyOf(pARGStates), pGhostVars);
    InvariantEntry invariantEntry = pInvariantCreationResult.invariantEntry();
    String ghostInvariant = createGhostInvariant(ghostLhs, invariantEntry.getValue());
    return new InvariantEntry(
        ghostInvariant,
        invariantEntry.getType(),
        invariantEntry.getFormat(),
        invariantEntry.getLocation());
  }

  /** Creates an invariant where {@code pLeftHandSide} implies {@code pInvariant}. */
  private @NonNull String createGhostInvariant(
      @NonNull String pLeftHandSide, @NonNull String pInvariant) {

    checkNotNull(pLeftHandSide);
    checkNotNull(pInvariant);
    StringBuilder rInvariant = new StringBuilder();
    rInvariant
        .append(BinaryLogicalOperator.LOGICAL_NOT.getOperator())
        .append(wrap("(", pLeftHandSide, ")"))
        .append(wrap(" ", BinaryOperator.BINARY_OR.getOperator()))
        .append(pInvariant);
    return rInvariant.toString();
  }

  /**
   * Creates a DNF formula of KNF formulas where each KNF represents the assumptions over the ghost
   * variables in an {@link ARGState}.
   */
  private @NonNull String createLeftHandSideGhostInvariant(
      @NonNull ImmutableList<ARGState> pARGStates, @NonNull ImmutableList<String> pGhostVars) {

    checkNotNull(pARGStates);
    StringBuilder rInvariant = new StringBuilder();
    for (int i = 0; i < pARGStates.size(); i++) {
      ARGState argState = pARGStates.get(i);
      Optional<ThreadingState> threadingState = ARGUtils.tryExtractThreadingState(argState);
      if (threadingState.isPresent()) {
        assert pGhostVars.containsAll(threadingState.orElseThrow().getLockIdsFromInputProgram())
            : "ghost vars must contain all locks";
        StringBuilder stateVars = new StringBuilder();
        for (int j = 0; j < pGhostVars.size(); j++) {
          String ghostVar = pGhostVars.get(j);
          boolean locked =
              threadingState.orElseThrow().getLockIdsFromInputProgram().contains(ghostVar);
          stateVars
              .append(ghostVar)
              .append(wrap(" ", BinaryOperator.EQUALS.getOperator()))
              .append(locked ? 1 : 0);
          if (j != pGhostVars.size() - 1) {
            stateVars.append(wrap(" ", BinaryLogicalOperator.LOGICAL_AND.getOperator()));
          }
        }
        rInvariant.append(wrap("(", stateVars.toString(), ")"));
        if (i != pARGStates.size() - 1) {
          rInvariant.append(wrap(" ", BinaryLogicalOperator.LOGICAL_OR.getOperator()));
        }
      }
    }
    return rInvariant.toString();
  }

  // TODO using ExpressionTree should make this redundant
  private String wrap(String pWrap, String pString) {
    return pWrap + pString + pWrap;
  }

  private String wrap(String pPrefix, String pString, String pSuffix) {
    return pPrefix + pString + pSuffix;
  }

  private enum BinaryLogicalOperator {
    LOGICAL_AND("&&"),
    LOGICAL_OR("||"),
    LOGICAL_NOT("!");

    private final String op;

    BinaryLogicalOperator(String pOp) {
      op = pOp;
    }

    public String getOperator() {
      return op;
    }
  }
}
