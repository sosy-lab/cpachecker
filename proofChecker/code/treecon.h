
/* interface of tree construction functions */

#ifndef TREECON_H
#define TREECON_H
#include "eliproto.h"

#include "err.h"

#include "nodeptr.h"

#include "HEAD.h"


extern void InitTree ELI_ARG((void));

extern void FreeTree ELI_ARG((void));

extern NODEPTR Mkunary_operator ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkshift_operator ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mksource ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkroot ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkparameter ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkparameter_list ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkinitializer ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkinit_declarator ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkinclusive_OR_operator ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mktranslation_unit ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkfile ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkpre_include ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkexternal_declaration ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkexclusive_OR_operator ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkinit_declarator_list ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkdeclaration ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkinteger_constant ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkconstant ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkargument_expression_list ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkand_operator ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR MkBinOp ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkargument_expression_list_opt ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR MkExpression ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkfunction_call_expression ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkassignment_operator ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR MkIdUse ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkassignment_expression ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkexpression_opt ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkjump_statement ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkselection_statement ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkexpression_statement ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mklabeled_statement ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkstatement ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkstatement_list ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkstatement_list_opt ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkdeclaration_list ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkcompound_statement ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkcompound_statement_opt ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkparameter_list_opt ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mktype_specifier ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkfunction_definition ELI_ARG((POSITION *_coordref, NODEPTR _currn));
extern NODEPTR Mkrule_1 ELI_ARG((POSITION *_coordref, int _TERM1, NODEPTR _desc1));
extern NODEPTR Mkrule_2 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_3 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_4 ELI_ARG((POSITION *_coordref, int _TERM1));
extern NODEPTR Mkrule_5 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2, NODEPTR _desc3));
extern NODEPTR Mkrule_6 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2));
extern NODEPTR Mkrule_7 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_8 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2, NODEPTR _desc3));
extern NODEPTR Mkrule_9 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_10 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_11 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_12 ELI_ARG((POSITION *_coordref, int _TERM1, NODEPTR _desc1));
extern NODEPTR Mkrule_13 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_14 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_15 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_16 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_17 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_18 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2));
extern NODEPTR Mkrule_19 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_20 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_21 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_22 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_23 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2));
extern NODEPTR Mkrule_24 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_25 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_26 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, int _TERM1, NODEPTR _desc2, NODEPTR _desc3));
extern NODEPTR Mkrule_100 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_99 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_98 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2));
extern NODEPTR Mkrule_97 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_96 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_95 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_94 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_93 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_92 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_91 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2));
extern NODEPTR Mkrule_90 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_89 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_88 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_87 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_86 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2, NODEPTR _desc3));
extern NODEPTR Mkrule_85 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_84 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_83 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_82 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_81 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_80 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_79 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_78 ELI_ARG((POSITION *_coordref, int _TERM1));
extern NODEPTR Mkrule_77 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, int _TERM1));
extern NODEPTR Mkrule_76 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2));
extern NODEPTR Mkrule_75 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_74 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_73 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_72 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_71 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_70 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_69 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_68 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_67 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_66 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_65 ELI_ARG((POSITION *_coordref, int _TERM1));
extern NODEPTR Mkrule_64 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_63 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2));
extern NODEPTR Mkrule_62 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_61 ELI_ARG((POSITION *_coordref, int _TERM1, NODEPTR _desc1, NODEPTR _desc2));
extern NODEPTR Mkrule_60 ELI_ARG((POSITION *_coordref, int _TERM1, NODEPTR _desc1));
extern NODEPTR Mkrule_59 ELI_ARG((POSITION *_coordref, int _TERM1));
extern NODEPTR Mkrule_58 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_57 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2, NODEPTR _desc3));
extern NODEPTR Mkrule_56 ELI_ARG((POSITION *_coordref, int _TERM1));
extern NODEPTR Mkrule_55 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_54 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_53 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_52 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_51 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_50 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2, NODEPTR _desc3));
extern NODEPTR Mkrule_49 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_48 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_47 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2));
extern NODEPTR Mkrule_46 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2));
extern NODEPTR Mkrule_45 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_44 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_43 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_42 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_41 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_40 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_39 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_38 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_37 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_36 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_35 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_34 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_33 ELI_ARG((POSITION *_coordref, NODEPTR _desc1));
extern NODEPTR Mkrule_32 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2));
extern NODEPTR Mkrule_31 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_30 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2, NODEPTR _desc3));
extern NODEPTR Mkrule_29 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_28 ELI_ARG((POSITION *_coordref));
extern NODEPTR Mkrule_27 ELI_ARG((POSITION *_coordref, NODEPTR _desc1, NODEPTR _desc2, NODEPTR _desc3));
#define Mkinteger(pos,val) (val)
#define Mkinclude_string(pos,val) (val)
#define Mkident(pos,val) (val)
#endif
