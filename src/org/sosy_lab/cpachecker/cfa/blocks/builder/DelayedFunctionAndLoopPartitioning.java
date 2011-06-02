package org.sosy_lab.cpachecker.cfa.blocks.builder;

import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.CFA;


/**
 * <code>PartitioningHeuristic</code> that creates blocks for each loop- and function-body.
 * In contrast to <code>FunctionAndLoopPartitioning</code> the heuristics tries to skip possible initial definitions at the blocks.
 * @author dwonisch
 *
 */
public class DelayedFunctionAndLoopPartitioning extends FunctionAndLoopPartitioning {

  public DelayedFunctionAndLoopPartitioning(LogManager pLogger) {
    super(pLogger);
  }

  @Override
  protected Set<CFANode> getCachedSubtree(CFANode pNode) {
    if(pNode instanceof CFAFunctionDefinitionNode) {
      CFAFunctionDefinitionNode functionNode = (CFAFunctionDefinitionNode) pNode;
      return removeInitialDeclarations(functionNode, CFA.exploreSubgraph(functionNode, functionNode.getExitNode()));
    }

    return super.getCachedSubtree(pNode);
  }

  private Set<CFANode> removeInitialDeclarations(CFAFunctionDefinitionNode functionNode, Set<CFANode> functionBody) {
    if (functionNode.getNumEnteringEdges() == 0) {
      // this is the main function
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
}
