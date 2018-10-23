/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


public class ValueInferenceObject implements InferenceObject {

  private final ValueAnalysisState source;
  private final ValueAnalysisInformation diff;
  private final Collection<CAssignment> statements;

  private ValueInferenceObject(
      ValueAnalysisState src,
      ValueAnalysisInformation pDst,
      Collection<CAssignment> pS) {
    Preconditions.checkNotNull(src);
    Preconditions.checkNotNull(pDst);
    source = src;
    diff = pDst;
    statements = ImmutableSet.copyOf(pS);
  }

  public static InferenceObject
      create(ValueAnalysisState src, ValueAnalysisState dst, CFAEdge pEdge) {
    if (pEdge instanceof CDeclarationEdge) {
      return EmptyInferenceObject.getInstance();
    }
    ValueAnalysisState newSrc = ValueAnalysisState.copyOf(src);
    PersistentMap<MemoryLocation, Value> values = PathCopyingPersistentTreeMap.of();
    PersistentMap<MemoryLocation, Type> types = PathCopyingPersistentTreeMap.of();

    Set<MemoryLocation> allMems = newSrc.getTrackedMemoryLocations();

    for (MemoryLocation mem : allMems) {
      if (mem.isOnFunctionStack()) {
        newSrc.forget(mem);
      }
    }

    allMems = dst.getTrackedMemoryLocations();

    for (MemoryLocation mem : allMems) {
      if (mem.isOnFunctionStack()) {

      } else if (newSrc.contains(mem)) {
        Value oldVal = newSrc.getValueFor(mem);
        Value newVal = dst.getValueFor(mem);
        if (!oldVal.equals(newVal)) {
          values = values.putAndCopy(mem, newVal);
          types = types.putAndCopy(mem, newSrc.getTypeForMemoryLocation(mem));
        }
      } else {
        Value newVal = dst.getValueFor(mem);
        values = values.putAndCopy(mem, newVal);
        types = types.putAndCopy(mem, dst.getTypeForMemoryLocation(mem));
      }
    }

    for (MemoryLocation mem : newSrc.getTrackedMemoryLocations()) {
      if (!dst.contains(mem)) {
        values = values.putAndCopy(mem, UnknownValue.getInstance());
      }
    }

    if (values.size() == 0) {
      return EmptyInferenceObject.getInstance();
    } else {
      CAssignment asgn;
      if (pEdge instanceof CStatementEdge) {
        CStatement stmnt = ((CStatementEdge) pEdge).getStatement();
        assert stmnt instanceof CAssignment;
        asgn = (CAssignment) stmnt;

      } else {
        assert pEdge instanceof CAssumeEdge;
        CExpression expr = ((CAssumeEdge)pEdge).getExpression();
        assert expr instanceof CBinaryExpression;
        CBinaryExpression bexpr = (CBinaryExpression) expr;
        asgn = new CExpressionAssignmentStatement(expr.getFileLocation(), (CLeftHandSide) bexpr.getOperand1(), bexpr.getOperand2());
      }
      ValueAnalysisInformation info =
          new ValueAnalysisInformation(values, types);
      return new ValueInferenceObject(newSrc, info, Collections.singleton(asgn));
    }
  }

  public ValueInferenceObject merge(ValueInferenceObject pObject) {
    ValueAnalysisState src1 = getSource();
    ValueAnalysisState src2 = pObject.getSource();

    ValueAnalysisInformation diff1 = getDifference();
    ValueAnalysisInformation diff2 = pObject.getDifference();

    Map<MemoryLocation, Value> values1 = diff1.getAssignments();
    Map<MemoryLocation, Value> values2 = diff2.getAssignments();
    Map<MemoryLocation, Type> types1 = diff1.getLocationTypes();
    Map<MemoryLocation, Type> types2 = diff2.getLocationTypes();

    PersistentMap<MemoryLocation, Value> newValues = PathCopyingPersistentTreeMap.of();
    PersistentMap<MemoryLocation, Type> newTypes = PathCopyingPersistentTreeMap.of();

    Set<MemoryLocation> jointMems = Sets.union(values1.keySet(), values2.keySet());

    for (MemoryLocation mem : jointMems) {
      if (values1.containsKey(mem) && !values2.containsKey(mem)) {
        newValues = newValues.putAndCopy(mem, values1.get(mem));
        newTypes = newTypes.putAndCopy(mem, types1.get(mem));
      } else if (values2.containsKey(mem) && !values1.containsKey(mem)) {
        newValues = newValues.putAndCopy(mem, values2.get(mem));
        newTypes = newTypes.putAndCopy(mem, types2.get(mem));
      } else if (values1.containsKey(mem) && values2.containsKey(mem)) {
        Value value1 = values1.get(mem);
        Value value2 = values2.get(mem);
        Value newValue;

        if (value1.equals(value2)) {
          newValue = value1;
        } else {
          newValue = UnknownValue.getInstance();
        }

        newValues = newValues.putAndCopy(mem, newValue);

        Type type1 = types1.get(mem);
        Type type2 = types2.get(mem);
        Type newType;
        if (type1 == null && type2 != null) {
          //Undef value
          newType = type2;
        } else if (type1 != null && type2 == null) {
          newType = type1;
        } else if (type1 != null && type2 != null && type1.equals(type2)) {
          newType = type1;
        } else {
          newType = null;
          assert false;
        }
        newTypes = newTypes.putAndCopy(mem, newType);
      } else {
        assert false;
      }
    }

    ValueAnalysisState newSrc = src1.join(src2);

    if (newValues.equals(values1) && newSrc.equals(src1)) {
      return this;
    } else if (newValues.equals(values2) && newSrc.equals(src2)) {
      return pObject;
    } else {
      Set<CAssignment> s = Sets.newHashSet(this.getStatements());
      s.addAll(pObject.getStatements());
      ValueAnalysisInformation newDiff = new ValueAnalysisInformation(newValues, newTypes);
      return new ValueInferenceObject(newSrc, newDiff, s);
    }
  }

  @Override
  public ValueInferenceObject clone() {
    return new ValueInferenceObject(source, diff, statements);
  }

  public ValueAnalysisInformation getDifference() {
    return diff;
  }

  public ValueAnalysisState getSource() {
    return source;
  }

  public boolean compatibleWith(ValueAnalysisState state) {
    return source.compatibleWith(state);
  }

  @Override
  public boolean hasEmptyAction() {
    assert diff.getAssignments().size() > 0;
    return false;
  }

  public Collection<CAssignment> getStatements() {
    return statements;
  }
}
