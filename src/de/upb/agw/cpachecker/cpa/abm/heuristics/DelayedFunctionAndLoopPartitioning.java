package de.upb.agw.cpachecker.cpa.abm.heuristics;

import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.CFA;

import de.upb.agw.cpachecker.cpa.abm.sa.LoopDetector;

/**
 * <code>PartitioningHeuristic</code> that creates blocks for each loop- and function-body. 
 * In contrast to <code>FunctionAndLoopPartitioning</code> the heuristics tries to skip possible initial definitions at the blocks.
 * @author dwonisch
 *
 */
public class DelayedFunctionAndLoopPartitioning extends PartitioningHeuristic {
  protected LogManager logger;
  
  public DelayedFunctionAndLoopPartitioning(LogManager pLogger) {
    this.logger = pLogger;
  }

  @Override
  protected boolean shouldBeCached(CFANode pNode) {
    if(pNode instanceof CFAFunctionDefinitionNode)
      return true;
    if(pNode.isLoopStart()) {
      if(hasBlankEdgeFromLoop(pNode) || selfLoop(pNode)) {
        return false;
      }      
      return true;
    }
    return false;
  }
  
  private boolean hasBlankEdgeFromLoop(CFANode pNode) {
    for(int i = 0; i < pNode.getNumEnteringEdges(); i++) {
      CFAEdge edge = pNode.getEnteringEdge(i);
      if(edge instanceof BlankEdge && edge.getPredecessor().isLoopStart()) {
        return true;
      }
    }
    return false;
  }
  
  private boolean selfLoop(CFANode pNode) {
    return pNode.getNumLeavingEdges() == 1 && pNode.getLeavingEdge(0).getSuccessor().equals(pNode);   
  }

  @Override
  protected Set<CFANode> getCachedSubtree(CFANode pNode) {
    if(pNode instanceof CFAFunctionDefinitionNode) {
      CFAFunctionDefinitionNode functionNode = (CFAFunctionDefinitionNode) pNode;
      return removeInitialDeclarations(functionNode, CFA.exploreSubgraph(functionNode, functionNode.getExitNode()));
    }
    if(pNode.isLoopStart()) {
      LoopDetector detector = new LoopDetector();
      Set<CFANode> loopBody = detector.detectLoopBody(pNode);
      insertLoopStartState(loopBody, pNode);
      if(!insertLoopBreakState(loopBody, pNode)) {
        return null;
      }
      
      return loopBody;
    }
    return null;
  }
  
  private Set<CFANode> removeInitialDeclarations(CFAFunctionDefinitionNode functionNode, Set<CFANode> functionBody) {
    if(functionNode.getFunctionName().equalsIgnoreCase("main")) {
      return functionBody;
    }
    
    //TODO: currently a call edge must not be branch as otherwise we may find the error locations multiple times within a single run as the analysis does explore all branches to depth 1 even if in one branch a error is found 
    
    assert functionNode.getNumLeavingEdges() == 1;    
    CFANode currentNode = functionNode.getLeavingEdge(0).getSuccessor(); //skip initial blank edge
    functionBody.remove(functionNode);
    
    int skippedDeclarations = 0;
    
    while(currentNode.getNumLeavingEdges() == 1 && currentNode.getLeavingEdge(0).getSuccessor().getNumLeavingEdges() == 1) { 
      assert currentNode.getNumEnteringEdges() == 1;
      CFAEdge edge = currentNode.getLeavingEdge(0);
      if(edge.getEdgeType() != CFAEdgeType.DeclarationEdge) {
        break;
      }
      //it is a declaration -> skip it      
      skippedDeclarations++;
      functionBody.remove(edge.getPredecessor());
      currentNode = edge.getSuccessor();
    }
    
    while(currentNode.getNumLeavingEdges() == 1 && skippedDeclarations > 0  && currentNode.getLeavingEdge(0).getSuccessor().getNumLeavingEdges() == 1) {
      assert currentNode.getNumEnteringEdges() == 1;
      CFAEdge edge = currentNode.getLeavingEdge(0);
      if(edge.getEdgeType() != CFAEdgeType.StatementEdge) {
        break;
      }
      //skip as many (hopefully) definitions      
      skippedDeclarations--;
      System.out.println(edge);
      functionBody.remove(edge.getPredecessor());
      currentNode = edge.getSuccessor();
    }
    
    assert currentNode.getNumEnteringEdges() == 1;
    return functionBody;
  }

  private void insertLoopStartState(Set<CFANode> pLoopBody, CFANode pLoopHeader) {
    for(int i = 0; i < pLoopHeader.getNumEnteringEdges(); i++) {
      CFAEdge edge = pLoopHeader.getEnteringEdge(i);
      if(edge instanceof BlankEdge && !pLoopBody.contains(edge.getPredecessor())) {
        pLoopBody.add(edge.getPredecessor());
      }
    }
  }
  
  private boolean insertLoopBreakState(Set<CFANode> pLoopBody, CFANode pLoopHeader) {
    assert pLoopHeader.getNumLeavingEdges() == 1;
    String loopStartLabel = pLoopHeader.getLeavingEdge(0).getRawStatement();
    //loopStartLabel is of form "Label: $loopName$continue"; e.g. "Label: while_0_continue" with loopName="while_0" 
    String loopName = loopStartLabel.split(" ")[1].substring(0, loopStartLabel.indexOf("continue") - "continue".length() + 1);
    //breakNodes name is then loopName+"break"
    String breakName = "Goto: " + loopName + "break";
    CFANode breakNode = findNodeByEdgeLabel(pLoopHeader, breakName);
    //assert breakNode != null;
    if(breakNode == null) {
      logger.log(Level.WARNING, "Found no loop end for loop " + loopName + " (loop is not considered for memorizing)");      
      return false;
    }
    pLoopBody.addAll(CFA.exploreSubgraph(pLoopHeader, breakNode));
    return true;
  }
  
  private CFANode findNodeByEdgeLabel(CFANode startNode, String label) {
    Set<CFANode> nodes = CFA.exploreSubgraph(startNode, null);
    for(CFANode node : nodes) {
      for(int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        if(edge.getRawStatement().equals(label)) {
          return edge.getSuccessor();
        }
      }
    }
    return null;
  }
}
