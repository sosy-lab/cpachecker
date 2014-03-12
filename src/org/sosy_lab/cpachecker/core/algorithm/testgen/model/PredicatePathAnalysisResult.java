package org.sosy_lab.cpachecker.core.algorithm.testgen.model;

import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

/**
 * represents the result of a path modification.
 * Consists of a solver result, as well as the deciding node (the last node that both paths have in common)
 * and the 'wrong' node (the successor of the deciding node that is no longer in the path)
 */
public class PredicatePathAnalysisResult {

  public static final PredicatePathAnalysisResult INVALID = new PredicatePathAnalysisResult(null, null, null,null);

  public PredicatePathAnalysisResult(CounterexampleTraceInfo pTrace, ARGState pDecidingState,
      ARGState pWrongState, ARGPath pArgPath) {
    super();
    trace = pTrace;
    decidingState = pDecidingState;
    wrongState = pWrongState;
    this.argPath = pArgPath;

  }

  private CounterexampleTraceInfo trace;
  private ARGState decidingState;
  private ARGState wrongState;
  private ARGPath argPath;

  public CounterexampleTraceInfo getTrace() {
    checkValid();
    return trace;
  }

  public ARGState getDecidingState() {
    checkValid();
    return decidingState;
  }

  public ARGState getWrongState() {
    checkValid();
    return wrongState;
  }

  public ARGPath getPath() {
    checkValid();
    return argPath;
  }

  /**
   *
   * @return
   */
  public boolean isValid() {
    return !isEmpty();
  }

  /**
   * checks if the result contains any data. Especially returns true for {@link PredicatePathAnalysisResult#INVALID}
   * @return
   */
  public boolean isEmpty() {
    return trace == null;
  }

  private void checkValid() {
    if (!isValid()) { throw new IllegalStateException(
        "this is not a valid result. It is not allowed to access data of an invalid result");
    }
  }
}

