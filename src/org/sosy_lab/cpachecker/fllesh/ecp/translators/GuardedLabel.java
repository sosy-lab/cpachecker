package org.sosy_lab.cpachecker.fllesh.ecp.translators;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.sosy_lab.cpachecker.fllesh.ecp.ECPGuard;

public abstract class GuardedLabel implements Iterable<ECPGuard> {

  private Set<ECPGuard> mGuards = new HashSet<ECPGuard>();
  
  public GuardedLabel() {
    
  }
  
  public GuardedLabel(Set<ECPGuard> pGuards) {
    mGuards.addAll(pGuards);
  }
  
  public GuardedLabel(ECPGuard pGuard) {
    mGuards.add(pGuard);
  }
  
  /** copy constructor */
  public GuardedLabel(GuardedLabel pEdge) {
    this(pEdge.mGuards);
  }
  
  @Override
  public Iterator<ECPGuard> iterator() {
    return mGuards.iterator();
  }
  
  protected Set<ECPGuard> getGuards() {
    return mGuards;
  }

  public abstract <T> T accept(GuardedLabelVisitor<T> pVisitor);
  
}
