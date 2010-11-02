/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONObject;
import org.sosy_lab.common.Files;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnEdge;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * 
 * Generates one DOT file per function for the report.
 * For large programs the traditional method with one single DOT file 
 * crashed graphviz with "error in init rank" (quite popular in the graphviz bugtracker).
 * 
 * Additionally, information about the CFA is exported in JSON format (also needed for the report).
 * 
 * Linear sequences of "normal" edges(StatementEdges, DeclarationEdges, and BlankEdges)
 * are displayed as a node containing a table. The left column contains the node number
 * of the predecessor of an edge. The right column contains the edge label.
 * The successor can be found in the left column of the next row.
 * 
 * reuses some code from DOTBuilder
 * 
 * @author Hendrik Speidel
 */
public final class DOTBuilder2 {
  
  private DOTBuilder2() { /* utility class */ }
  
  /**
   * output the CFA as DOT and JSON files
   * 
   * @param cfa
   * @param outdir
   * @throws IOException 
   */
  public static void writeReport(CFAFunctionDefinitionNode cfa, File outdir) throws IOException {    
    CFAJSONBuilder jsoner = new CFAJSONBuilder();
    DOTViewBuilder dotter = new DOTViewBuilder();
    CFAVisitor vis = new CompositeCFAVisitor(jsoner, dotter);
    traverse(cfa, vis);
    dotter.writeFiles(outdir);
    jsoner.writeJSON(outdir);
  }
 
  private static void addLeavingEdges(CFANode node, Deque<CFAEdge> waitingEdgeList) {
    
    for (int edgeIdx = 0; edgeIdx < node.getNumLeavingEdges (); edgeIdx++) {
      CFAEdge edge = node.getLeavingEdge (edgeIdx);      
      waitingEdgeList.add(edge);
    }
    if (node.getLeavingSummaryEdge() != null) {
      waitingEdgeList.add(node.getLeavingSummaryEdge());
    }
  }
  
  private static String getEdgeText(CFAEdge edge) {
    //the first call to replaceAll replaces \" with \ " to prevent a bug in dotty.
    //future updates of dotty may make this obsolete.
    return edge.getRawStatement()
      .replaceAll("\\Q\\\"\\E", "\\ \"")
      .replaceAll ("\\\"", "\\\\\\\"")
      .replaceAll("\n", " ")
      .replaceAll("\\s+", " ")
      .replaceAll(" ;", ";");
  }
  
  /**
   * traverse the CFA depth first 
   * 
   * @param cfa
   * @param visitor
   */
  private static void traverse(CFAFunctionDefinitionNode cfa, CFAVisitor visitor) {
    Set<CFANode> visitedNodes = Sets.newHashSet();
    Deque<CFAEdge> waitingEdgeList = new ArrayDeque<CFAEdge>();

    visitedNodes.add(cfa);
    addLeavingEdges(cfa, waitingEdgeList);
       
    while (!waitingEdgeList.isEmpty ()) {
      
      CFAEdge edge = waitingEdgeList.removeLast();
      visitor.visitEdge(edge);
      if (!visitedNodes.contains(edge.getSuccessor())) {
        visitedNodes.add(edge.getSuccessor());
        addLeavingEdges(edge.getSuccessor(), waitingEdgeList);        
      }
    }
  }
  
  private static class CFAVisitor {
    void visitEdge(CFAEdge edge) {}
  }
  
  private static class CompositeCFAVisitor extends CFAVisitor {
    
    CFAVisitor[] visitors;
    
    public CompositeCFAVisitor(CFAVisitor... visitors) {      
      this.visitors = visitors;
    }
    
    @Override
    void visitEdge(CFAEdge edge) {
      for (CFAVisitor visitor: visitors) {
        visitor.visitEdge(edge);
      }
    }        
  }
  
  /**
   * output DOT files and meta information about virtual and combined edges
   */
  private static class DOTViewBuilder extends CFAVisitor {
    
    private final ListMultimap<String, CFANode> func2nodes = ArrayListMultimap.create();
    private final ListMultimap<String, CFAEdge> func2edges = ArrayListMultimap.create();
    private final ListMultimap<String, List<CFAEdge>> func2comboedge = ArrayListMultimap.create();
    private final Set<CFANode> visitedNodes = Sets.newHashSet();    
    private final JSONObject node2combo = new JSONObject();
    private final JSONObject virtFuncCallEdges = new JSONObject();    
    private int virtFuncCallNodeIdCounter = 100000;
    
