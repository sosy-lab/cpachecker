package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

@Options(prefix="cpa.stator.policy")
public class PolicyIterationStatistics implements Statistics {
  Timer valueDeterminationTimer = new Timer();
  Timer policyPropagationTimer = new Timer();
  Timer valueDeterminationSolverTimer = new Timer();
  Timer timeInMerge = new Timer();

  int valueDetCalls = 0;

  public PolicyIterationStatistics(Configuration config)
      throws InvalidConfigurationException {

    config.inject(this, PolicyIterationStatistics.class);
  }

  @Override
  public void printStatistics(PrintStream out, CPAcheckerResult.Result result,
      ReachedSet reached) {

    out.printf("Time spent in value determination: %s (Max: %s)\n",
        valueDeterminationTimer,
        valueDeterminationTimer.getMaxTime().formatAs(TimeUnit.SECONDS));
    out.printf("Time spent in policy propagation: %s (Max: %s)\n",
        policyPropagationTimer,
        policyPropagationTimer.getMaxTime().formatAs(TimeUnit.SECONDS));
    out.printf("Time spent in value determination solver: %s (Max: %s)\n",
        valueDeterminationSolverTimer,
        valueDeterminationSolverTimer.getMaxTime().formatAs(TimeUnit.SECONDS));
    out.printf("Number of calls to the value determination solver: %s\n", valueDetCalls);
    out.printf("Time spent in merge-step: %s (Max: %s)\n",
        timeInMerge,
        timeInMerge.getMaxTime().formatAs(TimeUnit.SECONDS));
  }

  @Nullable
  @Override
  public String getName() {
    return "PolicyIterationCPA";
  }

}
