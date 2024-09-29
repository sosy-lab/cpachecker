package org.sosy_lab.cpachecker.cpa.tube_cpa;
// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
public class TubeState implements AbstractQueryableState, Partitionable, Serializable,
        FormulaReportingState {
    private final ImmutableMap<Integer,BooleanFormula> asserts;
    private final BooleanFormula booleanExp;
    private final CFAEdge cfaEdge;
    private int  error_counter = 0;
    public CtoFormulaConverter converter;
    FormulaManagerView formulaManagerView;
    BooleanFormulaManager booleanFormulaManager;
    public TubeState(CFAEdge pCFAEdge,ImmutableMap<Integer, BooleanFormula> pAssert, BooleanFormula exp, int pError_counter, FormulaManagerView pFormulaManagerView){
        this.cfaEdge = pCFAEdge;
        this.asserts = pAssert;
        this.booleanExp = exp;
        this.error_counter = pError_counter;
        this.formulaManagerView = pFormulaManagerView;
        booleanFormulaManager = formulaManagerView.getBooleanFormulaManager();
    }
    public BooleanFormula getAssertAtLine(int lineNumber, boolean negate){
        BooleanFormula f = asserts.get(lineNumber);
        if (negate){
            return booleanFormulaManager.not(f);
        }
        return f;
    }
    public ImmutableMap<Integer, BooleanFormula> getAsserts() {
        return this.asserts;
    }
    public BooleanFormula getBooleanExp() {
        return booleanExp;
    }
    public int getErrorCounter(){
        return this.error_counter;
    }
    public FormulaManagerView getFormulaManagerView() {
        return formulaManagerView;
    }

    public void incrementErrorCounter(){
        this.error_counter += 1;
    }
    public CFAEdge getCfaEdge(){
        return this.cfaEdge;
    }
    @Override
    public String getCPAName() {
        return TubeCPA.class.getSimpleName();
    }
    @Override
    public @Nullable Object getPartitionKey() {
        return this;
    }
    @Override
    public String toString() {
        return "TubeState: {" +
                "Asserts: " + asserts + "Boolean Expression: " + booleanExp + "Error Counter: " +
                error_counter + '}';
    }

    @Override
    public BooleanFormula getFormulaApproximation(FormulaManagerView manager) {
        BooleanFormulaManager bfmgr = manager.getBooleanFormulaManager();
        if (booleanExp == null){
            return bfmgr.makeTrue();
        }else {
            return booleanExp;
        }
    }
}
