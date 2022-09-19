// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * Used to store the types of the cfa that are lost when only a single or a block of statements of
 * the original program is parsed.
 */
public class CProgramScope implements Scope {

  public static final String ARTIFICIAL_RETVAL_NAME = "__artificial_result__";

  private static Iterable<CSimpleDeclaration> toCSimpleDeclarations(CFANode pNode) {
    return CFAUtils.leavingEdges(pNode)
        .transformAndConcat(
            pEdge -> {
              if (pEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
                CDeclaration dcl = ((CDeclarationEdge) pEdge).getDeclaration();
                return Collections.singleton(dcl);
              }

              if (pNode instanceof FunctionEntryNode) {
                FunctionEntryNode entryNode = (FunctionEntryNode) pNode;
                return from(entryNode.getFunctionParameters()).filter(CSimpleDeclaration.class);
              }

              return ImmutableSet.of();
            });
  }

  private static boolean hasName(CSimpleDeclaration pDeclaration) {
    if (pDeclaration.getName() != null && pDeclaration.getQualifiedName() != null) {
      return true;
    }
    if (pDeclaration.getName() == null
        && pDeclaration.getQualifiedName() == null
        && pDeclaration instanceof CComplexTypeDeclaration) {
      CComplexTypeDeclaration complexTypeDeclaration = (CComplexTypeDeclaration) pDeclaration;
      CComplexType complexType = complexTypeDeclaration.getType();
      return complexType != null
          && complexType.getName() != null
          && complexType.getQualifiedName() != null;
    }
    return false;
  }

  private static String getName(CSimpleDeclaration pDeclaration) {
    String result = pDeclaration.getName();
    if (result != null) {
      return result;
    }
    if (pDeclaration instanceof CComplexTypeDeclaration) {
      CComplexTypeDeclaration complexTypeDeclaration = (CComplexTypeDeclaration) pDeclaration;
      CComplexType complexType = complexTypeDeclaration.getType();
      if (complexType != null
          && complexType.getName() != null
          && complexType.getQualifiedName() != null) {
        return complexType.getName();
      }
    }
    throw new AssertionError("Cannot extract a name.");
  }

  private static String getOriginalQualifiedName(CSimpleDeclaration pDeclaration) {
    String name = pDeclaration.getName();
    if (name == null) {
      return getComplexDeclarationName(pDeclaration);
    }
    String originalName = pDeclaration.getOrigName();
    String qualifiedName = pDeclaration.getQualifiedName();
    if (name.equals(originalName)) {
      return qualifiedName;
    }
    assert qualifiedName.endsWith(name);
    return qualifiedName.substring(0, qualifiedName.length() - name.length()) + originalName;
  }

  private static String getComplexDeclarationName(CSimpleDeclaration pDeclaration) {
    if (pDeclaration instanceof CComplexTypeDeclaration) {
      CComplexType complexType = ((CComplexTypeDeclaration) pDeclaration).getType();
      if (complexType != null) {
        String name = complexType.getName();
        String originalName = complexType.getOrigName();
        String qualifiedName = complexType.getQualifiedName();
        if (name.equals(originalName)) {
          return qualifiedName;
        }
        assert qualifiedName.endsWith(name);

        return qualifiedName.substring(0, qualifiedName.length() - name.length()) + originalName;
      }
    }
    throw new AssertionError("Cannot extract a name.");
  }

  private final String currentFile = "";

  private final Set<String> variableNames;

  private final Multimap<String, CSimpleDeclaration> simpleDeclarations;

  private final Multimap<String, CFunctionDeclaration> functionDeclarations;

  private final Multimap<String, CSimpleDeclaration> qualifiedDeclarations;

  private final Map<String, CType> qualifiedTypeDefs;

  // TODO map type declarations to types and construct types that have original names for witness
  // automaton parsing
  private final Map<String, CComplexType> qualifiedTypes;

