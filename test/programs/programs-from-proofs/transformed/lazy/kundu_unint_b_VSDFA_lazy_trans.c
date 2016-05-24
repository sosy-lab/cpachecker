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
int __return_2349;
int __return_2376;
int __return_2394;
int __return_2426;
int __return_2811;
int __return_2879;
int __return_2906;
int __return_2952;
int __return_3213;
char __return_3166;
int __return_3319;
int __return_2932;
int __return_3236;
int __return_3344;
char __return_2530;
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
if (!(((int)P_1_pc) == 1))
{
label_2336:; 
__retres1 = 0;
label_2348:; 
 __return_2349 = __retres1;
}
else 
{
if (!(((int)P_1_ev) == 1))
{
goto label_2336;
}
else 
{
__retres1 = 1;
goto label_2348;
}
}
tmp = __return_2349;
if (!(tmp == 0))
{
P_1_st = 0;
label_2355:; 
{
int __retres1 ;
if (!(((int)P_2_pc) == 1))
{
label_2363:; 
__retres1 = 0;
label_2375:; 
 __return_2376 = __retres1;
}
else 
{
if (!(((int)P_2_ev) == 1))
{
goto label_2363;
}
else 
{
__retres1 = 1;
goto label_2375;
}
}
tmp___0 = __return_2376;
if (!(tmp___0 == 0))
{
P_2_st = 0;
label_2382:; 
{
int __retres1 ;
__retres1 = 0;
 __return_2394 = __retres1;
}
tmp___1 = __return_2394;
}
else 
{
goto label_2382;
}
{
}
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
label_2416:; 
{
int __retres1 ;
__retres1 = 1;
 __return_2426 = __retres1;
}
tmp___2 = __return_2426;
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (!(((int)C_1_st) == 0))
{
label_2478:; 
goto label_2416;
}
else 
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_2478;
}
else 
{
C_1_st = 1;
{
char c ;
timer = 1;
i = i + 1;
C_1_pc = 1;
C_1_st = 2;
}
label_2800:; 
{
int __retres1 ;
__retres1 = 1;
 __return_2811 = __retres1;
}
tmp___2 = __return_2811;
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_2800;
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
timer = 0;
e = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
if (!(((int)P_1_pc) == 1))
{
label_2866:; 
__retres1 = 0;
label_2878:; 
 __return_2879 = __retres1;
}
else 
{
if (!(((int)P_1_ev) == 1))
{
goto label_2866;
}
else 
{
__retres1 = 1;
goto label_2878;
}
}
tmp = __return_2879;
if (!(tmp == 0))
{
P_1_st = 0;
label_2885:; 
{
int __retres1 ;
if (!(((int)P_2_pc) == 1))
{
label_2893:; 
__retres1 = 0;
label_2905:; 
 __return_2906 = __retres1;
}
else 
{
if (!(((int)P_2_ev) == 1))
{
goto label_2893;
}
else 
{
__retres1 = 1;
goto label_2905;
}
}
tmp___0 = __return_2906;
if (!(tmp___0 == 0))
{
P_2_st = 0;
{
int __retres1 ;
if (!(((int)e) == 1))
{
__retres1 = 0;
label_2951:; 
 __return_2952 = __retres1;
}
else 
{
__retres1 = 1;
goto label_2951;
}
tmp___1 = __return_2952;
if (!(tmp___1 == 0))
{
C_1_st = 0;
}
else 
{
}
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
label_3002:; 
e = 2;
P_2_pc = 1;
P_2_st = 2;
goto label_3004;
}
else 
{
{
int __retres1 ;
if (!(((int)e) == 1))
{
__retres1 = 0;
label_2931:; 
 __return_2932 = __retres1;
}
else 
{
__retres1 = 1;
goto label_2931;
}
tmp___1 = __return_2932;
if (!(tmp___1 == 0))
{
C_1_st = 0;
}
else 
{
}
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
goto label_3002;
e = 2;
P_2_pc = 1;
P_2_st = 2;
label_3004:; 
goto label_3007;
}
label_3124:; 
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_3213 = __retres1;
}
tmp___2 = __return_3213;
tmp = 0;
goto label_3124;
}
else 
{
C_1_st = 1;
{
char c ;
num = num - 1;
{
int __tmp_3 = num;
int i___0 = __tmp_3;
char c ;
char __retres3 ;
if (!(i___0 == 0))
{
if (!(i___0 == 1))
{
{
}
__retres3 = c;
label_3161:; 
 __return_3166 = __retres3;
}
else 
{
__retres3 = data_1;
goto label_3161;
}
c = __return_3166;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
else 
{
__retres3 = data_0;
goto label_3161;
}
label_3306:; 
label_3308:; 
{
int __retres1 ;
__retres1 = 1;
 __return_3319 = __retres1;
}
tmp___2 = __return_3319;
tmp = 0;
goto label_3308;
}
}
}
}
}
else 
{
goto label_2885;
}
}
}
}
}
else 
{
P_2_st = 2;
label_3007:; 
}
label_3017:; 
{
int __retres1 ;
__retres1 = 1;
 __return_3236 = __retres1;
}
tmp___2 = __return_3236;
tmp = 0;
goto label_3017;
}
}
}
}
}
else 
{
P_2_st = 1;
{
{
int __tmp_4 = num;
char __tmp_5 = 'B';
int i___0 = __tmp_4;
char c = __tmp_5;
data_0 = c;
}
num = num + 1;
P_2_pc = 1;
P_2_st = 2;
}
label_2471:; 
if (!(((int)C_1_st) == 0))
{
label_2479:; 
{
int __retres1 ;
__retres1 = 1;
 __return_3344 = __retres1;
}
tmp___2 = __return_3344;
tmp = 0;
goto label_2471;
}
else 
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_2479;
}
else 
{
C_1_st = 1;
{
char c ;
num = num - 1;
{
int __tmp_6 = num;
int i___0 = __tmp_6;
char c ;
char __retres3 ;
if (!(i___0 == 0))
{
if (!(i___0 == 1))
{
{
}
__retres3 = c;
label_2525:; 
 __return_2530 = __retres3;
}
else 
{
__retres3 = data_1;
goto label_2525;
}
c = __return_2530;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
else 
{
__retres3 = data_0;
goto label_2525;
}
goto label_3306;
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
goto label_2355;
}
}
}
}
}
