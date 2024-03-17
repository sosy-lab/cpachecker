// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import io.github.cvc5.Stat;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.eclipse.cdt.internal.core.dom.parser.c.CVariable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.common.log.LogManager;

/**
 * The NoOverflowAlgorithm traverse the CFA tree of the Program,
 * and extract information for transformation from overflow analysis to reachability analysis.
 */
public class NoOverflowAlgorithm implements Algorithm{
  private final LogManager logger;
  private final CFA cfa;
  private final List<StatementInformation> information = new ArrayList<>();
  private final List<String> allTemporaryValueList = new ArrayList<String>();
  private final List<String> temporaryValueList = new ArrayList<String>();
  private final List<String> allExpressionList = new ArrayList<String>();

  public NoOverflowAlgorithm(
      final LogManager pLogger,
      final CFA pCFA) {
    logger = pLogger;
    cfa = pCFA;
  }

  /**
   * This function traverse the CFA tree.
   *
   * @param reachedSet Input.
   * @return
   * @throws CPAException
   * @throws InterruptedException
   */
  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    for (CFAEdge edge : cfa.edges()) {
      Optional<AAstNode> optionalAAstNode = edge.getRawAST();
      if (optionalAAstNode.isPresent()){
        AAstNode astNode = optionalAAstNode.get();
        if (astNode instanceof CReturnStatement) {
          // return statement
          Optional<CExpression> optionalCExpression = ((CReturnStatement) astNode).getReturnValue();
          if (optionalCExpression.isPresent()) {
            CExpression cExpression = optionalCExpression.get();
            if (cExpression instanceof CBinaryExpression) {
              handleBinaryExpression(edge, (CBinaryExpression) cExpression);
            } else if (cExpression instanceof CUnaryExpression){
              handleUnaryExpression(edge, (CUnaryExpression) cExpression);
            }
          }
        } else if (astNode instanceof CAssignment) {
          // assignment
          ALeftHandSide leftHandSide = ((CAssignment) astNode).getLeftHandSide();
          ARightHandSide rightHandSide = ((CAssignment) astNode).getRightHandSide();
          if (leftHandSide.toASTString().contains("__CPAchecker_TMP_")) {
            // store temporary values
            allTemporaryValueList.add(edge.getLineNumber() + " " + astNode.toASTString().replace(" ", ""));
          }
          if (rightHandSide instanceof CFunctionCallExpression) {
            // function call expression
            List<CExpression> parameterExpressions = ((CFunctionCallExpression) rightHandSide).getParameterExpressions();
            for (CExpression expression : parameterExpressions) {
              if (expression instanceof CBinaryExpression) {
                handleBinaryExpression(edge, (CBinaryExpression) expression);
              }
              if (expression instanceof CUnaryExpression) {
                handleUnaryExpression(edge, (CUnaryExpression) expression);
              }
            }
          } else if (rightHandSide instanceof CBinaryExpression) {
            handleBinaryExpression(edge, (CBinaryExpression) rightHandSide);
          } else if (rightHandSide instanceof CUnaryExpression){
            handleUnaryExpression(edge, (CUnaryExpression) rightHandSide);
          }
        } else if (astNode instanceof CBinaryExpression) {
          // binary expression
          handleBinaryExpression(edge, (CBinaryExpression) astNode);
        } else if (astNode instanceof CFunctionCall) {
          // function call
          CFunctionCallExpression cFunctionCallExpression = ((CFunctionCall) astNode).getFunctionCallExpression();
          List<CExpression> parameterExpressions = cFunctionCallExpression.getParameterExpressions();
          for (CExpression expression : parameterExpressions) {
            if (expression instanceof CBinaryExpression) {
              handleBinaryExpression(edge, (CBinaryExpression) expression);
            }
            if (expression instanceof CUnaryExpression) {
              handleUnaryExpression(edge, (CUnaryExpression) expression);
            }
          }
        } else if (astNode instanceof CVariableDeclaration) {
          // variable declaration
          CInitializer cInitializer = ((CVariableDeclaration) astNode).getInitializer();
          if (cInitializer instanceof CInitializerExpression) {
            CExpression cExpression = ((CInitializerExpression) cInitializer).getExpression();
            if (cExpression instanceof CBinaryExpression) {
              handleBinaryExpression(edge, (CBinaryExpression) cExpression);
            }
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

      // merge information if they are on same line
      if (!information.isEmpty()) {
        int i = 0;
        do {
          int codeLineNumber1 = information.get(i).codeLineNumber;
          int index_01 = information.get(i).typeList.size();
          int index_02 = information.get(i).expressionList.size();
          for (int j = information.size() - 1; j > i; j--) {
            int codeLineNumber2 = information.get(j).codeLineNumber;
            if (codeLineNumber1 == codeLineNumber2) {
              // Merge properties of element at index i and index j
              information.get(i).numOfOperator += information.get(j).numOfOperator;
              information.get(i).typeList.addAll(index_01, information.get(j).typeList);
              information.get(i).expressionList.addAll(index_02, information.get(j).expressionList);
              // Remove the duplicate element at index j
              information.remove(j);
            }
          }
          i++;
        } while (i != information.size());
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

  /**
   * Store line number, temporary values, split expressions of this binary expression.
   *
   * @param edge
   * @param expression
   */
  public void handleBinaryExpression(CFAEdge edge, CBinaryExpression expression) {
    // LineNumber
    int lineNumber = edge.getLineNumber();
    if (allExpressionList.contains(lineNumber + " " + expression.toASTString())) {
      return;
    }
    for (String tv: allTemporaryValueList) {
      if (tv.contains(Integer.toString(lineNumber)) && !temporaryValueList.contains(tv)) {
        temporaryValueList.add(tv);
      }
    }
    allExpressionList.add(lineNumber + " " + expression.toASTString());
    // subexpression
    List<List<String>> expressionList = new ArrayList<>();
    // data type
    List<List<String>> typeList = new ArrayList<>();
    getInformation(expression, expressionList, typeList, edge);
    information.add(new StatementInformation(lineNumber, expressionList, typeList));
  }

  /**
   * Store line number, temporary values, split expressions of this unary expression.
   *
   * @param edge
   * @param cExpression
   */
  public void handleUnaryExpression(CFAEdge edge, CUnaryExpression cExpression) {
    if (!Objects.equals(cExpression.getOperator(), UnaryOperator.MINUS)) {
      return;
    }
    List<List<String>> typeList = new ArrayList<>();
    List<List<String>> expressionList = new ArrayList<>();
    List<String> expression = new ArrayList<>();

    for (String tv: allTemporaryValueList) {
      if (tv.contains(Integer.toString(
          edge.getLineNumber())) && !temporaryValueList.contains(tv)) {
        temporaryValueList.add(tv);
        break;
      }
    }
    typeList.add(Collections.singletonList(cExpression.toASTString() + " " + cExpression.getExpressionType().toString()));
    expression.add("none");
    expression.add(cExpression.toASTString().replace(" ", ""));
    expression.add("UnaryMinus");
    expressionList.add(expression);
    information.add(new StatementInformation(edge.getLineNumber(), expressionList, typeList));
  }

  /**
   * Split binary expression.
   *
   * @param operand
   * @param expressionList
   * @param typeList
   * @param edge
   */
  public void getInformation(CBinaryExpression operand, List<List<String>> expressionList, List<List<String>> typeList, CFAEdge edge) {
    List<String> binaryExpression = new ArrayList<>();

    CExpression left = operand.getOperand1();
    CExpression right = operand.getOperand2();
    String leftString = left.toASTString();
    String rightString = right.toASTString();


    leftString = leftString.replace(" ", "");
    rightString = rightString.replace(" ", "");
    binaryExpression.add(leftString);
    binaryExpression.add(rightString);
    String operator = String.valueOf(operand.getOperator());
    binaryExpression.add(operator);
    expressionList.add(binaryExpression);

    if (left instanceof CBinaryExpression) {
      getInformation((CBinaryExpression) left, expressionList, typeList, edge);
    } else if (left instanceof CUnaryExpression) {
      handleUnaryExpression(edge, (CUnaryExpression) left);
    } else {
      CType type = left.getExpressionType();
      typeList.add(Collections.singletonList(leftString + " " + type.toString()));
    }
    if (right instanceof CBinaryExpression) {
      getInformation((CBinaryExpression) right, expressionList, typeList, edge);
    } else if (right instanceof CUnaryExpression) {
      handleUnaryExpression(edge, (CUnaryExpression) right);
    } else {
      CType type = right.getExpressionType();
      typeList.add(Collections.singletonList(rightString + " " + type.toString()));
    }
  }

  /**
   * Represents information about a statement, including code line number, number of operators,
   * expression list, and type list.
   */
  public static class StatementInformation implements Serializable {
    final private int codeLineNumber;
    private int numOfOperator;
    final private List<List<String>> expressionList;
    final private List<List<String>> typeList;

    /**
     * Constructs a new StatementInformation object with the specified parameters.
     *
     * @param pCodeLineNumber The code line number where the statement occurs.
     * @param pExpressionList The list of expressions in the statement.
     * @param pTypeList The list of types corresponding to expressions in the statement.
     */
    public StatementInformation(int pCodeLineNumber, List<List<String>> pExpressionList, List<List<String>> pTypeList) {
      codeLineNumber = pCodeLineNumber;
      numOfOperator = pExpressionList.size();
      expressionList = pExpressionList;
      typeList = pTypeList;
    }

    /**
     * Converts the statement information to a formatted string representation.
     *
     * @return A string representation of the statement information.
     */
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
