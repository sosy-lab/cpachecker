int F14_group_0;
int F14_group_1;
#ifndef LOCAL
extern int __VERIFIER_nondet_int();
#endif

int __SELECTED_FEATURE_CINDENT;
int __SELECTED_FEATURE_DIFF;
int __SELECTED_FEATURE_INS_EXPAND;
int __SELECTED_FEATURE_AUTOCMD;
int __SELECTED_FEATURE_EVAL;
int __SELECTED_FEATURE_MOUSE;
int __SELECTED_FEATURE_VREPLACE;
int __SELECTED_FEATURE_FKMAP;
int __SELECTED_FEATURE_CMDL_INFO;
int __SELECTED_FEATURE_RIGHTLEFT;
int __SELECTED_FEATURE_MBYTE;
int __SELECTED_FEATURE_FOLDING;
int __SELECTED_FEATURE_GUI;
int __SELECTED_FEATURE_DIGRAPHS;
int __SELECTED_FEATURE_SCROLLBIND;
int __SELECTED_FEATURE_CURSORBIND;
int __SELECTED_FEATURE_COMPL_FUNC;
int __SELECTED_FEATURE_VIRTUALEDIT;
int __SELECTED_FEATURE_FIND_ID;
int __SELECTED_FEATURE_GUI_TABLINE;
int __SELECTED_FEATURE_GUI_W32;
int __SELECTED_FEATURE_DND;
int __SELECTED_FEATURE_WINDOWS;
int __SELECTED_FEATURE_QUICKFIX;
int __SELECTED_FEATURE_CMDWIN;
int __SELECTED_FEATURE_CONCEAL;

typedef unsigned char	char_u;
typedef long	linenr_T;
static int TRUE = 1;
static int FALSE = 0;


