/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.visitors;

import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Addition;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Constant;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Exponent;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Multiplication;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Variable;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.visitors.Visitor.NoException;

import java.util.HashSet;
import java.util.Set;

public class CollectVariablesVisitor implements Visitor<Set<String>, NoException> {

  @Override
  public Set<String> visit(Addition pAdd) throws NoException {
    Set<String> variables = pAdd.getSummand1().accept(this);
    variables.addAll(pAdd.getSummand2().accept(this));
    return variables;
  }

  @Override
  public Set<String> visit(Multiplication pMult) throws NoException {
    Set<String> variables = pMult.getFactor1().accept(this);
    variables.addAll(pMult.getFactor2().accept(this));
    return variables;
  }

  @Override
  public Set<String> visit(Exponent pExp) throws NoException {
    return visit(pExp.getBasis());
  }

  @Override
  public Set<String> visit(Variable pVar) throws NoException {
    Set<String> variable = new HashSet<>();
    variable.add(pVar.getIdentifier());
    return variable;
  }

  @Override
  public Set<String> visit(Constant pC) throws NoException {
    return new HashSet<>();
  }

}
