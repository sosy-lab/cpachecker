package cpa.art;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import cfa.objectmodel.CFANode;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.AbstractWrapperElement;

public class ARTElement implements AbstractElementWithLocation, AbstractWrapperElement, Comparable<ARTElement>{

  public static long artElementEqualsTime = 0; 

  private final ARTCPA mCpa;
  private final AbstractElementWithLocation element;
  private final Set<ARTElement> children;
  private final Set<ARTElement> parents; // more than one parent if joining elements
  private ARTElement mCoveredBy = null;
  private boolean isBottom = false;
  private boolean destroyed = false;

  private final int elementId;

  private static int nextArtElementId = 0;

  protected ARTElement(ARTCPA pCpa, AbstractElementWithLocation pAbstractElement, ARTElement pParentElement) {
    mCpa = pCpa;
    element = pAbstractElement;
    parents = new HashSet<ARTElement>();
    if(pParentElement != null){
      addParent(pParentElement);
    }
    children = new HashSet<ARTElement>();
    elementId = ++nextArtElementId;
  }

  public Set<ARTElement> getParents(){
    assert !destroyed;
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

  public AbstractElementWithLocation getAbstractElementOnArtNode(){
    assert !destroyed;
    return element;
  }

  protected void setCovered(ARTElement pCoveredBy) {
    assert pCoveredBy != null;
    mCoveredBy = pCoveredBy;
    mCpa.setCovered(this, true);
  }

  protected void setUncovered() {
    assert !destroyed;
    mCoveredBy = null;
    mCpa.setCovered(this, false);
  }

  public boolean isCovered() {
    assert !destroyed;
    return mCpa.isCovered(this);
  }

  public ARTElement getCoveredBy() {
    assert !destroyed;
    return mCoveredBy;
  }

  public boolean isBottom() {
    assert !destroyed;
    return isBottom;
  }

  protected void setBottom(boolean pIsBottom) {
    assert !destroyed;
    isBottom = pIsBottom;
  }

  public ARTCPA getCpa() {
    return mCpa;
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
    sb.append(element);
    return sb.toString();
  }

  @Override
  public CFANode getLocationNode() {
    return element.getLocationNode();
  }

  @Override
  public AbstractElement retrieveElementOfType(String pElementClass) {
    assert !destroyed;
    if(element.getClass().getSimpleName().equals(pElementClass)){
      return element;
    }
    else{
      return ((AbstractWrapperElement)element).retrieveElementOfType(pElementClass);
    }
  }
  
  @Override
  public Iterable<AbstractElement> getWrappedElements() {
    assert !destroyed;
    return Collections.singletonList((AbstractElement) element);
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
   * This method removes all children of this element from the ART.
   * 
   * Note that it does not remove any elements from the covered set, this has to
   * be done by the caller.
   */
  /*protected void clearChildren() {
    for (ARTElement child : children) {
      assert (child.parents.contains(this));
      child.parents.remove(this);
    }
    children.clear();
  }*/


  /**
   * This method removes this element from the ART by removing it from its
   * parents' children list and from its children's parents list.
   * 
   * This method also removes the element from the set of covered elements.
   * 
   * This means, if its children do not have any other parents, they will be not
   * reachable any more, i.e. they do not belong to the ART any more. But those
   * elements will not be removed from the covered set.
   */
  protected void removeFromART() {
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

    setUncovered();
    destroyed = true;
  }

  public int getElementId() {
    return elementId;
  }

  @Override
  public boolean isError() {
    return element.isError();
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

  @Override
  public int compareTo(ARTElement pO) {

    int otherElementid = pO.getElementId();

    if(this.getElementId() > otherElementid){
      return 1;
    }
    else if(getElementId() < otherElementid){
      return -1;
    }
    return 0;
  }
  
}
