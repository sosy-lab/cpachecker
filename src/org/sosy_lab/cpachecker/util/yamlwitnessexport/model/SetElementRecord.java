// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

/**
 * A correctness witness contains multiple sets of entries, this interface is used to represent
 * which entries/records can be exported inside a set of a correctness witness.
 */
public sealed interface SetElementRecord permits FunctionContractRecord, InvariantEntry {}
