// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.indexOf;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

final class CompositeTransferRelation implements WrapperTransferRelation {

  private final ImmutableList<TransferRelation> transferRelations;
  private final CFA cfa;
  private final int size;
  private final boolean predicatesPresent;
  private final boolean aggregateBasicBlocks;

  CompositeTransferRelation(
      ImmutableList<TransferRelation> pTransferRelations, CFA pCFA, boolean pAggregateBasicBlocks) {
    transferRelations = pTransferRelations;
    cfa = pCFA;
    size = pTransferRelations.size();
    aggregateBasicBlocks = pAggregateBasicBlocks;

    // prepare special case handling if both predicates and assumptions are used
    predicatesPresent =
        (indexOf(pTransferRelations, Predicates.instanceOf(PredicateTransferRelation.class)) != -1);
  }

  @Override
  public Collection<CompositeState> getAbstractSuccessors(
      AbstractState element, Precision precision)
      throws CPATransferException, InterruptedException {
    CompositeState compositeState = (CompositeState) element;
    CompositePrecision compositePrecision = (CompositePrecision) precision;
    Collection<CompositeState> results;

    AbstractStateWithLocations locState =
        extractStateByType(compositeState, AbstractStateWithLocations.class);
    results = new ArrayList<>(2);
    if (locState == null) {
      getAbstractSuccessors(compositeState, compositePrecision, results);
    } else {
      for (CFAEdge edge : locState.getOutgoingEdges()) {
        getAbstractSuccessorForEdge(compositeState, compositePrecision, edge, results);
      }
    }

    return results;
  }

  private void getAbstractSuccessors(
      CompositeState pCompositeState,
      CompositePrecision pCompositePrecision,
      Collection<CompositeState> compositeSuccessors)
      throws CPATransferException, InterruptedException {
    int resultCount = 1;
    List<AbstractState> componentElements = pCompositeState.getWrappedStates();
    checkArgument(
        componentElements.size() == size, "State with wrong number of component states given");
    List<Collection<? extends AbstractState>> allComponentsSuccessors = new ArrayList<>(size);

    // first, call all the post operators
    for (int i = 0; i < size; i++) {
      TransferRelation lCurrentTransfer = transferRelations.get(i);
      AbstractState lCurrentElement = componentElements.get(i);
      Precision lCurrentPrecision = pCompositePrecision.get(i);

      Collection<? extends AbstractState> componentSuccessors;
      componentSuccessors =
          lCurrentTransfer.getAbstractSuccessors(lCurrentElement, lCurrentPrecision);
      resultCount *= componentSuccessors.size();

      if (resultCount == 0) {
        // shortcut
        break;
      }

      allComponentsSuccessors.add(componentSuccessors);
    }

    for (List<AbstractState> successor :
        createCartesianProduct(allComponentsSuccessors, resultCount)) {
      compositeSuccessors.add(new CompositeState(successor));
    }
  }

  @Override
  public Collection<CompositeState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    CompositeState compositeState = (CompositeState) element;
    CompositePrecision compositePrecision = (CompositePrecision) precision;

    Collection<CompositeState> results = new ArrayList<>(1);
    getAbstractSuccessorForSimpleEdge(compositeState, compositePrecision, cfaEdge, results);

