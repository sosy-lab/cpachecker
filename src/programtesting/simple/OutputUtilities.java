/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package programtesting.simple;

import cfa.objectmodel.CFAEdge;
import cpa.common.CompositeElement;
import cpa.common.automaton.Automaton;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import programtesting.ParametricAbstractReachabilityTree;
import programtesting.simple.QDPTCompositeCPA.Edge;
import programtesting.simple.QDPTCompositeCPA.QDPTCompositeElement;

/**
 *
 * @author holzera
 */
public class OutputUtilities {
  public static void outputAbstractReachabilityTree(String pFileId, QDPTCompositeElement pRoot, Collection<QDPTCompositeElement> pSpecialElements) {
    assert(pFileId != null);
    assert(pRoot != null);
    assert(pSpecialElements != null);
    
    File lFile = null;

    try {
      lFile = File.createTempFile(pFileId, ".dot");
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }

    List<String> lNodeDefinitions = new LinkedList<String>();
    
    List<String> lEdgeDefinitions = new LinkedList<String>();
    
    Stack<QDPTCompositeElement> lWorklist = new Stack<QDPTCompositeElement>();
    lWorklist.push(pRoot);
    
    int lUniqueId = 0;
    
    Map<QDPTCompositeElement, Integer> lIdMap = new HashMap<QDPTCompositeElement, Integer>();
    
    // putting ids into map
    while (!lWorklist.empty()) {
      QDPTCompositeElement lCurrentElement = lWorklist.pop();
      
      lIdMap.put(lCurrentElement, lUniqueId);
      
      for (Edge lEdge : lCurrentElement.getChildren()) {
        lWorklist.push(lEdge.getChild());
      }
      
      if (pSpecialElements.contains(lCurrentElement)) {
        lNodeDefinitions.add("node [label = \"<" + lCurrentElement + ", " + lCurrentElement.getDepth() + ">\", shape=diamond, fillcolor=yellow, style=filled]; " + (lUniqueId++) + ";");
      }
      else {
        lNodeDefinitions.add("node [label = \"<" + lCurrentElement + ", " + lCurrentElement.getDepth() + ">\", shape=box, fillcolor=white]; " + (lUniqueId++) + ";");
      }
    }
    
    
    lWorklist.push(pRoot);
    while (!lWorklist.empty()) {
      QDPTCompositeElement lCurrentElement = lWorklist.pop();
      
      Integer lId = lIdMap.get(lCurrentElement);
      
      lNodeDefinitions.add("node [label = \"" + lCurrentElement + "\", shape=box]; " + lId + ";");

      for (Edge lEdge : lCurrentElement.getChildren()) {
        QDPTCompositeElement lChildElement = lEdge.getChild();
        
        lWorklist.push(lChildElement);
        
        if (lEdge instanceof QDPTCompositeCPA.HasSubpaths) {
          if (lEdge instanceof QDPTCompositeCPA.CFAEdgeAndSubpathsEdge) {
            lEdgeDefinitions.add(lId + " -> " + lIdMap.get(lChildElement) + " [style=bold, color=green];");
          }
          else {
            lEdgeDefinitions.add(lId + " -> " + lIdMap.get(lChildElement) + " [style=bold, color=blue];");
          }
        }
        else {
          lEdgeDefinitions.add(lId + " -> " + lIdMap.get(lChildElement) + ";");
        }
      }
    }
    
    try {
      PrintWriter lWriter = new PrintWriter(lFile);
      
      lWriter.println("digraph ART {");
      
      for (String lString : lNodeDefinitions) {
        lWriter.println(lString);
      }
      
      for (String lString : lEdgeDefinitions) {
        lWriter.println(lString);
      }
      
      lWriter.print("}");

      lWriter.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      assert (false);
    }
  }
  
