// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import static com.google.common.base.Preconditions.checkNotNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

/**
 * This class is used to signal a missing block abstraction. It contains some data that might help
 * to compute the missing block.
 *
 * <p>This class implements many interfaces, because we want to insert this state into an arbitrary
 * reached-set, and some types of reached-sets have special requirements.
 */
@SuppressFBWarnings(justification = "serialization currently not needed", value = "SE_BAD_FIELD")
public final class MissingBlockAbstractionState extends AbstractSingleWrapperState {

  private static final long serialVersionUID = 1L;

  private final AbstractState reducedState;
  private final Precision reducedPrecision;
  private final Block block;
  private final @Nullable ReachedSet reachedSet;

  /**
   * Create instance.
   *
   * @param pState the non-reduced state at the block entry
   * @param pReducedState the reduced state at the block entry
   * @param pBlock the entered block
   * @param pReachedSet an optional reached-set from the BAM cache, such that its initial state
   *     matches the pReducedState and its block matches the pBlock
   */
  public MissingBlockAbstractionState(
      AbstractState pState,
      AbstractState pReducedState,
      Precision pReducedPrecision,
      Block pBlock,
      @Nullable ReachedSet pReachedSet) {
    super(checkNotNull(pState));
    reducedState = checkNotNull(pReducedState);
    reducedPrecision = checkNotNull(pReducedPrecision);
    block = checkNotNull(pBlock);
    reachedSet = pReachedSet;
  }

  public AbstractState getState() {
    return getWrappedState();
  }

  public AbstractState getReducedState() {
    return reducedState;
  }

  public Precision getReducedPrecision() {
    return reducedPrecision;
  }

  public Block getBlock() {
    return block;
  }

  public ReachedSet getReachedSet() {
    return reachedSet;
  }

  @Override
  public String toString() {
    return String.format(
        "missing block summary for state %s with reduced state %s at block entry %s",
        getWrappedState(), reducedState, block.getCallNodes());
  }
}
