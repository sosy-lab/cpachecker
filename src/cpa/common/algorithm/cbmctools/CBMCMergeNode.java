package cpa.common.algorithm.cbmctools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class CBMCMergeNode {

  private int elementId;
  private int numberOfProcessed;
  private Map<Integer, Boolean> branchesMap;
  private Set<Integer> processedConditions;

  public CBMCMergeNode(int pElementId) {
    elementId = pElementId;
    numberOfProcessed = 0;
    branchesMap = new HashMap<Integer, Boolean>();
    processedConditions = new HashSet<Integer>();
  }

  public Set<Integer> getProcessedConditions(){
    return processedConditions;
  }

  public int addBranch(CBMCEdge pNextCBMCEdge) {

    numberOfProcessed++;

    Stack<CBMCStackElement> addedStackElement = pNextCBMCEdge.getStack().peek();

    for(CBMCStackElement elementInStack: addedStackElement){
      int idOfElementInStack = elementInStack.getElementId();
      boolean nextConditionValue = elementInStack.isCondition();

      // if we already have a value for the same initial node of the condition
      if(branchesMap.containsKey(idOfElementInStack)){
        boolean firstConditionValue = branchesMap.get(idOfElementInStack);
        // if this is the end of the branch
        if(firstConditionValue ^ nextConditionValue){
          processedConditions.add(idOfElementInStack);
        }
        // else do nothing
      }
      // create the first entry in the map
      else{
        branchesMap.put(idOfElementInStack, nextConditionValue);
      }
    }
    return numberOfProcessed;
  }
  
  @Override
  public String toString() {
    return "id: " + elementId + " >> " + branchesMap;
  }

}
