// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Record for holding the information of a program transformation.
 */
public record ProgramTransformationInformation(
    SubCFA subCFA,
    ProgramTransformationRecovery programTransformationRecovery
) {}
