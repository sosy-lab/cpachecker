/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.constraints;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.IdentifierReplacer;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.EqualsExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LessThanExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LessThanOrEqualExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LogicalNotExpression;

/**
 * Visitor for replacing {@link SymbolicIdentifier}s occurring in {@link Constraint}s with
 * a new given value.
 */
public class ConstraintIdentifierReplacer implements ConstraintVisitor<Constraint> {

  private IdentifierReplacer replacer;

  public ConstraintIdentifierReplacer(SymbolicIdentifier pToReplace, Value pNewValue, MachineModel pMachineModel, LogManagerWithoutDuplicates pLogger) {
    replacer = new IdentifierReplacer(pToReplace, pNewValue, pMachineModel, pLogger);
  }

  @Override
  public Constraint visit(EqualsExpression pConstraint) {
    return (Constraint) replacer.visit(pConstraint);
  }

  @Override
  public Constraint visit(LessThanExpression pConstraint) {
    return (Constraint) replacer.visit(pConstraint);
  }

  @Override
  public Constraint visit(LessThanOrEqualExpression pConstraint) {
    return (Constraint) replacer.visit(pConstraint);
  }

  @Override
  public Constraint visit(LogicalNotExpression pConstraint) {
    return (Constraint) replacer.visit(pConstraint);
  }
}
