package cpa.common;

import java.util.Iterator;
import java.util.Vector;

import cfa.objectmodel.CFAEdge;

import common.Pair;

import cpa.common.interfaces.AbstractElement;

public class Path implements Iterable<Pair<AbstractElement, CFAEdge>>{

  private Vector<Pair<AbstractElement, CFAEdge>> path;
  
  public Path() {
    path = new Vector<Pair<AbstractElement,CFAEdge>>();
  }
  
  public void addToPathAsFirstElem(AbstractElement abstractElement, CFAEdge incomingEdge){
    path.add(0, new Pair<AbstractElement, CFAEdge>(abstractElement, incomingEdge));
  }
  
  @Override
  public String toString() {
    String s = "";

    CFAEdge currentEdge = path.lastElement().getSecond();
    int i = path.size()-1;
    do{
      s = s + currentEdge + "\n";
      i--;
      currentEdge = path.get(i).getSecond(); 
    } while(currentEdge != null);
    return s;
  }

  public int size() {
    return path.size();
  }
  
  public Pair<AbstractElement, CFAEdge> getElementAt(int i){
    return path.get(i);
  }

  @Override
  public Iterator<Pair<AbstractElement, CFAEdge>> iterator() {
    return path.iterator();
  }
  
  public Pair<AbstractElement, CFAEdge> firstElement(){
    return path.firstElement();
  }

  public Pair<AbstractElement, CFAEdge> lastElement() {
    return path.lastElement();
  }
}