    private List<CFAEdge> currentComboEdge = null;
    private boolean comboQualified;
    private CFANode toAdd = null;
    
    private void reset() {
      comboQualified = false;
      currentComboEdge = null;
      toAdd = null;
    }
    
    @Override
    void visitEdge(CFAEdge edge) {
      // first, handle predecessor if necessary
      CFANode predecessor = edge.getPredecessor();
      if (!visitedNodes.contains(predecessor)) {
        // source node
        toAdd = predecessor;      
        comboQualified = !predecessor.isLoopStart() 
            && predecessor.getNumEnteringEdges() == 1 && predecessor.getNumLeavingEdges() == 1 
            && predecessor.getLeavingSummaryEdge() == null;

      } else {
        currentComboEdge = null;
      }

      if (!comboQualified || edge instanceof ReturnEdge || edge instanceof AssumeEdge 
          || (edge instanceof FunctionCallEdge && !((FunctionCallEdge)edge).isExternalCall())) {
        
        // flush node
        if (toAdd != null) {
          func2nodes.put(toAdd.getFunctionName(), toAdd);
        }
        func2edges.put(edge.getPredecessor().getFunctionName(), edge);
        reset();

      } else {
        // add combo edge
        if (currentComboEdge == null) {
          currentComboEdge = Lists.newArrayList();
          func2comboedge.put(edge.getPredecessor().getFunctionName(), currentComboEdge);
        }
        currentComboEdge.add(edge);
      }

      // last, handle successor if necessary
      CFANode successor = edge.getSuccessor();
      if (successor.getNumLeavingEdges() == 0 && successor.getLeavingSummaryEdge() == null) {
        func2nodes.put(successor.getFunctionName(), successor);

        reset();
      }
    }
    
    void writeFiles(File outdir) throws IOException {
      
      for (String funcname: func2nodes.keySet()) {
        List<CFANode> nodes = func2nodes.get(funcname);
        List<CFAEdge> edges = func2edges.get(funcname);
        
        Writer out = new OutputStreamWriter(new FileOutputStream(new File(outdir, "cfa__" + funcname + ".dot")), "UTF-8");
        try {
          out.write("digraph " + funcname + " {\n");
          StringBuffer outb = new StringBuffer();
          //write comboedges
          for (List<CFAEdge> combo: func2comboedge.get(funcname)) {
            if (combo.size() == 1) {
              edges.add(combo.get(0));
              nodes.add(combo.get(0).getPredecessor());
            } else {
              outb.append(comboToDot(combo));
              CFAEdge first = combo.get(0);
              CFAEdge last = combo.get(combo.size() - 1);
              outb.append("" + first.getPredecessor().getNodeNumber());
              outb.append(" -> ");
              outb.append("" + last.getSuccessor().getNodeNumber());
              outb.append("[label=\"\" ];\n");
            }
          }

          //write nodes          
          for (CFANode node: nodes) {
            out.write(nodeToDot(node));
          }
          
          out.write(outb.toString());
          
          //write edges
          if (edges != null) {
            for (CFAEdge edge: edges) {
              out.write(edgeToDot(edge));
            }
          }
          out.write("}");
        } finally {
          out.close();
        }
        
        Files.writeFile(new File(outdir, "combinednodes.json"), node2combo.toJSONString());
        Files.writeFile(new File(outdir, "fcalledges.json"),    virtFuncCallEdges.toJSONString());        
      }
    }
    
    private static String nodeToDot(CFANode node) {
      String shape = "circle";
    
      if(node.isLoopStart()){
        shape = "doublecircle";
      } else if (node.getNumLeavingEdges() > 0 &&
          node.getLeavingEdge(0) instanceof AssumeEdge) {
        shape = "diamond";        
      }

      return "node [shape = " + shape + "]; " + node.getNodeNumber() + ";\n";
    }
    
