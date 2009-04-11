/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package programtesting.simple;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author holzera
 */
public class Graph<NodeType, EdgeAnnotationType> {
  public class Edge {
    private NodeType mSource;
    private NodeType mTarget;
    private EdgeAnnotationType mAnnotation;
    
    public Edge(NodeType pSource, NodeType pTarget, EdgeAnnotationType pAnnotation) {
      assert(pSource != null);
      assert(pTarget != null);
      
      mSource = pSource;
      mTarget = pTarget;
      mAnnotation = pAnnotation;
    }
    
    public NodeType getSource() {
      return mSource;
    }
    
    public NodeType getTarget() {
      return mTarget;
    }
    
    public EdgeAnnotationType getAnnotation() {
      return mAnnotation;
    }

    @Override
    public String toString() {
      return getAnnotation().toString();
    }
  }
        
  private Map<NodeType, Set<Edge>> mOutgoingEdges;
  private Map<NodeType, Set<Edge>> mIncomingEdges;
  
  public Graph() {
    mOutgoingEdges = new HashMap<NodeType, Set<Edge>>();
    mIncomingEdges = new HashMap<NodeType, Set<Edge>>();
  }
  
  public void addNode(NodeType pNode) {
    assert(pNode != null);
    assert(!contains(pNode));
    
    Set<Edge> lOutgoingEdges = new HashSet<Edge>();
    
    mOutgoingEdges.put(pNode, lOutgoingEdges);
    
    Set<Edge> lIngoingEdges = new HashSet<Edge>();
    
    mIncomingEdges.put(pNode, lIngoingEdges);
  }
  
  public boolean addEdge(NodeType pSourceNode, NodeType pTargetNode, EdgeAnnotationType pEdgeAnnotation) {
    assert(pSourceNode != null);
    assert(pTargetNode != null);
    
    if (!contains(pSourceNode)) {
      addNode(pSourceNode);
    }
    
    if (!contains(pTargetNode)) {
      addNode(pTargetNode);
    }
    
    Set<Edge> lOutgoingEdges = getOutgoingEdges(pSourceNode);
    Set<Edge> lIncomingEdges = getIncomingEdges(pTargetNode);

    Edge lEdge = new Edge(pSourceNode, pTargetNode, pEdgeAnnotation);
    
    boolean lChanged1 = lOutgoingEdges.add(lEdge);
    boolean lChanged2 = lIncomingEdges.add(lEdge);
    
    assert(lChanged1 == lChanged2);
    
    return lChanged1;
  }
  
  public Set<NodeType> getNodes() {
    return mOutgoingEdges.keySet();
  }

  public Set<Edge> getEdges() {
    Set<Edge> lAllEdges = new HashSet<Edge>();

    for (Set<Edge> lEdges : mOutgoingEdges.values()) {
      lAllEdges.addAll(lEdges);
    }

    return lAllEdges;
  }
  
  public Set<Edge> getOutgoingEdges(NodeType pNode) {
    assert(pNode != null);
    assert(contains(pNode));
    
    return mOutgoingEdges.get(pNode);
  }
  
  public Set<Edge> getIncomingEdges(NodeType pNode) {
    assert(pNode != null);
    assert(contains(pNode));
    
    return mIncomingEdges.get(pNode);
  }
  
  public boolean contains(NodeType pNode) {
    assert(pNode != null);
    
    return mOutgoingEdges.containsKey(pNode);
  }
  
  public int getNumberOfNodes() {
    return mOutgoingEdges.size();
  }
  
  public int size() {
    int lSize = getNumberOfNodes();
    
    for (Entry<NodeType, Set<Edge>> lEntry : mOutgoingEdges.entrySet()) {
      lSize += lEntry.getValue().size();
    }
    
    return lSize;
  }
  
  public boolean isEmpty() {
    return mOutgoingEdges.isEmpty();
  }

  public String toDot() {
    StringWriter lResult = new StringWriter();

    PrintWriter lPrintWriter = new PrintWriter(lResult);


    lPrintWriter.println("digraph MyGraph {");


    Map<NodeType, Integer> lIds = new HashMap<NodeType, Integer>();

    int lIndex = 0;

    // print nodes
    for (NodeType lNode : getNodes()) {
      lPrintWriter.println("  node [label = \"" + lNode + "\", shape=box]; " + lIndex + ";");

      lIds.put(lNode, lIndex);

      lIndex++;
    }

    // print edges
    for (Set<Edge> lEdges : mOutgoingEdges.values()) {
      for (Edge lEdge : lEdges) {
        lPrintWriter.println("  " + lIds.get(lEdge.getSource()) + " -> " + lIds.get(lEdge.getTarget()) + " [label = \"" + lEdge.getAnnotation() + "\"];");
      }
    }


    lPrintWriter.print("}");


    return lResult.toString();
  }

  public void printDotToFile(String pFileName) {
    assert(pFileName != null);

    File lFile = null;

    try {
      lFile = File.createTempFile(pFileName, ".dot");
    } catch (IOException e) {
      e.printStackTrace();
      
      assert (false);

      System.exit(-1);
    }

    try {
      PrintWriter lWriter = new PrintWriter(lFile);

      lWriter.println(toDot());

      lWriter.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();

      assert (false);

      System.exit(-1);
    }
  }
}
