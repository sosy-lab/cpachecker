package cpa.art;

import java.util.ArrayList;
import java.util.List;

import cfa.objectmodel.CFANode;
import cpa.common.interfaces.AbstractElementWithLocation;

public class ARTElement implements AbstractElementWithLocation {

  private AbstractElementWithLocation element;
  private ARTElement parentElement;
  private int elementId;
  private static int nextArtElementId= 0;

  public ARTElement(AbstractElementWithLocation pAbstractElement, ARTElement pParentElement) {
    element = pAbstractElement;
    parentElement = pParentElement;
    elementId = ++nextArtElementId;
  }

  public ARTElement getParent(){
    return parentElement;
  }

  public AbstractElementWithLocation getAbstractElementOnArtNode(){
    return element;
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
    s = s + "ART Element Id: " + elementId + "\n";
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
