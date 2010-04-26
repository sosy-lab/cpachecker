/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.fql.fllesh;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;

import org.sosy_lab.cpachecker.cpa.alwaystop.AlwaysTopCPA;
import org.sosy_lab.cpachecker.cpa.alwaystop.AlwaysTopTopElement;
import org.sosy_lab.cpachecker.core.CallElement;
import org.sosy_lab.cpachecker.core.CallStack;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.concrete.ConcreteAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.concrete.ConcreteAnalysisTopElement;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationElement;
import org.sosy_lab.cpachecker.cpa.mustmay.MustMayAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.mustmay.MustMayAnalysisElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fllesh.fql.backend.pathmonitor.Automaton;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.Node;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.reachability.Query;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.reachability.SingletonQuery;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.reachability.StandardQuery;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.reachability.Waypoint;

public class FeasibilityCheck {

  private AlwaysTopCPA mMayCPA;
  private ConcreteAnalysisCPA mMustCPA;
  private MustMayAnalysisCPA mMustMayAnalysisCPA;
  private LocationCPA mLocationCPA;
  private ConfigurableProgramAnalysis mCompositeCPA;

  private StandardQuery.Factory mQueryFactory;

  public FeasibilityCheck(LogManager pLogManager) throws InvalidConfigurationException, CPAException {

    mMayCPA = new AlwaysTopCPA();
    mMustCPA = new ConcreteAnalysisCPA();

    mMustMayAnalysisCPA = new MustMayAnalysisCPA(mMustCPA, mMayCPA);

    mLocationCPA = new LocationCPA();

    LinkedList<ConfigurableProgramAnalysis> lCPAs = new LinkedList<ConfigurableProgramAnalysis>();

    lCPAs.add(mLocationCPA);
    lCPAs.add(mMustMayAnalysisCPA);

    mCompositeCPA = CompositeCPA.factory().setChildren(lCPAs).createInstance();

    mQueryFactory = new StandardQuery.Factory(pLogManager, mMustCPA, mMayCPA);
  }

  public Witness run(LinkedList<Automaton> pAutomatonSequence, LinkedList<Node> pWaypointSequence, Automaton pPassingMonitor, Node pInitialState) {

    assert(pAutomatonSequence != null);
    assert(pWaypointSequence != null);
    assert(pPassingMonitor != null);
    assert(pInitialState != null);
    assert(pAutomatonSequence.size() == pWaypointSequence.size());


    // TODO remove output
    System.out.println(pAutomatonSequence);
    System.out.println(pWaypointSequence);


    LinkedList<Query> lQueries = new LinkedList<Query>();
    LinkedList<Waypoint> lWaypoints = new LinkedList<Waypoint>();

    int lMaxIndex = 0;

    int lLastIndex = pAutomatonSequence.size() + 1;


    CompositeElement lInitialElement = createInitialElement(pInitialState);
    CompositePrecision lInitialPrecision = createInitialPrecision(pInitialState);
    Automaton lFirstAutomaton = pAutomatonSequence.getFirst();
    Query lInitialQuery = SingletonQuery.create(lInitialElement, lInitialPrecision, lFirstAutomaton, lFirstAutomaton.getInitialStates(), pPassingMonitor, pPassingMonitor.getInitialStates());
    lQueries.add(lInitialQuery);

    while (!lQueries.isEmpty()) {
      Query lQuery = lQueries.getLast();

      if (lQuery.hasNext()) {
        Waypoint lWaypoint = lQuery.next();

        lWaypoints.addLast(lWaypoint);

        if (lQueries.size() == lLastIndex) {
          return generateWitness(lWaypoints);
        }
        else {

          // check and update backtrack level
          if (lQueries.size() > lMaxIndex) {
            lMaxIndex = lQueries.size();
          }

          Set<Integer> lFinalStates;

          if (lQueries.size() + 1 == lLastIndex) {
            lFinalStates = pPassingMonitor.getFinalStates();
          }
          else {
            lFinalStates = pPassingMonitor.getStates();
          }

          int lQueryIndex = lQueries.size() - 1;

          //CompositeElement lNextElement = createNextElement(pWaypointSequence.get(lQueryIndex));

          Automaton lNextAutomaton = pAutomatonSequence.get(lQueryIndex);

          //Query lNextQuery = StandardQuery.create(lNextAutomaton, pPassingMonitor, lWaypoint.getElement(), lWaypoint.getPrecision(), lNextAutomaton.getInitialStates(), lWaypoint.getStatesOfSecondAutomaton(), lNextElement, lNextAutomaton.getFinalStates(), lFinalStates);
          // TODO we have no predicate support currently
          CFANode lNextTargetCFANode = pWaypointSequence.get(lQueryIndex).getCFANode();

          Query lNextQuery = mQueryFactory.create(lNextAutomaton, pPassingMonitor, lWaypoint.getElement(), lWaypoint.getPrecision(), lNextAutomaton.getInitialStates(), lWaypoint.getStatesOfSecondAutomaton(), lNextTargetCFANode, lNextAutomaton.getFinalStates(), lFinalStates);

          lQueries.addLast(lNextQuery);
        }

      }
      else {
        lQueries.removeLast();
      }
    }

    return new InfeasibilityWitness(lMaxIndex);
  }

