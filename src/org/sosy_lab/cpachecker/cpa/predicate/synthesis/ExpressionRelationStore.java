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
package org.sosy_lab.cpachecker.cpa.predicate.synthesis;

import static org.sosy_lab.cpachecker.cpa.predicate.synthesis.RelationUtils.includesOrGlobalVariable;

import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cpa.predicate.synthesis.CExpressionInliner.SubstitutionProvider;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

public class ExpressionRelationStore {

  public static enum BooleanRelation {
    EQUAL             (BinaryOperator.EQUALS),
    NOT_EQUAL         (BinaryOperator.NOT_EQUALS),
    LESS              (BinaryOperator.LESS_THAN),
    LESS_OR_EQUAL     (BinaryOperator.LESS_EQUAL),
    GREATER           (BinaryOperator.GREATER_THAN),
    GREATER_OR_EQUAL  (BinaryOperator.GREATER_EQUAL);

    public final BinaryOperator binaryOperator;

    private BooleanRelation(BinaryOperator pOp) {
      this.binaryOperator = pOp;
    }
  }

  private final Table<CExpression, CExpression, BooleanRelation> store;
  private final Map<BinaryOperator, BooleanRelation> operatorRelationMap;

  public ExpressionRelationStore() {

    this.store = HashBasedTable.create();  //TODO: Ensure deterministic behavior!!!
    this.operatorRelationMap = Maps.newTreeMap();

    for (BooleanRelation r: BooleanRelation.values()) {
      operatorRelationMap.put(r.binaryOperator, r);
    }
  }

  public Set<CExpression> getStoredExpressionsWith(Set<CIdExpression> ids) {
    Set<CExpression> result = Sets.newHashSet();
    for (CExpression e: store.columnKeySet()) {
      if (includesOrGlobalVariable(e, ids)) {
        result.add(e);
      }
    }
    for (CExpression e: store.rowKeySet()) {
      if (includesOrGlobalVariable(e, ids)) {
        result.add(e);
      }
    }
    return result;
  }

  public Pair<CExpression,Set<CIdExpression>> getInlined(CExpression pExpr, final Set<CIdExpression> notInline) {

    final Set<CIdExpression> remainingIds = Sets.newHashSet();
    final CExpressionInliner inliner = new CExpressionInliner(new SubstitutionProvider() {
      @Override
      public Optional<CExpression> getSubstitutionFor(CIdExpression pLhs) {

        if (notInline.contains(pLhs)) {
          return Optional.absent();
        }

        Map<CExpression, BooleanRelation> relations = store.row(pLhs);

        for (CExpression relTo : relations.keySet()) {
          BooleanRelation rel = relations.get(relTo);

          if (rel == BooleanRelation.EQUAL) {
            Pair<CExpression, Set<CIdExpression>> recursionResult = getInlined(relTo,
                ImmutableSet.<CIdExpression>builder().addAll(notInline).add(pLhs).build());
            remainingIds.addAll(recursionResult.getSecond());
            return Optional.of(recursionResult.getFirst());
          }
        }

        remainingIds.add(pLhs);

        return Optional.absent();
      }
    });

    Pair<Boolean, CExpression> inlined = pExpr.accept(inliner);
    return Pair.of(inlined.getSecond(), remainingIds);
  }

  public void addFactEquality(CExpression pLhs, CExpression pRhs) {
    putRelation(pLhs, pRhs, BooleanRelation.EQUAL);
  }

  public void addFact(CBinaryExpression pE) {

    BooleanRelation rel  = operatorRelationMap.get(pE.getOperator());
    if (rel == null) {
      return;
    }

    putRelation(pE.getOperand1(), pE.getOperand2(), rel);
  }

  private void putRelation(CExpression pLhs, CExpression pRhs, BooleanRelation pRel) {
    store.put(pLhs, pRhs, pRel);
  }

  public void addFact(CAssignment pA) {
    if (pA.getLeftHandSide() instanceof CIdExpression) {
      if (pA.getRightHandSide() instanceof CExpression) {
        putRelation(
            (CIdExpression)pA.getLeftHandSide(),
            (CExpression)pA.getRightHandSide(),
            BooleanRelation.EQUAL);
      }
      // TODO: Support other RHS
    }
    // TODO: Support other LHS
  }

  public Map<CExpression, BooleanRelation> getRelationTo(CExpression pId) {
    Map<CExpression, BooleanRelation> result = Maps.newHashMap();
    Map<CExpression, BooleanRelation> row = store.row(pId);
    Map<CExpression, BooleanRelation> col = store.column(pId);
    if (row != null) {
      result.putAll(row);
    }
    if (col != null) {
      result.putAll(col);
    }
    return result;
  }


}
