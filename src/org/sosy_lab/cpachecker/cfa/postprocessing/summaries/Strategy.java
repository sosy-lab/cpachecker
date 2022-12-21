// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public interface Strategy {

  public enum StrategyQualifier {
    OverApproximating,
    Precise,
    Underapproximating,
  }

  /**
   * Gives a summary state back if the State can be summarized using the Strategy given.
   *
   * @param loopStartNode the cfaNode from which this strategy should begin
   * @return the ghostCFA describing the strategy being applied at the node.
   */
  public Optional<GhostCFA> summarize(final CFANode loopStartNode);

  /** Returns true if the Summary is Precise, false in any other case */
  public boolean isPrecise();

  default String getName() {
    return this.getClass().getSimpleName();
  }
}
