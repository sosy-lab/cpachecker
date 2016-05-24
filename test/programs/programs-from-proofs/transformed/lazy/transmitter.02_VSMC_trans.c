extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
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
int E_1  =    2;
int E_2  =    2;
int is_master_triggered(void) ;
int is_transmit1_triggered(void) ;
int is_transmit2_triggered(void) ;
void immediate_notify(void) ;
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
int __return_3037;
int __return_3053;
int __return_3069;
int __return_2652;
int __return_2668;
int __return_2684;
int __return_3136;
int __return_3185;
int __return_3201;
int __return_3217;
int __return_3273;
int __return_3302;
int __return_3318;
int __return_3334;
int __return_3394;
int __return_3407;
int __return_3408;
int __return_3432;
int __return_2872;
int __return_2888;
int __return_2904;
int __return_2817;
int __return_2833;
int __return_2849;
int __return_2982;
int __return_2998;
int __return_3014;
int __return_2707;
int __return_2723;
int __return_2739;
int __return_2927;
int __return_2943;
int __return_2959;
int __return_2762;
int __return_2778;
int __return_2794;
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
if (m_i < 2)
{
m_st = 0;
if (t1_i < 2)
{
t1_st = 0;
if (t2_i < 2)
{
t2_st = 0;
}
else 
{
t2_st = 2;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_2482;
}
else 
{
label_2482:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_3037 = __retres1;
}
tmp = __return_3037;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_3053 = __retres1;
}
tmp___0 = __return_3053;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_3069 = __retres1;
}
tmp___1 = __return_3069;
t2_st = 0;
}
goto label_2692;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_2622;
}
else 
{
label_2622:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2652 = __retres1;
}
tmp = __return_2652;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2668 = __retres1;
}
tmp___0 = __return_2668;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2684 = __retres1;
}
tmp___1 = __return_2684;
t2_st = 0;
}
label_2692:; 
{
if (M_E < 2)
{
M_E = 2;
goto label_3082;
}
else 
{
label_3082:; 
if (!(T1_E == 1))
{
label_3091:; 
if (!(T2_E == 1))
{
label_3098:; 
if (!(E_1 == 1))
{
label_3105:; 
if (!(E_2 == 1))
{
label_3112:; 
}
else 
{
E_2 = 2;
goto label_3112;
}
kernel_st = 1;
label_3122:; 
{
int tmp ;
{
int __retres1 ;
__retres1 = 1;
 __return_3136 = __retres1;
}
tmp = __return_3136;
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E < 1)
{
M_E = 1;
goto label_3155;
}
else 
{
label_3155:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_3185 = __retres1;
}
tmp = __return_3185;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_3201 = __retres1;
}
tmp___0 = __return_3201;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_3217 = __retres1;
}
tmp___1 = __return_3217;
t2_st = 0;
}
{
if (M_E < 2)
{
M_E = 2;
goto label_3230;
}
else 
{
label_3230:; 
if (!(T1_E == 1))
{
label_3239:; 
if (!(T2_E == 1))
{
label_3246:; 
if (!(E_1 == 1))
{
label_3253:; 
if (!(E_2 == 1))
{
label_3260:; 
}
else 
{
E_2 = 2;
goto label_3260;
}
{
int __retres1 ;
__retres1 = 1;
 __return_3273 = __retres1;
}
tmp = __return_3273;
if (tmp < 1)
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
 __return_3302 = __retres1;
}
tmp = __return_3302;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_3318 = __retres1;
}
tmp___0 = __return_3318;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_3334 = __retres1;
}
tmp___1 = __return_3334;
t2_st = 0;
}
{
if (M_E < 2)
{
M_E = 2;
goto label_3347;
}
else 
{
label_3347:; 
if (!(T1_E == 1))
{
label_3355:; 
if (!(T2_E == 1))
{
label_3362:; 
if (!(E_1 == 1))
{
label_3369:; 
if (!(E_2 == 1))
{
label_3376:; 
}
else 
{
E_2 = 2;
goto label_3376;
}
goto label_3279;
}
else 
{
E_1 = 2;
goto label_3369;
}
}
else 
{
T2_E = 2;
goto label_3362;
}
}
else 
{
T1_E = 2;
goto label_3355;
}
}
}
}
else 
{
label_3279:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 1;
 __return_3394 = __retres1;
}
tmp = __return_3394;
if (tmp < 1)
{
__retres2 = 0;
 __return_3407 = __retres2;
}
else 
{
__retres2 = 1;
 __return_3408 = __retres2;
}
tmp___0 = __return_3407;
tmp___0 = __return_3408;
if (tmp___0 < 1)
{
}
else 
{
kernel_st = 1;
goto label_3122;
}
label_3428:; 
__retres1 = 0;
 __return_3432 = __retres1;
return 1;
}
goto label_3428;
}
}
else 
{
E_1 = 2;
goto label_3253;
}
}
else 
{
T2_E = 2;
goto label_3246;
}
}
else 
{
T1_E = 2;
goto label_3239;
}
}
}
}
}
else 
{
E_1 = 2;
goto label_3105;
}
}
else 
{
T2_E = 2;
goto label_3098;
}
}
else 
{
T1_E = 2;
goto label_3091;
}
}
}
}
}
else 
{
t1_st = 2;
if (t2_i < 2)
{
t2_st = 0;
}
else 
{
t2_st = 2;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_2542;
}
else 
{
label_2542:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2872 = __retres1;
}
tmp = __return_2872;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2888 = __retres1;
}
tmp___0 = __return_2888;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2904 = __retres1;
}
tmp___1 = __return_2904;
t2_st = 0;
}
goto label_2692;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_2562;
}
else 
{
label_2562:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2817 = __retres1;
}
tmp = __return_2817;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2833 = __retres1;
}
tmp___0 = __return_2833;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2849 = __retres1;
}
tmp___1 = __return_2849;
t2_st = 0;
}
goto label_2692;
}
}
}
else 
{
m_st = 2;
if (t1_i < 2)
{
t1_st = 0;
if (t2_i < 2)
{
t2_st = 0;
}
else 
{
t2_st = 2;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_2502;
}
else 
{
label_2502:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2982 = __retres1;
}
tmp = __return_2982;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2998 = __retres1;
}
tmp___0 = __return_2998;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_3014 = __retres1;
}
tmp___1 = __return_3014;
t2_st = 0;
}
goto label_2692;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_2602;
}
else 
{
label_2602:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2707 = __retres1;
}
tmp = __return_2707;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2723 = __retres1;
}
tmp___0 = __return_2723;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2739 = __retres1;
}
tmp___1 = __return_2739;
t2_st = 0;
}
goto label_2692;
}
}
else 
{
t1_st = 2;
if (t2_i < 2)
{
t2_st = 0;
}
else 
{
t2_st = 2;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_2522;
}
else 
{
label_2522:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2927 = __retres1;
}
tmp = __return_2927;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2943 = __retres1;
}
tmp___0 = __return_2943;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2959 = __retres1;
}
tmp___1 = __return_2959;
t2_st = 0;
}
goto label_2692;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_2582;
}
else 
{
label_2582:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2762 = __retres1;
}
tmp = __return_2762;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2778 = __retres1;
}
tmp___0 = __return_2778;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2794 = __retres1;
}
tmp___1 = __return_2794;
t2_st = 0;
}
goto label_2692;
}
}
}
}
}
}
