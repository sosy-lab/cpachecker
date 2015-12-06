package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context;

import java.util.ArrayList;
import java.util.List;

public class CThreadContainer extends AThreadContainer {

  public CThreadContainer(List<CThread> threads) {
    super(threads);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<CThread> getAllThreads() {
    return new ArrayList<CThread>((List<CThread>) super.getAllThreads());
  }

  @Override
  public CThread getMainThread() {
    return (CThread) super.getMainThread();
  }


}
