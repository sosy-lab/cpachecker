package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

/**
 * Statistics for formula slicing.
 */
public class FormulaSlicingStatistics implements Statistics {
  final Timer formulaSlicingTimer = new Timer();

  @Override
  public void printStatistics(PrintStream out, Result result,
      ReachedSet reached) {
    printTimer(out, formulaSlicingTimer, "formula slicing");
  }

  private void printTimer(PrintStream out, Timer t, String name) {
    // todo: code duplication with PolicyIterationStatistics.
    out.printf("Time spent in %s: %s (Max: %s), (Avg: %s)%n",
        name, t, t.getMaxTime().formatAs(TimeUnit.SECONDS),
        t.getAvgTime().formatAs(TimeUnit.SECONDS));
  }

  @Nullable
  @Override
  public String getName() {
    return "FormulaSlicingCPA";
  }
}
