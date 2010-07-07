package org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat;

import java.util.HashMap;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Model;

public class MathsatModel implements Model {

  public enum MathsatType {
    Boolean,
    Uninterpreted,
    Real,
    Bitvector;
    
    public static MathsatType toMathsatType(int pTypeId) {
      
      switch (pTypeId) {
      case mathsat.api.MSAT_BOOL:
        return Boolean;
      case mathsat.api.MSAT_U:
        return Uninterpreted;
      case mathsat.api.MSAT_REAL:
        return Real;
      case mathsat.api.MSAT_BV:
        return Bitvector;
      }
      
      throw new IllegalArgumentException("Given parameter is not a mathsat type!");
    }
    
  }
  
  public interface MathsatAssignable {
    
  }
  
  public static class MathsatVariable implements MathsatAssignable {
    
    private String mName;
    private MathsatType mType;
    
    public MathsatVariable(String pName, MathsatType pType) {
      mName = pName;
      mType = pType;
    }
    
    public String getName() {
      return mName;
    }
    
    public MathsatType getType() {
      return mType;
    }
    
    @Override
    public String toString() {
      return mName + " : " + mType;
    }
    
    public static MathsatVariable toVariable(long pVariableId) {
      if (mathsat.api.msat_term_is_variable(pVariableId) == 0) {
        throw new IllegalArgumentException("Given mathsat id corresponds not to a variable! (" + mathsat.api.msat_term_repr(pVariableId) + ")");
      }
      
      long lDeclarationId = mathsat.api.msat_term_get_decl(pVariableId);
      String lName = mathsat.api.msat_decl_get_name(lDeclarationId);
      MathsatType lType = MathsatType.toMathsatType(mathsat.api.msat_decl_get_return_type(lDeclarationId));
      
      return new MathsatVariable(lName, lType);
    }
    
    @Override 
    public int hashCode() {
      return 324 + mName.hashCode() + mType.hashCode();
    }
    
    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      
      if (pOther == null) {
        return false;
      }
      
      if (!getClass().equals(pOther.getClass())) {
        return false;
      }
      
      MathsatVariable lVariable = (MathsatVariable)pOther;
      
      return mName.equals(lVariable.mName) && mType.equals(lVariable.mType);
    }
    
  }
  
  public static class MathsatFunction implements MathsatAssignable {
    
    private String mName;
    private MathsatType mReturnType;
    private long[] mArguments;
    
    private int mHashCode;
    
    public MathsatFunction(String pName, MathsatType pReturnType, long[] pArguments) {
      mName = pName;
      mReturnType = pReturnType;
      mArguments = pArguments;
      
      mHashCode = 32453 + mName.hashCode() + mReturnType.hashCode();
      
      for (long lValue : mArguments) {
        mHashCode += lValue;
      }
    }
    
    public String getName() {
      return mName;
    }
    
    public MathsatType getReturnType() {
      return mReturnType;
    }
    
    public int getArity() {
      return mArguments.length;
    }
    
    public long getArgument(int lArgumentIndex) {
      return mArguments[lArgumentIndex];
    }
    
    @Override
    public String toString() {
      String lArguments = "";
      
      boolean lIsFirst = true;
      
      for (long lValue : mArguments) {
        if (lIsFirst) {
          lIsFirst = false;
        }
        else {
          lArguments += ",";
        }
        
        lArguments += lValue;
      }
      
      return mName + "(" + lArguments + ") : " + mReturnType;
    }
    
    @Override
    public int hashCode() {
      return mHashCode;
    }
    
    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      
      if (pOther == null) {
        return false;
      }
      
      if (!getClass().equals(pOther.getClass())) {
        return false;
      }
      
      MathsatFunction lFunction = (MathsatFunction)pOther;
      
      if (lFunction.mName.equals(mName) && lFunction.mReturnType.equals(mReturnType) && lFunction.mArguments.length == mArguments.length) {
        for (int lArgumentIndex = 0; lArgumentIndex < mArguments.length; lArgumentIndex++) {
          if (mArguments[lArgumentIndex] != lFunction.mArguments[lArgumentIndex]) {
            return false;
          }
        }
        
        return true;
      }
      else {
        return false;
      }
    }
    
    public static MathsatFunction toFunction(long pFunctionId) {
      if (mathsat.api.msat_term_is_variable(pFunctionId) != 0) {
        throw new IllegalArgumentException("Given mathsat id is a variable! (" + mathsat.api.msat_term_repr(pFunctionId) + ")");
      }
      
      long lDeclarationId = mathsat.api.msat_term_get_decl(pFunctionId);
      String lName = mathsat.api.msat_decl_get_name(lDeclarationId);
      MathsatType lType = MathsatType.toMathsatType(mathsat.api.msat_decl_get_return_type(lDeclarationId));
      
      int lArity = mathsat.api.msat_decl_get_arity(lDeclarationId);
      
      // TODO we assume only constants (reals) as parameters for now
      long[] lArguments = new long[lArity];
      
      for (int lArgumentIndex = 0; lArgumentIndex < lArity; lArgumentIndex++) {
        long lArgument = mathsat.api.msat_term_get_arg(pFunctionId, lArgumentIndex);
        
        lArguments[lArgumentIndex] = MathsatNumber.toNumber(lArgument);
      }
      
      return new MathsatFunction(lName, lType, lArguments);
    }
    
  }
  
  public static class MathsatNumber {
    
    public static long toNumber(long pTermId) {
      if (mathsat.api.msat_term_is_number(pTermId) == 0) {
        throw new IllegalArgumentException("Mathsat term is not a number!");
      }
      
      return Long.valueOf(mathsat.api.msat_term_repr(pTermId));
    }
    
  }
  
  private static MathsatAssignable toAssignable(long pTermId) {
    long lDeclarationId = mathsat.api.msat_term_get_decl(pTermId);
    
    if (mathsat.api.MSAT_ERROR_DECL(lDeclarationId)) {
      throw new IllegalArgumentException("No declaration available!");
    }
    
    if (mathsat.api.msat_term_is_variable(pTermId) == 0) {
      return MathsatFunction.toFunction(pTermId);
    }
    else {
      return MathsatVariable.toVariable(pTermId);
    }
  }
  
  private HashMap<MathsatAssignable, Long> mModel;
  
  public MathsatModel(long lMathsatEnvironmentID) {
    mModel = new HashMap<MathsatAssignable, Long>();
    
    long lModelIterator = mathsat.api.msat_create_model_iterator(lMathsatEnvironmentID);
      
    while (mathsat.api.msat_model_iterator_has_next(lModelIterator) != 0) {
      long[] lModelElement = mathsat.api.msat_model_iterator_next(lModelIterator);
      
      long lKeyTerm = lModelElement[0];
      long lValueTerm = lModelElement[1];
      
      MathsatAssignable lAssignable = toAssignable(lKeyTerm);
      
      // TODO maybe we have to convert to SMTLIB format and then read in values in a controlled way, e.g., size of bitvector
      // TODO we are assuming numbers as values
      long lValue = MathsatNumber.toNumber(lValueTerm);
      
      mModel.put(lAssignable, lValue);
    }
  }
  
  @Override
  public String toString() {
    return mModel.toString();
  }
  
  @Override
  public int hashCode() {
    return mModel.hashCode() + 23094;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (!getClass().equals(pOther.getClass())) {
      return false;
    }
    
    MathsatModel lModel = (MathsatModel)pOther;
    
    return mModel.equals(lModel.mModel);
  }
  
}
