/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses({ CompoundStateTest.class, SimpleIntervalTest.class,
  org.sosy_lab.cpachecker.cpa.invariants.operators.interval.interval.tointerval.AddOperatorTest.class,
  org.sosy_lab.cpachecker.cpa.invariants.operators.interval.interval.tointerval.MultiplyOperatorTest.class,
  org.sosy_lab.cpachecker.cpa.invariants.operators.interval.interval.tointerval.DivideOperatorTest.class,
  org.sosy_lab.cpachecker.cpa.invariants.operators.interval.interval.tointerval.ModuloOperatorTest.class,
  org.sosy_lab.cpachecker.cpa.invariants.operators.interval.scalar.tointerval.ModuloOperatorTest.class,
  org.sosy_lab.cpachecker.cpa.invariants.operators.interval.scalar.tointerval.ShiftLeftOperatorTest.class,
  org.sosy_lab.cpachecker.cpa.invariants.operators.interval.scalar.tointerval.ShiftRightOperatorTest.class})
public class InvariantsTestSuite {

}
