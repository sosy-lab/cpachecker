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
int __return_8360;
int __return_8373;
int __return_8388;
int __return_8924;
int __return_8976;
int __return_8312;
int __return_8325;
int __return_8340;
int __return_8429;
int __return_8568;
int __return_8625;
int __return_8638;
int __return_8661;
int __return_8823;
int __return_8660;
int __return_8798;
char __return_8751;
int __return_8773;
int __return_8846;
int __return_8895;
char __return_8524;
int __return_8869;
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
 __return_8360 = __retres1;
}
tmp = __return_8360;
{
int __retres1 ;
__retres1 = 0;
 __return_8373 = __retres1;
}
tmp___0 = __return_8373;
{
int __retres1 ;
__retres1 = 0;
 __return_8388 = __retres1;
}
tmp___1 = __return_8388;
}
{
}
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
label_8914:; 
{
int __retres1 ;
__retres1 = 1;
 __return_8924 = __retres1;
}
tmp___2 = __return_8924;
tmp = 0;
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_8914;
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
label_8965:; 
{
int __retres1 ;
__retres1 = 1;
 __return_8976 = __retres1;
}
tmp___2 = __return_8976;
tmp = 0;
goto label_8965;
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
 __return_8312 = __retres1;
}
tmp = __return_8312;
{
int __retres1 ;
__retres1 = 0;
 __return_8325 = __retres1;
}
tmp___0 = __return_8325;
{
int __retres1 ;
__retres1 = 0;
 __return_8340 = __retres1;
}
tmp___1 = __return_8340;
}
{
}
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
label_8419:; 
{
int __retres1 ;
__retres1 = 1;
 __return_8429 = __retres1;
}
tmp___2 = __return_8429;
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_8419;
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
label_8557:; 
{
int __retres1 ;
__retres1 = 1;
 __return_8568 = __retres1;
}
tmp___2 = __return_8568;
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_8557;
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
__retres1 = 0;
 __return_8625 = __retres1;
}
tmp = __return_8625;
{
int __retres1 ;
__retres1 = 0;
 __return_8638 = __retres1;
}
tmp___0 = __return_8638;
{
int __retres1 ;
if (!(((int)e) == 1))
{
__retres1 = 0;
 __return_8661 = __retres1;
}
else 
{
__retres1 = 1;
 __return_8660 = __retres1;
}
tmp___1 = __return_8661;
tmp___1 = __return_8660;
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
label_8702:; 
{
int __retres1 ;
__retres1 = 1;
 __return_8823 = __retres1;
}
tmp___2 = __return_8823;
tmp = 0;
goto label_8702;
tmp___1 = __VERIFIER_nondet_int();
label_8711:; 
if (tmp___1 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_8798 = __retres1;
}
tmp___2 = __return_8798;
tmp = 0;
tmp___1 = __VERIFIER_nondet_int();
goto label_8711;
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
label_8746:; 
 __return_8751 = __retres3;
}
else 
{
__retres3 = data_1;
goto label_8746;
}
c = __return_8751;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
else 
{
__retres3 = data_0;
goto label_8746;
}
label_8762:; 
{
int __retres1 ;
__retres1 = 1;
 __return_8773 = __retres1;
}
tmp___2 = __return_8773;
tmp = 0;
goto label_8762;
}
}
}
}
else 
{
P_2_st = 2;
}
label_8701:; 
{
int __retres1 ;
__retres1 = 1;
 __return_8846 = __retres1;
}
tmp___2 = __return_8846;
tmp = 0;
goto label_8701;
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
label_8473:; 
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_8895 = __retres1;
}
tmp___2 = __return_8895;
tmp = 0;
goto label_8473;
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
label_8519:; 
 __return_8524 = __retres3;
}
else 
{
__retres3 = data_1;
goto label_8519;
}
c = __return_8524;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
else 
{
__retres3 = data_0;
goto label_8519;
}
label_8556:; 
{
int __retres1 ;
__retres1 = 1;
 __return_8869 = __retres1;
}
tmp___2 = __return_8869;
tmp = 0;
goto label_8556;
}
}
}
}
}
}
}
}
