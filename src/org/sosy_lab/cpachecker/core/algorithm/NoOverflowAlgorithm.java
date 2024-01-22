// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.common.log.LogManager;

public class NoOverflowAlgorithm implements Algorithm{
  private final LogManager logger;
  private final CFA cfa;
  private final List<StatementInformation> information = new ArrayList<>();
  private final List<String> allTemporaryValueList = new ArrayList<String>();
  private final List<String> temporaryValueList = new ArrayList<String>();

  public NoOverflowAlgorithm(
      final LogManager pLogger,
      final CFA pCFA) {
    logger = pLogger;
    cfa = pCFA;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    // cfa.getASTStructure();

    for (CFAEdge edge : cfa.edges()) {
      Optional<AAstNode> optionalAAstNode = edge.getRawAST();
      if (optionalAAstNode.isPresent()){
        AAstNode astNode = optionalAAstNode.get();

        if (astNode instanceof CReturnStatement) {
          Optional<CExpression> optionalCExpression = ((CReturnStatement) astNode).getReturnValue();
          if (optionalCExpression.isPresent()) {
            CExpression cExpression = optionalCExpression.get();
            if (cExpression instanceof CBinaryExpression) {
              handleBinaryExpression(edge, (CBinaryExpression) cExpression);
            }
          }
        } else if (astNode instanceof CAssignment) {
          ALeftHandSide leftHandSide = ((CAssignment) astNode).getLeftHandSide();
          ARightHandSide rightHandSide = ((CAssignment) astNode).getRightHandSide();
          if (leftHandSide.toASTString().contains("__CPAchecker_TMP_")) {
            allTemporaryValueList.add(astNode.toASTString());
          }

          if (rightHandSide instanceof CFunctionCallExpression) {
            List<CExpression> parameterExpressions = ((CFunctionCallExpression) rightHandSide).getParameterExpressions();
            for (CExpression expression : parameterExpressions) {
              if (expression instanceof  CBinaryExpression) {
                handleBinaryExpression(edge, (CBinaryExpression) expression);
              }
            }
          } else if (rightHandSide instanceof CBinaryExpression) {
            handleBinaryExpression(edge, (CBinaryExpression) rightHandSide);
          }
        }
      }
    }

    String path = "./output/AllOverflowInfos.txt";
    try (BufferedWriter writer = Files.newBufferedWriter(Path.of(path), StandardCharsets.UTF_8)) {

      if (!temporaryValueList.isEmpty()) {
        writer.write("number of temporary values:\n");
        writer.write(temporaryValueList.size()+"\n");
        writer.write("temporary values:\n");
        for (String s : temporaryValueList) {
          writer.write(s + "\n");
        }
      }

      for (StatementInformation s : information) {
        writer.write(s.listToString());
        logger.log(Level.INFO, s.listToString());
      }
    } catch (IOException pE) {
      throw new RuntimeException(pE);
    }
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  public void handleBinaryExpression(CFAEdge edge, CBinaryExpression expression) {
    // LineNumber
    int lineNumber = edge.getLineNumber();

    // subexpression
    List<List<String>> expressionList = new ArrayList<>();
    // data type
    List<List<String>> typeList = new ArrayList<>();
    getInformation(expression, expressionList, typeList, lineNumber);
    information.add(new StatementInformation(lineNumber, expressionList, typeList));
  }

  public void getInformation(CBinaryExpression operand, List<List<String>> expressionList, List<List<String>> typeList, int lineNumber) {
    List<String> binaryExpression = new ArrayList<>();

    CExpression left = operand.getOperand1();
    CExpression right = operand.getOperand2();
    String leftString = left.toASTString();
    String rightString = right.toASTString();

    for (String tv: allTemporaryValueList) {
      int flag = 0;
      String s = lineNumber + " " + tv.replace(" ", "");
      if (left.toASTString().contains("__CPAchecker_TMP_") && tv.contains(left.toASTString()) && !temporaryValueList.contains(s)) {
        temporaryValueList.add(s);
        flag++;
      }
      if (right.toASTString().contains("__CPAchecker_TMP_") && tv.contains(right.toASTString()) && !temporaryValueList.contains(s)) {
        temporaryValueList.add(s);
        flag++;
      }
      if (flag == 2){
        break;
      }
    }

    leftString = leftString.replace(" ", "");
    rightString = rightString.replace(" ", "");
    binaryExpression.add(leftString);
    binaryExpression.add(rightString);
    String operator = String.valueOf(operand.getOperator());
    binaryExpression.add(operator);
    expressionList.add(binaryExpression);

    if (left instanceof CBinaryExpression) {
      getInformation((CBinaryExpression) left, expressionList, typeList, lineNumber);
    } else {
      CType type = left.getExpressionType();
      typeList.add(Collections.singletonList(leftString + " " + type.toString()));
    }
    if (right instanceof CBinaryExpression) {
      getInformation((CBinaryExpression) right, expressionList, typeList, lineNumber);
    } else {
      CType type = right.getExpressionType();
      typeList.add(Collections.singletonList(rightString + " " + type.toString()));
    }
  }

  public static class StatementInformation implements Serializable {
    final private int codeLineNumber;
    final private int numOfOperator;
    final private List<List<String>> expressionList;
    final private List<List<String>> typeList;

    public StatementInformation(int pCodeLineNumber, List<List<String>> pExpressionList, List<List<String>> pTypeList) {
      codeLineNumber = pCodeLineNumber;
      numOfOperator = pExpressionList.size();
      expressionList = pExpressionList;
      typeList = pTypeList;
    }

    public String listToString(){
      StringBuilder resultBuilder = new StringBuilder();
      resultBuilder.append("code line number:\n");
      resultBuilder.append(codeLineNumber);
      resultBuilder.append("\n");
      resultBuilder.append("number of variables:\n");
      resultBuilder.append(typeList.size());
      resultBuilder.append("\n");
      resultBuilder.append("data type:\n");
      for (List<String> innerList : typeList) {
        for (String element : innerList) {
          resultBuilder.append(element).append("\n");
        }
      }
      resultBuilder.append("number of operators:\n");
      resultBuilder.append(numOfOperator);
      resultBuilder.append("\n");
      resultBuilder.append("expressions:\n");
      for (List<String> innerList : expressionList) {
        for (String element : innerList) {
          resultBuilder.append(element).append(" ");
        }
        resultBuilder.append("\n");
      }
      return resultBuilder.toString();
    }
  }
}
