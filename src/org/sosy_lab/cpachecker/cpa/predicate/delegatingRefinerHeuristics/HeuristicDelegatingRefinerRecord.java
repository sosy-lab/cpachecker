// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import org.sosy_lab.cpachecker.core.interfaces.Refiner;

/**
 * Each heuristic for the DelegatingRefiner is stored as a fixed together with a suitable refiner.}
 */
public record HeuristicDelegatingRefinerRecord(
    DelegatingRefinerHeuristic pHeuristic, Refiner pRefiner) {}
