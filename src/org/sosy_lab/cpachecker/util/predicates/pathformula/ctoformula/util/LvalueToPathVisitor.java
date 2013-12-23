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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.util;

import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;


public class LvalueToPathVisitor
extends DefaultCExpressionVisitor<PersistentList<String>, RuntimeException> {

  @Override
  public PersistentList<String> visit(final CArraySubscriptExpression e) {
    return e.getArrayExpression().accept(this).withAll(e.getSubscriptExpression().accept(this));
  }

  @Override
  public PersistentList<String> visit(final CCastExpression e) {
    return e.getOperand().accept(this);
  }

  @Override
  public PersistentList<String> visit(final CComplexCastExpression e) {
   return e.getOperand().accept(this);
  }

  @Override
  public PersistentList<String> visit(final CFieldReference e) {
    return e.getFieldOwner().accept(this).with(e.toString());
  }

  @Override
  protected PersistentList<String> visitDefault(final CExpression e) {
    return PersistentLinkedList.of(e.toString());
  }
}
