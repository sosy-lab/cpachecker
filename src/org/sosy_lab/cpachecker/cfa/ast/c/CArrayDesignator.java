/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;


public class CArrayDesignator extends CADesignator{

  private final IAExpression subscriptExpression;
  private final CIADesignator arrayDesignator;

  public CArrayDesignator(final FileLocation pFileLocation,
                          final CExpression pSubscriptExpression,
                          final CIADesignator pArrayDesignator) {
     super(pFileLocation);
     subscriptExpression = pSubscriptExpression;
     arrayDesignator = pArrayDesignator;
  }

  public CIADesignator getArrayDesignator() {
    return arrayDesignator;
  }

  public CExpression getSubscriptExpression() {
    return (CExpression) subscriptExpression;
  }

  @Override
  public String toASTString() {
    return arrayDesignator.toASTString() + "[" + getSubscriptExpression().toASTString() + "]";
  }

  @Override
  public String toParenthesizedASTString() {
    return toASTString();
  }

  @Override
  public <R, X extends Exception> R accept(CDesignatorVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }
}