int edit(int cmdchar, int startln, long count, int restart_edit)
{


	int INSERT = 0x10;	/* Insert mode */
	int LANGMAP = 0x20;	/* Language mapping, can be combined with
					   INSERT and CMDLINE */

	int REPLACE_FLAG = 0x40;	/* Replace mode flag */
	int REPLACE = REPLACE_FLAG + INSERT;
	int	did_restart_edit;

	int update_Insstart_orig;
	int HAVE_SANDBOX;
	int sandbox;
	int e_sandbox;
	int textlock;
	int e_secure;
	int compl_started;
	int compl_busy;
	int VV_INSERTMODE;
	int VV_CHAR;
	int NULL;
	int EVENT_INSERTENTER;
	int where_paste_started;
	int Insstart;
	int did_ai;
	int ai_col;
	int NUL;
	int p_fkmap;
	int p_ri;
	int farsi_text_3;
	int State;
	int VREPLACE;
	int vr_lines_changed;
	int stop_insert_mode;
	int revins_on;
	int revins_chars;
	int revins_legal;
	int revins_scol;
	int arrow_used;
	int has_mbyte;
	int ins_at_eol;
	int need_start_insertmode;
	int ins_need_undo;
	int can_cindent;
	int p_im;
	int p_smd;
	int msg_silent;
	int CURSOR_SHAPE;
	int new_insert_skip;
	int old_indent;
	int Insstart_orig;


   int		c = 0;
   char_u	*ptr;
   int		lastc = 0;
   int		mincol;
   static linenr_T o_lnum = 0;
   int		i;
   int		did_backspace = TRUE;	    /* previous char was backspace */
if( __SELECTED_FEATURE_CINDENT){
   int		line_is_white = FALSE;	    /* line is empty before insert */
}
   linenr_T	old_topline = 0;	    /* topline before insertion */
if( __SELECTED_FEATURE_DIFF){
   int		old_topfill = -1;
}
   int		inserted_space = FALSE;     /* just inserted a space */
   int		replaceState = REPLACE;
   int		nomove = FALSE;		    /* don't move cursor on return */

   /* Remember whether editing was restarted after CTRL-O. */
   did_restart_edit = restart_edit;

   /* sleep before redrawing, needed for "CTRL-O :" that results in an
    * error message */
   check_for_delay(TRUE);

   /* set Insstart_orig to Insstart */
   update_Insstart_orig = TRUE;

if( HAVE_SANDBOX){
   /* Don't allow inserting in the sandbox. */
   if (sandbox != 0)
   {
	EMSG(_(e_sandbox));
	return FALSE;
   }
}
   /* Don't allow changes in the buffer while editing the cmdline.  The
    * caller of getcmdline() may get confused. */
   if (textlock != 0)
   {
	EMSG(_(e_secure));
	return FALSE;
   }

if( __SELECTED_FEATURE_INS_EXPAND){
   /* Don't allow recursive insert mode when busy with completion. */
   if (compl_started || compl_busy || pum_visible())
   {
	EMSG(_(e_secure));
	return FALSE;
   }
   ins_compl_clear();	    /* clear stuff for CTRL-X mode */
}

if( __SELECTED_FEATURE_AUTOCMD){
   /*
    * Trigger InsertEnter autocommands.  Do not do this for "r<CR>" or "grx".
    */
   if (cmdchar != 'r' && cmdchar != 'v')
   {

if( __SELECTED_FEATURE_EVAL){
	if (cmdchar == 'R'){
	    ptr = (char_u *)"r";
	}
	else if (cmdchar == 'V'){
	    ptr = (char_u *)"v";
	}
	else{
	    ptr = (char_u *)"i";
	}
	set_vim_var_string(VV_INSERTMODE, ptr, 1);
	set_vim_var_string(VV_CHAR, NULL, -1);  /* clear v:char */
}

	/* Make sure the cursor didn't move.  Do call check_cursor_col() in
	 * case the text was modified.  Since Insert mode was not started yet
	 * a call to check_cursor_col() may move the cursor, especially with
	 * the "A" command, thus set State to avoid that. Also check that the
	 * line number is still valid (lines may have been deleted).
	 * Do not restore if v:char was set to a non-empty string. */
}

if( __SELECTED_FEATURE_CONCEAL){
   /* Check if the cursor line needs redrawing before changing State.  If
    * 'concealcursor' is "n" it needs to be redrawn without concealing. */
   conceal_check_cursur_line();
}

if( __SELECTED_FEATURE_MOUSE){
   /*
    * When doing a paste with the middle mouse button, Insstart is set to
    * where the paste started.
    */
   if (where_paste_started != 0){
	Insstart = where_paste_started;
   }
   if (!did_ai)
	ai_col = 0;

   if (cmdchar != NUL && restart_edit == 0)
   {
	ResetRedobuff();
	AppendNumberToRedobuff(count);
   }

}
if( __SELECTED_FEATURE_VREPLACE){
	if (cmdchar == 'V' || cmdchar == 'v')
	{
	    /* "gR" or "gr" command */
	    AppendCharToRedobuff('g');
	    AppendCharToRedobuff((cmdchar == 'v') ? 'r' : 'R');
	}else {
	    AppendCharToRedobuff(cmdchar);
	    if (cmdchar == 'g')		    /* "gI" command */
		AppendCharToRedobuff('I');
	    else if (cmdchar == 'r')	    /* "r<CR>" command */
		count = 1;		    /* insert only one <CR> */
	}
   }

   if (cmdchar == 'R')
   {
if( __SELECTED_FEATURE_FKMAP){
	if (p_fkmap && p_ri)
	{
	    beep_flush();
	    EMSG(farsi_text_3);	    /* encoded in Farsi */
	    State = INSERT;
	}
}
	State = REPLACE;
   }
if( __SELECTED_FEATURE_VREPLACE){
   if (cmdchar == 'V' || cmdchar == 'v')
   {
	State = VREPLACE;
	replaceState = VREPLACE;
	vr_lines_changed = 1;
   }
}
	State = INSERT;

   stop_insert_mode = FALSE;

   /*
    * Need to recompute the cursor position, it might move when the cursor is
    * on a TAB or special character.
    */
   curs_columns(TRUE);

   /*
    * Enable langmap or IME, indicated by 'iminsert'.
    * Note that IME may enabled/disabled without us noticing here, thus the
    * 'iminsert' value may not reflect what is actually used.  It is updated
    * when hitting <Esc>.
    */

if( __SELECTED_FEATURE_MOUSE){
   setmouse();
}
if( __SELECTED_FEATURE_CMDL_INFO){
   clear_showcmd();
}
if( __SELECTED_FEATURE_RIGHTLEFT){
   /* there is no reverse replace mode */
   revins_on = (State == INSERT && p_ri);
   if (revins_on)
	undisplay_dollar();
   revins_chars = 0;
   revins_legal = 0;
   revins_scol = -1;
}

   /*
    * Handle restarting Insert mode.
    * Don't do this for "CTRL-O ." (repeat an insert): we get here with
    * restart_edit non-zero, and something in the stuff buffer.
    */
   if (restart_edit != 0 && stuff_empty())
   {
if( __SELECTED_FEATURE_MOUSE){
	/*
	 * After a paste we consider text typed to be part of the insert for
	 * the pasted text. You can backspace over the pasted text too.
	 */
	if (where_paste_started){
	    arrow_used = FALSE;
	}
}else{

	    arrow_used = TRUE;
	restart_edit = 0;
}
	/*
	 * If the cursor was after the end-of-line before the CTRL-O and it is
	 * now at the end-of-line, put it after the end-of-line (this is not
	 * correct in very rare cases).
	 * Also do this if curswant is greater than the current virtual
	 * column.  Eg after "^O$" or "^O80|".
	 */
	validate_virtcol();
	update_curswant();

	ins_at_eol = FALSE;
   } else{
	arrow_used = FALSE;
   }
   /* we are in insert mode now, don't need to start it anymore */
   need_start_insertmode = FALSE;

   /* Need to save the line for undo before inserting the first char. */
   ins_need_undo = TRUE;

if( __SELECTED_FEATURE_MOUSE){
   where_paste_started = 0;
}
if( __SELECTED_FEATURE_CINDENT){
   can_cindent = TRUE;
}
if( __SELECTED_FEATURE_FOLDING){
   /* The cursor line is not in a closed fold, unless 'insertmode' is set or
    * restarting. */
   if (!p_im && did_restart_edit == 0)
	foldOpenCursor();
}

   /*
    * If 'showmode' is set, show the current (insert/replace/..) mode.
    * A warning message for changing a readonly file is given here, before
    * actually changing anything.  It's put after the mode, if any.
    */
   i = 0;
   if (p_smd && msg_silent == 0)
	i = showmode();

   if (!p_im && did_restart_edit == 0)
	change_warning(i == 0 ? 0 : i + 1);

if( CURSOR_SHAPE){
   ui_cursor_shape();		/* may show different cursor shape */
}
if( __SELECTED_FEATURE_DIGRAPHS){
   do_digraph(-1);		/* clear digraphs */
}

   /*
    * Get the current length of the redo buffer, those characters have to be
    * skipped if we want to get to the inserted characters.
    */
   ptr = get_inserted();
   if (ptr == NULL)
	new_insert_skip = 0;
   else
   {
	new_insert_skip = (int)STRLEN(ptr);
	vim_free(ptr);
   }

   old_indent = 0;

  return TRUE;
}
}



