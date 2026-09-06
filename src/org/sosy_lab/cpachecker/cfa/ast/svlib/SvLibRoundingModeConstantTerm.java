// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
import org.sosy_lab.java_smt.api.FloatingPointRoundingMode;

/** One of the five rounding modes of the SMT-LIB theory of floating point numbers. */
public final class SvLibRoundingModeConstantTerm implements SvLibConstantTerm {

  @Serial private static final long serialVersionUID = 3070417830418913243L;
  private final FloatingPointRoundingMode value;
  private final FileLocation fileLocation;

  public SvLibRoundingModeConstantTerm(
      FloatingPointRoundingMode pValue, FileLocation pFileLocation) {
    value = pValue;
    fileLocation = pFileLocation;
  }

  /** The name of the given rounding mode in SMT-LIB. */
  public static String nameOf(FloatingPointRoundingMode pRoundingMode) {
    return switch (pRoundingMode) {
      case NEAREST_TIES_TO_EVEN -> "roundNearestTiesToEven";
      case NEAREST_TIES_AWAY -> "roundNearestTiesToAway";
      case TOWARD_POSITIVE -> "roundTowardPositive";
      case TOWARD_NEGATIVE -> "roundTowardNegative";
      case TOWARD_ZERO -> "roundTowardZero";
    };
  }

  /** The rounding mode with the given name in SMT-LIB, if the name denotes one. */
  public static FloatingPointRoundingMode fromName(String pName) {
    for (FloatingPointRoundingMode roundingMode : FloatingPointRoundingMode.values()) {
      if (nameOf(roundingMode).equals(pName)) {
        return roundingMode;
      }
    }
    throw new IllegalArgumentException(pName + " is not a rounding mode");
  }

  /** Does the given name denote a rounding mode in SMT-LIB? */
  public static boolean isRoundingMode(String pName) {
    for (FloatingPointRoundingMode roundingMode : FloatingPointRoundingMode.values()) {
      if (nameOf(roundingMode).equals(pName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public FloatingPointRoundingMode getValue() {
    return value;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibTermVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public @NonNull SvLibType getExpressionType() {
    return SvLibSmtLibPredefinedType.ROUNDING_MODE;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibExpressionVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return nameOf(value);
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return nameOf(value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibRoundingModeConstantTerm other && value == other.value;
  }
}
