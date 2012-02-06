switch (CaseTbl[c = *p++]) {
case 0:	/* sentinel - probably EOB */
  if (c == '\0') {
    p = TokenStart = TokenEnd = auxNUL(TokenStart, 0);
    if (*p) extcode = NORETURN;
    else {
      p = TokenStart = TokenEnd = auxEOF(TokenStart, 0);
      if (*p) extcode = NORETURN;
      else { extcode = EOFTOKEN; EndOfText(p, 0, &extcode, v); }
    }
    goto done;
  } else {
    obstack_grow(Csm_obstk, "char '", 6);
    obstack_cchgrow(Csm_obstk, c);
    message(
      ERROR,
      (char *)obstack_copy0(Csm_obstk, "' is not a token", 16),
      0,
      &curpos);
    TokenEnd = p;
    continue;
  }
  
case 1:	/* space */
  while (scanTbl[c = *p++] & 1<<0) ;
  TokenEnd = p - 1;
  continue;
case 2:	/* tab */
  do { StartLine -= TABSIZE(p - StartLine); }
  while (scanTbl[c = *p++] & 1<<1);
  TokenEnd = p - 1;
  continue;
case 4:	/* carriage return */
  if (*p == '\n') { TokenEnd = p; continue; }
case 3:	/* newline */
  do { LineNum++; } while (scanTbl[c = *p++] & 1<<2);
  StartLine = (TokenEnd = p - 1) - 1;
  continue;

case 5:	/* Entered on:  A-Z _ a c-d h j-k m-n p-q s w-z */
	St_22:
		if( scanTbl[(c= *p++)+0] & 1<< 3){ /*  0-9 A-Z _ a-z */
			goto St_60;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}

case 6:	/* Entered on: 1-9 */
	St_16:
		if( scanTbl[(c= *p++)+0] & 1<< 4){ /*  0-9 */
			goto St_52;}
		else if( scanTbl[c+0] & 1<< 5){ /*  U u */
			goto St_50;}
		else if( scanTbl[c+0] & 1<< 6){ /*  L l */
			goto St_49;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 25;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}

case 7:	/* Entered on: | */
	St_36:
		if((c= *p++) ==124) {			goto St_75;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 23;
			goto done;
			}

case 8:	/* Entered on: v */
	St_34:
		if( scanTbl[(c= *p++)+0] & 1<< 7){ /*  0-9 A-Z _ a-n p-z */
			goto St_60;}
		else if(c ==111) {			goto St_74;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}

case 9:	/* Entered on: u */
	St_33:
		if( scanTbl[(c= *p++)+256] & 1<< 0){ /*  0-9 A-Z _ a-m o-z */
			goto St_60;}
		else if(c ==110) {			goto St_73;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}

case 10:	/* Entered on: t */
	St_32:
		if( scanTbl[(c= *p++)+256] & 1<< 1){ /*  0-9 A-Z _ a-q s-z */
			goto St_60;}
		else if(c ==114) {			goto St_72;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}

case 11:	/* Entered on: r */
	St_31:
		if( scanTbl[(c= *p++)+256] & 1<< 2){ /*  0-9 A-Z _ a-d f-z */
			goto St_60;}
		else if(c ==101) {			goto St_71;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}

case 12:	/* Entered on: o */
	St_30:
		if( scanTbl[(c= *p++)+256] & 1<< 3){ /*  0-9 A-Z _ a-e g-z */
			goto St_60;}
		else if(c ==102) {			goto St_70;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}

case 13:	/* Entered on: l */
	St_29:
		if( scanTbl[(c= *p++)+0] & 1<< 7){ /*  0-9 A-Z _ a-n p-z */
			goto St_60;}
		else if(c ==111) {			goto St_69;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}

case 14:	/* Entered on: i */
	St_28:
		if( scanTbl[(c= *p++)+256] & 1<< 4){ /*  0-9 A-Z _ a-e g-m o-z */
			goto St_60;}
		else if(c ==110) {			goto St_68;}
		else if(c ==102) {			goto St_67;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}

case 15:	/* Entered on: g */
	St_27:
		if( scanTbl[(c= *p++)+0] & 1<< 7){ /*  0-9 A-Z _ a-n p-z */
			goto St_60;}
		else if(c ==111) {			goto St_66;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}

case 16:	/* Entered on: f */
	St_26:
		if( scanTbl[(c= *p++)+256] & 1<< 5){ /*  0-9 A-Z _ b-z */
			goto St_60;}
		else if(c ==97) {			goto St_65;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}

case 17:	/* Entered on: e */
	St_25:
		if( scanTbl[(c= *p++)+256] & 1<< 6){ /*  0-9 A-Z _ a-k m-w y-z */
			goto St_60;}
		else if(c ==120) {			goto St_64;}
		else if(c ==108) {			goto St_63;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}

case 18:	/* Entered on: b */
	St_24:
		if( scanTbl[(c= *p++)+0] & 1<< 7){ /*  0-9 A-Z _ a-n p-z */
			goto St_60;}
		else if(c ==111) {			goto St_62;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}

case 19:	/* Entered on: ^ */
	St_23:
		if((c= *p++) ==94) {			goto St_61;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 21;
			goto done;
			}

case 20:	/* Entered on: > */
	St_21:
		if((c= *p++) ==62) {			goto St_59;}
		else if(c ==61) {			goto St_58;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 44;
			goto done;
			}

case 21:	/* Entered on: = */
	St_20:
		if((c= *p++) ==61) {			goto St_57;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 6;
			goto done;
			}

case 22:	/* Entered on: < */
	St_19:
		if( scanTbl[(c= *p++)+256] & 1<< 7){ /*  . A-Z a-z */
		extcode = 42;/* remember fallback*/
		TokenEnd = p-1;

		scan = NULL;
		proc = NULL;
			goto St_53;}
		else if(c ==62) {			goto St_56;}
		else if(c ==61) {			goto St_55;}
		else if(c ==60) {			goto St_54;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 42;
			goto done;
			}

case 23:	/* Entered on: 0 */
	St_15:
		if( scanTbl[(c= *p++)+512] & 1<< 0){ /*  0-7 */
			goto St_48;}
		else if( scanTbl[c+512] & 1<< 1){ /*  X x */
		extcode = 25;/* remember fallback*/
		TokenEnd = p-1;

		scan = NULL;
		proc = mkidn;
			goto St_51;}
		else if( scanTbl[c+0] & 1<< 5){ /*  U u */
			goto St_50;}
		else if( scanTbl[c+0] & 1<< 6){ /*  L l */
			goto St_49;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 25;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}

case 24:	/* Entered on: / */
	St_14:
		if((c= *p++) ==61) {			goto St_47;}
		else if(c ==42) {			goto St_46;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 32;
			goto done;
			}

case 25:	/* Entered on: - */
	St_13:
		if((c= *p++) ==61) {			goto St_45;}
		else if(c ==45) {			goto St_44;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 2;
			goto done;
			}

case 26:	/* Entered on: + */
	St_11:
		if((c= *p++) ==61) {			goto St_43;}
		else if(c ==43) {			goto St_42;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 3;
			goto done;
			}

case 27:	/* Entered on: * */
	St_10:
		if((c= *p++) ==61) {			goto St_41;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 33;
			goto done;
			}

case 28:	/* Entered on: & */
	St_7:
		if((c= *p++) ==38) {			goto St_40;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 4;
			goto done;
			}

case 29:	/* Entered on: % */
	St_6:
		if((c= *p++) ==61) {			goto St_39;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 34;
			goto done;
			}

case 30:	/* Entered on: ! */
	St_4:
		if((c= *p++) ==61) {			goto St_38;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 54;
			goto done;
			}


default: TokenEnd=p; extcode=ExtCodeTbl[c]; goto done; /*  # (-) , :-; { } */
}
	St_38:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 20;
			goto done;
	St_39:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 10;
			goto done;
	St_40:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 29;
			goto done;
	St_41:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 9;
			goto done;
	St_42:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 37;
			goto done;
	St_43:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 11;
			goto done;
	St_44:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 36;
			goto done;
	St_45:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 7;
			goto done;
	St_46:
			TokenEnd=p=auxCComment(TokenStart, p-TokenStart);
			extcode = 15001;
			goto done;
	St_47:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 8;
			goto done;
	St_49:
		if( scanTbl[(c= *p++)+0] & 1<< 5){ /*  U u */
			goto St_76;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 25;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_76:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 25;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
	St_50:
		if( scanTbl[(c= *p++)+0] & 1<< 6){ /*  L l */
			goto St_77;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 25;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_77:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 25;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
	St_51:
		if( scanTbl[(c= *p++)+512] & 1<< 2){ /*  0-9 A-F a-f */
			goto St_78;}
		else {--p; goto fallback; }
	St_78:
		/*  0-9 A-F a-f*/
		while(scanTbl[(c= *p++)+512] & 1<< 2);--p;
		if( scanTbl[(c= *p++)+0] & 1<< 5){ /*  U u */
			goto St_50;}
		else if( scanTbl[c+0] & 1<< 6){ /*  L l */
			goto St_49;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 25;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_48:
		/*  0-7*/
		while(scanTbl[(c= *p++)+512] & 1<< 0);--p;
		if( scanTbl[(c= *p++)+0] & 1<< 5){ /*  U u */
			goto St_50;}
		else if( scanTbl[c+0] & 1<< 6){ /*  L l */
			goto St_49;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 25;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_54:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 47;
			goto done;
	St_55:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 41;
			goto done;
	St_56:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 40;
			mkstr(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
	St_53:
		/*  . A-Z a-z*/
		while(scanTbl[(c= *p++)+256] & 1<< 7);--p;
		if((c= *p++) ==62) {			goto St_56;}
		else {--p; goto fallback; }
	St_57:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 19;
			goto done;
	St_58:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 43;
			goto done;
	St_59:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 48;
			goto done;
	St_61:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 31;
			goto done;
	St_62:
		if( scanTbl[(c= *p++)+0] & 1<< 7){ /*  0-9 A-Z _ a-n p-z */
			goto St_60;}
		else if(c ==111) {			goto St_79;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_79:
		if( scanTbl[(c= *p++)+512] & 1<< 3){ /*  0-9 A-Z _ a-k m-z */
			goto St_60;}
		else if(c ==108) {			goto St_91;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_91:
		if( scanTbl[(c= *p++)+0] & 1<< 3){ /*  0-9 A-Z _ a-z */
			goto St_60;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 49;
			goto done;
			}
	St_60:
		/*  0-9 A-Z _ a-z*/
		while(scanTbl[(c= *p++)+0] & 1<< 3);--p;
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
	St_63:
		if( scanTbl[(c= *p++)+512] & 1<< 4){ /*  0-9 A-Z _ a-r t-z */
			goto St_60;}
		else if(c ==115) {			goto St_80;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_80:
		if( scanTbl[(c= *p++)+256] & 1<< 2){ /*  0-9 A-Z _ a-d f-z */
			goto St_60;}
		else if(c ==101) {			goto St_92;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_92:
		if( scanTbl[(c= *p++)+0] & 1<< 3){ /*  0-9 A-Z _ a-z */
			goto St_60;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 46;
			goto done;
			}
	St_64:
		if( scanTbl[(c= *p++)+512] & 1<< 5){ /*  0-9 A-Z _ a-s u-z */
			goto St_60;}
		else if(c ==116) {			goto St_81;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_81:
		if( scanTbl[(c= *p++)+256] & 1<< 2){ /*  0-9 A-Z _ a-d f-z */
			goto St_60;}
		else if(c ==101) {			goto St_93;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_93:
		if( scanTbl[(c= *p++)+256] & 1<< 1){ /*  0-9 A-Z _ a-q s-z */
			goto St_60;}
		else if(c ==114) {			goto St_102;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_102:
		if( scanTbl[(c= *p++)+256] & 1<< 0){ /*  0-9 A-Z _ a-m o-z */
			goto St_60;}
		else if(c ==110) {			goto St_107;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_107:
		if( scanTbl[(c= *p++)+0] & 1<< 3){ /*  0-9 A-Z _ a-z */
			goto St_60;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 50;
			goto done;
			}
	St_65:
		if( scanTbl[(c= *p++)+512] & 1<< 3){ /*  0-9 A-Z _ a-k m-z */
			goto St_60;}
		else if(c ==108) {			goto St_82;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_82:
		if( scanTbl[(c= *p++)+512] & 1<< 4){ /*  0-9 A-Z _ a-r t-z */
			goto St_60;}
		else if(c ==115) {			goto St_94;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_94:
		if( scanTbl[(c= *p++)+256] & 1<< 2){ /*  0-9 A-Z _ a-d f-z */
			goto St_60;}
		else if(c ==101) {			goto St_103;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_103:
		if( scanTbl[(c= *p++)+0] & 1<< 3){ /*  0-9 A-Z _ a-z */
			goto St_60;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 17;
			goto done;
			}
	St_66:
		if( scanTbl[(c= *p++)+512] & 1<< 5){ /*  0-9 A-Z _ a-s u-z */
			goto St_60;}
		else if(c ==116) {			goto St_83;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_83:
		if( scanTbl[(c= *p++)+0] & 1<< 7){ /*  0-9 A-Z _ a-n p-z */
			goto St_60;}
		else if(c ==111) {			goto St_95;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_95:
		if( scanTbl[(c= *p++)+0] & 1<< 3){ /*  0-9 A-Z _ a-z */
			goto St_60;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 26;
			goto done;
			}
	St_67:
		if( scanTbl[(c= *p++)+0] & 1<< 3){ /*  0-9 A-Z _ a-z */
			goto St_60;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 45;
			goto done;
			}
	St_68:
		if( scanTbl[(c= *p++)+512] & 1<< 6){ /*  0-9 A-Z _ a-b d-s u-z */
			goto St_60;}
		else if(c ==116) {			goto St_85;}
		else if(c ==99) {			goto St_84;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_84:
		if( scanTbl[(c= *p++)+512] & 1<< 3){ /*  0-9 A-Z _ a-k m-z */
			goto St_60;}
		else if(c ==108) {			goto St_96;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_96:
		if( scanTbl[(c= *p++)+512] & 1<< 7){ /*  0-9 A-Z _ a-t v-z */
			goto St_60;}
		else if(c ==117) {			goto St_104;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_104:
		if( scanTbl[(c= *p++)+768] & 1<< 0){ /*  0-9 A-Z _ a-c e-z */
			goto St_60;}
		else if(c ==100) {			goto St_108;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_108:
		if( scanTbl[(c= *p++)+256] & 1<< 2){ /*  0-9 A-Z _ a-d f-z */
			goto St_60;}
		else if(c ==101) {			goto St_111;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_111:
		if( scanTbl[(c= *p++)+0] & 1<< 3){ /*  0-9 A-Z _ a-z */
			goto St_60;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 39;
			goto done;
			}
	St_85:
		if( scanTbl[(c= *p++)+0] & 1<< 3){ /*  0-9 A-Z _ a-z */
			goto St_60;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 51;
			goto done;
			}
	St_69:
		if( scanTbl[(c= *p++)+256] & 1<< 0){ /*  0-9 A-Z _ a-m o-z */
			goto St_60;}
		else if(c ==110) {			goto St_86;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_86:
		if( scanTbl[(c= *p++)+768] & 1<< 1){ /*  0-9 A-Z _ a-f h-z */
			goto St_60;}
		else if(c ==103) {			goto St_97;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_97:
		if( scanTbl[(c= *p++)+0] & 1<< 3){ /*  0-9 A-Z _ a-z */
			goto St_60;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 52;
			goto done;
			}
	St_70:
		if( scanTbl[(c= *p++)+0] & 1<< 3){ /*  0-9 A-Z _ a-z */
			goto St_60;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 24;
			goto done;
			}
	St_71:
		if( scanTbl[(c= *p++)+512] & 1<< 5){ /*  0-9 A-Z _ a-s u-z */
			goto St_60;}
		else if(c ==116) {			goto St_87;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_87:
		if( scanTbl[(c= *p++)+512] & 1<< 7){ /*  0-9 A-Z _ a-t v-z */
			goto St_60;}
		else if(c ==117) {			goto St_98;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_98:
		if( scanTbl[(c= *p++)+256] & 1<< 1){ /*  0-9 A-Z _ a-q s-z */
			goto St_60;}
		else if(c ==114) {			goto St_105;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_105:
		if( scanTbl[(c= *p++)+256] & 1<< 0){ /*  0-9 A-Z _ a-m o-z */
			goto St_60;}
		else if(c ==110) {			goto St_109;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_109:
		if( scanTbl[(c= *p++)+0] & 1<< 3){ /*  0-9 A-Z _ a-z */
			goto St_60;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 27;
			goto done;
			}
	St_72:
		if( scanTbl[(c= *p++)+512] & 1<< 7){ /*  0-9 A-Z _ a-t v-z */
			goto St_60;}
		else if(c ==117) {			goto St_88;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_88:
		if( scanTbl[(c= *p++)+256] & 1<< 2){ /*  0-9 A-Z _ a-d f-z */
			goto St_60;}
		else if(c ==101) {			goto St_99;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_99:
		if( scanTbl[(c= *p++)+0] & 1<< 3){ /*  0-9 A-Z _ a-z */
			goto St_60;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 18;
			goto done;
			}
	St_73:
		if( scanTbl[(c= *p++)+512] & 1<< 4){ /*  0-9 A-Z _ a-r t-z */
			goto St_60;}
		else if(c ==115) {			goto St_89;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_89:
		if( scanTbl[(c= *p++)+768] & 1<< 2){ /*  0-9 A-Z _ a-h j-z */
			goto St_60;}
		else if(c ==105) {			goto St_100;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_100:
		if( scanTbl[(c= *p++)+768] & 1<< 1){ /*  0-9 A-Z _ a-f h-z */
			goto St_60;}
		else if(c ==103) {			goto St_106;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_106:
		if( scanTbl[(c= *p++)+256] & 1<< 0){ /*  0-9 A-Z _ a-m o-z */
			goto St_60;}
		else if(c ==110) {			goto St_110;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_110:
		if( scanTbl[(c= *p++)+256] & 1<< 2){ /*  0-9 A-Z _ a-d f-z */
			goto St_60;}
		else if(c ==101) {			goto St_112;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_112:
		if( scanTbl[(c= *p++)+768] & 1<< 0){ /*  0-9 A-Z _ a-c e-z */
			goto St_60;}
		else if(c ==100) {			goto St_113;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_113:
		if( scanTbl[(c= *p++)+0] & 1<< 3){ /*  0-9 A-Z _ a-z */
			goto St_60;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 53;
			goto done;
			}
	St_74:
		if( scanTbl[(c= *p++)+768] & 1<< 2){ /*  0-9 A-Z _ a-h j-z */
			goto St_60;}
		else if(c ==105) {			goto St_90;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_90:
		if( scanTbl[(c= *p++)+768] & 1<< 0){ /*  0-9 A-Z _ a-c e-z */
			goto St_60;}
		else if(c ==100) {			goto St_101;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 22;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
	St_101:
		if( scanTbl[(c= *p++)+0] & 1<< 3){ /*  0-9 A-Z _ a-z */
			goto St_60;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 35;
			goto done;
			}
	St_75:
			TokenEnd= p; /* FINAL, no auxscan, must set */
			extcode = 30;
			goto done;
	St_52:
		/*  0-9*/
		while(scanTbl[(c= *p++)+0] & 1<< 4);--p;
		if( scanTbl[(c= *p++)+0] & 1<< 5){ /*  U u */
			goto St_50;}
		else if( scanTbl[c+0] & 1<< 6){ /*  L l */
			goto St_49;}
		else {
			TokenEnd= (--p); /* FINAL, no auxscan, must set */
			extcode = 25;
			mkidn(TokenStart, TokenEnd-TokenStart,&extcode,v);
			goto done;
			}
