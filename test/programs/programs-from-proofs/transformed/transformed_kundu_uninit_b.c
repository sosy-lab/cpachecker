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
int __return_32988;
int __return_33010;
int __return_33036;
int __return_33094;
int __return_33312;
int __return_33400;
int __return_33422;
int __return_33458;
int __return_33672;
char __return_33586;
int __return_33628;
int __return_33460;
int __return_33712;
int __return_33798;
char __return_33230;
int __return_33752;
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
 __return_32988 = __retres1;
}
tmp = __return_32988;
{
int __retres1 ;
__retres1 = 0;
 __return_33010 = __retres1;
}
tmp___0 = __return_33010;
{
int __retres1 ;
__retres1 = 0;
 __return_33036 = __retres1;
}
tmp___1 = __return_33036;
}
{
}
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
label_33076:; 
{
int __retres1 ;
__retres1 = 1;
 __return_33094 = __retres1;
}
tmp___2 = __return_33094;
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_33076;
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
label_33292:; 
{
int __retres1 ;
__retres1 = 1;
 __return_33312 = __retres1;
}
tmp___2 = __return_33312;
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_33292;
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
 __return_33400 = __retres1;
}
tmp = __return_33400;
{
int __retres1 ;
__retres1 = 0;
 __return_33422 = __retres1;
}
tmp___0 = __return_33422;
{
int __retres1 ;
if (((int)e) == 1)
{
__retres1 = 1;
 __return_33458 = __retres1;
}
else 
{
__retres1 = 0;
 __return_33460 = __retres1;
}
tmp___1 = __return_33458;
C_1_st = 0;
tmp___1 = __return_33460;
}
}
e = 2;
P_2_pc = 1;
P_2_st = 2;
e = 2;
P_2_pc = 1;
P_2_st = 2;
}
label_33530:; 
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_33672 = __retres1;
}
tmp___2 = __return_33672;
tmp = 0;
goto label_33530;
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
 __return_33586 = __retres3;
}
c = __return_33586;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
label_33608:; 
{
int __retres1 ;
__retres1 = 1;
 __return_33628 = __retres1;
}
tmp___2 = __return_33628;
tmp = 0;
goto label_33608;
}
label_33528:; 
{
int __retres1 ;
__retres1 = 1;
 __return_33712 = __retres1;
}
tmp___2 = __return_33712;
tmp = 0;
goto label_33528;
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
label_33168:; 
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_33798 = __retres1;
}
tmp___2 = __return_33798;
tmp = 0;
goto label_33168;
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
 __return_33230 = __retres3;
}
c = __return_33230;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
label_33290:; 
{
int __retres1 ;
__retres1 = 1;
 __return_33752 = __retres1;
}
tmp___2 = __return_33752;
tmp = 0;
goto label_33290;
}
}
}
}
}
