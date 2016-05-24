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
int __return_2965;
int __return_2992;
int __return_3010;
int __return_3042;
int __return_3182;
int __return_3250;
int __return_3277;
int __return_3323;
int __return_3485;
char __return_3435;
int __return_3303;
int __return_3512;
int __return_3536;
char __return_3138;
int __return_3460;
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
label_2952:; 
__retres1 = 0;
label_2964:; 
 __return_2965 = __retres1;
}
else 
{
if (!(((int)P_1_ev) == 1))
{
goto label_2952;
}
else 
{
__retres1 = 1;
goto label_2964;
}
}
tmp = __return_2965;
if (!(tmp == 0))
{
P_1_st = 0;
label_2971:; 
{
int __retres1 ;
if (!(((int)P_2_pc) == 1))
{
label_2979:; 
__retres1 = 0;
label_2991:; 
 __return_2992 = __retres1;
}
else 
{
if (!(((int)P_2_ev) == 1))
{
goto label_2979;
}
else 
{
__retres1 = 1;
goto label_2991;
}
}
tmp___0 = __return_2992;
if (!(tmp___0 == 0))
{
P_2_st = 0;
label_2998:; 
{
int __retres1 ;
__retres1 = 0;
 __return_3010 = __retres1;
}
tmp___1 = __return_3010;
}
else 
{
goto label_2998;
}
{
}
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
label_3032:; 
{
int __retres1 ;
__retres1 = 1;
 __return_3042 = __retres1;
}
tmp___2 = __return_3042;
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_3032;
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
label_3171:; 
{
int __retres1 ;
__retres1 = 1;
 __return_3182 = __retres1;
}
tmp___2 = __return_3182;
tmp = 0;
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_3171;
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
label_3237:; 
__retres1 = 0;
label_3249:; 
 __return_3250 = __retres1;
}
else 
{
if (!(((int)P_1_ev) == 1))
{
goto label_3237;
}
else 
{
__retres1 = 1;
goto label_3249;
}
}
tmp = __return_3250;
if (!(tmp == 0))
{
P_1_st = 0;
label_3256:; 
{
int __retres1 ;
if (!(((int)P_2_pc) == 1))
{
label_3264:; 
__retres1 = 0;
label_3276:; 
 __return_3277 = __retres1;
}
else 
{
if (!(((int)P_2_ev) == 1))
{
goto label_3264;
}
else 
{
__retres1 = 1;
goto label_3276;
}
}
tmp___0 = __return_3277;
if (!(tmp___0 == 0))
{
P_2_st = 0;
{
int __retres1 ;
if (!(((int)e) == 1))
{
__retres1 = 0;
label_3322:; 
 __return_3323 = __retres1;
}
else 
{
__retres1 = 1;
goto label_3322;
}
tmp___1 = __return_3323;
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
label_3373:; 
e = 2;
P_2_pc = 1;
P_2_st = 2;
goto label_3375;
}
else 
{
{
int __retres1 ;
if (!(((int)e) == 1))
{
__retres1 = 0;
label_3302:; 
 __return_3303 = __retres1;
}
else 
{
__retres1 = 1;
goto label_3302;
}
tmp___1 = __return_3303;
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
goto label_3373;
e = 2;
P_2_pc = 1;
P_2_st = 2;
label_3375:; 
goto label_3378;
}
label_3498:; 
label_3500:; 
tmp___1 = __VERIFIER_nondet_int();
label_3395:; 
if (tmp___1 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_3485 = __retres1;
}
tmp___2 = __return_3485;
tmp = 0;
goto label_3498;
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
label_3430:; 
 __return_3435 = __retres3;
}
else 
{
__retres3 = data_1;
goto label_3430;
}
c = __return_3435;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
else 
{
__retres3 = data_0;
goto label_3430;
}
goto label_3443;
}
}
}
}
}
else 
{
goto label_3256;
}
}
}
}
}
else 
{
P_2_st = 2;
label_3378:; 
}
label_3388:; 
{
int __retres1 ;
__retres1 = 1;
 __return_3512 = __retres1;
}
tmp___2 = __return_3512;
tmp = 0;
goto label_3388;
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
label_3087:; 
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_3536 = __retres1;
}
tmp___2 = __return_3536;
tmp = 0;
goto label_3087;
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
label_3133:; 
 __return_3138 = __retres3;
}
else 
{
__retres3 = data_1;
goto label_3133;
}
c = __return_3138;
i = i + 1;
C_1_pc = 2;
C_1_st = 2;
}
else 
{
__retres3 = data_0;
goto label_3133;
}
label_3443:; 
label_3445:; 
label_3449:; 
{
int __retres1 ;
__retres1 = 1;
 __return_3460 = __retres1;
}
tmp___2 = __return_3460;
tmp = 0;
goto label_3449;
}
}
}
}
}
}
}
else 
{
goto label_2971;
}
}
}
}
}
