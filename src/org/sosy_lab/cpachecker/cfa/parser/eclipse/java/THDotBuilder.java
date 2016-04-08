/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;

import com.google.common.base.Joiner;


class THDotBuilder {

  /**
   * Normal arrowhead style.
   */
  private static final String NORMAL = "normal";

  /**
   * Empty arrowhead style.
   */
  private static final String EMPTY = "empty";

  /**
   * No arrowhead.
   */
  private static final String NONE = "none";

  /**
   * Normal edge style.
   */
  private static final String SOLID = "solid";

  /**
   * Dashed edge style.
   */
  private static final String DASHED = "dashed";

  /**
   * Empty Dot arrowhead style.
   */
  private static final String ODOT = "odot";

  private final Appendable sb;

  private final Set<JClassOrInterfaceType> types;

  private final TypeHierarchy typeHierarchy;

  private THDotBuilder(Appendable pSb, Set<JClassOrInterfaceType> pTypes, TypeHierarchy pTypeHierarchy) {
    sb = pSb;
    types = pTypes;
    typeHierarchy = pTypeHierarchy;
  }

  public static void generateDOT(Appendable pW, Scope scope) throws IOException {

    Set<JClassOrInterfaceType> types = scope.getTypeHierarchy().getTypes();

    THDotBuilder builder = new THDotBuilder(pW, types, scope.getTypeHierarchy());

    builder.generateTypeHierarchyDOT();
  }

  private void generateTypeHierarchyDOT() throws IOException {
    generateHeader();

    for (JClassOrInterfaceType type : types) {
      addNodeDefinition(type);
    }

    addEdges();

    generateTail();
  }

  private void generateTail() throws IOException {
    sb.append("}");
  }

  private void addEdges() throws IOException {

    addSuperTypeEdges();
    addImplementsInterfaceEdges();
    addEnclosingTypeEdges();
  }

  private void addEnclosingTypeEdges() throws IOException {
    appendEdgeStyle(NORMAL, SOLID, ODOT);

    for (JClassOrInterfaceType type : types) {
      if (!type.isTopLevel()) {
        addEnclosingType(type);
      }
    }
  }

  private void addEnclosingType(JClassOrInterfaceType pType) throws IOException {
    String typeName = NameConverter.getNodeName(pType);
    JClassOrInterfaceType enclosingType = pType.getEnclosingType();

    String enclosingTypeName = NameConverter.getNodeName(enclosingType);
    addEdge(typeName, enclosingTypeName);
  }

  private void appendEdgeStyle(String arrowhead, String style, String arrowtail) throws IOException {
    sb.append("        edge [\n");
    sb.append("                  arrowhead = \"" + arrowhead + "\"\n");
    sb.append("                  style = \"" + style + "\"\n");
    sb.append("                  arrowtail = \"" + arrowtail + "\"\n");
    sb.append("        ]\n");
    sb.append("\n");
  }

  private void addImplementsInterfaceEdges() throws IOException {

    appendEdgeStyle(EMPTY, DASHED, NONE);


    for (JClassOrInterfaceType type : types) {
      if (type instanceof JClassType) {
        addImplementsInterfaceEdge((JClassType) type);
      }
    }
  }

  private void addImplementsInterfaceEdge(JClassType pType) throws IOException {
    String typeName = NameConverter.getNodeName(pType);
    Set<JInterfaceType> implementedInterfaces = pType.getImplementedInterfaces();

    for (JInterfaceType implementedInterface : implementedInterfaces) {
      String superTypeName = NameConverter.getNodeName(implementedInterface);
      addEdge(typeName, superTypeName);
    }
  }

  private void addSuperTypeEdges() throws IOException {

    appendEdgeStyle(EMPTY, SOLID, NONE);

    for (JClassOrInterfaceType type : types) {
      if (type instanceof JClassType) {
        addSuperTypeEdges((JClassType) type);
      } else if (type instanceof JInterfaceType) {
        addSuperTypeEdges((JInterfaceType) type);
      }
    }
  }

  private void addSuperTypeEdges(JInterfaceType pType) throws IOException {
    Set<JInterfaceType> superTypes = pType.getSuperInterfaces();
    String typeName = NameConverter.getNodeName(pType);

    for (JInterfaceType superType : superTypes) {
      String superTypeName = NameConverter.getNodeName(superType);
      addEdge(typeName, superTypeName);
    }
  }

  private void addEdge(String sourceTypeName, String destTypeName) throws IOException {
    sb.append("        ");
    sb.append(sourceTypeName);
    sb.append(" -> ");
    sb.append(destTypeName);
    sb.append("\n");
  }

  private void addSuperTypeEdges(JClassType pType) throws IOException {

    JClassType superType = pType.getParentClass();

    if (superType == null) {
      return;
    }

    String typeName = NameConverter.getNodeName(pType);
    String superTypeName = NameConverter.getNodeName(superType);
    addEdge(typeName, superTypeName);
  }

