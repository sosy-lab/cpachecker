// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CBMCExecutor;

@Options
public class ExternalCBMCAlgorithm implements Algorithm, StatisticsProvider {

  private static final DummyTargetState DUMMY_TARGET_STATE =
      DummyTargetState.withSingleProperty("Target location reachabable with CBMC!");

  private final Path fileName;
  private final LogManager logger;
  private final Stats stats = new Stats();

  @Option(secure=true, name="analysis.entryFunction", regexp="^[_a-zA-Z][_a-zA-Z0-9]*$",
      description="entry function")
      private String mainFunctionName = "main";

  @Option(secure=true, name="cbmc.timelimit",
      description="maximum time limit for CBMC (0 is infinite)")
      private int timelimit = 0; // milliseconds

  @Option(secure=true, name="cbmc.options.intWidth",
      description="set width of int (16, 32 or 64)")
      private int intWidth = 32;

  @Option(secure=true, name="cbmc.options.errorLabel",
      description="specify the name of the error label")
      private String errorLabel = "ERROR";

  @Option(secure=true, name="cbmc.options.unwindings",
      description="specify the limit for unwindings (0 is infinite)")
      private int unwind = 0;

  @Option(secure=true, name="cbmc.options.nuaf",
      description="disable unwinding assertions violation error")
      private boolean noUnwindingAssertions = false;

  public ExternalCBMCAlgorithm(Path fileName, Configuration config, LogManager logger)
      throws InvalidConfigurationException {
    this.fileName = fileName;
    this.logger = logger;
    config.inject(this);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    assert pReachedSet.isEmpty();

    // run CBMC
    logger.log(Level.INFO, "Starting CBMC algorithm.");
    stats.cbmcTime.start();
    CBMCExecutor cbmc;
    int exitCode;
    try {
      cbmc = new CBMCExecutor(logger, buildCBMCArguments());
      exitCode = cbmc.join(timelimit);

    } catch (IOException e) {
      throw new CPAException("Could not verify program with CBMC (" + e.getMessage() + ")");

    } catch (TimeoutException e) {
      logger.log(Level.INFO, "CBMC Algorithm timed out.");
      return AlgorithmStatus.UNSOUND_AND_PRECISE;

    } finally {
      stats.cbmcTime.stop();
      logger.log(Level.INFO, "CBMC Algorithm finished.");
    }

    if (cbmc.getResult() == null) {
      // exit code and stderr are already logged with level WARNING
      // throw new CPAException("CBMC could not verify the program (CBMC exit code was " + exitCode + ")!");
      logger.log(Level.INFO, "CBMC could not verify the program (CBMC exit code was " + exitCode + ")!");
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    }

    // ERROR is REACHED
    if (cbmc.getResult()) {
      // if this is unwinding assertions violation the analysis result is UNKNOWN
      if (cbmc.didUnwindingAssertionFail()) {
        logger.log(Level.INFO, "CBMC terminated with unwinding assertions violation");
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      } else {
        pReachedSet.add(DUMMY_TARGET_STATE, SingletonPrecision.getInstance());
        assert pReachedSet.size() == 1 && pReachedSet.hasWaitingState();

        // remove dummy state from waitlist
        pReachedSet.popFromWaitlist();
        assert !pReachedSet.hasWaitingState();
      }
    }

    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  private List<String> buildCBMCArguments() {
    List<String> paramsList = new ArrayList<>();

    paramsList.add("--function");
    paramsList.add(mainFunctionName);
    paramsList.add("--"+intWidth);
    paramsList.add("--unwind");
    paramsList.add(Integer.toString(unwind));
    paramsList.add("--error-label");
    paramsList.add(errorLabel);
    paramsList.add("--stop-on-fail");

    if (noUnwindingAssertions) {
      paramsList.add("--no-unwinding-assertions");
    }

    paramsList.add(fileName.toString());
    return paramsList;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  private static class Stats implements Statistics {

    private final Timer cbmcTime = new Timer();

    @Override
    public String getName() {
      return "CBMC";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
      out.println("Time for running CBMC: " + cbmcTime);
    }
  }
}