    @SuppressWarnings("unchecked")
    private String edgeToDot(CFAEdge edge) {
      if (edge instanceof ReturnEdge) {
        return "";
      }
      if (edge instanceof FunctionCallEdge 
          && !((FunctionCallEdge)edge).isExternalCall()) {
       //create the function node
        String ret = "node [shape = component label=\"" + edge.getSuccessor().getFunctionName() + "\"]; " + (++virtFuncCallNodeIdCounter) + ";\n";
        int from = edge.getPredecessor().getNodeNumber();
        int to = edge.getPredecessor().getLeavingSummaryEdge().getSuccessor().getNodeNumber(); 
        ret += String.format("%d -> %d [label=\"%s\" fontname=\"Courier New\"];\n",
            from,
            virtFuncCallNodeIdCounter,
            getEdgeText(edge));
        ret += String.format("%d -> %d [label=\"\" fontname=\"Courier New\"];\n",
            virtFuncCallNodeIdCounter,
            to);
        virtFuncCallEdges.put(from, Lists.newArrayList(virtFuncCallNodeIdCounter, to));
        return ret;
      }
      String extra = "";
      if (edge instanceof CallToReturnEdge) {
        extra = "style=dotted arrowhead=empty";
      }
      return String.format("%d -> %d [label=\"%s\" %s fontname=\"Courier New\"];\n",
          edge.getPredecessor().getNodeNumber(),
          edge.getSuccessor().getNodeNumber(),
          getEdgeText(edge),
          extra);
    }

    @SuppressWarnings("unchecked")
    private String comboToDot(List<CFAEdge> combo) {
      CFAEdge first = combo.get(0);      
      StringBuffer sb = new StringBuffer();
      int firstNo = first.getPredecessor().getNodeNumber();
      sb.append(firstNo);
      sb.append(" [style=\"filled,bold\" penwidth=1 fillcolor=\"white\" fontname=\"Courier New\" shape=\"Mrecord\" label=");
      sb.append("<<table border=\"0\" cellborder=\"0\" cellpadding=\"3\" bgcolor=\"white\">");      

      for (CFAEdge edge: combo) {
        sb.append("<tr><td align=\"right\">");      
        sb.append("" + edge.getPredecessor().getNodeNumber());
        sb.append("</td><td align=\"left\">");
        sb.append("" + getEdgeText(edge).replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
        sb.append("</td></tr>");
        
        node2combo.put(edge.getPredecessor().getNodeNumber(), firstNo);
      }    
      sb.append("</table>>];\n");
      return sb.toString();      
    }

  }
  
  /**
   * output information about CFA nodes and edges as JSON
   *
   */
  private static class CFAJSONBuilder extends CFAVisitor {
    JSONObject nodes = new JSONObject();
    JSONObject edges = new JSONObject();
    Set<CFANode> visited = Sets.newHashSet();
    
    @SuppressWarnings("unchecked")
    void visitNode(CFANode node) {
      if (!visited.contains(node)) {
        JSONObject jnode = new JSONObject();
        jnode.put("no", node.getNodeNumber());
        jnode.put("line", node.getLineNumber());
        jnode.put("func", node.getFunctionName());
        nodes.put(node.getNodeNumber(), jnode);
        visited.add(node);
      }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    void visitEdge(CFAEdge edge) {
      visitNode(edge.getPredecessor());
      visitNode(edge.getSuccessor());
      JSONObject jedge = new JSONObject();
      int src = edge.getPredecessor().getNodeNumber();
      int target = edge.getSuccessor().getNodeNumber();
      jedge.put("line", edge.getLineNumber());
      jedge.put("source", src);
      jedge.put("target", target);
      jedge.put("stmt", getEdgeText(edge));
      jedge.put("type", edge.getEdgeType().toString());

      edges.put("" + src + "->" + target, jedge);    
    }
    
    void writeJSON(File outdir) throws IOException {
      Writer json = new OutputStreamWriter(new FileOutputStream(new File(outdir, "cfainfo.json")), "UTF-8");
      try {
        json.write(getJSON().toJSONString());
      } finally {
        json.close();
      }      
    }
    
    @SuppressWarnings("unchecked")
    JSONObject getJSON() {
      JSONObject obj = new JSONObject();
      obj.put("nodes", nodes);
      obj.put("edges", edges);
      return obj;
    }
  }
}
