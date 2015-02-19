/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.symbolic;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Exception signalizing the maximum amount of symbolic values for a location is reached.
 */
public class SymbolicBoundReachedException extends UnrecognizedCodeException {
  public SymbolicBoundReachedException(String msg1, @Nullable String msg2,
      @Nullable CFAEdge edge, @Nullable AAstNode astNode) {
    super(msg1, msg2, edge, astNode);
  }

  public SymbolicBoundReachedException(String msg2, CFAEdge edge, AAstNode astNode) {
    super(msg2, edge, astNode);
  }

  public SymbolicBoundReachedException(String msg2, CFAEdge edge) {
    super(msg2, edge);
  }

  public SymbolicBoundReachedException(String msg2, AAstNode astNode) {
    super(msg2, astNode);
  }
}
