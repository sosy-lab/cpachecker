package org.sosy_lab.cpachecker.util.ecp.translators;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.sosy_lab.cpachecker.util.ecp.ECPGuard;

public abstract class GuardedLabel implements Iterable<ECPGuard> {

  private final Set<ECPGuard> mGuards;
  
  public GuardedLabel() {
    mGuards = Collections.emptySet();
  }
  
  public GuardedLabel(Set<ECPGuard> pGuards) {
    if (pGuards.size() == 0) {
      mGuards = Collections.emptySet();
    }
    else {
      mGuards = new HashSet<ECPGuard>(pGuards);
    }
  }
  
  public GuardedLabel(ECPGuard pGuard) {
    mGuards = new HashSet<ECPGuard>();
    mGuards.add(pGuard);
  }
  
  @Override
  public Iterator<ECPGuard> iterator() {
    return mGuards.iterator();
  }
  
  protected Set<ECPGuard> getGuards() {
    return mGuards;
  }
  
  public boolean hasGuards() {
    return !mGuards.isEmpty();
  }
  
  public int getNumberOfGuards() {
    return mGuards.size();
  }

  public abstract <T> T accept(GuardedLabelVisitor<T> pVisitor);
  
}
