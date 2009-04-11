/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package programtesting.simple;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import programtesting.simple.QDPTCompositeCPA.CFAEdgeEdge;
import programtesting.simple.QDPTCompositeCPA.QDPTCompositeElement;

/**
 *
 * @author holzera
 */
public class BasicBlock {
  private LinkedList<Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge> mEdges;
  private QDPTCompositeElement mFirstElement;
  private QDPTCompositeElement mLastElement;
  
  private int mId;
  
  public BasicBlock(QDPTCompositeElement pFirstElement) {
    assert(pFirstElement != null);
    
    mEdges = new LinkedList<Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge>();
    
    mFirstElement = pFirstElement;
    mLastElement = pFirstElement;
  }
  
  public int getId() {
    return mId;
  }
  
  public void setId(int pId) {
    mId = pId;
  }
  
  public QDPTCompositeElement getFirstElement() {
    return mFirstElement;
  }
  
  public QDPTCompositeElement getLastElement() {
    return mLastElement;
  }
  
  public List<Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge> getEdges() {
    return mEdges;
  }
  
  public void addEdge(Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge pEdge) {
    assert(pEdge != null);
    
    mEdges.addLast(pEdge);
    
    mLastElement = pEdge.getTarget();
  }

  public boolean isEmpty() {
    return mEdges.isEmpty();
  }

  @Override
  public String toString() {
    StringWriter lResult = new StringWriter();

    PrintWriter lPrintWriter = new PrintWriter(lResult);

    lPrintWriter.print("BB_" + getId() + ":\\n");

    for (Graph<QDPTCompositeElement, CFAEdgeEdge>.Edge lEdge : mEdges) {
      lPrintWriter.print(lEdge.getAnnotation() + "\\n");
    }

    return lResult.toString();
  }
}
