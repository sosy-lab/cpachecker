/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.bam;

import static com.google.common.base.Preconditions.checkNotNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nullable;
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
      ReachedSet pReachedSet) {
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