int edit_mutation_0(int cmdchar, int startln, long count, int restart_edit)
{
int INSERT = 0x10 ;
int LANGMAP = 0x20 ;
int REPLACE_FLAG = 0x40 ;
int REPLACE = REPLACE_FLAG + INSERT ;
int did_restart_edit ;
int update_Insstart_orig ;
int HAVE_SANDBOX ;
int sandbox ;
int e_sandbox ;
int textlock ;
int e_secure ;
int compl_started ;
int compl_busy ;
int VV_INSERTMODE ;
int VV_CHAR ;
int NULL ;
int EVENT_INSERTENTER ;
int where_paste_started ;
int Insstart ;
int did_ai ;
int ai_col ;
int NUL ;
int p_fkmap ;
int p_ri ;
int farsi_text_3 ;
int State ;
int VREPLACE ;
int vr_lines_changed ;
int stop_insert_mode ;
int revins_on ;
int revins_chars ;
int revins_legal ;
int revins_scol ;
int arrow_used ;
int has_mbyte ;
int ins_at_eol ;
int need_start_insertmode ;
int ins_need_undo ;
int can_cindent ;
int p_im ;
int p_smd ;
int msg_silent ;
int CURSOR_SHAPE ;
int new_insert_skip ;
int old_indent ;
int Insstart_orig ;
int c = 0 ;
char_u  * ptr ;
int lastc = 0 ;
int mincol ;
static linenr_T o_lnum = 0 ;
int i ;
int did_backspace = TRUE ;
if ( __SELECTED_FEATURE_CINDENT ) {
int line_is_white = FALSE ;
}

linenr_T old_topline = 0 ;
if ( __SELECTED_FEATURE_DIFF ) {
int old_topfill = - 1 ;
}

int inserted_space = FALSE ;
int replaceState = REPLACE ;
int nomove = FALSE ;
did_restart_edit = restart_edit ;
check_for_delay ( TRUE ) ;
update_Insstart_orig = TRUE ;
if ( HAVE_SANDBOX ) {
if ( sandbox != 0 ) {
EMSG ( _ ( e_sandbox ) ) ;
return FALSE ;
}

}

if ( textlock != 0 ) {
EMSG ( _ ( e_secure ) ) ;
return FALSE ;
}

if ( __SELECTED_FEATURE_INS_EXPAND ) {
if ( compl_started || compl_busy || pum_visible ( ) ) {
EMSG ( _ ( e_secure ) ) ;
return FALSE ;
}

ins_compl_clear ( ) ;
}

if ( __SELECTED_FEATURE_AUTOCMD ) {
if ( cmdchar != 'r'  && cmdchar != 'v'  ) {
if ( __SELECTED_FEATURE_EVAL ) {
if ( cmdchar == 'R'  ) {
ptr = (  char_u * ) "r"
 ;
;
}
else if ( cmdchar == 'V'  ) {
ptr = (  char_u * ) "v"
 ;
;
}
else {
ptr = (  char_u * ) "i"
 ;
;
}


set_vim_var_string ( VV_INSERTMODE , ptr , 1 ) ;
set_vim_var_string ( VV_CHAR , NULL , - 1 ) ;
}

}

if ( __SELECTED_FEATURE_CONCEAL ) {
conceal_check_cursur_line ( ) ;
}

if ( __SELECTED_FEATURE_MOUSE ) {
if ( where_paste_started != 0 ) {
Insstart = where_paste_started ;
}

if ( ! did_ai )
ai_col = 0 ;

if ( cmdchar != NUL && restart_edit == 0 ) {
ResetRedobuff ( ) ;
AppendNumberToRedobuff ( count ) ;
}

}

if ( __SELECTED_FEATURE_VREPLACE ) {
if ( cmdchar == 'V'  || cmdchar == 'v'  ) {
AppendCharToRedobuff ( 'g'  ) ;
AppendCharToRedobuff (  ( cmdchar == 'v'  ) ? 'r'  : 'R'  ) ;
}
else {
AppendCharToRedobuff ( cmdchar ) ;
if ( cmdchar == 'g'  ) AppendCharToRedobuff ( 'I'  ) ;
else if ( cmdchar == 'r'  )
count = 1 ;


}

}

if ( cmdchar == 'R'  ) {
if ( __SELECTED_FEATURE_FKMAP ) {
if ( p_fkmap && p_ri ) {
beep_flush ( ) ;
EMSG ( farsi_text_3 ) ;
State = INSERT ;
}

}

State = REPLACE ;
}

if ( __SELECTED_FEATURE_VREPLACE ) {
if ( cmdchar == 'V'  || cmdchar == 'v'  ) {
State = VREPLACE ;
replaceState = VREPLACE ;
vr_lines_changed = 1 ;
}

}

State = INSERT ;
stop_insert_mode = FALSE ;
curs_columns ( TRUE ) ;
if ( __SELECTED_FEATURE_MOUSE ) {
setmouse ( ) ;
}

if ( __SELECTED_FEATURE_CMDL_INFO ) {
clear_showcmd ( ) ;
}

if ( __SELECTED_FEATURE_RIGHTLEFT ) {
revins_on = ( State == INSERT && p_ri ) ;
if ( revins_on ) undisplay_dollar ( ) ;

revins_chars = 0 ;
revins_legal = 0 ;
revins_scol = - 1 ;
}

if ( restart_edit != 0 && stuff_empty ( ) ) {
if ( __SELECTED_FEATURE_MOUSE ) {
if ( where_paste_started ) {
arrow_used = FALSE ;
}

}
else {
arrow_used = TRUE ;
restart_edit = 0 ;
}

validate_virtcol ( ) ;
update_curswant ( ) ;
ins_at_eol = FALSE ;
}
else {
arrow_used = FALSE ;
}

need_start_insertmode = FALSE ;
ins_need_undo = TRUE ;
if ( __SELECTED_FEATURE_MOUSE ) {
where_paste_started = 0 ;
}

if ( __SELECTED_FEATURE_CINDENT ) {
can_cindent = TRUE ;
}

if ( __SELECTED_FEATURE_FOLDING ) {
if ( ! p_im && did_restart_edit == 0 ) foldOpenCursor ( ) ;

}

i = 0 ;
if ( p_smd && msg_silent == 0 )
i = showmode ( ) ;

if ( ! p_im && did_restart_edit == 0 ) change_warning (  i == 0 ? 0 : i + 1 ) ;

if ( CURSOR_SHAPE ) {
ui_cursor_shape ( ) ;
}

if ( __SELECTED_FEATURE_DIGRAPHS ) {
do_digraph ( - 1 ) ;
}

ptr = get_inserted ( ) ;
MUTATION /** ORRN **/:if ( ptr >= NULL )
new_insert_skip = 0 ;
else {
new_insert_skip = (  int ) STRLEN ( ptr ) ;
;
vim_free ( ptr ) ;
}

old_indent = 0 ;
return TRUE ;
}

}

