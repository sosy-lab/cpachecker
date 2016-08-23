/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.precisionConverter;

import static org.sosy_lab.java_smt.api.FormulaType.getBitvectorTypeWithSize;

import com.google.common.base.Joiner;
import java.util.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SymbolEncoding {

  private Set<CSimpleDeclaration> decls = new HashSet<>();
  private MachineModel machineModel = null;

  /** This set contains function symbols that have a (maybe) unknown, but valid type.
   *  We do not care about the type, because it is automatically determined. */
  private final static Set<String> functionSymbols = Sets.newHashSet(
      "and", "or", "not", "ite",
      "=", "<", ">", "<=", ">=",
      "+", "-", "*", "/",
      "Integer__*_", "Integer__/_", "Integer__%_",
      "Rational__*_", "Rational__/_", "Rational__%_",
      "_~_", "_&_", "_!!_", "_^_", "_<<_", "_>>_",
      "bvnot", "bvslt", "bvult", "bvsle", "bvule", "bvsgt", "bvugt", "bvsge", "bvuge",
      "bvadd", "bvsub", "bvmul", "bvsdiv", "bvudiv", "bvsrem", "bvurem",
      "bvand", "bvor", "bvxor", "bvshl", "bvlshr", "bvashr",
      "to_real", "to_int",
      "_", "divisible"
      );

  /** create an empty symbol encoding */
  public SymbolEncoding() { }

  /** create symbol encoding with information about symbol
   * from variables of the CFA */
  public SymbolEncoding(CFA pCfa) {
    decls = getAllDeclarations(pCfa.getAllNodes());
    machineModel = pCfa.getMachineModel();

    encodedSymbols.put("true", new Type<FormulaType<?>>(FormulaType.BooleanType));
    encodedSymbols.put("false", new Type<FormulaType<?>>(FormulaType.BooleanType));
  }


  private final Map<String, Type<FormulaType<?>>> encodedSymbols = new HashMap<>();

  public void put(String symbol, int length) {
    put(symbol, new Type<FormulaType<?>>(getBitvectorTypeWithSize(length)));
  }

  public void put(String symbol, FormulaType<?> pReturnType, List<FormulaType<?>> pArgs) {
    put(symbol, new Type<>(pReturnType, pArgs));
  }

  public void put(String symbol, Type<FormulaType<?>> t) {
    // TODO currently we store all variables (even SSA-indexed ones),
    // but the basic form (without indices) maybe would be enough.
    if (encodedSymbols.containsKey(symbol)) {
      assert encodedSymbols.get(symbol).equals(t) :
        String.format("Symbol '%s' of type '%s' is already declared with the type '%s'.",
            symbol, t, encodedSymbols.get(symbol));
    } else {
      encodedSymbols.put(symbol, t);
    }
  }

  public boolean containsSymbol(String symbol) {
    return encodedSymbols.containsKey(symbol);
  }

  public Type<FormulaType<?>> getType(String symbol) throws UnknownFormulaSymbolException {

    if (functionSymbols.contains(symbol)) {
      return null;
    }

    if (encodedSymbols.containsKey(symbol)) {
      return encodedSymbols.get(symbol);
    }

    symbol = symbol.split("@")[0]; // sometimes we need to clean up SSA-indices
    for (CSimpleDeclaration decl : decls) {
      if (symbol.equals(decl.getQualifiedName())) {
        CType cType = decl.getType().getCanonicalType();
        return getType(cType);
      }
    }

    // ignore complex types
    throw new UnknownFormulaSymbolException(symbol);
  }

  private Type<FormulaType<?>> getType(CType cType) {
    final FormulaType<?> fType;
    if (cType instanceof CSimpleType && ((CSimpleType)cType).getType().isFloatingPointType()) {
      fType = FormulaType.RationalType;
    } else {
      int length = machineModel.getSizeof(cType) * machineModel.getSizeofCharInBits();
      fType = BitvectorType.getBitvectorTypeWithSize(length);
    }
    Type<FormulaType<?>> type = new Type<>(fType);
    if (cType instanceof CSimpleType) {
      type.setSigness(!((CSimpleType)cType).isUnsigned());
    }
    return type;
  }

  /** iterator over all edges and collect all declarations */
  private Set<CSimpleDeclaration> getAllDeclarations(Collection<CFANode> nodes) {
    final Set<CSimpleDeclaration> sd = new HashSet<>();
    for (CFANode node : nodes){

      if (node instanceof CFunctionEntryNode) {
        Optional<? extends CVariableDeclaration> retVar = ((CFunctionEntryNode) node).getReturnVariable();
        if (retVar.isPresent()) {
          sd.add(retVar.get());
        }
      }

      final FluentIterable<CFAEdge> edges = CFAUtils.allLeavingEdges(node);
      for (CDeclarationEdge edge : edges.filter(CDeclarationEdge.class)) {
        sd.add(edge.getDeclaration());
      }
      for (CFunctionCallEdge edge : edges.filter(CFunctionCallEdge.class)) {
        final List<? extends CParameterDeclaration> params = edge.getSuccessor().getFunctionParameters();
        for (CParameterDeclaration param : params) {
          sd.add(param);
        }
      }
      for (CFunctionReturnEdge edge : edges.filter(CFunctionReturnEdge.class)) {
        Optional<? extends CVariableDeclaration> retVar = edge.getFunctionEntry().getReturnVariable();
        if (retVar.isPresent()) {
          sd.add(retVar.get());
        }
      }
    }
    return sd;
  }

  /** write out the current symbol encoding in a format,
   * that can be read again. */
  public void dump(Path symbolEncodingFile) throws IOException {
    if (symbolEncodingFile != null) {
      MoreFiles.writeFile(
          symbolEncodingFile,
          Charset.defaultCharset(),
          new Appender() {
            @Override
            public void appendTo(Appendable app) throws IOException {
              for (String symbol : encodedSymbols.keySet()) {
                final Type<FormulaType<?>> type = encodedSymbols.get(symbol);
                app.append(symbol + "\t" + type.getReturnType());
                if (!type.getParameterTypes().isEmpty()) {
                  app.append("\t" + Joiner.on("\t").join(type.getParameterTypes()));
                }
                app.append("\n");
              }
            }
          });
    }
  }

  /** just a nice replacement for Pair<T,List<T>> */
  public static class Type<T> {

    private boolean signed = true; // default case: signed identifiers
    private final T returnType;
    private final List<T> parameterTypes;

    public Type(T pReturnType, List<T> pParameterTypes) {
      this.returnType = pReturnType;
      this.parameterTypes = pParameterTypes;
    }

    public Type(T pReturnType) {
      this.returnType = pReturnType;
      this.parameterTypes = Collections.<T>emptyList();
    }

    public T getReturnType() { return returnType; }

    public List<T> getParameterTypes() { return parameterTypes; }

    public void setSigness(boolean signed) {
      this.signed = signed;
    }

    public boolean isSigned() {
      return signed;
    }

    @Override
    public String toString() {
      return returnType + " " + parameterTypes;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object other) {
      if (other != null && other instanceof Type) {
        Type<T> t = (Type<T>)other;
        return returnType.equals(t.returnType)
            && parameterTypes.equals(t.parameterTypes);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return returnType.hashCode() + 17 * parameterTypes.hashCode();
    }

    public final static Type<Integer> BOOL = new Type<>(-1);
  }

  public static class UnknownFormulaSymbolException extends CPAException {

    private static final long serialVersionUID = 150615L;

    public UnknownFormulaSymbolException(String symbol) {
      super("unknown symbol in formula: " + symbol);
    }

  }

}
