package cpa.art;

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
    int i = path.size();
    while(currentEdge != null){
      s = s + currentEdge + "\n";
      i--;
      currentEdge = path.get(i).getSecond(); 
    }
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
  
}
