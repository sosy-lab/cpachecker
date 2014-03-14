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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view.wrapping;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;


public class WrappingFormulaManager extends FormulaManagerView {

  public WrappingFormulaManager(LoadManagers pLoadManagers, FormulaManager pBaseManager, Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    super(pLoadManagers, pBaseManager, config, pLogger);
  }

  public WrappingFormulaManager(FormulaManager wrapped, Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    this(DEFAULTMANAGERS, wrapped, config, pLogger);
  }

  private static LoadManagers DEFAULTMANAGERS =
    new LoadManagers() {
      @Override
      public BitvectorFormulaManagerView wrapManager(BitvectorFormulaManager pManager) {
        return new WrappingBitvectorFormulaManagerView(pManager);
      }

      @Override
      public NumeralFormulaManagerView<IntegerFormula, IntegerFormula> wrapIntegerManager(NumeralFormulaManager<IntegerFormula, IntegerFormula> pManager) {
        return new WrappingIntegerFormulaManagerView(pManager);
      }

      @Override
      public NumeralFormulaManagerView<NumeralFormula, RationalFormula> wrapRationalManager(NumeralFormulaManager<NumeralFormula, RationalFormula> pManager) {
        return new WrappingRationalFormulaManagerView(pManager);
      }

      @Override
      public BooleanFormulaManagerView wrapManager(BooleanFormulaManager pManager, UnsafeFormulaManager pUnsafe) {
        return new WrappingBooleanFormulaManagerView(pManager, pUnsafe);
      }
      @Override
      public FunctionFormulaManagerView wrapManager(FunctionFormulaManager pManager) {
        return new WrappingFunctionFormulaManagerView(pManager);
      }
    };
}
