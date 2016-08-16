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
package org.sosy_lab.cpachecker.cpa.value.symbolic.util;

import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressOfExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinarySymbolicExpression;
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
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.UnarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

/**
 * Returns a <code>Collection</code> of all {@link MemoryLocation MemoryLocations} contained
 * in a {@link SymbolicValue}.
 */
public class MemoryLocationLocator implements SymbolicValueVisitor<Collection<MemoryLocation>> {

  private static final MemoryLocationLocator SINGLETON = new MemoryLocationLocator();

  private MemoryLocationLocator() {
    // DO NOTHING
  }

  public static MemoryLocationLocator getInstance() {
    return SINGLETON;
  }

  @Override
  public Collection<MemoryLocation> visit(SymbolicIdentifier pValue) {
    Optional<MemoryLocation> maybeLocation = pValue.getRepresentedLocation();

    if (maybeLocation.isPresent()) {
      return Collections.singleton(maybeLocation.get());
    } else {
      return Collections.emptySet();
    }
  }

  private Collection<MemoryLocation> visitExpression(final UnarySymbolicExpression pExpression) {
    Optional<MemoryLocation> maybeLocation = pExpression.getRepresentedLocation();

    if (maybeLocation.isPresent()) {
      return Collections.singleton(maybeLocation.get());
    } else {
      return pExpression.getOperand().accept(this);
    }
  }

  private Collection<MemoryLocation> visitExpression(final BinarySymbolicExpression pExpression) {
    Optional<MemoryLocation> maybeLocation = pExpression.getRepresentedLocation();

    if (maybeLocation.isPresent()) {
      return Collections.singleton(maybeLocation.get());
    } else {
      Collection<MemoryLocation> locations = new HashSet<>();

      locations.addAll(pExpression.getOperand1().accept(this));
      locations.addAll(pExpression.getOperand2().accept(this));

      return locations;
    }
  }

  @Override
  public Collection<MemoryLocation> visit(ConstantSymbolicExpression pExpression) {
    Optional<MemoryLocation> maybeLocation = pExpression.getRepresentedLocation();

    if (maybeLocation.isPresent()) {
      return Collections.singleton(maybeLocation.get());

    } else {
      Value innerValue = pExpression.getValue();

      if (innerValue instanceof SymbolicValue) {
        return ((SymbolicValue)innerValue).accept(this);
      } else {
        return Collections.emptySet();
      }
    }
  }

  @Override
  public Collection<MemoryLocation> visit(AdditionExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(SubtractionExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(MultiplicationExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(DivisionExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(ModuloExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(BinaryAndExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(BinaryNotExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(BinaryOrExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(BinaryXorExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(ShiftRightExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(ShiftLeftExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(LogicalNotExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(LessThanOrEqualExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(LessThanExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(EqualsExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(LogicalOrExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(LogicalAndExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(CastExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(PointerExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(AddressOfExpression pExpression) {
    return visitExpression(pExpression);
  }

  @Override
  public Collection<MemoryLocation> visit(NegationExpression pExpression) {
    return visitExpression(pExpression);
  }
}
