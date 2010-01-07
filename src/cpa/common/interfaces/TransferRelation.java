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

import java.util.Collection;
import java.util.List;

import cfa.objectmodel.CFAEdge;

import exceptions.CPATransferException;
import exceptions.TransferTimeOutException;
import cpa.common.interfaces.AbstractElement;

/**
 * Interface for transfer relations.
 * The instance of the relation is used to calculate the post operation
 * @author erkan
 *
 */
public interface TransferRelation {

  /**
   * Gets all successors of the current element. If cfaEdge is null, all edges
   * of the CFA may be checked if they lead to successors, otherwise only the
   * specified edge should be handled.
   * @param element abstract element with current state
   * @param cfaEdge null or an edge of the CFA
   * @return list of all successors of the current state (may be empty)
   */
  public Collection<? extends AbstractElement> getAbstractSuccessors(AbstractElement element, Precision precision, CFAEdge cfaEdge)
    throws CPATransferException, TransferTimeOutException;
  
  /**
   * Updates an abstract element with information from the abstract elements of
   * other CPAs. An implementation of this method should only modify the
   * abstract element of the domain it belongs to. 
   * @param element abstract element of the current domain
   * @param otherElements list of abstract elements of all domains
   * @param cfaEdge the current edge of the CFA 
   * @param precision
   * @return A new abstract element which should replace the old one, or null for no change.
   */
  public AbstractElement strengthen (AbstractElement element,
                                     List<AbstractElement> otherElements,
                                     CFAEdge cfaEdge,
                                     Precision precision) throws CPATransferException;
}
