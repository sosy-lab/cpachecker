/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.hybrid.visitor;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.HybridValueTransformer;
import org.sosy_lab.cpachecker.cpa.hybrid.exception.InvalidAssumptionException;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

/**
 * A hybrid value transformer for declaration cases
 */
public class HybridValueDeclarationTransformer
    extends HybridValueTransformer<CBinaryExpression, CDeclaration> {

  private final HybridValueTransformer<CBinaryExpression, CIdExpression> idExpressionTransformer;

  public HybridValueDeclarationTransformer(
      MachineModel pMachineModel,
      LogManager pLogger) {
    super(pMachineModel, pLogger);

    this.idExpressionTransformer = new HybridValueIdExpressionTransformer(pMachineModel, pLogger);
  }

  @Override
  public CBinaryExpression transform(Value pValue, CDeclaration pCDeclaration, BinaryOperator pOperator)
    throws InvalidAssumptionException{
    
    // we just build a CIdExpression and pass it to the internal transformer object
    CIdExpression variableExpression = new CIdExpression(
      FileLocation.DUMMY,
      pCDeclaration.getType(),
      pCDeclaration.getQualifiedName(), // check, if we need the qualified name or ::getName()
      pCDeclaration);
      
    return idExpressionTransformer.transform(pValue, variableExpression, pOperator);
  }

}
