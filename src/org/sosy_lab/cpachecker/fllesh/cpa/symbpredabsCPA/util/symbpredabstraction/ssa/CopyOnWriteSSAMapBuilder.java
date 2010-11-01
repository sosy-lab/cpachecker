package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.ssa;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;

public class CopyOnWriteSSAMapBuilder implements ISSAMap.ISSAMapBuilder {
  
  private UnmodifiableSSAMap mOriginalSSAMap;
  
  Map<String, Integer> VARIABLES;
  Map<Pair<String, SymbolicFormulaList>, Integer> FUNCTIONS;
  
  public CopyOnWriteSSAMapBuilder(UnmodifiableSSAMap pSSAMap) {
    VARIABLES = pSSAMap.VARIABLES;
    FUNCTIONS = pSSAMap.FUNCTIONS;
    mOriginalSSAMap = pSSAMap;
  }
  
  @Override
  public UnmodifiableSSAMap build() {
    Map<String, Integer> lVars = null;
    Map<Pair<String, SymbolicFormulaList>, Integer> lFuncs = null;
    
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
      lFuncs = new HashMap<Pair<String, SymbolicFormulaList>, Integer>(FUNCTIONS);
    }
    
    return new UnmodifiableSSAMap(lVars, lFuncs);
  }
  
  @Override
  public int getIndex(String pVariable) {
    Integer i = VARIABLES.get(pVariable);
    if (i != null) {
      return i;
    } else {
      // no index found, return -1
      return -1;
    }
  }
  
  @Override
  public void setIndex(String pVariable, int pIndex) {
    if (VARIABLES == mOriginalSSAMap.VARIABLES) {
      VARIABLES = new HashMap<String, Integer>(VARIABLES);
    }
    
    VARIABLES.put(pVariable, pIndex);
  }
  
  @Override
  public int getIndex(String name, SymbolicFormulaList args) {
    return getIndex(new Pair<String, SymbolicFormulaList>(name, args));
  }
  
  @Override
  public int getIndex(Pair<String, SymbolicFormulaList> pFunction) {
    Integer i = FUNCTIONS.get(pFunction);
    
    if (i != null) {
      return i;
    }
    else {
      return -1;
    }
  }
    
  @Override
  public void setIndex(String name, SymbolicFormulaList args, int idx) {
    setIndex(new Pair<String, SymbolicFormulaList>(name, args), idx);
  }
  

  @Override
  public void setIndex(Pair<String, SymbolicFormulaList> pFunction, int pIndex) {
    if (FUNCTIONS == mOriginalSSAMap.FUNCTIONS) {
      FUNCTIONS = new HashMap<Pair<String, SymbolicFormulaList>, Integer>(FUNCTIONS);
    }
    
    FUNCTIONS.put(pFunction, pIndex);
  }
  
  public void update(ISSAMap pSSAMap) {
    if (VARIABLES == mOriginalSSAMap.VARIABLES) {
      VARIABLES = new HashMap<String, Integer>(VARIABLES);
    }
    
    if (FUNCTIONS == mOriginalSSAMap.FUNCTIONS) {
      FUNCTIONS = new HashMap<Pair<String, SymbolicFormulaList>, Integer>(FUNCTIONS);
    }
    
    for (String lVariable : pSSAMap.allVariables()) {
      if (!VARIABLES.containsKey(lVariable)) {
        VARIABLES.put(lVariable, pSSAMap.getIndex(lVariable));
      }
    }
    
    for (Pair<String, SymbolicFormulaList> lFunction : pSSAMap.allFunctions()) {
      if (!FUNCTIONS.containsKey(lFunction)) {
        FUNCTIONS.put(lFunction, pSSAMap.getIndex(lFunction));
      }
    }
  }
  
}
