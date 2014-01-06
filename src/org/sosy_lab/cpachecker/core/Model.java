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
package org.sosy_lab.cpachecker.core;

import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.Model.AssignableTerm;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

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
  public Multimap<ARGState, Pair<AssignableTerm, Object>> getexactVariableValues(ARGPath pPath) {

    if (assignments.isEmpty()) {
      return null;
    }

    return assignments.getexactVariableValues(pPath, this);
  }

  @Nullable
  public List<Pair<CFAEdge, Collection<Pair<AssignableTerm, Object>>>> getExactVariableValuePath(List<CFAEdge> pPath) {

    if (assignments.isEmpty()) {
      return null;
    }

    return assignments.getexactVariableValues(pPath, this);
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

  /**
   * This class represents a path of cfaEdges, that contain the additional Information
   * at which edge which assignableTerm was created when this path was checked by
   * the class {@link PathChecker}.
   *
   */
  public static class CFAPathWithAssignments implements Iterable<Pair<CFAEdge, Set<AssignableTerm>>> {

    private final List<Pair<CFAEdge, Set<AssignableTerm>>> pathWithAssignments;
    private final Multimap<CFAEdge, AssignableTerm> assignableTerms;

    public CFAPathWithAssignments(List<Pair<CFAEdge, Set<AssignableTerm>>> pPathWithAssignments) {
      pathWithAssignments = ImmutableList.copyOf(pPathWithAssignments);
      Multimap<CFAEdge, AssignableTerm> multimap = HashMultimap.create();

      for (Pair<CFAEdge, Set<AssignableTerm>> pair : pathWithAssignments) {
        multimap.putAll(pair.getFirst(), pair.getSecond());
      }

      assignableTerms = ImmutableMultimap.copyOf(multimap);
    }

    private List<Pair<CFAEdge, Collection<Pair<AssignableTerm, Object>>>>
        getexactVariableValues(List<CFAEdge> pPath, Model pModel) {

      int index = pathWithAssignments.size() - pPath.size();

      if (index < 0) {
        return null;
      }

      List<Pair<CFAEdge, Collection<Pair<AssignableTerm, Object>>>> result;

      result = new ArrayList<>(pPath.size());

      for (CFAEdge edge : pPath) {

        if (index > pathWithAssignments.size()) { return null; }

        Pair<CFAEdge, Set<AssignableTerm>> cfaWithAssignment = pathWithAssignments.get(index);

        if (!cfaWithAssignment.getFirst().equals(edge)) {
          return null;
        }

        Collection<Pair<AssignableTerm, Object>> value = getValue(cfaWithAssignment.getSecond(), pModel);

        if (value == null) {
          // Assumption violated, return null
          return null;
        }

        result.add(Pair.of(edge, value));
        index++;
      }

      return result;
    }

    @Nullable
    private Collection<Pair<AssignableTerm, Object>> getValue(Set<AssignableTerm> pSetOfTerms, Model model) {

      Collection<Pair<AssignableTerm, Object>> result = new HashSet<>();

      for (AssignableTerm term : pSetOfTerms) {
        Object value = model.get(term);
        if (value == null) {
          // Assumption violated, return null
          return null;
        }
        result.add(Pair.of(term, value));
      }

      return result;
    }

    private Multimap<ARGState, Pair<AssignableTerm, Object>> getexactVariableValues(ARGPath pPath, Model model) {

      if (pPath.isEmpty() || pPath.size() != pathWithAssignments.size()) {
        return null;
      }

      Multimap<ARGState, Pair<AssignableTerm, Object>> result = HashMultimap.create();

      int index = 0;

      for (Pair<ARGState, CFAEdge> argPair : pPath) {

        Pair<CFAEdge, Set<AssignableTerm>> assignTermPair = pathWithAssignments.get(index);

        if (!assignTermPair.getFirst().equals(argPair.getSecond())) {
          // path is not equivalent
          return null;
        }

        Set<AssignableTerm> setOfTerms = assignTermPair.getSecond();

        Collection<Pair<AssignableTerm, Object>> value = getValue(setOfTerms, model);

        if (value == null) {
          // Assumption violated, return null
          return null;
        }

        result.putAll(argPair.getFirst(), value);
        index++;
      }

      return result;
    }

    public Collection<AssignableTerm> getAllAssignedTerms() {
      return assignableTerms.values();
    }

    public Collection<AssignableTerm> getAllAssignedTerms(CFAEdge edge) {
      return assignableTerms.get(edge);
    }

    public boolean isEmpty() {
      return pathWithAssignments.isEmpty();
    }

    public CFAPathWithAssignments() {
      pathWithAssignments = ImmutableList.of();
      assignableTerms = ImmutableMultimap.of();
    }

    @Override
    public String toString() {
      return pathWithAssignments.toString();
    }

    public CFAEdge getCFAEdgeAtPosition(int index) {
      return pathWithAssignments.get(index).getFirst();
    }

    @Override
    public Iterator<Pair<CFAEdge, Set<AssignableTerm>>> iterator() {
      return pathWithAssignments.iterator();
    }

    public int size() {
      return pathWithAssignments.size();
    }

    @Nullable
    public static String getAsCode(Collection<Pair<AssignableTerm, Object>> pAssumptionSet, CFAEdge pCfaEdge) {

      //TODO Implement Value processing (not just object.toString())

      if (pAssumptionSet.size() < 0) {
        return null;
      }

      if (pCfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
        return handleDeclaration(pAssumptionSet, ((ADeclarationEdge) pCfaEdge).getDeclaration());
      } else if (pCfaEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
        return handleStatement(((AStatementEdge) pCfaEdge).getStatement(), pAssumptionSet);
      } else if (pCfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
        //TODO Implement Function calls
        return null;
      } else if(pCfaEdge.getEdgeType() == CFAEdgeType.ReturnStatementEdge) {
        return handleReturnStatement(((AReturnStatementEdge)pCfaEdge).getExpression(), pAssumptionSet);
      }

      return null;
    }

    private static String handleReturnStatement(IAExpression pExpression,
        Collection<Pair<AssignableTerm, Object>> pAssumptionSet) {

      if ( pExpression != null && pAssumptionSet.size() == 1) {
        return "return " + getFirstValue(pAssumptionSet).toString();
      }

      return null;
    }

    private static String handleStatement(IAStatement pStatement,
        Collection<Pair<AssignableTerm, Object>> pAssumptionSet) {

      if (pStatement instanceof AFunctionCallAssignmentStatement) {
        return ((AFunctionCallAssignmentStatement) pStatement)
          .getLeftHandSide().toASTString() + " = "
          + getFirstValue(pAssumptionSet).toString()
          + ";";
      }

      if (pAssumptionSet.size() != 1) {
        return null;
      }

      if (pStatement instanceof AExpressionAssignmentStatement) {
        return ((AExpressionAssignmentStatement) pStatement)
          .getLeftHandSide().toASTString() + " = "
          + getFirstValue(pAssumptionSet).toString()
          + ";";
      }
      return null;
    }

    private static Object getFirstValue(Collection<Pair<AssignableTerm, Object>> pAssumptionSet) {
      return pAssumptionSet.iterator().next().getSecond().toString();
    }

    private static String handleDeclaration(Collection<Pair<AssignableTerm, Object>> pAssumptionSet,
        IADeclaration dcl) {

      if (pAssumptionSet.size() != 1 || !(dcl instanceof AVariableDeclaration)) {
        return null;
      } else {
        return dcl.getType().toASTString(dcl.getOrigName()) + " = "
            + getFirstValue(pAssumptionSet) + ";";
      }
    }
  }
}