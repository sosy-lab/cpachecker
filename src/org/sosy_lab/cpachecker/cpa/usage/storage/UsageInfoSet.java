package org.sosy_lab.cpachecker.cpa.usage.storage;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.UsageState;

public class UsageInfoSet {
  private final SortedSet<UsageInfo> unrefinedUsages;

  public UsageInfoSet() {
    unrefinedUsages = new TreeSet<>();
  }

  public UsageInfoSet(UsageInfo uinfo) {
    //Means, that the race is found and this particular usage is a part of the result;
    this();
    add(uinfo);
  }

  private UsageInfoSet(SortedSet<UsageInfo> usages) {
    unrefinedUsages = Sets.newTreeSet(usages);
  }

  public void add(UsageInfo newInfo) {
    unrefinedUsages.add(newInfo);
  }

  public int size() {
    return unrefinedUsages.size();
  }

  public boolean remove(UsageState pUstate) {
    Iterator<UsageInfo> iterator = unrefinedUsages.iterator();
    boolean changed = false;
    while (iterator.hasNext()) {
      UsageInfo uinfo = iterator.next();
      AbstractState keyState = uinfo.getKeyState();
      assert (keyState != null);
      if (UsageState.get(keyState).equals(pUstate)) {
        iterator.remove();
        changed = true;
      }
    }
    return changed;
  }

  public boolean remove(UsageInfo uinfo) {
    return unrefinedUsages.remove(uinfo);
  }

  public UsageInfo getOneExample() {
    return Iterables.get(unrefinedUsages, 0);
  }

  public Set<UsageInfo> getUsages() {
    return unrefinedUsages;
  }

  @Override
  public UsageInfoSet clone() {
    //For avoiding concurrent modification in refinement
    return new UsageInfoSet(this.unrefinedUsages);
  }
}
