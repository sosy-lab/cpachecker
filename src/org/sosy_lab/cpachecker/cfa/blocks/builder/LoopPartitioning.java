package org.sosy_lab.cpachecker.cfa.blocks.builder;

import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.CFA;


/**
 * <code>PartitioningHeuristic</code> that creates blocks for each loop- and function-body.
 * @author dwonisch
 *
 */
public class LoopPartitioning extends PartitioningHeuristic {
  protected final LogManager logger;

  public LoopPartitioning(LogManager pLogger) {
    this.logger = pLogger;
  }

  @Override
  protected boolean shouldBeCached(CFANode pNode) {
    if(pNode instanceof CFAFunctionDefinitionNode && pNode.getNumEnteringEdges() == 0) {
      //main function
      return true;
    }
    if(pNode.isLoopStart()) {
      if(hasBlankEdgeFromLoop(pNode) || selfLoop(pNode)) {
        return false;
      }
      return true;
    }
    return false;
  }

  private static boolean hasBlankEdgeFromLoop(CFANode pNode) {
    for(int i = 0; i < pNode.getNumEnteringEdges(); i++) {
      CFAEdge edge = pNode.getEnteringEdge(i);
      if(edge instanceof BlankEdge && edge.getPredecessor().isLoopStart()) {
        return true;
      }
    }
    return false;
  }

  private static boolean selfLoop(CFANode pNode) {
    return pNode.getNumLeavingEdges() == 1 && pNode.getLeavingEdge(0).getSuccessor().equals(pNode);
  }

  @Override
  protected Set<CFANode> getCachedSubtree(CFANode pNode) {
    if(pNode instanceof CFAFunctionDefinitionNode) {
      CFAFunctionDefinitionNode functionNode = (CFAFunctionDefinitionNode) pNode;
      return CFA.exploreSubgraph(functionNode, functionNode.getExitNode());
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
    int i = loopStartLabel.indexOf("continue");
    if (i < 0) {
      logger.log(Level.WARNING, "Found no loop end for loop at node", pLoopHeader, "(loop is not considered for memorizing)");
      return false;
    }
    String loopName = loopStartLabel.split(" ")[1].substring(0, i - "continue".length() + 1);
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

  private static CFANode findNodeByEdgeLabel(CFANode startNode, String label) {
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
