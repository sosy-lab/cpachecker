// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;

public class SerializeBooleanFormulaVisitor
        implements BooleanFormulaVisitor<CompoundInterval, String> {
    private final NumeralFormulaVisitor<CompoundInterval, String> numeralVisitor;

    public SerializeBooleanFormulaVisitor(
            NumeralFormulaVisitor<CompoundInterval, String> pNumeralVisitor) {
        numeralVisitor = pNumeralVisitor;
    }

    @Override
    public String visit(Equal<CompoundInterval> pEqual) {
        return "("+pEqual.getOperand1().accept(numeralVisitor)
                + " : "
                + pEqual.getOperand2().accept(numeralVisitor)+ ")";
    }

    @Override
    public String visit(LessThan<CompoundInterval> pLessThan) {
        return "("+pLessThan.getOperand1() + " < " + pLessThan.getOperand2()+")";
    }

    @Override
    public String visit(LogicalAnd<CompoundInterval> pAnd) {
        return "("+pAnd.getOperand1().accept(this) + " && " + pAnd.getOperand2().accept(this)+")";
    }

    @Override
    public String visit(LogicalNot<CompoundInterval> pNot) {
        return "(!(" + pNot.getNegated().accept(this)+"))";
    }

    @Override
    public String visitFalse() {
        return "(false)";
    }

    @Override
    public String visitTrue() {
        return "(true)";
    }
}

