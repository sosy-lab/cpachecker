/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core;

import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.Model.AssignableTerm;
import org.sosy_lab.cpachecker.core.concrete_counterexample.CFAEdgeWithAssignments;
import org.sosy_lab.cpachecker.core.concrete_counterexample.CFAPathWithAssignments;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;

/**
 * This class represents an assignment of values to program variables
 * along a path. Each variable can have several assignments with different
 * SSA indices if it gets re-assigned along the path.
 *
 * The value of each variable can be an arbitrary object, but usually,
 * this is a {@link Number}.
 */
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

  public static class Constant implements AssignableTerm {

    public Constant(final String name, final TermType type) {
      this.name = name;
      this.type = type;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public TermType getType() {
      return type;
    }

    @Override
    public String toString() {
      return name + " : " + type;
    }

    @Override
    public int hashCode() {
      return 324 + name.hashCode() + type.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
      if (this == other) {
        return true;
      }

      if (other == null) {
        return false;
      }

      if (!getClass().equals(other.getClass())) {
        return false;
      }

      final Constant otherConstant = (Constant) other;

      return name.equals(otherConstant.name)
          && type.equals(otherConstant.type);
    }

    protected final String name;
    protected final TermType type;
  }

  public static class Variable extends Constant implements AssignableTerm {

    public Variable(final String name, final int ssaIndex, final TermType type) {
      super(name, type);
      this.ssaIndex = ssaIndex;
    }

    public int getSSAIndex() {
      return ssaIndex;
    }

    @Override
    public String toString() {
      return name + "@" + ssaIndex + " : " + type;
    }

    @Override
    public int hashCode() {
      return 324 + name.hashCode() + ssaIndex + type.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
      if (this == other) {
        return true;
      }

      if (other == null) {
        return false;
      }

      if (!getClass().equals(other.getClass())) {
        return false;
      }

      Variable otherVariable = (Variable) other;

      return name.equals(otherVariable.name)
          && (ssaIndex == otherVariable.ssaIndex)
          && type.equals(otherVariable.type);
    }

    private final int ssaIndex;
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
  private final CFAPathWithAssignments assignments;

  @Override
  protected Map<AssignableTerm, Object> delegate() {
    return mModel;
  }

  public static Model empty() {
    return new Model();
  }

  private Model() {
    mModel = ImmutableMap.of();
    assignments = new CFAPathWithAssignments();
  }

  public Model(Map<AssignableTerm, Object> content) {
    mModel = ImmutableMap.copyOf(content);
    assignments = new CFAPathWithAssignments();
  }

  public Model(Map<AssignableTerm, Object> content, CFAPathWithAssignments pAssignments) {
    mModel = ImmutableMap.copyOf(content);
    assignments = pAssignments;

    checkArgument(mModel.keySet().containsAll(
        assignments.getAllAssignedTerms()));
  }

  /**
   * Return a new model that is equal to the current one,
   * but additionally has information about when each variable was assigned.
   * @see Model#getAssignedTermsPerEdge()
   */
  public Model withAssignmentInformation(CFAPathWithAssignments pAssignments) {
    checkState(assignments.isEmpty());
    return new Model(mModel, pAssignments);
  }

  /**
   * Return a path that indicates which terms where assigned at which edge.
   * Note that it is not guaranteed that this is information is present for
   * all terms, thus <code>this.getAssignedTermsPerEdge().getAllAssignedTerms()</code> may
   * be smaller than <code>this.keySet()</code> (but not larger).
   */
  public CFAPathWithAssignments getAssignedTermsPerEdge() {
    return assignments;
  }

  @Nullable
  public Map<ARGState, CFAEdgeWithAssignments> getExactVariableValues(ARGPath pPath) {

    if (assignments.isEmpty()) {
      return null;
    }

    return assignments.getExactVariableValues(pPath);
  }

  @Nullable
  public CFAPathWithAssignments getExactVariableValuePath(List<CFAEdge> pPath) {

    if (assignments.isEmpty()) {
      return null;
    }

    return assignments.getExactVariableValues(pPath);
  }

  private static final MapJoiner joiner = Joiner.on(System.lineSeparator()).withKeyValueSeparator(": ");

  @Override
  public void appendTo(Appendable output) throws IOException {
    Map<AssignableTerm, Object> sorted = ImmutableSortedMap.copyOf(mModel,
        Ordering.usingToString());
    joiner.appendTo(output, sorted);
  }

  @Override
  public String toString() {
    return Appenders.toString(this);
  }
}