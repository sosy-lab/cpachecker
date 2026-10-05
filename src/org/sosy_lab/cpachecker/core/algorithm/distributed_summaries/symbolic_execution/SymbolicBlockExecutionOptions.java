// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(prefix = "symbolicBlockExecution")
final class SymbolicBlockExecutionOptions {

  @Option(
      secure = true,
      description =
          "Number of threads that analyze blocks. With 0, every block gets a thread and a message"
              + " queue of its own, exactly like the workers of distributed summary synthesis."
              + " Otherwise, this many threads take the blocks with pending messages from a"
              + " coordinator, each preferring the blocks it analyzed before; a thread without"
              + " work steals a block from another thread. With -1, there is a thread for every"
              + " processor available to the JVM.")
  @IntegerOption(min = -1)
  private int threads = 0;

  /** How the solver eliminates the existential variables that the projection keeps. */
  enum QuantifierElimination {
    /** Keep them as private variables. */
    NONE,
    /** Light-weight elimination, e.g., the tactic qe-light of Z3. */
    LIGHT,
    /** Complete elimination, which can take very long on bitvectors. */
    FULL
  }

  @Option(
      secure = true,
      description =
          "How to eliminate the existential variables that the projection keeps, if the solver"
              + " supports quantifiers. Variables that stay become private variables of the"
              + " condition. FULL can be very expensive with bitvectors.")
  private QuantifierElimination quantifierElimination = QuantifierElimination.LIGHT;

  @Option(
      secure = true,
      description =
          "Quantify the private variables of the known conditions in coverage checks, if the"
              + " solver supports quantifiers. Without it, a known condition with private"
              + " variables hardly ever covers a new condition.")
  private boolean quantifiedCoverage = true;

  @Option(
      secure = true,
      description =
          "Whether a block analyzes and propagates only the states of a new condition that its"
              + " known conditions do not contain yet, i.e., the new condition conjoined with the"
              + " negation of all known ones. Otherwise, it analyzes and propagates the whole new"
              + " condition, whose messages stay smaller but repeat known states. Conditions that"
              + " add no state are ignored either way.")
  private boolean subtractKnownStates = true;

  @Option(
      secure = true,
      description =
          "Prove the program safe as soon as no postcondition is queued or processed, i.e., once"
              + " the forward exploration is exhausted. Every reachable state was then checked"
              + " against the target locations of its block.")
  private boolean forwardProof = true;

  @Option(
      secure = true,
      description =
          "Prove the program safe as soon as no violation condition is queued or processed and no"
              + " block except the root holds a violation condition while it has a precondition,"
              + " i.e., once the backward exploration is exhausted.")
  private boolean backwardProof = true;

  SymbolicBlockExecutionOptions(Configuration pConfiguration) throws InvalidConfigurationException {
    pConfiguration.inject(this);
  }

  int threads() {
    return threads == -1 ? Runtime.getRuntime().availableProcessors() : threads;
  }

  QuantifierElimination quantifierElimination() {
    return quantifierElimination;
  }

  boolean quantifiedCoverage() {
    return quantifiedCoverage;
  }

  boolean subtractKnownStates() {
    return subtractKnownStates;
  }

  boolean forwardProof() {
    return forwardProof;
  }

  boolean backwardProof() {
    return backwardProof;
  }

  boolean useQuantifiers() {
    return quantifierElimination != QuantifierElimination.NONE || quantifiedCoverage;
  }
}
