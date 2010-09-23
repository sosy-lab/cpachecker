package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabs.util.ssa;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.SymbolicFormulaList;

public interface ReadableSSAMap {

  public int getIndex(String pVariable);
  public int getIndex(String pName, SymbolicFormulaList pArgs);
  public Iterable<String> allVariables();
  public Iterable<Pair<String, SymbolicFormulaList>> allFunctions();
  
}
