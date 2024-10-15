package org.sosy_lab.cpachecker.cfa.export;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.reflections.Reflections;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonData;

public class MixinWriter {

  public static void main(String[] pArgs) {
    Reflections reflections = new Reflections("org.sosy_lab.cpachecker");
    // typeInfoWriter(org.sosy_lab.cpachecker.cfa.types.Type.class, reflections);

    // System.out.println(getMixinSetters());

    // writeToFile(org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge.class, false);

    System.out.println();
    Set<Class<?>> missingMixins =
        determineMissingMixins(org.sosy_lab.cpachecker.cfa.ast.AAstNode.class, reflections);
    for (Class<?> missingMixin : missingMixins) {
      System.out.println(missingMixin.getSimpleName());
      //writeToFile(missingMixin, false);
    }
  }

  public static Set<Class<?>> determineMissingMixins(Class<?> pOrigin, Reflections pReflections) {
    return pReflections.getSubTypesOf(pOrigin).stream()
        .filter(subType -> missingMixinFilter(subType))
        .collect(Collectors.toSet());
  }

  private static boolean missingMixinFilter(Class<?> pClass) {
    String className = pClass.getSimpleName();

    File mixinFile =
        new File(
            "/home/felix/cpachecker/src/org/sosy_lab/cpachecker/cfa/export/json/mixins/"
                + className
                + "Mixin.java");

    return Modifier.isPublic(pClass.getModifiers())
        && !Modifier.isInterface(pClass.getModifiers())
        && !Modifier.isAbstract(pClass.getModifiers())
        && !(className.startsWith("J")
            && className.length() > 1
            && Character.isUpperCase(className.charAt(1)))
        && (!mixinFile.exists() || !isSubstringInFile(mixinFile, "@JsonCreator"));
  }

