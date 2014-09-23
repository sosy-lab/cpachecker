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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.predicate.synthesis.DefaultRelationStore.Relation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


public class RelationSynthesis {

  private final RelationStore relstore;
  private final LogManager logger;

  public RelationSynthesis(LogManager pLogger, RelationStore pRelstore) {
    relstore = pRelstore;
    logger = pLogger;
  }

  public Collection<CBinaryExpression> getCombinedExpressionsOn(Set<CIdExpression> pIdExprs, SSAMap pSsaMap) {
    Set<CIdExpression> instanciatedIds = Sets.newHashSet();
    for (CIdExpression id: pIdExprs) {
      instanciatedIds.add(instanciate(id, pSsaMap, 0));
    }
    return getCombinedExpressionsOnInstances(instanciatedIds);
  }

  public Collection<CBinaryExpression> getCombinedExpressionsOnInstances(Set<CIdExpression> pIdExprs) {

    Collection<CBinaryExpression> result = Lists.newArrayList();

    for (CExpression lhs: relstore.getStoredExpressionsWith(pIdExprs)) {
      Pair<CExpression, Set<CIdExpression>> inlinedLhs = relstore.getInlined(lhs, pIdExprs);
      Map<CExpression, Relation> row = relstore.getRelationTo(lhs);

      if (inlinedLhs.getSecond().size() > 0) { // only if all substitutions were possible
        continue;
      }

      for (CExpression rhs: row.keySet()) {
        Pair<CExpression, Set<CIdExpression>> inlinedRhs = relstore.getInlined(rhs, pIdExprs);
        Relation rel = row.get(rhs);

        if (inlinedRhs.getSecond().size() > 0) { // only if all substitutions were possible
          continue;
        }

        CBinaryExpressionBuilder b = new CBinaryExpressionBuilder(MachineModel.LINUX64, logger); // TODO
        CBinaryExpression bin = b.buildBinaryExpression(
            inlinedLhs.getFirst(),
            inlinedRhs.getFirst(),
            rel.binaryOperator);

        result.add(uninstanciate(bin));
      }
    }

    return result;
  }



}