  private final Map<String, CSimpleDeclaration> retValDeclarations;

  private final Multimap<CAstNode, FileLocation> uses;

  private final String functionName;

  private final Predicate<FileLocation> locationDescriptor;

  /** Returns an empty program scope. */
  private CProgramScope() {
    variableNames = ImmutableSet.of();
    qualifiedDeclarations = ImmutableListMultimap.of();
    simpleDeclarations = ImmutableListMultimap.of();
    functionDeclarations = ImmutableListMultimap.of();
    qualifiedTypes = ImmutableMap.of();
    qualifiedTypeDefs = ImmutableMap.of();
    retValDeclarations = ImmutableMap.of();
    uses = ImmutableListMultimap.of();
    functionName = null;
    locationDescriptor = Predicates.alwaysTrue();
  }

  /**
   * Copies the given program scope but with the given function name.
   *
   * @param pScope the old scope.
   * @param pFunctionName the new function name.
   * @param pLocationDescriptor the new location descriptor.
   */
  private CProgramScope(
      CProgramScope pScope, String pFunctionName, Predicate<FileLocation> pLocationDescriptor) {
    variableNames = pScope.variableNames;
    simpleDeclarations = pScope.simpleDeclarations;
    functionDeclarations = pScope.functionDeclarations;
    qualifiedDeclarations = pScope.qualifiedDeclarations;
    qualifiedTypes = pScope.qualifiedTypes;
    qualifiedTypeDefs = pScope.qualifiedTypeDefs;
    retValDeclarations = pScope.retValDeclarations;
    uses = pScope.uses;
    functionName = pFunctionName;
    locationDescriptor = pLocationDescriptor;
  }

  /**
   * Creates an object of this class.
   *
   * <p>When a single or a block of statements is supposed to be parsed, first a cfa for the whole
   * program has to be parsed to generate complex types for the variables. These types and
   * declarations are stored in this scope.
   *
   * @param pCFA the cfa of the program, where single or block of statements are supposed to be
   *     parsed
   */
  public CProgramScope(CFA pCFA, LogManager pLogger) {

    assert pCFA.getLanguage() == Language.C || pCFA.getLanguage() == Language.LLVM;

    functionName = null;
    locationDescriptor = Predicates.alwaysTrue();

    /* Get all nodes, get all edges from nodes, get all declarations from edges,
     * assign every declaration its name.
     */
    Collection<CFANode> nodes = pCFA.getAllNodes();

    FluentIterable<CSimpleDeclaration> allDcls =
        FluentIterable.from(nodes).transformAndConcat(CProgramScope::toCSimpleDeclarations);

    FluentIterable<CSimpleDeclaration> dcls = allDcls.filter(CProgramScope::hasName);

    FluentIterable<CFunctionDeclaration> functionDcls = dcls.filter(CFunctionDeclaration.class);
    FluentIterable<CSimpleDeclaration> nonFunctionDcls =
        dcls.filter(not(instanceOf(CFunctionDeclaration.class)));
    FluentIterable<CTypeDeclaration> typeDcls = dcls.filter(CTypeDeclaration.class);

    qualifiedTypes = extractTypes(nonFunctionDcls, pLogger);

    qualifiedTypeDefs = extractTypeDefs(typeDcls, pLogger);

    functionDeclarations = functionDcls.index(CProgramScope::getOriginalQualifiedName);

    Map<String, CSimpleDeclaration> artificialRetValDeclarations = new HashMap<>();
    for (CFunctionDeclaration functionDeclaration : functionDeclarations.values()) {
      if (!(functionDeclaration.getType().getReturnType().getCanonicalType()
          instanceof CVoidType)) {
        String name = functionDeclaration.getName();
        if (!artificialRetValDeclarations.containsKey(name)) {
          CSimpleDeclaration retValDecl = getArtificialFunctionReturnVariable(functionDeclaration);
          artificialRetValDeclarations.put(name, retValDecl);
        }
      }
    }
    retValDeclarations = Collections.unmodifiableMap(artificialRetValDeclarations);
    nonFunctionDcls =
        FluentIterable.from(
            Iterables.concat(nonFunctionDcls, artificialRetValDeclarations.values()));

    variableNames = nonFunctionDcls.transform(CProgramScope::getName).toSet();

    qualifiedDeclarations = extractQualifiedDeclarations(nonFunctionDcls);

    uses = extractVarUseLocations(nodes);

    simpleDeclarations = extractSimpleDeclarations(qualifiedDeclarations);
  }

