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
package org.sosy_lab.cpachecker.cpa.composite;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageTransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class CompositeTransferRelation implements TransferRelation {

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
  public Collection<CompositeState> getAbstractSuccessors(AbstractState element, Precision precision, CFAEdge cfaEdge)
        throws CPATransferException, InterruptedException {
    CompositeState compositeState = (CompositeState) element;
    CompositePrecision compositePrecision = (CompositePrecision)precision;
    Collection<CompositeState> results;

    if (cfaEdge == null) {
      CFANode node = extractLocation(compositeState);
      if (node == null) {
        throw new CPATransferException("Analysis without LocationCPA is not supported, please add one to the configuration");
      }

      results = new ArrayList<>(node.getNumLeavingEdges());

      for (CFAEdge edge : leavingEdges(node)) {
        getAbstractSuccessorForEdge(compositeState, compositePrecision, edge, results);
      }

    } else {
      results = new ArrayList<>(1);
      getAbstractSuccessorForEdge(compositeState, compositePrecision, cfaEdge, results);

    }

    return results;
  }

  private void getAbstractSuccessorForEdge(CompositeState compositeState, CompositePrecision compositePrecision, CFAEdge cfaEdge,
      Collection<CompositeState> compositeSuccessors) throws CPATransferException, InterruptedException {
    assert cfaEdge != null;


    // first, call all the post operators
    int resultCount = 1;
    List<AbstractState> componentElements = compositeState.getWrappedStates();
    List<Collection<? extends AbstractState>> allComponentsSuccessors = new ArrayList<>(size);

    for (int i = 0; i < size; i++) {
      TransferRelation lCurrentTransfer = transferRelations.get(i);
      AbstractState lCurrentElement = componentElements.get(i);
      Precision lCurrentPrecision = compositePrecision.get(i);

      Collection<? extends AbstractState> componentSuccessors = lCurrentTransfer.getAbstractSuccessors(lCurrentElement, lCurrentPrecision, cfaEdge);
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

    // second, call strengthen for each result of the cartesian product
    for (List<AbstractState> lReachedState : allResultingElements) {

      List<Collection<? extends AbstractState>> lStrengthenResults = new ArrayList<>(size);

      resultCount = 1;

      for (int i = 0; i < size; i++) {

        TransferRelation lCurrentTransfer = transferRelations.get(i);
        AbstractState lCurrentElement = lReachedState.get(i);
        Precision lCurrentPrecision = compositePrecision.get(i);

        Collection<? extends AbstractState> lResultsList = lCurrentTransfer.strengthen(lCurrentElement, lReachedState, cfaEdge, lCurrentPrecision);

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
        AbstractState predElement = Iterables.getOnlyElement(lStrengthenResults.get(predicatesIndex));
        AbstractState assumptionElement = Iterables.getOnlyElement(lStrengthenResults.get(assumptionIndex));
        Precision predPrecision = compositePrecision.get(predicatesIndex);
        TransferRelation predTransfer = transferRelations.get(predicatesIndex);

        Collection<? extends AbstractState> predResult = predTransfer.strengthen(predElement, Collections.singletonList(assumptionElement), cfaEdge, predPrecision);
        resultCount *= predResult.size();

        lStrengthenResults.set(predicatesIndex, predResult);
      }

      // create cartesian product again
      Collection<List<AbstractState>> lResultingElements
          = createCartesianProduct(lStrengthenResults, resultCount);

      // finally, create a CompositeState for each result of the cartesian product
      for (List<AbstractState> lList : lResultingElements) {
        compositeSuccessors.add(new CompositeState(lList));
      }
    }
  }

  protected static Collection<List<AbstractState>> createCartesianProduct(
      List<Collection<? extends AbstractState>> allComponentsSuccessors, int resultCount) {
    Collection<List<AbstractState>> allResultingElements;
    switch (resultCount) {
    case 0:
      // at least one CPA decided that there is no successor
      allResultingElements = Collections.emptySet();
      break;

    case 1:
      List<AbstractState> resultingElements = new ArrayList<>(allComponentsSuccessors.size());
      for (Collection<? extends AbstractState> componentSuccessors : allComponentsSuccessors) {
        resultingElements.add(Iterables.getOnlyElement(componentSuccessors));
      }
      allResultingElements = Collections.singleton(resultingElements);
      break;

    default:
      // create cartesian product of all componentSuccessors and store the result in allResultingElements
      List<AbstractState> initialPrefix = Collections.emptyList();
      allResultingElements = new ArrayList<>(resultCount);
      createCartesianProduct0(allComponentsSuccessors, initialPrefix, allResultingElements);
    }

    assert resultCount == allResultingElements.size();
    return allResultingElements;
  }

  private static void createCartesianProduct0(List<Collection<? extends AbstractState>> allComponentsSuccessors,
      List<AbstractState> prefix, Collection<List<AbstractState>> allResultingElements) {

    if (prefix.size() == allComponentsSuccessors.size()) {
      allResultingElements.add(prefix);

    } else {
      int depth = prefix.size();
      Collection<? extends AbstractState> myComponentsSuccessors = allComponentsSuccessors.get(depth);

      for (AbstractState currentComponent : myComponentsSuccessors) {
        List<AbstractState> newPrefix = new ArrayList<>(prefix);
        newPrefix.add(currentComponent);

        createCartesianProduct0(allComponentsSuccessors, newPrefix, allResultingElements);
      }
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element,
      List<AbstractState> otherElements, CFAEdge cfaEdge,
      Precision precision) {
    // strengthen is only called by the composite CPA on its component CPAs
    return null;
  }

  boolean areAbstractSuccessors(AbstractState pElement, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors, List<ConfigurableProgramAnalysis> cpas) throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(pCfaEdge);

    CompositeState compositeState = (CompositeState)pElement;

    int resultCount = 1;
    boolean result = true;
    for (int i = 0; i < size; ++i) {
      Set<AbstractState> componentSuccessors = new HashSet<>();
      for (AbstractState successor : pSuccessors) {
        CompositeState compositeSuccessor = (CompositeState)successor;
        if (compositeSuccessor.getNumberOfStates() != size) {
          return false;
        }
        componentSuccessors.add(compositeSuccessor.get(i));
      }
      resultCount *= componentSuccessors.size();
      ProofChecker componentProofChecker = (ProofChecker)cpas.get(i);
      if (!componentProofChecker.areAbstractSuccessors(compositeState.get(i), pCfaEdge, componentSuccessors)) {
        result = false; //if there are no successors it might be still ok if one of the other components is fine with the empty set
      } else {
        if (componentSuccessors.isEmpty()) {
          assert pSuccessors.isEmpty();
          return true; //another component is indeed fine with the empty set as set of successors; transition is ok
        }
      }
    }

    if (resultCount > pSuccessors.size()) { return false; }

    HashSet<List<AbstractState>> states = new HashSet<>();
    for (AbstractState successor : pSuccessors) {
      states.add(((CompositeState) successor).getWrappedStates());
    }
    if (resultCount != states.size()) { return false; }

    return result;
  }
}
