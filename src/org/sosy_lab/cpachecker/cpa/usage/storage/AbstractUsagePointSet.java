package org.sosy_lab.cpachecker.cpa.usage.storage;

import org.sosy_lab.cpachecker.cpa.usage.UsageState;

public interface AbstractUsagePointSet {

  public abstract int size();
  public abstract UsageInfoSet getUsageInfo(UsagePoint point);
  public abstract int getNumberOfTopUsagePoints();
  public abstract void remove(UsageState pUstate);
}
