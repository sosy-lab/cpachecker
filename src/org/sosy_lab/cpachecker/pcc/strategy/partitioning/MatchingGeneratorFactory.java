/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.MatchingGenerator;

public class MatchingGeneratorFactory {

  private MatchingGeneratorFactory() {}
  public static enum MatchingGenerators {
    RANDOM,
    HEAVY_EDGE
  }

  public static MatchingGenerator createMatchingGenerator( final LogManager pLogger, MatchingGenerators generator){
    switch(generator){
      case HEAVY_EDGE:
        return new HeavyEdgeMatchingGenerator(pLogger);
        default:
          return new RandomMatchingGenerator(pLogger);
    }
  }
}
