package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabs.util.ssa;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.SymbolicFormulaList;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class ImmutableSSAMap implements ReadableSSAMap {

  public static class Builder implements ReadableSSAMap {

    private ImmutableSSAMap mSSAMap;
    
    private HashMap<String, Integer> mVars;
    private HashMap<FuncKey, Integer> mFuncs;
    
    public Builder() {
      this(ImmutableSSAMap.EMPTY_SSA_MAP);
    }
    
    public Builder(ImmutableSSAMap pSSAMap) {
      mSSAMap = pSSAMap;
      mVars = new HashMap<String, Integer>();
      mFuncs = new HashMap<FuncKey, Integer>();
    }
    
    public void setSSAMap(ImmutableSSAMap pSSAMap) {
      mSSAMap = pSSAMap;
      mVars.clear();
      mFuncs.clear();
    }
    
    public ImmutableSSAMap build() {
      if (mVars.size() == 0 && mFuncs.size() == 0) {
        return mSSAMap;
      }
      
      ImmutableMap<String, Integer> lVars;
      
      if (mVars.size() == 0) {
        lVars = mSSAMap.mVars;
      }
      else {
        lVars = ImmutableMap.copyOf(mVars);
      }
      
      ImmutableMap<FuncKey, Integer> lFuncs;
      
      if (mFuncs.size() == 0) {
        lFuncs = mSSAMap.mFuncs;
      }
      else {
        lFuncs = ImmutableMap.copyOf(mFuncs);
      }
      
      return new ImmutableSSAMap(lVars, lFuncs);
    }
    
    @Override
    public int getIndex(String pVariable) {    
      Integer lIndex = mVars.get(pVariable);
        
      if (lIndex == null) {
        return mSSAMap.getIndex(pVariable);
      }
        
      return lIndex;
    }

    public void setIndex(String pVariable, int pIdx) {
      mVars.put(pVariable, pIdx);
    }

    @Override
    public int getIndex(String pName, SymbolicFormulaList pArgs) {
      FuncKey lFuncKey = new FuncKey(pName, pArgs);
      
      Integer lIndex = mFuncs.get(lFuncKey);
      
      if (lIndex == null) {
        return mSSAMap.getIndex(pName, pArgs);
      }
      
      return lIndex;
    }

    public void setIndex(String pName, SymbolicFormulaList pArgs, int pIdx) {
      FuncKey lFuncKey = new FuncKey(pName, pArgs);
      
      mFuncs.put(lFuncKey, pIdx);
    }

    @Override
    public Iterable<String> allVariables() {
      if (mVars.size() == 0) {
        return mSSAMap.allVariables();
      }
      
      if (mSSAMap.mVars.keySet().containsAll(mVars.keySet())) {
        return mSSAMap.allVariables();
      }
      
      HashSet<String> lVariables = new HashSet<String>(mVars.keySet());
      lVariables.addAll(mSSAMap.mVars.keySet());
      
      return lVariables;
    }

    @Override
    public Iterable<Pair<String, SymbolicFormulaList>> allFunctions() {
      if (mFuncs.size() == 0) {
        return mSSAMap.allFunctions();
      }
      
      if (mSSAMap.mFuncs.keySet().containsAll(mFuncs.keySet())) {
        return mSSAMap.allFunctions();
      }
      
      List<Pair<String, SymbolicFormulaList>> lFunctions = Lists.newArrayList();

      for (FuncKey k : mFuncs.keySet()) {
        lFunctions.add(new Pair<String, SymbolicFormulaList>(k.getName(), k.getArgs()));
      }
      
      for (FuncKey k : mSSAMap.mFuncs.keySet()) {
        lFunctions.add(new Pair<String, SymbolicFormulaList>(k.getName(), k.getArgs()));
      }
      
      return lFunctions;
    }

    public void update(ImmutableSSAMap pOther) {
      for (String lVariable : pOther.allVariables()) {
        if (!(mVars.containsKey(lVariable) || mSSAMap.mVars.containsKey(lVariable))) {
          mVars.put(lVariable, pOther.getIndex(lVariable));
        }
      }
      
      for (Pair<String, SymbolicFormulaList> lFunc : pOther.allFunctions()) {
        if (!(mFuncs.containsKey(lFunc.getFirst()) || mSSAMap.mFuncs.containsKey(lFunc.getFirst()))) {
          setIndex(lFunc.getFirst(), lFunc.getSecond(), pOther.getIndex(lFunc.getFirst()));
        }
      }
    }

  }
  
  public static final ImmutableSSAMap EMPTY_SSA_MAP = new ImmutableSSAMap();
  
  private final ImmutableMap<String, Integer> mVars;
  private final ImmutableMap<FuncKey, Integer> mFuncs;
  
  private ImmutableSSAMap() {
    ImmutableMap.Builder<String, Integer> lVarsBuilder = ImmutableMap.builder();
    mVars = lVarsBuilder.build();
    ImmutableMap.Builder<FuncKey, Integer> lFuncsBuilder = ImmutableMap.builder();
    mFuncs = lFuncsBuilder.build();
  }
  
  public ImmutableSSAMap(ImmutableMap<String, Integer> pVars, ImmutableMap<FuncKey, Integer> pFuncs) {
    mVars = pVars;
    mFuncs = pFuncs;
  }
  
  public ImmutableSSAMap(ImmutableSSAMap pSSAMap) {
    this(pSSAMap.mVars, pSSAMap.mFuncs);
  }
  
  @Override
  public int getIndex(String pVariable) {
    Integer i = mVars.get(pVariable);
    if (i != null) {
      return i;
    } else {
      // no index found, return -1
      return -1;
    }
  }

  @Override
  public int getIndex(String pName, SymbolicFormulaList pArgs) {
    Integer i = mFuncs.get(new FuncKey(pName, pArgs));
    if (i != null) {
      return i;
    } else {
      // no index found, return -1
      return -1;
    }
  }

  @Override
  public Iterable<String> allVariables() {
    return Collections.unmodifiableSet(mVars.keySet());
  }

  @Override
  public Iterable<Pair<String, SymbolicFormulaList>> allFunctions() {
    List<Pair<String, SymbolicFormulaList>> ret = Lists.newArrayList();

    for (FuncKey k : mFuncs.keySet()) {
      ret.add(new Pair<String, SymbolicFormulaList>(k.getName(), k.getArgs()));
    }
    
    return ret;
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
      ImmutableSSAMap lOtherMap = (ImmutableSSAMap)pOther;
      
      return lOtherMap.mVars.equals(mVars) && lOtherMap.mFuncs.equals(mFuncs);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 2234 + 31 * mVars.hashCode() + mFuncs.hashCode();
  }

}
