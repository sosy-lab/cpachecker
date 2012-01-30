
/* definition of tree node structure */

#ifndef NODE_H
#define NODE_H
#include "err.h"
#include "nodeptr.h" /* defines NODEPTR */
#include "HEAD.h"

#ifdef MONITOR
#define _NODECOMMON int _prod; POSITION _coord; int _uid;
#else
#define _NODECOMMON int _prod;
#endif
struct NODEPTR_struct {
        _NODECOMMON
#ifdef __cplusplus
        void* operator new(size_t size);
#endif
};

typedef struct _TSunary_operator* _TSPunary_operator;
typedef struct _TSshift_operator* _TSPshift_operator;
typedef struct _TSsource* _TSPsource;
typedef struct _TSroot* _TSProot;
typedef struct _TSparameter* _TSPparameter;
typedef struct _TSparameter_list* _TSPparameter_list;
typedef struct _TSinitializer* _TSPinitializer;
typedef struct _TSinit_declarator* _TSPinit_declarator;
typedef struct _TSinclusive_OR_operator* _TSPinclusive_OR_operator;
typedef struct _TStranslation_unit* _TSPtranslation_unit;
typedef struct _TSfile* _TSPfile;
typedef struct _TSpre_include* _TSPpre_include;
typedef struct _TSexternal_declaration* _TSPexternal_declaration;
typedef struct _TSexclusive_OR_operator* _TSPexclusive_OR_operator;
typedef struct _TSinit_declarator_list* _TSPinit_declarator_list;
typedef struct _TSdeclaration* _TSPdeclaration;
typedef struct _TSinteger_constant* _TSPinteger_constant;
typedef struct _TSconstant* _TSPconstant;
typedef struct _TSargument_expression_list* _TSPargument_expression_list;
typedef struct _TSand_operator* _TSPand_operator;
typedef struct _TSBinOp* _TSPBinOp;
typedef struct _TSargument_expression_list_opt* _TSPargument_expression_list_opt;
typedef struct _TSExpression* _TSPExpression;
typedef struct _TSfunction_call_expression* _TSPfunction_call_expression;
typedef struct _TSassignment_operator* _TSPassignment_operator;
typedef struct _TSIdUse* _TSPIdUse;
typedef struct _TSassignment_expression* _TSPassignment_expression;
typedef struct _TSexpression_opt* _TSPexpression_opt;
typedef struct _TSselection_statement* _TSPselection_statement;
typedef struct _TSexpression_statement* _TSPexpression_statement;
typedef struct _TSlabeled_statement* _TSPlabeled_statement;
typedef struct _TSstatement* _TSPstatement;
typedef struct _TSstatement_list* _TSPstatement_list;
typedef struct _TSstatement_list_opt* _TSPstatement_list_opt;
typedef struct _TSdeclaration_list* _TSPdeclaration_list;
typedef struct _TScompound_statement* _TSPcompound_statement;
typedef struct _TSjump_statement* _TSPjump_statement;
typedef struct _TScompound_statement_opt* _TSPcompound_statement_opt;
typedef struct _TSparameter_list_opt* _TSPparameter_list_opt;
typedef struct _TStype_specifier* _TSPtype_specifier;
typedef struct _TSfunction_definition* _TSPfunction_definition;
typedef struct _TPrule_1* _TPPrule_1;
typedef struct _TPrule_2* _TPPrule_2;
typedef struct _TPrule_3* _TPPrule_3;
typedef struct _TPrule_4* _TPPrule_4;
typedef struct _TPrule_5* _TPPrule_5;
typedef struct _TPrule_6* _TPPrule_6;
typedef struct _TPrule_7* _TPPrule_7;
typedef struct _TPrule_8* _TPPrule_8;
typedef struct _TPrule_9* _TPPrule_9;
typedef struct _TPrule_10* _TPPrule_10;
typedef struct _TPrule_11* _TPPrule_11;
typedef struct _TPrule_12* _TPPrule_12;
typedef struct _TPrule_13* _TPPrule_13;
typedef struct _TPrule_14* _TPPrule_14;
typedef struct _TPrule_15* _TPPrule_15;
typedef struct _TPrule_16* _TPPrule_16;
typedef struct _TPrule_17* _TPPrule_17;
typedef struct _TPrule_18* _TPPrule_18;
typedef struct _TPrule_19* _TPPrule_19;
typedef struct _TPrule_20* _TPPrule_20;
typedef struct _TPrule_21* _TPPrule_21;
typedef struct _TPrule_22* _TPPrule_22;
typedef struct _TPrule_23* _TPPrule_23;
typedef struct _TPrule_24* _TPPrule_24;
typedef struct _TPrule_25* _TPPrule_25;
typedef struct _TPrule_26* _TPPrule_26;
typedef struct _TPrule_100* _TPPrule_100;
typedef struct _TPrule_99* _TPPrule_99;
typedef struct _TPrule_98* _TPPrule_98;
typedef struct _TPrule_97* _TPPrule_97;
typedef struct _TPrule_96* _TPPrule_96;
typedef struct _TPrule_95* _TPPrule_95;
typedef struct _TPrule_94* _TPPrule_94;
typedef struct _TPrule_93* _TPPrule_93;
typedef struct _TPrule_92* _TPPrule_92;
typedef struct _TPrule_91* _TPPrule_91;
typedef struct _TPrule_90* _TPPrule_90;
typedef struct _TPrule_89* _TPPrule_89;
typedef struct _TPrule_88* _TPPrule_88;
typedef struct _TPrule_87* _TPPrule_87;
typedef struct _TPrule_86* _TPPrule_86;
typedef struct _TPrule_85* _TPPrule_85;
typedef struct _TPrule_84* _TPPrule_84;
typedef struct _TPrule_83* _TPPrule_83;
typedef struct _TPrule_82* _TPPrule_82;
typedef struct _TPrule_81* _TPPrule_81;
typedef struct _TPrule_80* _TPPrule_80;
typedef struct _TPrule_79* _TPPrule_79;
typedef struct _TPrule_78* _TPPrule_78;
typedef struct _TPrule_77* _TPPrule_77;
typedef struct _TPrule_76* _TPPrule_76;
typedef struct _TPrule_75* _TPPrule_75;
typedef struct _TPrule_74* _TPPrule_74;
typedef struct _TPrule_73* _TPPrule_73;
typedef struct _TPrule_72* _TPPrule_72;
typedef struct _TPrule_71* _TPPrule_71;
typedef struct _TPrule_70* _TPPrule_70;
typedef struct _TPrule_69* _TPPrule_69;
typedef struct _TPrule_68* _TPPrule_68;
typedef struct _TPrule_67* _TPPrule_67;
typedef struct _TPrule_66* _TPPrule_66;
typedef struct _TPrule_65* _TPPrule_65;
typedef struct _TPrule_64* _TPPrule_64;
typedef struct _TPrule_63* _TPPrule_63;
typedef struct _TPrule_62* _TPPrule_62;
typedef struct _TPrule_61* _TPPrule_61;
typedef struct _TPrule_60* _TPPrule_60;
typedef struct _TPrule_59* _TPPrule_59;
typedef struct _TPrule_58* _TPPrule_58;
typedef struct _TPrule_57* _TPPrule_57;
typedef struct _TPrule_56* _TPPrule_56;
typedef struct _TPrule_55* _TPPrule_55;
typedef struct _TPrule_54* _TPPrule_54;
typedef struct _TPrule_53* _TPPrule_53;
typedef struct _TPrule_52* _TPPrule_52;
typedef struct _TPrule_51* _TPPrule_51;
typedef struct _TPrule_50* _TPPrule_50;
typedef struct _TPrule_49* _TPPrule_49;
typedef struct _TPrule_48* _TPPrule_48;
typedef struct _TPrule_47* _TPPrule_47;
typedef struct _TPrule_46* _TPPrule_46;
typedef struct _TPrule_45* _TPPrule_45;
typedef struct _TPrule_44* _TPPrule_44;
typedef struct _TPrule_43* _TPPrule_43;
typedef struct _TPrule_42* _TPPrule_42;
typedef struct _TPrule_41* _TPPrule_41;
typedef struct _TPrule_40* _TPPrule_40;
typedef struct _TPrule_39* _TPPrule_39;
typedef struct _TPrule_38* _TPPrule_38;
typedef struct _TPrule_37* _TPPrule_37;
typedef struct _TPrule_36* _TPPrule_36;
typedef struct _TPrule_35* _TPPrule_35;
typedef struct _TPrule_34* _TPPrule_34;
typedef struct _TPrule_33* _TPPrule_33;
typedef struct _TPrule_32* _TPPrule_32;
typedef struct _TPrule_31* _TPPrule_31;
typedef struct _TPrule_30* _TPPrule_30;
typedef struct _TPrule_29* _TPPrule_29;
typedef struct _TPrule_28* _TPPrule_28;
typedef struct _TPrule_27* _TPPrule_27;

