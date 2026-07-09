// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

/**
 * One event of the ordering-consistency exploration. Events of one thread instance form a tree via
 * {@link #poParentId} (program order along each explored path; sibling branches have mutually
 * exclusive guards).
 *
 * @param id globally unique event id
 * @param instanceId the thread instance this event belongs to
 * @param kind the kind of the event
 * @param poParentId id of the preceding event of the same instance on the path, or {@link
 *     #NO_EVENT} for the first event(s) of an instance
 * @param pathGuard guard of the event within its own instance's tree (the creation guard of the
 *     instance is not included; it is added during encoding)
 * @param memoryLocation accessed global variable, for READ/WRITE events
 * @param cssaName the fresh qualified name minted for this access, for READ/WRITE events
 * @param variable the instantiated solver variable holding the read/written value, for READ/WRITE
 *     events
 * @param mutexId identifier of the mutex (or {@link #ATOMIC_BLOCK_MUTEX}), for LOCK/UNLOCK
 * @param otherInstanceId the created/joined thread instance, for CREATE/JOIN events, else {@link
 *     #NO_INSTANCE}
 * @param regionId alias region (canonical accessed type) for READ/WRITE events of address-taken
 *     variables and pointer dereferences; null for accesses that can never be aliased
 * @param addressTerm the base (object identity) of the access, for events with a {@link #regionId}:
 *     two region events touch the same object iff their base terms are equal (bases are pairwise
 *     distinct, so this equality is exact)
 * @param offsetTerm the byte offset within the object, for events with a {@link #regionId}; null
 *     means a zero offset. Two region events touch the same cell iff both base and offset are equal
 * @param fill whether this is a fill write that covers the whole object of its region (an
 *     initialization of a zero-initialized or indeterminate aggregate/allocation): it provides one
 *     value at every offset, so any same-base access of the region reads from it regardless of
 *     offset. This lets one event stand in for an array of any size
 * @param edge the CFA edge whose processing produced this event, or null for synthesized events
 *     (aggregate/heap fill writes) that correspond to no single edge. Used to reconstruct a
 *     sequentialized counterexample trace from the enabled events
 */
public record MemoryEvent(
    int id,
    int instanceId,
    EventKind kind,
    int poParentId,
    BooleanFormula pathGuard,
    @Nullable MemoryLocation memoryLocation,
    @Nullable String cssaName,
    @Nullable Formula variable,
    @Nullable String mutexId,
    int otherInstanceId,
    @Nullable String regionId,
    @Nullable Formula addressTerm,
    @Nullable Formula offsetTerm,
    boolean fill,
    @Nullable CFAEdge edge) {

  public static final int NO_EVENT = -1;
  public static final int NO_INSTANCE = -1;

  /** Pseudo-mutex representing {@code __VERIFIER_atomic_begin/end} blocks. */
  public static final String ATOMIC_BLOCK_MUTEX = "__VERIFIER_atomic__";

  /** Whether this access goes through the aliasing regime (address-equality read-from). */
  public boolean isRegionAccess() {
    return regionId != null;
  }
}
