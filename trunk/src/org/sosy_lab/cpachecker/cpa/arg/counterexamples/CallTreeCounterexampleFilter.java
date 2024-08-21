// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * A {@link CounterexampleFilter} that ignores the concrete edges of paths and looks only at the
 * function call trees. If those are equal, the paths are considered similar.
 *
 * <p>Note that in the following program, paths through both branches are similar (call locations
 * are ignored, only function names matter): <code>
 * void f() { }
 * void main() {
 *   if (...) { f(); } else { f(); }
 * }
 * </code> This filter is cheap and subsumes {@link PathEqualityCounterexampleFilter}.
 */
public class CallTreeCounterexampleFilter
    extends AbstractSetBasedCounterexampleFilter<ImmutableList<CFANode>> {

  public CallTreeCounterexampleFilter(
      Configuration pConfig, LogManager pLogger, ConfigurableProgramAnalysis pCpa) {
    super(pConfig, pLogger, pCpa);
  }

  @Override
  protected Optional<ImmutableList<CFANode>> getCounterexampleRepresentation(
      CounterexampleInfo counterexample) {
    return Optional.of(
        from(counterexample.getTargetPath().asStatesList())
            .transform(AbstractStates::extractLocation)
            .filter(or(instanceOf(FunctionEntryNode.class), instanceOf(FunctionExitNode.class)))
            .toList());
  }
}
