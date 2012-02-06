/* $Id: StructConn,v 1.13 1998/10/12 06:09:57 tony Exp $ */
#include "gsdescr.h"
#include "treestack.h"
#include "HEAD.h"

#define TokenStack(i)	(ZAttributKeller[(i)])

#ifdef RIGHTCOORD
extern POSITION rightpos;
#endif

#if defined(__cplusplus) || defined(__STDC__) 
void
StrukturAnknuepfung(unsigned PR, GRUNDSYMBOLDESKRIPTOR *ZAttributKeller)
#else
void
StrukturAnknuepfung(PR, ZAttributKeller)
unsigned  PR;
register GRUNDSYMBOLDESKRIPTOR  *ZAttributKeller;
#endif
{
  curpos = ZAttributKeller->Pos;
#ifdef RIGHTCOORD
  RLineOf (curpos) = RLineOf (rightpos);
  RColOf (curpos) = RColOf (rightpos);
#ifdef MONITOR
  RCumColOf (curpos) = RCumColOf (rightpos);
#endif
#endif
  switch (PR) {
  case 1: _nst[_nsp]=Mkrule_55(&curpos, _nst[_nsp+0]); break;
  case 2: _nst[_nsp]=Mkrule_93(&curpos, _nst[_nsp+0]); break;
  case 3: _nst[_nsp]=Mkrule_56(&curpos, _nst[_nsp+0]); break;
  case 4: _nst[_nsp]=Mkrule_57(&curpos, _nst[_nsp+0]); break;
  case 5: _nst[_nsp]=Mkrule_92(&curpos, _nst[_nsp+0]); break;
  case 6: _nst[_nsp]=Mkrule_88(&curpos, _nst[_nsp+0]); break;
  case 7: _nst[_nsp]=Mkrule_65(&curpos, _nst[_nsp+0]); break;
  case 8: _nsp -= 1;_nst[_nsp]=Mkrule_94(&curpos, _nst[_nsp+0], _nst[_nsp+1]); break;
  case 9: _nst[_nsp]=Mkrule_78(&curpos, _nst[_nsp+0]); break;
  case 10: _nst[_nsp]=Mkrule_83(&curpos, _nst[_nsp+0]); break;
  case 11: _nst[_nsp]=Mkrule_82(&curpos, _nst[_nsp+0]); break;
  case 12: _nst[_nsp]=Mkrule_63(&curpos, Mkident(&(T_POS(TokenStack(0))), T_ATTR(TokenStack(0))), _nst[_nsp+0]); break;
  case 13: _nsp -= 1;_nst[_nsp]=Mkrule_66(&curpos, _nst[_nsp+0], _nst[_nsp+1]); break;
  case 15: _nst[_nsp]=Mkrule_46(&curpos, _nst[_nsp+0]); break;
  case 19: _nsp -= 1;_nst[_nsp]=Mkrule_105(&curpos, _nst[_nsp+0], _nst[_nsp+1]); break;
  case 20: _nsp -= 1;_nst[_nsp]=Mkrule_79(&curpos, _nst[_nsp+0], _nst[_nsp+1]); break;
  case 21: _nst[_nsp]=Mkrule_25(&curpos, _nst[_nsp+0]); break;
  case 22: _nsp -= 2;_nst[_nsp]=Mkrule_28(&curpos, _nst[_nsp+0], Mkident(&(T_POS(TokenStack(1))), T_ATTR(TokenStack(1))), _nst[_nsp+1], _nst[_nsp+2]); break;
  case 23: _nsp -= 1;_nst[_nsp]=Mkrule_64(&curpos, Mkident(&(T_POS(TokenStack(0))), T_ATTR(TokenStack(0))), _nst[_nsp+0], _nst[_nsp+1]); break;
  case 24: _nsp -= 2;_nst[_nsp]=Mkrule_29(&curpos, _nst[_nsp+0], _nst[_nsp+1], _nst[_nsp+2]); break;
  case 25: _nst[_nsp]=Mkrule_10(&curpos, _nst[_nsp+0]); break;
  case 26: _nst[_nsp]=Mkrule_8(&curpos, _nst[_nsp+0]); break;
  case 27: _nst[_nsp]=Mkrule_17(&curpos, _nst[_nsp+0]); break;
  case 28: _nst[_nsp]=Mkrule_48(&curpos, _nst[_nsp+0]); break;
  case 29: _nst[_nsp]=Mkrule_16(&curpos, _nst[_nsp+0]); break;
  case 30: _nst[_nsp]=Mkrule_14(&curpos, _nst[_nsp+0]); break;
  case 31: _nst[_nsp]=Mkrule_18(&curpos, _nst[_nsp+0]); break;
  case 32: _nst[_nsp]=Mkrule_20(&curpos, _nst[_nsp+0]); break;
  case 33: _nst[_nsp]=Mkrule_15(&curpos, _nst[_nsp+0]); break;
  case 34: _nsp -= 1;_nst[_nsp]=Mkrule_44(&curpos, _nst[_nsp+0], _nst[_nsp+1]); break;
  case 35: _nsp -= 2;_nst[_nsp]=Mkrule_27(&curpos, _nst[_nsp+0], Mkident(&(T_POS(TokenStack(2))), T_ATTR(TokenStack(2))), _nst[_nsp+1], _nst[_nsp+2]); break;
  case 36: _nsp -= 1;_nst[_nsp]=Mkrule_49(&curpos, _nst[_nsp+0], _nst[_nsp+1]); break;
  case 37: _nsp -= 1;_nst[_nsp]=Mkrule_19(&curpos, _nst[_nsp+0], _nst[_nsp+1]); break;
  case 38: _nst[_nsp]=Mkrule_13(&curpos, Mkident(&(T_POS(TokenStack(0))), T_ATTR(TokenStack(0))), _nst[_nsp+0]); break;
  case 39: _nsp -= 2;_nst[_nsp]=Mkrule_9(&curpos, _nst[_nsp+0], _nst[_nsp+1], _nst[_nsp+2]); break;
  case 40: _nsp -= 2;_nst[_nsp]=Mkrule_6(&curpos, _nst[_nsp+0], _nst[_nsp+1], _nst[_nsp+2]); break;
  case 41: _incrnodestack();_nst[_nsp]=Mkrule_104(&curpos); break;
  case 42: _incrnodestack();_nst[_nsp]=Mkrule_95(&curpos); break;
  case 43: _incrnodestack();_nst[_nsp]=Mkrule_100(&curpos); break;
  case 44: _incrnodestack();_nst[_nsp]=Mkrule_101(&curpos); break;
  case 45: _incrnodestack();_nst[_nsp]=Mkrule_99(&curpos); break;
  case 46: _incrnodestack();_nst[_nsp]=Mkrule_96(&curpos); break;
  case 47: _incrnodestack();_nst[_nsp]=Mkrule_97(&curpos); break;
  case 48: _incrnodestack();_nst[_nsp]=Mkrule_98(&curpos); break;
  case 49: _incrnodestack();_nst[_nsp]=Mkrule_102(&curpos); break;
  case 50: _incrnodestack();_nst[_nsp]=Mkrule_103(&curpos); break;
  case 51: _nst[_nsp]=Mkrule_58(&curpos, _nst[_nsp+0]); break;
  case 53: _incrnodestack();_nst[_nsp]=Mkrule_62(&curpos, Mkident(&(T_POS(TokenStack(0))), T_ATTR(TokenStack(0)))); break;
  case 54: _nsp -= 1;_nst[_nsp]=Mkrule_50(&curpos, _nst[_nsp+0], _nst[_nsp+1]); break;
  case 55: _incrnodestack();_nst[_nsp]=Mkrule_81(&curpos, Mkinclude_string(&(T_POS(TokenStack(2))), T_ATTR(TokenStack(2)))); break;
  case 56: _incrnodestack();_nst[_nsp]=Mkrule_75(&curpos); break;
  case 57: _incrnodestack();_nst[_nsp]=Mkrule_106(&curpos); break;
  case 58: _incrnodestack();_nst[_nsp]=Mkrule_45(&curpos); break;
  case 59: _incrnodestack();_nst[_nsp]=Mkrule_47(&curpos); break;
  case 60: _incrnodestack();_nst[_nsp]=Mkrule_59(&curpos, Mkident(&(T_POS(TokenStack(0))), T_ATTR(TokenStack(0)))); break;
  case 61: _incrnodestack();_nst[_nsp]=Mkrule_68(&curpos, Mkinteger(&(T_POS(TokenStack(0))), T_ATTR(TokenStack(0)))); break;
  case 62: _incrnodestack();_nst[_nsp]=Mkrule_107(&curpos); break;
  case 63: _nst[_nsp]=Mkrule_80(&curpos, _nst[_nsp+0], Mkident(&(T_POS(TokenStack(1))), T_ATTR(TokenStack(1)))); break;
  case 64: _nst[_nsp]=Mkrule_76(&curpos, _nst[_nsp+0]); break;
  case 65: _incrnodestack();_nst[_nsp]=Mkrule_77(&curpos); break;
  case 66: _incrnodestack();_nst[_nsp]=Mkrule_30(&curpos); break;
  case 67: _incrnodestack();_nst[_nsp]=Mkrule_31(&curpos); break;
  case 68: _incrnodestack();_nst[_nsp]=Mkrule_33(&curpos); break;
  case 69: _nst[_nsp]=Mkrule_67(&curpos, _nst[_nsp+0]); break;
  case 70: _incrnodestack();_nst[_nsp]=Mkrule_51(&curpos); break;
  case 71: _incrnodestack();_nst[_nsp]=Mkrule_52(&curpos); break;
  case 72: _incrnodestack();_nst[_nsp]=Mkrule_54(&curpos); break;
  case 73: _incrnodestack();_nst[_nsp]=Mkrule_61(&curpos); break;
  case 74: _incrnodestack();_nst[_nsp]=Mkrule_69(&curpos); break;
  case 75: _incrnodestack();_nst[_nsp]=Mkrule_70(&curpos); break;
  case 76: _incrnodestack();_nst[_nsp]=Mkrule_71(&curpos); break;
  case 77: _incrnodestack();_nst[_nsp]=Mkrule_72(&curpos); break;
  case 78: _incrnodestack();_nst[_nsp]=Mkrule_73(&curpos); break;
  case 79: _incrnodestack();_nst[_nsp]=Mkrule_74(&curpos); break;
  case 82: _incrnodestack();_nst[_nsp]=Mkrule_84(&curpos); break;
  case 83: _incrnodestack();_nst[_nsp]=Mkrule_85(&curpos); break;
  case 84: _incrnodestack();_nst[_nsp]=Mkrule_86(&curpos); break;
  case 85: _incrnodestack();_nst[_nsp]=Mkrule_87(&curpos); break;
  case 86: _incrnodestack();_nst[_nsp]=Mkrule_90(&curpos); break;
  case 87: _incrnodestack();_nst[_nsp]=Mkrule_91(&curpos); break;
  case 88: _incrnodestack();_nst[_nsp]=Mkrule_26(&curpos); break;
  case 90: _incrnodestack();_nst[_nsp]=Mkrule_22(&curpos); break;
  case 91: _incrnodestack();_nst[_nsp]=Mkrule_11(&curpos); break;
  case 92: _nsp -= 2;_nst[_nsp]=Mkrule_29(&curpos, _nst[_nsp+0], _nst[_nsp+1], _nst[_nsp+2]); break;
  case 93: _nsp -= 2;_nst[_nsp]=Mkrule_29(&curpos, _nst[_nsp+0], _nst[_nsp+1], _nst[_nsp+2]); break;
  case 94: _nsp -= 2;_nst[_nsp]=Mkrule_29(&curpos, _nst[_nsp+0], _nst[_nsp+1], _nst[_nsp+2]); break;
  case 95: _nsp -= 2;_nst[_nsp]=Mkrule_60(&curpos, _nst[_nsp+0], _nst[_nsp+1], _nst[_nsp+2]); break;
  case 96: _nsp -= 2;_nst[_nsp]=Mkrule_53(&curpos, _nst[_nsp+0], _nst[_nsp+1], _nst[_nsp+2]); break;
  case 97: _nsp -= 2;_nst[_nsp]=Mkrule_32(&curpos, _nst[_nsp+0], _nst[_nsp+1], _nst[_nsp+2]); break;
  case 98: _nsp -= 2;_nst[_nsp]=Mkrule_29(&curpos, _nst[_nsp+0], _nst[_nsp+1], _nst[_nsp+2]); break;
  case 99: _nsp -= 2;_nst[_nsp]=Mkrule_29(&curpos, _nst[_nsp+0], _nst[_nsp+1], _nst[_nsp+2]); break;
  case 100: _nsp -= 2;_nst[_nsp]=Mkrule_89(&curpos, _nst[_nsp+0], _nst[_nsp+1], _nst[_nsp+2]); break;
  case 101: _nsp -= 2;_nst[_nsp]=Mkrule_29(&curpos, _nst[_nsp+0], _nst[_nsp+1], _nst[_nsp+2]); break;
  case 102: _incrnodestack();_nst[_nsp]=Mkrule_38(&curpos); break;
  case 103: _incrnodestack();_nst[_nsp]=Mkrule_39(&curpos); break;
  case 104: _incrnodestack();_nst[_nsp]=Mkrule_40(&curpos); break;
  case 105: _incrnodestack();_nst[_nsp]=Mkrule_41(&curpos); break;
  case 106: _incrnodestack();_nst[_nsp]=Mkrule_42(&curpos); break;
  case 107: _incrnodestack();_nst[_nsp]=Mkrule_43(&curpos); break;
  case 108: _nst[_nsp]=Mkrule_3(&curpos, _nst[_nsp+0]); break;
  case 109: _nst[_nsp]=Mkrule_12(&curpos, _nst[_nsp+0]); break;
  case 110: _nst[_nsp]=Mkrule_21(&curpos, _nst[_nsp+0]); break;
  case 111: _nst[_nsp]=Mkrule_23(&curpos, _nst[_nsp+0]); break;
  case 112: _nsp -= 1;_nst[_nsp]=Mkrule_24(&curpos, _nst[_nsp+0], _nst[_nsp+1]); break;
  case 113: _incrnodestack();_nst[_nsp]=Mkrule_5(&curpos, Mkident(&(T_POS(TokenStack(1))), T_ATTR(TokenStack(1)))); break;
  case 114: _nst[_nsp]=Mkrule_4(&curpos, _nst[_nsp+0]); break;
  case 115: _incrnodestack();_nst[_nsp]=Mkrule_36(&curpos); break;
  case 116: _nst[_nsp]=Mkrule_35(&curpos, _nst[_nsp+0]); break;
  case 117: _nst[_nsp]=Mkrule_37(&curpos, _nst[_nsp+0]); break;
  case 118: _nst[_nsp]=Mkrule_2(&curpos, Mkident(&(T_POS(TokenStack(0))), T_ATTR(TokenStack(0))), _nst[_nsp+0]); break;
  case 119: _nsp -= 1;_nst[_nsp]=Mkrule_7(&curpos, _nst[_nsp+0], _nst[_nsp+1]); break;
  case 120: _nsp -= 1;_nst[_nsp]=Mkrule_34(&curpos, _nst[_nsp+0], _nst[_nsp+1]); break;
  case 121: _nsp -= 1;_nst[_nsp]=Mkrule_1(&curpos, _nst[_nsp+0], Mkident(&(T_POS(TokenStack(3))), T_ATTR(TokenStack(3))), _nst[_nsp+1]); break;
  } /* end switch */
}
