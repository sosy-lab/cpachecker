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
package org.sosy_lab.cpachecker.cpa.cpalien;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class SMGState implements AbstractState {
  private final CLangSMG heap;
  private final LogManager logger;

  public SMGState(LogManager pLogger){
    heap = new CLangSMG();

    logger = pLogger;

    performConsistencyCheck();
  }

  public SMGState(SMGState originalState){
    heap = new CLangSMG(originalState.heap);
    logger = originalState.logger;

    performConsistencyCheck();
  }

  void addStackObject(SMGObject obj){
    heap.addStackObject(obj);
    performConsistencyCheck();
  }

  public void addValue(int pValue) {
    heap.addValue(Integer.valueOf(pValue));
    performConsistencyCheck();
  }

  public SMGObject getObjectForVariable(CIdExpression pVariableName) {
    return heap.getObjectForVariable(pVariableName);
  }

  public void insertNewHasValueEdge(SMGEdgeHasValue pNewEdge) {
    heap.addHasValueEdge(pNewEdge);
    performConsistencyCheck();
  }

  public void performConsistencyCheck(){
    CLangSMGConsistencyVerifier.verifyCLangSMG(logger, heap);
  }

  public void visualize(String name){
    SMGPlotter.produceAsDotFile(heap, name, logger);
  }

  @Override
  public String toString(){
    return heap.toString();
  }

  public void addStackFrame(CFunctionDeclaration pFunctionDefinition) {
    heap.addStackFrame(pFunctionDefinition);
  }
}
