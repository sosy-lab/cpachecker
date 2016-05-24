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
int __return_8471;
int __return_8484;
int __return_8499;
int __return_8969;
int __return_9020;
int __return_8423;
int __return_8436;
int __return_8451;
int __return_8540;
int __return_8662;
int __return_8715;
int __return_8728;
int __return_8751;
int __return_8891;
int __return_8750;
int __return_8866;
char __return_8819;
int __return_8841;
int __return_8940;
char __return_8619;
int __return_8914;
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
if (!(((int)P_2_i) == 1))
{
P_2_st = 2;
C_1_st = 0;
}
else 
{
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
 __return_8471 = __retres1;
}
tmp = __return_8471;
{
int __retres1 ;
__retres1 = 0;
 __return_8484 = __retres1;
}
tmp___0 = __return_8484;
{
int __retres1 ;
__retres1 = 0;
 __return_8499 = __retres1;
}
tmp___1 = __return_8499;
}
{
}
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
label_8959:; 
{
int __retres1 ;
__retres1 = 1;
 __return_8969 = __retres1;
}
tmp___2 = __return_8969;
tmp = 0;
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_8959;
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
label_9009:; 
{
int __retres1 ;
__retres1 = 1;
 __return_9020 = __retres1;
}
tmp___2 = __return_9020;
tmp = 0;
goto label_9009;
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
 __return_8423 = __retres1;
}
tmp = __return_8423;
{
int __retres1 ;
__retres1 = 0;
 __return_8436 = __retres1;
}
tmp___0 = __return_8436;
{
int __retres1 ;
__retres1 = 0;
 __return_8451 = __retres1;
}
tmp___1 = __return_8451;
}
{
}
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
label_8530:; 
{
int __retres1 ;
__retres1 = 1;
 __return_8540 = __retres1;
}
tmp___2 = __return_8540;
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_8530;
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
label_8651:; 
{
int __retres1 ;
__retres1 = 1;
 __return_8662 = __retres1;
}
tmp___2 = __return_8662;
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_8651;
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
 __return_8715 = __retres1;
}
tmp = __return_8715;
{
int __retres1 ;
__retres1 = 0;
 __return_8728 = __retres1;
}
tmp___0 = __return_8728;
{
int __retres1 ;
if (!(((int)e) == 1))
{
__retres1 = 0;
 __return_8751 = __retres1;
}
else 
{
__retres1 = 1;
 __return_8750 = __retres1;
}
tmp___1 = __return_8751;
tmp___1 = __return_8750;
C_1_st = 0;
}
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
e = 2;
P_2_pc = 1;
P_2_st = 2;
}
label_8788:; 
{
int __retres1 ;
__retres1 = 1;
 __return_8891 = __retres1;
}
tmp___2 = __return_8891;
tmp = 0;
goto label_8788;
tmp___1 = __VERIFIER_nondet_int();
label_8795:; 
if (tmp___1 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_8866 = __retres1;
}
tmp___2 = __return_8866;
tmp = 0;
tmp___1 = __VERIFIER_nondet_int();
goto label_8795;
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
 __return_8819 = __retres3;
}
c = __return_8819;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
label_8830:; 
{
int __retres1 ;
__retres1 = 1;
 __return_8841 = __retres1;
}
tmp___2 = __return_8841;
tmp = 0;
goto label_8830;
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
label_8584:; 
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_8940 = __retres1;
}
tmp___2 = __return_8940;
tmp = 0;
goto label_8584;
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
 __return_8619 = __retres3;
}
c = __return_8619;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
label_8650:; 
{
int __retres1 ;
__retres1 = 1;
 __return_8914 = __retres1;
}
tmp___2 = __return_8914;
tmp = 0;
goto label_8650;
}
}
}
}
}
}
