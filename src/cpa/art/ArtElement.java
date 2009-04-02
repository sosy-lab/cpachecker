package cpa.art;

import cpa.common.interfaces.AbstractElement;

public class ArtElement implements AbstractElement {

  private AbstractElement element;
  private ArtElement parentElement;
  private int elementId;
  private static int nextArtElementId= 0;
  
  public ArtElement(AbstractElement pAbstractElement, ArtElement pParentElement) {
    element = pAbstractElement;
    parentElement = pParentElement;
    elementId = ++nextArtElementId;
  }
  
  public ArtElement getParent(){
    return parentElement;
  }
  
  public AbstractElement getAbstractElementOnArtNode(){
    return element;
  }
  
  @Override
  public boolean equals(Object pObj) {
    return ((ArtElement)pObj).elementId == this.elementId;
  }
  
  @Override
  public int hashCode() {
    return this.elementId;
  }
  
  @Override
  public String toString() {
    String s = "";
    s = s + "ART Element Id: " + elementId + "\n";
    s = s + "Parent Element's Id: " + getParent().elementId;
    return s;
  }
  
}
