// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class DataRaceTransferRelation extends SingleEdgeTransferRelation {

  private final MemoryAccessExtractor memoryAccessExtractor;
  private final MachineModel machineModel;
  private final CBinaryExpressionBuilder expressionBuilder;

  DataRaceTransferRelation(
      MachineModel pMachineModel,
      Set<String> pAddressedVariables,
      CBinaryExpressionBuilder pExpressionBuilder) {
    machineModel = pMachineModel;
    memoryAccessExtractor = new MemoryAccessExtractor(pAddressedVariables);
    expressionBuilder = pExpressionBuilder;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    // Can only update state with info from ThreadingCPA
    return ImmutableSet.of(((DataRaceState) pState).withoutAssumptions());
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    if (cfaEdge == null) {
      return ImmutableSet.of(pState);
    }
    DataRaceState state = (DataRaceState) pState;
    Map<String, ThreadInfo> threadInfo = state.getThreadInfo();
    ImmutableSet.Builder<ThreadSynchronization> synchronizationBuilder = ImmutableSet.builder();
    synchronizationBuilder.addAll(state.getThreadSynchronizations());

    ThreadingState threadingState =
        Iterables.getOnlyElement(AbstractStates.projectToType(otherStates, ThreadingState.class));

    Set<String> threadIds = threadingState.getThreadIds();
    String activeThread = getActiveThread(cfaEdge, threadingState);
    assert Objects.equals(activeThread, threadInfo.get(activeThread).getThreadId());
    ImmutableMap<String, ThreadInfo> newThreadInfo =
        updateThreadInfo(threadInfo, threadIds, activeThread, synchronizationBuilder);

    if (newThreadInfo.values().stream().filter(ThreadInfo::isRunning).count() == 1) {
      // No data race possible in sequential part
      return ImmutableSet.of(new DataRaceState(newThreadInfo, state.hasDataRace()));
    }

    // Update locking related info with info from ThreadingCPA
    Set<String> activeThreadLocks = threadingState.getLocksForThread(activeThread);
    ImmutableSetMultimap<String, String> heldLocks =
        updateHeldLocks(state, activeThread, activeThreadLocks);
    ImmutableSet<LockRelease> lastReleases =
        updateLastReleases(state, activeThread, activeThreadLocks, threadInfo);

    for (String lock : activeThreadLocks) {
      if (!state.getLocksForThread(activeThread).contains(lock)) {
        //  Lock was newly acquired
        LockRelease lastRelease = state.getLastReleaseForLock(lock);
        if (lastRelease != null && !lastRelease.getThreadId().equals(activeThread)) {
          // A lock release synchronizes-with the next acquisition of that lock (but
          // synchronizes-with is unnecessary if acquire and release are done by the same thread)
          synchronizationBuilder.add(
              new ThreadSynchronization(
                  lastRelease.getThreadId(),
                  activeThread,
                  lastRelease.getAccessEpoch(),
                  threadInfo.get(activeThread).getEpoch()));
        }
      }
    }

    // Collect new accesses
    Set<MemoryLocation> changedVariables = memoryAccessExtractor.getChangedVariables(cfaEdge);
    Set<MemoryAccess> newMemoryAccesses =
        FluentIterable.from(
                memoryAccessExtractor.getNewAccesses(
                    threadInfo.get(activeThread), cfaEdge, activeThreadLocks))
            .transform(access -> access.withChangedVariables(changedVariables))
            .toSet();

    // Update tracked memory accesses
    ImmutableSet.Builder<MemoryAccess> memoryAccessBuilder = ImmutableSet.builder();
    for (MemoryAccess access : state.getMemoryAccesses()) {
      if (!threadIds.contains(access.getThreadId())) {
        // If the thread that made the access is no longer running,
        // then this access can not conflict with any newer accesses.
        // Therefore, we do not need to track it any longer.
        continue;
      }
      memoryAccessBuilder.add(access.withChangedVariables(changedVariables));

      for (MemoryAccess newAccess : newMemoryAccesses) {
        if (!access.mightAccessSameLocationAs(newAccess)) {
          continue;
        }
        if (!newAccess.isWrite()) {
          if (access.getThreadId().equals(newAccess.getThreadId())) {
            // Adding synchronizes-with edge between accesses made by the same thread is
            // unnecessary, because happens-before is established anyway.
            continue;
          }
          if (!Sets.intersection(access.getLocks(), newAccess.getLocks()).isEmpty()) {
            // Add synchronizes-with edge:
            // An atomic write operation synchronizes-with an atomic read operation of the same
            // variable that reads the value written by the former.
            synchronizationBuilder.add(
                new ThreadSynchronization(
                    access.getThreadId(),
                    newAccess.getThreadId(),
                    access.getAccessEpoch(),
                    newAccess.getAccessEpoch()));
          }
        }
      }
    }

    // Determine whether any of the new accesses constitutes a data race
    boolean hasDataRace = state.hasDataRace();
    boolean hasUnsupportedConflict = false;
    ImmutableSet.Builder<CExpression> aliasConditions = ImmutableSet.builder();
    Set<ThreadSynchronization> threadSynchronizations = synchronizationBuilder.build();
    for (MemoryAccess access : memoryAccessBuilder.build()) {
      if (hasDataRace) {
        // Already found a data race, no need to continue
        break;
      }
      if (access.getThreadId().equals(activeThread)) {
        // All new accesses were made by the currently active thread and accesses made by the same
        // thread are never conflicting.
        continue;
      }

      for (MemoryAccess newAccess : newMemoryAccesses) {
        boolean bothPrecise = !access.isOverapproximating() && !newAccess.isOverapproximating();
        if ((access.isWrite() || newAccess.isWrite())
            && (bothPrecise
                ? access.mightAccessSameLocationAs(newAccess)
                : mayOverlap(access, newAccess))
            && Sets.intersection(access.getLocks(), newAccess.getLocks()).isEmpty()
            && !access.happensBefore(newAccess, threadSynchronizations)
            && !(newAccess.memoryLocationIsAtomic() && access.memoryLocationIsAtomic())) {
          // Two accesses are conflicting if:
          //   - They access the same memory location
          //   - They were made by two different threads
          //   - At least one of them is a write access
          // Two conflicting accesses constitute a data race unless:
          //   - Both accesses are performed through atomic operations, or
          //   - One access happens-before the other
          if (bothPrecise) {
            hasDataRace = true;
            break;
          }
          Optional<CExpression> aliasCondition = getAliasCondition(access, newAccess);
          if (aliasCondition.isPresent()) {
            aliasConditions.add(aliasCondition.orElseThrow());
          } else {
            hasUnsupportedConflict = true;
          }
        }
      }
    }
    if (!hasDataRace && hasUnsupportedConflict) {
      throw new CPATransferException("DataRaceCPA does not support pointer analysis");
    }

    Set<MemoryAccess> successorAccesses =
        determineSuccessorAccessedMemoryLocations(
            memoryAccessBuilder.build(), newMemoryAccesses, cfaEdge, activeThreadLocks);
    ImmutableList<CExpression> conditions = aliasConditions.build().asList();
    if (hasDataRace || conditions.isEmpty()) {
      return ImmutableSet.of(
          new DataRaceState(
              successorAccesses,
              newThreadInfo,
              threadSynchronizations,
              heldLocks,
              lastReleases,
              hasDataRace,
              ImmutableList.of()));
    }

    // There is a data race iff an access through a pointer refers to the location of a conflicting
    // access, which the other CPAs check with the following assumptions.
    CExpression raceCondition = conditions.getFirst();
    for (CExpression condition : conditions.subList(1, conditions.size())) {
      raceCondition =
          expressionBuilder.buildBinaryExpression(
              raceCondition, condition, BinaryOperator.BITWISE_OR);
    }
    if (!(raceCondition instanceof CBinaryExpression binaryExpression
        && binaryExpression.getOperator().isLogicalOperator())) {
      raceCondition =
          expressionBuilder.buildBinaryExpression(
              raceCondition, CIntegerLiteralExpression.ZERO, BinaryOperator.NOT_EQUALS);
    }
    ImmutableList.Builder<CExpression> noRaceConditions = ImmutableList.builder();
    for (CExpression condition : conditions) {
      noRaceConditions.add(expressionBuilder.negateExpressionAndSimplify(condition));
    }
    return ImmutableList.of(
        new DataRaceState(
            successorAccesses,
            newThreadInfo,
            threadSynchronizations,
            heldLocks,
            lastReleases,
            true,
            ImmutableList.of(raceCondition)),
        new DataRaceState(
            successorAccesses,
            newThreadInfo,
            threadSynchronizations,
            heldLocks,
            lastReleases,
            false,
            noRaceConditions.build()));
  }

  /**
   * Returns the condition under which the two accesses overlap and the pointers dereferenced on the
   * way are not null (otherwise the program crashes). Returns empty if the address of an access may
   * have changed since the access.
   */
  private Optional<CExpression> getAliasCondition(MemoryAccess pAccess1, MemoryAccess pAccess2)
      throws UnrecognizedCodeException {
    CType type1 = pAccess1.getType();
    CType type2 = pAccess2.getType();
    if (pAccess1.addressMayHaveChanged()
        || pAccess2.addressMayHaveChanged()
        || type1.isIncomplete()
        || type2.isIncomplete()) {
      return Optional.empty();
    }
    BigInteger size1 = machineModel.getSizeof(type1);
    BigInteger size2 = machineModel.getSizeof(type2);
    CExpression address1 = getAddress(pAccess1.getExpression());
    CExpression address2 = getAddress(pAccess2.getExpression());
    CExpression condition;
    if (size1.equals(size2) && CTypes.isScalarType(type1) && CTypes.isScalarType(type2)) {
      // Aligned scalars of the same size overlap iff they start at the same address
      if (!address1
          .getExpressionType()
          .getCanonicalType()
          .equals(address2.getExpressionType().getCanonicalType())) {
        address1 =
            new CCastExpression(address1.getFileLocation(), CPointerType.POINTER_TO_VOID, address1);
        address2 =
            new CCastExpression(address2.getFileLocation(), CPointerType.POINTER_TO_VOID, address2);
      }
      condition =
          expressionBuilder.buildBinaryExpression(address1, address2, BinaryOperator.EQUALS);
    } else {
      // Two locations overlap iff each of them starts before the other one ends
      CExpression start1 =
          new CCastExpression(address1.getFileLocation(), CPointerType.POINTER_TO_CHAR, address1);
      CExpression start2 =
          new CCastExpression(address2.getFileLocation(), CPointerType.POINTER_TO_CHAR, address2);
      condition =
          expressionBuilder.buildBinaryExpression(
              expressionBuilder.buildBinaryExpression(
                  start1, getEnd(start2, size2), BinaryOperator.LESS_THAN),
              expressionBuilder.buildBinaryExpression(
                  start2, getEnd(start1, size1), BinaryOperator.LESS_THAN),
              BinaryOperator.BITWISE_AND);
    }
    for (CExpression pointer :
        Iterables.concat(
            getDereferencedPointers(pAccess1.getExpression()),
            getDereferencedPointers(pAccess2.getExpression()))) {
      condition =
          expressionBuilder.buildBinaryExpression(
              condition,
              expressionBuilder.buildBinaryExpression(
                  pointer, CIntegerLiteralExpression.ZERO, BinaryOperator.NOT_EQUALS),
              BinaryOperator.BITWISE_AND);
    }
    return Optional.of(condition);
  }

  private CExpression getEnd(CExpression pStart, BigInteger pSize)
      throws UnrecognizedCodeException {
    return expressionBuilder.buildBinaryExpression(
        pStart,
        new CIntegerLiteralExpression(
            pStart.getFileLocation(), machineModel.getPointerSizedIntType(), pSize),
        BinaryOperator.PLUS);
  }

  /** Returns the pointers that are dereferenced to evaluate the given expression. */
  private static List<CExpression> getDereferencedPointers(CExpression pExpression) {
    return switch (pExpression) {
      case CPointerExpression pointerExpression ->
          ImmutableList.<CExpression>builder()
              .add(pointerExpression.getOperand())
              .addAll(getDereferencedPointers(pointerExpression.getOperand()))
              .build();
      case CFieldReference fieldRef when fieldRef.isPointerDereference() ->
          ImmutableList.<CExpression>builder()
              .add(fieldRef.getFieldOwner())
              .addAll(getDereferencedPointers(fieldRef.getFieldOwner()))
              .build();
      case CFieldReference fieldRef -> getDereferencedPointers(fieldRef.getFieldOwner());
      case CArraySubscriptExpression subscript -> {
        ImmutableList.Builder<CExpression> pointers = ImmutableList.builder();
        if (subscript.getArrayExpression().getExpressionType().getCanonicalType()
            instanceof CPointerType) {
          pointers.add(subscript.getArrayExpression());
        }
        yield pointers
            .addAll(getDereferencedPointers(subscript.getArrayExpression()))
            .addAll(getDereferencedPointers(subscript.getSubscriptExpression()))
            .build();
      }
      case CCastExpression cast -> getDereferencedPointers(cast.getOperand());
      case CUnaryExpression unary -> getDereferencedPointers(unary.getOperand());
      case CBinaryExpression binary ->
          ImmutableList.<CExpression>builder()
              .addAll(getDereferencedPointers(binary.getOperand1()))
              .addAll(getDereferencedPointers(binary.getOperand2()))
              .build();
      default -> ImmutableList.of();
    };
  }

  private static CExpression getAddress(CExpression pLvalue) {
    if (pLvalue instanceof CPointerExpression pointerExpression
        && pointerExpression.getOperand().getExpressionType().getCanonicalType()
            instanceof CPointerType) {
      return pointerExpression.getOperand();
    }
    return new CUnaryExpression(
        pLvalue.getFileLocation(),
        new CPointerType(CTypeQualifiers.NONE, pLvalue.getExpressionType()),
        pLvalue,
        UnaryOperator.AMPER);
  }

  /**
   * Checks whether two accesses, of which at least one is through a pointer, may overlap. A
   * variable whose address is never taken can not be accessed through a pointer. By the
   * strict-aliasing rule, scalars of different sizes do not overlap unless one of them is a
   * character.
   */
  private boolean mayOverlap(MemoryAccess pAccess1, MemoryAccess pAccess2) {
    if (!pAccess1.mayBePointedTo() || !pAccess2.mayBePointedTo()) {
      return false;
    }
    CType type1 = pAccess1.getType();
    CType type2 = pAccess2.getType();
    if (CTypes.isCharacterType(type1)
        || CTypes.isCharacterType(type2)
        || !CTypes.isScalarType(type1)
        || !CTypes.isScalarType(type2)) {
      return true;
    }
    return machineModel.getSizeof(type1).equals(machineModel.getSizeof(type2));
  }

  private static Set<MemoryAccess> determineSuccessorAccessedMemoryLocations(
      Set<MemoryAccess> previousAccesses,
      Set<MemoryAccess> newAccesses,
      CFAEdge cfaEdge,
      Set<String> activeThreadLocks)
      throws CPATransferException {
    if (activeThreadLocks.contains(ThreadingTransferRelation.LOCAL_ACCESS_LOCK)) {
      // The ThreadingCPA does not interleave other threads after edges with only local accesses,
      // so the previous accesses must stay visible to the next edge of another thread.
      return previousAccesses;
    }
    // Some edges, are not actually statements which should reset the
    // tracked accesses (e.g., function call edges, and blank edges).
    // In this, cases we need to keep the old accesses as well.
    return switch (cfaEdge.getEdgeType()) {
      case BlankEdge -> previousAccesses;
      case AssumeEdge,
          DeclarationEdge,
          FunctionCallEdge,
          FunctionReturnEdge,
          ReturnStatementEdge,
          CallToReturnEdge ->
          // Function calls and returns, declarations, and assume edges are not statements and
          // therefore do not invalidate previously tracked accesses, but they may add new accesses
          // as well, due to function arguments, return values, assigned results, initializers, or
          // the variables being read in the assume condition.
          FluentIterable.concat(previousAccesses, newAccesses).toSet();
      case StatementEdge -> {
        if (!(cfaEdge instanceof AStatementEdge statementEdge)) {
          throw new CPATransferException(
              "Unexpected statement edge type: " + cfaEdge.getClass().getSimpleName());
        }
        AStatement statement = statementEdge.getStatement();
        if (statement instanceof AFunctionCallStatement pCallStatement
            && ImmutableSet.of("__VERIFIER_atomic_begin", "__VERIFIER_atomic_end")
                .contains(pCallStatement.getFunctionCallExpression().getDeclaration().getName())) {
          // Atomic sections do not invalidate tracked accesses
          // (they are not synchronization operations)
          yield previousAccesses;
        } else {
          yield newAccesses;
        }
      }
    };
  }

  private ImmutableSetMultimap<String, String> updateHeldLocks(
      DataRaceState state, String activeThread, Set<String> activeThreadLocks) {
    ImmutableSetMultimap.Builder<String, String> newHeldLocks = ImmutableSetMultimap.builder();
    // For locks held by the active thread use info from ThreadingCPA
    for (String lock : activeThreadLocks) {
      newHeldLocks.put(activeThread, lock);
    }

    // For locks held by any other thread nothing changes
    for (Entry<String, String> entry : state.getHeldLocks().entries()) {
      if (!entry.getKey().equals(activeThread)) {
        newHeldLocks.put(entry);
      }
    }
    return newHeldLocks.build();
  }

  private ImmutableSet<LockRelease> updateLastReleases(
      DataRaceState state,
      String activeThread,
      Set<String> activeThreadLocks,
      Map<String, ThreadInfo> threadInfo) {
    ImmutableSet.Builder<LockRelease> newReleases = ImmutableSet.builder();
    // First, add any new lock releases
    for (String lock : state.getLocksForThread(activeThread)) {
      if (!activeThreadLocks.contains(lock)) {
        // Lock was newly released
        if (!lock.equals(ThreadingTransferRelation.LOCAL_ACCESS_LOCK)) {
          // Do not track releases of local access lock,
          // as these may not be used for synchronization
          newReleases.add(
              new LockRelease(lock, activeThread, threadInfo.get(activeThread).getEpoch()));
        }
      }
    }

    // Then add already tracked lock releases that were not invalidated
    for (LockRelease release : state.getLastReleases()) {
      if (!Sets.symmetricDifference(activeThreadLocks, state.getLocksForThread(activeThread))
          .contains(release.getLockId())) {
        newReleases.add(release);
      }
    }
    return newReleases.build();
  }

  /**
   * Updates the currently tracked thread information with information from the ThreadingCPA.
   *
   * @param threadInfo The current map of thread ID -> ThreadInfo.
   * @param threadIds The IDs of currently existing thread as obtained from the ThreadingCPA.
   * @param activeThread The ID of the current active thread.
   * @return The new map of thread ID -> ThreadInfo objects.
   */
  private ImmutableMap<String, ThreadInfo> updateThreadInfo(
      Map<String, ThreadInfo> threadInfo,
      Set<String> threadIds,
      String activeThread,
      ImmutableSet.Builder<ThreadSynchronization> threadSynchronizations) {
    Set<String> added = Sets.difference(threadIds, threadInfo.keySet());
    assert added.size() < 2 : "Multiple thread creations in same step not supported";
    Set<String> removed = Sets.difference(threadInfo.keySet(), threadIds);
    assert !removed.contains(activeThread) : "Thread active after join";

    ImmutableMap.Builder<String, ThreadInfo> threadsBuilder = ImmutableMap.builder();
    if (!added.isEmpty()) {
      String threadId = added.iterator().next();
      ThreadInfo addedThreadInfo;
      if (threadInfo.containsKey(threadId)) {
        addedThreadInfo = new ThreadInfo(threadId, threadInfo.get(threadId).getEpoch() + 1, true);
      } else {
        addedThreadInfo = new ThreadInfo(threadId, 0, true);
      }
      threadsBuilder.put(threadId, addedThreadInfo);
      threadSynchronizations.add(
          new ThreadSynchronization(
              activeThread,
              threadId,
              threadInfo.get(activeThread).getEpoch() + 1,
              addedThreadInfo.getEpoch()));
    }
    for (Entry<String, ThreadInfo> entry : threadInfo.entrySet()) {
      if (entry.getKey().equals(activeThread)) {
        threadsBuilder.put(
            activeThread, new ThreadInfo(activeThread, entry.getValue().getEpoch() + 1, true));
      } else if (removed.contains(entry.getKey())) {
        threadsBuilder.put(
            entry.getKey(), new ThreadInfo(entry.getKey(), entry.getValue().getEpoch(), false));
      } else if (!added.contains(entry.getKey())) {
        threadsBuilder.put(entry);
      }
    }
    return threadsBuilder.buildOrThrow();
  }

  /**
   * Search for the thread where the given edge is available.
   *
   * <p>This method is necessary, because neither ThreadingState::getActiveThread nor
   * ThreadingTransferRelation::getActiveThread are guaranteed to give the correct result during
   * strengthening.
   */
  private String getActiveThread(final CFAEdge cfaEdge, final ThreadingState threadingState) {
    for (String id : threadingState.getThreadIds()) {
      if (Iterables.contains(threadingState.getThreadLocation(id).getIncomingEdges(), cfaEdge)) {
        return id;
      }
    }
    throw new AssertionError("Unable to determine active thread");
  }
}
