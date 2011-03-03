/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.cbmctools;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.CounterexampleChecker;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Counterexample checker that creates a C program for the counterexample
 * and calls CBMC on it.
 */
@Options(prefix="cbmc")
public class CBMCChecker implements CounterexampleChecker, Statistics {

  private final Map<String, CFAFunctionDefinitionNode> cfa;
  private final LogManager logger;
  
  private final Timer cbmcTime = new Timer();

  @Option(name="dumpCBMCfile", type=Option.Type.OUTPUT_FILE)
  private File CBMCFile;
  
  public CBMCChecker(Map<String, CFAFunctionDefinitionNode> cfa, Configuration config, LogManager logger) throws InvalidConfigurationException, CPAException {
    this.cfa = cfa;
    this.logger = logger;
    config.inject(this);
  }
  
  @Override
  public boolean checkCounterexample(ARTElement pRootElement, ARTElement pErrorElement,
      Set<ARTElement> pErrorPathElements) throws CPAException {
    
    String pathProgram = AbstractPathToCTranslator.translatePaths(cfa, pRootElement, pErrorPathElements);
    
    cbmcTime.start();
    try {
      return CProver.checkFeasibility(pathProgram, logger, CBMCFile);
    
    } catch (IOException e) {
      throw new CPAException("Could not verify program with CBMC (" + e.getMessage() + ")");

    } finally {
      cbmcTime.stop();
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
    out.println("Time for running CBMC:              " + cbmcTime);
  }

  @Override
  public String getName() {
    return "CBMC Counterexample Check";
  }
}
