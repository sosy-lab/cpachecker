package org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat;

import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Model;

import com.google.common.collect.ImmutableMap;

public class MathsatModel implements Model {

  public enum MathsatType {
    Boolean,
    Uninterpreted,
    Integer,
    Real,
    Bitvector;
    
    private static MathsatType toMathsatType(int pTypeId) {
      
      switch (pTypeId) {
      case mathsat.api.MSAT_BOOL:
        return Boolean;
      case mathsat.api.MSAT_U:
        return Uninterpreted;
      case mathsat.api.MSAT_INT:
        return Integer;
      case mathsat.api.MSAT_REAL:
        return Real;
      case mathsat.api.MSAT_BV:
        return Bitvector;
      }
      
      throw new IllegalArgumentException("Given parameter is not a mathsat type!");
    }
    
  }
  
  public interface MathsatAssignable {
   
    public MathsatType getType();
    public String getName();
    
  }
  
  public static class MathsatVariable implements MathsatAssignable {
    
    private final String mName;
    private final MathsatType mType;
    
    private MathsatVariable(String pName, MathsatType pType) {
      mName = pName;
      mType = pType;
    }
    
    @Override
    public String getName() {
      return mName;
    }
    
    @Override
    public MathsatType getType() {
      return mType;
    }
    
    @Override
    public String toString() {
      return mName + " : " + mType;
    }
    
