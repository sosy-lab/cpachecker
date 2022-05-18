// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

/**
 * The categories of which every CoverageMeasureTypes belongs to. LocationBased looks on CFANodes.
 * LineBased looks on Source Code Lines.
 */
public enum CoverageMeasureCategory {
  LocationBased,
  LineBased,
  VariableBased,
}