  public static CProgramScope empty() {
    return new CProgramScope();
  }

  @Override
  public boolean isGlobalScope() {
    return functionName == null;
  }

  @Override
  public boolean variableNameInUse(String pName) {
    return variableNames.contains(pName);
  }

  @Override
  public @Nullable CSimpleDeclaration lookupVariable(String pName) {

    List<Supplier<Iterable<CSimpleDeclaration>>> lookups = new ArrayList<>(isGlobalScope() ? 2 : 3);
    if (!isGlobalScope()) {
      lookups.add(() -> qualifiedDeclarations.get(createScopedNameOf(pName)));
    }
    lookups.add(() -> qualifiedDeclarations.get(pName));
    lookups.add(() -> simpleDeclarations.get(pName));

    Set<CSimpleDeclaration> results = ImmutableSet.of();

    Iterable<Supplier<Iterable<CSimpleDeclaration>>> filteredAndUnfiltered =
        Iterables.concat(
            Iterables.transform(
                lookups,
                s ->
                    () ->
                        FluentIterable.from(s.get())
                            .filter(d -> uses.get(d).stream().anyMatch(locationDescriptor))),
            lookups);

    Iterator<Supplier<Iterable<CSimpleDeclaration>>> lookupSupplierIterator =
        filteredAndUnfiltered.iterator();
    while (results.size() != 1 && lookupSupplierIterator.hasNext()) {
      results = ImmutableSet.copyOf(lookupSupplierIterator.next().get());
    }

    @Nullable CSimpleDeclaration result = null;
    Iterator<CSimpleDeclaration> resultIt = results.iterator();
    if (resultIt.hasNext()) {
      result = resultIt.next();
      if (resultIt.hasNext()) {
        result = null;
      }
    }
    return result;
  }

  @Override
  public @Nullable CFunctionDeclaration lookupFunction(String pName) {
    // Just take the first declaration; multiple different ones are not allowed
    Iterator<CFunctionDeclaration> it = functionDeclarations.get(pName).iterator();
    if (it.hasNext()) {
      return it.next();
    }
    return null;
  }

  @Override
  public @Nullable CComplexType lookupType(String pName) {
    CComplexType result = null;
    if (!isGlobalScope()) {
      String functionQualifiedName = createScopedNameOf(pName);
      result = lookupQualifiedComplexType(functionQualifiedName, qualifiedTypes);
      if (result != null) {
        return result;
      }
      result = qualifiedTypes.get(functionQualifiedName);
      if (result != null) {
        return result;
      }
    }
    result = lookupQualifiedComplexType(pName, qualifiedTypes);
    if (result != null) {
      return result;
    }
    result = qualifiedTypes.get(pName);
    if (result != null) {
      return result;
    }
    CType typdefResult = lookupTypedef(pName);
    if (typdefResult instanceof CComplexType) {
      return (CComplexType) typdefResult;
    }
    return null;
  }

  @Override
  public CType lookupTypedef(String pName) {
    CType result = null;
    if (!isGlobalScope()) {
      String functionQualifiedName = createScopedNameOf(pName);
      result = lookupQualifiedComplexType(functionQualifiedName, qualifiedTypeDefs);
      if (result != null) {
        return result;
      }
      result = qualifiedTypeDefs.get(functionQualifiedName);
      if (result != null) {
        return result;
      }
    }
    result = lookupQualifiedComplexType(pName, qualifiedTypeDefs);
    if (result != null) {
      return result;
    }
    return qualifiedTypeDefs.get(pName);
  }

