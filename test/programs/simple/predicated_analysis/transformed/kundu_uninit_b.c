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
int __return_403890;
int __return_403917;
int __return_403958;
int __return_404454;
int __return_404817;
int __return_404889;
int __return_405921;
int __return_405423;
int __return_405450;
int __return_405476;
int __return_405965;
char __return_405868;
int __return_405895;
int __return_404863;
int __return_404942;
char __return_405690;
char __return_404777;
int __return_405020;
int __return_404915;
int __return_404992;
int __return_403998;
int __return_404025;
int __return_404066;
int __return_406090;
int __return_406254;
int __return_406324;
int __return_406298;
int __return_404106;
int __return_404133;
int __return_404174;
int __return_406519;
int __return_406655;
int __return_406715;
int __return_406689;
int __return_404212;
int __return_404258;
int __return_404285;
int __return_404326;
int __return_406932;
int __return_407008;
int __return_404364;
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
 __return_403890 = __retres1;
}
tmp = __return_403890;
if (tmp == 0)
{
label_403896:; 
{
int __retres1 ;
__retres1 = 0;
 __return_403917 = __retres1;
}
tmp___0 = __return_403917;
if (tmp___0 == 0)
{
label_403925:; 
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
 __return_403958 = __retres1;
}
tmp___1 = __return_403958;
}
label_403972:; 
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
 __return_404454 = __retres1;
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
tmp___2 = __return_404454;
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_404582:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_404817 = __retres1;
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
tmp___2 = __return_404817;
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
goto label_404582;
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
goto label_404639;
}
else 
{
C_1_st = 2;
}
goto label_404633;
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
if (((int)P_2_pc) == 0)
{
label_405197:; 
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
goto label_405184;
goto label_405177;
}
else 
{
goto label_405197;
}
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
label_404639:; 
label_404660:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_404889 = __retres1;
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
tmp___2 = __return_404889;
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
goto label_404660;
}
}
else 
{
P_2_st = 1;
{
if (((int)P_2_pc) == 0)
{
label_405346:; 
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
 __return_405423 = __retres1;
}
tmp = __return_405423;
if (tmp == 0)
{
label_405429:; 
{
int __retres1 ;
__retres1 = 0;
 __return_405450 = __retres1;
}
tmp___0 = __return_405450;
if (tmp___0 == 0)
{
label_405458:; 
{
int __retres1 ;
if (((int)e) == 1)
{
__retres1 = 1;
 __return_405476 = __retres1;
}
else 
{
return 1;
}
tmp___1 = __return_405476;
C_1_st = 0;
}
}
else 
{
P_2_st = 0;
goto label_405458;
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
}
else 
{
P_1_st = 0;
goto label_405429;
}
label_405573:; 
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
 __return_405965 = __retres1;
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
tmp___2 = __return_405965;
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
goto label_405573;
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
if (((int)C_1_pc) == 1)
{
num = num - 1;
{
int __tmp_5 = num;
int i___0 = __tmp_5;
char c ;
char __retres3 ;
__retres3 = data_0;
 __return_405868 = __retres3;
}
c = __return_405868;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
else 
{
return 1;
}
goto label_404789;
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
label_405571:; 
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
 __return_405921 = __retres1;
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
tmp___2 = __return_405921;
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
goto label_405571;
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
label_405569:; 
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
 __return_405895 = __retres1;
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
tmp___2 = __return_405895;
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
goto label_405569;
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
goto label_405346;
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
label_404633:; 
label_404658:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_404863 = __retres1;
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
tmp___2 = __return_404863;
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
goto label_404658;
}
}
else 
{
P_2_st = 1;
{
if (((int)P_2_pc) == 0)
{
label_405314:; 
P_2_st = 2;
}
else 
{
goto label_405314;
}
goto label_405289;
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
}
}
else 
{
P_2_st = 1;
{
if (((int)P_2_pc) == 0)
{
label_404506:; 
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
label_404654:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_404942 = __retres1;
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
tmp___2 = __return_404942;
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
label_405184:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_404654;
}
else 
{
C_1_st = 1;
{
char c ;
if (i < max_loop)
{
num = num - 1;
{
int __tmp_8 = num;
int i___0 = __tmp_8;
char c ;
char __retres3 ;
__retres3 = data_0;
 __return_405690 = __retres3;
}
c = __return_405690;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
else 
{
C_1_st = 2;
}
goto label_404789;
goto label_404704;
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
if (i < max_loop)
{
num = num - 1;
{
int __tmp_9 = num;
int i___0 = __tmp_9;
char c ;
char __retres3 ;
__retres3 = data_0;
 __return_404777 = __retres3;
}
c = __return_404777;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
else 
{
C_1_st = 2;
}
label_404789:; 
label_404836:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_405020 = __retres1;
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
tmp___2 = __return_405020;
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
goto label_404836;
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
goto label_404704;
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
label_404651:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_404915 = __retres1;
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
tmp___2 = __return_404915;
if (tmp___2 == 0)
{
}
else 
{
if (((int)P_1_st) == 0)
{
tmp = 0;
label_405177:; 
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_404651;
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
goto label_404704;
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
label_404704:; 
label_404833:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_404992 = __retres1;
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
tmp___2 = __return_404992;
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
label_405289:; 
goto label_404833;
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
else 
{
goto label_404506;
}
}
}
}
}
}
else 
{
P_2_st = 0;
goto label_403925;
}
}
else 
{
P_1_st = 0;
goto label_403896;
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
 __return_403998 = __retres1;
}
tmp = __return_403998;
if (tmp == 0)
{
label_404004:; 
{
int __retres1 ;
__retres1 = 0;
 __return_404025 = __retres1;
}
tmp___0 = __return_404025;
if (tmp___0 == 0)
{
label_404033:; 
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
 __return_404066 = __retres1;
}
tmp___1 = __return_404066;
}
label_404080:; 
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
 __return_406090 = __retres1;
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
tmp___2 = __return_406090;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
label_406132:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_406254 = __retres1;
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
tmp___2 = __return_406254;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_406132;
}
else 
{
P_2_st = 1;
{
if (((int)P_2_pc) == 0)
{
label_406424:; 
if (i < max_loop)
{
{
int __tmp_10 = num;
char __tmp_11 = 'B';
int i___0 = __tmp_10;
char c = __tmp_11;
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
goto label_406197;
goto label_406187;
}
else 
{
goto label_406424;
}
}
}
}
return 1;
}
}
else 
{
P_2_st = 1;
{
if (((int)P_2_pc) == 0)
{
label_406142:; 
if (i < max_loop)
{
{
int __tmp_12 = num;
char __tmp_13 = 'B';
int i___0 = __tmp_12;
char c = __tmp_13;
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
label_406197:; 
label_406210:; 
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
 __return_406324 = __retres1;
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
tmp___2 = __return_406324;
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
goto label_406210;
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
label_406187:; 
label_406208:; 
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
 __return_406298 = __retres1;
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
tmp___2 = __return_406298;
if (tmp___2 == 0)
{
}
else 
{
if (((int)P_1_st) == 0)
{
tmp = 0;
goto label_406208;
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
goto label_406142;
}
}
}
}
return 1;
}
}
}
else 
{
P_2_st = 0;
goto label_404033;
}
}
else 
{
P_1_st = 0;
goto label_404004;
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
 __return_404106 = __retres1;
}
tmp = __return_404106;
if (tmp == 0)
{
label_404112:; 
{
int __retres1 ;
__retres1 = 0;
 __return_404133 = __retres1;
}
tmp___0 = __return_404133;
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
 __return_404174 = __retres1;
}
tmp___1 = __return_404174;
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
 __return_406519 = __retres1;
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
tmp___2 = __return_406519;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_406573:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_406655 = __retres1;
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
tmp___2 = __return_406655;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_406573;
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
goto label_406624;
}
else 
{
C_1_st = 2;
}
goto label_406618;
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
label_406624:; 
label_406635:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_406715 = __retres1;
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
tmp___2 = __return_406715;
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
goto label_406635;
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
label_406618:; 
label_406633:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_406689 = __retres1;
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
tmp___2 = __return_406689;
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
goto label_406633;
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
return 1;
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
 __return_404212 = __retres1;
}
tmp___1 = __return_404212;
if (tmp___1 == 0)
{
}
else 
{
C_1_st = 0;
return 1;
}
goto label_403972;
}
}
}
else 
{
P_1_st = 0;
goto label_404112;
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
 __return_404258 = __retres1;
}
tmp = __return_404258;
if (tmp == 0)
{
label_404264:; 
{
int __retres1 ;
__retres1 = 0;
 __return_404285 = __retres1;
}
tmp___0 = __return_404285;
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
 __return_404326 = __retres1;
}
tmp___1 = __return_404326;
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
 __return_406932 = __retres1;
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
tmp___2 = __return_406932;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
label_406960:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_407008 = __retres1;
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
tmp___2 = __return_407008;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
goto label_406960;
}
return 1;
}
}
return 1;
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
 __return_404364 = __retres1;
}
tmp___1 = __return_404364;
if (tmp___1 == 0)
{
}
else 
{
C_1_st = 0;
return 1;
}
goto label_404080;
}
}
}
else 
{
P_1_st = 0;
goto label_404264;
}
}
}
}
}
}
