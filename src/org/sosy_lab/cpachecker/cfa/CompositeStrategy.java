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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;

public class CompositeStrategy extends AbstractCFAMutationStrategy {

  private final ImmutableList<AbstractCFAMutationStrategy> strategiesList;
  private final UnmodifiableIterator<AbstractCFAMutationStrategy> strategies;
  private AbstractCFAMutationStrategy currentStrategy;
  private int round = 0;
  private final Deque<Integer> rounds;
  private final Deque<Integer> rollbacks;

  public CompositeStrategy(LogManager pLogger) {
    super(pLogger);
    strategiesList =
        ImmutableList.of(
            new FunctionBodyStrategy(pLogger, 5, 0),
            new BlankChainStrategy(pLogger, 5, 0),
            new ChainStrategy(pLogger, 5, 1),
            new AssumeEdgeStrategy(pLogger, 5, 1),
            //
            new BlankChainStrategy(pLogger, 5, 0),
            new SingleNodeStrategy(pLogger, 5, 1),
            new AssumeEdgeStrategy(pLogger, 5, 1),
            //
            new BlankChainStrategy(pLogger, 5, 0),
            new SingleNodeStrategy(pLogger, 5, 1),
            new AssumeEdgeStrategy(pLogger, 5, 1),
            //
            new SingleNodeStrategy(pLogger, 5, 1),
            new SpoilerFunctionStrategy(pLogger, 5, 0),
            new GlobalDeclarationStrategy(pLogger, 5, 0));
    strategies = strategiesList.iterator();
    currentStrategy = strategies.next();
    rounds = new ArrayDeque<>();
    rollbacks = new ArrayDeque<>();
  }

  @Override
  public boolean mutate(ParseResult parseResult) {
    logger.logf(Level.SEVERE, "Round %d. Mutation strategy %s", ++round, currentStrategy);
    rounds.addLast(rounds.pollLast() + 1);
    boolean answer = currentStrategy.mutate(parseResult);
    while (!answer) {
      logger.logf(
          Level.SEVERE,
          "Round %d. Mutation strategy %s finished in %d rounds with %d rollbacks.",
          round,
          currentStrategy,
          rounds.peekLast(),
          rollbacks.peekLast());
      rounds.addLast(0);
      rollbacks.addLast(0);
      if (!strategies.hasNext()) {
        return answer;
      }
      currentStrategy = strategies.next();
      logger.logf(Level.SEVERE, "Switching strategy to %s", currentStrategy);
      answer = currentStrategy.mutate(parseResult);
    }
    return answer;
  }

  @Override
  public void rollback(ParseResult parseResult) {
    rollbacks.addLast(rollbacks.pollLast() + 1);
    currentStrategy.rollback(parseResult);
  }

  @Override
  public int countPossibleMutations(ParseResult parseResult) {
    int sum = 0;
    for (AbstractCFAMutationStrategy strategy : strategiesList) {
      int term = strategy.countPossibleMutations(parseResult);
      logger.logf(Level.SEVERE, "Strategy %s: %d possible mutations", strategy, term);
      sum += term;

      if (!rounds.isEmpty()) {
        logger.logf(
            Level.SEVERE,
            "in %d rounds with %d rollbacks",
            rounds.pollFirst(),
            rollbacks.pollFirst());
      }
    }
    rounds.add(0);
    rollbacks.add(0);
    return sum;
  }
}
