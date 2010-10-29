package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.ssa;

import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;

public abstract class ReadableSSAMap {

  protected Map<String, Integer> VARIABLES;
  protected Map<FuncKey, Integer> FUNCTIONS;
  
  /**
   * returns the index of the variable in the map
   */
  public abstract int getIndex(String pVariable);
  public abstract int getIndex(String pName, SymbolicFormulaList pArgs);
  public abstract Iterable<String> allVariables();
  public abstract Iterable<Pair<String, SymbolicFormulaList>> allFunctions();
  public abstract UnmodifiableSSAMap immutable();
  
}
