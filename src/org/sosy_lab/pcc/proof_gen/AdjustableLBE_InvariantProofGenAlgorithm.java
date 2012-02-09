/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.pcc.proof_gen;

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.pcc.common.FormulaHandler;
import org.sosy_lab.pcc.common.Separators;


public class AdjustableLBE_InvariantProofGenAlgorithm extends InvariantProofGenAlgorithm{

  protected FormulaHandler fh;
  private HashSet<String> edges = new HashSet<String>();

  public AdjustableLBE_InvariantProofGenAlgorithm(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    fh = new FormulaHandler(pConfig, pLogger, whichProver);
  }

  @Override
  protected boolean addOperation(ARTElement pSource, ARTElement pTarget) {
    PredicateAbstractElement predicate =
        AbstractElements.extractElementByType(pSource,
            PredicateAbstractElement.class);
    if(predicate == null){return false;}
    //check if it is start of an edge
    if (predicate.isAbstractionElement()){
      // get abstract successors and add them to edges
      return addAbstractSuccesors(pSource);
    }
    return true;
  }

  private boolean addAbstractSuccesors(ARTElement pSource){
    if(pSource == null){return false;}
    CFANode node = extractLocation(pSource);;
    if(node == null){return false;}
    String sourceID = node.getNodeNumber()+Separators.commonSeparator;
    String edgeID;
    Stack<ARTElement> toVisit = new Stack<ARTElement>();
    toVisit.push(pSource);
    ARTElement current;
    PredicateAbstractElement predicate;
    while(!toVisit.isEmpty()){
      current = toVisit.pop();
      node = extractLocation(current);;
      if(node == null){return false;}
      edgeID = sourceID + node.getNodeNumber() + Separators.commonSeparator;
      for(ARTElement child:current.getChildren()){
        predicate = AbstractElements.extractElementByType(child, PredicateAbstractElement.class);
        if(predicate == null){return false;}
        if(predicate.isAbstractionElement() || child.isCovered()){
          node = extractLocation(child);;
          if(node == null){return false;}
          // add edge
          edges.add(edgeID+node.getNodeNumber()+Separators.commonSeparator);
        }else{
          toVisit.push(child);
        }
      }
    }
    return true;
  }

  @Override
  protected StringBuilder writeOperations() {
    StringBuilder builder = new StringBuilder();
    Iterator<String> it = edges.iterator();
    while(it.hasNext()){
      builder.append(it.next());
    }
    return builder;
  }

  @Override
  protected boolean addInvariant(ARTElement pNode, String pStack) {
    // if covered, this element is not part of proof, redirection to covering node
    if (pNode.isCovered()) { return true; }
    PredicateAbstractElement predicate =
        AbstractElements.extractElementByType(pNode,
            PredicateAbstractElement.class);
    if (predicate == null) { return false; }
    CFANode corresponding = extractLocation(pNode);
    //check if it is an abstraction element, otherwise nothing to do
    if (predicate.isAbstractionElement()) {
      StringBuilder builder =
          cfaNodeInvariants.get(corresponding.getNodeNumber());
      if (builder == null) {
        builder = new StringBuilder();
        cfaNodeInvariants.put(corresponding.getNodeNumber(), builder);
      }
      // add invariant and stack
      logger.log(Level.INFO, "Add another region description.");
      String invariant = getAbstraction(predicate);
      if (invariant == null || invariant.length() == 0) { return false; }
      builder.append(invariant + pStack + Separators.commonSeparator);
    }
    return true;
  }

  protected String getAbstraction(PredicateAbstractElement pPredicate) {
    String f =
        fh.removeIndicesStr(pPredicate.getAbstractionFormula().asFormula());
    if (f == null) { return null; }
    return f;
  }

}
