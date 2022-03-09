// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;

/**
 * This class represents forward declarations of functions.
 * Example code:
 *
 * int foo(int x);
 */
public final class CFunctionDeclaration extends AFunctionDeclaration implements CDeclaration {

  public static final CFunctionDeclaration DUMMY =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          CFunctionType.NO_ARGS_VOID_FUNCTION,
          "dummy",
          ImmutableList.of(),
          ImmutableSet.of());

  /**
   * All GNU C function attributes that are known by CPAchecker. The keys of this map are the names
   * of the C attributes. The value of each name is one of the following two:
   *
   * <ul>
   *   <li>a {@link FunctionAttribute} that is used within CPAchecker to represent the attribute, or
   *   <li>empty if the attribute is known by CPAchecker, but ignored.
   * </ul>
   */
  public static final ImmutableMap<String, Optional<FunctionAttribute>> KNOWN_ATTRIBUTES;

  static {
    ImmutableMap.Builder<String, Optional<FunctionAttribute>> builder = ImmutableMap.builder();
    KNOWN_ATTRIBUTES = builder
        .put("access", Optional.empty())
        .put("alias", Optional.empty())
        .put("aligned", Optional.empty())
        .put("always_inline", Optional.empty())
        .put("cdecl", Optional.empty())
        .put("const", Optional.empty())
        .put("dllimport", Optional.empty())
        .put("fastcall", Optional.empty())
        .put("format", Optional.empty())
        .put("deprecated", Optional.empty())
        .put("ldv_model", Optional.empty())
        .put("ldv_model_inline", Optional.empty())
        .put("leaf", Optional.empty())
        .put("malloc", Optional.empty())
        .put("mode", Optional.empty()) // handled in ASTConverter
        .put("no_instrument_function", Optional.empty())
        .put("noinline", Optional.empty())
        .put("nonnull", Optional.empty())
        .put("noreturn", Optional.of(FunctionAttribute.NO_RETURN))
        .put("nothrow", Optional.empty())
        .put("pure", Optional.empty())
        .put("regparm", Optional.empty())
        .put("returns_twice", Optional.empty())
        .put("section", Optional.empty())
        .put("stdcall", Optional.empty())
        .put("warn_unused_result", Optional.empty())
        .put("unused", Optional.empty())
        .put("used", Optional.empty())
        .put("visibility", Optional.empty())
        .put("warning", Optional.empty())
        .put("weak", Optional.empty())
        .build();
  }

  private static final long serialVersionUID = 5485363555708455537L;

  /**
   * GNU C function attributes used by CPAchecker.
   * See {@link #KNOWN_ATTRIBUTES} for a list of all known attributes,
   * including those that are ignored.
   */
  public enum FunctionAttribute {
    /** GNU C attribute 'noreturn'. */
    NO_RETURN
  }


  private final ImmutableSet<FunctionAttribute> attributes;

  public CFunctionDeclaration(
      FileLocation pFileLocation,
      CFunctionType pType,
      String pName,
      List<CParameterDeclaration> parameters,
      ImmutableSet<FunctionAttribute> pAttributes) {
    super(pFileLocation, pType, checkNotNull(pName), pName, parameters);
    attributes = pAttributes;
  }

  public CFunctionDeclaration(
      FileLocation pFileLocation,
      CFunctionType pType,
      String pName,
      String pOrigName,
      List<CParameterDeclaration> parameters,
      ImmutableSet<FunctionAttribute> pAttributes) {
    super(pFileLocation, pType, checkNotNull(pName), checkNotNull(pOrigName), parameters);
    attributes = pAttributes;
  }

  @Override
  public CFunctionType getType() {
    return (CFunctionType) super.getType();
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<CParameterDeclaration> getParameters() {
    return (List<CParameterDeclaration>)super.getParameters();
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }

    if (!(obj instanceof CFunctionDeclaration)) {
      return false;
    }

    return super.equals(obj);
  }

  @Override
  public <R, X extends Exception> R accept(CSimpleDeclarationVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  /** 
   * Returns the list of GNU C attributes that are associated with this function declaration
   * and known to CPAchecker.
   */
  public ImmutableSet<FunctionAttribute> getAttributes() {
    return attributes;
  }

  /**
   * Returns whether this function declaration has the GNU C attribute 'noreturn'.
   */
  public boolean doesNotReturn() {
    return attributes.contains(FunctionAttribute.NO_RETURN);
  }

}
