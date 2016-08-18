package org.sosy_lab.cpachecker.core.algorithm.tgar.interfaces;

import org.sosy_lab.cpachecker.core.algorithm.mpa.TargetSummary;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public interface TestificationOperator {

  TargetSummary testify(ReachedSet pCounterexample, ARGState pForProperties)
      throws InterruptedException;
}
