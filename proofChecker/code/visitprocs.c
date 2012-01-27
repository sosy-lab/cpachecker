
#include "HEAD.h"
#include "err.h"
#include "node.h"
#include "visitprocs.h"
#include "attrpredef.h"

#include "visitmap.h"

#ifdef MONITOR
#include "attr_mon_dapto.h"
#include "liga_dapto.h"
#endif

#ifndef _VisitVarDecl
#define _VisitVarDecl()
#endif

#ifndef _VisitEntry
#define _VisitEntry()
#endif

#ifndef _VisitExit
#define _VisitExit()
#endif


#if defined(__STDC__) || defined(__cplusplus)
#define _CALL_VS_(args) (void (*)args)
#else
#define _CALL_VS_(args) 
#endif
Environment _AVroot_Env;
UINT _AVfunction_call_expression_result;
UINT _AVfunction_call_expression_input;
UINT _AVassignment_expression_result;
UINT _AVassignment_expression_input;
UINT _AVexpression_opt_result;
UINT _AVexpression_opt_input;
UINT _AVjump_statement_result;
UINT _AVjump_statement_input;
UINT _AVselection_statement_result;
UINT _AVexpression_statement_result;
UINT _AVexpression_statement_input;
UINT _AVlabeled_statement_result;
UINT _AVstatement_input;
UINT _AVstatement_list_result;
UINT _AVstatement_list_input;
UINT _AVstatement_list_opt_result;
UINT _AVstatement_list_opt_input;
UINT _AVcompound_statement_result;
UINT _AVcompound_statement_input;
UINT _AVcompound_statement_opt_result;
UINT _AVcompound_statement_opt_input;

#if defined(__STDC__) || defined(__cplusplus)
void LIGA_ATTREVAL (NODEPTR _currn)
#else
void LIGA_ATTREVAL (_currn) NODEPTR _currn;
#endif
{(*(VS1MAP[_currn->_prod])) ((NODEPTR)_currn);}
/*SPC(0)*/

