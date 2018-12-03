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

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.HybridValueTransformer;
import org.sosy_lab.cpachecker.cpa.hybrid.value.HybridValue;

/**
 * A hybrid value transformer for non deterministic function assignments to variable
 */
public class HybridValueIdExpressionTransformer
    implements HybridValueTransformer<CExpression, CIdExpression> {

  /**
   * @param pValue The value to transform
   * @param pCIdExpression The variable expression to assign the given value to
   */
  @Override
  public CExpression transform(HybridValue pValue, CIdExpression pCIdExpression) {
    return null;
  }


}