struct _TSunary_operator
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSshift_operator
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSsource
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSroot
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSparameter
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSparameter_list
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSinitializer
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSinit_declarator
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSinclusive_OR_operator
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TStranslation_unit
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSfile
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSpre_include
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSexternal_declaration
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSexclusive_OR_operator
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSinit_declarator_list
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSdeclaration
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSinteger_constant
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSconstant
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSargument_expression_list
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSand_operator
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSBinOp
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSargument_expression_list_opt
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSExpression
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSfunction_call_expression
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSassignment_operator
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSIdUse
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSassignment_expression
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSexpression_opt
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSselection_statement
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSexpression_statement
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSlabeled_statement
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSstatement
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSstatement_list
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSstatement_list_opt
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSdeclaration_list
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TScompound_statement
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSjump_statement
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
UINT _ATinput;
};

struct _TScompound_statement_opt
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSparameter_list_opt
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TStype_specifier
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TSfunction_definition
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_1
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPargument_expression_list_opt _desc1;
int _ATTERM_1;
};

struct _TPrule_2
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPExpression _desc1;
};

struct _TPrule_3
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
UINT _ATinput;
_TSPexpression_opt _desc1;
};

struct _TPrule_4
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
UINT _ATinput;
int _ATTERM_1;
};

