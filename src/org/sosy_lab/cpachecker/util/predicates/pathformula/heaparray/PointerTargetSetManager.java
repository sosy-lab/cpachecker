/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.pathformula.heaparray;

import java.math.BigInteger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTarget;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet.CompositeField;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.solver.api.BooleanFormula;

/**
 *
 */
public class PointerTargetSetManager {

  static boolean isFakeBaseType(final CType type) {
    return type instanceof CArrayType
        && ((CArrayType) type).getType() instanceof CVoidType;
  }

  static CType getFakeBaseType(int size) {
    return CTypeUtils.simplifyType(
        new CArrayType(false, false, CVoidType.VOID,
            new CIntegerLiteralExpression(FileLocation.DUMMY,
                CNumericTypes.SIGNED_CHAR, BigInteger.valueOf(size))));
  }

  private final ShutdownNotifier shutdownNotifier;
  private final FormulaEncodingWithPointerAliasingOptions options;
  private final FormulaManagerView formulaManager;
  private final BooleanFormulaManagerView bfmgr;
  private final FunctionFormulaManagerView ffmgr;
  private final TypeHandlerWithPointerAliasing typeHandler;

  public PointerTargetSetManager(
      final FormulaEncodingWithPointerAliasingOptions options,
      final FormulaManagerView formulaManager,
      final TypeHandlerWithPointerAliasing typeHandler,
      final ShutdownNotifier shutdownNotifier) {
    this.options = options;
    this.formulaManager = formulaManager;
    bfmgr = this.formulaManager.getBooleanFormulaManager();
    ffmgr = this.formulaManager.getFunctionFormulaManager();
    this.typeHandler = typeHandler;
    this.shutdownNotifier = shutdownNotifier;
  }

  /**
   * The method is used to speed up {@code sizeof} computation by caching sizes
   * of declared composite types.
   *
   * @param type
   * @return
   */
  int getSize(CType type) {
    return typeHandler.getSizeof(type);
  }

  BooleanFormula getNextBaseAddressInequality(final String newBase,
      final PersistentSortedMap<String, CType> bases,
      final String lastBase) {
    return null;
  }

  /**
   * Recursively adds pointer targets for every used (tracked) (sub)field of the
   * newly allocated base.
   *
   * Note: the recursion doesn't proceed on unused (untracked) (sub)fields.
   *
   * @param base the name of the newly allocated base variable
   * @param currentType type of the allocated base or the next added pointer
   *                    target
   * @param containerType either {@code null} or the type of the innermost
   *                      container of the next added pointer target
   * @param properOffset either {@code 0} or the offset of the next added
   *                     pointer target in its innermost container
   * @param containerOffset either {@code 0} or the offset of the innermost
   *                        container (relative to the base adddress)
   * @param targets The list of targets where the new targets should be added to
   * @param fields The set of "shared" fields that are accessed directly via
   *               pointers.
   * @return The targets map together with all the added targets.
   */
  @CheckReturnValue
  PersistentSortedMap<String, PersistentList<PointerTarget>> addToTargets(
      final String base,
      final CType currentType,
      final @Nullable CType containerType,
      final int properOffset,
      final int containerOffset,
      PersistentSortedMap<String, PersistentList<PointerTarget>> targets,
      final PersistentSortedMap<CompositeField, Boolean> fields) {
    return null;
  }

}
