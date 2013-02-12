/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.types.c;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;

import com.google.common.base.Function;


public class CTypeUtils {
  private static CTypeVisitor<CType, RuntimeException> simplifyType = new BaseCTypeSimplifyVisitor();

  public static class BaseCTypeSimplifyVisitor implements CTypeVisitor<CType, RuntimeException> {
    @Override
    public CType visit(CArrayType pArrayType) {
      // We do not support arrays, so simplify to pointer
      return new CPointerType(pArrayType.isConst(), pArrayType.isVolatile(), pArrayType.getType());
    }

    @Override
    public CType visit(CCompositeType pCompositeType) {
      return pCompositeType;
    }

    @Override
    public CType visit(CElaboratedType pElaboratedType) {
      if (pElaboratedType.getRealType() != null) {
        return pElaboratedType.getRealType();
      }

      return pElaboratedType;
    }

    @Override
    public CType visit(CEnumType pEnumType) {
      return pEnumType;
    }

    @Override
    public CType visit(CFunctionType pFunctionType) {
      return pFunctionType;
    }

    @Override
    public CType visit(CPointerType pPointerType) {
      return pPointerType;
    }

    @Override
    public CType visit(CProblemType pProblemType) {
      return pProblemType;
    }

    @Override
    public CType visit(CSimpleType pSimpleType) {
      return pSimpleType;
    }

    @Override
    public CType visit(CTypedefType pTypedefType) {
      return pTypedefType.getRealType().accept(this);
    }
  }

  public static CType simplifyType(CType t1) {
    return t1.accept(simplifyType);
  }

  public static class BaseCTypeEqualsVisitor implements CTypeVisitor<Boolean, RuntimeException> {
    final Object obj;
    List<String> stack = new LinkedList<>();
    public BaseCTypeEqualsVisitor(Object other) {
      this.obj = other;
    }

    protected Object getObj() {
      return obj;
    }

    protected CType simplifyType(CType t1) {
      return CTypeUtils.simplifyType(t1);
    }

    public BaseCTypeEqualsVisitor copyWith(Object other) {
      if (other instanceof CType) {
        other = simplifyType((CType)other);
      }

      return new BaseCTypeEqualsVisitor(other);
    }

    protected BaseCTypeEqualsVisitor workCopy(Object other, List<String> stack) {
      BaseCTypeEqualsVisitor copy = copyWith(other);
      copy.stack = stack;
      return copy;
    }

    protected boolean compareTypes(CType t1, CType t2) {
      return simplifyType(t1).accept(this.workCopy(t2, stack));
    }
    @Override
    public Boolean visit(CArrayType pThis) {
      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CArrayType other = (CArrayType) obj;
      return equalsArrayType(pThis, other);
    }

    private boolean equalsArrayType(CArrayType pThis, CArrayType other) {
      return
          Objects.equals(pThis.getLength(), other.getLength()) &&
          compareTypes(pThis.getType(), other.getType());
    }

    @Override
    public Boolean visit(CCompositeType pThis) {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CCompositeType other = (CCompositeType) obj;
      return equalsCompositeType(pThis, other);
    }

    private Boolean equalsCompositeType(CCompositeType pThis, CCompositeType other) {

      if (stack.contains(pThis.getName())) {
        // prevent stackoverflow, we have to use strings because CType.equals would call us again
        return true;
      } else {
        stack.add(pThis.getName());
      }

      if (stack.contains(other.getName())) {
        return true;
      } else {
        stack.add(other.getName());
      }


      return
          Objects.equals(pThis.getName(), other.getName()) &&
          compareMembers(pThis.getMembers(), other.getMembers()) &&
          pThis.getKind() == other.getKind();
    }


    private boolean compareMembers(List<CCompositeTypeMemberDeclaration> l1, List<CCompositeTypeMemberDeclaration> l2) {
      return compareLists(l1, l2, new Function<Pair<CCompositeTypeMemberDeclaration,CCompositeTypeMemberDeclaration>, Boolean>() {
        @Override
        public Boolean apply(Pair<CCompositeTypeMemberDeclaration,CCompositeTypeMemberDeclaration> pair) {
          CCompositeTypeMemberDeclaration m1 = pair.getFirst();
          CCompositeTypeMemberDeclaration m2 = pair.getSecond();
          return
                m2.getName().equals(m1.getName()) &&
                compareTypes(m2.getType(), m1.getType());
        }
      });
    }

    @Override
    public Boolean visit(CElaboratedType pThis) {
      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CElaboratedType other = (CElaboratedType) obj;
      return equalsElaboratedType(pThis, other);
    }

