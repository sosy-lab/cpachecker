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

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import javax.annotation.Nullable;

/**
 * Exception thrown when a CPA cannot handle some code attached to a CFAEdge
 * because it uses features that are unsupported.
 */
public class UnsupportedCodeException extends UnrecognizedCodeException {

  private static final long serialVersionUID = -7693635256672813804L;

  private @Nullable ARGState parentState = null;

  public UnsupportedCodeException(String msg, CFAEdge edge, @Nullable AAstNode astNode) {
    super("Unsupported feature", msg, edge, astNode);
  }

  public UnsupportedCodeException(String msg, CFAEdge cfaEdge) {
    this(msg, cfaEdge, null);
  }

  public void setParentState(ARGState pParentState) {
    Preconditions.checkState(parentState == null);
    parentState = pParentState;
  }

  public @Nullable ARGState getParentState() {
    return parentState;
  }
}
