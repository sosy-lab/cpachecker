/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.algorithm.cbmctools.CBMCDummyErrorElement;
import org.sosy_lab.cpachecker.core.algorithm.cbmctools.CBMCExecutor;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options()
public class ExternalCBMCAlgorithm implements Algorithm, StatisticsProvider {

  private final String fileName;
  private final LogManager logger;
  private final Timer cbmcTime = new Timer();

  @Option(name="analysis.entryFunction", regexp="^[_a-zA-Z][_a-zA-Z0-9]*$",
      description="entry function")
      private String mainFunctionName = "main";

  @Option(name="cbmc.timelimit",
      description="maximum time limit for CBMC (0 is infinite)")
      private int timelimit = 0; // milliseconds

  @Option(name="cbmc.options.intWidth",
      description="set width of int (16, 32 or 64)")
      private int intWidth = 32;

  @Option(name="cbmc.options.errorLabel",
      description="specify the name of the error label")
      private String errorLabel = "ERROR";

  @Option(name="cbmc.options.unwindings",
      description="specify the limit for unwindings (0 is infinite)")
      private int unwind = 0;

  @Option(name="cbmc.options.nuaf",
      description="disable unwinding assertions violation error")
      private boolean noUnwindingAssertions = false;

  public ExternalCBMCAlgorithm(String fileName, Configuration config, LogManager logger) throws InvalidConfigurationException {
    this.fileName = fileName;
    this.logger = logger;
    config.inject(this);
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return null;
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    assert pReachedSet.isEmpty();

    // run CBMC
    logger.log(Level.INFO, "Starting CBMC algorithm.");
    cbmcTime.start();
    CBMCExecutor cbmc;
    int exitCode;
    try {

      String CBMCArgs[] = buildCBMCArguments();

      cbmc = new CBMCExecutor(logger, new File(fileName), CBMCArgs);
      exitCode = cbmc.join(timelimit);

    } catch (IOException e) {
      throw new CPAException("Could not verify program with CBMC (" + e.getMessage() + ")");

    } catch (TimeoutException e) {
      logger.log(Level.INFO, "CBMC Algorithm timed out.");
      return false;

    } finally {
      cbmcTime.stop();
      logger.log(Level.INFO, "CBMC Algorithm finished.");
    }

    if (cbmc.getResult() == null) {
      // exit code and stderr are already logged with level WARNING
      // throw new CPAException("CBMC could not verify the program (CBMC exit code was " + exitCode + ")!");
      logger.log(Level.INFO, "CBMC could not verify the program (CBMC exit code was " + exitCode + ")!");
      return false;
    }

    // ERROR is REACHED
    if(cbmc.getResult()){
      // if this is unwinding assertions violation the analysis result is UNKNOWN
      if(cbmc.didUnwindingAssertionFailed()){
        logger.log(Level.INFO, "CBMC terminated with unwinding assertions violation");
        return false;
      }
      else{
        pReachedSet.add(new CBMCDummyErrorElement(), SingletonPrecision.getInstance());
        assert pReachedSet.size() == 1 && pReachedSet.hasWaitingElement();

        // remove dummy element from waitlist
        pReachedSet.popFromWaitlist();
        assert !pReachedSet.hasWaitingElement();
      }
    }

    return true;
  }

  private String[] buildCBMCArguments() {
    List<String> paramsList = new ArrayList<String>();

    paramsList.add("cbmc");
    paramsList.add("--function");
    paramsList.add(mainFunctionName);
    paramsList.add("--"+intWidth);
    paramsList.add("--unwind");
    paramsList.add(Integer.toString(unwind));
    paramsList.add("--error-label");
    paramsList.add(errorLabel);

    if(noUnwindingAssertions){
      paramsList.add("--no-unwinding-assertions");
    }

    String s[] = new String[paramsList.size()];
    return paramsList.toArray(s);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    // TODO Auto-generated method stub
  }

}
