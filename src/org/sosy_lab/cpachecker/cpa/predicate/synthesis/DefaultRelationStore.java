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

import static org.sosy_lab.cpachecker.cpa.predicate.synthesis.RelationUtils.*;

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializers;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.predicate.synthesis.CExpressionInliner.SubstitutionProvider;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

@Options
public class DefaultRelationStore implements RelationStore, RelationView {

  public static enum RelationMode {
    BACKWARDS_SEQUENCE,
    FORWARDS_SEQUENCE,
    RANDONM;
  }

  public static enum Relation {
    EQUAL             (BinaryOperator.EQUALS),
    NOT_EQUAL         (BinaryOperator.NOT_EQUALS),
    LESS              (BinaryOperator.LESS_THAN),
    LESS_OR_EQUAL     (BinaryOperator.LESS_EQUAL),
    GREATER           (BinaryOperator.GREATER_THAN),
    GREATER_OR_EQUAL  (BinaryOperator.GREATER_EQUAL);

    public final BinaryOperator binaryOperator;

    private Relation(BinaryOperator pOp) {
      this.binaryOperator = pOp;
    }
  }

  private final LogManager log;

  private final Table<CExpression, CExpression, Relation> store;
  private final Map<BinaryOperator, Relation> operatorRelationMap;
  private final AnalysisDirection direction;
  private boolean instanciateVars = true;

  public DefaultRelationStore(Configuration pConfig, LogManager pLogger, CFA pCfa, AnalysisDirection pDirection) throws InvalidConfigurationException {
    pConfig.inject(this);

    this.log = pLogger;

    this.store = HashBasedTable.create();
    this.operatorRelationMap = Maps.newTreeMap(); //TODO: Ensure deterministic behavior!!!
    this.direction = pDirection;

    for (Relation r: Relation.values()) {
      operatorRelationMap.put(r.binaryOperator, r);
    }
  }

  @Override
  public Set<CExpression> getStoredExpressionsWith(Set<CIdExpression> ids) {
    Set<CExpression> result = Sets.newHashSet();
    for (CExpression e: store.columnKeySet()) {
      if (includes(e, ids)) {
        result.add(e);
      }
    }
    for (CExpression e: store.rowKeySet()) {
      if (includes(e, ids)) {
        result.add(e);
      }
    }
    return result;
  }

  /**
   * No special treatment regarding SSAs ... CExpressions are handled as they are (we do not remove SSA indices here)!
   * Result pair:
   *    First element: Inlined expression
   *    Second element: Remaining IdExpressions (inlining was not possible for them)
   */
  @Override
  public Pair<CExpression,Set<CIdExpression>> getInlined(CExpression pExpr, final Set<CIdExpression> notInline) {

    final Set<CIdExpression> remainingIds = Sets.newHashSet();
    final CExpressionInliner inliner = new CExpressionInliner(new SubstitutionProvider() {
      @Override
      public Optional<CExpression> getSubstitutionFor(CIdExpression pLhs) {

        if (notInline.contains(pLhs)) {
          return Optional.absent();
        }

        Map<CExpression, Relation> relations = store.row(pLhs);

        for (CExpression relTo : relations.keySet()) {
          Relation rel = relations.get(relTo);

          if (rel == Relation.EQUAL) {
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

  public void addFact(CBinaryExpression pE, SSAMap pSsaMap, int lhsSsaDelta) {

    Relation rel  = operatorRelationMap.get(pE.getOperator());
    if (rel == null) {
      return;
    }

    // TODO: Contradiction on branches (positive case and negative case!!

    if (instanciateVars && pSsaMap != null) {
      CExpression lhs = instanciate(pE.getOperand1(), pSsaMap, lhsSsaDelta);
      CExpression rhs = instanciate(pE.getOperand2(), pSsaMap, 0);
      putRelation(lhs, rhs, rel);
    } else {
      putRelation(pE.getOperand1(), pE.getOperand2(), rel);
    }
  }

  private void putRelation(CExpression pLhs, CExpression pRhs, Relation pRel) {
    log.log(Level.ALL, "Adding expression to relation store: ", pLhs, pRel, pRhs);

    store.put(pLhs, pRhs, pRel);
  }

  public void addFact(CAssignment pA, SSAMap pSsaMap) {
    if (instanciateVars && pSsaMap != null) {
      pA = instanciateAssign(pA, pSsaMap, direction);
    }

    if (pA.getLeftHandSide() instanceof CIdExpression) {
      if (pA.getRightHandSide() instanceof CExpression) {
        putRelation(
            (CIdExpression)pA.getLeftHandSide(),
            (CExpression)pA.getRightHandSide(),
            Relation.EQUAL);
      }
      // TODO: Support other RHS
    }
    // TODO: Support other LHS
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.cpa.predicate.synthesis.OperationRelationStore#addConjunctiveFact(org.sosy_lab.cpachecker.cfa.model.CFAEdge, org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap)
   */
  @Override
  public void addFact(CFAEdge pEdge, SSAMap pSsaMap) throws UnrecognizedCCodeException {

    if (pEdge instanceof CAssumeEdge) {

      if (((CAssumeEdge) pEdge).getExpression() instanceof CBinaryExpression) {
        CBinaryExpression bin = (CBinaryExpression)((CAssumeEdge) pEdge).getExpression();
        addFact(bin, pSsaMap, 0);
      }

    } else if (pEdge instanceof CFunctionCallEdge) {

    } else if (pEdge instanceof CStatementEdge) {

      if (((CStatementEdge) pEdge).getStatement() instanceof CAssignment) {
        CAssignment assign = (CAssignment) ((CStatementEdge) pEdge).getStatement();
        addFact(assign, pSsaMap);
      }

    } else if (pEdge instanceof CDeclarationEdge) {

      CDeclarationEdge declEdge = (CDeclarationEdge) pEdge;
      CDeclaration decl = declEdge.getDeclaration();

      if (decl instanceof CVariableDeclaration) {
        CVariableDeclaration varDecl = (CVariableDeclaration) decl;

        for (CAssignment assignment : CInitializers.convertToAssignments(varDecl, declEdge)) {
          addFact(assignment, pSsaMap);
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.cpa.predicate.synthesis.OperationRelationStore#getRelationTo(org.sosy_lab.cpachecker.cfa.ast.c.CExpression)
   */
  @Override
  public Map<CExpression, Relation> getRelationTo(CExpression pId) {
    Map<CExpression, Relation> result = Maps.newHashMap();
    Map<CExpression, Relation> row = store.row(pId);
    Map<CExpression, Relation> col = store.column(pId);
    if (row != null) {
      result.putAll(row);
    }
    if (col != null) {
      result.putAll(col);
    }
    return result;
  }


}
