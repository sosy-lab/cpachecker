/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;


/**
 * We need the values of this options at two code sites (CFAcreator and CoreComponentsFactory).
 * Should not define the option twice.
 * Therefore we define them once here.
 * Each time this class is instantiated, the options are set and can be accessed.
 *
 * Not nice but small and obvious.
 */
@Options
public class TigerConfiguration {

  public TigerConfiguration(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }
  @Option(secure=true, name="analysis.algorithm.tiger",
      description = "Use Test Input GEneRator algorithm (Information Reuse for Multi-Goal Reachability Analyses, ESOP'13)")
  public boolean useTigerAlgorithm = false;
  
}
