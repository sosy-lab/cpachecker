// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task;

import com.google.common.collect.Table;
import java.util.Map;

/**
 * {@link Task} preprocessing with {@link Task#preprocess(Table, Map)} can determine that the data
 * with which a task was created has become outdated. In this case, the task is to be discarded.
 * {@link TaskValidity} represents this verdict, with {@link TaskValidity#VALID} indicating that the
 * task shall still get executed, and {@link TaskValidity#INVALID} requesting its cancellation.
 */
enum TaskValidity {
  VALID,
  INVALID
}
