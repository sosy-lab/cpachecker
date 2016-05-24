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
int __return_2714;
int __return_2728;
int __return_2742;
int __return_2371;
int __return_2385;
int __return_2399;
int __return_2798;
int __return_2843;
int __return_2857;
int __return_2871;
int __return_2917;
int __return_2942;
int __return_2956;
int __return_2970;
int __return_3020;
int __return_3030;
int __return_3031;
int __return_3053;
int __return_2567;
int __return_2581;
int __return_2595;
int __return_2518;
int __return_2532;
int __return_2546;
int __return_2665;
int __return_2679;
int __return_2693;
int __return_2420;
int __return_2434;
int __return_2448;
int __return_2616;
int __return_2630;
int __return_2644;
int __return_2469;
int __return_2483;
int __return_2497;
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
goto label_2210;
}
else 
{
label_2210:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2714 = __retres1;
}
tmp = __return_2714;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2728 = __retres1;
}
tmp___0 = __return_2728;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2742 = __retres1;
}
tmp___1 = __return_2742;
t2_st = 0;
}
goto label_2406;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_2343;
}
else 
{
label_2343:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2371 = __retres1;
}
tmp = __return_2371;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2385 = __retres1;
}
tmp___0 = __return_2385;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2399 = __retres1;
}
tmp___1 = __return_2399;
t2_st = 0;
}
label_2406:; 
{
if (M_E < 2)
{
M_E = 2;
goto label_2754;
}
else 
{
label_2754:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_2761;
}
else 
{
label_2761:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_2766;
}
else 
{
label_2766:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_2771;
}
else 
{
label_2771:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_2776;
}
else 
{
label_2776:; 
}
kernel_st = 1;
label_2784:; 
{
int tmp ;
{
int __retres1 ;
__retres1 = 1;
 __return_2798 = __retres1;
}
tmp = __return_2798;
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E < 1)
{
M_E = 1;
goto label_2815;
}
else 
{
label_2815:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2843 = __retres1;
}
tmp = __return_2843;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2857 = __retres1;
}
tmp___0 = __return_2857;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2871 = __retres1;
}
tmp___1 = __return_2871;
t2_st = 0;
}
{
if (M_E < 2)
{
M_E = 2;
goto label_2883;
}
else 
{
label_2883:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_2890;
}
else 
{
label_2890:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_2895;
}
else 
{
label_2895:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_2900;
}
else 
{
label_2900:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_2905;
}
else 
{
label_2905:; 
}
{
int __retres1 ;
__retres1 = 1;
 __return_2917 = __retres1;
}
tmp = __return_2917;
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
 __return_2942 = __retres1;
}
tmp = __return_2942;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2956 = __retres1;
}
tmp___0 = __return_2956;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2970 = __retres1;
}
tmp___1 = __return_2970;
t2_st = 0;
}
{
if (M_E < 2)
{
M_E = 2;
goto label_2982;
}
else 
{
label_2982:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_2988;
}
else 
{
label_2988:; 
if (T2_E == 1)
{
T2_E = 2;
goto label_2993;
}
else 
{
label_2993:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_2998;
}
else 
{
label_2998:; 
if (E_2 == 1)
{
E_2 = 2;
goto label_3003;
}
else 
{
label_3003:; 
}
goto label_2922;
}
}
}
}
}
}
else 
{
label_2922:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 1;
 __return_3020 = __retres1;
}
tmp = __return_3020;
if (tmp < 1)
{
__retres2 = 0;
 __return_3030 = __retres2;
}
else 
{
__retres2 = 1;
 __return_3031 = __retres2;
}
tmp___0 = __return_3030;
tmp___0 = __return_3031;
if (tmp___0 < 1)
{
}
else 
{
kernel_st = 1;
goto label_2784;
}
label_3050:; 
__retres1 = 0;
 __return_3053 = __retres1;
return 1;
}
goto label_3050;
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
goto label_2267;
}
else 
{
label_2267:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2567 = __retres1;
}
tmp = __return_2567;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2581 = __retres1;
}
tmp___0 = __return_2581;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2595 = __retres1;
}
tmp___1 = __return_2595;
t2_st = 0;
}
goto label_2406;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_2286;
}
else 
{
label_2286:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2518 = __retres1;
}
tmp = __return_2518;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2532 = __retres1;
}
tmp___0 = __return_2532;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2546 = __retres1;
}
tmp___1 = __return_2546;
t2_st = 0;
}
goto label_2406;
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
goto label_2229;
}
else 
{
label_2229:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2665 = __retres1;
}
tmp = __return_2665;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2679 = __retres1;
}
tmp___0 = __return_2679;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2693 = __retres1;
}
tmp___1 = __return_2693;
t2_st = 0;
}
goto label_2406;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_2324;
}
else 
{
label_2324:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2420 = __retres1;
}
tmp = __return_2420;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2434 = __retres1;
}
tmp___0 = __return_2434;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2448 = __retres1;
}
tmp___1 = __return_2448;
t2_st = 0;
}
goto label_2406;
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
goto label_2248;
}
else 
{
label_2248:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2616 = __retres1;
}
tmp = __return_2616;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2630 = __retres1;
}
tmp___0 = __return_2630;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2644 = __retres1;
}
tmp___1 = __return_2644;
t2_st = 0;
}
goto label_2406;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_2305;
}
else 
{
label_2305:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2469 = __retres1;
}
tmp = __return_2469;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2483 = __retres1;
}
tmp___0 = __return_2483;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2497 = __retres1;
}
tmp___1 = __return_2497;
t2_st = 0;
}
goto label_2406;
}
}
}
}
}
}
