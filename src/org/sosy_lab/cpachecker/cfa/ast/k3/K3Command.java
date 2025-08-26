// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serializable;

public sealed interface K3Command extends Serializable
    permits K3AnnotateTagCommand,
        K3GetCounterexampleCommand,
        K3GetProofCommand,
        K3ProcedureDefinitionCommand,
        K3VariableDeclarationCommand,
        VerifyCallCommand {}
