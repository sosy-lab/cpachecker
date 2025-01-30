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
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.math.BigInteger;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.ReportingMethodNotImplementedException;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.ARGToYAMLWitness.GhostARGStates.ARGStatePair;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.MetadataRecord;
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

  private static final CSimpleType intType =
      new CSimpleType(
          false, false, CBasicType.INT, false, false, false, false, false, false, false);

  private final CBinaryExpressionBuilder binExpressionBuilder;

  public ARGToWitnessV2dG(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {

    super(pConfig, pCfa, pSpecification, pLogger, YAMLWitnessVersion.V2dG);
    binExpressionBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
  }

  WitnessExportResult exportWitness(ARGState pRootState, Path pPath)
      throws IOException,
          ReportingMethodNotImplementedException,
          InterruptedException,
          UnrecognizedCodeException {

    // collect ARGStates relevant for the witness
    CollectedARGStates statesCollector = getRelevantStates(pRootState);

    InvariantCreationResults invariantCreationResults;
    ImmutableList<AbstractEntry> witnessEntries;
    MetadataRecord metadata = getMetadata();

    if (statesCollector.ghostStates.isEmpty()) {
      // no ghosts -> just use obtained invariants without any ghost information
      invariantCreationResults = createInvariantEntries(statesCollector, Optional.empty());
      witnessEntries =
          ImmutableList.of(
              new InvariantSetEntry(metadata, invariantCreationResults.invariantEntries()));

    } else {
      // first create ghost variables and their respective ghost updates for ghost content
      GhostARGStates ghostStates = statesCollector.ghostStates.orElseThrow();
      ImmutableList<GhostUpdateRecord> ghostUpdates =
          getGhostUpdates(ghostStates.lockUpdates, ghostStates.unlockUpdates);
      ImmutableList<GhostVariableRecord> ghostVariables = getGhostVariables(ghostUpdates);
      GhostInstrumentationContentRecord ghostContent =
          new GhostInstrumentationContentRecord(ghostVariables, ghostUpdates);

      // add ghost assumptions to obtained invariants
      ImmutableSet<String> ghostVars =
          transformedImmutableSetCopy(ghostVariables, GhostVariableRecord::name);
      invariantCreationResults = createInvariantEntries(statesCollector, Optional.of(ghostVars));
      witnessEntries =
          ImmutableList.of(
              new InvariantSetEntry(metadata, invariantCreationResults.invariantEntries()),
              new GhostInstrumentationEntry(metadata, ghostContent));
    }

    // TODO add function contracts (V2.1) here later to combine the two?
    exportEntries(witnessEntries, pPath);

    return new WitnessExportResult(invariantCreationResults.translationAlwaysSuccessful());
  }

  /** Obtain invariants from the collected {@link ARGState}s with or without ghost variables. */
  private InvariantCreationResults createInvariantEntries(
      CollectedARGStates pCollectedStates, Optional<ImmutableSet<String>> pGhostVars)
      throws ReportingMethodNotImplementedException,
          InterruptedException,
          UnrecognizedCodeException {

    final boolean ghosts = pGhostVars.isPresent();
    checkArgument(
        ghosts == pCollectedStates.ghostStates.isPresent(),
        "if ghost states were collected, ghost vars must be present");

    // use collected states to generate invariants
    Multimap<CFANode, ARGState> loopInvariants = pCollectedStates.loopInvariants;
    Multimap<CFANode, ARGState> functionCallInvariants = pCollectedStates.functionCallInvariants;

    ImmutableList.Builder<AbstractInvariantEntry> invariantEntries = new ImmutableList.Builder<>();
    boolean translationAlwaysSuccessful = true;

    // handle the loop invariants
    for (CFANode node : loopInvariants.keySet()) {
      Collection<ARGState> argStates = loopInvariants.get(node);
      InvariantCreationResult loopInvariant =
          createInvariant(argStates, node, InvariantRecordType.LOOP_INVARIANT.getKeyword());
      if (loopInvariant != null) {
        invariantEntries.add(
            ghosts
                ? createGhostInvariant(loopInvariant, argStates, pGhostVars.orElseThrow())
                : loopInvariant.invariantEntry());
        translationAlwaysSuccessful &= loopInvariant.translationSuccessful();
      }
    }

    // handle the location invariants
    for (CFANode node : functionCallInvariants.keySet()) {
      Collection<ARGState> argStates = functionCallInvariants.get(node);
      InvariantCreationResult locationInvariant =
          createInvariant(argStates, node, InvariantRecordType.LOCATION_INVARIANT.getKeyword());
      if (locationInvariant != null) {
        invariantEntries.add(
            ghosts
                ? createGhostInvariant(locationInvariant, argStates, pGhostVars.orElseThrow())
                : locationInvariant.invariantEntry());
        translationAlwaysSuccessful &= locationInvariant.translationSuccessful();
      }
    }

    return new InvariantCreationResults(invariantEntries.build(), translationAlwaysSuccessful);
  }

  /** Creates {@link GhostUpdateRecord}s from the collected {@link ARGState}s. */
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
    assert lockEdge != null : "no edge connects pParent and pChild";
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
    assert symmetricDifference.size() == 1
        : "there must be exactly one lock update between pParent and pChild";
    String rLockId = symmetricDifference.iterator().next();
    assert rLockId != null : "the updated lock cannot be null";
    return rLockId;
  }

  /** Adds the additional ghost variable information to the obtained invariant. */
  private @NonNull InvariantEntry createGhostInvariant(
      @NonNull InvariantCreationResult pInvariantCreationResult,
      @NonNull Collection<ARGState> pARGStates,
      // TODO obtain the set of all locks from the ARGStates to not pass the param?
      @NonNull ImmutableSet<String> pGhostVars)
      throws UnrecognizedCodeException {

    ExpressionTree<CBinaryExpression> ghostAntecedent =
        createGhostInvariantAntecedent(pARGStates, pGhostVars);
    InvariantEntry invariantEntry = pInvariantCreationResult.invariantEntry();
    String ghostInvariant =
        createImplication(ghostAntecedent.toString(), invariantEntry.getValue());
    return new InvariantEntry(
        ghostInvariant,
        invariantEntry.getType(),
        invariantEntry.getFormat(),
        invariantEntry.getLocation());
  }

  /**
   * Creates an {@link ExpressionTree} DNF formula of CNF formulas where each CNF represents the
   * assumptions (as {@link CBinaryExpression}) over the ghost variables in an {@link ARGState}.
   */
  private @NonNull ExpressionTree<CBinaryExpression> createGhostInvariantAntecedent(
      @NonNull Collection<ARGState> pARGStates, @NonNull ImmutableSet<String> pGhostVars)
      throws UnrecognizedCodeException {

    checkNotNull(pARGStates);
    checkNotNull(pGhostVars);

    Multimap<ARGState, CBinaryExpression> expressions = HashMultimap.create();
    for (ARGState argState : pARGStates) {
      Optional<ThreadingState> threadingState = ARGUtils.tryExtractThreadingState(argState);
      if (threadingState.isPresent()) {
        assert pGhostVars.containsAll(threadingState.orElseThrow().getLockIdsFromInputProgram())
            : "ghost vars must contain all locks";
        for (String ghostVar : pGhostVars) {
          boolean locked =
              threadingState.orElseThrow().getLockIdsFromInputProgram().contains(ghostVar);
          CStringLiteralExpression variable =
              new CStringLiteralExpression(FileLocation.DUMMY, ghostVar);
          CIntegerLiteralExpression value =
              new CIntegerLiteralExpression(
                  FileLocation.DUMMY,
                  intType,
                  locked ? BigInteger.valueOf(1) : BigInteger.valueOf(0));
          CBinaryExpression binExpression =
              binExpressionBuilder.buildBinaryExpression(variable, value, BinaryOperator.EQUALS);
          expressions.put(argState, binExpression);
        }
      }
    }
    return Or.of(
        FluentIterable.from(expressions.keySet())
            .transform(
                state ->
                    And.of(
                        FluentIterable.from(expressions.get(state))
                            .transform(expression -> LeafExpression.of(expression)))));
  }
}
