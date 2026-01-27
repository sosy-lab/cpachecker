// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.util;

import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressOfExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryXorExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.CastExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.DivisionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.EqualsExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanOrEqualExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ModuloExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.MultiplicationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.NegationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.PointerExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftLeftExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftRightExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SubtractionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

public class SymbolicIdentifierRenamer implements SymbolicValueVisitor<SymbolicValue> {

  public Map<Long, Long> getIdentifierMap() {
    return identifierMap;
  }

  Map<Long, Long> identifierMap;
  SymbolicValueFactory svf;
  Set<SymbolicIdentifier> rename;

  public SymbolicIdentifierRenamer(
      Map<Long, Long> pIdentifierMap, Set<SymbolicIdentifier> pRenameSymbols) {
    identifierMap = pIdentifierMap;
    svf = SymbolicValueFactory.getInstance();
    rename = pRenameSymbols;
  }

  @Override
  public SymbolicValue visit(final SymbolicIdentifier pValue) {
    if (identifierMap.containsKey(pValue.getId())) {
      return new SymbolicIdentifier(
          identifierMap.get(pValue.getId()), pValue.getRepresentedLocation().orElse(null));
    }
    if (!rename.contains(pValue)) {
      return pValue;
    }
    SymbolicIdentifier newId = svf.newIdentifier(pValue.getRepresentedLocation().orElse(null));
    identifierMap.put(pValue.getId(), newId.getId());
    return newId;
  }

  @Override
  public SymbolicValue visit(final ConstantSymbolicExpression pExpression) {
    final Value containedValue = pExpression.getValue();
    if (containedValue instanceof SymbolicValue symVal) {
      return new ConstantSymbolicExpression(symVal.accept(this), pExpression.getType());
    } else {
      return pExpression;
    }
  }

  @Override
  public SymbolicValue visit(final AdditionExpression pExpression) {
    final SymbolicValue renamedLeft = pExpression.getOperand1().accept(this);
    final SymbolicValue renamedRight = pExpression.getOperand2().accept(this);
    return svf.add(
        (SymbolicExpression) renamedLeft,
        (SymbolicExpression) renamedRight,
        pExpression.getType(),
        pExpression.getCalculationType());
  }

  @Override
  public SymbolicValue visit(final SubtractionExpression pExpression) {
    final SymbolicValue renamedLeft = pExpression.getOperand1().accept(this);
    final SymbolicValue renamedRight = pExpression.getOperand2().accept(this);
    return svf.minus(
        (SymbolicExpression) renamedLeft,
        (SymbolicExpression) renamedRight,
        pExpression.getType(),
        pExpression.getCalculationType());
  }

  @Override
  public SymbolicValue visit(final MultiplicationExpression pExpression) {
    final SymbolicValue renamedLeft = pExpression.getOperand1().accept(this);
    final SymbolicValue renamedRight = pExpression.getOperand2().accept(this);
    return svf.multiply(
        (SymbolicExpression) renamedLeft,
        (SymbolicExpression) renamedRight,
        pExpression.getType(),
        pExpression.getCalculationType());
  }

  @Override
  public SymbolicValue visit(final DivisionExpression pExpression) {
    final SymbolicValue renamedLeft = pExpression.getOperand1().accept(this);
    final SymbolicValue renamedRight = pExpression.getOperand2().accept(this);
    return svf.divide(
        (SymbolicExpression) renamedLeft,
        (SymbolicExpression) renamedRight,
        pExpression.getType(),
        pExpression.getCalculationType());
  }

  @Override
  public SymbolicValue visit(final ModuloExpression pExpression) {
    final SymbolicValue renamedLeft = pExpression.getOperand1().accept(this);
    final SymbolicValue renamedRight = pExpression.getOperand2().accept(this);
    return svf.modulo(
        (SymbolicExpression) renamedLeft,
        (SymbolicExpression) renamedRight,
        pExpression.getType(),
        pExpression.getCalculationType());
  }

  @Override
  public SymbolicValue visit(final BinaryAndExpression pExpression) {
    final SymbolicValue renamedLeft = pExpression.getOperand1().accept(this);
    final SymbolicValue renamedRight = pExpression.getOperand2().accept(this);
    return svf.binaryAnd(
        (SymbolicExpression) renamedLeft,
        (SymbolicExpression) renamedRight,
        pExpression.getType(),
        pExpression.getCalculationType());
  }

  @Override
  public SymbolicValue visit(final BinaryNotExpression pExpression) {
    final SymbolicValue renamedOperand = pExpression.getOperand().accept(this);
    return svf.binaryNot((SymbolicExpression) renamedOperand, pExpression.getType());
  }

  @Override
  public SymbolicValue visit(final BinaryOrExpression pExpression) {
    final SymbolicValue renamedLeft = pExpression.getOperand1().accept(this);
    final SymbolicValue renamedRight = pExpression.getOperand2().accept(this);
    return svf.binaryOr(
        (SymbolicExpression) renamedLeft,
        (SymbolicExpression) renamedRight,
        pExpression.getType(),
        pExpression.getCalculationType());
  }

