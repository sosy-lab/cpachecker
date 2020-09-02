// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * The Numeric CPA uses numeric domains which are offered by the ELINA and the Apron library.
 *
 * <p>Unlike the {@link org.sosy_lab.cpachecker.cpa.apron.ApronCPA} the numeric cpa aims to be
 * usable with all numeric domains, which implement the Apron interface or the ELINA interface. This
 * is done by using the org.sosy_lab.numericdomains interface.
 *
 * <p>Available domains at the moment include the polyhedra, octagon and zone (named box in Apron)
 * domain.
 */
package org.sosy_lab.cpachecker.cpa.numeric;
