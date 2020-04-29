/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.model.timedautomata;

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;

public class TCFAEdge extends BlankEdge {

  private static final long serialVersionUID = 5472749446453717391L;

  private final TaVariableCondition guard;
  private final Set<TaIdExpression> variablesToReset;

  public TCFAEdge(
      FileLocation pFileLocation,
      TCFANode pPredecessor,
      TCFANode pSuccessor,
      TaVariableCondition pGuard,
      Set<TaIdExpression> pVariablesToReset) {
    super(getEdgeLabel(pGuard), pFileLocation, pPredecessor, pSuccessor, getEdgeLabel(pGuard));

    guard = pGuard;
    variablesToReset = pVariablesToReset;
  }

  private static String getEdgeLabel(TaVariableCondition guard) {
    return guard.toASTString();
  }

  public TaVariableCondition getGuard() {
    return guard;
  }

  public Set<TaIdExpression> getVariablesToReset() {
    return variablesToReset;
  }
}
