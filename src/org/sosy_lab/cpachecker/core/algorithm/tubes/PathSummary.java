// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.tubes;

import java.util.List;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

public interface PathSummary<T> {

  T summarize(List<ARGPath> paths);
}
