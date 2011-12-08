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
package org.sosy_lab.cpachecker.cpa.composite;

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageTransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class CompositeTransferRelation implements TransferRelation{

  protected final ImmutableList<TransferRelation> transferRelations;
  protected final int size;
  private int assumptionIndex = -1;
  private int predicatesIndex = -1;

  public CompositeTransferRelation(ImmutableList<TransferRelation> transferRelations) {
    this.transferRelations = transferRelations;
    size = transferRelations.size();

    // prepare special case handling if both predicates and assumptions are used
    for (int i = 0; i < size; i++) {
      TransferRelation t = transferRelations.get(i);
      if (t instanceof PredicateTransferRelation) {
        predicatesIndex = i;
      }
      if (t instanceof AssumptionStorageTransferRelation) {
        assumptionIndex = i;
      }
    }
  }

  @Override
  public Collection<CompositeElement> getAbstractSuccessors(AbstractElement element, Precision precision, CFAEdge cfaEdge)
        throws CPATransferException, InterruptedException {
    CompositeElement compositeElement = (CompositeElement) element;
    CompositePrecision compositePrecision = (CompositePrecision)precision;
    Collection<CompositeElement> results;

    if (cfaEdge == null) {
      CFANode node = extractLocation(compositeElement);
      results = new ArrayList<CompositeElement>(node.getNumLeavingEdges());

      for (int edgeIdx = 0; edgeIdx < node.getNumLeavingEdges(); edgeIdx++) {
        CFAEdge edge = node.getLeavingEdge(edgeIdx);
        getAbstractSuccessorForEdge(compositeElement, compositePrecision, edge, results);
      }

    } else {
      results = new ArrayList<CompositeElement>(1);
      getAbstractSuccessorForEdge(compositeElement, compositePrecision, cfaEdge, results);

    }

    return results;
  }

  private void getAbstractSuccessorForEdge(CompositeElement compositeElement, CompositePrecision compositePrecision, CFAEdge cfaEdge,
      Collection<CompositeElement> compositeSuccessors) throws CPATransferException, InterruptedException {
    assert cfaEdge != null;


    // first, call all the post operators
    int resultCount = 1;
    List<AbstractElement> componentElements = compositeElement.getElements();
    List<Collection<? extends AbstractElement>> allComponentsSuccessors = new ArrayList<Collection<? extends AbstractElement>>(size);

    for (int i = 0; i < size; i++) {
      TransferRelation lCurrentTransfer = transferRelations.get(i);
      AbstractElement lCurrentElement = componentElements.get(i);
      Precision lCurrentPrecision = compositePrecision.get(i);

      Collection<? extends AbstractElement> componentSuccessors = lCurrentTransfer.getAbstractSuccessors(lCurrentElement, lCurrentPrecision, cfaEdge);
      resultCount *= componentSuccessors.size();

      if (resultCount == 0) {
        // shortcut
        break;
      }

      allComponentsSuccessors.add(componentSuccessors);
    }

    // create cartesian product of all elements we got
    Collection<List<AbstractElement>> allResultingElements
        = createCartesianProduct(allComponentsSuccessors, resultCount);

    // second, call strengthen for each result of the cartesian product
    for (List<AbstractElement> lReachedElement : allResultingElements) {

      List<Collection<? extends AbstractElement>> lStrengthenResults = new ArrayList<Collection<? extends AbstractElement>>(size);

      resultCount = 1;

      for (int i = 0; i < size; i++) {

        TransferRelation lCurrentTransfer = transferRelations.get(i);
        AbstractElement lCurrentElement = lReachedElement.get(i);
        Precision lCurrentPrecision = compositePrecision.get(i);

        Collection<? extends AbstractElement> lResultsList = lCurrentTransfer.strengthen(lCurrentElement, lReachedElement, cfaEdge, lCurrentPrecision);

        if (lResultsList == null) {
          lStrengthenResults.add(Collections.singleton(lCurrentElement));
        } else {
          resultCount *= lResultsList.size();

          if (resultCount == 0) {
            // shortcut
            break;
          }

          lStrengthenResults.add(lResultsList);
        }
      }

      // special case handling if we have predicate and assumption cpas
      if (predicatesIndex >= 0 && assumptionIndex >= 0 && resultCount > 0) {
        AbstractElement predElement = Iterables.getOnlyElement(lStrengthenResults.get(predicatesIndex));
        AbstractElement assumptionElement = Iterables.getOnlyElement(lStrengthenResults.get(assumptionIndex));
        Precision predPrecision = compositePrecision.get(predicatesIndex);
        TransferRelation predTransfer = transferRelations.get(predicatesIndex);

        Collection<? extends AbstractElement> predResult = predTransfer.strengthen(predElement, Collections.singletonList(assumptionElement), cfaEdge, predPrecision);
        resultCount *= predResult.size();

        lStrengthenResults.set(predicatesIndex, predResult);
      }

      // create cartesian product again
      Collection<List<AbstractElement>> lResultingElements
          = createCartesianProduct(lStrengthenResults, resultCount);

      // finally, create a CompositeElement for each result of the cartesian product
      for (List<AbstractElement> lList : lResultingElements) {
        compositeSuccessors.add(new CompositeElement(lList));
      }
    }
  }

  protected static Collection<List<AbstractElement>> createCartesianProduct(
      List<Collection<? extends AbstractElement>> allComponentsSuccessors, int resultCount) {
    Collection<List<AbstractElement>> allResultingElements;
    switch (resultCount) {
    case 0:
      // at least one CPA decided that there is no successor
      allResultingElements = Collections.emptySet();
      break;

    case 1:
      List<AbstractElement> resultingElements = new ArrayList<AbstractElement>(allComponentsSuccessors.size());
      for (Collection<? extends AbstractElement> componentSuccessors : allComponentsSuccessors) {
        resultingElements.add(Iterables.getOnlyElement(componentSuccessors));
      }
      allResultingElements = Collections.singleton(resultingElements);
      break;

    default:
      // create cartesian product of all componentSuccessors and store the result in allResultingElements
      List<AbstractElement> initialPrefix = Collections.emptyList();
      allResultingElements = new ArrayList<List<AbstractElement>>(resultCount);
      createCartesianProduct0(allComponentsSuccessors, initialPrefix, allResultingElements);
    }

    assert resultCount == allResultingElements.size();
    return allResultingElements;
  }

  private static void createCartesianProduct0(List<Collection<? extends AbstractElement>> allComponentsSuccessors,
      List<AbstractElement> prefix, Collection<List<AbstractElement>> allResultingElements) {

    if (prefix.size() == allComponentsSuccessors.size()) {
      allResultingElements.add(prefix);

    } else {
      int depth = prefix.size();
      Collection<? extends AbstractElement> myComponentsSuccessors = allComponentsSuccessors.get(depth);

      for (AbstractElement currentComponent : myComponentsSuccessors) {
        List<AbstractElement> newPrefix = new ArrayList<AbstractElement>(prefix);
        newPrefix.add(currentComponent);

        createCartesianProduct0(allComponentsSuccessors, newPrefix, allResultingElements);
      }
    }
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
      List<AbstractElement> otherElements, CFAEdge cfaEdge,
      Precision precision) {
    // strengthen is only called by the composite CPA on its component CPAs
    return null;
  }
}