    private static MathsatVariable toVariable(long pVariableId) {
      if (mathsat.api.msat_term_is_variable(pVariableId) == 0) {
        throw new IllegalArgumentException("Given mathsat id doesn't correspond to a variable! (" + mathsat.api.msat_term_repr(pVariableId) + ")");
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
    
    private final String mName;
    private final MathsatType mReturnType;
    private final MathsatValue[] mArguments;
    
    private int mHashCode;
    
    private MathsatFunction(String pName, MathsatType pReturnType, MathsatValue[] pArguments) {
      mName = pName;
      mReturnType = pReturnType;
      mArguments = pArguments;
      
      mHashCode = 32453 + mName.hashCode() + mReturnType.hashCode();
      
      for (MathsatValue lValue : mArguments) {
        mHashCode += lValue.hashCode();
      }
    }
    
    @Override
    public String getName() {
      return mName;
    }
    
    @Override
    public MathsatType getType() {
      return mReturnType;
    }
    
    public int getArity() {
      return mArguments.length;
    }
    
    public MathsatValue getArgument(int lArgumentIndex) {
      return mArguments[lArgumentIndex];
    }
    
    @Override
    public String toString() {
      String lArguments = "";
      
      boolean lIsFirst = true;
      
      for (MathsatValue lValue : mArguments) {
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
    
    private static MathsatFunction toFunction(long pFunctionId) {
      if (mathsat.api.msat_term_is_variable(pFunctionId) != 0) {
        throw new IllegalArgumentException("Given mathsat id is a variable! (" + mathsat.api.msat_term_repr(pFunctionId) + ")");
      }
      
      long lDeclarationId = mathsat.api.msat_term_get_decl(pFunctionId);
      String lName = mathsat.api.msat_decl_get_name(lDeclarationId);
      MathsatType lType = MathsatType.toMathsatType(mathsat.api.msat_decl_get_return_type(lDeclarationId));
      
      int lArity = mathsat.api.msat_decl_get_arity(lDeclarationId);
      
      // TODO we assume only constants (reals) as parameters for now
      MathsatValue[] lArguments = new MathsatValue[lArity];
      
      for (int lArgumentIndex = 0; lArgumentIndex < lArity; lArgumentIndex++) {
        long lArgument = mathsat.api.msat_term_get_arg(pFunctionId, lArgumentIndex);
        
        String lTermRepresentation = mathsat.api.msat_term_repr(lArgument);
        
        MathsatValue lValue;
        
        try {
          lValue = new MathsatRealValue(Double.valueOf(lTermRepresentation));
        }
        catch (NumberFormatException e) {
          // lets try special case for mathsat
          String[] lNumbers = lTermRepresentation.split("/");
          
          if (lNumbers.length != 2) {
            throw new RuntimeException("I do not understand this format!");
          }
          
          double lNumerator = Double.valueOf(lNumbers[0]);
          double lDenominator = Double.valueOf(lNumbers[1]);
          
          lValue = new MathsatRealValue(lNumerator/lDenominator); 
        }
        
        lArguments[lArgumentIndex] = lValue;
      }
      
      return new MathsatFunction(lName, lType, lArguments);
    }
    
  }
  
  public interface MathsatValue {
    
  }
  
  public static class MathsatBooleanValue implements MathsatValue {
    
    private final boolean mValue;
    
    private MathsatBooleanValue(boolean pValue) {
      mValue = pValue;
    }
    
    public boolean isTrue() {
      return mValue;
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
      
      MathsatBooleanValue lValue = (MathsatBooleanValue)pOther;
      
      return (lValue.mValue == mValue);
    }
    
    @Override
    public int hashCode() {
      return 23421 + (mValue?0:1);
    }
    
    @Override
    public String toString() {
      return "" + mValue;
    }
    
  }
  
  public static class MathsatRealValue implements MathsatValue {
    
    private final double mValue;
    
    private MathsatRealValue(double pValue) {
      mValue = pValue;
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
      
      MathsatRealValue lValue = (MathsatRealValue)pOther;
      
      return (lValue.mValue == mValue);
    }
    
    @Override
    public int hashCode() {
      return 234820 + (int)mValue;
    }
    
    @Override
    public String toString() {
      return "" + mValue;
    }
    
  }
  
  public static class MathsatIntegerValue implements MathsatValue {
    
    private final long mValue;
    
    private MathsatIntegerValue(long pValue) {
      mValue = pValue;
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
      
      MathsatIntegerValue lValue = (MathsatIntegerValue)pOther;
      
      return (lValue.mValue == mValue);
    }
    
    @Override
    public int hashCode() {
      return 234345 + (int)mValue;
    }
    
    @Override
    public String toString() {
      return "" + mValue;
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
  
  private final Map<MathsatAssignable, MathsatValue> mModel;
  
  MathsatModel(long lMathsatEnvironmentID) {
    ImmutableMap.Builder<MathsatAssignable, MathsatValue> model
        = ImmutableMap.builder();
    
    long lModelIterator = mathsat.api.msat_create_model_iterator(lMathsatEnvironmentID);
      
    if (mathsat.api.MSAT_ERROR_MODEL_ITERATOR(lModelIterator)) {
      throw new RuntimeException("Erroneous model iterator! (" + lModelIterator + ")");
    }
    
    while (mathsat.api.msat_model_iterator_has_next(lModelIterator) != 0) {
      long[] lModelElement = mathsat.api.msat_model_iterator_next(lModelIterator);
      
      long lKeyTerm = lModelElement[0];
      long lValueTerm = lModelElement[1];
      
      MathsatAssignable lAssignable = toAssignable(lKeyTerm);
      
      // TODO maybe we have to convert to SMTLIB format and then read in values in a controlled way, e.g., size of bitvector
      // TODO we are assuming numbers as values
      if (!(mathsat.api.msat_term_is_number(lValueTerm) != 0
            || mathsat.api.msat_term_is_boolean_var(lValueTerm) != 0)) {
        throw new IllegalArgumentException("Mathsat term is not a number!");
      }
      
      String lTermRepresentation = mathsat.api.msat_term_repr(lValueTerm);
      
      MathsatValue lValue;
      
      switch (lAssignable.getType()) {
      case Boolean:
        lValue = new MathsatBooleanValue(Boolean.valueOf(lTermRepresentation));
        break;
      case Real:
        try {
          lValue = new MathsatRealValue(Double.valueOf(lTermRepresentation));
        }
        catch (NumberFormatException e) {
          // lets try special case for mathsat
          String[] lNumbers = lTermRepresentation.split("/");
          
          if (lNumbers.length != 2) {
            throw new RuntimeException("I do not understand this format!");
          }
          
          double lNumerator = Double.valueOf(lNumbers[0]);
          double lDenominator = Double.valueOf(lNumbers[1]);
          
          lValue = new MathsatRealValue(lNumerator/lDenominator); 
        }
        
        break;
      case Integer:
        lValue = new MathsatIntegerValue(Long.valueOf(lTermRepresentation));
        break;
      default:
        throw new RuntimeException("I don't understand this!");
      }
      
      model.put(lAssignable, lValue);
    }
    
    mathsat.api.msat_destroy_model_iterator(lModelIterator);
    mModel = model.build();
  }
  
  public Set<MathsatAssignable> getAssignables() {
    return mModel.keySet();
  }
  
  public MathsatValue getValue(MathsatAssignable pAssignable) {
    return mModel.get(pAssignable);
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
