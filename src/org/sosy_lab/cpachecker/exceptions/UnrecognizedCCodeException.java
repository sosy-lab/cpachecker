/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.exceptions;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.IAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Exception thrown when a CPA cannot handle some C code attached to a CFAEdge.
 */
public class UnrecognizedCCodeException extends UnrecognizedCodeException {

  private static final long serialVersionUID = -8319167530363457020L;

  protected UnrecognizedCCodeException(String msg1, @Nullable String msg2,
      @Nullable CFAEdge edge, @Nullable IAstNode astNode) {
    super(msg1, msg2, edge, astNode);
  }

  public UnrecognizedCCodeException(String msg2, CFAEdge edge, CAstNode astNode) {
    super(msg2, edge, astNode);
  }

  public UnrecognizedCCodeException(String msg2, CFAEdge edge) {
    super(msg2, edge);
  }

  public UnrecognizedCCodeException(String msg2, CAstNode astNode) {
    super(msg2, astNode);
  }
}
