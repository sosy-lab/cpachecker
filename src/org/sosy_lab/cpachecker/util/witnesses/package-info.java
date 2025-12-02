// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package provides some common functionality for handling witnesses.
 *
 * <p>Witnesses are used to represent either correctness proofs or counterexamples for software
 * verification tasks.
 *
 * <p>The main necessity of this package is that irrespective of the format of the witness (e.g.,
 * GraphML, YAML, etc.) there is a common way to analyze the internal representation of CPAchecker
 * i.e. the ARG (Abstract Reachability Graph), or counterexamples.
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.util.witnesses;