    private Boolean equalsElaboratedType(CElaboratedType pThis, CElaboratedType other) {
      return
          Objects.equals(pThis.getName(), other.getName()) &&
          pThis.getKind() == other.getKind();
    }

    @Override
    public Boolean visit(CEnumType pThis) {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CEnumType other = (CEnumType) obj;
      return equalsEnumType(pThis, other);
    }

    private Boolean equalsEnumType(CEnumType pThis, CEnumType other) {
      return
          Objects.equals(pThis.getEnumerators(), other.getEnumerators()) &&
          Objects.equals(pThis.getName(), other.getName());
    }

    @Override
    public Boolean visit(CFunctionType pThis) {

      if (this == obj) {
        return true;
      }
      if (!(obj instanceof CFunctionType)) {
        return false;
      }
      CFunctionType other = (CFunctionType) obj;
      return equalsFunctionType(pThis, other);
    }

    private Boolean equalsFunctionType(CFunctionType pThis, CFunctionType other) {
      return
          // Do not compare name, it is irrelevant in the C type system.
          //Objects.equals(pThis.getName(), other.getName()) &&
          compareTypes(pThis.getParameters(), other.getParameters()) &&
          compareTypes(pThis.getReturnType(), other.getReturnType()) &&
          pThis.takesVarArgs() == other.takesVarArgs();
    }

    private boolean compareTypes(List<CType> l1, List<CType> l2) {
      return compareLists(l1, l2, new Function<Pair<CType,CType>, Boolean>() {
        @Override
        public Boolean apply(Pair<CType,CType> pair) {
          return compareTypes(pair.getFirst(), pair.getSecond());
        }
      });
    }

    private <T> boolean compareLists(List<T> l1, List<T> l2, Function<Pair<T,T>,Boolean> compare) {
      Iterator<T> it1 = l2.iterator(), it2 = l1.iterator();
      for (; it1.hasNext() && it2.hasNext();) {
        T item1 = it1.next();
        T item2 = it2.next();

        if (!compare.apply(Pair.of(item1, item2))) {
          return false;
        }
      }

      return !it1.hasNext() && !it2.hasNext();
    }

    @Override
    public Boolean visit(CPointerType pThis) {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CPointerType other = (CPointerType) obj;
      return equalsPointerType(pThis, other);
    }

    private Boolean equalsPointerType(CPointerType pThis, CPointerType other) {
      return
          compareTypes(pThis.getType(), other.getType());
    }

    @Override
    public Boolean visit(CProblemType pThis) {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CProblemType other = (CProblemType) obj;
      return equalsProblemType(pThis, other);
    }

    private Boolean equalsProblemType(CProblemType pThis, CProblemType other) {
      return
          Objects.equals(pThis.toString(), other.toString());
    }

    @Override
    public Boolean visit(CSimpleType pThis) {

      if (this == obj) {
        return true;
      }
      if (pThis.getClass() != obj.getClass()) {
        return false;
      }
      CSimpleType other = (CSimpleType) obj;
      return equalsSimpleType(pThis, other);
    }

    private Boolean equalsSimpleType(CSimpleType pThis, CSimpleType other) {
      return
          pThis.isComplex() == other.isComplex() &&
          pThis.isImaginary() == other.isImaginary() &&
          pThis.isLong() == other.isLong() &&
          pThis.isLongLong() == other.isLongLong() &&
          pThis.isShort() == other.isShort() &&
          pThis.isSigned() == other.isSigned() &&
          pThis.isUnsigned() == other.isUnsigned() &&

          (pThis.getType() == other.getType() ||
            (pThis.getType() == CBasicType.INT && other.getType() == CBasicType.UNSPECIFIED) ||
            (pThis.getType() == CBasicType.UNSPECIFIED && other.getType() == CBasicType.INT));
    }

    @Override
    public Boolean visit(CTypedefType pThis) {
      if (this == obj) {
        return true;
      }

      return pThis.getRealType().accept(this);
    }
  }

  public static boolean equals(CType t1, Object other) {
    if (t1 == null || other == null) {
      return t1 == other;
    }

    if (!(other instanceof CType)) return false;
    return equals(t1, (CType)other);
  }

  public static boolean equals(CType t1, CType t2) {
    if (t1 == null || t2 == null) {
      return t1 == t2;
    }

    BaseCTypeEqualsVisitor visitor = new BaseCTypeEqualsVisitor(simplifyType(t2));
    return simplifyType(t1).accept(visitor);
  }
}
