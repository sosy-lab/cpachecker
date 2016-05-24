extern int __VERIFIER_nondet_int();
void error(void);
void immediate_notify(void) ;
int max_loop ;
int clk ;
int num ;
int i  ;
int e  ;
int timer ;
char data_0  ;
char data_1  ;
char read_data(int i___0 );
void write_data(int i___0 , char c );
int P_1_pc;
int P_1_st  ;
int P_1_i  ;
int P_1_ev  ;
void P_1(void);
int is_P_1_triggered(void);
int P_2_pc  ;
int P_2_st  ;
int P_2_i  ;
int P_2_ev  ;
void P_2(void);
int is_P_2_triggered(void);
int C_1_pc  ;
int C_1_st  ;
int C_1_i  ;
int C_1_ev  ;
int C_1_pr  ;
void C_1(void);
int is_C_1_triggered(void);
void update_channels(void);
void init_threads(void);
int exists_runnable_thread(void);
void eval(void);
void fire_delta_events(void);
void reset_delta_events(void);
void fire_time_events(void);
void reset_time_events(void);
void activate_threads(void);
int stop_simulation(void);
void start_simulation(void);
void init_model(void);
int main(void);
int __return_47502;
int __return_47524;
int __return_47595;
int __return_47525;
int __return_47563;
int __return_47652;
char __return_47764;
char __return_47765;
char __return_47766;
int __return_48936;
int __return_49013;
char __return_49100;
char __return_49101;
char __return_49102;
int __return_47841;
int __return_47911;
int __return_47960;
int __return_48102;
int __return_48103;
int __return_47961;
int __return_48003;
int __return_48004;
int __return_47912;
int __return_47938;
int __return_48069;
int __return_48070;
int __return_48494;
char __return_48585;
char __return_48586;
char __return_48587;
char __return_48357;
char __return_48358;
char __return_48359;
int __return_47939;
int __return_48036;
int __return_48638;
char __return_48729;
char __return_48730;
char __return_48731;
char __return_48287;
char __return_48288;
char __return_48289;
int __return_48415;
int __return_48782;
int __return_48037;
int __return_48859;
int main()
{
int count ;
int __retres2 ;
num = 0;
i = 0;
clk = 0;
max_loop = 8;
e;
timer = 0;
P_1_pc = 0;
P_2_pc = 0;
C_1_pc = 0;
count = 0;
{
P_1_i = 1;
P_2_i = 1;
C_1_i = 1;
}
{
int kernel_st ;
int tmp ;
int tmp___0 ;
kernel_st = 0;
{
}
{
P_1_st = 0;
P_2_st = 0;
C_1_st = 0;
}
{
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
if (((int)P_1_pc) == 1)
{
goto label_47496;
}
else 
{
label_47496:; 
__retres1 = 0;
 __return_47502 = __retres1;
}
tmp = __return_47502;
{
int __retres1 ;
if (((int)P_2_pc) == 1)
{
if (((int)P_2_ev) == 1)
{
__retres1 = 1;
 __return_47524 = __retres1;
}
else 
{
goto label_47519;
}
tmp___0 = __return_47524;
P_2_st = 0;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
return 1;
}
else 
{
goto label_47578;
}
}
else 
{
label_47578:; 
if (((int)C_1_pc) == 2)
{
goto label_47586;
}
else 
{
label_47586:; 
__retres1 = 0;
 __return_47595 = __retres1;
}
tmp___1 = __return_47595;
}
goto label_47606;
}
}
else 
{
label_47519:; 
__retres1 = 0;
 __return_47525 = __retres1;
}
tmp___0 = __return_47525;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
return 1;
}
else 
{
goto label_47546;
}
}
else 
{
label_47546:; 
if (((int)C_1_pc) == 2)
{
goto label_47554;
}
else 
{
label_47554:; 
__retres1 = 0;
 __return_47563 = __retres1;
}
tmp___1 = __return_47563;
}
label_47606:; 
{
}
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
label_47628:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_47651;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_47651;
}
else 
{
__retres1 = 1;
label_47651:; 
 __return_47652 = __retres1;
}
tmp___2 = __return_47652;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_47666;
}
else 
{
label_47666:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_47701;
}
else 
{
P_2_st = 1;
{
{
int __tmp_1 = num;
char __tmp_2 = 'B';
int i___0 = __tmp_1;
char c = __tmp_2;
data_0 = c;
}
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_47811;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_47731;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_47736:; 
num = num - 1;
{
int __tmp_3 = num;
int i___0 = __tmp_3;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_47764 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_47765 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_47766 = __retres3;
}
c = __return_47765;
goto label_47768;
c = __return_47766;
label_47768:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_47764;
goto label_47768;
label_47779:; 
label_48912:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_48935;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_48935;
}
else 
{
__retres1 = 1;
label_48935:; 
 __return_48936 = __retres1;
}
tmp___2 = __return_48936;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_48950;
}
else 
{
label_48950:; 
goto label_48912;
}
}
}
}
}
else 
{
label_47731:; 
goto label_47736;
}
}
}
}
}
else 
{
label_47811:; 
label_48989:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_49012;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_49012;
}
else 
{
__retres1 = 1;
label_49012:; 
 __return_49013 = __retres1;
}
tmp___2 = __return_49013;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_49027;
}
else 
{
label_49027:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
tmp___1 = __VERIFIER_nondet_int();
return 1;
}
else 
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_49121;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_49067;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_49072:; 
num = num - 1;
{
int __tmp_4 = num;
int i___0 = __tmp_4;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_49100 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_49101 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_49102 = __retres3;
}
c = __return_49101;
goto label_49104;
c = __return_49102;
label_49104:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_49100;
goto label_49104;
goto label_47779;
}
}
else 
{
label_49067:; 
goto label_49072;
}
}
}
}
}
else 
{
label_49121:; 
goto label_48989;
}
}
}
}
}
}
}
}
else 
{
label_47701:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_47813;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_47794;
}
else 
{
label_47794:; 
timer = 1;
i = i + 1;
C_1_pc = 1;
C_1_st = 2;
}
label_47817:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_47840;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_47840;
}
else 
{
__retres1 = 1;
label_47840:; 
 __return_47841 = __retres1;
}
tmp___2 = __return_47841;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_47855;
}
else 
{
label_47855:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_48204;
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_5 = num;
char __tmp_6 = 'B';
int i___0 = __tmp_5;
char c = __tmp_6;
data_0 = c;
}
num = num + 1;
timer = 0;
e = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
if (((int)P_1_pc) == 1)
{
if (((int)P_1_ev) == 1)
{
__retres1 = 1;
 __return_47911 = __retres1;
}
else 
{
goto label_47906;
}
tmp = __return_47911;
P_1_st = 0;
{
int __retres1 ;
if (((int)P_2_pc) == 1)
{
if (((int)P_2_ev) == 1)
{
__retres1 = 1;
 __return_47960 = __retres1;
}
else 
{
goto label_47955;
}
tmp___0 = __return_47960;
P_2_st = 0;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_48101;
}
else 
{
goto label_48086;
}
}
else 
{
label_48086:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_48101:; 
 __return_48102 = __retres1;
}
else 
{
goto label_48094;
}
tmp___1 = __return_48102;
C_1_st = 0;
}
else 
{
label_48094:; 
__retres1 = 0;
 __return_48103 = __retres1;
}
label_48137:; 
tmp___1 = __return_48103;
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
goto label_48149;
}
goto label_48196;
}
else 
{
label_47955:; 
__retres1 = 0;
 __return_47961 = __retres1;
}
tmp___0 = __return_47961;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_48002;
}
else 
{
goto label_47987;
}
}
else 
{
label_47987:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_48002:; 
 __return_48003 = __retres1;
}
else 
{
goto label_47995;
}
tmp___1 = __return_48003;
C_1_st = 0;
}
else 
{
label_47995:; 
__retres1 = 0;
 __return_48004 = __retres1;
}
goto label_48141;
tmp___1 = __return_48004;
}
label_48145:; 
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
}
goto label_48192;
}
else 
{
label_47906:; 
__retres1 = 0;
 __return_47912 = __retres1;
}
tmp = __return_47912;
{
int __retres1 ;
if (((int)P_2_pc) == 1)
{
if (((int)P_2_ev) == 1)
{
__retres1 = 1;
 __return_47938 = __retres1;
}
else 
{
goto label_47933;
}
tmp___0 = __return_47938;
P_2_st = 0;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_48068;
}
else 
{
goto label_48053;
}
}
else 
{
label_48053:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_48068:; 
 __return_48069 = __retres1;
}
else 
{
goto label_48061;
}
tmp___1 = __return_48069;
C_1_st = 0;
}
else 
{
label_48061:; 
__retres1 = 0;
 __return_48070 = __retres1;
}
goto label_48137;
tmp___1 = __return_48070;
}
label_48149:; 
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
}
else 
{
label_47933:; 
__retres1 = 0;
 __return_47939 = __retres1;
}
label_48192:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_48470:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_48493;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_48493;
}
else 
{
__retres1 = 1;
label_48493:; 
 __return_48494 = __retres1;
}
tmp___2 = __return_48494;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_48508;
}
else 
{
label_48508:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
tmp___1 = __VERIFIER_nondet_int();
return 1;
}
else 
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_48609;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_48548;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_48557:; 
num = num - 1;
{
int __tmp_7 = num;
int i___0 = __tmp_7;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_48585 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_48586 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_48587 = __retres3;
}
c = __return_48586;
goto label_48589;
c = __return_48587;
label_48589:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_48585;
goto label_48589;
goto label_48305;
}
}
else 
{
label_48548:; 
if (i < max_loop)
{
goto label_48557;
}
else 
{
C_1_st = 2;
}
goto label_48303;
}
}
}
}
}
else 
{
label_48609:; 
goto label_48470;
}
}
}
}
}
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_48320;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_48329:; 
num = num - 1;
{
int __tmp_8 = num;
int i___0 = __tmp_8;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_48357 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_48358 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_48359 = __retres3;
}
c = __return_48358;
goto label_48361;
c = __return_48359;
label_48361:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_48357;
goto label_48361;
goto label_48305;
}
}
else 
{
label_48320:; 
if (i < max_loop)
{
goto label_48329;
}
else 
{
C_1_st = 2;
}
goto label_48303;
}
}
}
}
}
else 
{
goto label_48391;
}
tmp___0 = __return_47939;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_48035;
}
else 
{
goto label_48020;
}
}
else 
{
label_48020:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
label_48035:; 
 __return_48036 = __retres1;
}
else 
{
goto label_48028;
}
tmp___1 = __return_48036;
C_1_st = 0;
}
else 
{
label_48028:; 
__retres1 = 0;
 __return_48037 = __retres1;
}
label_48141:; 
tmp___1 = __return_48037;
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
goto label_48145;
}
label_48196:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_48614:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_48637;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_48637;
}
else 
{
__retres1 = 1;
label_48637:; 
 __return_48638 = __retres1;
}
tmp___2 = __return_48638;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_48652;
}
else 
{
label_48652:; 
if (((int)P_2_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
tmp___1 = __VERIFIER_nondet_int();
return 1;
}
else 
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_48753;
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_48692;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_48701:; 
num = num - 1;
{
int __tmp_9 = num;
int i___0 = __tmp_9;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_48729 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_48730 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_48731 = __retres3;
}
c = __return_48730;
goto label_48733;
c = __return_48731;
label_48733:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_48729;
goto label_48733;
goto label_48305;
}
}
else 
{
label_48692:; 
if (i < max_loop)
{
goto label_48701;
}
else 
{
C_1_st = 2;
}
goto label_48303;
}
}
}
}
}
else 
{
label_48753:; 
goto label_48614;
}
}
}
}
}
}
else 
{
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
goto label_48250;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_48259:; 
num = num - 1;
{
int __tmp_10 = num;
int i___0 = __tmp_10;
char c ;
char __retres3 ;
if (i___0 == 0)
{
__retres3 = data_0;
 __return_48287 = __retres3;
}
else 
{
if (i___0 == 1)
{
__retres3 = data_1;
 __return_48288 = __retres3;
}
else 
{
{
}
__retres3 = c;
 __return_48289 = __retres3;
}
c = __return_48288;
goto label_48291;
c = __return_48289;
label_48291:; 
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
c = __return_48287;
goto label_48291;
label_48305:; 
label_48391:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_48414;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_48414;
}
else 
{
__retres1 = 1;
label_48414:; 
 __return_48415 = __retres1;
}
tmp___2 = __return_48415;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_48429;
}
else 
{
label_48429:; 
goto label_48391;
}
}
}
}
}
else 
{
label_48250:; 
if (i < max_loop)
{
goto label_48259;
}
else 
{
C_1_st = 2;
}
label_48303:; 
goto label_48391;
}
}
}
}
}
else 
{
label_48758:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_48781;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_48781;
}
else 
{
__retres1 = 1;
label_48781:; 
 __return_48782 = __retres1;
}
tmp___2 = __return_48782;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_48796;
}
else 
{
label_48796:; 
goto label_48758;
}
}
}
}
}
}
}
}
}
else 
{
P_2_st = 2;
}
label_48835:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
goto label_48858;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
goto label_48858;
}
else 
{
__retres1 = 1;
label_48858:; 
 __return_48859 = __retres1;
}
tmp___2 = __return_48859;
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_48873;
}
else 
{
label_48873:; 
goto label_48835;
}
}
}
}
}
}
else 
{
label_48204:; 
goto label_47817;
}
}
}
}
}
}
}
else 
{
label_47813:; 
goto label_47628;
}
}
}
}
}
}
}
}
}
}
}
}
