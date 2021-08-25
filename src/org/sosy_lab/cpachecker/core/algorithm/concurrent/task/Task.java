// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task;

import java.util.concurrent.Callable;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;

/**
 * {@link Task} provides a common interface for all classes which implement subtasks of concurrent
 * analysis.
 */
public interface Task extends Callable<AlgorithmStatus> {}
