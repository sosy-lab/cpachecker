// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.witnesses;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public interface RelevantArgStatesCollector {

  /**
   * A class to keep track of parent child relations between abstract states which enter a function
   * and those which exit it
   */
  public record FunctionEntryExitPair(ARGState entry, ARGState exit) {}

  /** A data structure for collecting the relevant information for a witness from an ARG */
  public record CollectedARGStates(
      Multimap<CFANode, ARGState> loopInvariants,
      Multimap<CFANode, ARGState> functionCallInvariants,
      Multimap<FunctionEntryNode, ARGState> functionContractRequires,
      Multimap<FunctionExitNode, FunctionEntryExitPair> functionContractEnsures) {

    public CollectedARGStates {
      checkNotNull(loopInvariants);
      checkNotNull(functionCallInvariants);
      checkNotNull(functionContractRequires);
      checkNotNull(functionContractEnsures);
    }

    public CollectedARGStates immutableCopy() {
      return new CollectedARGStates(
          ImmutableListMultimap.copyOf(loopInvariants),
          ImmutableListMultimap.copyOf(functionCallInvariants),
          ImmutableListMultimap.copyOf(functionContractRequires),
          ImmutableListMultimap.copyOf(functionContractEnsures));
    }
  }

  /**
   * Collect the relevant states from the ARG starting at the given root state for the export of a
   * witness
   *
   * @param pRootState the state for where the traversal of the ARG should start for the collection
   *     of the information
   * @return the collected information about the ARG
   */
  public CollectedARGStates getRelevantStates(ARGState pRootState);
}
