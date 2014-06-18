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
package org.sosy_lab.cpachecker.util.predicates.z3;

import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;

/** This class contains many constants (enums) from Z3. */
public class Z3NativeApiConstants {

  /** returns, if the function of the expression is the given operation. */
  public static boolean isOP(long z3context, long expr, int op) {
    long decl = get_app_decl(z3context, expr);
    return get_decl_kind(z3context, decl) == op;
  }

  // CONSTANT VALUES, TODO should we make enums?

  public static enum Z3_LBOOL {
    Z3_L_FALSE(-1),
    Z3_L_UNDEF(0),
    Z3_L_TRUE(1);

    final int status;
    Z3_LBOOL(int status) {
       this.status = status;
    }
  }

  // Z3_ast_kind
  public static final int Z3_NUMERAL_AST = 0;
  public static final int Z3_APP_AST = 1;
  public static final int Z3_VAR_AST = 2;
  public static final int Z3_QUANTIFIER_AST = 3;
  public static final int Z3_SORT_AST = 4;
  public static final int Z3_FUNC_DECL_AST = 5;
  public static final int Z3_UNKNOWN_AST = 1000;

  // Z3_sort_kind
  public static final int Z3_UNINTERPRETED_SORT = 0;
  public static final int Z3_BOOL_SORT = 1;
  public static final int Z3_INT_SORT = 2;
  public static final int Z3_REAL_SORT = 3;
  public static final int Z3_BV_SORT = 4;
  public static final int Z3_ARRAY_SORT = 5;
  public static final int Z3_DATATYPE_SORT = 6;
  public static final int Z3_RELATION_SORT = 7;
  public static final int Z3_FINITE_DOMAIN_SORT = 8;
  public static final int Z3_UNKNOWN_SORT = 1000;

  // Z3_symbol_kind
  public static final int Z3_INT_SYMBOL = 0;
  public static final int Z3_STRING_SYMBOL = 1;

  // Z3_decl_kind
  public static final int Z3_OP_TRUE = 256;
  public static final int Z3_OP_FALSE = 257;
  public static final int Z3_OP_EQ = 258;
  public static final int Z3_OP_DISTINCT = 259;
  public static final int Z3_OP_ITE = 260;
  public static final int Z3_OP_AND = 261;
  public static final int Z3_OP_OR = 262;
  public static final int Z3_OP_IFF = 263;
  public static final int Z3_OP_XOR = 264;
  public static final int Z3_OP_NOT = 265;
  public static final int Z3_OP_IMPLIES = 266;
  public static final int Z3_OP_OEQ = 267;

  public static final int Z3_OP_ANUM = 512;
  public static final int Z3_OP_AGNUM = 513;
  public static final int Z3_OP_LE = 514;
  public static final int Z3_OP_GE = 515;
  public static final int Z3_OP_LT = 516;
  public static final int Z3_OP_GT = 517;
  public static final int Z3_OP_ADD = 518;
  public static final int Z3_OP_SUB = 519;
  public static final int Z3_OP_UMINUS = 520;
  public static final int Z3_OP_MUL = 521;
  public static final int Z3_OP_DIV = 522;
  public static final int Z3_OP_IDIV = 523;
  public static final int Z3_OP_REM = 524;
  public static final int Z3_OP_MOD = 525;
  public static final int Z3_OP_TO_REAL = 526;
  public static final int Z3_OP_TO_INT = 527;
  public static final int Z3_OP_IS_INT = 528;
  public static final int Z3_OP_POWER = 529;

  public static final int Z3_OP_STORE = 768;
  public static final int Z3_OP_SELECT = 769;
  public static final int Z3_OP_CONST_ARRAY = 770;
  public static final int Z3_OP_ARRAY_MAP = 771;
  public static final int Z3_OP_ARRAY_DEFAULT = 772;
  public static final int Z3_OP_SET_UNION = 773;
  public static final int Z3_OP_SET_INTERSECT = 774;
  public static final int Z3_OP_SET_DIFFERENCE = 775;
  public static final int Z3_OP_SET_COMPLEMENT = 776;
  public static final int Z3_OP_SET_SUBSET = 777;
  public static final int Z3_OP_AS_ARRAY = 778;

  public static final int Z3_OP_BNUM = 1024;
  public static final int Z3_OP_BIT1 = 1025;
  public static final int Z3_OP_BIT0 = 1026;
  public static final int Z3_OP_BNEG = 1027;
  public static final int Z3_OP_BADD = 1028;
  public static final int Z3_OP_BSUB = 1029;
  public static final int Z3_OP_BMUL = 1030;
  public static final int Z3_OP_BSDIV = 1031;
  public static final int Z3_OP_BUDIV = 1032;
  public static final int Z3_OP_BSREM = 1033;
  public static final int Z3_OP_BUREM = 1034;
  public static final int Z3_OP_BSMOD = 1035;
  public static final int Z3_OP_BSDIV0 = 1036;
  public static final int Z3_OP_BUDIV0 = 1037;
  public static final int Z3_OP_BSREM0 = 1038;
  public static final int Z3_OP_BUREM0 = 1039;
  public static final int Z3_OP_BSMOD0 = 1040;
  public static final int Z3_OP_ULEQ = 1041;
  public static final int Z3_OP_SLEQ = 1042;
  public static final int Z3_OP_UGEQ = 1043;
  public static final int Z3_OP_SGEQ = 1044;
  public static final int Z3_OP_ULT = 1045;
  public static final int Z3_OP_SLT = 1046;
  public static final int Z3_OP_UGT = 1047;
  public static final int Z3_OP_SGT = 1048;
  public static final int Z3_OP_BAND = 1049;

