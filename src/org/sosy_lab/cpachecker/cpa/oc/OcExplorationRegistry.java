// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.oc.ThreadInstance.InstanceKey;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

/**
 * Mutable side store of the ordering-consistency exploration. The transfer relation appends events,
 * thread instances, and guarded path constraints while the CPA algorithm expands the per-thread
 * trees; the ordering-consistency algorithm reads everything afterwards for the SMT encoding.
 */
public final class OcExplorationRegistry {

  /**
   * A branch whose condition read produced an event. {@code condition} is the SSA formula of the
   * {@code firstEdge} direction; evaluating it in a model tells which of the two assume edges was
   * actually taken, so a counterexample can show the real direction instead of the (arbitrary) edge
   * the shared condition read was attached to.
   */
  public record AssumeBranch(BooleanFormula condition, CFAEdge firstEdge, CFAEdge secondEdge) {}

  private final Map<Integer, MemoryEvent> events = new LinkedHashMap<>();
  private final Map<Integer, Integer> chainTerminalEventIds = new HashMap<>();
  private final Map<Integer, AssumeBranch> assumeBranches = new HashMap<>();
  private final Set<Integer> atomicAccessEventIds = new HashSet<>();
  private final Set<Integer> readLockEventIds = new HashSet<>();
  private final Set<Integer> ambiguousUnlockEventIds = new HashSet<>();
  private final Set<Integer> threadHandleAccessEventIds = new HashSet<>();
  private final Map<Integer, ThreadInstance> instances = new LinkedHashMap<>();
  private final Map<InstanceKey, ThreadInstance> instancesByKey = new HashMap<>();
  private final List<BooleanFormula> pathConstraints = new ArrayList<>();
  private final List<AddressBase> addressBases = new ArrayList<>();
  private final SetMultimap<Integer, Integer> poPredecessors = LinkedHashMultimap.create();
  private int nextCssaIndex;
  private boolean truncated = false;

  public OcExplorationRegistry() {
    this(0);
  }

  /**
   * Starts the fresh-name counter at {@code pStartCssaIndex} instead of zero. Iterative deepening
   * (see {@code OrderingConsistencyCPA#resetExploration}) throws away the registry every round and
   * starts a new one, but keeps solving in the very same, persistent solver context. If the counter
   * restarted at zero every round, two different rounds could mint the identical fresh name (same
   * prefix, same counter value) for accesses of different types; the solver rejects redeclaring a
   * symbol at a different sort under the same name. Carrying the counter forward (see the caller)
   * keeps every name minted over the whole analysis run globally unique.
   */
  public OcExplorationRegistry(int pStartCssaIndex) {
    nextCssaIndex = pStartCssaIndex;
  }

  /** Mints a globally fresh qualified name for a single access to the given global variable. */
  public String freshCssaName(String pQualifiedName) {
    return pQualifiedName + "__" + nextCssaIndex++;
  }

  /**
   * The next unused fresh-name counter value; carried into the following round's registry so names
   * stay unique across iterative-deepening rounds (see {@link #OcExplorationRegistry(int)}).
   */
  public int getNextCssaIndex() {
    return nextCssaIndex;
  }

  /** The id the next created event will receive. */
  public int nextEventId() {
    return events.size();
  }

  /**
   * Records that the events {@code [pFirstEventId, pTerminalEventId]} were all created by one
   * {@code chainAccessEvents} call, i.e. one CFA edge: several accesses on the same edge (e.g. both
   * operands of {@code a != b}) are chained into a single event sequence, but only the last one
   * becomes a reached state's {@code lastEventIds}. Earlier events in the chain need this mapping
   * to be resolved back to the state that actually carries them (see {@link
   * #chainTerminalEventId}).
   */
  public void registerChainTerminal(int pFirstEventId, int pTerminalEventId) {
    for (int id = pFirstEventId; id <= pTerminalEventId; id++) {
      chainTerminalEventIds.put(id, pTerminalEventId);
    }
  }

  /**
   * The id of the last event chained on the same CFA edge as the given event (identity if the event
   * is not part of a recorded chain). See {@link #registerChainTerminal}.
   */
  public int chainTerminalEventId(int pEventId) {
    return chainTerminalEventIds.getOrDefault(pEventId, pEventId);
  }

