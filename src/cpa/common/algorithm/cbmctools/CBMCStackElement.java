package cpa.common.algorithm.cbmctools;

import java.util.ArrayList;
import java.util.List;

import cfa.objectmodel.c.AssumeEdge;

public class CBMCStackElement {

  // element id of the art element that has the conditional statement
  private int elementId;
  // true for if, false for else
  private boolean condition;
  // this is the code of this element
  private List<Object> codeList;
  
  public CBMCStackElement(int pElementId, String pFunctionName){
    elementId = pElementId;
    codeList = new ArrayList<Object>();
    codeList.add(pFunctionName);
  }

  public CBMCStackElement(int pElementId, AssumeEdge pEdge) {
    elementId = pElementId;
    codeList = new ArrayList<Object>();
    boolean truthAssumption = pEdge.getTruthAssumption();
    condition = truthAssumption;
  }

  public int getElementId() {
    return elementId;
  }

  public void setElementId(int pElementId) {
    elementId = pElementId;
  }

  public boolean isCondition() {
    return condition;
  }

  public void setCondition(boolean pCondition) {
    condition = pCondition;
  }

  public void write(Object pStatement){
    codeList.add(pStatement);
  }

  public StringBuffer getCode(){
    StringBuffer ret = new StringBuffer();

    for(Object obj: codeList){
      // check whether we have a simple statement
      // or a conditional statement
      if(obj instanceof String){
        ret.append((String)obj);
        ret.append("\n");
      }
      else if(obj instanceof CBMCStackElement){
        ret.append(((CBMCStackElement)obj).getCode());
        ret.append("\n");
      }
      else{
        assert(false);
      }
    }

    return ret;

  }
  
  @Override
  public String toString() {
    return "Element id: " + elementId + " Condition: " + condition;
  }

}