  @Override
  public SymbolicValue visit(final BinaryXorExpression pExpression) {
    final SymbolicValue renamedLeft = pExpression.getOperand1().accept(this);
    final SymbolicValue renamedRight = pExpression.getOperand2().accept(this);
    return svf.binaryXor(
        (SymbolicExpression) renamedLeft,
        (SymbolicExpression) renamedRight,
        pExpression.getType(),
        pExpression.getCalculationType());
  }

  @Override
  public SymbolicValue visit(final ShiftRightExpression pExpression) {
    final SymbolicValue renamedLeft = pExpression.getOperand1().accept(this);
    final SymbolicValue renamedRight = pExpression.getOperand2().accept(this);
    if (pExpression.isSigned()) {
      return svf.shiftRightSigned(
          (SymbolicExpression) renamedLeft,
          (SymbolicExpression) renamedRight,
          pExpression.getType(),
          pExpression.getCalculationType());
    }
    return svf.shiftRightUnsigned(
        (SymbolicExpression) renamedLeft,
        (SymbolicExpression) renamedRight,
        pExpression.getType(),
        pExpression.getCalculationType());
  }

  @Override
  public SymbolicValue visit(final ShiftLeftExpression pExpression) {
    final SymbolicValue renamedLeft = pExpression.getOperand1().accept(this);
    final SymbolicValue renamedRight = pExpression.getOperand2().accept(this);
    return svf.shiftLeft(
        (SymbolicExpression) renamedLeft,
        (SymbolicExpression) renamedRight,
        pExpression.getType(),
        pExpression.getCalculationType());
  }

  @Override
  public SymbolicValue visit(final LogicalNotExpression pExpression) {
    final SymbolicValue renamedOperand = pExpression.getOperand().accept(this);
    return svf.logicalNot((SymbolicExpression) renamedOperand, pExpression.getType());
  }

  @Override
  public SymbolicValue visit(final LessThanOrEqualExpression pExpression) {
    final SymbolicValue renamedLeft = pExpression.getOperand1().accept(this);
    final SymbolicValue renamedRight = pExpression.getOperand2().accept(this);
    return svf.lessThanOrEqual(
        (SymbolicExpression) renamedLeft,
        (SymbolicExpression) renamedRight,
        pExpression.getType(),
        pExpression.getCalculationType());
  }

  @Override
  public SymbolicValue visit(final LessThanExpression pExpression) {
    final SymbolicValue renamedLeft = pExpression.getOperand1().accept(this);
    final SymbolicValue renamedRight = pExpression.getOperand2().accept(this);
    return svf.lessThan(
        (SymbolicExpression) renamedLeft,
        (SymbolicExpression) renamedRight,
        pExpression.getType(),
        pExpression.getCalculationType());
  }

  @Override
  public SymbolicValue visit(final EqualsExpression pExpression) {
    final SymbolicValue renamedLeft = pExpression.getOperand1().accept(this);
    final SymbolicValue renamedRight = pExpression.getOperand2().accept(this);
    return svf.equal(
        (SymbolicExpression) renamedLeft,
        (SymbolicExpression) renamedRight,
        pExpression.getType(),
        pExpression.getCalculationType());
  }

  @Override
  public SymbolicValue visit(final LogicalOrExpression pExpression) {
    final SymbolicValue renamedLeft = pExpression.getOperand1().accept(this);
    final SymbolicValue renamedRight = pExpression.getOperand2().accept(this);
    return svf.logicalOr(
        (SymbolicExpression) renamedLeft,
        (SymbolicExpression) renamedRight,
        pExpression.getType(),
        pExpression.getCalculationType());
  }

  @Override
  public SymbolicValue visit(final LogicalAndExpression pExpression) {
    final SymbolicValue renamedLeft = pExpression.getOperand1().accept(this);
    final SymbolicValue renamedRight = pExpression.getOperand2().accept(this);
    return svf.logicalAnd(
        (SymbolicExpression) renamedLeft,
        (SymbolicExpression) renamedRight,
        pExpression.getType(),
        pExpression.getCalculationType());
  }

  @Override
  public SymbolicValue visit(final CastExpression pExpression) {
    final SymbolicValue renamedOperand = pExpression.getOperand().accept(this);
    return svf.cast(renamedOperand, pExpression.getType());
  }

  @Override
  public SymbolicValue visit(final PointerExpression pExpression) {
    final SymbolicValue renamedOperand = pExpression.getOperand().accept(this);
    return svf.pointer((SymbolicExpression) renamedOperand, pExpression.getType());
  }

  @Override
  public SymbolicValue visit(final AddressOfExpression pExpression) {
    final SymbolicValue renamedOperand = pExpression.getOperand().accept(this);
    return svf.addressOf((SymbolicExpression) renamedOperand, pExpression.getType());
  }

  @Override
  public SymbolicValue visit(final NegationExpression pExpression) {
    final SymbolicValue renamedOperand = pExpression.getOperand().accept(this);
    return svf.negate((SymbolicExpression) renamedOperand, pExpression.getType());
  }
}