  public static void outputAbstractReachabilityTreePDF(String pFileId, QDPTCompositeElement pRoot, Collection<QDPTCompositeElement> pSpecialElements) {
    assert(pFileId != null);
    assert(pRoot != null);
    assert(pSpecialElements != null);
    
    File lFile = null;

    try {
      lFile = File.createTempFile(pFileId, ".dot");
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }

    lFile.deleteOnExit();

    List<String> lNodeDefinitions = new LinkedList<String>();
    
    List<String> lEdgeDefinitions = new LinkedList<String>();
    
    Stack<QDPTCompositeElement> lWorklist = new Stack<QDPTCompositeElement>();
    lWorklist.push(pRoot);
    
    int lUniqueId = 0;
    
    Map<QDPTCompositeElement, Integer> lIdMap = new HashMap<QDPTCompositeElement, Integer>();
    
    // putting ids into map
    while (!lWorklist.empty()) {
      QDPTCompositeElement lCurrentElement = lWorklist.pop();
      
      lIdMap.put(lCurrentElement, lUniqueId);
      
      for (Edge lEdge : lCurrentElement.getChildren()) {
        lWorklist.push(lEdge.getChild());
      }
      
      if (pSpecialElements.contains(lCurrentElement)) {
        lNodeDefinitions.add("node [label = \"<" + lCurrentElement + ", " + lCurrentElement.getDepth() + ">\", shape=diamond, fillcolor=yellow, style=filled]; " + (lUniqueId++) + ";");
      }
      else {
        lNodeDefinitions.add("node [label = \"<" + lCurrentElement + ", " + lCurrentElement.getDepth() + ">\", shape=box, fillcolor=white]; " + (lUniqueId++) + ";");
      }
    }
    
    
    lWorklist.push(pRoot);
    while (!lWorklist.empty()) {
      QDPTCompositeElement lCurrentElement = lWorklist.pop();
      
      Integer lId = lIdMap.get(lCurrentElement);
      
      lNodeDefinitions.add("node [label = \"" + lCurrentElement + "\", shape=box]; " + lId + ";");

      for (Edge lEdge : lCurrentElement.getChildren()) {
        QDPTCompositeElement lChildElement = lEdge.getChild();
        
        lWorklist.push(lChildElement);
        
        if (lEdge instanceof QDPTCompositeCPA.HasSubpaths) {
          if (lEdge instanceof QDPTCompositeCPA.CFAEdgeAndSubpathsEdge) {
            lEdgeDefinitions.add(lId + " -> " + lIdMap.get(lChildElement) + " [style=bold, color=green];");
          }
          else {
            lEdgeDefinitions.add(lId + " -> " + lIdMap.get(lChildElement) + " [style=bold, color=blue];");
          }
        }
        else {
          lEdgeDefinitions.add(lId + " -> " + lIdMap.get(lChildElement) + ";");
        }
      }
    }
    
    try {
      PrintWriter lWriter = new PrintWriter(lFile);
      
      lWriter.println("digraph ART {");
      
      lWriter.println("size=\"6,10\";");
      
      for (String lString : lNodeDefinitions) {
        lWriter.println(lString);
      }
      
      for (String lString : lEdgeDefinitions) {
        lWriter.println(lString);
      }
      
      lWriter.print("}");

      lWriter.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      assert (false);
    }
    

    try {
      File lPostscriptFile = File.createTempFile(pFileId, ".ps");

      lPostscriptFile.deleteOnExit();

      Process lDotProcess = Runtime.getRuntime().exec("dot -Tps -o" + lPostscriptFile.getAbsolutePath() + " " + lFile.getAbsolutePath());

      lDotProcess.waitFor();

      File lPDFFile = File.createTempFile(pFileId, ".pdf");

      Process lPs2PdfProcess = Runtime.getRuntime().exec("ps2pdf " + lPostscriptFile.getAbsolutePath() + " " + lPDFFile.getAbsolutePath());

      lPs2PdfProcess.waitFor();
    } catch (Exception e) {
      e.printStackTrace();
      assert (false);
    }
  }
  
  public static void outputAbstractReachabilityTree(String pFileId, Collection<CompositeElement> pSpecialElements, ParametricAbstractReachabilityTree<CompositeElement> pAbstractReachabilityTree) {
    assert(pAbstractReachabilityTree != null);
    assert(pFileId != null);
    assert(pSpecialElements != null);
    
    File lFile = null;

    try {
      lFile = File.createTempFile(pFileId, ".dot");
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }

    lFile.deleteOnExit();

    PrintWriter lWriter = null;

    try {
      lWriter = new PrintWriter(lFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      assert (false);
    }

    lWriter.println(pAbstractReachabilityTree.toDot(pSpecialElements));

    lWriter.close();

    try {
      File lPostscriptFile = File.createTempFile(pFileId, ".ps");

      lPostscriptFile.deleteOnExit();

      Process lDotProcess = Runtime.getRuntime().exec("dot -Tps -o" + lPostscriptFile.getAbsolutePath() + " " + lFile.getAbsolutePath());

      lDotProcess.waitFor();

      File lPDFFile = File.createTempFile(pFileId, ".pdf");

      Process lPs2PdfProcess = Runtime.getRuntime().exec("ps2pdf " + lPostscriptFile.getAbsolutePath() + " " + lPDFFile.getAbsolutePath());

      lPs2PdfProcess.waitFor();
    } catch (Exception e) {
      e.printStackTrace();
      assert (false);
    }
  }
  
  public static void printTestGoals(String pTitle, Collection<Automaton<CFAEdge>.State> pTestGoals) {
    System.out.print(pTitle);

    printTestGoals(pTestGoals);
  }
  
  public static void printTestGoals(Collection<Automaton<CFAEdge>.State> pTestGoals) {
    boolean lFirstTestGoal = true;

    System.out.print("{");

    for (Automaton<CFAEdge>.State lTestGoal : pTestGoals) {
      if (lFirstTestGoal) {
        lFirstTestGoal = false;
      } else {
        System.out.print(",");
      }

      System.out.print("q" + lTestGoal.getIndex());
    }

    System.out.println("}");
  }
}
