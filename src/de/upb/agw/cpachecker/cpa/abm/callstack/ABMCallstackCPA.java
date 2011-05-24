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
package de.upb.agw.cpachecker.cpa.abm.callstack;

import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;

import de.upb.agw.cpachecker.cpa.abm.predicate.ABMPTransferRelation;

/**
 * Implements a <code>CallstackCPA</code> that replaces the normal <code>CallstackTransferRelation</code> by a
 * <code>ABMCTransferRelation</code>.
 * @see de.upb.agw.cpachecker.cpa.abm.callstack.ABMCTransferRelation
 * @author dwonisch
 *
 */
public class ABMCallstackCPA extends CallstackCPA {
  
  private ABMCTransferRelation transfer;
  
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ABMCallstackCPA.class);
  }
  
  public ABMCallstackCPA() {
    transfer = new ABMCTransferRelation();
  }
  
  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }  
  
  public void setPredicateTransferRelation(ABMPTransferRelation predicateTransferRelation) {
    transfer.setPredicateTransferRelation(predicateTransferRelation);
  }
}