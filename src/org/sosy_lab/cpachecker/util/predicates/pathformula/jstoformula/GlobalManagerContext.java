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

/** State shared between all (formula) managers in this package. */
class GlobalManagerContext {
  final FormulaEncodingOptions options;
  final JSFormulaEncodingOptions jsOptions;
  final LogManagerWithoutDuplicates logger;
  final ShutdownNotifier shutdownNotifier;
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

  GlobalManagerContext(
      final FormulaEncodingOptions pOptions,
      final JSFormulaEncodingOptions pJSOptions,
      final LogManagerWithoutDuplicates pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final AnalysisDirection pDirection,
      final FormulaManagerView pFmgr) {
    options = pOptions;
    jsOptions = pJSOptions;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    direction = pDirection;
    fmgr = pFmgr;

    afmgr = fmgr.getArrayFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    ffmgr = fmgr.getFunctionFormulaManager();
    fpfmgr = fmgr.getFloatingPointFormulaManager();
    ifmgr = fmgr.getIntegerFormulaManager();
    rfmgr = fmgr.getRationalFormulaManager();
    bitVecMgr = fmgr.getBitvectorFormulaManager();

    numMgr = new JSNumberFormulaManager(pJSOptions.useNaN, bfmgr, fpfmgr);
    typedVarValues = new TypedVariableValues(ffmgr);
    typeTags = new TypeTags(fmgr.getIntegerFormulaManager());
    objIdMgr = new ObjectIdFormulaManager(fmgr);
    tvmgr = new TypedValueManager(pFmgr, typeTags, objIdMgr.getNullObjectId());
    functionDeclarationIds = new Ids<>();
    functionScopeManager = new FunctionScopeManager();
    strMgr = new StringFormulaManager(pFmgr, pJSOptions.maxFieldNameCount);
    valConv = new ValueConverterManager(typedVarValues, typeTags, tvmgr, strMgr, fmgr, numMgr);
    globalDeclarationsMgr = new GlobalDeclarationManager();
  }
}
