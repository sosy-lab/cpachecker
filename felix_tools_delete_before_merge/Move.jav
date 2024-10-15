package org.sosy_lab.cpachecker.cfa.export.json;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Move {
  private static final String HEADER =
      "// This file is part of CPAchecker,\r\n"
          + //
          "// a tool for configurable software verification:\r\n"
          + //
          "// https://cpachecker.sosy-lab.org\r\n"
          + //
          "//\r\n"
          + //
          "// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>\r\n"
          + //
          "//\r\n"
          + //
          "// SPDX-License-Identifier: Apache-2.0\r\n"
          + //
          "\r\n"
          + //
          "package org.sosy_lab.cpachecker.cfa.export.json.mixins;\r\n"
          + //
          "\r\n"
          + //
          "import static com.google.common.base.Preconditions.checkNotNull;\r\n"
          + //
          "\r\n"
          + //
          "import com.fasterxml.jackson.annotation.JsonCreator;\r\n"
          + //
          "import com.fasterxml.jackson.annotation.JsonIdentityInfo;\r\n"
          + //
          "import com.fasterxml.jackson.annotation.JsonIdentityReference;\r\n"
          + //
          "import com.fasterxml.jackson.annotation.JsonIgnore;\r\n"
          + //
          "import com.fasterxml.jackson.annotation.JsonProperty;\r\n"
          + //
          "import com.fasterxml.jackson.annotation.JsonPropertyOrder;\r\n"
          + //
          "import com.fasterxml.jackson.annotation.JsonTypeInfo;\r\n"
          + //
          "import com.fasterxml.jackson.annotation.ObjectIdGenerator;\r\n"
          + //
          "import com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey;\r\n"
          + //
          "import com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator;\r\n"
          + //
          "import com.fasterxml.jackson.annotation.ObjectIdResolver;\r\n"
          + //
          "import com.fasterxml.jackson.annotation.SimpleObjectIdResolver;\r\n"
          + //
          "import com.fasterxml.jackson.core.JsonGenerator;\r\n"
          + //
          "import com.fasterxml.jackson.core.JsonParser;\r\n"
          + //
          "import com.fasterxml.jackson.core.JsonProcessingException;\r\n"
          + //
          "import com.fasterxml.jackson.databind.DeserializationContext;\r\n"
          + //
          "import com.fasterxml.jackson.databind.JsonDeserializer;\r\n"
          + //
          "import com.fasterxml.jackson.databind.JsonNode;\r\n"
          + //
          "import com.fasterxml.jackson.databind.JsonSerializer;\r\n"
          + //
          "import com.fasterxml.jackson.databind.ObjectMapper;\r\n"
          + //
          "import com.fasterxml.jackson.databind.SerializerProvider;\r\n"
          + //
          "import com.fasterxml.jackson.databind.annotation.JsonDeserialize;\r\n"
          + //
          "import com.fasterxml.jackson.databind.annotation.JsonSerialize;\r\n"
          + //
          "import com.fasterxml.jackson.databind.module.SimpleModule;\r\n"
          + //
          "import com.fasterxml.jackson.databind.util.ClassUtil;\r\n"
          + //
          "import com.fasterxml.jackson.databind.util.StdConverter;\r\n"
          + //
          "import com.google.common.collect.HashBasedTable;\r\n"
          + //
          "import com.google.common.collect.ImmutableList;\r\n"
          + //
          "import com.google.common.collect.ImmutableListMultimap;\r\n"
          + //
          "import com.google.common.collect.ImmutableSet;\r\n"
          + //
          "import com.google.common.collect.Multimap;\r\n"
          + //
          "import com.google.common.collect.Multiset;\r\n"
          + //
          "import com.google.common.collect.Table;\r\n"
          + //
          "import com.google.common.collect.Table.Cell;\r\n"
          + //
          "import com.google.common.collect.TreeMultimap;\r\n"
          + //
          "import java.io.IOException;\r\n"
          + //
          "import java.io.NotSerializableException;\r\n"
          + //
          "import java.io.ObjectInputStream;\r\n"
          + //
          "import java.io.ObjectOutputStream;\r\n"
          + //
          "import java.lang.reflect.Constructor;\r\n"
          + //
          "import java.lang.reflect.Field;\r\n"
          + //
          "import java.math.BigInteger;\r\n"
          + //
          "import java.nio.file.Path;\r\n"
          + //
          "import java.util.ArrayList;\r\n"
          + //
          "import java.util.Collection;\r\n"
          + //
          "import java.util.Collections;\r\n"
          + //
          "import java.util.Comparator;\r\n"
          + //
          "import java.util.HashMap;\r\n"
          + //
          "import java.util.HashSet;\r\n"
          + //
          "import java.util.Iterator;\r\n"
          + //
          "import java.util.List;\r\n"
          + //
          "import java.util.Map;\r\n"
          + //
          "import java.util.Map.Entry;\r\n"
          + //
          "import java.util.NavigableMap;\r\n"
          + //
          "import java.util.NavigableSet;\r\n"
          + //
          "import java.util.Optional;\r\n"
          + //
          "import java.util.Set;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.CfaMetadata;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.AAstNode;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.FileLocation;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.model.BlankEdge;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.model.CFAEdge;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.model.CFANode;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.types.Type;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;\r\n"
          + //
          "import org.sosy_lab.cpachecker.cfa.types.c.CType;\r\n"
          + //
          "import org.sosy_lab.cpachecker.util.LoopStructure;\r\n"
          + //
          "import org.sosy_lab.cpachecker.util.LoopStructure.Loop;\r\n"
          + //
          "import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;\r\n"
          + //
          "import org.sosy_lab.cpachecker.util.variableclassification.Partition;\r\n"
          + //
          "import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;\r\n"
          + //
          "\n";

  public static void main(String[] args) {
    String inputFileName =
        "/home/felix/cpachecker/src/org/sosy_lab/cpachecker/cfa/export/json/inner.txt";
    String fileContent = readFile(inputFileName);
    if (fileContent != null) {
      List<String> classes = extractClasses(fileContent);
      for (String classContent : classes) {
        String className = extractClassName(classContent);
        if (className != null) {
          writeFile(
              "/home/felix/cpachecker/src/org/sosy_lab/cpachecker/cfa/export/json/mixins/"
                  + className
                  + ".java",
              classContent);
          System.out.println("Extracted class: " + className);
        }
      }
    }
  }

  private static String readFile(String fileName) {
    StringBuilder content = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
      String line;
      while ((line = br.readLine()) != null) {
        content.append(line).append("\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    return content.toString();
  }

  private static List<String> extractClasses(String content) {
    List<String> classes = new ArrayList<>();
    Pattern pattern =
        Pattern.compile(
            "\\s\\s/\\*\\*.*?\\n\\s*public (final )?class (\\w+\\s\\{\\}|.*?\\n\\s\\s\\})",
            Pattern.DOTALL);
    Matcher matcher = pattern.matcher(content);
    while (matcher.find()) {
      classes.add(matcher.group());
    }
    return classes;
  }

  private static String extractClassName(String classContent) {
    Pattern pattern = Pattern.compile("public (final )?class (\\w+)");
    Matcher matcher = pattern.matcher(classContent);
    if (matcher.find()) {
      return matcher.group(2);
    }
    return null;
  }

  private static void writeFile(String fileName, String content) {
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
      bw.write(HEADER);
      bw.write(content);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
