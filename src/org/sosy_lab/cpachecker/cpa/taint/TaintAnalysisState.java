// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taint;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.defaults.NamedProperty;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.PseudoPartitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.refinement.ForgetfulState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;

public final class TaintAnalysisState
    implements AbstractQueryableState,
        Graphable,
        Targetable {

  private final boolean isTarget;

  private HashMap<String, Boolean> map = new HashMap<>();

  private final Set<Property> violations;

  private final LogManager logger;

  private final @Nullable MachineModel machineModel;

  public TaintAnalysisState(@Nullable MachineModel pMachineModel, LogManager plogger) {
    machineModel = pMachineModel;
    logger = plogger;
    map = new HashMap<>();
    isTarget = false;
    violations = Collections.emptySet();
  }

  private TaintAnalysisState(TaintAnalysisState state, LogManager plogger) {
    machineModel = state.machineModel;
    logger = plogger;
    map = new HashMap<>(state.map);
    isTarget = false;
    violations = Collections.emptySet();
  }
  private TaintAnalysisState(TaintAnalysisState state, LogManager plogger, Boolean target, String violation) {
    machineModel = state.machineModel;
    logger = plogger;
    map = new HashMap<>(state.map);
    isTarget = target;
    violations = NamedProperty.singleton(violation);
    logger.log(Level.INFO, "isTarget: "+isTarget+" | violations: "+violations);
  }

  public static TaintAnalysisState copyOf(TaintAnalysisState state) {
    return new TaintAnalysisState(state, state.logger);
  }
  public static TaintAnalysisState copyOf(TaintAnalysisState state, Boolean target, String violation) {
    return new TaintAnalysisState(state, state.logger, target, violation);
  }

  public void assignTaint(String var, Boolean tainted) {
    addToMap(var, tainted);
  }
  private void addToMap(final String value, final Boolean tainted) {
    map.put(value, tainted);
    // logger.log(Level.INFO, "Hinzugefügt: "+value+": "+tainted);
    // hashCode += (pMemLoc.hashCode() ^ valueAndType.hashCode());
  }

  public void change(String var, Boolean tainted) {
    changeMap(var, tainted);
  }
  private void changeMap(
      final String value, final Boolean tainted) {
    map.replace(value, tainted);
    logger.log(Level.FINEST, "Changed: "+value+" => "+tainted);
  }

  public Boolean getStatus(String var) {
    return getMap(var);
  }
  private Boolean getMap(final String value) {
    return map.get(value);
    // logger.log(Level.INFO, "Hinzugefügt: "+value+": "+tainted);
    // hashCode += (pMemLoc.hashCode() ^ valueAndType.hashCode());
  }
  
  public void remove(String var) {
    removeFromMap(var);
  }
  private void removeFromMap(final String value) {
    map.remove(value);
    // logger.log(Level.INFO, "Hinzugefügt: "+value+": "+tainted);
    // hashCode += (pMemLoc.hashCode() ^ valueAndType.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Tainted: [");
    for (Entry<String, Boolean> entry : map.entrySet()) {
      String key = entry.getKey();
      sb.append(" <");
      sb.append(key);
      sb.append(" = ");
      sb.append(entry.getValue());
      sb.append(">\n");
    }

    return sb.append("] size->  ").append(map.size()).toString();
  }

  /**
   * This method returns a more compact string representation of the state, compared to toString().
   *
   * @return a more compact string representation of the state
   */
  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("[");
    Joiner.on(", ").withKeyValueSeparator("=").appendTo(sb, map);
    sb.append("]");

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    pProperty = pProperty.trim();
    return true;

    // if (pProperty.startsWith("contains(")) {
    //   String varName = pProperty.substring("contains(".length(), pProperty.length() - 1);
    //   return this.constantsMap.containsKey(MemoryLocation.valueOf(varName));
    // } else {
    //   List<String> parts = Splitter.on("==").trimResults().splitToList(pProperty);
    //   if (parts.size() != 2) {
    //     ValueAndType value = this.constantsMap.get(MemoryLocation.valueOf(pProperty));
    //     if (value != null && value.getValue().isExplicitlyKnown()) {
    //       return value.getValue();
    //     } else {
    //       throw new InvalidQueryException(
    //           "The Query \""
    //               + pProperty
    //               + "\" is invalid. Could not find the variable \""
    //               + pProperty
    //               + "\"");
    //     }
    //   } else {
    //     return checkProperty(pProperty);
    //   }
    // }
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    // e.g. "x==5" where x is a variable. Returns if 5 is the associated constant
    List<String> parts = Splitter.on("==").trimResults().splitToList(pProperty);
    return true;

    // if (parts.size() != 2) {
    //   throw new InvalidQueryException(
    //       "The Query \""
    //           + pProperty
    //           + "\" is invalid. Could not split the property string correctly.");
    // } else {
    //   // The following is a hack
    //   ValueAndType val = this.constantsMap.get(MemoryLocation.valueOf(parts.get(0)));
    //   if (val == null) {
    //     return false;
    //   }
    //   Long value = val.getValue().asLong(CNumericTypes.INT);

    //   if (value == null) {
    //     return false;
    //   } else {
    //     try {
    //       return value == Long.parseLong(parts.get(1));
    //     } catch (NumberFormatException e) {
    //       // The command might contains something like "main::p==cmd" where the user wants to
    //       // compare the variable p to the variable cmd (nearest in scope)
    //       // perhaps we should omit the "main::" and find the variable via static scoping ("main::p"
    //       // is also not intuitive for a user)
    //       // TODO: implement Variable finding via static scoping
    //       throw new InvalidQueryException(
    //           "The Query \""
    //               + pProperty
    //               + "\" is invalid. Could not parse the long \""
    //               + parts.get(1)
    //               + "\"");
    //     }
    //   }
    // }
  }

  private static boolean startsWithIgnoreCase(String s, String prefix) {
    s = s.substring(0, prefix.length());
    return s.equalsIgnoreCase(prefix);
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    // Preconditions.checkNotNull(pModification);

    // // either "deletevalues(methodname::varname)" or "setvalue(methodname::varname:=1929)"
    // for (String statement : Splitter.on(';').trimResults().split(pModification)) {
    //   if (startsWithIgnoreCase(statement, "deletevalues(")) {
    //     if (!statement.endsWith(")")) {
    //       throw new InvalidQueryException(statement + " should end with \")\"");
    //     }

    //     MemoryLocation varName =
    //         MemoryLocation.valueOf(
    //             statement.substring("deletevalues(".length(), statement.length() - 1));

    //     if (contains(varName)) {
    //       forget(varName);
    //     } else {
    //       // varname was not present in one of the maps
    //       // i would like to log an error here, but no logger is available
    //     }

    //   } else if (startsWithIgnoreCase(statement, "setvalue(")) {
    //     if (!statement.endsWith(")")) {
    //       throw new InvalidQueryException(statement + " should end with \")\"");
    //     }

    //     String assignment = statement.substring("setvalue(".length(), statement.length() - 1);
    //     List<String> assignmentParts = Splitter.on(":=").trimResults().splitToList(assignment);

    //     if (assignmentParts.size() != 2) {
    //       throw new InvalidQueryException(
    //           "The Query \""
    //               + pModification
    //               + "\" is invalid. Could not split the property string correctly.");
    //     } else {
    //       String varName = assignmentParts.get(0);
    //       try {
    //         Value newValue = new NumericValue(Long.parseLong(assignmentParts.get(1)));
    //         this.assignConstant(varName, newValue);
    //       } catch (NumberFormatException e) {
    //         throw new InvalidQueryException(
    //             "The Query \""
    //                 + pModification
    //                 + "\" is invalid. Could not parse the long \""
    //                 + assignmentParts.get(1)
    //                 + "\"");
    //       }
    //     }
    //   }
    // }
  }

  @Override
  public String getCPAName() {
    return "TaintAnalysis";
  }

  @Override
  public boolean isTarget() {
    return isTarget;
  }

  public void addViolation(String violation) {
    violations.add(NamedProperty.create(violation));
  }

  @Override
  public @NonNull Set<Property> getViolatedProperties() throws IllegalStateException {
    return violations;
  }
}
