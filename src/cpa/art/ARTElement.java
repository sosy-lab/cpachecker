package cpa.art;

import java.util.ArrayList;
import java.util.List;

import cfa.objectmodel.CFANode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElementWithLocation;

public class ARTElement implements AbstractElementWithLocation {

  private AbstractElementWithLocation element;
  private ARTElement parentElement;
  private List<ARTElement> children;

  private int elementId;
  private static int nextArtElementId= 0;

  private AbstractDomain domain;
  private boolean covered;
  private int mark;

  public ARTElement(AbstractDomain pDomain, AbstractElementWithLocation pAbstractElement, ARTElement pParentElement) {
    domain = pDomain;
    element = pAbstractElement;
    setParent(pParentElement);
    children = new ArrayList<ARTElement>();
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
  
  public List<ARTElement> getChildren(){
    return children;
  }
  
  public int numberOfChildren(){
    return children.size();
  }

  public AbstractElementWithLocation getAbstractElementOnArtNode(){
    return element;
  }

  public void addToChildrenList(ARTElement child){
    children.add(child);
  }

  public boolean isMarked() { 
    return mark > 0; 
  }

  public void setMark() { 
    mark = nextArtElementId++; 
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

  @Override
  public boolean equals(Object pObj) {
    return ((ARTElement)pObj).elementId == this.elementId;
  }

  @Override
  public int hashCode() {
    return this.elementId;
  }

  @Override
  public String toString() {
    String s = "";
    s = s + "ART Element Id: " + elementId + ", ";
    if(parentElement != null){
      s = s + "Parent Element's Id: " + getParent().elementId + "\n";
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
}
