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
package org.sosy_lab.cpachecker.fllesh.fql.fllesh.cpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnEdge;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.mustmay.SimpleMustMayAnalysisCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.reachability.Query;


public class QueryTransferRelationTest {

  @Before
  public void tearDown() {
    /* XXX: Currently this is necessary to pass all assertions. */
    org.sosy_lab.cpachecker.core.CPAchecker.logger = null;
  }

  @Test
  public void test_01() throws CPATransferException, InvalidConfigurationException {
    LogManager lLogManager = new LogManager(new Configuration(Collections.<String, String>emptyMap()));
    SimpleMustMayAnalysisCPA lMustMayCPA = new SimpleMustMayAnalysisCPA(lLogManager);

    AbstractElement lMustBottomElement = lMustMayCPA.getAbstractDomain().getBottomElement().getMustElement();

    // TODO implement
    Query lQuery = null;

    QueryTransferRelation lTransferRelation = new QueryTransferRelation(lQuery, QueryTopElement.getInstance(), QueryBottomElement.getInstance(), lMustMayCPA.getTransferRelation(), lMustBottomElement);

    Collection<? extends AbstractElement> lSuccessors = lTransferRelation.getAbstractSuccessors(QueryBottomElement.getInstance(), null, null);

    assertTrue(lSuccessors.isEmpty());
  }

  @Test
  public void test_02() throws CPATransferException, InvalidConfigurationException {
    LogManager lLogManager = new LogManager(new Configuration(Collections.<String, String>emptyMap()));
    SimpleMustMayAnalysisCPA lMustMayCPA = new SimpleMustMayAnalysisCPA(lLogManager);

    AbstractElement lMustBottomElement = lMustMayCPA.getAbstractDomain().getBottomElement().getMustElement();

    // TODO implement
    Query lQuery = null;

    QueryTransferRelation lTransferRelation = new QueryTransferRelation(lQuery, QueryTopElement.getInstance(), QueryBottomElement.getInstance(), lMustMayCPA.getTransferRelation(), lMustBottomElement);

    Collection<? extends AbstractElement> lSuccessors = lTransferRelation.getAbstractSuccessors(QueryTopElement.getInstance(), null, null);

    assertEquals(lSuccessors.size(), 1);
    assertEquals(lSuccessors.iterator().next(), QueryTopElement.getInstance());
  }

  @Test
  public void test_03() throws CPATransferException, InvalidConfigurationException {
    LogManager lLogManager = new LogManager(new Configuration(Collections.<String, String>emptyMap()));
    SimpleMustMayAnalysisCPA lMustMayCPA = new SimpleMustMayAnalysisCPA(lLogManager);

    AbstractElement lMustBottomElement = lMustMayCPA.getAbstractDomain().getBottomElement().getMustElement();

    // TODO implement
    Query lQuery = null;

    QueryTransferRelation lTransferRelation = new QueryTransferRelation(lQuery, QueryTopElement.getInstance(), QueryBottomElement.getInstance(), lMustMayCPA.getTransferRelation(), lMustBottomElement);

    QueryStandardElement lElement = new QueryStandardElement(0, true, 1, false, lMustMayCPA.getAbstractDomain().getTopElement());

    CFANode lNode = new CFANode(10);

    CFAEdge lCFAEdge = InternalSelfLoop.getOrCreate(lNode);

    @SuppressWarnings("unused")
    Collection<? extends AbstractElement> lSuccessors = lTransferRelation.getAbstractSuccessors(lElement, null, lCFAEdge);
  }

  @Test
  public void test_04() throws CPATransferException, InvalidConfigurationException {
    LogManager lLogManager = new LogManager(new Configuration(Collections.<String, String>emptyMap()));
    SimpleMustMayAnalysisCPA lMustMayCPA = new SimpleMustMayAnalysisCPA(lLogManager);

    AbstractElement lMustBottomElement = lMustMayCPA.getAbstractDomain().getBottomElement().getMustElement();

    // TODO implement
    Query lQuery = null;

    QueryTransferRelation lTransferRelation = new QueryTransferRelation(lQuery, QueryTopElement.getInstance(), QueryBottomElement.getInstance(), lMustMayCPA.getTransferRelation(), lMustBottomElement);

    QueryStandardElement lElement = new QueryStandardElement(0, true, 1, false, lMustMayCPA.getAbstractDomain().getTopElement());

    CFAEdge lCFAEdge = new ReturnEdge("bla", 0, null, null);

    @SuppressWarnings("unused")
    Collection<? extends AbstractElement> lSuccessors = lTransferRelation.getAbstractSuccessors(lElement, null, lCFAEdge);
  }

  @Test
  public void test_05() throws CPATransferException, InvalidConfigurationException {
    LogManager lLogManager = new LogManager(new Configuration(Collections.<String, String>emptyMap()));
    SimpleMustMayAnalysisCPA lMustMayCPA = new SimpleMustMayAnalysisCPA(lLogManager);

    AbstractElement lMustBottomElement = lMustMayCPA.getAbstractDomain().getBottomElement().getMustElement();

    // TODO implement
    Query lQuery = null;

    QueryTransferRelation lTransferRelation = new QueryTransferRelation(lQuery, QueryTopElement.getInstance(), QueryBottomElement.getInstance(), lMustMayCPA.getTransferRelation(), lMustBottomElement);

    QueryStandardElement lElement = new QueryStandardElement(0, true, 1, false, lMustMayCPA.getAbstractDomain().getTopElement());

    CFAEdge lCFAEdge = new BlankEdge("blub", 0, null, null);

    @SuppressWarnings("unused")
    Collection<? extends AbstractElement> lSuccessors = lTransferRelation.getAbstractSuccessors(lElement, null, lCFAEdge);
  }

}
