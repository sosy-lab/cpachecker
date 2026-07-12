// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.matheclipse.core.interfaces.IExpr;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;

public class AffineLoopClosedFormRepresentation {
  private final ImmutableMap<CIdExpression, ImmutableList<RowSummand>> closedForm;

  private AffineLoopClosedFormRepresentation(Map<CIdExpression, ImmutableList<RowSummand>> pClosedForm) {
    closedForm = ImmutableMap.copyOf(pClosedForm);
  }

  private AffineLoopClosedFormRepresentation(List<List<RowSummand>> pMatrix, List<CIdExpression> pVariables) {
    ImmutableMap.Builder<CIdExpression, ImmutableList<RowSummand>> builder = ImmutableMap.builder();
    for (int i = 0; i < pMatrix.size(); i++) {
      builder.put(pVariables.get(i), ImmutableList.copyOf(pMatrix.get(i)));
    }
    closedForm = builder.build();
  }

  public Set<CIdExpression> getVariables() {
    return closedForm.keySet();
  }

  public ImmutableMap<CIdExpression, ImmutableList<RowSummand>> getClosedForm() {
    return closedForm;
  }

  public static class Builder {
    private List<CIdExpression> newVariables;
    private List<List<List<Summand>>> currentMatrix;
    private List<List<List<Summand>>> tmpMatrix;

    public Builder(List<CIdExpression> pVariables) {
      // set up the square matrix of Summand lists
      currentMatrix = new ArrayList<>(pVariables.size());
      for (int i = 0; i < pVariables.size(); i++) {
        List<List<Summand>> newRow = new ArrayList<>(pVariables.size());
        for (int j = 0; j < pVariables.size(); j++) {
          newRow.add(new ArrayList<>());
        }
        currentMatrix.add(newRow);
      }
      newVariables = pVariables;
    }

    public List<Summand> getSummands(int i, int j) {
      return currentMatrix.get(i).get(j);
    }

    public void addSummand(Summand  pSummand, CIdExpression pAssignedVariable, CIdExpression pVariable) {
      if (newVariables.contains(pAssignedVariable) && newVariables.contains(pVariable)) {
        addSummand(pSummand, newVariables.indexOf(pAssignedVariable), newVariables.indexOf(pVariable));
      }
    }

    public void addSummand(Summand  pSummand, int i, int j) {
      currentMatrix.get(i).get(j).add(pSummand);
    }

    public void setSummands(List<Summand> pSummands, int i, int j) {
      currentMatrix.get(i).set(j, pSummands);
    }

    public void initTmpMatrix() {
      tmpMatrix = new ArrayList<>(newVariables.size());
      for (int i = 0; i < newVariables.size(); i++) {
        List<List<Summand>> newRow = new ArrayList<>(newVariables.size());
        for (int j = 0; j < newVariables.size(); j++) {
          newRow.add(new ArrayList<>());
        }
        tmpMatrix.add(newRow);
      }
    }

    public void setTmpSummands(List<Summand> pSummands, int i, int j) {
      tmpMatrix.get(i).set(j, pSummands);
    }

    public void setTmpMatrix() {
      currentMatrix = tmpMatrix;
    }

    public AffineLoopClosedFormRepresentation build() {
      // multiply the currentMatrix with the newVariables array
      List<List<RowSummand>> newMatrix = new ArrayList<>(newVariables.size());
      for (int i = 0; i < newVariables.size(); i++) {
        ArrayList<RowSummand> row = new ArrayList<>();
        for (int j = 0; j < newVariables.size(); j++) {
          for (Summand summand : currentMatrix.get(i).get(j)) {
            if (!summand.coeff().isZero()) {
              row.add(new RowSummand(
                  summand.coeff(),
                  newVariables.get(j),
                  summand.power(),
                  summand.lambda()));
            }
          }
        }
        newMatrix.add(row);
      }

      return new AffineLoopClosedFormRepresentation(newMatrix, newVariables);
    }
  }

  public AffineLoopClosedFormRepresentation withoutVariable(CIdExpression pVariable) {
    ImmutableMap.Builder<CIdExpression, ImmutableList<RowSummand>> builder = ImmutableMap.builder();
    for (Entry<CIdExpression, ImmutableList<RowSummand>> entry : closedForm.entrySet()) {
      if (entry.getKey() != pVariable) {
        ImmutableList.Builder<RowSummand> rowSummands = ImmutableList.builder();
        for (RowSummand summand : entry.getValue()) {
          if (summand.variable != pVariable) {
            rowSummands.add(summand);
          } else {
            rowSummands.add(new RowSummand(summand.coeff(), null, summand.power(), summand.lambda()));
          }
        }
        builder.put(entry.getKey(), rowSummands.build());
      }
    }

    return new AffineLoopClosedFormRepresentation(builder.build());
  }

  private CIdExpression getPow() {
    ImmutableList.Builder<CParameterDeclaration> parameters = ImmutableList.builder();
    parameters.add(new CParameterDeclaration(FileLocation.DUMMY, CNumericTypes.DOUBLE, "base"));
    parameters.add(new CParameterDeclaration(FileLocation.DUMMY, CNumericTypes.DOUBLE, "exponent"));
    CSimpleDeclaration declaration = new CFunctionDeclaration(
        FileLocation.DUMMY,
        CFunctionType.functionTypeWithReturnType(CNumericTypes.DOUBLE),
        "pow",
        parameters.build(),
        ImmutableSet.of()
    );

    return new CIdExpression(
        FileLocation.DUMMY,
        declaration
    );
  }

  /**
   * Representation of a summand without variable: coeff * n^power * lambda^n
   * @param coeff
   * @param power
   * @param lambda
   */
  public record Summand (
      IExpr coeff,
      int power,
      IExpr lambda
  ) {}

  /**
   * Representation of a summand with the corresponding variable: coeff * variable * n^power * lambda^n
   * @param coeff
   * @param variable
   * @param power
   * @param lambda
   */
  public record RowSummand (
      IExpr coeff,
      CIdExpression variable,
      int power,
      IExpr lambda
  ) {}

}
