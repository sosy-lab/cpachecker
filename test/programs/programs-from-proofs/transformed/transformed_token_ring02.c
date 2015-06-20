void t1_started();
void t2_started();
void error(void);
int m_pc  =    0;
int t1_pc  =    0;
int t2_pc  =    0;
int m_st  ;
int t1_st  ;
int t2_st  ;
int m_i  ;
int t1_i  ;
int t2_i  ;
int M_E  =    2;
int T1_E  =    2;
int T2_E  =    2;
int E_M  =    2;
int E_1  =    2;
int E_2  =    2;
int is_master_triggered(void) ;
int is_transmit1_triggered(void) ;
int is_transmit2_triggered(void) ;
void immediate_notify(void) ;
int token  ;
int __VERIFIER_nondet_int()  ;
int local  ;
void master(void);
void transmit1(void);
void transmit2(void);
void update_channels(void);
void init_threads(void);
int exists_runnable_thread(void);
void eval(void);
void fire_delta_events(void);
void reset_delta_events(void);
void activate_threads(void);
void fire_time_events(void);
void reset_time_events(void);
void init_model(void);
int stop_simulation(void);
void start_simulation(void);
int main(void);
int __return_56978;
int __return_57000;
int __return_57022;
int __return_57116;
int __return_58422;
int __return_58490;
int __return_58512;
int __return_58538;
int __return_58678;
int __return_58746;
int __return_58768;
int __return_58792;
int __return_59160;
int __return_58894;
int __return_58920;
int __return_58942;
int __return_59124;
int __return_59096;
int __return_59374;
int __return_59400;
int __return_59426;
int __return_59648;
int __return_59738;
int __return_59764;
int __return_59790;
int __return_60072;
int __return_60090;
int __return_60160;
int __return_60230;
int __return_57576;
int __return_57644;
int __return_57666;
int __return_57694;
int __return_58308;
int __return_57796;
int __return_57822;
int __return_57844;
int __return_58272;
int __return_57954;
int __return_57982;
int __return_58008;
int __return_58068;
int __return_58160;
int __return_58182;
int __return_58210;
int __return_57184;
int __return_57206;
int __return_57228;
int __return_59192;
int __return_58340;
int __return_58644;
int __return_57552;
int __return_59466;
int __return_59492;
int __return_59518;
int __return_59670;
int __return_59830;
int __return_59856;
int __return_59882;
int __return_60024;
int __return_60042;
int __return_60212;
int __return_60232;
int main()
{
int __retres1 ;
{
m_i = 1;
t1_i = 1;
t2_i = 1;
}
{
int kernel_st ;
int tmp ;
int tmp___0 ;
kernel_st = 0;
{
}
{
m_st = 0;
t1_st = 0;
t2_st = 0;
}
{
if (M_E == 0)
{
M_E = 1;
goto label_56912;
}
else 
{
label_56912:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_56922;
}
else 
{
label_56922:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_56932;
}
else 
{
label_56932:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_56978 = __retres1;
}
tmp = __return_56978;
{
int __retres1 ;
__retres1 = 0;
 __return_57000 = __retres1;
}
tmp___0 = __return_57000;
{
int __retres1 ;
__retres1 = 0;
 __return_57022 = __retres1;
}
tmp___1 = __return_57022;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_57042;
}
else 
{
label_57042:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_57052;
}
else 
{
label_57052:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_57062;
}
else 
{
label_57062:; 
}
kernel_st = 1;
{
int tmp ;
label_57098:; 
{
int __retres1 ;
__retres1 = 1;
 __return_57116 = __retres1;
}
tmp = __return_57116;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_57098;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_57520:; 
{
int __retres1 ;
__retres1 = 1;
 __return_58422 = __retres1;
}
tmp = __return_58422;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_57520;
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_57734;
}
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_58490 = __retres1;
}
tmp = __return_58490;
{
int __retres1 ;
__retres1 = 0;
 __return_58512 = __retres1;
}
tmp___0 = __return_58512;
{
int __retres1 ;
__retres1 = 0;
 __return_58538 = __retres1;
}
tmp___1 = __return_58538;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_58350;
}
}
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
label_57348:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_58678 = __retres1;
}
tmp = __return_58678;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_57348;
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_58746 = __retres1;
}
tmp = __return_58746;
{
int __retres1 ;
__retres1 = 1;
 __return_58768 = __retres1;
}
tmp___0 = __return_58768;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_58792 = __retres1;
}
tmp___1 = __return_58792;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_58826:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_59160 = __retres1;
}
tmp = __return_59160;
goto label_58826;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_57896;
}
}
else 
{
t1_st = 1;
{
t1_started();
token = token + 1;
E_2 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_58894 = __retres1;
}
tmp = __return_58894;
{
int __retres1 ;
__retres1 = 0;
 __return_58920 = __retres1;
}
tmp___0 = __return_58920;
{
int __retres1 ;
__retres1 = 0;
 __return_58942 = __retres1;
}
tmp___1 = __return_58942;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_58982:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_59124 = __retres1;
}
tmp = __return_59124;
goto label_58982;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
{
int __retres1 ;
__retres1 = 0;
 __return_59096 = __retres1;
}
tmp = __return_59096;
}
label_59220:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_59304;
}
else 
{
label_59304:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_59314;
}
else 
{
label_59314:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_59324;
}
else 
{
label_59324:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_59374 = __retres1;
}
tmp = __return_59374;
{
int __retres1 ;
__retres1 = 0;
 __return_59400 = __retres1;
}
tmp___0 = __return_59400;
{
int __retres1 ;
__retres1 = 0;
 __return_59426 = __retres1;
}
tmp___1 = __return_59426;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_59588;
}
else 
{
label_59588:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_59598;
}
else 
{
label_59598:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_59608;
}
else 
{
label_59608:; 
}
{
int __retres1 ;
__retres1 = 0;
 __return_59648 = __retres1;
}
tmp = __return_59648;
if (tmp == 0)
{
kernel_st = 4;
{
M_E = 1;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_59738 = __retres1;
}
tmp = __return_59738;
{
int __retres1 ;
__retres1 = 0;
 __return_59764 = __retres1;
}
tmp___0 = __return_59764;
{
int __retres1 ;
__retres1 = 0;
 __return_59790 = __retres1;
}
tmp___1 = __return_59790;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_59952;
}
else 
{
label_59952:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_59962;
}
else 
{
label_59962:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_59972;
}
else 
{
label_59972:; 
}
goto label_59682;
}
}
}
}
else 
{
label_59682:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 0;
 __return_60072 = __retres1;
}
tmp = __return_60072;
if (tmp == 0)
{
__retres2 = 1;
goto label_60082;
}
else 
{
__retres2 = 0;
label_60082:; 
 __return_60090 = __retres2;
}
tmp___0 = __return_60090;
if (tmp___0 == 0)
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
__retres1 = 0;
 __return_60160 = __retres1;
}
tmp = __return_60160;
}
goto label_59220;
}
else 
{
}
__retres1 = 0;
 __return_60230 = __retres1;
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
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_57524:; 
{
int __retres1 ;
__retres1 = 1;
 __return_57576 = __retres1;
}
tmp = __return_57576;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
label_57734:; 
goto label_57524;
}
else 
{
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_57644 = __retres1;
}
tmp = __return_57644;
{
int __retres1 ;
__retres1 = 1;
 __return_57666 = __retres1;
}
tmp___0 = __return_57666;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_57694 = __retres1;
}
tmp___1 = __return_57694;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_57720:; 
label_57728:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
label_57896:; 
{
int __retres1 ;
__retres1 = 1;
 __return_58308 = __retres1;
}
tmp = __return_58308;
goto label_57728;
}
else 
{
t1_st = 1;
{
t1_started();
token = token + 1;
E_2 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_57796 = __retres1;
}
tmp = __return_57796;
{
int __retres1 ;
__retres1 = 0;
 __return_57822 = __retres1;
}
tmp___0 = __return_57822;
{
int __retres1 ;
__retres1 = 1;
 __return_57844 = __retres1;
}
tmp___1 = __return_57844;
t2_st = 0;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_57886:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_58272 = __retres1;
}
tmp = __return_58272;
goto label_57886;
}
else 
{
t2_st = 1;
{
t2_started();
token = token + 1;
E_M = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 1;
 __return_57954 = __retres1;
}
tmp = __return_57954;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_57982 = __retres1;
}
tmp___0 = __return_57982;
{
int __retres1 ;
__retres1 = 0;
 __return_58008 = __retres1;
}
tmp___1 = __return_58008;
}
}
E_M = 2;
t2_pc = 1;
t2_st = 2;
}
label_58048:; 
{
int __retres1 ;
__retres1 = 1;
 __return_58068 = __retres1;
}
tmp = __return_58068;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_58048;
}
else 
{
m_st = 1;
{
if (token != (local + 2))
{
{
}
goto label_58104;
}
else 
{
label_58104:; 
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_58160 = __retres1;
}
tmp = __return_58160;
{
int __retres1 ;
__retres1 = 1;
 __return_58182 = __retres1;
}
tmp___0 = __return_58182;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_58210 = __retres1;
}
tmp___1 = __return_58210;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_57720;
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
m_st = 1;
{
token = __VERIFIER_nondet_int();
local = token;
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_57184 = __retres1;
}
tmp = __return_57184;
{
int __retres1 ;
__retres1 = 0;
 __return_57206 = __retres1;
}
tmp___0 = __return_57206;
{
int __retres1 ;
__retres1 = 0;
 __return_57228 = __retres1;
}
tmp___1 = __return_57228;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_57262:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_59192 = __retres1;
}
tmp = __return_59192;
goto label_57262;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_57522:; 
{
int __retres1 ;
__retres1 = 1;
 __return_58340 = __retres1;
}
tmp = __return_58340;
label_58350:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_57522;
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_57526;
}
}
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
label_57350:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_58644 = __retres1;
}
tmp = __return_58644;
goto label_57350;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_57526:; 
{
int __retres1 ;
__retres1 = 0;
 __return_57552 = __retres1;
}
tmp = __return_57552;
}
label_59218:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_59254;
}
else 
{
label_59254:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_59264;
}
else 
{
label_59264:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_59274;
}
else 
{
label_59274:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_59466 = __retres1;
}
tmp = __return_59466;
{
int __retres1 ;
__retres1 = 0;
 __return_59492 = __retres1;
}
tmp___0 = __return_59492;
{
int __retres1 ;
__retres1 = 0;
 __return_59518 = __retres1;
}
tmp___1 = __return_59518;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_59538;
}
else 
{
label_59538:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_59548;
}
else 
{
label_59548:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_59558;
}
else 
{
label_59558:; 
}
{
int __retres1 ;
__retres1 = 0;
 __return_59670 = __retres1;
}
tmp = __return_59670;
if (tmp == 0)
{
kernel_st = 4;
{
M_E = 1;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_59830 = __retres1;
}
tmp = __return_59830;
{
int __retres1 ;
__retres1 = 0;
 __return_59856 = __retres1;
}
tmp___0 = __return_59856;
{
int __retres1 ;
__retres1 = 0;
 __return_59882 = __retres1;
}
tmp___1 = __return_59882;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_59902;
}
else 
{
label_59902:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_59912;
}
else 
{
label_59912:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_59922;
}
else 
{
label_59922:; 
}
goto label_59684;
}
}
}
}
else 
{
label_59684:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 0;
 __return_60024 = __retres1;
}
tmp = __return_60024;
if (tmp == 0)
{
__retres2 = 1;
goto label_60034;
}
else 
{
__retres2 = 0;
label_60034:; 
 __return_60042 = __retres2;
}
tmp___0 = __return_60042;
if (tmp___0 == 0)
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
__retres1 = 0;
 __return_60212 = __retres1;
}
tmp = __return_60212;
}
goto label_59218;
}
else 
{
}
__retres1 = 0;
 __return_60232 = __retres1;
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
}
}
}
}
}
}
}
}
}
