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
package org.sosy_lab.cpachecker.cpa.fsmbdd.tests;

import java.math.BigInteger;

import net.sf.javabdd.BDDFactory;

import org.junit.Before;
import org.junit.Ignore;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.fsmbdd.FsmBddState;
import org.sosy_lab.cpachecker.cpa.fsmbdd.FsmBddStatistics;
import org.sosy_lab.cpachecker.cpa.fsmbdd.interfaces.DomainIntervalProvider;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Ignore
public abstract class FsmBddTesting {
  protected static BDDFactory bddfactory = BDDFactory.init("java", 500000, 200000);

  protected CIdExpression v2 = new CIdExpression(null, null, "v2", null);
  protected CIdExpression v3 = new CIdExpression(null, null, "v3", null);

  protected CFANode l1 = new CFANode(0, "fn");

  protected CIntegerLiteralExpression int7 = new CIntegerLiteralExpression(null, null, new BigInteger("7"));
  protected CIntegerLiteralExpression int8 = new CIntegerLiteralExpression(null, null, new BigInteger("8"));
  protected CIntegerLiteralExpression int9 = new CIntegerLiteralExpression(null, null, new BigInteger("9"));

  @Before
  public void setup() {
    FsmBddState.statistic = new FsmBddStatistics(bddfactory);
  }

  protected DomainIntervalProvider domainInterval = new DomainIntervalProvider() {
    @Override
    public int mapLiteralToIndex(CExpression pLiteral) throws CPATransferException {
      if (pLiteral instanceof CIntegerLiteralExpression) {
        return ((CIntegerLiteralExpression) pLiteral).getValue().intValue();
      } else {
        throw new RuntimeException("Type of expression not supported!");
      }
    }

    @Override
    public int getIntervalMaximum() throws CPATransferException {
      return 10;
    }
  };

}
