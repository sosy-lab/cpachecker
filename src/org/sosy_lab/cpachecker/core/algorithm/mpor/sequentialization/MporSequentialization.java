// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ProgramTransformation;

/**
 * The transformation of a concurrent program into an equivalent sequential one, which is analyzed
 * in its stead.
 *
 * @param originalCfa the CFA of the concurrent input program
 * @param mapping relates the elements of the sequentialization to the input program. It is absent
 *     if building the sequentialization failed, in which case the input program itself is analyzed.
 */
public record MporSequentialization(CFA originalCfa, Optional<SequentializationMapping> mapping)
    implements ProgramTransformation {}
