// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import java.util.Arrays;
import java.util.List;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.UFManager;

public class FunctionFormulaManagerView extends BaseManagerView implements UFManager {

  private final UFManager manager;

  FunctionFormulaManagerView(FormulaWrappingHandler pWrappingHandler, UFManager pManager) {
    super(pWrappingHandler);
    manager = checkNotNull(pManager);
  }

  @Immutable
  private static class ReplaceUninterpretedFunctionDeclaration<T extends Formula>
      implements FunctionDeclaration<T> {

    private final FunctionDeclaration<?> wrapped;
    private final FormulaType<T> returnType;
    private final ImmutableList<FormulaType<?>> argumentTypes;

    ReplaceUninterpretedFunctionDeclaration(
        FunctionDeclaration<?> wrapped,
        FormulaType<T> pReturnType,
        List<FormulaType<?>> pArgumentTypes) {
      this.wrapped = checkNotNull(wrapped);
      returnType = pReturnType;
      argumentTypes = ImmutableList.copyOf(pArgumentTypes);
    }

    @Override
    public int hashCode() {
      return 17 + wrapped.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ReplaceUninterpretedFunctionDeclaration)) {
        return false;
      }
      ReplaceUninterpretedFunctionDeclaration<?> other =
          (ReplaceUninterpretedFunctionDeclaration<?>) obj;

      return wrapped.equals(other.wrapped);
    }

    @Override
    public FunctionDeclarationKind getKind() {
      return FunctionDeclarationKind.UF;
    }

    @Override
    public String getName() {
      return wrapped.getName();
    }

    @Override
    public FormulaType<T> getType() {
      return returnType;
    }

    @Override
    public List<FormulaType<?>> getArgumentTypes() {
      return argumentTypes;
    }

    @Override
    public String toString() {
      return String.format("ReplacementUF(%s :: %s --> %s)", wrapped, argumentTypes, returnType);
    }
  }

  @Override
  public <T extends Formula> FunctionDeclaration<T> declareUF(
      String pName, FormulaType<T> pReturnType, List<FormulaType<?>> pArgs) {
    List<FormulaType<?>> newArgs = unwrapType(pArgs);
    FormulaType<?> ret = unwrapType(pReturnType);
    FunctionDeclaration<?> func = manager.declareUF(pName, ret, newArgs);

    return new ReplaceUninterpretedFunctionDeclaration<>(func, pReturnType, pArgs);
  }

  @Override
  public <T extends Formula> FunctionDeclaration<T> declareUF(
      String pName, FormulaType<T> pReturnType, FormulaType<?>... pArgs) {
    return declareUF(pName, pReturnType, Arrays.asList(pArgs));
  }

  public <T extends Formula> T declareAndCallUninterpretedFunction(
      String pName, int idx, FormulaType<T> pReturnType, List<Formula> pArgs) {
    String name = FormulaManagerView.makeName(pName, idx);
    return declareAndCallUF(name, pReturnType, pArgs);
  }

  public <T extends Formula> T declareAndCallUninterpretedFunction(
      String pName, int pIdx, FormulaType<T> pReturnType, Formula... pArgs) {
    return declareAndCallUninterpretedFunction(pName, pIdx, pReturnType, Arrays.asList(pArgs));
  }

  @Override
  public <T extends Formula> T declareAndCallUF(
      String name, FormulaType<T> pReturnType, List<Formula> pArgs) {
    List<FormulaType<?>> argTypes = transformedImmutableListCopy(pArgs, this::getFormulaType);
    FunctionDeclaration<T> func = declareUF(name, pReturnType, argTypes);
    return callUF(func, pArgs);
  }

  @Override
  public <T extends Formula> T declareAndCallUF(
      String pName, FormulaType<T> pReturnType, Formula... pArgs) {
    return declareAndCallUF(pName, pReturnType, Arrays.asList(pArgs));
  }

  @Override
  public <T extends Formula> T callUF(
      FunctionDeclaration<T> pFuncType, List<? extends Formula> pArgs) {

    ReplaceUninterpretedFunctionDeclaration<T> rep =
        (ReplaceUninterpretedFunctionDeclaration<T>) pFuncType;

    Formula f = manager.callUF(rep.wrapped, unwrap(pArgs));

    return wrap(pFuncType.getType(), f);
  }

  @Override
  public <T extends Formula> T callUF(FunctionDeclaration<T> pFuncType, Formula... pArgs) {
    return callUF(pFuncType, Arrays.asList(pArgs));
  }
}
