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
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;

public class CompositeStrategy extends AbstractCFAMutationStrategy {

  protected final ImmutableList<AbstractCFAMutationStrategy> strategiesList;
  protected UnmodifiableIterator<AbstractCFAMutationStrategy> strategies;
  protected AbstractCFAMutationStrategy currentStrategy;

  public CompositeStrategy(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pLogger);
    strategiesList =
        ImmutableList.of(
            // First, try to remove most functions.
            //   Remove functions, 60-150 rounds for 10-15k nodes in input, 500-800 nodes remain.
            new FunctionStrategy(pConfig, pLogger, 5, 0),
            new BlankNodeStrategy(pLogger, 5, 0),
            new DummyStrategy(pLogger),

            // Second, mutate remained functions somehow.

            //   1. Remove unneeded assumes and statements.
            new CycleStrategy(pLogger),
            //   TODO remove remained branches with a return statement

            //   2. Remove loops on nodes (edges from node to itself).
            new NodeWithLoopStrategy(pLogger, 5, 0),
            new DummyStrategy(pLogger),
            //   Now we can remove delooped blank edges.
            new BlankNodeStrategy(pLogger, 5, 0),
            new DummyStrategy(pLogger),

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
            new SpoilerFunctionStrategy(pConfig, pLogger, 5, 0),
            new DummyStrategy(pLogger),

            // And last: remove global declarations, certainly of already removed functions.
            // TODO declarations of global variables and types
            new GlobalDeclarationStrategy(pLogger, 5, 1),
            new DummyStrategy(pLogger));
    strategies = strategiesList.iterator();
    currentStrategy = strategies.next();
  }

  public CompositeStrategy(
      LogManager pLogger, ImmutableList<AbstractCFAMutationStrategy> pStrategiesList) {
    super(pLogger);
    strategiesList = pStrategiesList;
    strategies = strategiesList.iterator();
    currentStrategy = strategies.next();
  }

  @Override
  public boolean mutate(ParseResult parseResult) {
    stats.rounds.inc();
    logger.logf(
        Level.INFO, "Round %d. Mutation strategy %s", stats.rounds.getValue(), currentStrategy);
    boolean answer = currentStrategy.mutate(parseResult);
    while (!answer) {
      logger.logf(
          Level.INFO,
          "Round %d. Mutation strategy %s finished in %d rounds with %d rollbacks.",
          stats.rounds.getValue(),
          currentStrategy,
          currentStrategy.stats.rounds.getValue(),
          currentStrategy.stats.rollbacks.getValue());
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
    stats.rollbacks.inc();
    currentStrategy.rollback(parseResult);
  }

  @Override
  public void makeAftermath(ParseResult pParseResult) {
    for (AbstractCFAMutationStrategy strategy : strategiesList) {
      strategy.makeAftermath(pParseResult);
      if (strategy instanceof DummyStrategy) {
        // TODO checksDone+=rounds or somthin
      }
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    for (AbstractCFAMutationStrategy str : strategiesList) {
      str.collectStatistics(pStatsCollection);
    }
  }
}
