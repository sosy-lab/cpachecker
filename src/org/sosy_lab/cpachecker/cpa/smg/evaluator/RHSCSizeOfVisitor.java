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
package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelationKind;

class RHSCSizeOfVisitor extends CSizeOfVisitor {

  private final SMGRightHandSideEvaluator smgRightHandSideEvaluator;

  public RHSCSizeOfVisitor(SMGRightHandSideEvaluator pSmgRightHandSideEvaluator,
      CFAEdge pEdge, SMGState pState, Optional<CExpression> pExpression) {
    super(pSmgRightHandSideEvaluator, pEdge, pState, pExpression);
    smgRightHandSideEvaluator = pSmgRightHandSideEvaluator;
  }

  @Override
  protected int handleUnkownArrayLengthValue(CArrayType pArrayType) {
    if (smgRightHandSideEvaluator.smgTransferRelation.kind == SMGTransferRelationKind.REFINEMENT) {
      return 0;
    } else {
      return super.handleUnkownArrayLengthValue(pArrayType);
    }
  }
}