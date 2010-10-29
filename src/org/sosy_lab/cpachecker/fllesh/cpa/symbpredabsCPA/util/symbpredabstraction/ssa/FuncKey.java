package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.ssa;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;

class FuncKey {
  
  private final String mFunctionName;
  private final SymbolicFormulaList mFunctionArguments;

  public FuncKey(String pFunctionName, SymbolicFormulaList pFunctionArguments) {
      mFunctionName = pFunctionName;
      mFunctionArguments = pFunctionArguments;
  }

  public String getName() { 
    return mFunctionName; 
  }
  
  public SymbolicFormulaList getArgs() {
    return mFunctionArguments; 
  }

  @Override
  public int hashCode() {
      return 31 * mFunctionName.hashCode() + mFunctionArguments.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof FuncKey) {
        FuncKey f = (FuncKey)o;
        return mFunctionName.equals(f.mFunctionName) && mFunctionArguments.equals(f.mFunctionArguments);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return mFunctionName + "(" + mFunctionArguments + ")";
  }
  
}
