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
package org.sosy_lab.cpachecker.cfa.mutation.strategy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;

public class CompositeStrategy extends AbstractCFAMutationStrategy {

  protected final ImmutableList<AbstractCFAMutationStrategy> strategiesList;
  protected UnmodifiableIterator<AbstractCFAMutationStrategy> strategies;
  protected AbstractCFAMutationStrategy currentStrategy;
  private int round = 0;
  private final Deque<Integer> rounds;
  private final Deque<Integer> rollbacks;

  public CompositeStrategy(LogManager pLogger) {
    super(pLogger);
    strategiesList =
        ImmutableList.of(
            // First, try to remove most functions.
            //   Remove functions, 60-150 rounds for 10-15k nodes in input, 500-800 nodes remain.
            new FunctionStrategy(pLogger, 5, 1),
            //   Check that analysis result remains unchanged.
            new DummyStrategy(pLogger),
            //   It seems it does not change result, so try to remove all in one round.
            new BlankNodeStrategy(pLogger, 5, 0),

            // Second, mutate remained functions somehow.
            //   1. Remove unneeded assumes and statements.
            new CycleStrategy(pLogger),
            //   Check the result
            new DummyStrategy(pLogger),
            //   2. Remove loops on nodes (edges from node to itself).
            new NodeWithLoopStrategy(pLogger, 5, 0),
            //   Now we can remove delooped blank edges.
            new BlankNodeStrategy(pLogger, 5, 0),
            //   3. Remove unneeded declarations. TODO *unneeded*
            new DeclarationStrategy(pLogger, 5, 0),
            new DummyStrategy(pLogger),
            new CycleStrategy(pLogger),
            new DummyStrategy(pLogger),
            //   4. Linearize loops: instead branching
            //   insert loop body branch and "exit" branch successively,
            //   as if loop is "executed" once.
            new LoopAssumeEdgeStrategy(pLogger, 5, 0),
            new DummyStrategy(pLogger),

            // Third, remove functions-spoilers: they just call another function
            // It seems it does not change result, so try to remove all in one round
            new SpoilerFunctionStrategy(pLogger, 5, 0),
            new DummyStrategy(pLogger),

            // And last: remove global declarations, certainly of already removed functions.
            // TODO declarations of global variables and types
            new GlobalDeclarationStrategy(pLogger, 5, 1),
            new DummyStrategy(pLogger));
    strategies = strategiesList.iterator();
    currentStrategy = strategies.next();
    rounds = new ArrayDeque<>();
    rollbacks = new ArrayDeque<>();
  }

  public CompositeStrategy(
      LogManager pLogger, ImmutableList<AbstractCFAMutationStrategy> pStrategiesList) {
    super(pLogger);
    strategiesList = pStrategiesList;
    strategies = strategiesList.iterator();
    currentStrategy = strategies.next();
    rounds = new ArrayDeque<>();
    rollbacks = new ArrayDeque<>();
  }

  @Override
  public boolean mutate(ParseResult parseResult) {
    logger.logf(Level.INFO, "Round %d. Mutation strategy %s", ++round, currentStrategy);
    rounds.addLast(rounds.pollLast() + 1);
    boolean answer = currentStrategy.mutate(parseResult);
    while (!answer) {
      logger.logf(
          Level.INFO,
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
      logger.logf(Level.INFO, "Switching strategy to %s", currentStrategy);
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
      logger.logf(Level.INFO, "Strategy %s: %d possible mutations", strategy, term);
      sum += term;

      if (!rounds.isEmpty()) {
        logger.logf(
            Level.INFO,
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
