/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
