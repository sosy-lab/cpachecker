/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;

public class CompositeStrategy extends AbstractCFAMutationStrategy {

  private final ImmutableList<AbstractCFAMutationStrategy> strategiesList;
  private final UnmodifiableIterator<AbstractCFAMutationStrategy> strategies;
  private AbstractCFAMutationStrategy currentStrategy;

  public CompositeStrategy(LogManager pLogger) {
    super(pLogger);
    strategiesList =
        ImmutableList.of(
            new FunctionBodyStrategy(pLogger),
            new GlobalDeclarationStrategy(pLogger, 300),
            new ChainStrategy(pLogger, 10),
            new ChainStrategy(pLogger, 1),
            new SingleNodeStrategy(pLogger, 1),
            new AssumeEdgeStrategy(pLogger),
            new GlobalDeclarationStrategy(pLogger, 1));
    strategies = strategiesList.iterator();
    currentStrategy = strategies.next();
  }

  @Override
  public boolean mutate(ParseResult parseResult) {
    logger.logf(Level.FINE, "Mutation strategy %s", currentStrategy);
    boolean answer = currentStrategy.mutate(parseResult);
    while (!answer) {
      if (!strategies.hasNext()) {
        return answer;
      }
      currentStrategy = strategies.next();
      logger.logf(Level.INFO, "Switching strategy to %s", currentStrategy);
      answer = currentStrategy.mutate(parseResult);
    }
    return answer;
  }

  @Override
  public void rollback(ParseResult parseResult) {
    currentStrategy.rollback(parseResult);
  }

  @Override
  public long countPossibleMutations(ParseResult parseResult) {
    long sum = 0;
    for (AbstractCFAMutationStrategy strategy : strategiesList) {
      long term = strategy.countPossibleMutations(parseResult);
      logger.logf(Level.INFO, "Strategy %s: %d possible mutations", strategy, term);
      sum += term;
    }
    return sum;
  }
}
