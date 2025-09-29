// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

rule BVExtractExpr:
  id: BVExtractExpr
  category: Bitvector
  match: (bvextract_<hi>_<lo> <var>)
  normalize: (extract (<hi>, <lo> <var>))
  fingerprint: (extract (<hi>, <lo> <var>))
  tags: {
    bvextract: Function,
    <var>: Variable,
    <hi>: Constant,
    <lo>: Constant
  }

rule BVExtractConst:
  id: BVExtractConst
  category: Bitvector
  match: (= (bvextract_<hi>_<lo> <var>) <const>)
  normalize: (<var>[<hi>:<lo>] == <const>)
  fingerprint: (<var>[<hi>:<lo>] == <const>)
  tags: {
    bvextract: Function,
    =: Operator,
    <var>: Variable,
    <const>: Constant
  }

rule BVExtractBoolOne:
  id: BVExtractBoolOne
  category: Bitvector
  match: (= (bvextract_<hi>_<lo> <var>) 1)
  normalize: (<var>[<hi>:<lo>] == 1)
  fingerprint: (<var>[<hi>:<lo>] == 1)
  tags: {
    bvextract: Function,
    =: Operator,
    <var>: Variable,
    1: Constant
  }

rule BVExtractBoolZero:
  id: BVExtractBoolZero
  category: Bitvector
  match: (= (bvextract_<hi>_<lo> <var>) 0)
  normalize: (<var>[<hi>:<lo>] == 0)
  fingerprint: (<var>[<hi>:<lo>] == 0)
  tags: {
    bvextract: Function,
    =: Operator,
    <var>: Variable,
    0: Constant
  }

rule NotBVExtract:
  id: NotBVExtract
  category: Bitvector
  match: (! (= (bvextract_<hi>_<lo> <var>) <const>))
  normalize: (not (<var>[<hi>:<lo>] == <const>))
  fingerprint: (not (<var>[<hi>:<lo>] == <const>))
  tags: {
    bvextract: Function,
    !: Operator,
    =: Operator,
    <var>: Variable,
    <const>: Constant
  }

rule NotBVExtractBoolOne:
  id: NotBVExtractBoolOne
  category: Bitvector
  match: (! (= (bvextract_<hi>_<lo> <var>) 1))
  normalize: (not (<var>[<hi>:<lo>] == 1))
  fingerprint: (not (<var>[<hi>:<lo>] == 1))
  tags: {
    bvextract: Function,
    !: Operator,
    =: Operator,
    <var>: Variable,
    1: Constant
  }

rule NotBVExtractBoolZero:
  id: NotBVExtractBoolZero
  category: Bitvector
  match: (! (= (bvextract_<hi>_<lo> <var>) 0))
  normalize: (not (<var>[<hi>:<lo>] == 0))
  fingerprint: (not (<var>[<hi>:<lo>] == 0))
  tags: {
    bvextract: Function,
    !: Operator,
    =: Operator,
    <var>: Variable,
    0: Constant
  }

rule NotBVExtractExpr:
  id: NotBVExtractExpr
  category: Bitvector
  match: (! bvextract_<hi>_<lo> <var>)
  normalize: (not extract (<hi>, <lo>, <var>))
  fingerprint: (not extract (<hi>, <lo>, <var>))
  tags: {
    bvextract: Function,
    !: Operator,
    <var>: Variable,
    <hi>: Constant,
    <lo>: Constant
  }

rule BVAdd:
  id: BVAdd
  category: Bitvector
  match: (= (bvadd <const1> <var>) <const2>)
  normalize: (<var> + <const1> == <const2>)
  fingerprint: (<var> + <const1> == <const2>)
  tags: {
    bvadd: Function,
    =: Operator,
    <var>: Variable,
    <const1>: Constant,
    <const2>: Constant
  }

rule BVAddExpr:
  id: BVAddExpr
  category: Bitvector
  match: (bvadd <const> <var>)
  normalize: (<var> + <const>)
  fingerprint: (<var> + <const>)
  tags: {
    bvadd: Function,
    <var>: Variable,
    <const>: Constant
  }

rule BVLshl:
  id: BVLshl
  category: Bitvector
  match: (= (bvlshl <var> <const1>) <const2>)
  normalize: (<var> << <const1> == <const2>)
  fingerprint: (<var> << <const1> == <const2>)
  tags: {
    bvlshl: Function,
    =: Operator,
    <var>: Variable,
    <const1>: Constant,
    <const2>: Constant
  }

rule BVLshlExpr:
  id: BVLshlExpr
  category: Bitvector
  match: (bvlshl <var> <const>)
  normalize: (<var> << <const>)
  fingerprint: (<var> << <const>)
  tags: {
    bvlshl: Function,
    <var>: Variable,
    <const>: Constant,
  }

rule EqVarConst32:
  id: EqVarConst32
  category: Equality
  match: (= <var1> <const>_32)
  normalize: (<var1> == <const>)
  fingerprint: (<var1> == <const>)
  tags: {
     =: Operator,
    <var1>: Variable,
    <const>_32: Constant
  }

rule EqVarConst:
  id: EqVarConst
  category: Equality
  match: (= <var> <const>)
  normalize: (<var> == <const>)
  fingerprint: (<var> == <const>)
  tags: {
    =: Operator,
    <var>: Variable,
    <const>: Constant
  }

rule EqVarVar:
  id: EqVarVar
  category: Equality
  match: (= <var1> <var2>)
  normalize: (<var1> == <var2>)
  fingerprint: (<var1> == <var2>)
  tags: {
    =: Operator,
    <var1>: Variable,
    <var2>: Variable
  }

rule EqVarSingle:
  id: EqVarTerm
  category: Equality
  match: (= <var>)
  normalize: (<var>)
  fingerprint: (<var>)
  tags: {
    =: Operator,
    <var>: Variable
  }

rule EqVarTerm:
  id: EqVarTerm
  category: Equality
  match: (= <var> <term>)
  normalize: (<var> == <term>)
  fingerprint: (<var> == <term>)
  tags: {
    =: Operator,
    <var>: Variable,
    <term>: Term
  }

rule NestedAND:
  id: NestedAND
  category: Logical
  match: (and <term1> <term2>)
  normalize: (<term1> AND <term2>)
  fingerprint: (<term1> AND <term2>)
  tags: {
    and: Operator
  }

rule MultiAnd:
  id: MultiAnd
  category: Logical
  match: (and <term1> (and <term2> <term3>))
  normalize: (<term1> AND (<term2> AND <term3>))
  fingerprint: (<term1> AND (<term2> AND <term3>))
  tags: {
      and: Operator
  }

rule NestedOR:
  id: NestedOR
  category: Logical
  match: (or <term1> <term2>)
  normalize: (<term1> OR <term2>)
  fingerprint: (<term1> OR <term2>)
  tags: {
    or: Operator
  }

rule NegatedExpr:
  id: NegatedExpr
  category: Logical
  match: (not <term>)
  normalize: (NOT <term>)
  fingerprint: (NOT <term>)
  tags: {
    not: Operator
  }