  public static final int Z3_OP_BOR = 1050;
  public static final int Z3_OP_BNOT = 1051;
  public static final int Z3_OP_BXOR = 1052;
  public static final int Z3_OP_BNAND = 1053;
  public static final int Z3_OP_BNOR = 1054;
  public static final int Z3_OP_BXNOR = 1055;
  public static final int Z3_OP_CONCAT = 1056;
  public static final int Z3_OP_SIGN_EXT = 1057;
  public static final int Z3_OP_ZERO_EXT = 1058;
  public static final int Z3_OP_EXTRACT = 1059;
  public static final int Z3_OP_REPEAT = 1060;
  public static final int Z3_OP_BREDOR = 1061;
  public static final int Z3_OP_BREDAND = 1062;
  public static final int Z3_OP_BCOMP = 1063;
  public static final int Z3_OP_BSHL = 1064;
  public static final int Z3_OP_BLSHR = 1065;
  public static final int Z3_OP_BASHR = 1066;
  public static final int Z3_OP_ROTATE_LEFT = 1067;
  public static final int Z3_OP_ROTATE_RIGHT = 1068;
  public static final int Z3_OP_EXT_ROTATE_LEFT = 1069;
  public static final int Z3_OP_EXT_ROTATE_RIGHT = 1070;
  public static final int Z3_OP_INT2BV = 1071;
  public static final int Z3_OP_BV2INT = 1072;
  public static final int Z3_OP_CARRY = 1073;
  public static final int Z3_OP_XOR3 = 1074;

  public static final int Z3_OP_PR_UNDEF = 1280;
  public static final int Z3_OP_PR_TRUE = 1281;
  public static final int Z3_OP_PR_ASSERTED = 1282;
  public static final int Z3_OP_PR_GOAL = 1283;
  public static final int Z3_OP_PR_MODUS_PONENS = 1284;
  public static final int Z3_OP_PR_REFLEXIVITY = 1285;
  public static final int Z3_OP_PR_SYMMETRY = 1286;
  public static final int Z3_OP_PR_TRANSITIVITY = 1287;
  public static final int Z3_OP_PR_TRANSITIVITY_STAR = 1288;
  public static final int Z3_OP_PR_MONOTONICITY = 1289;
  public static final int Z3_OP_PR_QUANT_INTRO = 1290;
  public static final int Z3_OP_PR_DISTRIBUTIVITY = 1291;
  public static final int Z3_OP_PR_AND_ELIM = 1292;
  public static final int Z3_OP_PR_NOT_OR_ELIM = 1293;
  public static final int Z3_OP_PR_REWRITE = 1294;
  public static final int Z3_OP_PR_REWRITE_STAR = 1295;
  public static final int Z3_OP_PR_PULL_QUANT = 1296;
  public static final int Z3_OP_PR_PULL_QUANT_STAR = 1297;
  public static final int Z3_OP_PR_PUSH_QUANT = 1298;
  public static final int Z3_OP_PR_ELIM_UNUSED_VARS = 1299;

  public static final int Z3_OP_PR_DER = 1300;
  public static final int Z3_OP_PR_QUANT_INST = 1301;
  public static final int Z3_OP_PR_HYPOTHESIS = 1302;
  public static final int Z3_OP_PR_LEMMA = 1303;
  public static final int Z3_OP_PR_UNIT_RESOLUTION = 1304;
  public static final int Z3_OP_PR_IFF_TRUE = 1305;
  public static final int Z3_OP_PR_IFF_FALSE = 1306;
  public static final int Z3_OP_PR_COMMUTATIVITY = 1307;
  public static final int Z3_OP_PR_DEF_AXIOM = 1308;
  public static final int Z3_OP_PR_DEF_INTRO = 1309;
  public static final int Z3_OP_PR_APPLY_DEF = 1310;
  public static final int Z3_OP_PR_IFF_OEQ = 1311;
  public static final int Z3_OP_PR_NNF_POS = 1312;
  public static final int Z3_OP_PR_NNF_NEG = 1313;
  public static final int Z3_OP_PR_NNF_STAR = 1314;
  public static final int Z3_OP_PR_CNF_STAR = 1315;
  public static final int Z3_OP_PR_SKOLEMIZE = 1316;
  public static final int Z3_OP_PR_MODUS_PONENS_OEQ = 1317;
  public static final int Z3_OP_PR_TH_LEMMA = 1318;
  public static final int Z3_OP_PR_HYPER_RESOLVE = 1319;

  public static final int Z3_OP_RA_STORE = 1536;
  public static final int Z3_OP_RA_EMPTY = 1537;
  public static final int Z3_OP_RA_IS_EMPTY = 1538;
  public static final int Z3_OP_RA_JOIN = 1539;
  public static final int Z3_OP_RA_UNION = 1540;
  public static final int Z3_OP_RA_WIDEN = 1541;
  public static final int Z3_OP_RA_PROJECT = 1542;
  public static final int Z3_OP_RA_FILTER = 1543;
  public static final int Z3_OP_RA_NEGATION_FILTER = 1544;
  public static final int Z3_OP_RA_RENAME = 1545;
  public static final int Z3_OP_RA_COMPLEMENT = 1546;
  public static final int Z3_OP_RA_SELECT = 1547;
  public static final int Z3_OP_RA_CLONE = 1548;
  public static final int Z3_OP_FD_LT = 1549;

  public static final int Z3_OP_LABEL = 1792;
  public static final int Z3_OP_LABEL_LIT = 1793;

  public static final int Z3_OP_DT_CONSTRUCTOR = 2048;
  public static final int Z3_OP_DT_RECOGNISER = 2049;
  public static final int Z3_OP_DT_ACCESSOR = 2050;
  public static final int Z3_OP_UNINTERPRETED = 2051;
}