  /** Records that the events with the given ids are condition reads of the given branch. */
  public void registerAssumeBranch(int pFromEventId, int pToEventId, AssumeBranch pBranch) {
    for (int id = pFromEventId; id < pToEventId; id++) {
      assumeBranches.put(id, pBranch);
    }
  }

  /** The branch a condition-read event belongs to, or null if the event is not a condition read. */
  public @Nullable AssumeBranch getAssumeBranch(int pEventId) {
    return assumeBranches.get(pEventId);
  }

  /**
   * Records that the given READ/WRITE event is an atomic access (its lvalue is {@code _Atomic}
   * qualified). A data race needs at least one non-atomic access, so two atomic accesses of the
   * same location in different threads never race (see the data-race check).
   */
  public void markAtomicAccess(int pEventId) {
    atomicAccessEventIds.add(pEventId);
  }

  /** Whether the given access event is atomic (see {@link #markAtomicAccess}). */
  public boolean isAtomicAccess(int pEventId) {
    return atomicAccessEventIds.contains(pEventId);
  }

  /**
   * Records that the given READ/WRITE event is the synthetic thread-id bookkeeping for an arbitrary
   * pthread_create/pthread_join handle expression (see OrderingConsistencyTransferRelation's
   * writeThreadHandle/readThreadHandle). It must still participate in the normal read-from
   * machinery (so a join's candidate-branch equality is only satisfiable when the handle genuinely
   * aliases that candidate's create), but is not program memory and must never be reported as a
   * data-race candidate.
   */
  public void markThreadHandleAccess(int pEventId) {
    threadHandleAccessEventIds.add(pEventId);
  }

  /**
   * Whether the given access event is thread-handle bookkeeping (see {@link
   * #markThreadHandleAccess}).
   */
  public boolean isThreadHandleAccess(int pEventId) {
    return threadHandleAccessEventIds.contains(pEventId);
  }

  /**
   * Records that the given LOCK event acquires a shared/read lock ({@code pthread_rwlock_rdlock}).
   * Two read-locked critical sections of the same rwlock may overlap; only pairs involving a
   * write-locked section must be ordered.
   */
  public void markReadLock(int pEventId) {
    readLockEventIds.add(pEventId);
  }

  /** Whether the given LOCK event acquires a shared/read lock (see {@link #markReadLock}). */
  public boolean isReadLock(int pEventId) {
    return readLockEventIds.contains(pEventId);
  }

  /**
   * Records that the given UNLOCK event releases a mutex reached through an expression whose target
   * object is not statically fixed (a bare pointer value, a dereference, or {@code
   * &arr[symbolic]}). Such an unlock can release a lock held under a <em>different</em> syntactic
   * name, so the critical section it closes must be resolved by address, not by the syntactic
   * nesting key (see {@code OcEncoder.buildCsPairs}). Without this, an aliased unlock never closes
   * the section it truly releases, leaving a later unsynchronized access wrongly inside the section
   * (a missed data race).
   */
  public void markAmbiguousUnlock(int pEventId) {
    ambiguousUnlockEventIds.add(pEventId);
  }

  /**
   * Whether the given UNLOCK event has a non-static target object (see {@link
   * #markAmbiguousUnlock}).
   */
  public boolean isAmbiguousUnlock(int pEventId) {
    return ambiguousUnlockEventIds.contains(pEventId);
  }

  /** Creates and stores the event with the next free id. */
  public MemoryEvent addEvent(
      int pInstanceId,
      EventKind pKind,
      int pPoParentId,
      BooleanFormula pPathGuard,
      @Nullable MemoryLocation pMemoryLocation,
      @Nullable String pCssaName,
      @Nullable Formula pVariable,
      @Nullable String pMutexId,
      int pOtherInstanceId,
      @Nullable CFAEdge pEdge) {
    return addEvent(
        pInstanceId,
        pKind,
        pPoParentId,
        pPathGuard,
        pMemoryLocation,
        pCssaName,
        pVariable,
        pMutexId,
        pOtherInstanceId,
        null,
        null,
        null,
        false,
        0,
        pEdge);
  }

