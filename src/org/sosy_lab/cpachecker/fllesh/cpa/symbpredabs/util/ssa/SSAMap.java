package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabs.util.ssa;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;

public interface SSAMap {

  /**
   * returns the index of the variable in the map
   */
  public int getIndex(String variable);
  public void setIndex(String variable, int idx);

  public int getIndex(String name, SymbolicFormulaList args);
  public void setIndex(String name, SymbolicFormulaList args, int idx);
  
  public Iterable<String> allVariables();
  public Iterable<Pair<String, SymbolicFormulaList>> allFunctions();
  
  /**
   * updates this map with the contents of other. That is, adds to this map
   * all the variables present in other but not in this
   */
  public void update(SSAMap other);
  
}
