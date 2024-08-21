// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;

/**
 * A {@link CounterexampleFilter} that ignores the concrete edges of paths and looks only at the
 * function calls. If those are equal, the paths are considered similar.
 *
 * <p>In contrast to {@link StacktraceCounterexampleFilter} we directly use function call nodes.
 *
 * <p>This filter is very cheap.
 */
public class StacktraceCounterexampleFilter
    extends AbstractSetBasedCounterexampleFilter<ImmutableList<CFANode>> {

  public StacktraceCounterexampleFilter(
      Configuration pConfig, LogManager pLogger, ConfigurableProgramAnalysis pCpa) {
    super(pConfig, pLogger, pCpa);
  }

  @Override
  protected Optional<ImmutableList<CFANode>> getCounterexampleRepresentation(
      CounterexampleInfo counterexample) {
    ARGState lastState = counterexample.getTargetPath().getLastState();
    ImmutableList.Builder<CFANode> stacktrace = ImmutableList.builder();
    stacktrace.add(extractLocation(lastState));
    CallstackState callstack = extractStateByType(lastState, CallstackState.class);
    do {
      stacktrace.add(callstack.getCallNode());
    } while ((callstack = callstack.getPreviousState()) != null);
    return Optional.of(stacktrace.build());
  }
}
