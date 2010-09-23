package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabs.util.ssa;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;

class FuncKey {
  private final String name;
  private final SymbolicFormulaList args;

  public FuncKey(String n, SymbolicFormulaList a) {
      name = n;
      args = a;
  }

  public String getName() { 
    return name; 
  }
  
  public SymbolicFormulaList getArgs() { 
    return args; 
  }

  @Override
  public int hashCode() {
      return 31 * name.hashCode() + args.hashCode();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass().equals(getClass())) {
      FuncKey lOther = (FuncKey)pOther;
      
      return name.equals(lOther.name) && args.equals(lOther.args);
    }
    
    return false;
  }
  
}
