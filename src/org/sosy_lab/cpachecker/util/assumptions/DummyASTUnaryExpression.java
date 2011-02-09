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
package org.sosy_lab.cpachecker.util.assumptions;

import org.eclipse.cdt.core.dom.ast.ASTSignatureUtil;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;

/**
 * Hack!!!
 * @author g.theoduloz
 */
public class DummyASTUnaryExpression extends IASTUnaryExpression {

  public DummyASTUnaryExpression(int op, IASTExpression op1) {
    super(null, null, null, op1, op);
  }

  @Override
  public String getRawSignature() {
    return ASTSignatureUtil.getUnaryOperatorString(this) + getOperand().getRawSignature();
  }

  @Override
  public String toString() {
    return getRawSignature();
  }
}
