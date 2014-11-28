package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

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
  Timer strengthenTimer = new Timer();
  Timer abstractionTimer = new Timer();

  int valueDetCalls = 0;

  public PolicyIterationStatistics(Configuration config)
      throws InvalidConfigurationException {

    config.inject(this, PolicyIterationStatistics.class);
  }

  @Override
  public void printStatistics(PrintStream out, CPAcheckerResult.Result result,
      ReachedSet reached) {

    printTimer(out, valueDeterminationTimer, "value determination");
    printTimer(out, policyPropagationTimer, "policy propagation");
    printTimer(out, valueDeterminationSolverTimer, "value determination solver");
    out.printf("Number of calls to the value determination solver: %s\n", valueDetCalls);
    printTimer(out, timeInMerge, "merge-step");
    printTimer(out, strengthenTimer, "strengthen");
    printTimer(out, strengthenTimer, "abstraction");
  }

  public void printTimer(PrintStream out, Timer t, String name) {
    out.printf("Time spent in %s: %s (Max: %s)\n",
        name, t, t.getMaxTime().formatAs(TimeUnit.SECONDS));
  }

  @Override
  public String getName() {
    return "PolicyIterationCPA";
  }
}
