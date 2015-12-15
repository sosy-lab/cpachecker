/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeTransferRelation;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonPredicateElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStandardElement;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.collect.ImmutableList;

@Options
public class ProductAutomatonTransferRelation extends CompositeTransferRelation {

  public ProductAutomatonTransferRelation(ImmutableList<TransferRelation> pTransferRelations,
      Configuration pConfig, CFA pCFA) throws InvalidConfigurationException {
    super (pTransferRelations, pConfig, pCFA);
  }

  @Override
  public Collection<CompositeState> getAbstractSuccessors(AbstractState state, Precision precision) throws CPATransferException {
    throw new RuntimeException("TODO implement");
  }

  /*
   * (non-Javadoc)
   * @see org.sosy_lab.cpachecker.cpa.composite.CompositeTransferRelation#getAbstractSuccessors(org.sosy_lab.cpachecker.core.interfaces.AbstractElement, org.sosy_lab.cpachecker.core.interfaces.Precision, org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge)
   *
   * We do not strengthen!
   */
  @Override
  public Collection<CompositeState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    CompositeState lCompositeElement = (CompositeState)pElement;
    CompositePrecision lCompositePrecision = (CompositePrecision)pPrecision;

    int resultCount = 1;
    List<AbstractState> componentElements = lCompositeElement.getWrappedStates();
    List<Collection<? extends AbstractState>> allComponentsSuccessors = new ArrayList<>(size);

    for (int i = 0; i < size; i++) {
      TransferRelation lCurrentTransfer = transferRelations.get(i);
      AbstractState lCurrentElement = componentElements.get(i);
      Precision lCurrentPrecision = lCompositePrecision.get(i);

      Collection<? extends AbstractState> componentSuccessors = lCurrentTransfer.getAbstractSuccessorsForEdge(lCurrentElement, lCurrentPrecision, pCfaEdge);
      resultCount *= componentSuccessors.size();

      if (resultCount == 0) {
        // shortcut
        break;
      }

      allComponentsSuccessors.add(componentSuccessors);
    }

    // create cartesian product of all elements we got
    Collection<List<AbstractState>> allResultingElements
        = createCartesianProduct(allComponentsSuccessors, resultCount);

    Collection<CompositeState> lSuccessors = new LinkedHashSet<>();

    for (List<AbstractState> lSuccessor : allResultingElements) {
      ProductAutomatonElement lNewSuccessor = ProductAutomatonElement.createElement(lSuccessor);

      lSuccessors.add(lNewSuccessor);
    }

    return lSuccessors;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {

    if (pElement instanceof ProductAutomatonElement.PredicateElement) {
      ProductAutomatonElement.PredicateElement lElement = (ProductAutomatonElement.PredicateElement)pElement;
      List<AbstractState> lSubelements = new ArrayList<>(lElement.getNumberOfStates());

      for (AbstractState lSubelement : lElement.getWrappedStates()) {
        if (lSubelement instanceof GuardedEdgeAutomatonPredicateElement) {
          GuardedEdgeAutomatonPredicateElement lPredicateElement = (GuardedEdgeAutomatonPredicateElement)lSubelement;
          lSubelements.add(lPredicateElement.getStandardElement());
        }
        else if (lSubelement instanceof GuardedEdgeAutomatonStandardElement) {
          lSubelements.add(lSubelement);
        }
        else {
          throw new RuntimeException("Unsupported element type!");
        }
      }

      return Collections.singleton(new ProductAutomatonElement.StateElement(lSubelements));
    }

    return null;
  }

}
