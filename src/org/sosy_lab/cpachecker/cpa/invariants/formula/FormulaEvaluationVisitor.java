// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import java.util.Map;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Instances of implementing classes are invariants formula visitors used to evaluate the visited
 * formulae to values of their constant types.
 *
 * @param <T> the type of the constants used in the visited formulae.
 */
public interface FormulaEvaluationVisitor<T>
    extends ParameterizedNumeralFormulaVisitor<
            T, Map<? extends MemoryLocation, ? extends NumeralFormula<T>>, T>,
        ParameterizedBooleanFormulaVisitor<
            T, Map<? extends MemoryLocation, ? extends NumeralFormula<T>>, BooleanConstant<T>> {}
