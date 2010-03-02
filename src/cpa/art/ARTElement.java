package cpa.art;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;

import com.google.common.base.Preconditions;

import cpa.common.defaults.AbstractSingleWrapperElement;
import cpa.common.interfaces.AbstractElement;

public class ARTElement extends AbstractSingleWrapperElement {

  private final Set<ARTElement> children;
  private final Set<ARTElement> parents; // more than one parent if joining elements
  private ARTElement mCoveredBy = null;
  private Set<ARTElement> mCoveredByThis = null; // lazy initialization because rarely needed
  private boolean destroyed = false;
  private ARTElement mergedWith = null;

  private final int elementId;

  private static int nextArtElementId = 0;

  protected ARTElement(AbstractElement pWrappedElement, ARTElement pParentElement) {
    super(pWrappedElement);
    elementId = ++nextArtElementId;
    parents = new HashSet<ARTElement>();
    if(pParentElement != null){
      addParent(pParentElement);
    }
    children = new HashSet<ARTElement>();
  }

  public Set<ARTElement> getParents(){
    return parents;
  }

  protected void addParent(ARTElement pOtherParent){
    assert !destroyed;
    if(parents.add(pOtherParent)){
      pOtherParent.children.add(this);
    }
  }

  public Set<ARTElement> getChildren(){
    assert !destroyed;
    return children;
  }

  protected void setCovered(ARTElement pCoveredBy) {
    assert pCoveredBy != null;
    mCoveredBy = pCoveredBy;
    if (pCoveredBy.mCoveredByThis == null) {
      // lazy initialization because rarely needed
      pCoveredBy.mCoveredByThis = new HashSet<ARTElement>(2);
    }
    pCoveredBy.mCoveredByThis.add(this);
  }

  public boolean isCovered() {
    assert !destroyed;
    return mCoveredBy != null;
  }

  public Set<ARTElement> getCoveredByThis() {
    assert !destroyed;
    if (mCoveredByThis == null) {
      return Collections.emptySet();
    } else {
      return Collections.unmodifiableSet(mCoveredByThis);
    }
  }

  protected void setMergedWith(ARTElement pMergedWith) {
    assert !destroyed;
    assert mergedWith == null;
    
    mergedWith = pMergedWith;
  }
  
  public ARTElement getMergedWith() {
    return mergedWith;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    if (destroyed) {
      sb.append("Destroyed ");
    }
    sb.append("ART Element (Id: ");
    sb.append(elementId);
    if (!destroyed) {
      sb.append(", Parents: ");
      List<Integer> list = new ArrayList<Integer>();
      for (ARTElement e: parents) {
        list.add(e.elementId);
      }
      sb.append(list);
      sb.append(", Children: ");
      list.clear();
      for (ARTElement e: children) {
        list.add(e.elementId);
      }
      sb.append(list);
    }
    sb.append(") ");
    sb.append(getWrappedElement());
    return sb.toString();
  }

  // TODO check
  public Set<ARTElement> getSubtree() {
    assert !destroyed;
    Set<ARTElement> result = new HashSet<ARTElement>();
    Deque<ARTElement> workList = new ArrayDeque<ARTElement>();

    workList.add(this);

    while (!workList.isEmpty()) {
      ARTElement currentElement = workList.removeFirst();
      if (result.add(currentElement)) {
        // currentElement was not in result
        workList.addAll(currentElement.children);
      }
    }
    return result;
  }

  /**
   * This method removes this element from the ART by removing it from its
   * parents' children list and from its children's parents list.
   * 
   * This method also removes the element from the covered set of the other
   * element covering this element, if it is covered.
   * 
   * This means, if its children do not have any other parents, they will be not
   * reachable any more, i.e. they do not belong to the ART any more. But those
   * elements will not be removed from the covered set.
   */
  public void removeFromART() {
    assert !destroyed;
    
    // clear children
    for (ARTElement child : children) {
      assert (child.parents.contains(this));
      child.parents.remove(this);
    }
    children.clear();

    // clear parents
    for (ARTElement parent : parents) {
      assert (parent.children.contains(this));
      parent.children.remove(this);
    }
    parents.clear();

    // clear coverage relation
    if (isCovered()) {
      assert mCoveredBy.mCoveredByThis.contains(this);
    
      mCoveredBy.mCoveredByThis.remove(this);
      mCoveredBy = null;
    }
    
    if (mCoveredByThis != null) {
      for (ARTElement covered : mCoveredByThis) {
        covered.mCoveredBy = null;
      }
      mCoveredByThis.clear();
    }
    
    destroyed = true;
  }

  public int getElementId() {
    return elementId;
  }

  /**
   * This method returns a random element from the list of parents.
   * @return A parent of this element.
   */
  public ARTElement getFirstParent() {
    if (parents.isEmpty()) {
      return null;
    }
    Iterator<ARTElement> it = parents.iterator();
    return it.next();
  }

  public CFAEdge getEdgeToChild(ARTElement pChild) {
    Preconditions.checkArgument(children.contains(pChild));
    
    CFANode currentLoc = this.retrieveLocationElement().getLocationNode();
    CFANode childNode = pChild.retrieveLocationElement().getLocationNode();

    for (int i = 0; i < childNode.getNumEnteringEdges(); i++) {
      CFAEdge edge = childNode.getEnteringEdge(i);
      if (currentLoc.getNodeNumber() == edge.getPredecessor().getNodeNumber()) {
        return edge;
      }
    }
    throw new IllegalStateException("Invalid ART, parent<->child relation without corresponding CFAEdge");
  }
}
