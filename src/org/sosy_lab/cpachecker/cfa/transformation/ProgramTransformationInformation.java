// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Record for representing the information needed for performing a program transformation.
 *
 * @param entryNode CFANode
 * @param exitNode CFANode
 * @param programTransformation ProgramTransformationEnum
 */
public record ProgramTransformationInformation(CFANode entryNode, CFANode exitNode, ProgramTransformationEnum programTransformation) {}