  @Override
  public void registerDeclaration(CSimpleDeclaration pDeclaration) {
    // Assume that all declarations are already registered
  }

  @Override
  public boolean registerTypeDeclaration(CComplexTypeDeclaration pDeclaration) {
    // Assume that all type declarations are already registered
    return false;
  }

  @Override
  public String createScopedNameOf(String pName) {
    if (!isGlobalScope()) {
      return createScopedNameOf(getCurrentFunctionName(), pName);
    }
    return pName;
  }

  private static String createScopedNameOf(String pFunctionName, String pName) {
    return pFunctionName + "::" + pName;
  }

  /** Returns the name for the type as it would be if it is renamed. */
  @Override
  public String getFileSpecificTypeName(String type) {
    if (isFileSpecificTypeName(type)) {
      return type;
    }
    String fileSpecificTypeName = type + "__" + currentFile;
    if (currentFile.isEmpty()
        && lookupTypedef(fileSpecificTypeName) == null
        && lookupTypedef(type) != null) {
      return type;
    }
    return fileSpecificTypeName;
  }

  @Override
  public boolean isFileSpecificTypeName(String type) {
    return type.endsWith("__" + currentFile);
  }

  /**
   * Create a CProgramScope that tries to simulate a function scope.
   *
   * @param pFunctionName the name of the function.
   */
  public CProgramScope withFunctionScope(String pFunctionName) {
    return new CProgramScope(this, pFunctionName, locationDescriptor);
  }

  /**
   * Create a CProgramScope with the given location descriptor.
   *
   * @param pLocationDescriptor the new location descriptor.
   */
  public CProgramScope withLocationDescriptor(
      java.util.function.Predicate<FileLocation> pLocationDescriptor) {
    return new CProgramScope(this, functionName, pLocationDescriptor);
  }

  public String getCurrentFunctionName() {
    Preconditions.checkState(!isGlobalScope());
    return functionName;
  }

  private static boolean equals(CType pA, CType pB) {
    return equals(pA, pB, new HashSet<>());
  }

  private static boolean equals(
      @Nullable CType pA, @Nullable CType pB, Set<Pair<CType, CType>> pResolved) {

    // Identity check
    if (pA == pB) {
      return true;
    }

    // Exactly one of both null
    if (pA == null) {
      return false;
    }

    Pair<CType, CType> ab = Pair.of(pA, pB);

    // Check if equality is already known
    if (pResolved.contains(ab)) {
      return true;
    }

    // Standard equals check (non-recursive for composite types)
    boolean nonRecEq = pA.equals(pB);
    if (!nonRecEq) {
      return false;
    }

    // If the types are not composite types, we are done
    if (!(pA instanceof CCompositeType)) {
      pResolved.add(ab);
      return true;
    }

    CCompositeType aComp = (CCompositeType) pA;
    CCompositeType bComp = (CCompositeType) pB;

    // Check member count
    if (aComp.getMembers().size() != bComp.getMembers().size()) {
      return false;
    }

    // Assume they are equal for the recursive check
    pResolved.add(ab);

    Iterator<CCompositeTypeMemberDeclaration> aMembers = aComp.getMembers().iterator();
    for (CCompositeTypeMemberDeclaration bMember : bComp.getMembers()) {
      if (!equals(aMembers.next().getType(), bMember.getType(), pResolved)) {
        pResolved.remove(ab);
        return false;
      }
    }
    return true;
  }

