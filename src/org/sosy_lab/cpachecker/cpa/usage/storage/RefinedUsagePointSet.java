package org.sosy_lab.cpachecker.cpa.usage.storage;


import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.UsageState;
import org.sosy_lab.cpachecker.util.Pair;

public class RefinedUsagePointSet implements AbstractUsagePointSet {

  public static class DoubleRefinedUsagePointSet extends RefinedUsagePointSet {
    protected final UsageInfo target2;

    private DoubleRefinedUsagePointSet(UsageInfo newSet, UsageInfo newSet2) {
      super(newSet);
      target2 = newSet2;
    }

    @Override
    public int size() {
      return 2;
    }

    @Override
    public Pair<UsageInfo, UsageInfo> getUnsafePair() {
      return Pair.of(target, target2);
    }

    @Override
    public UsageInfoSet getUsageInfo(UsagePoint point) {
      UsageInfoSet result = super.getUsageInfo(point);
      if (result != null) {
        return result;
      }
      UsagePoint p = UsagePoint.createUsagePoint(target2);
      if (p.equals(point)) {
        return new UsageInfoSet(target2);
      }
      return null;
    }
  }

  protected final UsageInfo target;

  private RefinedUsagePointSet(UsageInfo newSet) {
    target = newSet;
  }

  public static RefinedUsagePointSet create(UsageInfo newSet, UsageInfo newSet2) {
    //We may clone it, so just == can not help
    if (newSet.getPath().equals(newSet2.getPath()) && newSet.equals(newSet2)) {
      return new RefinedUsagePointSet(newSet);
    } else {
      return new DoubleRefinedUsagePointSet(newSet, newSet2);
    }
  }

  @Override
  public int size() {
    return 1;
  }

  @Override
  public UsageInfoSet getUsageInfo(UsagePoint point) {
    UsagePoint p = UsagePoint.createUsagePoint(target);
    if (p.equals(point)) {
      return new UsageInfoSet(target);
    }
    return null;
  }

  @Override
  public int getNumberOfTopUsagePoints() {
    return size();
  }

  public Pair<UsageInfo, UsageInfo> getUnsafePair() {
    return Pair.of(target, target);
  }

  @Override
  public void remove(UsageState pUstate) {
    // Do nothing, we don't delete true usages
  }

}
