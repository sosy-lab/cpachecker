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
int __return_25820=0;
int __return_25842=0;
int __return_25868=0;
int __return_25926=0;
int __return_27852=0;
int __return_27874=0;
int __return_27900=0;
int __return_28316=0;
int __return_31544=0;
int __return_31566=0;
int __return_31592=0;
int __return_32304=0;
int __return_32322=0;
int __return_67696=0;
int __return_67362=0;
int __return_67450=0;
int __return_67472=0;
int __return_67494=0;
int __return_67670=0;
char __return_67584=0;
int __return_67626=0;
int __return_36944=0;
char __return_26064=0;
int __return_46960=0;
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
 __return_25820 = __retres1;
}
tmp = __return_25820;
{
int __retres1 ;
__retres1 = 0;
 __return_25842 = __retres1;
}
tmp___0 = __return_25842;
{
int __retres1 ;
__retres1 = 0;
 __return_25868 = __retres1;
}
tmp___1 = __return_25868;
}
{
}
label_25888:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
label_25908:; 
{
int __retres1 ;
__retres1 = 1;
 __return_25926 = __retres1;
}
tmp___2 = __return_25926;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_25908;
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
label_26126:; 
{
int __retres1 ;
__retres1 = 1;
 __return_67362 = __retres1;
}
tmp___2 = __return_67362;
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_26126;
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
 __return_67450 = __retres1;
}
tmp = __return_67450;
{
int __retres1 ;
__retres1 = 0;
 __return_67472 = __retres1;
}
tmp___0 = __return_67472;
{
int __retres1 ;
__retres1 = 1;
 __return_67494 = __retres1;
}
tmp___1 = __return_67494;
C_1_st = 0;
}
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
}
label_67532:; 
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_67670 = __retres1;
}
tmp___2 = __return_67670;
tmp = 0;
goto label_67532;
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
__retres3 = data_0;
 __return_67584 = __retres3;
}
c = __return_67584;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
label_67606:; 
{
int __retres1 ;
__retres1 = 1;
 __return_67626 = __retres1;
}
tmp___2 = __return_67626;
tmp = 0;
goto label_67606;
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
label_26002:; 
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_36944 = __retres1;
}
tmp___2 = __return_36944;
tmp = 0;
goto label_26002;
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
__retres3 = data_0;
 __return_26064 = __retres3;
}
c = __return_26064;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
label_26124:; 
{
int __retres1 ;
__retres1 = 1;
 __return_46960 = __retres1;
}
tmp___2 = __return_46960;
tmp = 0;
goto label_26124;
}
}
}
kernel_st = 2;
{
}
kernel_st = 3;
{
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_27852 = __retres1;
}
tmp = __return_27852;
{
int __retres1 ;
__retres1 = 0;
 __return_27874 = __retres1;
}
tmp___0 = __return_27874;
{
int __retres1 ;
__retres1 = 0;
 __return_27900 = __retres1;
}
tmp___1 = __return_27900;
}
{
}
{
int __retres1 ;
__retres1 = 1;
 __return_28316 = __retres1;
}
tmp = __return_28316;
if (tmp == 0)
{
kernel_st = 4;
{
C_1_ev = 1;
if (clk == 1)
{
P_1_ev = 1;
P_2_ev = 1;
clk = 0;
goto label_28468;
}
else 
{
clk = clk + 1;
label_28468:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_31544 = __retres1;
}
tmp = __return_31544;
{
int __retres1 ;
__retres1 = 0;
 __return_31566 = __retres1;
}
tmp___0 = __return_31566;
{
int __retres1 ;
__retres1 = 0;
 __return_31592 = __retres1;
}
tmp___1 = __return_31592;
}
{
if (((int)P_1_ev) == 1)
{
P_1_ev = 2;
goto label_31612;
}
else 
{
label_31612:; 
if (((int)P_2_ev) == 1)
{
P_2_ev = 2;
goto label_31622;
}
else 
{
label_31622:; 
if (((int)C_1_ev) == 1)
{
C_1_ev = 2;
goto label_31632;
}
else 
{
label_31632:; 
}
goto label_28420;
}
}
}
}
}
else 
{
label_28420:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 1;
 __return_32304 = __retres1;
}
tmp = __return_32304;
if (tmp == 0)
{
__retres2 = 1;
goto label_32314;
}
else 
{
__retres2 = 0;
label_32314:; 
 __return_32322 = __retres2;
}
tmp___0 = __return_32322;
if (tmp___0 == 0)
{
goto label_25888;
}
else 
{
}
__retres2 = 0;
 __return_67696 = __retres2;
return 1;
}
}
}
}
}
