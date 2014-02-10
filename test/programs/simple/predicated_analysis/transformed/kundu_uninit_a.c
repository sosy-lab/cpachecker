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
int __return_169533;
int __return_169560;
int __return_169601;
int __return_170075;
int __return_170247;
int __return_170314;
int __return_170288;
int __return_170207;
int __return_169641;
int __return_169668;
int __return_169709;
int __return_169749;
int __return_169776;
int __return_169817;
int __return_170560;
int __return_170730;
int __return_170797;
int __return_170771;
int __return_170690;
int __return_169855;
int __return_169901;
int __return_169928;
int __return_169969;
int __return_170007;
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
if (((int)P_2_i) == 1)
{
P_2_st = 0;
if (((int)C_1_i) == 1)
{
C_1_st = 0;
}
else 
{
C_1_st = 2;
}
{
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_169533 = __retres1;
}
tmp = __return_169533;
if (tmp == 0)
{
label_169539:; 
{
int __retres1 ;
__retres1 = 0;
 __return_169560 = __retres1;
}
tmp___0 = __return_169560;
if (tmp___0 == 0)
{
label_169568:; 
{
int __retres1 ;
if (((int)C_1_pc) == 2)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
 __return_169601 = __retres1;
}
tmp___1 = __return_169601;
if (tmp___1 == 0)
{
label_169609:; 
}
else 
{
C_1_st = 0;
goto label_169609;
}
label_169615:; 
{
}
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_170075 = __retres1;
}
else 
{
if (((int)C_1_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
return 1;
}
}
tmp___2 = __return_170075;
tmp = 0;
tmp___0 = 0;
label_170117:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_170133:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_170247 = __retres1;
}
else 
{
if (((int)C_1_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
return 1;
}
}
tmp___2 = __return_170247;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
tmp___0 = 0;
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_170133;
}
else 
{
C_1_st = 1;
{
char c ;
if (i < max_loop)
{
if (num == 0)
{
timer = 1;
i = i + 1;
C_1_pc = 1;
C_1_st = 2;
}
else 
{
return 1;
}
goto label_170185;
}
else 
{
C_1_st = 2;
}
goto label_170179;
}
}
}
return 1;
}
}
else 
{
C_1_st = 1;
{
char c ;
if (i < max_loop)
{
if (num == 0)
{
timer = 1;
i = i + 1;
C_1_pc = 1;
C_1_st = 2;
}
else 
{
return 1;
}
label_170185:; 
label_170223:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_170314 = __retres1;
}
else 
{
if (((int)C_1_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
return 1;
}
}
tmp___2 = __return_170314;
if (tmp___2 == 0)
{
}
else 
{
if (((int)P_1_st) == 0)
{
tmp = 0;
if (((int)P_2_st) == 0)
{
tmp___0 = 0;
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
return 1;
}
else 
{
goto label_170223;
}
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
return 1;
}
}
else 
{
C_1_st = 2;
}
label_170179:; 
label_170221:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_170288 = __retres1;
}
else 
{
if (((int)C_1_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
return 1;
}
}
tmp___2 = __return_170288;
if (tmp___2 == 0)
{
}
else 
{
if (((int)P_1_st) == 0)
{
tmp = 0;
if (((int)P_2_st) == 0)
{
tmp___0 = 0;
goto label_170221;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
return 1;
}
}
}
}
else 
{
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_170207 = __retres1;
}
else 
{
if (((int)C_1_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
return 1;
}
}
tmp___2 = __return_170207;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
tmp___0 = 0;
goto label_170117;
}
return 1;
}
}
}
}
}
}
else 
{
P_2_st = 0;
goto label_169568;
}
}
else 
{
P_1_st = 0;
goto label_169539;
}
}
{
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_169641 = __retres1;
}
tmp = __return_169641;
if (tmp == 0)
{
label_169647:; 
{
int __retres1 ;
__retres1 = 0;
 __return_169668 = __retres1;
}
tmp___0 = __return_169668;
if (tmp___0 == 0)
{
label_169676:; 
{
int __retres1 ;
if (((int)C_1_pc) == 2)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
 __return_169709 = __retres1;
}
tmp___1 = __return_169709;
if (tmp___1 == 0)
{
label_169717:; 
}
else 
{
C_1_st = 0;
goto label_169717;
}
goto label_169615;
}
}
else 
{
P_2_st = 0;
goto label_169676;
}
}
else 
{
P_1_st = 0;
goto label_169647;
}
}
}
else 
{
P_2_st = 2;
if (((int)C_1_i) == 1)
{
C_1_st = 0;
}
else 
{
C_1_st = 2;
}
{
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_169749 = __retres1;
}
tmp = __return_169749;
if (tmp == 0)
{
label_169755:; 
{
int __retres1 ;
__retres1 = 0;
 __return_169776 = __retres1;
}
tmp___0 = __return_169776;
if (tmp___0 == 0)
{
{
int __retres1 ;
if (((int)C_1_pc) == 2)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
 __return_169817 = __retres1;
}
tmp___1 = __return_169817;
if (tmp___1 == 0)
{
label_169859:; 
}
else 
{
C_1_st = 0;
goto label_169859;
}
label_169870:; 
{
}
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_170560 = __retres1;
}
else 
{
if (((int)C_1_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
return 1;
}
}
tmp___2 = __return_170560;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
label_170588:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_170616:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_170730 = __retres1;
}
else 
{
if (((int)C_1_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
return 1;
}
}
tmp___2 = __return_170730;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_170616;
}
else 
{
C_1_st = 1;
{
char c ;
if (i < max_loop)
{
if (num == 0)
{
timer = 1;
i = i + 1;
C_1_pc = 1;
C_1_st = 2;
}
else 
{
return 1;
}
goto label_170668;
}
else 
{
C_1_st = 2;
}
goto label_170662;
}
}
}
return 1;
}
}
else 
{
C_1_st = 1;
{
char c ;
if (i < max_loop)
{
if (num == 0)
{
timer = 1;
i = i + 1;
C_1_pc = 1;
C_1_st = 2;
}
else 
{
return 1;
}
label_170668:; 
label_170706:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_170797 = __retres1;
}
else 
{
if (((int)C_1_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
return 1;
}
}
tmp___2 = __return_170797;
if (tmp___2 == 0)
{
}
else 
{
if (((int)P_1_st) == 0)
{
tmp = 0;
if (((int)P_2_st) == 0)
{
tmp___0 = 0;
return 1;
}
else 
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
return 1;
}
else 
{
goto label_170706;
}
}
}
else 
{
return 1;
}
}
return 1;
}
}
else 
{
C_1_st = 2;
}
label_170662:; 
label_170704:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_170771 = __retres1;
}
else 
{
if (((int)C_1_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
return 1;
}
}
tmp___2 = __return_170771;
if (tmp___2 == 0)
{
}
else 
{
if (((int)P_1_st) == 0)
{
tmp = 0;
if (((int)P_2_st) == 0)
{
tmp___0 = 0;
return 1;
}
else 
{
goto label_170704;
}
}
else 
{
return 1;
}
}
return 1;
}
}
}
}
else 
{
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_170690 = __retres1;
}
else 
{
if (((int)C_1_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
return 1;
}
}
tmp___2 = __return_170690;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
goto label_170588;
}
return 1;
}
}
}
return 1;
}
}
}
}
else 
{
P_2_st = 0;
{
int __retres1 ;
if (((int)C_1_pc) == 2)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
 __return_169855 = __retres1;
}
tmp___1 = __return_169855;
if (tmp___1 == 0)
{
label_169867:; 
}
else 
{
C_1_st = 0;
goto label_169867;
}
goto label_169615;
}
}
}
else 
{
P_1_st = 0;
goto label_169755;
}
}
{
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_169901 = __retres1;
}
tmp = __return_169901;
if (tmp == 0)
{
label_169907:; 
{
int __retres1 ;
__retres1 = 0;
 __return_169928 = __retres1;
}
tmp___0 = __return_169928;
if (tmp___0 == 0)
{
{
int __retres1 ;
if (((int)C_1_pc) == 2)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
 __return_169969 = __retres1;
}
tmp___1 = __return_169969;
if (tmp___1 == 0)
{
label_170011:; 
}
else 
{
C_1_st = 0;
goto label_170011;
}
goto label_169870;
}
}
else 
{
P_2_st = 0;
{
int __retres1 ;
if (((int)C_1_pc) == 2)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
 __return_170007 = __retres1;
}
tmp___1 = __return_170007;
if (tmp___1 == 0)
{
label_170019:; 
}
else 
{
C_1_st = 0;
goto label_170019;
}
goto label_169615;
}
}
}
else 
{
P_1_st = 0;
goto label_169907;
}
}
}
}
}
}
