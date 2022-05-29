// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

/**
 * The categories regarding the analysis type of which every CoverageMeasureTypes belongs to. The
 * case ANALYSIS_INDEPENDENT means that it is suitable for any kind of analysis. The case
 * PREDICATE_ANALYSIS means that it is suitable only for predicate analysis.
 */
public enum CoverageMeasureAnalysisCategory {
  ANALYSIS_INDEPENDENT,
  PREDICATE_ANALYSIS
}
