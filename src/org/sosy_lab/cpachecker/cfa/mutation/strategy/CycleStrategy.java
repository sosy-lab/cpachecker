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
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;

public class CycleStrategy extends CompositeStrategy {
  private int cycle = 0;

  public CycleStrategy(LogManager pLogger) {
    super(
        pLogger,
        ImmutableList.of(
            // First, remove AssumeEdges if possible
            new SimpleAssumeEdgeStrategy(pLogger, 5, 1),
            // Second, remove statements if possible
            new StatementNodeStrategy(pLogger, 5, 1),
            // Then remove blank edges
            new BlankNodeStrategy(pLogger, 5, 0)));
  }

  @Override
  public boolean mutate(ParseResult pParseResult) {
    if (super.mutate(pParseResult)) {
      return true;
    }
    logger.logf(Level.INFO, "Starting cycle %d", ++cycle);
    strategies = strategiesList.iterator();
    currentStrategy = strategies.next();
    return super.mutate(pParseResult);
  }

  @Override
  public String toString() {
    return super.toString() + ", " + cycle + " cycles";
  }
}