  public static boolean isSubstringInFile(File pFile, String pSubstring) {
    try (BufferedReader reader = new BufferedReader(new FileReader(pFile))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains(pSubstring)) {
          return true;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  public static List<Class<?>> getDirectSubTypes(Class<?> pClass, Reflections pReflections) {
    return pReflections.getSubTypesOf(pClass).stream()
        .filter(subType -> fil(pClass, subType))
        .sorted(Comparator.comparing(Class::getSimpleName))
        .collect(Collectors.toList());
  }

  private static boolean fil(Class<?> pClass, Class<?> pSubType) {
    return !pSubType.getSimpleName().startsWith("J")
        && (Arrays.asList(pSubType.getInterfaces()).contains(pClass)
            || (pSubType.getSuperclass() != null && pSubType.getSuperclass().equals(pClass)))
        && (Modifier.isInterface(pSubType.getModifiers())
            || Modifier.isPublic(pSubType.getModifiers()));
  }

  public static boolean isLeaf(Class<?> pClass, Reflections pReflections) {
    return pReflections.getSubTypesOf(pClass).stream().noneMatch(subType -> fil(pClass, subType));
  }

  public static String getMixinSetters() {
    return readAllFileNames(
            "/home/felix/cpachecker/src/org/sosy_lab/cpachecker/cfa/export/json/mixins")
        .stream()
        .map(
            fileName -> {
              String className = fileName.replace(".java", "");
              return "    pContext.setMixInAnnotations("
                  + className.replace("Mixin", "")
                  + ".class, "
                  + className
                  + ".class);";
            })
        .collect(Collectors.joining(System.lineSeparator()));
  }

  public static List<String> readAllFileNames(String folderPath) {
    List<String> fileNames = new ArrayList<>();
    Path path = Paths.get(folderPath);

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
      for (Path entry : stream) {
        fileNames.add(entry.getFileName().toString());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    Collections.sort(fileNames);

    return fileNames;
  }

  public static void typeInfoWriter(Class<?> pClass, Reflections pReflections) {
    if (!isLeaf(pClass, pReflections)) {
      File outputFile =
          new File(
              "/home/felix/cpachecker/src/org/sosy_lab/cpachecker/cfa/export/json/mixins/"
                  + pClass.getSimpleName()
                  + "Mixin.java");
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
        writer.write(constructTypeInfoMixin(pClass, getDirectSubTypes(pClass, pReflections)));
      } catch (Exception e) {
        System.err.println("Could not write to file " + outputFile);
        e.printStackTrace();
      }

      for (Class<?> subType : getDirectSubTypes(pClass, pReflections)) {
        typeInfoWriter(subType, pReflections);
      }
    }
  }

  public static String constructTypeInfoMixin(Class<?> pClass, List<Class<?>> pSubTypes) {
    StringBuilder builder = new StringBuilder();

    builder.append(COPYRIGHT_HEADER);
    builder.append(
        "\r\n"
            + //
            "\r\n"
            + //
            "package org.sosy_lab.cpachecker.cfa.export.json.mixins;\r\n"
            + //
            "\r\n"
            + //
            "import com.fasterxml.jackson.annotation.JsonSubTypes;\r\n"
            + //
            "import com.fasterxml.jackson.annotation.JsonSubTypes.Type;\r\n"
            + //
            "import "
            + pClass.getName()
            + ";\r\n");

    for (Class<?> subType : pSubTypes) {
      builder.append("import " + subType.getName() + ";\r\n");
    }

    builder.append(
        "\r\n"
            + //
            "/**\r\n"
            + //
            " * This class is a mixin for {@link "
            + pClass.getSimpleName()
            + "}.\r\n"
            + //
            " *\r\n"
            + //
            " * <p>It sets the names to use for all relevant subtypes.\r\n"
            + //
            " */\r\n"
            + //
            "@JsonSubTypes({\r\n");

    for (Class<?> subType : pSubTypes) {
      builder.append(
          "    @Type(value = "
              + subType.getSimpleName()
              + ".class, name = \""
              + subType.getSimpleName()
              + "\"),\r\n");
    }

    builder.append(
        "})\r\n"
            + //
            "public final class "
            + pClass.getSimpleName()
            + "Mixin {}");

    return builder.toString();
  }

  public static void writeToFile(Class<?> pClass, boolean pWriteTypeInfo) {
    MixinWriter mixinWriter = new MixinWriter(CfaJsonData.class, "org.sosy_lab.cpachecker");

    File outputFile =
        new File(
            "/home/felix/cpachecker/src/org/sosy_lab/cpachecker/cfa/export/json/mixins/"
                + pClass.getSimpleName()
                + "Mixin.java");

    if (outputFile.exists()) {
      System.out.println(mixinWriter.constructMixin(pClass, pWriteTypeInfo));
      System.err.println("File already exists: " + outputFile);
      return;
    }

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
      writer.write(
          COPYRIGHT_HEADER
              + System.lineSeparator()
              + System.lineSeparator()
              + "package org.sosy_lab.cpachecker.cfa.export.json.mixins;"
              + System.lineSeparator()
              + System.lineSeparator()
              + mixinWriter.constructMixin(pClass, pWriteTypeInfo));

      System.out.println(
          "pContext.setMixInAnnotations("
              + pClass.getSimpleName()
              + ".class, "
              + pClass.getSimpleName()
              + "Mixin.class);");
    } catch (Exception e) {
      System.err.println("Could not write to file " + outputFile);
      e.printStackTrace();
    }
  }

  private static final float MATCHING_THRESHOLD = 0.75f;
  private static final String COPYRIGHT_HEADER =
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
          "// SPDX-License-Identifier: Apache-2.0";
  private static final String SETUP_MODULE_HEADER =
      "    /**\r\n"
          + //
          "     * Sets up the module by registering all mixins.\r\n"
          + //
          "     *\r\n"
          + //
          "     * @param pContext The setup context.\r\n"
          + //
          "     */\r\n"
          + //
          "    @Override\r\n"
          + //
          "    public void setupModule(SetupContext pContext) {\r\n"
          + //
          "      super.setupModule(pContext);\r\n"
          + //
          "\r\n"
          + //
          "      /* Register all mixins. */";

  private final Class<?> origin;
  private final String relevantFolder;
  private final Reflections reflections;

  /**
   * Constructs a new {@link MixinWriter} object.
   *
   * @param pOrigin The origin type to start the search for necessary types from.
   * @param pRelevantFolder The folder which contains the files to create the Mixins for.
   */
  public MixinWriter(Class<?> pOrigin, String pRelevantFolder) {
    this.origin = pOrigin;
    this.relevantFolder = pRelevantFolder;
    this.reflections = new Reflections(this.relevantFolder);
  }

  public void write(File pOutputFile) {
    /* Get all relevant types. */
    Set<Type> types = getAllRelevantTypes(this.origin, new HashSet<>());

    /* Create a type hierarchy with all subtypes and a set for the imports. */
    HashMap<Type, Set<Type>> typeTree =
        types.stream().collect(HashMap::new, (m, t) -> m.put(t, new HashSet<>()), HashMap::putAll);
    List<Type> imports = new ArrayList<>();
    imports.add(JsonCreator.class);
    imports.add(JsonProperty.class);
    imports.add(JsonTypeInfo.class);
    imports.add(SimpleModule.class);

    for (Type type : types) {
      for (Type subType : this.reflections.getSubTypesOf((Class<?>) type)) {

        if (typeTree.containsKey(type) && !subType.getTypeName().contains(".java.")) {
          typeTree.get(type).add(subType);
        }

        typeTree.remove(subType);
      }
    }

    /* Write everything to file. */
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(pOutputFile))) {

      /* Write header. */
      writer.write(COPYRIGHT_HEADER + System.lineSeparator() + System.lineSeparator());

      /* Write package. */
      String pack = pOutputFile.getParentFile().getPath().replace("/", ".");
      pack = pack.substring(pack.indexOf(relevantFolder));
      writer.write("package " + pack + ";" + System.lineSeparator() + System.lineSeparator());

      /* Write imports. */
      Collections.sort(imports, Comparator.comparing(Type::getTypeName));

      for (Type type : imports) {
        writer.write("import " + type.getTypeName() + ";" + System.lineSeparator());
      }
      writer.write(System.lineSeparator());

      /* Write class signature. */
      writer.write(
          "/* This class is a Jackson module for serialization and deserialization. */"
              + System.lineSeparator()
              + "public class "
              + pOutputFile.getName().replace(".java", "")
              + " extends SimpleModule {"
              + System.lineSeparator()
              + System.lineSeparator());

      /* Write setup method. */
      writer.write(SETUP_MODULE_HEADER + System.lineSeparator());

      List<Type> sortedTypes = new ArrayList<>(typeTree.keySet());
      Collections.sort(sortedTypes, Comparator.comparing(Type::getTypeName));

      for (Type type : sortedTypes) {
        LinkedList<Type> sortedSubTypes = new LinkedList<>(typeTree.get(type));
        Collections.sort(sortedSubTypes, Comparator.comparing(Type::getTypeName));
        sortedSubTypes.addFirst(type);

        for (Type subType : sortedSubTypes) {
          Class<?> clazz = (Class<?>) subType;
          List<Entry<String, Parameter>> signature = getConstructorSignature(clazz);

          if ((signature == null || !signature.isEmpty())
              && clazz.getCanonicalName() != null
              && !clazz.isEnum()) {
            if (Modifier.isPublic(clazz.getModifiers())) {
              writer.write(
                  "      pContext.setMixInAnnotations("
                      + clazz.getCanonicalName()
                      + ".class, "
                      + clazz.getSimpleName()
                      + "Mixin.class);"
                      + System.lineSeparator());
            } else {
              System.err.println(
                  "Could not write mixin for " + clazz.getSimpleName() + ": Not public.");
            }
          }
        }
      }
      writer.write("    }" + System.lineSeparator() + System.lineSeparator());

      /* Write mixins. */
      for (Type type : sortedTypes) {
        String mixin = constructMixin((Class<?>) type, !typeTree.get(type).isEmpty());

        if (!mixin.equals("")) {
          writer.write(mixin + System.lineSeparator());
        }

        List<Type> sortedSubTypes = new ArrayList<>(typeTree.get(type));
        Collections.sort(sortedSubTypes, Comparator.comparing(Type::getTypeName));

        for (Type subType : sortedSubTypes) {
          mixin = constructMixin((Class<?>) subType, false);

          if (!mixin.equals("")) {
            writer.write(mixin + System.lineSeparator());
          }
        }
      }

      writer.write("}" + System.lineSeparator());

    } catch (Exception e) {
      System.err.println("Could not write to file " + pOutputFile);
      e.printStackTrace();
    }
  }

  /**
   * Constructs the mixin for a given class.
   *
   * @param pClass The class for which the mixin is constructed.
   * @param pWriteTypeInfo Indicates whether type information should be serialized with this mixin.
   * @return The constructed mixin as a string.
   */
  private String constructMixin(Class<?> pClass, boolean pWriteTypeInfo) {

    List<Entry<String, Parameter>> signature = getConstructorSignature(pClass);

    /* No mixin necessary. */
    if ((signature != null && signature.isEmpty() && !pWriteTypeInfo)
        || pClass.getCanonicalName() == null
        || !Modifier.isPublic(pClass.getModifiers())
        || pClass.isEnum()) {
      return "";
    }

    /* Construct the mixin. */
    StringBuilder builder = new StringBuilder();

    /* Write javadoc. */

    /* What type is this mixin for? */
    builder.append("/**" + System.lineSeparator());
    builder.append(
        " * This class is a mixin for {@link "
            + pClass.getSimpleName()
            + "}."
            + System.lineSeparator());
    builder.append(" *" + System.lineSeparator());

    /* Is type information being serialized with this mixin? */
    if (pWriteTypeInfo) {
      builder.append(
          " * <p>Type information is being serialized to account for subtype polymorphism."
              + System.lineSeparator());

      if (signature == null || !signature.isEmpty()) {
        builder.append("   *" + System.lineSeparator());
      }
    }

    /* Is a constructor being defined in this mixin? */
    if (signature == null || !signature.isEmpty()) {
      builder.append(
          " * <p>It specifies the constructor to use during deserialization."
              + System.lineSeparator());
    }

    builder.append(" */" + System.lineSeparator());

    /* Write the jackson type info annotation. */
    if (pWriteTypeInfo) {
      builder.append("@JsonTypeInfo(" + System.lineSeparator());
      builder.append("    use = JsonTypeInfo.Id.CLASS," + System.lineSeparator());
      builder.append("    include = JsonTypeInfo.As.PROPERTY," + System.lineSeparator());
      builder.append(
          "    property = \"typeOf" + pClass.getSimpleName() + "\")" + System.lineSeparator());
    }

    /* Write the mixin class signature. */
    builder.append("public final class " + pClass.getSimpleName() + "Mixin {");

    /* Constructor */
    if (signature != null && signature.isEmpty()) {
      /* No constructor necessary. */
      builder.append("}" + System.lineSeparator());
      return builder.toString();

    } else if (signature == null) {
      /* No matching constructor found. */
      builder.append(System.lineSeparator() + System.lineSeparator());
      builder.append("  // TODO: Write @JsonCreator constructor." + System.lineSeparator());

    } else {
      /* Write the constructor with annotations. */
      builder.append(System.lineSeparator() + System.lineSeparator());
      builder.append("  @SuppressWarnings(\"unused\")" + System.lineSeparator());
      builder.append("  @JsonCreator" + System.lineSeparator());
      builder.append("  public " + pClass.getSimpleName() + "Mixin(" + System.lineSeparator());

      Iterator<Entry<String, Parameter>> iterator = signature.iterator();

      /* Write the constructor parameters with annotations. */
      while (iterator.hasNext()) {
        Entry<String, Parameter> entry = iterator.next();
        String fieldName = getMatchingFieldName(entry.getKey(), entry.getValue(), pClass);

        builder.append("        ");

        /* No matching field name found. */
        if (fieldName == null) {
          fieldName = "TODO";
        }

        builder.append(
            "@JsonProperty(\""
                + fieldName
                + "\") "
                + entry.getValue().getType().getSimpleName()
                + " "
                + entry.getKey());

        if (iterator.hasNext()) {
          builder.append("," + System.lineSeparator());
        }
      }
      builder.append(") {}" + System.lineSeparator());
    }
    builder.append("}" + System.lineSeparator());

    return builder.toString();
  }

  /**
   * Retrieves the constructor signature for a given class.
   *
   * @param pClass The class for which to retrieve the constructor signature.
   * @return The list of entries representing the constructor signature, where each entry contains
   *     the parameter name and parameter object. Returns null if no constructors are available, no
   *     matching constructor is found, if there is a type mismatch or if there is an error reading
   *     the java file.
   */
  private static List<Entry<String, Parameter>> getConstructorSignature(Class<?> pClass) {
    Constructor<?>[] constructors =
        Arrays.stream(pClass.getDeclaredConstructors()).toArray(Constructor<?>[]::new);

    List<Entry<String, Parameter>> signature = new LinkedList<>();

    /* No constructors available. */
    if (constructors.length == 0) {
      return signature;
    }

    /* Default constructor available. */
    for (Constructor<?> con : constructors) {
      if (con.getParameterCount() == 0) {
        return signature;
      }
    }

    /* Get the path to the java file. */
    String path = pClass.getTypeName();

    if (path.contains("$")) {
      path = path.substring(0, path.indexOf("$"));
    }

    String filePath = "src/" + path.replaceAll("\\.", "/") + ".java";

    /* Read constructor signatures out of java file. */
    List<String> signatureListRaw = new LinkedList<>();
    StringBuilder builder = new StringBuilder();

    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.contains("public " + pClass.getSimpleName() + "(")) {
          builder.append(line).append(System.lineSeparator());
          if (line.contains(")")) {
            signatureListRaw.add(builder.substring(builder.indexOf("(") + 1, builder.indexOf(")")));
            builder.setLength(0);
            break;
          }
          while ((line = br.readLine()) != null) {
            if (line.contains(")")) {
              builder.append(line).append(System.lineSeparator());
              break;
            }
            builder.append(line).append(System.lineSeparator());
          }
          signatureListRaw.add(builder.substring(builder.indexOf("(") + 1, builder.indexOf(")")));
          builder.setLength(0);
        }
      }
    } catch (IOException e) {
      System.err.println("Could not read file " + filePath);
      return null;
    }

    /* Remove all generics from the strings. */
    List<String> signatureList =
        signatureListRaw.stream().map(s -> s.replaceAll("<.*>", "")).collect(Collectors.toList());

    /* Find the constructor with the most parameters. */
    int constructorIndex =
        IntStream.range(0, constructors.length)
            .boxed()
            .max(
                (i, j) ->
                    Integer.compare(
                        constructors[i].getParameterCount(), constructors[j].getParameterCount()))
            .orElse(-1);

    /* Find the signature with the most parameters. */
    int signatureIndex =
        IntStream.range(0, signatureList.size())
            .boxed()
            .max(
                (i, j) ->
                    Integer.compare(
                        signatureList.get(i).split(",").length,
                        signatureList.get(j).split(",").length))
            .orElse(-1);

    /* No matching constructor found. */
    if (constructorIndex == -1
        || signatureIndex == -1
        || constructors[constructorIndex].getParameterCount()
            != signatureList.get(signatureIndex).split(",").length) {
      return null;
    }

    /* Combine the signature and constructor information. */
    String[] signatureStrings = signatureList.get(signatureIndex).split(",");
    for (int i = 0; i < constructors[constructorIndex].getParameterCount(); i++) {

      /* Signature: Extract name and type. */
      String[] words = signatureStrings[i].split(" ");
      String name = words[words.length - 1];
      String type = words[words.length - 2];
      if (type.contains(".")) {
        type = type.substring(type.lastIndexOf(".") + 1);
      }

      /* Constructor */
      Parameter parameter = constructors[constructorIndex].getParameters()[i];
      String parameterType = parameter.getType().getSimpleName();

      /* Type mismatch? */
      if (!type.equals(parameterType)) {
        System.err.println(
            "Type mismatch for "
                + name
                + " in "
                + pClass.getTypeName()
                + ": \""
                + type
                + "\" != \""
                + parameterType
                + "\"");
        return null;
      }

      signature.add(new SimpleEntry<>(name, parameter));
    }

    return signature;
  }

  /**
   * Retrieves the name of the field in the given class that matches the specified parameter name
   * and type.
   *
   * @param pParameterName The name of the parameter.
   * @param pParameter The parameter object.
   * @param pClass The class in which to search for the matching field.
   * @return The name of the matching field, or null if no matching field is found or if the
   *     matching score is too low.
   */
  private static String getMatchingFieldName(
      String pParameterName, Parameter pParameter, Class<?> pClass) {

    /* Collect all fields with matching type. */
    List<Field> fields = new ArrayList<>();
    Class<?> currentClass = pClass;

    while (currentClass != null) {
      fields.addAll(
          Arrays.stream(currentClass.getDeclaredFields())
              .filter(field -> field.getType().isAssignableFrom(pParameter.getType()))
              .collect(Collectors.toList()));
      currentClass = currentClass.getSuperclass();
    }

    /* No matching fields found. */
    if (fields.isEmpty()) {
      return null;
    }

    /* Compute matching score for each remaining field. */
    float[] matchingScores = new float[fields.size()];

    for (int i = 0; i < fields.size(); i++) {
      matchingScores[i] = getMatchingScore(pParameterName, fields.get(i).getName());
    }

    /* Find the field with the highest matching score. */
    int maxIndex =
        IntStream.range(0, matchingScores.length)
            .boxed()
            .max((i, j) -> Float.compare(matchingScores[i], matchingScores[j]))
            .orElse(-1);

    /* Matching score too low. */
    if (matchingScores[maxIndex] < MATCHING_THRESHOLD) {
      return null;
    }

    return fields.get(maxIndex).getName();
  }

  /**
   * Calculates the matching score between a parameter name and a field name.
   *
   * @param pParameterName The parameter name to compare.
   * @param pFieldName The field name to compare.
   * @return The matching score between the parameter name and the field name.
   */
  private static float getMatchingScore(String pParameterName, String pFieldName) {
    String parameter = reverseString(pParameterName.toLowerCase());
    String field = reverseString(pFieldName.toLowerCase());

    int i = 0;

    for (; i < Math.min(parameter.length(), field.length()); i++) {
      if (parameter.charAt(i) != field.charAt(i)) {
        break;
      }
    }

    return (float) i / pParameterName.length();
  }

  /**
   * Reverses the given input string.
   *
   * @param input the string to be reversed
   * @return the reversed string
   */
  private static String reverseString(String input) {
    return new StringBuilder(input).reverse().toString();
  }

  /**
   * Retrieves all types that are relevant for the given type.
   *
   * <p>The method is recursive and collects only types that are in the relevant folder.
   *
   * @param pType The type to start the search from.
   * @param pTypes The set of types to add the types to.
   * @return The set of all relevant types.
   */
  private Set<Type> getAllRelevantTypes(Type pType, Set<Type> pTypes) {
    /* Go through all relevant types. */
    for (Type type : getIndividualTypes(pType)) {

      /* Stop criterion: Already collected */
      if (!pTypes.contains(type)) {

        /* Add type to set. */
        pTypes.add(type);

        /* Recursive: Field types */
        for (Field field : ((Class<?>) type).getDeclaredFields()) {
          getAllRelevantTypes(field.getGenericType(), pTypes);
        }

        /* Recursive: Inherited field types */
        Class<?> superClass = ((Class<?>) type).getSuperclass();

        if (superClass != null) {
          getAllRelevantTypes(superClass, pTypes);
          /* We only need the field types. */
          pTypes.remove(superClass);
        }

        /* Recursive: Subtypes */
        // for(Type subType : this.reflections.getSubTypesOf((Class <?>) type)) {
        // getAllRelevantTypes(subType, pTypes);
        // }
      }
    }

    return pTypes;
  }

  /**
   * Retrieves the given type and when the type is parameterized, collects all individual types from
   * it as well.
   *
   * <p>Only types that are in the relevant folder are added.
   *
   * @param pType The type to add and retrieve individual types from.
   * @return The set of individual types.
   */
  private Set<Type> getIndividualTypes(Type pType) {
    return getIndividualTypes(pType, new HashSet<>());
  }

  /**
   * Adds the provided type to the set of types if it is relevant.
   *
   * <p>If the provided type is parameterized, all individual types are added to the set as well.
   *
   * @param pType The type to add.
   * @param pTypes The set of types to add types to.
   * @return The set of types with the added type(s).
   */
  private Set<Type> getIndividualTypes(Type pType, Set<Type> pTypes) {

    if (pType instanceof Class) {
      /* Add the class. */
      addType(pType, pTypes);

    } else if (pType instanceof ParameterizedType) {
      /* Add raw type. */
      addType(((ParameterizedType) pType).getRawType(), pTypes);

      /* Recursive: Type arguments */
      for (Type type : ((ParameterizedType) pType).getActualTypeArguments()) {
        getIndividualTypes(type, pTypes);
      }
    }

    return pTypes;
  }

  /**
   * Adds a type to the set of types if it is in the relevant folder. If the given type is an array,
   * the component type is added instead.
   *
   * @param pType The type to be added.
   * @param pTypes The set of types to add the type to.
   */
  private void addType(Type pType, Set<Type> pTypes) {
    Type type = pType;

    /* Use component type if pType is an array. */
    if (pType instanceof Class && ((Class<?>) pType).isArray()) {
      type = ((Class<?>) pType).getComponentType();
    }

    /* Add type if it is in the relevant folder. */
    if (type.getTypeName().startsWith(this.relevantFolder)) {
      pTypes.add(type);
    }
  }
}