struct _TPrule_5
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPExpression _desc1;
_TSPstatement _desc2;
_TSPstatement _desc3;
};

struct _TPrule_6
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPExpression _desc1;
_TSPstatement _desc2;
};

struct _TPrule_7
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPfunction_call_expression _desc1;
};

struct _TPrule_8
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPIdUse _desc1;
_TSPassignment_operator _desc2;
_TSPfunction_call_expression _desc3;
};

struct _TPrule_9
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPassignment_expression _desc1;
};

struct _TPrule_10
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_11
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPexpression_opt _desc1;
};

struct _TPrule_12
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPstatement _desc1;
int _ATTERM_1;
};

struct _TPrule_13
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPjump_statement _desc1;
};

struct _TPrule_14
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPselection_statement _desc1;
};

struct _TPrule_15
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPexpression_statement _desc1;
};

struct _TPrule_16
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPcompound_statement _desc1;
};

struct _TPrule_17
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPlabeled_statement _desc1;
};

struct _TPrule_18
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPstatement_list _desc1;
_TSPstatement _desc2;
};

struct _TPrule_19
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPstatement _desc1;
};

struct _TPrule_20
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPstatement_list _desc1;
};

struct _TPrule_21
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_22
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPstatement_list_opt _desc1;
};

struct _TPrule_23
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPdeclaration_list _desc1;
_TSPstatement_list_opt _desc2;
};

struct _TPrule_24
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPcompound_statement _desc1;
};

struct _TPrule_25
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_26
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPtype_specifier _desc1;
_TSPparameter_list_opt _desc2;
_TSPcompound_statement_opt _desc3;
int _ATTERM_1;
};

struct _TPrule_100
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_99
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_98
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPunary_operator _desc1;
_TSPExpression _desc2;
};

struct _TPrule_97
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_96
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_95
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_94
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_93
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_92
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_91
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPtranslation_unit _desc1;
_TSPexternal_declaration _desc2;
};

struct _TPrule_90
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPexternal_declaration _desc1;
};

struct _TPrule_89
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPfile _desc1;
};

struct _TPrule_88
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_87
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_86
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPExpression _desc1;
_TSPshift_operator _desc2;
_TSPExpression _desc3;
};

struct _TPrule_85
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPsource _desc1;
};

struct _TPrule_84
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_83
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_82
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_81
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_80
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPIdUse _desc1;
};

struct _TPrule_79
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPconstant _desc1;
};

struct _TPrule_78
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_77
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPtype_specifier _desc1;
};

struct _TPrule_76
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPparameter_list _desc1;
_TSPparameter _desc2;
};

struct _TPrule_75
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPparameter _desc1;
};

struct _TPrule_74
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_73
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPparameter_list _desc1;
};

struct _TPrule_72
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_71
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_70
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_69
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_68
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_67
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_66
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_65
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_64
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPExpression _desc1;
};

struct _TPrule_63
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPinit_declarator_list _desc1;
_TSPinit_declarator _desc2;
};

struct _TPrule_62
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPinit_declarator _desc1;
};

struct _TPrule_61
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPinteger_constant _desc1;
_TSPtype_specifier _desc2;
};

struct _TPrule_60
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPinitializer _desc1;
};

struct _TPrule_59
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_58
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_57
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPExpression _desc1;
_TSPinclusive_OR_operator _desc2;
_TSPExpression _desc3;
};

struct _TPrule_56
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_55
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPtranslation_unit _desc1;
};

struct _TPrule_54
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPpre_include _desc1;
};

struct _TPrule_53
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPfunction_definition _desc1;
};

struct _TPrule_52
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPdeclaration _desc1;
};

struct _TPrule_51
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_50
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPExpression _desc1;
_TSPexclusive_OR_operator _desc2;
_TSPExpression _desc3;
};

struct _TPrule_49
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_48
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_47
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPtype_specifier _desc1;
_TSPinit_declarator_list _desc2;
};

struct _TPrule_46
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPdeclaration_list _desc1;
_TSPdeclaration _desc2;
};

struct _TPrule_45
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPdeclaration _desc1;
};

struct _TPrule_44
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_43
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPinteger_constant _desc1;
};

struct _TPrule_42
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_41
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_40
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_39
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_38
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_37
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_36
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_35
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPargument_expression_list _desc1;
};

struct _TPrule_34
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_33
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPExpression _desc1;
};

struct _TPrule_32
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPargument_expression_list _desc1;
_TSPExpression _desc2;
};

struct _TPrule_31
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_30
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPExpression _desc1;
_TSPand_operator _desc2;
_TSPExpression _desc3;
};

struct _TPrule_29
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_28
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
};

struct _TPrule_27
#ifdef __cplusplus
	: public NODEPTR_struct {
#else
{
_NODECOMMON
#endif
_TSPExpression _desc1;
_TSPBinOp _desc2;
_TSPExpression _desc3;
};

#undef _NODECOMMON
#endif
