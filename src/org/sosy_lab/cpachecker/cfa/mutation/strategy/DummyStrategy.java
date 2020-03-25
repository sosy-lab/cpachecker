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

import java.util.Collection;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;

public class DummyStrategy extends AbstractCFAMutationStrategy {
  private int steps;

  public DummyStrategy(LogManager pLogger) {
    super(pLogger);
    steps = 1;
  }

  public DummyStrategy(LogManager pLogger, int pSteps) {
    super(pLogger);
    steps = pSteps;
  }

  @Override
  public boolean mutate(ParseResult pParseResult) {
    if (steps > 0) {
      steps--;
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void rollback(ParseResult pParseResult) {
    assert false : "Dummy strategy does not change parseResult, there has to be no rollbacks";
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {}

  @Override
  public void makeAftermath(ParseResult pParseResult) {}
}
