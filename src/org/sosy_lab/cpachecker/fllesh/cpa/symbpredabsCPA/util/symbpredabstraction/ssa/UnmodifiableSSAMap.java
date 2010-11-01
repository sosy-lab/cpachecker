package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.ssa;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.Lists;

public class UnmodifiableSSAMap implements ISSAMap<UnmodifiableSSAMap> {
  
  protected Map<String, Integer> VARIABLES;
  protected Map<Pair<String, SymbolicFormulaList>, Integer> FUNCTIONS;
  
  public static final UnmodifiableSSAMap EMPTY_MAP = new UnmodifiableSSAMap();
  
  private UnmodifiableSSAMap() {
    VARIABLES = new HashMap<String, Integer>();
    FUNCTIONS = new HashMap<Pair<String, SymbolicFormulaList>, Integer>();
  }
  
  public UnmodifiableSSAMap(UnmodifiableSSAMap pSSAMap) {
    this(pSSAMap.VARIABLES, pSSAMap.FUNCTIONS);
  }
  
  protected UnmodifiableSSAMap(Map<String, Integer> pVariables, Map<Pair<String, SymbolicFormulaList>, Integer> pFunctions) {
    VARIABLES = pVariables;
    FUNCTIONS = pFunctions;
  }
  
  @Override
  public int getIndex(String variable) {
    Integer i = VARIABLES.get(variable);
    if (i != null) {
      return i;
    } else {
      // no index found, return -1
      return -1;
    }
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
  
  private static final MapJoiner joiner = Joiner.on(" ").withKeyValueSeparator("@");
  
  @Override
  public String toString() {
    return joiner.join(VARIABLES) + " " + joiner.join(FUNCTIONS);
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
  public CopyOnWriteSSAMapBuilder builder() {
    return new CopyOnWriteSSAMapBuilder(this);
  }

  @Override
  public Collection<String> allVariables() {
    return Collections.unmodifiableSet(VARIABLES.keySet());
  }

  @Override
  public Collection<Pair<String, SymbolicFormulaList>> allFunctions() {
    List<Pair<String, SymbolicFormulaList>> ret = Lists.newArrayList();

    for (Pair<String, SymbolicFormulaList> k : FUNCTIONS.keySet()) {
      ret.add(new Pair<String, SymbolicFormulaList>(k.getFirst(), k.getSecond()));
    }
    return ret;
  }

  @Override
  public UnmodifiableSSAMap merge(UnmodifiableSSAMap pSSAMap) {
    ISSAMapBuilder<UnmodifiableSSAMap> lSSAMapBuilder = builder();
    
    for (String lVariable : allVariables()) {
      int lIndex1 = getIndex(lVariable); // lIndex1 >= 0
      int lIndex2 = getIndex(lVariable);
      
      if (lIndex1 != lIndex2) {
        lSSAMapBuilder.setIndex(lVariable, Math.max(lIndex1, lIndex2));
      }
    }
    
    for (String lVariable : pSSAMap.allVariables()) {
      if (getIndex(lVariable) == -1) {
        lSSAMapBuilder.setIndex(lVariable, pSSAMap.getIndex(lVariable));
      }
    }
    
    
    for (Pair<String, SymbolicFormulaList> lFunction : allFunctions()) {
      int lIndex1 = getIndex(lFunction); // lIndex1 >= 0
      int lIndex2 = getIndex(lFunction);
      
      if (lIndex1 != lIndex2) {
        lSSAMapBuilder.setIndex(lFunction, Math.max(lIndex1, lIndex2));
      }
    }
    
    for (Pair<String, SymbolicFormulaList> lFunction : pSSAMap.allFunctions()) {
      if (getIndex(lFunction) == -1) {
        lSSAMapBuilder.setIndex(lFunction, pSSAMap.getIndex(lFunction));
      }
    }
    
    
    return lSSAMapBuilder.build();
  }

  @Override
  public UnmodifiableSSAMap update(UnmodifiableSSAMap pSSAMap) {
    CopyOnWriteSSAMapBuilder lSSAMapBuilder = builder();
    
    lSSAMapBuilder.update(pSSAMap);
    
    return lSSAMapBuilder.build();
  }
  
  @Override
  public UnmodifiableSSAMap emptySSAMap() {
    return UnmodifiableSSAMap.EMPTY_MAP;
  }
  
}
