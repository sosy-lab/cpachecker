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
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;

public class TCFAEdge extends AssumeEdge {

  private static final long serialVersionUID = 5472749446453717391L;

  private final Set<AIdExpression> variablesToReset;

  public TCFAEdge(
      FileLocation pFileLocation,
      TCFANode pPredecessor,
      TCFANode pSuccessor,
      AExpression pGuard,
      Set<AIdExpression> pVariablesToReset) {
    super(
        getEdgeLabel(pGuard), pFileLocation, pPredecessor, pSuccessor, pGuard, true, false, false);

    variablesToReset = pVariablesToReset;
  }

  private static String getEdgeLabel(AExpression guard) {
    return guard.toASTString();
  }

  public Set<AIdExpression> getVariablesToReset() {
    return variablesToReset;
  }
}