  /** Creates and stores an event of the aliasing regime (with region, base, and offset). */
  public MemoryEvent addEvent(
      int pInstanceId,
      EventKind pKind,
      int pPoParentId,
      BooleanFormula pPathGuard,
      @Nullable MemoryLocation pMemoryLocation,
      @Nullable String pCssaName,
      @Nullable Formula pVariable,
      @Nullable String pMutexId,
      int pOtherInstanceId,
      @Nullable String pRegionId,
      @Nullable Formula pAddressTerm,
      @Nullable Formula pOffsetTerm,
      boolean pFill,
      long pFillSize,
      @Nullable CFAEdge pEdge) {
    MemoryEvent event =
        new MemoryEvent(
            events.size(),
            pInstanceId,
            pKind,
            pPoParentId,
            pPathGuard,
            pMemoryLocation,
            pCssaName,
            pVariable,
            pMutexId,
            pOtherInstanceId,
            pRegionId,
            pAddressTerm,
            pOffsetTerm,
            pFill,
            pFillSize,
            pEdge);
    events.put(event.id(), event);
    if (pPoParentId != MemoryEvent.NO_EVENT) {
      poPredecessors.put(event.id(), pPoParentId);
    }
    return event;
  }

  /** Adds one more program-order predecessor (an event after a merge point has several). */
  public void addPoPredecessor(int pEventId, int pPredecessorId) {
    poPredecessors.put(pEventId, pPredecessorId);
  }

  /** Program-order predecessor events within the same instance, by event id. */
  public ImmutableSetMultimap<Integer, Integer> getPoPredecessors() {
    return ImmutableSetMultimap.copyOf(poPredecessors);
  }

  /**
   * One allocation base: its address term and the byte size of the object it heads. The size lets
   * the encoder lay objects out in disjoint address ranges (a flat memory layout), so a full
   * address {@code base + offset} identifies a cell uniquely across all objects and interior
   * pointers ({@code &a[i]}, {@code &s.field}) alias exactly.
   */
  public record AddressBase(Formula term, long size) {}

  /**
   * Registers the solver term of one allocation base (the address of an address-taken variable or
   * of one heap allocation) together with the byte size of the object it heads, so the encoder can
   * place it in an address range disjoint from every other object.
   */
  public void addAddressBase(Formula pBase, long pSize) {
    addressBases.add(new AddressBase(pBase, pSize));
  }

  public ImmutableList<AddressBase> getAddressBases() {
    return ImmutableList.copyOf(addressBases);
  }

  public MemoryEvent getEvent(int pId) {
    return events.get(pId);
  }

  public ImmutableList<MemoryEvent> getEvents() {
    return ImmutableList.copyOf(events.values());
  }

  /** Returns the already known instance for the given key, or null. */
  public @Nullable ThreadInstance getInstance(InstanceKey pKey) {
    return instancesByKey.get(pKey);
  }

  public ThreadInstance getInstance(int pId) {
    return instances.get(pId);
  }

  public ImmutableList<ThreadInstance> getInstances() {
    return ImmutableList.copyOf(instances.values());
  }

  /** Allocates a new instance for the given, so far unknown, key. */
  public ThreadInstance newInstance(InstanceKey pKey) {
    checkArgument(!instancesByKey.containsKey(pKey), "duplicate thread instance %s", pKey);
    ThreadInstance instance = new ThreadInstance(instances.size(), pKey);
    instances.put(instance.getId(), instance);
    instancesByKey.put(pKey, instance);
    return instance;
  }

  /** Records one more CREATE event that starts the given instance. */
  public void addCreateEvent(int pInstanceId, int pCreateEventId) {
    instances.get(pInstanceId).addCreateEvent(pCreateEventId);
  }

  /** Adds one already guarded edge formula (guard of the source state implies edge semantics). */
  public void addPathConstraint(BooleanFormula pConstraint) {
    pathConstraints.add(pConstraint);
  }

  public ImmutableList<BooleanFormula> getPathConstraints() {
    return ImmutableList.copyOf(pathConstraints);
  }

  /** Marks that some path was cut at the loop bound, making safety verdicts unsound. */
  public void markTruncated() {
    truncated = true;
  }

  public boolean isTruncated() {
    return truncated;
  }
}
