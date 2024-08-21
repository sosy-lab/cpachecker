// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Implementation of the fault localization techniques with distance metrics. Paper for Abstract
 * Distance Metric: "Explaining Abstract Counterexamples", by Alex Groce Paper for Control Flow
 * Distance Metric: Accurately Choosing Execution Runs for Software Fault Localization, by Tao Wang
 * Paper for Path Generation Technique: Automated Path Generation for Software Fault Localization,
 * by Tao Wang
 */
package org.sosy_lab.cpachecker.core.algorithm.explainer;
