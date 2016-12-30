/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.templates;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.java_smt.api.Formula;

/**
 * Variable tracked by {@link Template}
 */
public interface TVariable {

  /**
   * @return Full variable name, analogous
   * to {@link CIdExpression#getName()}.
   */
  String getName();

  /**
   * Convert to formula representation with a given context information,
   * using given dependencies.
   * @param contextFormula Used for {@link org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap}
   *                       and {@link org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet}.
   *
   * @return Instantiated formula.
   */
  Formula toFormula(
      PathFormulaManager pfmgr,
      PathFormula contextFormula);

  /**
   * Associated type.
   */
  CSimpleType getType();
}
