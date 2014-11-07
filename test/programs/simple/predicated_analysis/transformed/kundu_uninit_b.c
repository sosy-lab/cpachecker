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
int __return_588631;
int __return_588658;
int __return_588699;
int __return_588750;
int __return_589072;
int __return_589144;
int __return_590135;
int __return_589636;
int __return_589663;
int __return_589689;
int __return_590185;
char __return_590075;
int __return_590233;
int __return_590109;
int __return_589118;
int __return_589197;
char __return_589884;
char __return_589032;
int __return_589275;
int __return_589170;
int __return_589247;
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
__retres1 = 0;
 __return_588631 = __retres1;
}
tmp = __return_588631;
if (tmp == 0)
{
label_588637:; 
{
int __retres1 ;
__retres1 = 0;
 __return_588658 = __retres1;
}
tmp___0 = __return_588658;
if (tmp___0 == 0)
{
label_588666:; 
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
 __return_588699 = __retres1;
}
tmp___1 = __return_588699;
}
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
 __return_588750 = __retres1;
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
tmp___2 = __return_588750;
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_588864:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_589072 = __retres1;
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
tmp___2 = __return_589072;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_588864;
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
goto label_588913;
}
else 
{
C_1_st = 2;
}
goto label_588907;
}
}
}
else 
{
return 1;
}
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
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
else 
{
P_2_st = 2;
}
goto label_589439;
goto label_589432;
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
label_588913:; 
label_588934:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_589144 = __retres1;
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
tmp___2 = __return_589144;
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
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
return 1;
}
else 
{
goto label_588934;
}
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_3 = num;
char __tmp_4 = 'B';
int i___0 = __tmp_3;
char c = __tmp_4;
data_0 = c;
}
num = num + 1;
if (timer == 0)
{
P_2_pc = 1;
P_2_st = 2;
}
else 
{
timer = 0;
e = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_589636 = __retres1;
}
tmp = __return_589636;
if (tmp == 0)
{
label_589642:; 
{
int __retres1 ;
__retres1 = 0;
 __return_589663 = __retres1;
}
tmp___0 = __return_589663;
if (tmp___0 == 0)
{
label_589671:; 
{
int __retres1 ;
if (((int)e) == 1)
{
__retres1 = 1;
 __return_589689 = __retres1;
}
else 
{
return 1;
}
tmp___1 = __return_589689;
C_1_st = 0;
}
}
else 
{
P_2_st = 0;
goto label_589671;
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
}
else 
{
P_1_st = 0;
goto label_589642;
}
label_589786:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_590185 = __retres1;
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
tmp___2 = __return_590185;
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
tmp___0 = __VERIFIER_nondet_int();
return 1;
}
else 
{
goto label_589786;
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
C_1_st = 1;
{
char c ;
if (((int)C_1_pc) == 0)
{
if (num == 0)
{
timer = 1;
i = i + 1;
C_1_pc = 1;
C_1_st = 2;
return 1;
}
else 
{
goto label_590031;
}
}
else 
{
if (((int)C_1_pc) == 1)
{
label_590031:; 
num = num - 1;
{
int __tmp_5 = num;
int i___0 = __tmp_5;
char c ;
char __retres3 ;
__retres3 = data_0;
 __return_590075 = __retres3;
}
c = __return_590075;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
else 
{
if (i < max_loop)
{
if (num == 0)
{
timer = 1;
i = i + 1;
C_1_pc = 1;
C_1_st = 2;
return 1;
}
else 
{
goto label_590031;
}
}
else 
{
C_1_st = 2;
return 1;
}
}
label_590157:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_590233 = __retres1;
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
tmp___2 = __return_590233;
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
tmp___0 = __VERIFIER_nondet_int();
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
goto label_590157;
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
}
}
}
else 
{
return 1;
}
}
}
}
label_589784:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
return 1;
}
else 
{
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_590135 = __retres1;
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
tmp___2 = __return_590135;
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
tmp___0 = __VERIFIER_nondet_int();
return 1;
}
else 
{
goto label_589784;
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
else 
{
P_2_st = 2;
}
label_589782:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
return 1;
}
else 
{
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_590109 = __retres1;
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
tmp___2 = __return_590109;
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
tmp___0 = __VERIFIER_nondet_int();
return 1;
}
else 
{
goto label_589782;
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
label_588907:; 
label_588932:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_589118 = __retres1;
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
tmp___2 = __return_589118;
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
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
return 1;
}
else 
{
goto label_588932;
}
}
else 
{
P_2_st = 1;
{
P_2_st = 2;
}
goto label_589530;
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
}
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_6 = num;
char __tmp_7 = 'B';
int i___0 = __tmp_6;
char c = __tmp_7;
data_0 = c;
}
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
else 
{
P_2_st = 2;
}
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_588928:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_589197 = __retres1;
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
tmp___2 = __return_589197;
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
tmp___0 = __VERIFIER_nondet_int();
return 1;
}
else 
{
label_589439:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_588928;
}
else 
{
C_1_st = 1;
{
char c ;
if (num == 0)
{
timer = 1;
i = i + 1;
C_1_pc = 1;
C_1_st = 2;
return 1;
}
else 
{
num = num - 1;
{
int __tmp_8 = num;
int i___0 = __tmp_8;
char c ;
char __retres3 ;
__retres3 = data_0;
 __return_589884 = __retres3;
}
c = __return_589884;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
goto label_589044;
}
}
}
else 
{
return 1;
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
C_1_st = 1;
{
char c ;
if (num == 0)
{
timer = 1;
i = i + 1;
C_1_pc = 1;
C_1_st = 2;
return 1;
}
else 
{
num = num - 1;
{
int __tmp_9 = num;
int i___0 = __tmp_9;
char c ;
char __retres3 ;
__retres3 = data_0;
 __return_589032 = __retres3;
}
c = __return_589032;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
label_589044:; 
label_589091:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_589275 = __retres1;
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
tmp___2 = __return_589275;
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
tmp___0 = __VERIFIER_nondet_int();
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
goto label_589091;
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
}
}
else 
{
return 1;
}
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_588925:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_589170 = __retres1;
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
tmp___2 = __return_589170;
if (tmp___2 == 0)
{
}
else 
{
if (((int)P_1_st) == 0)
{
tmp = 0;
label_589432:; 
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_588925;
}
else 
{
C_1_st = 1;
{
char c ;
if (i < max_loop)
{
return 1;
}
else 
{
C_1_st = 2;
}
goto label_588970;
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
C_1_st = 1;
{
char c ;
if (i < max_loop)
{
return 1;
}
else 
{
C_1_st = 2;
}
label_588970:; 
label_589088:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_589247 = __retres1;
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
tmp___2 = __return_589247;
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
tmp___0 = __VERIFIER_nondet_int();
return 1;
}
else 
{
label_589530:; 
goto label_589088;
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
goto label_588666;
}
}
else 
{
P_1_st = 0;
goto label_588637;
}
}
}
}
