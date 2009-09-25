package cpa.art;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cfa.objectmodel.CFANode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.AbstractWrapperElement;

public class ARTElement implements AbstractElementWithLocation, AbstractWrapperElement{

  private AbstractElementWithLocation element;
  private ARTElement parentElement;
  private Set<ARTElement> children;
public static long artElementEqualsTime = 0; 
  // second parent is used for joining elements
  private ARTElement secondParent = null;
  
  private int elementId;
  private static int nextArtElementId= 0;

  private AbstractDomain domain;
  private boolean covered;
  private int mark;

  public ARTElement(AbstractDomain pDomain, AbstractElementWithLocation pAbstractElement, ARTElement pParentElement) {
    domain = pDomain;
    element = pAbstractElement;
    setParent(pParentElement);
    children = new HashSet<ARTElement>();
    elementId = ++nextArtElementId;
    mark = 0;
    covered = false;
  }

  private void setParent(ARTElement pParentElement) {
    if(pParentElement == null) return;
    parentElement = pParentElement;
    parentElement.addToChildrenList(this);
  }

  public ARTElement getParent(){
    return parentElement;
  }
  
  public ARTElement getSecondParent(){
    return secondParent;
  }
  
  public void addSecondParent(ARTElement pSecondParent){
    if(pSecondParent == parentElement){
      return;
    }
    secondParent = pSecondParent;
    secondParent.addToChildrenList(this);
  }
  
  public Set<ARTElement> getChildren(){
    return children;
  }
  
  public int numberOfChildren(){
    return children.size();
  }

  public AbstractElementWithLocation getAbstractElementOnArtNode(){
    return element;
  }

  private void addToChildrenList(ARTElement child){
    children.add(child);
  }

  public boolean isMarked() { 
    return mark > 0; 
  }

  public void setMark() { 
    mark = nextArtElementId++; 
  }
  
  public void setMark(int pMark) { 
    mark = pMark; 
  }

  public int getMark() { 
    return mark; 
  }

  public boolean isCovered() { 
    return covered; 
  }

  public void setCovered(boolean yes) { 
    covered = yes; setMark(); 
  }
  
  public AbstractDomain getDomain(){
    return domain;
  }

//  @Override
//  public boolean equals(Object pObj) {
//    long start = System.currentTimeMillis();
//    boolean b = ((ARTElement)pObj).getAbstractElementOnArtNode().equals(getAbstractElementOnArtNode());
//    long end = System.currentTimeMillis();
//    artElementEqualsTime = artElementEqualsTime + (end - start);
//    return b;
//  }

  @Override
  public int hashCode() {
    return this.elementId;
  }

  @Override
  public String toString() {
    String s = "";
    s = s + "ART Element Id: " + elementId + " : Mark: "+ mark +", ";
    if(parentElement != null){
      s = s + "Parent Element's Id: " + getParent().elementId + "\n";
      if(secondParent != null){
        s = s + "2nd Parent's Id: " + secondParent.elementId + "\n";
      }
    }
    else{
      s = s + "parent is null" + "\n";
    }
    s = s + element;
    return s;
  }

  @Override
  public CFANode getLocationNode() {
    return element.getLocationNode();
  }

  public List<CFANode> getPath(){
    List<CFANode> path = new ArrayList<CFANode>();
    CFANode firstNode = element.getLocationNode();
    path.add(firstNode);
    ARTElement parent = parentElement;
    CFANode nextNode = null;
    while(parent != null){
      nextNode = parent.getLocationNode();
      path.add(nextNode);
      parent = parent.getParent();
    }
    return path;
  }

  public String pathToString(){
    String s = "";
    List<CFANode> path = getPath();
    for(CFANode node:path){
      s = s + node.toString() + "\n";
    }
    return s;
  }

  @Override
  public AbstractElement retrieveElementOfType(String pElementClass) {
    if(element.getClass().getSimpleName().equals(pElementClass)){
      return element;
    }
    else{
      return ((AbstractWrapperElement)element).retrieveElementOfType(pElementClass);
    }
  }

  // TODO check
  public Collection<ARTElement> getSubtree() {
      Set<ARTElement> ret = new HashSet<ARTElement>();
      List<ARTElement> workList = new ArrayList<ARTElement>();
      
      workList.add(this);

      while(workList.size() > 0){
        ARTElement currentElement = workList.remove(0);
        if(ret.contains(currentElement)){
          continue;
        }
        ret.add(currentElement);
        Set<ARTElement> childrenOfCurrentElement = currentElement.getChildren();
        workList.addAll(childrenOfCurrentElement);
      }
      return ret;
  }

  public void clearChildren() {
    children.clear();
  }
}
