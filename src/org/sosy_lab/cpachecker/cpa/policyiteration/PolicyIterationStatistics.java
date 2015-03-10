package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

@Options(prefix="cpa.stator.policy")
public class PolicyIterationStatistics implements Statistics {

  private final @Nullable FormulaSlicingManager slicing;

  private final Timer valueDeterminationTimer = new Timer();
  private final Timer abstractionTimer = new Timer();
  private final Timer checkSATTimer = new Timer();
  final Timer optTimer = new Timer();

  public void startCheckSATTimer() {
    checkSATTimer.start();
  }

  public void stopCheckSATTimer() {
    checkSATTimer.stop();
  }

  public void startOPTTimer() {
    optTimer.start();
  }

  public void stopOPTTimer() {
    optTimer.stop();
  }

  public void startAbstractionTimer() {
    abstractionTimer.start();
  }

  public void stopAbstractionTimer() {
    abstractionTimer.stop();
  }

  public void startValueDeterminationTimer() {
    valueDeterminationTimer.start();
  }

  public void stopValueDeterminationTimer() {
    valueDeterminationTimer.stop();
  }

  public PolicyIterationStatistics(Configuration config,
      @Nullable FormulaSlicingManager pSlicing)
      throws InvalidConfigurationException {
    config.inject(this, PolicyIterationStatistics.class);
    slicing = pSlicing;
  }

  @Override
  public void printStatistics(
      PrintStream out, CPAcheckerResult.Result result, ReachedSet reached) {

    printTimer(out, valueDeterminationTimer, "value determination");
    printTimer(out, abstractionTimer, "abstraction");
    printTimer(out, optTimer, "optimization");
    printTimer(out, checkSATTimer, "checking satisfiability");
    if (slicing != null) {
      printTimer(out, slicing.getSlicingTime(), "checking inductiveness in formula slicing");
    }
    out.printf("Time spent in %s: %s (Max: %s)%n",
        "SMT solver",
        TimeSpan.sum(
            optTimer.getSumTime(),
            checkSATTimer.getSumTime()
        ).formatAs(TimeUnit.SECONDS),
        optTimer.getMaxTime().formatAs(TimeUnit.SECONDS));
  }

  public void printTimer(PrintStream out, Timer t, String name) {
    out.printf("Time spent in %s: %s (Max: %s)%n",
        name, t, t.getMaxTime().formatAs(TimeUnit.SECONDS));
  }

  @Override
  public String getName() {
    return "PolicyIterationCPA";
  }
}
