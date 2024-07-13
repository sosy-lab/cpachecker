// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.tests;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class MPORTests {

  MPORAlgorithm algorithm;

  public MPORTests(MPORAlgorithm pAlgorithm) {
    algorithm = pAlgorithm;
  }
}
