/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPAException;


public abstract class AbstractInvariantGenerator implements InvariantGenerator {

  @Override
  public abstract void start(CFANode pInitialLocation);

  @Override
  public abstract void cancel();

  @Override
  public InvariantSupplierWithoutContext getWithoutContext()
      throws CPAException, InterruptedException {
    return InvariantSupplierWithoutContext.TrivialInvariantSupplier.INSTANCE;
  }

  @Override
  public ExpressionTreeSupplier getAsExpressionTree() throws CPAException, InterruptedException {
    return ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;
  }

  @Override
  public abstract boolean isProgramSafe();

}
