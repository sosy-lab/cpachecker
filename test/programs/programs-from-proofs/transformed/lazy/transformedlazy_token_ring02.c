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
int __return_53836;
int __return_53858;
int __return_53880;
int __return_53974;
int __return_55280;
int __return_55348;
int __return_55370;
int __return_55396;
int __return_55536;
int __return_55604;
int __return_55626;
int __return_55650;
int __return_56018;
int __return_55752;
int __return_55778;
int __return_55800;
int __return_55982;
int __return_55954;
int __return_56232;
int __return_56258;
int __return_56284;
int __return_56506;
int __return_56596;
int __return_56622;
int __return_56648;
int __return_56930;
int __return_56948;
int __return_57018;
int __return_57088;
int __return_54434;
int __return_54502;
int __return_54524;
int __return_54552;
int __return_55166;
int __return_54654;
int __return_54680;
int __return_54702;
int __return_55130;
int __return_54812;
int __return_54840;
int __return_54866;
int __return_54926;
int __return_55018;
int __return_55040;
int __return_55068;
int __return_54042;
int __return_54064;
int __return_54086;
int __return_56050;
int __return_55198;
int __return_55502;
int __return_54410;
int __return_56324;
int __return_56350;
int __return_56376;
int __return_56528;
int __return_56688;
int __return_56714;
int __return_56740;
int __return_56882;
int __return_56900;
int __return_57070;
int __return_57090;
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
goto label_53770;
}
else 
{
label_53770:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_53780;
}
else 
{
label_53780:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_53790;
}
else 
{
label_53790:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_53836 = __retres1;
}
tmp = __return_53836;
{
int __retres1 ;
__retres1 = 0;
 __return_53858 = __retres1;
}
tmp___0 = __return_53858;
{
int __retres1 ;
__retres1 = 0;
 __return_53880 = __retres1;
}
tmp___1 = __return_53880;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_53900;
}
else 
{
label_53900:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_53910;
}
else 
{
label_53910:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_53920;
}
else 
{
label_53920:; 
}
kernel_st = 1;
{
int tmp ;
label_53956:; 
{
int __retres1 ;
__retres1 = 1;
 __return_53974 = __retres1;
}
tmp = __return_53974;
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
goto label_53956;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_54378:; 
{
int __retres1 ;
__retres1 = 1;
 __return_55280 = __retres1;
}
tmp = __return_55280;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_54378;
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_54592;
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
 __return_55348 = __retres1;
}
tmp = __return_55348;
{
int __retres1 ;
__retres1 = 0;
 __return_55370 = __retres1;
}
tmp___0 = __return_55370;
{
int __retres1 ;
__retres1 = 0;
 __return_55396 = __retres1;
}
tmp___1 = __return_55396;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_55208;
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
label_54206:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_55536 = __retres1;
}
tmp = __return_55536;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_54206;
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
 __return_55604 = __retres1;
}
tmp = __return_55604;
{
int __retres1 ;
__retres1 = 1;
 __return_55626 = __retres1;
}
tmp___0 = __return_55626;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_55650 = __retres1;
}
tmp___1 = __return_55650;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_55684:; 
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
 __return_56018 = __retres1;
}
tmp = __return_56018;
goto label_55684;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_54754;
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
 __return_55752 = __retres1;
}
tmp = __return_55752;
{
int __retres1 ;
__retres1 = 0;
 __return_55778 = __retres1;
}
tmp___0 = __return_55778;
{
int __retres1 ;
__retres1 = 0;
 __return_55800 = __retres1;
}
tmp___1 = __return_55800;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_55840:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_55982 = __retres1;
}
tmp = __return_55982;
goto label_55840;
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
 __return_55954 = __retres1;
}
tmp = __return_55954;
}
label_56078:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_56162;
}
else 
{
label_56162:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_56172;
}
else 
{
label_56172:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_56182;
}
else 
{
label_56182:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_56232 = __retres1;
}
tmp = __return_56232;
{
int __retres1 ;
__retres1 = 0;
 __return_56258 = __retres1;
}
tmp___0 = __return_56258;
{
int __retres1 ;
__retres1 = 0;
 __return_56284 = __retres1;
}
tmp___1 = __return_56284;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_56446;
}
else 
{
label_56446:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_56456;
}
else 
{
label_56456:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_56466;
}
else 
{
label_56466:; 
}
{
int __retres1 ;
__retres1 = 0;
 __return_56506 = __retres1;
}
tmp = __return_56506;
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
 __return_56596 = __retres1;
}
tmp = __return_56596;
{
int __retres1 ;
__retres1 = 0;
 __return_56622 = __retres1;
}
tmp___0 = __return_56622;
{
int __retres1 ;
__retres1 = 0;
 __return_56648 = __retres1;
}
tmp___1 = __return_56648;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_56810;
}
else 
{
label_56810:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_56820;
}
else 
{
label_56820:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_56830;
}
else 
{
label_56830:; 
}
goto label_56540;
}
}
}
}
else 
{
label_56540:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 0;
 __return_56930 = __retres1;
}
tmp = __return_56930;
if (tmp == 0)
{
__retres2 = 1;
goto label_56940;
}
else 
{
__retres2 = 0;
label_56940:; 
 __return_56948 = __retres2;
}
tmp___0 = __return_56948;
if (tmp___0 == 0)
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
__retres1 = 0;
 __return_57018 = __retres1;
}
tmp = __return_57018;
}
goto label_56078;
}
else 
{
}
__retres1 = 0;
 __return_57088 = __retres1;
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
label_54382:; 
{
int __retres1 ;
__retres1 = 1;
 __return_54434 = __retres1;
}
tmp = __return_54434;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
label_54592:; 
goto label_54382;
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
 __return_54502 = __retres1;
}
tmp = __return_54502;
{
int __retres1 ;
__retres1 = 1;
 __return_54524 = __retres1;
}
tmp___0 = __return_54524;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_54552 = __retres1;
}
tmp___1 = __return_54552;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_54578:; 
label_54586:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
label_54754:; 
{
int __retres1 ;
__retres1 = 1;
 __return_55166 = __retres1;
}
tmp = __return_55166;
goto label_54586;
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
 __return_54654 = __retres1;
}
tmp = __return_54654;
{
int __retres1 ;
__retres1 = 0;
 __return_54680 = __retres1;
}
tmp___0 = __return_54680;
{
int __retres1 ;
__retres1 = 1;
 __return_54702 = __retres1;
}
tmp___1 = __return_54702;
t2_st = 0;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_54744:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_55130 = __retres1;
}
tmp = __return_55130;
goto label_54744;
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
 __return_54812 = __retres1;
}
tmp = __return_54812;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_54840 = __retres1;
}
tmp___0 = __return_54840;
{
int __retres1 ;
__retres1 = 0;
 __return_54866 = __retres1;
}
tmp___1 = __return_54866;
}
}
E_M = 2;
t2_pc = 1;
t2_st = 2;
}
label_54906:; 
{
int __retres1 ;
__retres1 = 1;
 __return_54926 = __retres1;
}
tmp = __return_54926;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_54906;
}
else 
{
m_st = 1;
{
if (token != (local + 2))
{
{
}
goto label_54962;
}
else 
{
label_54962:; 
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
 __return_55018 = __retres1;
}
tmp = __return_55018;
{
int __retres1 ;
__retres1 = 1;
 __return_55040 = __retres1;
}
tmp___0 = __return_55040;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_55068 = __retres1;
}
tmp___1 = __return_55068;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_54578;
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
 __return_54042 = __retres1;
}
tmp = __return_54042;
{
int __retres1 ;
__retres1 = 0;
 __return_54064 = __retres1;
}
tmp___0 = __return_54064;
{
int __retres1 ;
__retres1 = 0;
 __return_54086 = __retres1;
}
tmp___1 = __return_54086;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_54120:; 
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
 __return_56050 = __retres1;
}
tmp = __return_56050;
goto label_54120;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_54380:; 
{
int __retres1 ;
__retres1 = 1;
 __return_55198 = __retres1;
}
tmp = __return_55198;
label_55208:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_54380;
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_54384;
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
label_54208:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_55502 = __retres1;
}
tmp = __return_55502;
goto label_54208;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_54384:; 
{
int __retres1 ;
__retres1 = 0;
 __return_54410 = __retres1;
}
tmp = __return_54410;
}
label_56076:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E == 0)
{
M_E = 1;
goto label_56112;
}
else 
{
label_56112:; 
if (T1_E == 0)
{
T1_E = 1;
goto label_56122;
}
else 
{
label_56122:; 
if (T2_E == 0)
{
T2_E = 1;
goto label_56132;
}
else 
{
label_56132:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_56324 = __retres1;
}
tmp = __return_56324;
{
int __retres1 ;
__retres1 = 0;
 __return_56350 = __retres1;
}
tmp___0 = __return_56350;
{
int __retres1 ;
__retres1 = 0;
 __return_56376 = __retres1;
}
tmp___1 = __return_56376;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_56396;
}
else 
{
label_56396:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_56406;
}
else 
{
label_56406:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_56416;
}
else 
{
label_56416:; 
}
{
int __retres1 ;
__retres1 = 0;
 __return_56528 = __retres1;
}
tmp = __return_56528;
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
 __return_56688 = __retres1;
}
tmp = __return_56688;
{
int __retres1 ;
__retres1 = 0;
 __return_56714 = __retres1;
}
tmp___0 = __return_56714;
{
int __retres1 ;
__retres1 = 0;
 __return_56740 = __retres1;
}
tmp___1 = __return_56740;
}
{
if (M_E == 1)
{
M_E = 2;
goto label_56760;
}
else 
{
label_56760:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_56770;
}
else 
{
label_56770:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_56780;
}
else 
{
label_56780:; 
}
goto label_56542;
}
}
}
}
else 
{
label_56542:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 0;
 __return_56882 = __retres1;
}
tmp = __return_56882;
if (tmp == 0)
{
__retres2 = 1;
goto label_56892;
}
else 
{
__retres2 = 0;
label_56892:; 
 __return_56900 = __retres2;
}
tmp___0 = __return_56900;
if (tmp___0 == 0)
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
__retres1 = 0;
 __return_57070 = __retres1;
}
tmp = __return_57070;
}
goto label_56076;
}
else 
{
}
__retres1 = 0;
 __return_57090 = __retres1;
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
