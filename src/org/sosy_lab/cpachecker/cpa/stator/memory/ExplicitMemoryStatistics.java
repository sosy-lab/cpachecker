package org.sosy_lab.cpachecker.cpa.stator.memory;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

public class ExplicitMemoryStatistics implements Statistics {
  Timer memoryAnalysisTimer = new Timer();

  @Override
  public void printStatistics(PrintStream out, CPAcheckerResult.Result result,
      ReachedSet reached) {

    out.printf("Time spent in memory analysis: %s (Max: %s)\n",
        memoryAnalysisTimer, memoryAnalysisTimer.getMaxTime().formatAs(
            TimeUnit.SECONDS));
  }

  @Nullable
  @Override
  public String getName() {
    return "ExplicitMemoryCPA";
  }
}
