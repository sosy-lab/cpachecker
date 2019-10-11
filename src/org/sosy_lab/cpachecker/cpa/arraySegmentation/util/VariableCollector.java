/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.arraySegmentation.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class VariableCollector {


  CBinaryExpressionBuilder builder;

  public VariableCollector(MachineModel pMachineModel, LogManager pLogger) {
    super();
    builder = new CBinaryExpressionBuilder(pMachineModel, pLogger);
  }

  public Collection<CIdExpression> collectVariables(CExpression pExpr) {
    return collectVars(pExpr).stream().map(v -> (CIdExpression) v).collect(Collectors.toList());
  }

  public Collection<JIdExpression> collectVariables(JExpression pExpr) {
    return collectVars(pExpr).stream().map(v -> (JIdExpression) v).collect(Collectors.toList());
  }

  private Collection<AIdExpression> collectVars(AExpression pExpr) {
    Collection<AIdExpression> vars = new HashSet<>();
    if (pExpr instanceof ABinaryExpression) {
      vars.addAll(collectVars(((ABinaryExpression) pExpr).getOperand1()));
      vars.addAll(collectVars(((ABinaryExpression) pExpr).getOperand2()));
    } else if (pExpr instanceof AUnaryExpression) {
      vars.addAll(collectVars(((AUnaryExpression) pExpr).getOperand()));

    } else if (pExpr instanceof AIdExpression) {
      vars.add((AIdExpression) pExpr);
    }
    return vars;
  }

  public CExpression
      replaceVarsInExpr(CExpression pExpr, Map<CIdExpression, CExpression> replacements)
          throws UnrecognizedCodeException {
    if (pExpr instanceof ABinaryExpression) {
      CBinaryExpression bin = (CBinaryExpression) pExpr;
      CExpression lhs = replaceVarsInExpr(bin.getOperand1(), replacements);
      CExpression rhs = replaceVarsInExpr(bin.getOperand2(), replacements);
      return builder.buildBinaryExpression(lhs, rhs, bin.getOperator());
    } else if (pExpr instanceof AUnaryExpression) {
      // TODO: Implement this
    } else if (pExpr instanceof AIdExpression) {
      if (replacements.containsKey(pExpr)) {
        return replacements.get(pExpr);
      }
    }
    return pExpr;
  }

}