  private void addNodeDefinition(JClassOrInterfaceType pType) throws IOException {
    Set<JFieldDeclaration> fieldDecl = new HashSet<>();
    Set<JMethodDeclaration> methodDecl = new HashSet<>();
    boolean isExternType = typeHierarchy.isExternType(pType);

    if (!isExternType) {
      fieldDecl = typeHierarchy.getFieldDeclarations(pType);
      methodDecl = typeHierarchy.getMethodDeclarations(pType);
    }

    addNodeDefinition(pType, fieldDecl, methodDecl);
  }

  private void addNodeDefinition(JClassOrInterfaceType pType,
      Set<JFieldDeclaration> pFieldDecl, Set<JMethodDeclaration> pMethodDecl) throws IOException {

    String nodeName = NameConverter.getNodeName(pType);

    String label = getLabelContentOfNode(pType, pFieldDecl, pMethodDecl);

    sb.append("        " + nodeName + " [\n");
    sb.append("                  label = \"" + label + "\"\n");
    sb.append("        ]\n");
    sb.append("\n");

  }

  private String getLabelContentOfNode(JClassOrInterfaceType pType, Set<JFieldDeclaration> pFieldDecl,
      Set<JMethodDeclaration> pMethodDecl) {

    StringBuilder label = new StringBuilder();

    label.append("{");
    appendNameRowToLabel(label, pType);
    appendAttributesToLabel(label, pFieldDecl);
    appendMethodsToLabel(label, pMethodDecl);
    label.append("}");

    return label.toString();
  }

  private void appendMethodsToLabel(StringBuilder pLabel, Set<JMethodDeclaration> pMethodDecl) {

    for (JMethodDeclaration method : pMethodDecl) {
      appendMethodToLabel(pLabel, method);
    }
  }

  private void appendMethodToLabel(StringBuilder pLabel, JMethodDeclaration pMethod) {
    String visibilityLabel = getVisibilityLabel(pMethod);
    String methodName = pMethod.getSimpleName();
    String typeString = pMethod.getType().getReturnType().toASTString("");
    String parameters = getParameterLabel(pMethod.getParameters());

    pLabel.append(visibilityLabel);
    pLabel.append(" ");
    pLabel.append(methodName);
    pLabel.append("(");
    pLabel.append(parameters);
    pLabel.append(")");
    pLabel.append(": ");
    pLabel.append(typeString);
    pLabel.append("\\l");
  }

  private String getParameterLabel(List<JParameterDeclaration> pParameters) {

    List<String> parameterStrings = new ArrayList<>(pParameters.size());

    for (JParameterDeclaration param : pParameters) {
      parameterStrings.add(getParameterLabel(param));
    }

    return Joiner.on(",").join(parameterStrings);
  }

  private String getParameterLabel(JParameterDeclaration pParam) {
    String paramName = pParam.getName();
    String paramType = pParam.getType().toASTString("");

    return paramName + ":" + paramType;
  }

  private String getVisibilityLabel(JMethodDeclaration pMethod) {
    return getVisibilityLabel(pMethod.getVisibility());
  }

  private void appendAttributesToLabel(StringBuilder pLabel, Set<JFieldDeclaration> pFieldDecl) {

    for (JFieldDeclaration attr : pFieldDecl) {
      appendAttributToLabel(pLabel, attr);
    }

    pLabel.append("|");
  }

  private void appendAttributToLabel(StringBuilder pLabel, JFieldDeclaration pAttr) {

    String visibilityLabel = getVisibilityLabel(pAttr);
    String fieldName = pAttr.getSimpleName();
    String typeName =  pAttr.getType().toASTString("");

    pLabel.append(visibilityLabel);
    pLabel.append(fieldName);
    pLabel.append(": ");
    pLabel.append(typeName);
    pLabel.append("\\l");
  }

  private String getVisibilityLabel(JFieldDeclaration pAttr) {
    return getVisibilityLabel(pAttr.getVisibility());
  }

  private String getVisibilityLabel(VisibilityModifier visibility) {
    switch (visibility) {
    case PUBLIC:
      return "+";
    case PROTECTED:
      return "#";
    case NONE:
      return "";
    case PRIVATE:
      return "-";
    default:
      throw new AssertionError();
    }
  }

  private void appendNameRowToLabel(StringBuilder pLabel,
      JClassOrInterfaceType pType) {

    String simpleName = pType.getSimpleName();

    pLabel.append(simpleName + "|");
  }

  private void generateHeader() throws IOException {
    sb.append("digraph TypeHierarchy { \n");
    sb.append("        fontname = \"Bitstream Vera Sans\"\n");
    sb.append("        fontsize = 8\n");
    sb.append("\n");
    sb.append("        node [\n");
    sb.append("                  fontname = \"Bitstream Vera Sans\"\n");
    sb.append("                  fontsize = 8\n");
    sb.append("                  shape = \"record\"\n");
    sb.append("        ]\n");
    sb.append("\n");
    sb.append("        edge [\n");
    sb.append("                  fontname = \"Bitstream Vera Sans\"\n");
    sb.append("                  fontsize = 8\n");
    sb.append("        ]\n");
    sb.append("\n");
  }

  private static class NameConverter {

    private NameConverter() {} /*private Utility class*/

    private static String getNodeName(JClassOrInterfaceType pType) {
      return escapeNodeName(pType.getName());
    }

    private static String escapeNodeName(String nodeName) {
      return nodeName.replace(".", "_");
    }
  }
}