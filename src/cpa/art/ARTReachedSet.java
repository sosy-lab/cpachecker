package cpa.art;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import cpa.common.ReachedElements;
import cpa.common.interfaces.Precision;

/**
 * This class is a modifiable live view of a reached set, which shows the ART
 * relations between the elements, and enforces a correct ART when the set is
 * modified through this wrapper.
 */
public class ARTReachedSet {

  private final ReachedElements mReached;
  private final ARTCPA mCpa;

  public ARTReachedSet(ReachedElements pReached, ARTCPA pCpa) {
    mReached = pReached;
    mCpa = pCpa;
  }
  
  public ARTElement getFirstElement() {
    return (ARTElement)mReached.getFirstElement();
  }
  
  public ARTElement getLastElement() {
    return (ARTElement)mReached.getLastElement();
  }
  
  public Precision getPrecision(ARTElement e) {
    return mReached.getPrecision(e);
  }
  
  /**
   * Remove an element and all elements below it from the tree. Re-add all those
   * elements to the waitlist which have children which are either removed or were
   * covered by removed elements.
   *  
   * @param e The root of the removed subtree, may not be the initial element.
   */
  public void removeSubtree(ARTElement e) {
    Set<ARTElement> toWaitlist = removeSubtree0(e);
    
    for (ARTElement ae : toWaitlist) {
      mReached.reAddToWaitlist(ae);
    }
  }
  
  /**
   * Like {@link #removeSubtree(ARTElement)}, but when re-adding elements to the
   * waitlist use the supplied precision p, no matter what precision those
   * elements previously had.
   * @param e The root of the removed subtree, may not be the initial element.
   * @param p The new precision.
   */
  public void removeSubtree(ARTElement e, Precision p) {
    Set<ARTElement> toWaitlist = removeSubtree0(e);
    
    for (ARTElement ae : toWaitlist) {
      mReached.add(ae, p);
    }
  }

  private Set<ARTElement> removeSubtree0(ARTElement e) {
    Preconditions.checkNotNull(e);
    Preconditions.checkArgument(!e.getParents().isEmpty(), "May not remove the initial element from the ART/reached set");
    
    // collect all elements to remove from ART (the subtree and the elements
    // covered by the subtree)
    Set<ARTElement> toUnreach = e.getSubtree();

    for (ARTElement ae : mCpa.getCovered()) {
      if (toUnreach.contains(ae.getCoveredBy())) {
        toUnreach.add(ae);
      }
    }

    // collect all elements to re-add to the waitlist (the parents of the
    // removed elements, if they are not removed themselves)
    Set<ARTElement> toWaitlist = new HashSet<ARTElement>();
    for (ARTElement ae : toUnreach) {

      for (ARTElement parent : ae.getParents()) {
        if (!toUnreach.contains(parent)) {
          toWaitlist.add(parent);
        }
      }
      
      ae.removeFromART();
    }

    mReached.removeAll(toUnreach);
    return toWaitlist;
  }
  
  /**
   * Remove an element and all elements below it from the tree, which are not
   * reachable via other paths through the ART (which in fact is a DAG, so there
   * may be such paths). Re-add all those elements to the waitlist which have
   * children which were covered by removed elements.
   * 
   * The effect on the reached set is the same as if the transfer relation had
   * returned bottom (e.g. no successor) instead of this element. The only
   * exception is that the elements on such remaining paths may still be weaker
   * than if they had not been merged with the removed elements.
   *  
   * @param e The root of the removed subtree, may not be the initial element.
   */
  public void replaceWithBottom(ARTElement e) {
    Preconditions.checkNotNull(e);
    Preconditions.checkArgument(!e.getParents().isEmpty(), "May not remove the initial element from the ART/reached set");
    
    Set<ARTElement> removedElements = new HashSet<ARTElement>();
    Deque<ARTElement> workList = new ArrayDeque<ARTElement>();

    workList.addAll(e.getChildren());
    removedElements.add(e);
    e.removeFromART();
    
    while (!workList.isEmpty()) {
      ARTElement currentElement = workList.removeFirst();
      if (currentElement.getParents().isEmpty()) {
        // no other paths to this element
        
        if (removedElements.add(currentElement)) {
          // not yet handled
          workList.addAll(currentElement.getChildren());
          currentElement.removeFromART();
        }
      }
    }
    
    mReached.removeAll(removedElements);
    
    removeCoverage(removedElements);
  }
  
  /**
   * Assume that some elements do not cover any elements anymore.
   * This re-adds the parents of elements previously covered to the waitlist.
   * 
   * You do not need to call this method if you removed elements through the
   * other methods provided by this class, they already ensure the same effect.
   * 
   * Be careful when using this method! It's only necessary when either the
   * reached set is modified in some way that the ARTCPA doesn't notice, 
   * or elements in the reached set were made stronger after they were added,
   * but elements should never be modified normally.
   * 
   * @param elements The elements which do not cover anymore.
   */
  public void removeCoverage(Collection<ARTElement> elements) {
    for (ARTElement ae : ImmutableSet.copyOf(mCpa.getCovered())) {
      if (elements.contains(ae.getCoveredBy())) {
        
        for (ARTElement parent : ae.getParents()) {
          mReached.reAddToWaitlist(parent);
        }
        ae.removeFromART();
      }
    }
  }
}
