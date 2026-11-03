// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibAnyType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibFunctionType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public final class SvLibFunctionDeclaration extends AFunctionDeclaration
    implements SvLibDeclaration {
  @Serial private static final long serialVersionUID = -7637229289026207373L;

  /** Where the meaning of the function comes from. */
  private enum Interpretation {
    /** An operator of a theory of the SMT-LIB logic, e.g. bvadd. */
    LOGIC_BUILTIN,
    /** A procedure of the program. */
    PROCEDURE,
    /** A function of `declare-fun`, which has no definition. */
    UNINTERPRETED,
    /** A function of `define-fun`, which has been defined in the script. */
    DEFINED,
  }

  // Only set for an operator of SMT-LIB that is an indexed identifier
  private final @Nullable SmtLibIndexedSymbol indexedSymbol;

  private final Interpretation interpretation;

  public static SvLibFunctionDeclaration nondetFunctionWithReturnType(SvLibType pType) {
    return procedure(
        FileLocation.DUMMY,
        new SvLibFunctionType(ImmutableList.of(), pType),
        "#VERIFIER_nondet_" + pType,
        "#VERIFIER_nondet_" + pType,
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration mainFunctionDeclaration() {
    return procedure(
        FileLocation.DUMMY,
        new SvLibFunctionType(ImmutableList.of(), new SvLibAnyType()),
        "#VERIFIER_MAIN",
        "#VERIFIER_MAIN",
        ImmutableList.of());
  }

  /** An operator of a theory of the SMT-LIB logic, e.g. bvadd. */
  public static SvLibFunctionDeclaration logicBuiltinFunction(
      FileLocation pFileLocation,
      SvLibFunctionType pType,
      String pName,
      String pOrigName,
      List<SvLibParameterDeclaration> pParameters) {
    return new SvLibFunctionDeclaration(
        pFileLocation, pType, pName, pOrigName, pParameters, null, Interpretation.LOGIC_BUILTIN);
  }

  /**
   * An operator of a theory of the SMT-LIB logic that is an indexed identifier, e.g. (_ extract 7
   * 0).
   */
  public static SvLibFunctionDeclaration logicBuiltinFunction(
      FileLocation pFileLocation, SvLibFunctionType pType, SmtLibIndexedSymbol pIndexedSymbol) {
    return new SvLibFunctionDeclaration(
        pFileLocation,
        pType,
        pIndexedSymbol.toString(),
        pIndexedSymbol.toString(),
        ImmutableList.of(),
        pIndexedSymbol,
        Interpretation.LOGIC_BUILTIN);
  }

  /** A procedure of the program. */
  public static SvLibFunctionDeclaration procedure(
      FileLocation pFileLocation,
      SvLibFunctionType pType,
      String pName,
      String pOrigName,
      List<SvLibParameterDeclaration> pParameters) {
    return new SvLibFunctionDeclaration(
        pFileLocation, pType, pName, pOrigName, pParameters, null, Interpretation.PROCEDURE);
  }

  /** A function of `declare-fun`, which has no definition. */
  public static SvLibFunctionDeclaration uninterpretedFunction(
      FileLocation pFileLocation, SvLibFunctionType pType, String pName) {
    return new SvLibFunctionDeclaration(
        pFileLocation, pType, pName, pName, ImmutableList.of(), null, Interpretation.UNINTERPRETED);
  }

  /** A function of `define-fun`, which has been defined in the script. */
  public static SvLibFunctionDeclaration definedFunction(
      FileLocation pFileLocation,
      SvLibFunctionType pType,
      String pName,
      List<SvLibParameterDeclaration> pParameters) {
    return new SvLibFunctionDeclaration(
        pFileLocation, pType, pName, pName, pParameters, null, Interpretation.DEFINED);
  }

  private SvLibFunctionDeclaration(
      FileLocation pFileLocation,
      SvLibFunctionType pType,
      String pName,
      String pOrigName,
      List<SvLibParameterDeclaration> pParameters,
      @Nullable SmtLibIndexedSymbol pIndexedSymbol,
      Interpretation pInterpretation) {
    super(pFileLocation, pType, pName, pOrigName, pParameters);
    indexedSymbol = pIndexedSymbol;
    interpretation = pInterpretation;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ImmutableList<SvLibParameterDeclaration> getParameters() {
    return (ImmutableList<SvLibParameterDeclaration>) super.getParameters();
  }

  @Override
  public SvLibFunctionType getType() {
    return (SvLibFunctionType) super.getType();
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  public Optional<SmtLibIndexedSymbol> getIndexedSymbol() {
    return Optional.ofNullable(indexedSymbol);
  }

  public boolean isUninterpreted() {
    return interpretation == Interpretation.UNINTERPRETED;
  }

  public boolean hasBeenDefined() {
    return interpretation == Interpretation.DEFINED;
  }
}
