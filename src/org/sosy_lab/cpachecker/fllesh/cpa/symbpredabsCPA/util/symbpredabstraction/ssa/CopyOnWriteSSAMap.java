package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.ssa;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.SymbolicFormulaList;

public class CopyOnWriteSSAMap extends SSAMap {
  
  private UnmodifiableSSAMap mOriginalSSAMap;
  
  public CopyOnWriteSSAMap(UnmodifiableSSAMap pSSAMap) {
    super(pSSAMap.VARIABLES, pSSAMap.FUNCTIONS);
    mOriginalSSAMap = pSSAMap;
  }
  
  @Override
  public UnmodifiableSSAMap immutable() {
    Map<String, Integer> lVars = null;
    Map<FuncKey, Integer> lFuncs = null;
    
    if (VARIABLES == mOriginalSSAMap.VARIABLES) {
      lVars = VARIABLES;
    }
    else {
      lVars = new HashMap<String, Integer>(VARIABLES);
    }
    
    if (FUNCTIONS == mOriginalSSAMap.FUNCTIONS) {
      lFuncs = FUNCTIONS;
    }
    else {
      lFuncs = new HashMap<FuncKey, Integer>(FUNCTIONS);
    }
    
    return new UnmodifiableSSAMap(lVars, lFuncs);
  }
  
  @Override
  public int getIndex(String pVariable) {
    return super.getIndex(pVariable);
  }
  
  @Override
  public void setIndex(String pVariable, int pIndex) {
    if (VARIABLES == mOriginalSSAMap.VARIABLES) {
      VARIABLES = new HashMap<String, Integer>(VARIABLES);
    }
    
    super.setIndex(pVariable, pIndex);
  }
  
  @Override
  public int getIndex(String name, SymbolicFormulaList args) {
    return super.getIndex(name, args);
  }
    
  @Override
  public void setIndex(String name, SymbolicFormulaList args, int idx) {
    if (FUNCTIONS == mOriginalSSAMap.FUNCTIONS) {
      FUNCTIONS = new HashMap<FuncKey, Integer>(FUNCTIONS);
    }
    
    super.setIndex(name, args, idx);
  }
  
  @Override
  public void update(SSAMap other) {
    if (VARIABLES == mOriginalSSAMap.VARIABLES) {
      VARIABLES = new HashMap<String, Integer>(VARIABLES);
    }
    
    if (FUNCTIONS == mOriginalSSAMap.FUNCTIONS) {
      FUNCTIONS = new HashMap<FuncKey, Integer>(FUNCTIONS);
    }
    
    super.update(other);
  }
  
}
