// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

public class AffineLoopRepresentation {
  private final ImmutableList<ImmutableList<Integer>> iteraionMatrix;
  private final ImmutableList<CIdExpression> variables;
  private final ImmutableList<Integer> iterationConstants;

  public AffineLoopRepresentation(ImmutableList<ImmutableList<Integer>> pIteraionMatrix, ImmutableList<CIdExpression> pVariables, ImmutableList<Integer> pIterationConstants) {
    iteraionMatrix = pIteraionMatrix;
    variables = pVariables;
    iterationConstants = pIterationConstants;
  }

  public List<? extends List<Integer>> getIteraionMatrix() {
    return iteraionMatrix;
  }

  public List<CIdExpression> getVariables() {
    return variables;
  }

  public List<Integer> getIterationConstants() {
    return iterationConstants;
  }

  public static class Builder {
    private ArrayList<ArrayList<Integer>> newMatrix;
    private List<CIdExpression> newVariables;
    private ArrayList<Integer> newConstant;

    private Builder() {
      newMatrix = new ArrayList<>();
      newVariables = new ArrayList<>();
      newConstant = new ArrayList<>();
    }

    public static Builder builder() {
      return new Builder();
    }

    public void addRow() {
      newMatrix.add(new ArrayList<>());
    }

    public void addElement(int index, int value) {
      newMatrix.get(index).add(value);
    }

    public void addVariable(CIdExpression pVariable) {
      newVariables.add(pVariable);
    }

    public void addConstant(int value) {
      newConstant.add(value);
    }

    public AffineLoopRepresentation build() {
      if (newMatrix.size() != newVariables.size() || newVariables.size() != newConstant.size())
        throw new IllegalStateException("Cannot build an AffineLoopRepresentation");
      ImmutableList.Builder<ImmutableList<Integer>> builder = ImmutableList.builder();
      for (int i = 0; i < newVariables.size(); i++) {
        ImmutableList.Builder<Integer> rowBuilder = ImmutableList.builder();
        for (Integer entry : newMatrix.get(i)) {
          rowBuilder.add(entry);
        }
        builder.add(rowBuilder.build());
      }
      return new AffineLoopRepresentation(builder.build(), ImmutableList.copyOf(newVariables), ImmutableList.copyOf(newConstant));
    }
  }

  public static AffineLoopRepresentation fromIterationMatrixMap(Map<CIdExpression, List<BigInteger>> pIterationMatrix) {
    Builder builder = Builder.builder();
    int counter = 0;
    for (Map.Entry<CIdExpression, List<BigInteger>> entry : pIterationMatrix.entrySet()) {
      builder.addRow();
      builder.addVariable(entry.getKey());
      for (BigInteger value : entry.getValue()) {
        if (Objects.equals(entry.getValue().getLast(), value)) {
          builder.addConstant(value.intValue());
          continue;
        }
        builder.addElement(counter, value.intValue());
      }
      counter++;
    }
    return builder.build();
  }

  public String printMatrix() {
    int d = iteraionMatrix.size();
    StringBuilder returnString = new StringBuilder("{");
    for (int i = 0; i < d; i++) {
      returnString.append("{");
      for (int j = 0; j < d; j++) {
        if (j == d - 1) {
          returnString.append(iteraionMatrix.get(i).get(j));
        } else {
          returnString.append(iteraionMatrix.get(i).get(j)).append(",");
        }
      }
      returnString.append("}");
      if (i != d - 1) {
        returnString.append(",");
      }
    }
    return returnString + "}";
  }
}
