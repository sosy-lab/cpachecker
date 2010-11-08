/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.symbpredabstraction;

import java.util.Map;

import org.sosy_lab.cpachecker.util.symbpredabstraction.Model.AssignableTerm;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

public class Model extends ForwardingMap<AssignableTerm, Object> {

  public static enum TermType {
    Boolean,
    Uninterpreted,
    Integer,
    Real,
    Bitvector;
  }
  
  public static interface AssignableTerm {
    
    public TermType getType();
    public String getName();
    
  }
 
  public static class Variable implements AssignableTerm {
    
    private final String mName;
    private final TermType mType;
    
    public Variable(String pName, TermType pType) {
      mName = pName;
      mType = pType;
    }
    
    @Override
    public String getName() {
      return mName;
    }
    
    @Override
    public TermType getType() {
      return mType;
    }
    
    @Override
    public String toString() {
      return mName + " : " + mType;
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
      
      Variable lVariable = (Variable)pOther;
      
      return mName.equals(lVariable.mName) && mType.equals(lVariable.mType);
    }
  }
  
  public static class Function implements AssignableTerm {
    
    private final String mName;
    private final TermType mReturnType;
    private final Object[] mArguments;
    
    private int mHashCode;
    
    public Function(String pName, TermType pReturnType, Object[] pArguments) {
      mName = pName;
      mReturnType = pReturnType;
      mArguments = pArguments;
      
      mHashCode = 32453 + mName.hashCode() + mReturnType.hashCode();
      
      for (Object lValue : mArguments) {
        mHashCode += lValue.hashCode();
      }
    }
    
    @Override
    public String getName() {
      return mName;
    }
    
    @Override
    public TermType getType() {
      return mReturnType;
    }
    
    public int getArity() {
      return mArguments.length;
    }
    
    public Object getArgument(int lArgumentIndex) {
      return mArguments[lArgumentIndex];
    }
    
    @Override
    public String toString() {
      String lArguments = "";
      
      boolean lIsFirst = true;
      
      for (Object lValue : mArguments) {
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
      
      Function lFunction = (Function)pOther;
      
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
  }
  
  private final Map<AssignableTerm, Object> mModel;
  
  @Override
  protected Map<AssignableTerm, Object> delegate() {
    return mModel;
  }
  
  public Model() {
    mModel = ImmutableMap.of();
  }
  
  public Model(Map<AssignableTerm, Object> content) {
    mModel = ImmutableMap.copyOf(content);
  }
  
  private static final MapJoiner joiner = Joiner.on('\n').withKeyValueSeparator(": ");
  
  @Override
  public String toString() {
    return joiner.join(mModel);
  }
}