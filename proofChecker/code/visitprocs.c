
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
UINT _AVfunction_call_expression_mark1_RuleAttr_58;
UINT _AVfunction_call_expression_mark1_RuleAttr_57;
UINT _AVfunction_call_expression_result;
UINT _AVfunction_call_expression_input;
UINT _AVassignment_expression_result;
UINT _AVassignment_expression_input;
UINT _AVexpression_opt_result;
UINT _AVexpression_opt_input;
UINT _AVselection_statement_mark2_RuleAttr_63;
UINT _AVselection_statement_mark3_RuleAttr_62;
UINT _AVselection_statement_result;
UINT _AVexpression_statement_result;
UINT _AVexpression_statement_input;
UINT _AVlabeled_statement_result;
UINT _AVlabeled_statement_input;
UINT _AVstatement_input;
UINT _AVstatement_list_result;
UINT _AVstatement_list_input;
UINT _AVstatement_list_opt_result;
UINT _AVstatement_list_opt_input;
UINT _AVcompound_statement_result;
UINT _AVcompound_statement_input;
UINT _AVjump_statement_result;
UINT _AVcompound_statement_opt_result;
UINT _AVcompound_statement_opt_input;
UINT _AVfunction_definition_mark1_RuleAttr_84;
UINT _AVfunction_definition_mark1_RuleAttr_83;

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
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2);
_AVfunction_call_expression_mark1_RuleAttr_57=new_node();
/*SPC(313)*/
create_edge(_AVfunction_call_expression_input, _AVfunction_call_expression_mark1_RuleAttr_57, StringTable(_currn->_ATTERM_1));
/*SPC(314)*/
_AVfunction_call_expression_result=_AVfunction_call_expression_mark1_RuleAttr_57;
/*SPC(315)*/

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
_AVfunction_call_expression_mark1_RuleAttr_58=new_node();
/*SPC(307)*/
create_edge(_AVfunction_call_expression_input, _AVfunction_call_expression_mark1_RuleAttr_58, StringTable(_currn->_ATTERM_1));
/*SPC(308)*/
_AVfunction_call_expression_result=_AVfunction_call_expression_mark1_RuleAttr_58;
/*SPC(309)*/

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
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVfunction_call_expression_result=_AVfunction_call_expression_input;
/*SPC(303)*/

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
_AVexpression_opt_input=_currn->_ATinput;
/*SPC(297)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVjump_statement_result=new_node();
/*SPC(298)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_5(_TPPrule_5 _currn)
#else
void _VS1rule_5(_currn )
_TPPrule_5 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVjump_statement_result=new_node();
/*SPC(293)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS2rule_5(_TPPrule_5 _currn)
#else
void _VS2rule_5(_currn )
_TPPrule_5 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
create_edge(_currn->_ATinput, resolve_label(StringTable(_currn->_ATTERM_1)), StringTable(_currn->_ATTERM_1));
/*SPC(292)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_6(_TPPrule_6 _currn,UINT* _AS0input,UINT* _AS0mark2_RuleAttr_62,UINT* _AS0mark1_RuleAttr_62,UINT* _AS0mark1_RuleAttr_63)
#else
void _VS1rule_6(_currn ,_AS0input,_AS0mark2_RuleAttr_62,_AS0mark1_RuleAttr_62,_AS0mark1_RuleAttr_63)
_TPPrule_6 _currn;
UINT* _AS0mark1_RuleAttr_63;
UINT* _AS0mark1_RuleAttr_62;
UINT* _AS0mark2_RuleAttr_62;
UINT* _AS0input;

#endif
{
UINT _AS3result;
UINT _AS2result;

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(* _AS0mark1_RuleAttr_62)=new_node();
/*SPC(276)*/
_AVstatement_input=(* _AS0mark1_RuleAttr_62);
/*SPC(280)*/
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2,(&( _AS2result)));
(* _AS0mark2_RuleAttr_62)=new_node();
/*SPC(277)*/
_AVstatement_input=(* _AS0mark2_RuleAttr_62);
/*SPC(284)*/
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS1MAP[_currn->_desc3->_prod])))((NODEPTR) _currn->_desc3,(&( _AS3result)));
_AVselection_statement_mark3_RuleAttr_62=new_node();
/*SPC(278)*/
create_edge((* _AS0input), (* _AS0mark1_RuleAttr_62), "if branch 1 start");
/*SPC(281)*/
create_edge(_AS2result, _AVselection_statement_mark3_RuleAttr_62, "if branch 1 merge");
/*SPC(282)*/
create_edge((* _AS0input), (* _AS0mark2_RuleAttr_62), "if branch 2 start");
/*SPC(285)*/
create_edge(_AS3result, _AVselection_statement_mark3_RuleAttr_62, "if branch 2 merge");
/*SPC(286)*/
_AVselection_statement_result=_AVselection_statement_mark3_RuleAttr_62;
/*SPC(288)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS2rule_6(_TPPrule_6 _currn,UINT* _AS0input,UINT* _AS0mark2_RuleAttr_62,UINT* _AS0mark1_RuleAttr_62,UINT* _AS0mark1_RuleAttr_63)
#else
void _VS2rule_6(_currn ,_AS0input,_AS0mark2_RuleAttr_62,_AS0mark1_RuleAttr_62,_AS0mark1_RuleAttr_63)
_TPPrule_6 _currn;
UINT* _AS0mark1_RuleAttr_63;
UINT* _AS0mark1_RuleAttr_62;
UINT* _AS0mark2_RuleAttr_62;
UINT* _AS0input;

#endif
{
UINT _AS3result;
UINT _AS2result;

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS2MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2,(&( _AS2result)));
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS2MAP[_currn->_desc3->_prod])))((NODEPTR) _currn->_desc3,(&( _AS3result)));

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_7(_TPPrule_7 _currn,UINT* _AS0input,UINT* _AS0mark2_RuleAttr_62,UINT* _AS0mark1_RuleAttr_62,UINT* _AS0mark1_RuleAttr_63)
#else
void _VS1rule_7(_currn ,_AS0input,_AS0mark2_RuleAttr_62,_AS0mark1_RuleAttr_62,_AS0mark1_RuleAttr_63)
_TPPrule_7 _currn;
UINT* _AS0mark1_RuleAttr_63;
UINT* _AS0mark1_RuleAttr_62;
UINT* _AS0mark2_RuleAttr_62;
UINT* _AS0input;

#endif
{
UINT _AS2result;

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(* _AS0mark1_RuleAttr_63)=new_node();
/*SPC(264)*/
_AVstatement_input=(* _AS0mark1_RuleAttr_63);
/*SPC(267)*/
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2,(&( _AS2result)));
_AVselection_statement_mark2_RuleAttr_63=new_node();
/*SPC(265)*/
create_edge((* _AS0input), (* _AS0mark1_RuleAttr_63), "if branch start");
/*SPC(268)*/
create_edge(_AS2result, _AVselection_statement_mark2_RuleAttr_63, "if branch merge");
/*SPC(269)*/
create_edge((* _AS0input), _AVselection_statement_mark2_RuleAttr_63, "if branch skip");
/*SPC(270)*/
_AVselection_statement_result=_AVselection_statement_mark2_RuleAttr_63;
/*SPC(272)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS2rule_7(_TPPrule_7 _currn,UINT* _AS0input,UINT* _AS0mark2_RuleAttr_62,UINT* _AS0mark1_RuleAttr_62,UINT* _AS0mark1_RuleAttr_63)
#else
void _VS2rule_7(_currn ,_AS0input,_AS0mark2_RuleAttr_62,_AS0mark1_RuleAttr_62,_AS0mark1_RuleAttr_63)
_TPPrule_7 _currn;
UINT* _AS0mark1_RuleAttr_63;
UINT* _AS0mark1_RuleAttr_62;
UINT* _AS0mark2_RuleAttr_62;
UINT* _AS0input;

#endif
{
UINT _AS2result;

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS2MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2,(&( _AS2result)));

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
_AVfunction_call_expression_input=_AVassignment_expression_input;
/*SPC(259)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVassignment_expression_result=_AVfunction_call_expression_result;
/*SPC(260)*/

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
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2);
_AVfunction_call_expression_input=_AVassignment_expression_input;
/*SPC(254)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc3->_prod])))((NODEPTR) _currn->_desc3);
_AVassignment_expression_result=_AVfunction_call_expression_result;
/*SPC(255)*/

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
_AVassignment_expression_input=_AVexpression_opt_input;
/*SPC(249)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVexpression_opt_result=_AVassignment_expression_result;
/*SPC(250)*/

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
_AVexpression_opt_result=_AVexpression_opt_input;
/*SPC(245)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_12(_TPPrule_12 _currn)
#else
void _VS1rule_12(_currn )
_TPPrule_12 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVexpression_opt_input=_AVexpression_statement_input;
/*SPC(240)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVexpression_statement_result=_AVexpression_opt_result;
/*SPC(241)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_13(_TPPrule_13 _currn)
#else
void _VS1rule_13(_currn )
_TPPrule_13 _currn;

#endif
{
UINT _AS1result;

_VisitVarDecl()
_VisitEntry();
_AVstatement_input=_AVlabeled_statement_input;
/*SPC(235)*/
register_node(StringTable(_currn->_ATTERM_1), _AVlabeled_statement_input);
/*SPC(234)*/
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1,(&( _AS1result)));
_AVlabeled_statement_result=_AS1result;
/*SPC(236)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS2rule_13(_TPPrule_13 _currn)
#else
void _VS2rule_13(_currn )
_TPPrule_13 _currn;

#endif
{
UINT _AS1result;

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS2MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1,(&( _AS1result)));

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

_VisitVarDecl()
_VisitEntry();
_currn->_desc1->_ATinput=_AVstatement_input;
/*SPC(229)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(* _AS0result)=_AVjump_statement_result;
/*SPC(230)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS2rule_14(_TPPrule_14 _currn,UINT* _AS0result)
#else
void _VS2rule_14(_currn ,_AS0result)
_TPPrule_14 _currn;
UINT* _AS0result;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS2MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);

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
UINT _AS1mark1_RuleAttr_63;
UINT _AS1mark1_RuleAttr_62;
UINT _AS1mark2_RuleAttr_62;
UINT _AS1input;

_VisitVarDecl()
_VisitEntry();
_AS1input=_AVstatement_input;
/*SPC(224)*/
(*(_CALL_VS_((NODEPTR ,UINT*,UINT*,UINT*,UINT*)) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1,(&( _AS1input)),(&( _AS1mark2_RuleAttr_62)),(&( _AS1mark1_RuleAttr_62)),(&( _AS1mark1_RuleAttr_63)));
(* _AS0result)=_AVselection_statement_result;
/*SPC(225)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS2rule_15(_TPPrule_15 _currn,UINT* _AS0result)
#else
void _VS2rule_15(_currn ,_AS0result)
_TPPrule_15 _currn;
UINT* _AS0result;

#endif
{
UINT _AS1mark1_RuleAttr_63;
UINT _AS1mark1_RuleAttr_62;
UINT _AS1mark2_RuleAttr_62;
UINT _AS1input;

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR ,UINT*,UINT*,UINT*,UINT*)) (VS2MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1,(&( _AS1input)),(&( _AS1mark2_RuleAttr_62)),(&( _AS1mark1_RuleAttr_62)),(&( _AS1mark1_RuleAttr_63)));

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
_AVexpression_statement_input=_AVstatement_input;
/*SPC(219)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(* _AS0result)=_AVexpression_statement_result;
/*SPC(220)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS2rule_16(_TPPrule_16 _currn,UINT* _AS0result)
#else
void _VS2rule_16(_currn ,_AS0result)
_TPPrule_16 _currn;
UINT* _AS0result;

#endif
{

_VisitVarDecl()
_VisitEntry();

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

_VisitVarDecl()
_VisitEntry();
_AVcompound_statement_input=_AVstatement_input;
/*SPC(214)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(* _AS0result)=_AVcompound_statement_result;
/*SPC(215)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS2rule_17(_TPPrule_17 _currn,UINT* _AS0result)
#else
void _VS2rule_17(_currn ,_AS0result)
_TPPrule_17 _currn;
UINT* _AS0result;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS2MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_18(_TPPrule_18 _currn,UINT* _AS0result)
#else
void _VS1rule_18(_currn ,_AS0result)
_TPPrule_18 _currn;
UINT* _AS0result;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVlabeled_statement_input=_AVstatement_input;
/*SPC(209)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(* _AS0result)=_AVlabeled_statement_result;
/*SPC(210)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS2rule_18(_TPPrule_18 _currn,UINT* _AS0result)
#else
void _VS2rule_18(_currn ,_AS0result)
_TPPrule_18 _currn;
UINT* _AS0result;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS2MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_19(_TPPrule_19 _currn)
#else
void _VS1rule_19(_currn )
_TPPrule_19 _currn;

#endif
{
UINT _AS2result;

_VisitVarDecl()
_VisitEntry();
_AVstatement_list_input=_AVstatement_list_input;
/*SPC(203)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVstatement_input=_AVstatement_list_result;
/*SPC(204)*/
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2,(&( _AS2result)));
_AVstatement_list_result=_AS2result;
/*SPC(205)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS2rule_19(_TPPrule_19 _currn)
#else
void _VS2rule_19(_currn )
_TPPrule_19 _currn;

#endif
{
UINT _AS2result;

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS2MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS2MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2,(&( _AS2result)));

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_20(_TPPrule_20 _currn)
#else
void _VS1rule_20(_currn )
_TPPrule_20 _currn;

#endif
{
UINT _AS1result;

_VisitVarDecl()
_VisitEntry();
_AVstatement_input=_AVstatement_list_input;
/*SPC(198)*/
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1,(&( _AS1result)));
_AVstatement_list_result=_AS1result;
/*SPC(199)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS2rule_20(_TPPrule_20 _currn)
#else
void _VS2rule_20(_currn )
_TPPrule_20 _currn;

#endif
{
UINT _AS1result;

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR ,UINT*)) (VS2MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1,(&( _AS1result)));

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
_AVstatement_list_input=_AVstatement_list_opt_input;
/*SPC(193)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVstatement_list_opt_result=_AVstatement_list_result;
/*SPC(194)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS2rule_21(_TPPrule_21 _currn)
#else
void _VS2rule_21(_currn )
_TPPrule_21 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS2MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);

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
_AVstatement_list_opt_result=_AVstatement_list_opt_input;
/*SPC(189)*/

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
_AVstatement_list_opt_input=_AVcompound_statement_input;
/*SPC(184)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVcompound_statement_result=_AVstatement_list_opt_result;
/*SPC(185)*/

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
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVstatement_list_opt_input=_AVcompound_statement_input;
/*SPC(179)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2);
_AVcompound_statement_result=_AVstatement_list_opt_result;
/*SPC(180)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS2rule_24(_TPPrule_24 _currn)
#else
void _VS2rule_24(_currn )
_TPPrule_24 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS2MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2);

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
_AVcompound_statement_input=_AVcompound_statement_opt_input;
/*SPC(174)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
_AVcompound_statement_opt_result=_AVcompound_statement_result;
/*SPC(175)*/

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
_AVcompound_statement_opt_result=_AVcompound_statement_opt_input;
/*SPC(170)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_27(_TPPrule_27 _currn)
#else
void _VS1rule_27(_currn )
_TPPrule_27 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2);
_AVfunction_definition_mark1_RuleAttr_83=new_node();
/*SPC(162)*/
_AVcompound_statement_opt_input=_AVfunction_definition_mark1_RuleAttr_83;
/*SPC(163)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc3->_prod])))((NODEPTR) _currn->_desc3);
(*(_CALL_VS_((NODEPTR )) (VS2MAP[_currn->_desc3->_prod])))((NODEPTR) _currn->_desc3);
traverse_cfa(_AVfunction_definition_mark1_RuleAttr_83, StringTable(_currn->_ATTERM_1));
/*SPC(166)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_28(_TPPrule_28 _currn)
#else
void _VS1rule_28(_currn )
_TPPrule_28 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2);
_AVfunction_definition_mark1_RuleAttr_84=new_node();
/*SPC(154)*/
_AVcompound_statement_opt_input=_AVfunction_definition_mark1_RuleAttr_84;
/*SPC(155)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc3->_prod])))((NODEPTR) _currn->_desc3);
(*(_CALL_VS_((NODEPTR )) (VS2MAP[_currn->_desc3->_prod])))((NODEPTR) _currn->_desc3);
traverse_cfa(_AVfunction_definition_mark1_RuleAttr_84, StringTable(_currn->_ATTERM_1));
/*SPC(158)*/

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_105(_TPPrule_105 _currn)
#else
void _VS1rule_105(_currn )
_TPPrule_105 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc2->_prod])))((NODEPTR) _currn->_desc2);

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_93(_TPPrule_93 _currn)
#else
void _VS1rule_93(_currn )
_TPPrule_93 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);

_VisitExit();
}

#if defined(__STDC__) || defined(__cplusplus)
void _VS1rule_89(_TPPrule_89 _currn)
#else
void _VS1rule_89(_currn )
_TPPrule_89 _currn;

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
void _VS1rule_88(_TPPrule_88 _currn)
#else
void _VS1rule_88(_currn )
_TPPrule_88 _currn;

#endif
{

_VisitVarDecl()
_VisitEntry();
_AVroot_Env=RootEnv;
/*SPC(13)*/
(*(_CALL_VS_((NODEPTR )) (VS1MAP[_currn->_desc1->_prod])))((NODEPTR) _currn->_desc1);

_VisitExit();
}

