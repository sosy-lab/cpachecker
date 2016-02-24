package org.sosy_lab.cpachecker.cpa.formulaslicing;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class InductiveWeakeningStatistics implements Statistics {

  // TODO: but we want to also measure #iterations for a single weakening
  final AtomicInteger noCexIterations = new AtomicInteger();

  final Timer annotationTime = new Timer();
  final Timer destructiveWeakeningTime = new Timer();
  final Timer cexWeakeningTime = new Timer();
  final Timer quantifierEliminationTime = new Timer();

  @Override
  public void printStatistics(
      PrintStream out, Result result, ReachedSet reached) {

    printTimer(out, annotationTime, "Annotation");
    printTimer(out, destructiveWeakeningTime, "Destructive Weakening");
    printTimer(out, cexWeakeningTime, "Counterexample-based Weakening");
    printTimer(out, quantifierEliminationTime, "Light QE");
    out.printf("# of CEX iterations: %d\n", noCexIterations.get());
  }

  @Override
  public String getName() {
    return "Inductive Weakening";
  }

  private void printTimer(PrintStream out, Timer t, String name) {
    out.printf("Time spent in %s: %s (Max: %s), (Avg: %s), (#intervals = %s)%n",
        name, t, t.getMaxTime().formatAs(TimeUnit.SECONDS),
        t.getAvgTime().formatAs(TimeUnit.SECONDS),
        t.getNumberOfIntervals());
  }
}
