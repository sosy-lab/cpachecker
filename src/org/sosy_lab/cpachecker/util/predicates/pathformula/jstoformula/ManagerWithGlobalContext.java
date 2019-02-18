/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.smt.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.RationalFormulaManagerView;

/**
 * Provides unpacked properties of the global context object for shorter access.
 *
 * @see GlobalManagerContext
 */
abstract class ManagerWithGlobalContext {
  final GlobalManagerContext gctx;

  final FormulaEncodingOptions options;
  final JSFormulaEncodingOptions jsOptions;
  final LogManagerWithoutDuplicates logger;

  @SuppressWarnings("WeakerAccess")
  final ShutdownNotifier shutdownNotifier; // not used yet, but exists for future use

  final AnalysisDirection direction;
  final TypedVariableValues typedVarValues;
  final TypeTags typeTags;
  final TypedValueManager tvmgr;
  final Ids<JSFunctionDeclaration> functionDeclarationIds;
  final FunctionScopeManager functionScopeManager;
  final ObjectIdFormulaManager objIdMgr;
  final StringFormulaManager strMgr;
  final ValueConverterManager valConv;
  final GlobalDeclarationManager globalDeclarationsMgr;
  final JSNumberFormulaManager numMgr;

  final FormulaManagerView fmgr;
  final ArrayFormulaManagerView afmgr;
  final BooleanFormulaManagerView bfmgr;
  final FunctionFormulaManagerView ffmgr;
  final FloatingPointFormulaManagerView fpfmgr;
  final IntegerFormulaManagerView ifmgr;
  final RationalFormulaManagerView rfmgr;
  final BitvectorFormulaManagerView bitVecMgr;

  ManagerWithGlobalContext(final GlobalManagerContext pContext) {
    gctx = pContext;

    options = pContext.options;
    jsOptions = pContext.jsOptions;
    logger = pContext.logger;
    shutdownNotifier = pContext.shutdownNotifier;
    direction = pContext.direction;
    typedVarValues = pContext.typedVarValues;
    typeTags = pContext.typeTags;
    tvmgr = pContext.tvmgr;
    functionDeclarationIds = pContext.functionDeclarationIds;
    functionScopeManager = pContext.functionScopeManager;
    objIdMgr = pContext.objIdMgr;
    strMgr = pContext.strMgr;
    valConv = pContext.valConv;
    globalDeclarationsMgr = pContext.globalDeclarationsMgr;
    numMgr = pContext.numMgr;

    fmgr = pContext.fmgr;
    afmgr = pContext.afmgr;
    bfmgr = pContext.bfmgr;
    ffmgr = pContext.ffmgr;
    fpfmgr = pContext.fpfmgr;
    ifmgr = pContext.ifmgr;
    rfmgr = pContext.rfmgr;
    bitVecMgr = pContext.bitVecMgr;
  }
}