#if defined(__STDC__) || defined(__cplusplus)
void _VS0Empty(NODEPTR _currn)
#else
void _VS0Empty(_currn) NODEPTR _currn;
#endif
{ _VisitVarDecl()
_VisitEntry();

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_1(_TPPrule_1 _currn)
#else
void _VS1rule_1(_currn )
_TPPrule_1 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVfunction_call_expression_result=compute_successor(_AVfunction_call_expression_input, StringTable(_currn->_ATTERM_1));
/*SPC(272)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_2(_TPPrule_2 _currn)
#else
void _VS1rule_2(_currn )
_TPPrule_2 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVfunction_call_expression_result=_AVfunction_call_expression_input;
/*SPC(268)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_3(_TPPrule_3 _currn)
#else
void _VS1rule_3(_currn )
_TPPrule_3 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVexpression_opt_input=_AVjump_statement_input;
/*SPC(263)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVjump_statement_result=_AVexpression_opt_result;
/*SPC(264)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_4(_TPPrule_4 _currn)
#else
void _VS1rule_4(_currn )
_TPPrule_4 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVjump_statement_result=_AVjump_statement_input;
/*SPC(259)*/
assert_equal(get_value(StringTable(_currn->_ATTERM_1)), _AVjump_statement_input);
/*SPC(258)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_5(_TPPrule_5 _currn,UINT* _AS0input)
#else
void _VS1rule_5(_currn ,_AS0input)
_TPPrule_5 _currn;
UINT* _AS0input;

#endif
{
UINT _AS3result;
UINT _AS2result;

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVstatement_input=(* _AS0input);
/*SPC(251)*/
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2,(&( _AS2result)));
_AVstatement_input=(* _AS0input);
/*SPC(252)*/
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS1MAP[_currn->_desc3->_prod])))((NODEPTR) _currn->_desc3,(&( _AS3result)));
assert_equal(_AS2result, _AS3result);
/*SPC(253)*/
_AVselection_statement_result=_AS2result;
/*SPC(254)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_6(_TPPrule_6 _currn,UINT* _AS0input)
#else
void _VS1rule_6(_currn ,_AS0input)
_TPPrule_6 _currn;
UINT* _AS0input;

#endif
{
UINT _AS2result;

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVstatement_input=(* _AS0input);
/*SPC(246)*/
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2,(&( _AS2result)));
_AVselection_statement_result=_AS2result;
/*SPC(247)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_7(_TPPrule_7 _currn)
#else
void _VS1rule_7(_currn )
_TPPrule_7 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVfunction_call_expression_input=_AVassignment_expression_input;
/*SPC(241)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVassignment_expression_result=_AVfunction_call_expression_result;
/*SPC(242)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_8(_TPPrule_8 _currn)
#else
void _VS1rule_8(_currn )
_TPPrule_8 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2);
_AVfunction_call_expression_input=_AVassignment_expression_input;
/*SPC(236)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc3->_prod])))((NODEPTR) _currn->_desc3);
_AVassignment_expression_result=_AVfunction_call_expression_result;
/*SPC(237)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_9(_TPPrule_9 _currn)
#else
void _VS1rule_9(_currn )
_TPPrule_9 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVassignment_expression_input=_AVexpression_opt_input;
/*SPC(231)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVexpression_opt_result=_AVassignment_expression_result;
/*SPC(232)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_10(_TPPrule_10 _currn)
#else
void _VS1rule_10(_currn )
_TPPrule_10 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVexpression_opt_result=_AVexpression_opt_input;
/*SPC(227)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_11(_TPPrule_11 _currn)
#else
void _VS1rule_11(_currn )
_TPPrule_11 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVexpression_opt_input=_AVexpression_statement_input;
/*SPC(222)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVexpression_statement_result=_AVexpression_opt_result;
/*SPC(223)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_12(_TPPrule_12 _currn,UINT* _AS0input)
#else
void _VS1rule_12(_currn ,_AS0input)
_TPPrule_12 _currn;
UINT* _AS0input;

#endif
{
UINT _AS1result;

_VisitVarDecl()
_VisitEntry();
_AVstatement_input=(* _AS0input);
/*SPC(217)*/
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1,(&( _AS1result)));
register_value(StringTable(_currn->_ATTERM_1), (* _AS0input));
/*SPC(216)*/
_AVlabeled_statement_result=_AS1result;
/*SPC(218)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_13(_TPPrule_13 _currn,UINT* _AS0result)
#else
void _VS1rule_13(_currn ,_AS0result)
_TPPrule_13 _currn;
UINT* _AS0result;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVjump_statement_input=_AVstatement_input;
/*SPC(211)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(* _AS0result)=_AVjump_statement_result;
/*SPC(212)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_14(_TPPrule_14 _currn,UINT* _AS0result)
#else
void _VS1rule_14(_currn ,_AS0result)
_TPPrule_14 _currn;
UINT* _AS0result;

#endif
{
UINT _AS1input;

_VisitVarDecl()
_VisitEntry();
_AS1input=_AVstatement_input;
/*SPC(206)*/
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1,(&( _AS1input)));
(* _AS0result)=_AVselection_statement_result;
/*SPC(207)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_15(_TPPrule_15 _currn,UINT* _AS0result)
#else
void _VS1rule_15(_currn ,_AS0result)
_TPPrule_15 _currn;
UINT* _AS0result;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVexpression_statement_input=_AVstatement_input;
/*SPC(201)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(* _AS0result)=_AVexpression_statement_result;
/*SPC(202)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_16(_TPPrule_16 _currn,UINT* _AS0result)
#else
void _VS1rule_16(_currn ,_AS0result)
_TPPrule_16 _currn;
UINT* _AS0result;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVcompound_statement_input=_AVstatement_input;
/*SPC(196)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(* _AS0result)=_AVcompound_statement_result;
/*SPC(197)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_17(_TPPrule_17 _currn,UINT* _AS0result)
#else
void _VS1rule_17(_currn ,_AS0result)
_TPPrule_17 _currn;
UINT* _AS0result;

#endif
{
UINT _AS1input;

_VisitVarDecl()
_VisitEntry();
_AS1input=_AVstatement_input;
/*SPC(191)*/
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1,(&( _AS1input)));
(* _AS0result)=_AVlabeled_statement_result;
/*SPC(192)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_18(_TPPrule_18 _currn)
#else
void _VS1rule_18(_currn )
_TPPrule_18 _currn;

#endif
{
UINT _AS2result;

_VisitVarDecl()
_VisitEntry();
_AVstatement_list_input=_AVstatement_list_input;
/*SPC(185)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVstatement_input=_AVstatement_list_result;
/*SPC(186)*/
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2,(&( _AS2result)));
_AVstatement_list_result=_AS2result;
/*SPC(187)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_19(_TPPrule_19 _currn)
#else
void _VS1rule_19(_currn )
_TPPrule_19 _currn;

#endif
{
UINT _AS1result;

_VisitVarDecl()
_VisitEntry();
_AVstatement_input=_AVstatement_list_input;
/*SPC(180)*/
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1,(&( _AS1result)));
_AVstatement_list_result=_AS1result;
/*SPC(181)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_20(_TPPrule_20 _currn)
#else
void _VS1rule_20(_currn )
_TPPrule_20 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVstatement_list_input=_AVstatement_list_opt_input;
/*SPC(175)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVstatement_list_opt_result=_AVstatement_list_result;
/*SPC(176)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_21(_TPPrule_21 _currn)
#else
void _VS1rule_21(_currn )
_TPPrule_21 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVstatement_list_opt_result=_AVstatement_list_opt_input;
/*SPC(171)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_22(_TPPrule_22 _currn)
#else
void _VS1rule_22(_currn )
_TPPrule_22 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVstatement_list_opt_input=_AVcompound_statement_input;
/*SPC(166)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVcompound_statement_result=_AVstatement_list_opt_result;
/*SPC(167)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_23(_TPPrule_23 _currn)
#else
void _VS1rule_23(_currn )
_TPPrule_23 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVstatement_list_opt_input=_AVcompound_statement_input;
/*SPC(161)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2);
_AVcompound_statement_result=_AVstatement_list_opt_result;
/*SPC(162)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_24(_TPPrule_24 _currn)
#else
void _VS1rule_24(_currn )
_TPPrule_24 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVcompound_statement_input=_AVcompound_statement_opt_input;
/*SPC(156)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVcompound_statement_opt_result=_AVcompound_statement_result;
/*SPC(157)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_25(_TPPrule_25 _currn)
#else
void _VS1rule_25(_currn )
_TPPrule_25 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVcompound_statement_opt_result=_AVcompound_statement_opt_input;
/*SPC(152)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_26(_TPPrule_26 _currn)
#else
void _VS1rule_26(_currn )
_TPPrule_26 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2);
_AVcompound_statement_opt_input=get_init_state();
/*SPC(148)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc3->_prod])))((NODEPTR) _currn->_desc3);

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_98(_TPPrule_98 _currn)
#else
void _VS1rule_98(_currn )
_TPPrule_98 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2);

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_90(_TPPrule_90 _currn)
#else
void _VS1rule_90(_currn )
_TPPrule_90 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_86(_TPPrule_86 _currn)
#else
void _VS1rule_86(_currn )
_TPPrule_86 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2);
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc3->_prod])))((NODEPTR) _currn->_desc3);

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_85(_TPPrule_85 _currn)
#else
void _VS1rule_85(_currn )
_TPPrule_85 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVroot_Env=RootEnv;
/*SPC(13)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);

_VisitExit();
}