  private static Multimap<String, CSimpleDeclaration> extractQualifiedDeclarations(
      FluentIterable<CSimpleDeclaration> pNonFunctionDcls) {
    Multimap<String, CSimpleDeclaration> qualifiedDeclarationsMultiMap =
        pNonFunctionDcls.index(CProgramScope::getOriginalQualifiedName);
    return Multimaps.transformValues(
        qualifiedDeclarationsMultiMap,
        v -> {
          if (v instanceof CVariableDeclaration) {
            CVariableDeclaration original = (CVariableDeclaration) v;
            if (original.getInitializer() == null) {
              return v;
            }
            return new CVariableDeclaration(
                original.getFileLocation(),
                original.isGlobal(),
                original.getCStorageClass(),
                original.getType(),
                original.getName(),
                original.getOrigName(),
                original.getQualifiedName(),
                null);
          }
          return v;
        });
  }

  private static Map<String, CComplexType> extractTypes(
      FluentIterable<? extends CSimpleDeclaration> pDcls, LogManager pLogger) {

    // Collect all types
    TypeCollector typeCollector = new TypeCollector();
    for (CSimpleDeclaration declaration : pDcls) {
      declaration.getType().accept(typeCollector);
    }

    // Construct multimap that may contain duplicates
    Multimap<String, CComplexType> typesMap =
        from(typeCollector.getCollectedTypes())
            .filter(CComplexType.class)
            .index(CComplexType::getQualifiedName);

    // Get unique types
    Map<String, CComplexType> uniqueTypes = new HashMap<>();

    for (Map.Entry<String, Collection<CComplexType>> typeEntry : typesMap.asMap().entrySet()) {
      String qualifiedName = typeEntry.getKey();
      Collection<CComplexType> types = typeEntry.getValue();
      putIfUnique(uniqueTypes, qualifiedName, types, pLogger);
    }

    return Collections.unmodifiableMap(uniqueTypes);
  }

  private static Map<String, CType> extractTypeDefs(
      FluentIterable<CTypeDeclaration> pTypeDcls, LogManager pLogger) {
    FluentIterable<CTypeDefDeclaration> plainTypeDefs = pTypeDcls.filter(CTypeDefDeclaration.class);

    // Construct multimap that may contain duplicates
    Multimap<String, CTypeDefDeclaration> typeDefDeclarationsMap =
        plainTypeDefs.index(CTypeDefDeclaration::getQualifiedName);

    // Get unique type defs
    Map<String, CType> uniqueTypeDefs = new HashMap<>();

    for (Map.Entry<String, Collection<CTypeDefDeclaration>> typeDefEntry :
        typeDefDeclarationsMap.asMap().entrySet()) {
      String qualifiedName = typeDefEntry.getKey();
      FluentIterable<CType> types =
          from(typeDefEntry.getValue()).transform(CTypeDefDeclaration::getType);
      putIfUnique(uniqueTypeDefs, qualifiedName, types, pLogger);
    }

    return Collections.unmodifiableMap(uniqueTypeDefs);
  }

  private static Multimap<String, CSimpleDeclaration> extractSimpleDeclarations(
      Multimap<String, CSimpleDeclaration> pQualifiedDeclarations) {
    return Multimaps.index(pQualifiedDeclarations.values(), CProgramScope::getName);
  }

  private static Iterable<AAstNode> getAstNodesFromCfaEdge(CFAEdge pEdge) {
    Iterable<AAstNode> nodes =
        FluentIterable.from(CFAUtils.getAstNodesFromCfaEdge(pEdge))
            .transformAndConcat(CFAUtils::traverseRecursively);
    if (pEdge instanceof ADeclarationEdge) {
      ADeclarationEdge declarationEdge = (ADeclarationEdge) pEdge;
      ADeclaration declaration = declarationEdge.getDeclaration();
      if (declaration instanceof AFunctionDeclaration) {
        nodes = Iterables.concat(nodes, ((AFunctionDeclaration) declaration).getParameters());
      }
    }
    return nodes;
  }

