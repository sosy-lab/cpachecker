package rfsGenerator;

import java.util.ArrayDeque;
import java.util.Deque;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.StatementEdge;

public class RFSTraversal {
  
  public RFSTraversal() {
    // TODO Auto-generated constructor stub
  }

  public void traverse(CFANode initialNode) {

    Deque<CFANode> workList = new ArrayDeque<CFANode> ();
    Deque<CFANode> processedList = new ArrayDeque<CFANode> ();
    
    workList.addLast (initialNode);
    processedList.addLast (initialNode);
    
    while (!workList.isEmpty ())
    {
      CFANode node = workList.pollFirst ();
      int numLeavingEdges = node.getNumLeavingEdges ();

      for (int edgeIdx = 0; edgeIdx < numLeavingEdges; edgeIdx++)
      {
        CFAEdge edge = node.getLeavingEdge (edgeIdx);
        CFANode successorNode = edge.getSuccessor();
        processEdge(edge);
        
        if(!processedList.contains(successorNode)){
           workList.add(successorNode);
         }
      }
      processedList.add(node);
    }
    
  }

  private void processEdge(CFAEdge edge) {
    
    switch (edge.getEdgeType ())
    {
    case StatementEdge:
    {
      break;
    }
    case FunctionCallEdge:
    {
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) edge;
      System.out.println(functionCallEdge.getSuccessor().getLineNumber());
      System.out.println(functionCallEdge);
      break;
    }
    }
    
  }
}
