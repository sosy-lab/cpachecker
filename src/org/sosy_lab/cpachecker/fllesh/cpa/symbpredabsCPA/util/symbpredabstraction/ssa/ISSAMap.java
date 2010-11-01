package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.ssa;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;

/*
 * ISSAMaps are unmodifiable SSAMaps, this is not totally reflected in the usage code now.
 * TODO: fix that.
 * 
 */
public interface ISSAMap<T extends ISSAMap<T>> {

  // TODO documentation
  public interface ISSAMapBuilder<T> {
    
    public int getIndex(String pVariable);
    public int getIndex(String pFunction, SymbolicFormulaList pArguments);
    public int getIndex(Pair<String, SymbolicFormulaList> pFunction);
    
    public void setIndex(String pVariable, int pIndex);
    public void setIndex(String pFunction, SymbolicFormulaList pArguments, int pIndex);
    public void setIndex(Pair<String, SymbolicFormulaList> pFunction, int pIndex);
    
    /*
     * Returns an SSA map that reflects the current SSA map of the builder object.
     */
    public T build();
    
  }
  
  /*
   * Returns a SSA map builder that is initially equivalent to the SSA map.
   */
  public ISSAMapBuilder<T> builder();
  
  // TODO documentation
  public int getIndex(String pVariable);
  public int getIndex(String pFunction, SymbolicFormulaList pArguments);
  public int getIndex(Pair<String, SymbolicFormulaList> pFunction);
  
  // TODO do we need these methods ?
  public Iterable<String> allVariables();
  public Iterable<Pair<String, SymbolicFormulaList>> allFunctions();
  
  // TODO documentation
  public T merge(T pSSAMap);
  
  // TODO documentation
  public T update(T pSSAMap);
  
  /*
   * Returns an empty SSA map.
   */
  public T emptySSAMap();
  
}
