// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.core.algorithm.instrumentation.NormalLoopInfo;

/**
 * Represents a container for normal loop information(for, while, do-while, and goto loop).
 *
 * @param loopLocation the line number where the loop is located
 * @param liveVariablesAndTypes the mapping from variable names used, but not declared, in the loop
 *     to their types
 */
public record NormalLoopInfo(
    int loopLocation, 
    ImmutableMap<String, String> liveVariablesAndTypes, 
    VariableBoundInfo boundInfo
) {}