  private static Multimap<CAstNode, FileLocation> extractVarUseLocations(
      Collection<CFANode> pNodes) {
    FluentIterable<CAstNode> varUses =
        FluentIterable.from(pNodes)
            .transformAndConcat(CFAUtils::leavingEdges)
            .transformAndConcat(CProgramScope::getAstNodesFromCfaEdge)
            .filter(CAstNode.class)
            .filter(
                astNode ->
                    astNode instanceof CIdExpression || astNode instanceof CSimpleDeclaration)
            .filter(
                astNode -> {
                  if (astNode instanceof CIdExpression) {
                    return ((CIdExpression) astNode).getDeclaration() != null;
                  }
                  return true;
                });

    return varUses.stream()
        .collect(
            ImmutableSetMultimap.toImmutableSetMultimap(
                astNode -> {
                  if (astNode instanceof CSimpleDeclaration) {
                    CSimpleDeclaration decl = (CSimpleDeclaration) astNode;
                    if (decl instanceof CVariableDeclaration) {
                      CVariableDeclaration original = (CVariableDeclaration) decl;
                      if (original.getInitializer() != null) {
                        return new CVariableDeclaration(
                            original.getFileLocation(),
                            original.isGlobal(),
                            original.getCStorageClass(),
                            original.getType(),
                            original.getName(),
                            original.getOrigName(),
                            original.getQualifiedName(),
                            null);
                      }
                    }
                    return decl;
                  }
                  CIdExpression idExpression = (CIdExpression) astNode;
                  return idExpression.getDeclaration();
                },
                astNode -> astNode.getFileLocation()));
  }

  private static <T extends CType> void putIfUnique(
      Map<String, ? super T> pTarget,
      String pQualifiedName,
      Iterable<? extends T> pValues,
      LogManager pLogger) {
    if (!Iterables.isEmpty(pValues)) {
      Iterator<? extends T> typeIterator = pValues.iterator();
      T firstType = typeIterator.next();
      // Check that all types are the same; resolve elaborated types before checking
      CType firstChecktype = resolveElaboratedTypeForEqualityCheck(firstType);
      boolean duplicateFound = false;
      while (typeIterator.hasNext() && !duplicateFound) {
        if (!equals(firstChecktype, resolveElaboratedTypeForEqualityCheck(typeIterator.next()))) {
          // Does not seem to happen in competition benchmark set, so we should be fine
          pLogger.log(
              Level.FINEST,
              "Ignoring declaration for",
              pQualifiedName,
              " for creation of program-wide scope because it is not unique.");
          duplicateFound = true;
        }
      }
      if (!duplicateFound) {
        // Prefer recording elaborated types
        for (T type : pValues) {
          if (type instanceof CElaboratedType) {
            pTarget.put(pQualifiedName, type);
            return;
          }
        }
        pTarget.put(pQualifiedName, firstType);
      }
    }
  }

  private static CType resolveElaboratedTypeForEqualityCheck(CType pType) {
    CType currentType = pType;
    while (currentType instanceof CElaboratedType) {
      currentType = ((CElaboratedType) currentType).getRealType();
    }
    return currentType;
  }

  private static <T> T lookupQualifiedComplexType(String pName, Map<String, T> pStorage) {
    Set<T> potentialResults = new HashSet<>();
    for (ComplexTypeKind kind : ComplexTypeKind.values()) {
      T potentialResult = pStorage.get(kind.toASTString() + " " + pName);
      if (potentialResult != null) {
        potentialResults.add(potentialResult);
      }
    }
    if (potentialResults.size() == 1) {
      return potentialResults.iterator().next();
    }
    return null;
  }

  private static class TypeCollector extends DefaultCTypeVisitor<Void, NoException> {

    private final Set<CType> collectedTypes;

    public TypeCollector() {
      this(new HashSet<>());
    }

    public TypeCollector(Set<CType> pCollectedTypes) {
      collectedTypes = pCollectedTypes;
    }

    public Set<CType> getCollectedTypes() {
      return Collections.unmodifiableSet(collectedTypes);
    }

