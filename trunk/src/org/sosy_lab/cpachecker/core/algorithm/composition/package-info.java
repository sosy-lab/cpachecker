// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Support to continue the analysis with a different analysis configuration after current
 * configuration stopped with an unknown result. A strategy selects the analysis configuration,
 * which is used to continue the analysis.
 *
 * <p>One example, for a composition is CoVeriTest (D. Beyer, M.-C. Jakobs: CoVeriTest: Cooperative
 * Verifier-Based Testing, FASE 2019.), which uses a circular combination of analyses for test
 * generation.
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.core.algorithm.composition;
