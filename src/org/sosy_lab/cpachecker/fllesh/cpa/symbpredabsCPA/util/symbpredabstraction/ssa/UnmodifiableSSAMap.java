package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.ssa;

import java.util.Map;

import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.SymbolicFormulaList;

public class UnmodifiableSSAMap extends SSAMap {
  
  public static final UnmodifiableSSAMap EMPTY_MAP = new UnmodifiableSSAMap();
  
  private UnmodifiableSSAMap() {
    super();
  }
  
  public UnmodifiableSSAMap(SSAMap ssa) {
    super(ssa);
  }
  
  protected UnmodifiableSSAMap(Map<String, Integer> pVariables, Map<FuncKey, Integer> pFunctions) {
    super(pVariables, pFunctions);
  }
  
  @Override
  public void setIndex(String pName, SymbolicFormulaList pArgs, int pIdx) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public void setIndex(String pVariable, int pIdx) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public void update(SSAMap pOther) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (getClass().equals(pOther.getClass())) {
      UnmodifiableSSAMap lSSAMap = (UnmodifiableSSAMap)pOther;
      
      return VARIABLES.equals(lSSAMap.VARIABLES) && FUNCTIONS.equals(lSSAMap.FUNCTIONS);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 31 * VARIABLES.hashCode() + FUNCTIONS.hashCode() + 243;
  }
  
  @Override
  public UnmodifiableSSAMap immutable() {
    return this;
  }
  
}