    @Override
    public @Nullable Void visitDefault(CType pT) {
      collectedTypes.add(pT);
      return null;
    }

    @Override
    public @Nullable Void visit(CArrayType pArrayType) {
      if (collectedTypes.add(pArrayType)) {
        pArrayType.getType().accept(this);
        if (pArrayType.getLength() != null) {
          pArrayType.getLength().getExpressionType().accept(this);
        }
      }
      return null;
    }

    @Override
    public @Nullable Void visit(CCompositeType pCompositeType) {
      if (collectedTypes.add(pCompositeType)) {
        for (CCompositeTypeMemberDeclaration member : pCompositeType.getMembers()) {
          member.getType().accept(this);
        }
      }
      return null;
    }

    @Override
    public @Nullable Void visit(CElaboratedType pElaboratedType) {
      if (collectedTypes.add(pElaboratedType)) {
        if (pElaboratedType.getRealType() != null) {
          pElaboratedType.getRealType().accept(this);
        }
      }
      return null;
    }

    @Override
    public @Nullable Void visit(CFunctionType pFunctionType) {
      if (collectedTypes.add(pFunctionType)) {
        for (CType parameterType : pFunctionType.getParameters()) {
          parameterType.accept(this);
        }
      }
      return null;
    }

    @Override
    public @Nullable Void visit(CPointerType pPointerType) {
      if (collectedTypes.add(pPointerType)) {
        pPointerType.getType().accept(this);
      }
      return null;
    }

    @Override
    public @Nullable Void visit(CTypedefType pTypedefType) {
      if (collectedTypes.add(pTypedefType)) {
        pTypedefType.getRealType().accept(this);
      }
      return null;
    }

    @Override
    public @Nullable Void visit(CBitFieldType pCBitFieldType) {
      if (collectedTypes.add(pCBitFieldType)) {
        pCBitFieldType.getType().accept(this);
      }
      return null;
    }
  }

  public boolean hasFunctionReturnVariable(String pFunctionName) {
    return retValDeclarations.containsKey(pFunctionName);
  }

  public CSimpleDeclaration getFunctionReturnVariable(String pFunctionName) {
    CSimpleDeclaration result = retValDeclarations.get(pFunctionName);
    checkArgument(
        result != null, "Function unknown or does not have a return value: %s", pFunctionName);
    return result;
  }

  private static CSimpleDeclaration getArtificialFunctionReturnVariable(
      CFunctionDeclaration pFunctionDeclaration) {
    String name = ARTIFICIAL_RETVAL_NAME + pFunctionDeclaration.getName() + "__";
    return new CVariableDeclaration(
        pFunctionDeclaration.getFileLocation(),
        false,
        CStorageClass.AUTO,
        pFunctionDeclaration.getType().getReturnType(),
        name,
        name,
        createScopedNameOf(pFunctionDeclaration.getName(), name),
        null);
  }

  public static boolean isArtificialFunctionReturnVariable(CIdExpression pCIdExpression) {
    if (pCIdExpression.getDeclaration() == null) {
      return false;
    }
    String name = pCIdExpression.getDeclaration().getName();
    if (!name.startsWith(ARTIFICIAL_RETVAL_NAME)) {
      return false;
    }
    String qualifiedName = pCIdExpression.getDeclaration().getQualifiedName();
    List<String> parts = Splitter.on("::").splitToList(qualifiedName);
    if (parts.size() < 2) {
      return false;
    }
    return parts.get(1).equals(ARTIFICIAL_RETVAL_NAME + parts.get(0) + "__");
  }

  public static String getFunctionNameOfArtificialReturnVar(CIdExpression pCIdExpression) {
    checkArgument(
        isArtificialFunctionReturnVariable(pCIdExpression),
        "Variable is not an artificial return variable.");
    String qualifiedName = pCIdExpression.getDeclaration().getQualifiedName();
    return qualifiedName.substring(0, qualifiedName.indexOf("::"));
  }
}
