// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class PreferenceOrder {

  /** The set of CFAEdges that must be executed before {@link PreferenceOrder#subsequentEdge}. */
  public final ImmutableSet<CFAEdge> precedingEdges;

  /** The CFAEdge that is executed once all {@link PreferenceOrder#precedingEdges} are executed. */
  public final CFAEdge subsequentEdge;

  public PreferenceOrder(ImmutableSet<CFAEdge> pPrecedingEdges, CFAEdge pSubsequentEdge) {
    precedingEdges = pPrecedingEdges;
    subsequentEdge = pSubsequentEdge;
  }
}
