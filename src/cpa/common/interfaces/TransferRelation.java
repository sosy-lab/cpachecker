/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.common.interfaces;

import java.util.List;

import cfa.objectmodel.CFAEdge;

import exceptions.CPATransferException;
import cpa.common.interfaces.AbstractElement;
import exceptions.CPAException;

/**
 * Interface for transfer relations.
 * The instance of the relation is used to calculate the post operation
 * @author erkan
 *
 */
public interface TransferRelation
{
  /**
   * Transfers the state to the next abstract state. For example if the analysis in on node 3 and it
   * will proceed to node 4, there is an edge from node 3 to 4.
   * Element is the abstract element on node 3 and it is copied as the element on node 4. The copied element
   * will be updated by processing the edge.
   * @param element abstract element on current state
   * @param cfaEdge the edge from one location of CFA to the other
   * @return updated abstract element
   */
  public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge, Precision precision)
    throws CPATransferException;

  /** Gets all successors of the current element. This method returns a non-null list
   * only if the abstract element contains traversal information such as a CFA or CFG
   * @param element abstract element on current state
   * @return list of all successors of the current state
   * @throws CPAException if the element does not contain any traversal information such as nodes
   * and edges on CFA.
   */
  public List<AbstractElementWithLocation> getAllAbstractSuccessors (AbstractElementWithLocation element, Precision precision)
    throws CPAException, CPATransferException;
  
  /**
   * Updates an abstract element with information from the abstract elements of
   * other CPAs. An implementation of this method should only modify the
   * abstract element of the domain it belongs to. 
   * @param element abstract element of the current domain
   * @param otherElements list of abstract elements of all domains
   * @param cfaEdge the current edge of the CFA 
   * @param precision
   */
  public void strengthen (AbstractElement element,
                          List<AbstractElement> otherElements,
                          CFAEdge cfaEdge,
                          Precision precision);
}
