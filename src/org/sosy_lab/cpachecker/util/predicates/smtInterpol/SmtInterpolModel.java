/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.smtInterpol;

import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.Model.AssignableTerm;

import com.google.common.collect.ImmutableMap;

import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;

public class SmtInterpolModel {

  static Model createSmtInterpolModel(Script script, SmtInterpolFormulaManager fmgr) {
    ImmutableMap.Builder<AssignableTerm, Object> model = ImmutableMap.builder();
    Term modelFormula = script.getTheory().TRUE;

    // TODO this is only an empty class. add much code

    throw new InternalError("unimlemented feature: SMTINTERPOLMODEL");

    //return new Model(model.build(), new SmtInterpolFormula(modelFormula, script));
  }

}