  private CompositePrecision createInitialPrecision(Node pInitialNode) {
    assert(pInitialNode != null);

    CFANode lInitialCFANode = pInitialNode.getCFANode();

    if (!(lInitialCFANode instanceof CFAFunctionDefinitionNode)) {
      throw new UnsupportedOperationException();
    }

    // TODO this cast is ugly
    return (CompositePrecision)mCompositeCPA.getInitialPrecision((CFAFunctionDefinitionNode)lInitialCFANode);
  }

  public static CompositeElement createInitialElement(CFANode pInitialCFANode, AbstractElement pDataSpaceElement) {
    return createInitialElement(pInitialCFANode, Collections.singletonList(pDataSpaceElement));
  }

  public static CompositeElement createInitialElement(CFANode pInitialCFANode, List<AbstractElement> pDataSpaceComposites) {
    assert(pInitialCFANode != null);
    assert(pDataSpaceComposites != null);

    LinkedList<AbstractElement> lComposites = new LinkedList<AbstractElement>();
    lComposites.add(new LocationElement(pInitialCFANode));

    lComposites.addAll(pDataSpaceComposites);

    CompositeElement lInitialCompositeElement = new CompositeElement(lComposites, null);

    CallElement lInitialCallElement = new CallElement(pInitialCFANode.getFunctionName(), pInitialCFANode, lInitialCompositeElement);

    CallStack lInitialCallStack = new CallStack();
    lInitialCallStack.push(lInitialCallElement);
    lInitialCompositeElement.setCallStack(lInitialCallStack);

    return lInitialCompositeElement;
  }

  public static CompositeElement createInitialElement(Node pInitialNode) {
    assert(pInitialNode != null);

    if (!pInitialNode.getPredicates().isEmpty()) {
      // TODO implement support for predicates, i.e., the initial elements have to be
      // restricted according to the predicates in pInitialState.getPredicates()
      throw new UnsupportedOperationException("Predicates not supported currently!");
    }

    CFANode lInitialCFANode = pInitialNode.getCFANode();

    AlwaysTopTopElement lAlwaysTopTopElement = AlwaysTopTopElement.getInstance();
    ConcreteAnalysisTopElement lConcreteAnalysisTopElement = ConcreteAnalysisTopElement.getInstance();
    MustMayAnalysisElement lInitialMustMayAnalysisElement = new MustMayAnalysisElement(lConcreteAnalysisTopElement, lAlwaysTopTopElement);

    //LocationElement lInitialLocationElement = new LocationElement(lInitialCFANode);

    //LinkedList<AbstractElement> lAbstractElements = new LinkedList<AbstractElement>();

    //lAbstractElements.add(lInitialLocationElement);
    //lAbstractElements.add(lInitialMustMayAnalysisElement);

    return createInitialElement(lInitialCFANode, lInitialMustMayAnalysisElement);

    /*CompositeElement lInitialCompositeElement = new CompositeElement(lAbstractElements, null);

    CallElement lInitialCallElement = new CallElement(lInitialCFANode.getFunctionName(), lInitialCFANode, lInitialCompositeElement);

    CallStack lInitialCallStack = new CallStack();
    lInitialCallStack.push(lInitialCallElement);
    lInitialCompositeElement.setCallStack(lInitialCallStack);

    return lInitialCompositeElement;*/
  }

  /*public static CompositeElement createNextElement(Node pNextNode) {
    assert(pNextNode != null);

    if (!pNextNode.getPredicates().isEmpty()) {
      // TODO implement support for predicates, i.e., the initial elements have to be
      // restricted according to the predicates in pInitialState.getPredicates()
      throw new UnsupportedOperationException("Predicates not supported currently!");
    }

    CFANode lCFANode = pNextNode.getCFANode();

    AlwaysTopTopElement lAlwaysTopTopElement = AlwaysTopTopElement.getInstance();
    ConcreteAnalysisTopElement lConcreteAnalysisTopElement = ConcreteAnalysisTopElement.getInstance();
    MustMayAnalysisElement lInitialMustMayAnalysisElement = new MustMayAnalysisElement(lConcreteAnalysisTopElement, lAlwaysTopTopElement);

    LocationElement lInitialLocationElement = new LocationElement(lCFANode);

    LinkedList<AbstractElement> lAbstractElements = new LinkedList<AbstractElement>();

    lAbstractElements.add(lInitialLocationElement);
    lAbstractElements.add(lInitialMustMayAnalysisElement);

    CompositeElement lNextElement = new CompositeElement(lAbstractElements, null);

    // TODO: special treatment of last element? ... stack only containing call to main function
    // ... stack predicates? ... would make sense

    //CallElement lInitialCallElement = new CallElement(lInitialCFANode.getFunctionName(), lInitialCFANode, lInitialCompositeElement);
    //CallStack lInitialCallStack = new CallStack();
    //lInitialCallStack.push(lInitialCallElement);
    //lInitialCompositeElement.setCallStack(lInitialCallStack);

    return lNextElement;
  }*/

  private FeasibilityWitness generateWitness(LinkedList<Waypoint> lWaypoints) {
    assert(lWaypoints != null);

    // TODO implement

    return new FeasibilityWitness();
  }
}
