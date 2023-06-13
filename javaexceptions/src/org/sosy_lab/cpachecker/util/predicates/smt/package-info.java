// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Extensions of the pure {@link org.sosy_lab.java_smt.api.FormulaManager} interface and its related
 * interfaces that make it easier to use by client code. This package can be used regardless of
 * which SMT solver is the backend.
 *
 * <p>The most important feature of this package is to replace an SMT theory with another one,
 * simulating the semantics of the replaced theory with other theories. This can be used to allow
 * working with {@link org.sosy_lab.java_smt.api.BitvectorFormula} even if the solver does not
 * support the theory of bitvectors. Bitvectors will then be approximated with rationals or
 * integers.
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.util.predicates.smt;
