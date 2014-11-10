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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.predicate.synthesis.DefaultRelationStore.Relation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


public class RelationSynthesis {

  private final LogManager logger;
  private final RelationView relview;
  private final CBinaryExpressionBuilder binExprBuilder;

  public RelationSynthesis(LogManager pLogger, RelationView pRelView) {
    relview = pRelView;
    logger = pLogger;
    binExprBuilder = new CBinaryExpressionBuilder(MachineModel.LINUX64, logger); // TODO
  }

  public Collection<CBinaryExpression> getCombinedExpressionsOn(Set<CIdExpression> uninstanciatedIds, SSAMap pSsaMap) {

    Set<CIdExpression> idInstances = Sets.newHashSet();
    for (CIdExpression id: uninstanciatedIds) {
      idInstances.add(instanciate(id, pSsaMap, 0));
    }

    Collection<CBinaryExpression> result = Lists.newArrayList();

    for (CExpression lhs: relview.getStoredExpressionsWithOrGlobal(idInstances)) {
      // e is a expression that contains interface variables.

      // inline e, i.e., substitute all non-interface variables
      final Pair<CExpression, Set<CIdExpression>> lhsInliningResult = relview.getInlined(lhs, idInstances);
      final CExpression lhsInlined = lhsInliningResult.getFirst();
      final Set<CIdExpression> notInlinedIds = lhsInliningResult.getSecond();

      if (!notInlinedIds.isEmpty()) { // only if all substitutions were possible
        continue;
      }

      Map<CExpression, Relation> row = relview.getRelationTo(lhs);

      for (CExpression rhs: row.keySet()) {
        final Pair<CExpression, Set<CIdExpression>> rhsInliningResult = relview.getInlined(rhs, idInstances);
        final CExpression rhsInlined = rhsInliningResult.getFirst();
        final Set<CIdExpression> rhsNotInlinedIds = rhsInliningResult.getSecond();

        final Relation relation = row.get(rhs);

        if (!rhsNotInlinedIds.isEmpty()) { // only if all substitutions were possible
          continue;
        }

        CBinaryExpression bin = binExprBuilder.buildBinaryExpressionUnchecked(
            lhsInlined, rhsInlined, relation.binaryOperator);

        result.add(uninstanciate(bin));
      }
    }

    System.out.println(result);
    return result;
  }

}
