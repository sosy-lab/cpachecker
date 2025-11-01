// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

public interface K3CommandVisitor<R, X extends Exception> {
  R visit(K3AnnotateTagCommand pK3AnnotateTagCommand) throws X;

  R visit(K3AssertCommand pK3AssertCommand) throws X;

  R visit(K3DeclareConstCommand pK3DeclareConstCommand) throws X;

  R visit(K3DeclareFunCommand pK3DeclareFunCommand) throws X;

  R visit(K3DeclareSortCommand pK3DeclareSortCommand) throws X;

  R visit(K3GetWitnessCommand pK3GetWitnessCommand) throws X;

  R visit(K3ProcedureDefinitionCommand pK3ProcedureDefinitionCommand) throws X;

  R visit(K3SetLogicCommand pK3SetLogicCommand) throws X;

  R visit(K3SetOptionCommand pK3SetOptionCommand) throws X;

  R visit(K3VariableDeclarationCommand pK3VariableDeclarationCommand) throws X;

  R visit(K3VerifyCallCommand pK3VerifyCallCommand) throws X;
}
