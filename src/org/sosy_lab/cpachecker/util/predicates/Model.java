/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.cpachecker.util.predicates.Model.AssignableTerm;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class Model extends ForwardingMap<AssignableTerm, Object> implements Appender {

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
    private final int mSSAIndex;
    private final TermType mType;

    public Variable(String pName, int pSSAIndex, TermType pType) {
      mName = pName;
      mSSAIndex = pSSAIndex;
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

    public int getSSAIndex() {
      return mSSAIndex;
    }

    @Override
    public String toString() {
      return mName + "@" + mSSAIndex + " : " + mType;
    }

    @Override
    public int hashCode() {
      return 324 + mName.hashCode() + mSSAIndex + mType.hashCode();
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

      return mName.equals(lVariable.mName)
          && (mSSAIndex == lVariable.mSSAIndex)
          && mType.equals(lVariable.mType);
    }
  }

  public static class Function implements AssignableTerm {

    private final String mName;
    private final TermType mReturnType;
    private final List<Object> mArguments;

    private int mHashCode;

    public Function(String pName, TermType pReturnType, Object[] pArguments) {
      mName = pName;
      mReturnType = pReturnType;
      mArguments = ImmutableList.copyOf(pArguments);

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
      return mArguments.size();
    }

    public Object getArgument(int lArgumentIndex) {
      return mArguments.get(lArgumentIndex);
    }

    @Override
    public String toString() {
      return mName + "(" + Joiner.on(',').join(mArguments) + ") : " + mReturnType;
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

      return (lFunction.mName.equals(mName)
          && lFunction.mReturnType.equals(mReturnType)
          && lFunction.mArguments.equals(mArguments));
    }
  }

  private final Map<AssignableTerm, Object> mModel;

  @Override
  protected Map<AssignableTerm, Object> delegate() {
    return mModel;
  }

  public static Model empty() {
    return new Model();
  }

  private Model() {
    mModel = ImmutableMap.of();
  }

  public Model(Map<AssignableTerm, Object> content) {
    mModel = ImmutableMap.copyOf(content);
  }

  private static final MapJoiner joiner = Joiner.on('\n').withKeyValueSeparator(": ");

  @Override
  public void appendTo(Appendable output) throws IOException {
    joiner.appendTo(output, mModel);
  }

  @Override
  public String toString() {
    return Appenders.toString(this);
  }
}