package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.ssa;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;

/*
 * ISSAMaps are unmodifiable SSAMaps, this is not totally reflected in the usage code now.
 * TODO: fix that.
 * 
 */
public interface ISSAMap {

  public interface ISSAMapBuilder {
    
    public int getIndex(String pVariable);
    public int getIndex(String pFunction, SymbolicFormulaList pArguments);
    public int getIndex(Pair<String, SymbolicFormulaList> pFunction);
    
    public void setIndex(String pVariable, int pIndex);
    public void setIndex(String pFunction, SymbolicFormulaList pArguments, int pIndex);
    public void setIndex(Pair<String, SymbolicFormulaList> pFunction, int pIndex);
    
    public ISSAMap build();
    
  }
  
  public ISSAMapBuilder builder();
  
  public int getIndex(String pVariable);
  public int getIndex(String pFunction, SymbolicFormulaList pArguments);
  public int getIndex(Pair<String, SymbolicFormulaList> pFunction);
  
  public Iterable<String> allVariables();
  public Iterable<Pair<String, SymbolicFormulaList>> allFunctions();
  
  public ISSAMap merge(ISSAMap pSSAMap);
  
  public ISSAMap update(ISSAMap pSSAMap);
  
}
