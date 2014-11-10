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
int __return_162042;
int __return_162071;
int __return_162112;
int __return_172084;
int __return_173385;
int __return_172924;
int __return_175133;
int __return_175162;
int __return_175188;
int __return_176399;
char __return_176203;
int __return_176328;
int __return_176269;
int __return_173735;
int __return_172785;
int __return_173676;
int __return_173617;
char __return_176123;
char __return_172733;
int __return_173546;
int __return_173279;
int __return_172397;
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
 __return_162042 = __retres1;
}
tmp = __return_162042;
{
int __retres1 ;
__retres1 = 0;
 __return_162071 = __retres1;
}
tmp___0 = __return_162071;
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
 __return_162112 = __retres1;
}
tmp___1 = __return_162112;
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
 __return_172084 = __retres1;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 1;
return 1;
}
}
tmp___2 = __return_172084;
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_172355:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_173385 = __retres1;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 1;
return 1;
}
}
tmp___2 = __return_173385;
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
if (tmp___1 == 0)
{
goto label_172355;
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
goto label_172341;
}
else 
{
C_1_st = 2;
}
goto label_172339;
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
goto label_176048;
goto label_173302;
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
label_172341:; 
label_172363:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_172924 = __retres1;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 1;
return 1;
}
}
tmp___2 = __return_172924;
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
goto label_172363;
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
 __return_175133 = __retres1;
}
tmp = __return_175133;
{
int __retres1 ;
__retres1 = 0;
 __return_175162 = __retres1;
}
tmp___0 = __return_175162;
{
int __retres1 ;
if (((int)e) == 1)
{
__retres1 = 1;
 __return_175188 = __retres1;
}
else 
{
return 1;
}
tmp___1 = __return_175188;
C_1_st = 0;
}
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
}
label_176054:; 
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
 __return_176399 = __retres1;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 1;
return 1;
}
}
tmp___2 = __return_176399;
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
goto label_176054;
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
goto label_176153;
}
}
else 
{
C_1_st = 2;
}
goto label_172232;
}
else 
{
if (((int)C_1_pc) == 1)
{
label_176153:; 
num = num - 1;
{
int __tmp_5 = num;
int i___0 = __tmp_5;
char c ;
char __retres3 ;
__retres3 = data_0;
 __return_176203 = __retres3;
}
c = __return_176203;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
else 
{
if (i < max_loop)
{
goto label_176153;
}
else 
{
C_1_st = 2;
}
label_176239:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_176328 = __retres1;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 1;
return 1;
}
}
tmp___2 = __return_176328;
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
goto label_176239;
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
goto label_172749;
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
 __return_176269 = __retres1;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 1;
return 1;
}
}
tmp___2 = __return_176269;
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
goto label_176054;
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
label_173107:; 
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
 __return_173735 = __retres1;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 1;
return 1;
}
}
tmp___2 = __return_173735;
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
goto label_173107;
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
label_172339:; 
label_172361:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_172785 = __retres1;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 1;
return 1;
}
}
tmp___2 = __return_172785;
if (tmp___2 == 0)
{
}
else 
{
if (((int)P_1_st) == 0)
{
tmp = 0;
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
goto label_172361;
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
label_172870:; 
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
 __return_173676 = __retres1;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 1;
return 1;
}
}
tmp___2 = __return_173676;
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
goto label_172870;
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
goto label_172420;
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
}
else 
{
P_2_st = 1;
{
if (i < max_loop)
{
{
int __tmp_8 = num;
char __tmp_9 = 'B';
int i___0 = __tmp_8;
char c = __tmp_9;
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
label_172753:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_173617 = __retres1;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 1;
return 1;
}
}
tmp___2 = __return_173617;
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
label_176048:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_172753;
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
int __tmp_10 = num;
int i___0 = __tmp_10;
char c ;
char __retres3 ;
__retres3 = data_0;
 __return_176123 = __retres3;
}
c = __return_176123;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
goto label_172749;
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
int __tmp_11 = num;
int i___0 = __tmp_11;
char c ;
char __retres3 ;
__retres3 = data_0;
 __return_172733 = __retres3;
}
c = __return_172733;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
label_172749:; 
label_172755:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_173546 = __retres1;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 1;
return 1;
}
}
tmp___2 = __return_173546;
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
goto label_172755;
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
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_172351:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_173279 = __retres1;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 1;
return 1;
}
}
tmp___2 = __return_173279;
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
label_173302:; 
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_172351;
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
goto label_172232;
}
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
label_172232:; 
label_172357:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_172397 = __retres1;
}
else 
{
if (((int)P_2_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 1;
return 1;
}
}
tmp___2 = __return_172397;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
label_172420:; 
goto label_172357;
}
return 1;
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
