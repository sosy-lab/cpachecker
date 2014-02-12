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
package org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Transfer relation for the generic assumption generator.
 */
public class GenericAssumptionsTransferRelation implements TransferRelation {

  /**
   * List of interfaces used to build the default
   * assumptions made by the model checker for
   * program operations.
   *
   * Modify this to register new kind of assumptions.
   */
  private final List<GenericAssumptionBuilder> assumptionBuilders =
    ImmutableList.<GenericAssumptionBuilder>of(
        new ArithmeticOverflowAssumptionBuilder());

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState el, Precision p, CFAEdge edge)
  throws CPATransferException {

    List<CExpression> allAssumptions = Lists.newArrayList();
    for (GenericAssumptionBuilder b : assumptionBuilders) {
      allAssumptions.addAll(b.assumptionsForEdge(edge));
    }

    return Collections.singleton(new GenericAssumptionsState(allAssumptions));
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState el, List<AbstractState> otherElements,
      CFAEdge edge, Precision p)
      throws CPATransferException {
    // TODO Improve strengthening for assumptions so that they
    //      may be discharged online
    return null;
  }

}
