package de.upb.agw.cpachecker.cpa.abm.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.util.CFA;

import de.upb.agw.cpachecker.cpa.abm.sa.ReferencedVariablesCollector;

/**
 * Helper class can build a <code>BlockPartitioning</code> from a partition of a program's CFA into blocks.
 * @author dwonisch
 *
 */
public class BlockPartitioningBuilder {
  private ReferencedVariablesCollector referenceCollector = null;
  
  private Map<CFANode, Set<ReferencedVariable>> referencedVariablesMap = new HashMap<CFANode, Set<ReferencedVariable>>();
  private Map<CFANode, Set<CFANode>> callNodesMap = new HashMap<CFANode, Set<CFANode>>();
  private Map<CFANode, Set<CFANode>> returnNodesMap = new HashMap<CFANode, Set<CFANode>>();
  private Map<CFANode, Set<CFANode>> innerFunctionCallsMap = new HashMap<CFANode, Set<CFANode>>();
  private Map<CFANode, Set<CFANode>> blockNodesMap = new HashMap<CFANode, Set<CFANode>>();
  
  public BlockPartitioningBuilder(Set<CFANode> mainFunctionBody) {        
    referenceCollector = new ReferencedVariablesCollector(mainFunctionBody);    
  }
  
  public BlockPartitioning build() {
    //fixpoint iteration to take inner function calls into account for referencedVariables and callNodesMap
    boolean changed = true;   
    outer: while(changed) {
      changed = false;
      for(CFANode node : referencedVariablesMap.keySet()) {
        for(CFANode calledFun : innerFunctionCallsMap.get(node)) { 
          Set<ReferencedVariable> functionVars = referencedVariablesMap.get(calledFun);
          Set<CFANode> functionBody = blockNodesMap.get(calledFun);
          if(functionVars == null || functionBody == null) {
            assert functionVars == null && functionBody == null;
            //compute it only the fly
            functionBody = CFA.exploreSubgraph(calledFun, ((CFAFunctionDefinitionNode)calledFun).getExitNode());
            functionVars = collectReferencedVariables(functionBody);
            //and save it
            blockNodesMap.put(calledFun, functionBody);
            referencedVariablesMap.put(calledFun, functionVars); 
            innerFunctionCallsMap.put(calledFun, collectInnerFunctionCalls(functionBody));            
            changed = true;
            continue outer;
          }
          
          if(referencedVariablesMap.get(node).addAll(functionVars)) {
            changed = true;
          } 
          if(blockNodesMap.get(node).addAll(functionBody)) {
            changed = true;
          } 
        }
      }
    }
    
    //copy block nodes
    Map<CFANode, Set<CFANode>> uniqueNodesMap = new HashMap<CFANode, Set<CFANode>>();
    for(CFANode key : blockNodesMap.keySet()) {
      uniqueNodesMap.put(key, new HashSet<CFANode>(blockNodesMap.get(key)));
    }
    
    //fix unique nodes set by removing interleavings
    for(CFANode key : returnNodesMap.keySet()) {
      Set<CFANode> outerNodes = uniqueNodesMap.get(key);
      for(CFANode innerKey : returnNodesMap.keySet()) {
        if(innerKey.equals(key)) {
          continue;
        }
        if(outerNodes.containsAll(callNodesMap.get(innerKey))) {
          Set<CFANode> innerNodes = uniqueNodesMap.get(innerKey);
          outerNodes.removeAll(innerNodes);
        }
      }
    }  
    
    
    //now we can create the Blocks   for the BlockPartitioning
    Collection<Block> blocks = new ArrayList<Block>(referencedVariablesMap.keySet().size());
    for(CFANode key : returnNodesMap.keySet()) {
      blocks.add(new Block(referencedVariablesMap.get(key), callNodesMap.get(key), returnNodesMap.get(key), uniqueNodesMap.get(key), blockNodesMap.get(key)));
    }
    return new BlockPartitioning(blocks);
  }
  
  /**
   * @param nodes Nodes from which Block should be created; if the set of nodes contains inner function calls, the called function body should NOT be included
   */
  
  public void addBlock(Set<CFANode> nodes) {
    Set<ReferencedVariable> referencedVariables = collectReferencedVariables(nodes);
    Set<CFANode> callNodes = collectCallNodes(nodes);
    Set<CFANode> returnNodes = collectReturnNodes(nodes);    
    Set<CFANode> innerFunctionCalls = collectInnerFunctionCalls(nodes);

    CFANode registerNode = null;
    for(CFANode node : callNodes) {
      registerNode = node;
      if(node instanceof FunctionDefinitionNode) {
        break;
      }
    }
    
    referencedVariablesMap.put(registerNode, referencedVariables);
    callNodesMap.put(registerNode, callNodes);
    returnNodesMap.put(registerNode, returnNodes);
    innerFunctionCallsMap.put(registerNode, innerFunctionCalls);   
    blockNodesMap.put(registerNode, nodes);
  }
 
  private Set<CFANode> collectInnerFunctionCalls(Set<CFANode> pNodes) {
    Set<CFANode> result = new HashSet<CFANode>();
    for(CFANode node : pNodes) {
      for(int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFANode succ = node.getLeavingEdge(i).getSuccessor();        
        if(node.getLeavingEdge(i) instanceof FunctionCallEdge) {
          result.add(succ);
        }        
      }
    }    
    return result;
  }
  
  private Set<CFANode> collectCallNodes(Set<CFANode> pNodes) {
    Set<CFANode> result = new HashSet<CFANode>();
    for(CFANode node : pNodes) {
      if(node instanceof CFAFunctionDefinitionNode && node.getFunctionName().equalsIgnoreCase("main")) {
        //main definition is always a call edge
        result.add(node);
        continue;
      }
      if(node.getEnteringSummaryEdge() != null) {
        CFANode pred = node.getEnteringSummaryEdge().getPredecessor();
        if(!pNodes.contains(pred)) {
          result.add(node);
        }
        //ignore inner function calls
        continue;
      }
      for(int i = 0; i < node.getNumEnteringEdges(); i++) {
        CFANode pred = node.getEnteringEdge(i).getPredecessor();
        if(!pNodes.contains(pred)) {
          //entering edge from "outside" of the given set of nodes
          //-> this is a call-node
          result.add(node);
        }
      }
    }
    return result;
  }
  
  private Set<CFANode> collectReturnNodes(Set<CFANode> pNodes) {
    Set<CFANode> result = new HashSet<CFANode>();
    for(CFANode node : pNodes) {
      if(node instanceof CFAFunctionExitNode && node.getFunctionName().equalsIgnoreCase("main")) {
        //main exit nodes are always return nodes
        result.add(node);
        continue;
      }
      
      for(int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFANode succ = node.getLeavingEdge(i).getSuccessor();
        if(!pNodes.contains(succ)) {
          //TODO: BUG: block ending directly with a function call
          //leaving edge from inside of the given set of nodes to outside
          //-> this is a either return-node or a function call
          if(!(node.getLeavingEdge(i) instanceof FunctionCallEdge)) {
            //-> only add if its not a function call
            result.add(node);
          }
        }
      }
    }    
    return result;
  } 

  private Set<ReferencedVariable> collectReferencedVariables(Set<CFANode> nodes) {
    return referenceCollector.collectVars(nodes);
  }
}
