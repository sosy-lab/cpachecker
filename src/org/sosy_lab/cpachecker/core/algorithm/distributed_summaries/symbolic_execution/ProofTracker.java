// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;

/**
 * Detects the proofs of symbolic block execution, two of which can come long before every block
 * runs out of work:
 *
 * <ul>
 *   <li><b>Forward:</b> no postcondition is queued or processed. Every precondition a block accepts
 *       is checked against the target locations inside the block, so once the forward exploration
 *       is exhausted, every reachable state has been checked against every target location.
 *   <li><b>Backward:</b> no violation condition is queued or processed, and no block except the
 *       root holds a violation condition while it has a precondition. A block without a
 *       precondition sends the full preimage of every new violation condition to its predecessors,
 *       so the violation conditions then describe all states from which a target location is
 *       reachable. The root has checked the ones it received against its precondition, which
 *       contains all states. A block with a precondition keeps new violation conditions to itself,
 *       so a later precondition could still meet them.
 * </ul>
 *
 * <p>Both proofs need every block to be initialized, because initializing sends the first
 * postcondition of the root and the first violation condition of every block with a target
 * location. Neither kind of message can arise later without a message of the same kind: only
 * processing a postcondition sends postconditions, and only processing a violation condition sends
 * violation conditions. Once a count is zero after the initialization, it therefore stays zero.
 *
 * <p>A message counts from the moment it is sent until its receiver has processed it and sent
 * everything it produced. A receiver therefore reports what it sends, and whether it now blocks the
 * backward proof, before it reports its message as processed, so there is no moment in which work
 * is in flight without being counted.
 */
final class ProofTracker {

  enum Proof {
    FORWARD("the forward exploration is exhausted"),
    BACKWARD("the backward exploration is exhausted"),
    NO_WORK_LEFT("no block has work left");

    private final String description;

    Proof(String pDescription) {
      description = pDescription;
    }

    @Override
    public String toString() {
      return description;
    }
  }

  private final boolean forward;
  private final boolean backward;
  private final AtomicLong postconditions = new AtomicLong();
  private final AtomicLong violationConditions = new AtomicLong();
  private final AtomicInteger uninitializedBlocks;
  private final Set<String> blockingBlocks = ConcurrentHashMap.newKeySet();
  private final AtomicReference<Proof> proof = new AtomicReference<>();

  /**
   * Creates the tracker for the given number of blocks.
   *
   * @param pForward whether to detect the forward proof
   * @param pBackward whether to detect the backward proof
   */
  ProofTracker(int pBlocks, boolean pForward, boolean pBackward) {
    uninitializedBlocks = new AtomicInteger(pBlocks);
    forward = pForward;
    backward = pBackward;
  }

  /** Called before a message is handed to its receivers. */
  void sending(DssMessage pMessage, int pReceivers) {
    switch (pMessage.getType()) {
      case POST_CONDITION -> postconditions.addAndGet(pReceivers);
      case VIOLATION_CONDITION -> violationConditions.addAndGet(pReceivers);
      case RESULT, EXCEPTION, WITNESS -> {}
    }
  }

  /** Called by a receiver after it processed a message and sent everything it produced. */
  void processed(DssMessage pMessage) {
    switch (pMessage.getType()) {
      case POST_CONDITION -> postconditions.decrementAndGet();
      case VIOLATION_CONDITION -> violationConditions.decrementAndGet();
      case RESULT, EXCEPTION, WITNESS -> {}
    }
  }

  /** Called once per block, after it sent the messages of its initialization. */
  void initialized() {
    uninitializedBlocks.decrementAndGet();
  }

  /** Called after a block processed a message, before it reports the message as processed. */
  void update(SymbolicBlockWorker pWorker) {
    if (pWorker.blocksBackwardProof()) {
      blockingBlocks.add(pWorker.getBlockId());
    }
  }

  /**
   * The proof that holds now, if any. Only the first caller that finds a proof gets it, so that the
   * verdict is announced once.
   */
  Optional<Proof> claimProof() {
    if (uninitializedBlocks.get() > 0) {
      return Optional.empty();
    }
    Proof found = null;
    if (forward && postconditions.get() == 0) {
      found = Proof.FORWARD;
      // Read the count before the blocking blocks: a block that starts blocking while it processes
      // a violation condition reports it before the count can drop.
    } else if (backward && violationConditions.get() == 0 && blockingBlocks.isEmpty()) {
      found = Proof.BACKWARD;
    }
    return found != null && claim(found) ? Optional.of(found) : Optional.empty();
  }

  /** Records the given proof unless another one was recorded before. */
  boolean claim(Proof pProof) {
    return proof.compareAndSet(null, pProof);
  }

  /** The proof that was recorded, if any. */
  Optional<Proof> proof() {
    return Optional.ofNullable(proof.get());
  }
}
