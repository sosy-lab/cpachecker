/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.FluentIterable.from;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * A {@link CounterexampleFilter} that ignores the concrete edges of paths
 * and looks only at the function call trees.
 * If those are equal, the paths are considered similar.
 *
 * Note that in the following program, paths through both branches are similar
 * (call locations are ignored, only function names matter):
 * <code>
 * void f() { }
 * void main() {
 *   if (...) { f(); } else { f(); }
 * }
 * </code>
 *
 * This filter is cheap and subsumes {@link PathEqualityCounterexampleFilter}.
 */
public class CallTreeCounterexampleFilter extends AbstractSetBasedCounterexampleFilter<ImmutableList<CFANode>> {

  public CallTreeCounterexampleFilter(Configuration pConfig, LogManager pLogger, ConfigurableProgramAnalysis pCpa) {
    super(pConfig, pLogger, pCpa);
  }

  @Override
  protected Optional<ImmutableList<CFANode>> getCounterexampleRepresentation(CounterexampleInfo counterexample) {
    return Optional.of(
        from(counterexample.getTargetPath().asStatesList())
            .transform(AbstractStates.EXTRACT_LOCATION)
            .filter(or(
                      instanceOf(FunctionEntryNode.class),
                      instanceOf(FunctionExitNode.class)))
            .toList()
            );
  }
}
