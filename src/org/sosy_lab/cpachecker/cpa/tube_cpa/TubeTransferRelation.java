package org.sosy_lab.cpachecker.cpa.tube_cpa;
// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

import static com.google.common.base.Preconditions.checkNotNull;


import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import netscape.javascript.JSObject;
import org.sosy_lab.common.JSON;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.tube_cpa.TubeState;
import org.sosy_lab.common.JSON.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.*;


import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;


public class TubeTransferRelation extends SingleEdgeTransferRelation {
    @Override
    public Collection<TubeState> getAbstractSuccessorsForEdge(
            AbstractState element, Precision prec, CFAEdge cfaEdge) {
        TubeState tubeState = (TubeState) element;
        TubeState tubeState2 = new TubeState(cfaEdge,tubeState.getAsserts(), null, tubeState.getErrorCounter(),tubeState.getFormulaManagerView());

        if (cfaEdge.getCode().contains("reach_error")&& cfaEdge.getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
            tubeState2.incrementErrorCounter();
            return ImmutableSet.of(tubeState2);
        }

        if(tubeState2.getAsserts().containsKey(cfaEdge.getLineNumber())) {
            BooleanFormula exp = tubeState2.getAssertAtLine(cfaEdge.getLineNumber(), false);
            BooleanFormula negExp = tubeState2.getAssertAtLine(cfaEdge.getLineNumber(), true);

            TubeState successor = new TubeState(cfaEdge,tubeState2.getAsserts(), exp, tubeState2.getErrorCounter(), tubeState2.getFormulaManagerView());
            TubeState successor2 = new TubeState(cfaEdge,tubeState2.getAsserts(), negExp, tubeState2.getErrorCounter(),tubeState2.getFormulaManagerView());
            return ImmutableSet.of(successor, successor2);
        }else{
            return ImmutableSet.of(tubeState2);
        }
    }
}

