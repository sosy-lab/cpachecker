/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.invariants.templates;

import java.util.List;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

public class TemplateFalse extends TemplateConstraint {

  public TemplateFalse() {}

  @Override
  public boolean isTrue() {
    return false;
  }

  @Override
  public boolean isFalse() {
    return true;
  }

  public static boolean isInstance(Object obj) {
    TemplateFalse F = new TemplateFalse();
    return F.getClass().isInstance(obj);
  }

  @Override
  public String toString(VariableWriteMode vwm) {
    return "false";
  }

  @Override
  public Formula translate(FormulaManager fmgr) {
  	return fmgr.makeFalse();
  }

  @Override
  public List<TemplateFormula> extractAtoms(boolean sAE, boolean cO) {
    List<TemplateFormula> atoms = new Vector<TemplateFormula>();
  	atoms.add(this);
  	return atoms;
  }

}