    return results;
  }

  private void getAbstractSuccessorForEdge(
      CompositeState compositeState,
      CompositePrecision compositePrecision,
      CFAEdge cfaEdge,
      Collection<CompositeState> compositeSuccessors)
      throws CPATransferException, InterruptedException {

    if (aggregateBasicBlocks) {
      final CFANode startNode = cfaEdge.getPredecessor();

      // dynamic multiEdges may be used if the following conditions apply
      if (isValidMultiEdgeStart(startNode) && isValidMultiEdgeComponent(cfaEdge)) {

        Collection<CompositeState> currentStates = new ArrayList<>(1);
        currentStates.add(compositeState);

        while (isValidMultiEdgeComponent(cfaEdge)) {
          Collection<CompositeState> successorStates = new ArrayList<>(currentStates.size());

          for (CompositeState currentState : currentStates) {
            getAbstractSuccessorForSimpleEdge(
                currentState, compositePrecision, cfaEdge, successorStates);
          }

          // if we found a target state in the current successors immediately return
          if (from(successorStates).anyMatch(AbstractStates::isTargetState)) {
            compositeSuccessors.addAll(successorStates);
            return;
          }

          // make successor states the new to-be-handled states for the next edge
          currentStates = Collections.unmodifiableCollection(successorStates);

          // if there is more than one leaving edge we do not create a further
          // multi edge part
          if (cfaEdge.getSuccessor().getNumLeavingEdges() == 1) {
            cfaEdge = cfaEdge.getSuccessor().getLeavingEdge(0);
          } else {
            break;
          }
        }

        compositeSuccessors.addAll(currentStates);

        // no use for dynamic multi edges right now, just compute the successor
        // for the given edge
      } else {
        getAbstractSuccessorForSimpleEdge(
            compositeState, compositePrecision, cfaEdge, compositeSuccessors);
      }

    } else {
      getAbstractSuccessorForSimpleEdge(
          compositeState, compositePrecision, cfaEdge, compositeSuccessors);
    }
  }

  private boolean isValidMultiEdgeStart(CFANode node) {
    return node.getNumLeavingEdges() == 1 // linear chain of edges
        && node.getLeavingSummaryEdge() == null // without a functioncall
        && node.getNumEnteringEdges() > 0; // without a functionstart
  }

  /**
   * This method checks if the given edge and its successor node are a valid component for a
   * continuing dynamic MultiEdge.
   */
  private boolean isValidMultiEdgeComponent(CFAEdge edge) {
    boolean result =
        edge.getEdgeType() == CFAEdgeType.BlankEdge
            || edge.getEdgeType() == CFAEdgeType.DeclarationEdge
            || edge.getEdgeType() == CFAEdgeType.StatementEdge
            || edge.getEdgeType() == CFAEdgeType.ReturnStatementEdge;

    CFANode nodeAfterEdge = edge.getSuccessor();

    result =
        result
            && nodeAfterEdge.getNumEnteringEdges() == 1
            && nodeAfterEdge.getClass() == CFANode.class;

    return result && !containsFunctionCall(edge);
  }

  /**
   * This method checks, if the given (statement) edge contains a function call directly or via a
   * function pointer.
   *
   * @param edge the edge to inspect
   * @return whether or not this edge contains a function call or not.
   */
  private boolean containsFunctionCall(CFAEdge edge) {
    if (edge.getEdgeType() == CFAEdgeType.StatementEdge) {
      CStatementEdge statementEdge = (CStatementEdge) edge;

      if ((statementEdge.getStatement() instanceof CFunctionCall)) {
        CFunctionCall call = ((CFunctionCall) statementEdge.getStatement());
        CSimpleDeclaration declaration = call.getFunctionCallExpression().getDeclaration();

        // declaration == null -> functionPointer
        // functionName exists in CFA -> functioncall with CFA for called function
        // otherwise: call of non-existent function, example: nondet_int() -> ignore this case
        return declaration == null
            || cfa.getAllFunctionNames().contains(declaration.getQualifiedName());
      }
      return (statementEdge.getStatement() instanceof CFunctionCall);
    }
    return false;
  }

  private void getAbstractSuccessorForSimpleEdge(
      CompositeState compositeState,
      CompositePrecision compositePrecision,
      CFAEdge cfaEdge,
      Collection<CompositeState> compositeSuccessors)
      throws CPATransferException, InterruptedException {
    assert cfaEdge != null;

    // first, call all the post operators
    Collection<List<AbstractState>> allResultingElements =
        callTransferRelation(compositeState, compositePrecision, cfaEdge);

    // second, call strengthen for each result
    for (List<AbstractState> lReachedState : allResultingElements) {

      Collection<List<AbstractState>> lResultingElements =
          callStrengthen(lReachedState, compositePrecision, cfaEdge);

      // finally, create a CompositeState for each result of strengthen
      for (List<AbstractState> lList : lResultingElements) {
        compositeSuccessors.add(new CompositeState(lList));
      }
    }
  }

  private Collection<List<AbstractState>> callTransferRelation(
      final CompositeState compositeState,
      final CompositePrecision compositePrecision,
      final CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    int resultCount = 1;
    List<AbstractState> componentElements = compositeState.getWrappedStates();
    checkArgument(
        componentElements.size() == size, "State with wrong number of component states given");
    List<Collection<? extends AbstractState>> allComponentsSuccessors = new ArrayList<>(size);

    for (int i = 0; i < size; i++) {
      TransferRelation lCurrentTransfer = transferRelations.get(i);
      AbstractState lCurrentElement = componentElements.get(i);
      Precision lCurrentPrecision = compositePrecision.get(i);

      Collection<? extends AbstractState> componentSuccessors;
      componentSuccessors =
          lCurrentTransfer.getAbstractSuccessorsForEdge(
              lCurrentElement, lCurrentPrecision, cfaEdge);
      resultCount *= componentSuccessors.size();

      if (resultCount == 0) {
        // shortcut
        break;
      }

      allComponentsSuccessors.add(componentSuccessors);
    }

    // create cartesian product of all elements we got
    return createCartesianProduct(allComponentsSuccessors, resultCount);
  }

  private Collection<List<AbstractState>> callStrengthen(
      final List<AbstractState> reachedState,
      final CompositePrecision compositePrecision,
      final CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    List<Collection<? extends AbstractState>> lStrengthenResults = new ArrayList<>(size);
    int resultCount = 1;

    for (int i = 0; i < size; i++) {

      TransferRelation lCurrentTransfer = transferRelations.get(i);
      AbstractState lCurrentElement = reachedState.get(i);
      Precision lCurrentPrecision = compositePrecision.get(i);

      Collection<? extends AbstractState> lResultsList =
          lCurrentTransfer.strengthen(lCurrentElement, reachedState, cfaEdge, lCurrentPrecision);

      resultCount *= lResultsList.size();
      if (resultCount == 0) {
        // shortcut
        break;
      }

      lStrengthenResults.add(lResultsList);
    }

    // create cartesian product
    Collection<List<AbstractState>> strengthenedStates =
        createCartesianProduct(lStrengthenResults, resultCount);

    // special case handling if we have predicate and assumption cpas
    // TODO remove as soon as we call strengthen in a fixpoint loop
    if (predicatesPresent && resultCount > 0) {
      Iterator<List<AbstractState>> it = strengthenedStates.iterator();
      while (it.hasNext()) {
        final List<AbstractState> strengthenedState = it.next();
        ImmutableList<AbstractState> assumptionElements =
            from(strengthenedState).filter(CompositeTransferRelation::hasAssumptions).toList();
        if (assumptionElements.isEmpty()) {
          continue;
        }

        final int predIndex =
            Iterables.indexOf(strengthenedState, x -> x instanceof PredicateAbstractState);
        Preconditions.checkState(
            predIndex >= 0, "cartesian product should ensure that predicates do not vanish!");
        AbstractState predElement = strengthenedState.get(predIndex);
        Precision predPrecision = compositePrecision.get(predIndex);
        TransferRelation predTransfer = transferRelations.get(predIndex);
        Collection<? extends AbstractState> predResult =
            predTransfer.strengthen(predElement, assumptionElements, cfaEdge, predPrecision);
        if (predResult.isEmpty()) {
          it.remove();
          resultCount--;
        } else {
          assert predResult.size() == 1;
          strengthenedState.set(predIndex, predResult.iterator().next());
        }
      }
    }

    // If state was not a target state before but a target state was found during strengthening,
    // we call strengthen again such that the other CPAs can act on this information.
    // Note that this terminates because in the inner call the input state
    // is already a target state and this branch won't be taken.
    // TODO Generalize this into a full fixpoint algorithm.
    if (!any(reachedState, AbstractStates::isTargetState)) {
      Collection<List<AbstractState>> newStrengthenedStates = new ArrayList<>(resultCount);

      for (List<AbstractState> strengthenedState : strengthenedStates) {
        if (any(strengthenedState, AbstractStates::isTargetState)) {
          newStrengthenedStates.addAll(
              callStrengthen(strengthenedState, compositePrecision, cfaEdge));
        } else {
          newStrengthenedStates.add(strengthenedState);
        }
      }
      return newStrengthenedStates;

    } else {
      return strengthenedStates;
    }
  }

  private static boolean hasAssumptions(AbstractState x) {
    return x instanceof AbstractStateWithAssumptions
        || x instanceof AssumptionStorageState
        || x instanceof FormulaReportingState;
  }

  static Collection<List<AbstractState>> createCartesianProduct(
      List<Collection<? extends AbstractState>> allComponentsSuccessors, int resultCount) {
    Collection<List<AbstractState>> allResultingElements;
    switch (resultCount) {
      case 0:
        // at least one CPA decided that there is no successor
        allResultingElements = ImmutableSet.of();
        break;

      case 1:
        List<AbstractState> resultingElements = new ArrayList<>(allComponentsSuccessors.size());
        for (Collection<? extends AbstractState> componentSuccessors : allComponentsSuccessors) {
          resultingElements.add(Iterables.getOnlyElement(componentSuccessors));
        }
        allResultingElements = Collections.singleton(resultingElements);
        break;

      default:
        // create cartesian product of all componentSuccessors and store the result in
        // allResultingElements
        List<AbstractState> initialPrefix = ImmutableList.of();
        allResultingElements = new ArrayList<>(resultCount);
        createCartesianProduct0(allComponentsSuccessors, initialPrefix, allResultingElements);
    }

    assert resultCount == allResultingElements.size();
    return allResultingElements;
  }

  private static void createCartesianProduct0(
      List<Collection<? extends AbstractState>> allComponentsSuccessors,
      List<AbstractState> prefix,
      Collection<List<AbstractState>> allResultingElements) {

    if (prefix.size() == allComponentsSuccessors.size()) {
      allResultingElements.add(prefix);

    } else {
      int depth = prefix.size();
      Collection<? extends AbstractState> myComponentsSuccessors =
          allComponentsSuccessors.get(depth);

      for (AbstractState currentComponent : myComponentsSuccessors) {
        List<AbstractState> newPrefix = new ArrayList<>(prefix);
        newPrefix.add(currentComponent);

        createCartesianProduct0(allComponentsSuccessors, newPrefix, allResultingElements);
      }
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState element,
      Iterable<AbstractState> otherElements,
      CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {

    CompositeState compositeState = (CompositeState) element;
    CompositePrecision compositePrecision = (CompositePrecision) precision;
    List<Collection<? extends AbstractState>> lStrengthenResults = new ArrayList<>(size);
    int resultCount = 1;

    for (int i = 0; i < size; i++) {

      TransferRelation lCurrentTransfer = transferRelations.get(i);
      AbstractState lCurrentElement = compositeState.get(i);
      Precision lCurrentPrecision = compositePrecision.get(i);

      Collection<? extends AbstractState> lResultsList =
          lCurrentTransfer.strengthen(lCurrentElement, otherElements, cfaEdge, lCurrentPrecision);

      resultCount *= lResultsList.size();
      if (resultCount == 0) {
        // shortcut
        break;
      }

      lStrengthenResults.add(lResultsList);
    }

    Collection<List<AbstractState>> lResultingElements =
        createCartesianProduct(lStrengthenResults, resultCount);
    return transformedImmutableListCopy(lResultingElements, CompositeState::new);
  }

  boolean areAbstractSuccessors(
      AbstractState pElement,
      CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors,
      List<ConfigurableProgramAnalysis> cpas)
      throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(pCfaEdge);
    Preconditions.checkState(!aggregateBasicBlocks);

    CompositeState compositeState = (CompositeState) pElement;

    int resultCount = 1;
    boolean result = true;
    for (int i = 0; i < size; ++i) {
      Set<AbstractState> componentSuccessors = new HashSet<>();
      for (AbstractState successor : pSuccessors) {
        CompositeState compositeSuccessor = (CompositeState) successor;
        if (compositeSuccessor.getNumberOfStates() != size) {
          return false;
        }
        componentSuccessors.add(compositeSuccessor.get(i));
      }
      resultCount *= componentSuccessors.size();
      ProofChecker componentProofChecker = (ProofChecker) cpas.get(i);
      if (!componentProofChecker.areAbstractSuccessors(
          compositeState.get(i), pCfaEdge, componentSuccessors)) {
        // if there are no successors it might be still ok if one of the other components is fine
        // with the empty set
        result = false;
      } else {
        if (componentSuccessors.isEmpty()) {
          assert pSuccessors.isEmpty();
          // another component is indeed fine with the empty set as set of successors; transition is
          // ok
          return true;
        }
      }
    }

    if (resultCount > pSuccessors.size()) {
      return false;
    }

    Set<List<AbstractState>> states = new HashSet<>();
    for (AbstractState successor : pSuccessors) {
      states.add(((CompositeState) successor).getWrappedStates());
    }
    if (resultCount != states.size()) {
      return false;
    }

    return result;
  }

  @Override
  @Nullable
  public <T extends TransferRelation> T retrieveWrappedTransferRelation(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    }
    for (TransferRelation tr : transferRelations) {
      if (pType.isAssignableFrom(tr.getClass())) {
        return pType.cast(tr);
      } else if (tr instanceof WrapperTransferRelation) {
        T result = ((WrapperTransferRelation) tr).retrieveWrappedTransferRelation(pType);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  @Override
  public Iterable<TransferRelation> getWrappedTransferRelations() {
    return transferRelations;
  }
}