int isValid(){
	 if ((__SELECTED_FEATURE_CINDENT) &&
((__SELECTED_FEATURE_DIFF || !__SELECTED_FEATURE_CINDENT) && (!__SELECTED_FEATURE_DIFF || __SELECTED_FEATURE_CINDENT)) &&
((__SELECTED_FEATURE_INS_EXPAND || !__SELECTED_FEATURE_CINDENT) && (!__SELECTED_FEATURE_INS_EXPAND || __SELECTED_FEATURE_CINDENT)) &&
((__SELECTED_FEATURE_AUTOCMD || !__SELECTED_FEATURE_CINDENT) && (!__SELECTED_FEATURE_AUTOCMD || __SELECTED_FEATURE_CINDENT)) &&
((__SELECTED_FEATURE_EVAL || !__SELECTED_FEATURE_AUTOCMD) && (!__SELECTED_FEATURE_EVAL || __SELECTED_FEATURE_AUTOCMD)) &&
((!__SELECTED_FEATURE_MOUSE || __SELECTED_FEATURE_CINDENT)) &&
((__SELECTED_FEATURE_VREPLACE || !__SELECTED_FEATURE_MOUSE) && (!__SELECTED_FEATURE_VREPLACE || __SELECTED_FEATURE_MOUSE)) &&
((__SELECTED_FEATURE_FKMAP || !__SELECTED_FEATURE_VREPLACE) && (!__SELECTED_FEATURE_FKMAP || __SELECTED_FEATURE_VREPLACE)) &&
((!__SELECTED_FEATURE_CMDL_INFO || __SELECTED_FEATURE_VREPLACE)) &&
((!__SELECTED_FEATURE_RIGHTLEFT || __SELECTED_FEATURE_CMDL_INFO)) &&
((F14_group_0 || !__SELECTED_FEATURE_CMDL_INFO) && (!F14_group_0 || __SELECTED_FEATURE_CMDL_INFO)) &&
((__SELECTED_FEATURE_MBYTE || !(!__SELECTED_FEATURE_FOLDING && F14_group_0)) && (!__SELECTED_FEATURE_MBYTE || (!__SELECTED_FEATURE_FOLDING && F14_group_0))) &&
((__SELECTED_FEATURE_FOLDING || !(!__SELECTED_FEATURE_MBYTE && F14_group_0)) && (!__SELECTED_FEATURE_FOLDING || (!__SELECTED_FEATURE_MBYTE && F14_group_0))) &&
((F14_group_1 || !__SELECTED_FEATURE_CMDL_INFO) && (!F14_group_1 || __SELECTED_FEATURE_CMDL_INFO)) &&
((F14_group_1 || !(__SELECTED_FEATURE_GUI || __SELECTED_FEATURE_DIGRAPHS || __SELECTED_FEATURE_SCROLLBIND)) && (!F14_group_1 || (__SELECTED_FEATURE_GUI || __SELECTED_FEATURE_DIGRAPHS || __SELECTED_FEATURE_SCROLLBIND))) &&
((!__SELECTED_FEATURE_CURSORBIND || __SELECTED_FEATURE_CMDL_INFO)) &&
((!__SELECTED_FEATURE_COMPL_FUNC || __SELECTED_FEATURE_CMDL_INFO)) &&
((!__SELECTED_FEATURE_VIRTUALEDIT || __SELECTED_FEATURE_COMPL_FUNC)) &&
((__SELECTED_FEATURE_FIND_ID || !__SELECTED_FEATURE_COMPL_FUNC) && (!__SELECTED_FEATURE_FIND_ID || __SELECTED_FEATURE_COMPL_FUNC)) &&
((__SELECTED_FEATURE_GUI_TABLINE || !__SELECTED_FEATURE_COMPL_FUNC) && (!__SELECTED_FEATURE_GUI_TABLINE || __SELECTED_FEATURE_COMPL_FUNC)) &&
((__SELECTED_FEATURE_GUI_W32 || !__SELECTED_FEATURE_CINDENT) && (!__SELECTED_FEATURE_GUI_W32 || __SELECTED_FEATURE_CINDENT)) &&
((__SELECTED_FEATURE_DND || !__SELECTED_FEATURE_CINDENT) && (!__SELECTED_FEATURE_DND || __SELECTED_FEATURE_CINDENT)) &&
((!__SELECTED_FEATURE_WINDOWS || __SELECTED_FEATURE_DND)) &&
((!__SELECTED_FEATURE_QUICKFIX || __SELECTED_FEATURE_DND)) &&
((!__SELECTED_FEATURE_CMDWIN || __SELECTED_FEATURE_CINDENT)) &&
((__SELECTED_FEATURE_CONCEAL || !__SELECTED_FEATURE_CINDENT) && (!__SELECTED_FEATURE_CONCEAL || __SELECTED_FEATURE_CINDENT)) &&
((!__SELECTED_FEATURE_AUTOCMD || __SELECTED_FEATURE_GUI))){
		 return 1;
	 }
	return 0;
}

int main_run(){
	int restart_edit = __VERIFIER_nondet_int();
	int cmdchar = __VERIFIER_nondet_int();
	int count = __VERIFIER_nondet_int();
	int startln = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_DIFF = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_RIGHTLEFT = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_FIND_ID = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_DIGRAPHS = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_WINDOWS = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_SCROLLBIND = __VERIFIER_nondet_int();
	F14_group_1 = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_QUICKFIX = __VERIFIER_nondet_int();
	F14_group_0 = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_GUI = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_INS_EXPAND = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_FKMAP = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_VREPLACE = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_AUTOCMD = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_VIRTUALEDIT = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_CMDWIN = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_FOLDING = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_GUI_TABLINE = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_DND = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_MBYTE = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_COMPL_FUNC = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_MOUSE = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_EVAL = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_CURSORBIND = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_GUI_W32 = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_CINDENT = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_CMDL_INFO = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_CONCEAL = __VERIFIER_nondet_int();

if (isValid()){
	int test_0_0 = edit(restart_edit,cmdchar,count,startln);
int test_0_0_mutation_0 = edit_mutation_0(restart_edit,cmdchar,count,startln);
if(test_0_0!=test_0_0_mutation_0){
		label_0_0: printf("label_0_0");
}
}}
