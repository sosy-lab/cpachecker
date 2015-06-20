extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void error(void);
int m_pc  =    0;
int t1_pc  =    0;
int m_st  ;
int t1_st  ;
int m_i  ;
int t1_i  ;
int M_E  =    2;
int T1_E  =    2;
int E_1  =    2;
int is_master_triggered(void) ;
int is_transmit1_triggered(void) ;
void immediate_notify(void) ;
void master(void);
void transmit1(void);
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
int __return_1693;
int __return_1709;
int __return_1767;
int __return_1819;
int __return_1835;
int __return_1938;
int __return_2438;
int __return_2461;
int __return_2644;
int __return_2771;
int __return_2792;
int __return_2793;
int __return_3047;
int __return_3058;
int __return_3769;
int __return_3669;
int __return_2462;
int __return_2194;
int __return_2341;
int __return_2357;
int __return_2600;
int __return_2878;
int __return_2892;
int __return_3127;
int __return_3138;
int __return_3771;
int __return_3194;
int __return_3287;
int __return_2036;
int __return_2088;
int __return_2111;
int __return_2112;
int __return_2382;
int __return_2405;
int __return_2622;
int __return_2823;
int __return_2846;
int __return_2847;
int __return_3087;
int __return_3098;
int __return_3770;
int __return_3397;
int __return_3449;
int __return_3472;
int __return_3595;
int __return_3473;
int __return_2406;
int __return_2492;
int __return_2508;
int __return_2666;
int __return_2729;
int __return_2745;
int __return_3007;
int __return_3018;
int __return_3768;
int main()
{
int __retres1 ;
{
m_i = 1;
t1_i = 1;
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
}
{
if (T1_E == 0)
{
T1_E = 1;
goto label_1666;
}
else 
{
label_1666:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_1671;
}
else 
{
label_1671:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_1693 = __retres1;
}
tmp = __return_1693;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_1709 = __retres1;
}
tmp___0 = __return_1709;
t1_st = 0;
}
{
if (T1_E == 1)
{
T1_E = 2;
goto label_1723;
}
else 
{
label_1723:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_1728;
}
else 
{
label_1728:; 
}
label_1733:; 
kernel_st = 1;
{
int tmp ;
int tmp_ndt_2;
int tmp_ndt_1;
label_1744:; 
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_1762;
}
else 
{
goto label_1753;
}
}
else 
{
label_1753:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_1762;
}
else 
{
goto label_1757;
}
}
else 
{
label_1757:; 
__retres1 = 0;
label_1762:; 
 __return_1767 = __retres1;
}
tmp = __return_1767;
if (tmp < 1)
{
if (tmp > -1)
{
if (m_st < 1)
{
if (m_st > -1)
{
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 < 1)
{
if (tmp_ndt_1 > -1)
{
m_st = 1;
{
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_1819 = __retres1;
}
tmp = __return_1819;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_1835 = __retres1;
}
tmp___0 = __return_1835;
t1_st = 0;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_1853:; 
label_1854:; 
label_1855:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 < 1)
{
if (tmp_ndt_2 > -1)
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
label_1908:; 
label_1909:; 
label_1912:; 
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_1933;
}
else 
{
goto label_1924;
}
}
else 
{
label_1924:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_1933;
}
else 
{
goto label_1928;
}
}
else 
{
label_1928:; 
__retres1 = 0;
label_1933:; 
 __return_1938 = __retres1;
}
tmp = __return_1938;
if (tmp < 1)
{
if (tmp > -1)
{
if (m_st < 1)
{
if (m_st > -1)
{
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 < 1)
{
if (tmp_ndt_1 > -1)
{
m_st = 1;
{
m_pc = 1;
m_st = 2;
}
label_1976:; 
goto label_1960;
}
else 
{
goto label_1957;
}
}
else 
{
label_1957:; 
label_1960:; 
goto label_1953;
}
}
else 
{
goto label_1950;
}
}
else 
{
label_1950:; 
label_1953:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 < 1)
{
if (tmp_ndt_2 > -1)
{
t1_st = 1;
{
{
__VERIFIER_error();
}
t1_pc = 1;
t1_st = 2;
}
goto label_1908;
}
else 
{
goto label_1987;
}
}
else 
{
label_1987:; 
goto label_1909;
}
}
else 
{
goto label_1980;
}
}
else 
{
label_1980:; 
goto label_1912;
}
}
}
else 
{
goto label_1941;
}
}
else 
{
label_1941:; 
}
label_1947:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (T1_E == 0)
{
T1_E = 1;
goto label_2280;
}
else 
{
label_2280:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_2285;
}
else 
{
label_2285:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2438 = __retres1;
}
tmp = __return_2438;
m_st = 0;
{
int __retres1 ;
if (E_1 < 2)
{
if (E_1 > 0)
{
__retres1 = 1;
 __return_2461 = __retres1;
}
else 
{
goto label_2452;
}
tmp___0 = __return_2461;
}
else 
{
label_2452:; 
__retres1 = 0;
 __return_2462 = __retres1;
}
label_2473:; 
{
if (T1_E == 1)
{
T1_E = 2;
goto label_2538;
}
else 
{
label_2538:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_2543;
}
else 
{
label_2543:; 
}
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_2639;
}
else 
{
goto label_2630;
}
}
else 
{
label_2630:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_2639;
}
else 
{
goto label_2634;
}
}
else 
{
label_2634:; 
__retres1 = 0;
label_2639:; 
 __return_2644 = __retres1;
}
tmp = __return_2644;
if (tmp < 1)
{
if (tmp > -1)
{
kernel_st = 4;
{
M_E = 1;
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 1;
 __return_2771 = __retres1;
}
tmp = __return_2771;
{
int __retres1 ;
if (E_1 < 2)
{
if (E_1 > 0)
{
__retres1 = 1;
 __return_2792 = __retres1;
}
else 
{
goto label_2783;
}
tmp___0 = __return_2792;
}
else 
{
label_2783:; 
__retres1 = 0;
 __return_2793 = __retres1;
}
label_2804:; 
{
M_E = 2;
if (T1_E == 1)
{
T1_E = 2;
goto label_2947;
}
else 
{
label_2947:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_2952;
}
else 
{
label_2952:; 
}
goto label_2686;
}
}
tmp___0 = __return_2793;
t1_st = 0;
}
goto label_2804;
}
}
else 
{
goto label_2671;
}
}
else 
{
label_2671:; 
label_2686:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_3042;
}
else 
{
goto label_3033;
}
}
else 
{
label_3033:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_3042;
}
else 
{
goto label_3037;
}
}
else 
{
label_3037:; 
__retres1 = 0;
label_3042:; 
 __return_3047 = __retres1;
}
tmp = __return_3047;
if (tmp < 1)
{
if (tmp > -1)
{
__retres2 = 0;
goto label_3055;
}
else 
{
goto label_3050;
}
}
else 
{
label_3050:; 
__retres2 = 1;
label_3055:; 
 __return_3058 = __retres2;
}
tmp___0 = __return_3058;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
}
else 
{
goto label_3145;
}
__retres1 = 0;
 __return_3769 = __retres1;
return 1;
}
else 
{
label_3145:; 
kernel_st = 1;
{
int tmp ;
int tmp_ndt_2;
int tmp_ndt_1;
label_3646:; 
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_3664;
}
else 
{
goto label_3655;
}
}
else 
{
label_3655:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_3664;
}
else 
{
goto label_3659;
}
}
else 
{
label_3659:; 
__retres1 = 0;
label_3664:; 
 __return_3669 = __retres1;
}
tmp = __return_3669;
if (tmp < 1)
{
if (tmp > -1)
{
if (m_st < 1)
{
if (m_st > -1)
{
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 < 1)
{
if (tmp_ndt_1 > -1)
{
m_st = 1;
{
m_pc = 1;
m_st = 2;
}
goto label_3691;
}
else 
{
goto label_3688;
}
}
else 
{
label_3688:; 
label_3691:; 
goto label_3684;
}
}
else 
{
goto label_3681;
}
}
else 
{
label_3681:; 
label_3684:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 < 1)
{
if (tmp_ndt_2 > -1)
{
t1_st = 1;
{
{
__VERIFIER_error();
}
t1_pc = 1;
t1_st = 2;
}
goto label_3721;
}
else 
{
goto label_3718;
}
}
else 
{
label_3718:; 
label_3721:; 
goto label_3714;
}
}
else 
{
goto label_3711;
}
}
else 
{
label_3711:; 
label_3714:; 
goto label_3646;
}
}
}
else 
{
goto label_3672;
}
}
else 
{
label_3672:; 
}
goto label_1947;
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
tmp___0 = __return_2462;
t1_st = 0;
}
goto label_2473;
}
}
}
}
}
}
else 
{
goto label_1872;
}
}
else 
{
label_1872:; 
goto label_1866;
}
}
else 
{
goto label_1858;
}
}
else 
{
label_1858:; 
label_1866:; 
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_2189;
}
else 
{
goto label_2180;
}
}
else 
{
label_2180:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_2189;
}
else 
{
goto label_2184;
}
}
else 
{
label_2184:; 
__retres1 = 0;
label_2189:; 
 __return_2194 = __retres1;
}
tmp = __return_2194;
if (tmp < 1)
{
if (tmp > -1)
{
if (m_st < 1)
{
if (m_st > -1)
{
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 < 1)
{
if (tmp_ndt_1 > -1)
{
m_st = 1;
{
m_pc = 1;
m_st = 2;
}
goto label_1853;
}
else 
{
goto label_2213;
}
}
else 
{
label_2213:; 
goto label_1854;
}
}
else 
{
goto label_2206;
}
}
else 
{
label_2206:; 
goto label_1855;
}
}
else 
{
goto label_2197;
}
}
else 
{
label_2197:; 
}
label_2203:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (T1_E == 0)
{
T1_E = 1;
goto label_2312;
}
else 
{
label_2312:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_2317;
}
else 
{
label_2317:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2341 = __retres1;
}
tmp = __return_2341;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2357 = __retres1;
}
tmp___0 = __return_2357;
t1_st = 0;
}
{
if (T1_E == 1)
{
T1_E = 2;
goto label_2570;
}
else 
{
label_2570:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_2575;
}
else 
{
label_2575:; 
}
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_2595;
}
else 
{
goto label_2586;
}
}
else 
{
label_2586:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_2595;
}
else 
{
goto label_2590;
}
}
else 
{
label_2590:; 
__retres1 = 0;
label_2595:; 
 __return_2600 = __retres1;
}
tmp = __return_2600;
if (tmp < 1)
{
if (tmp > -1)
{
kernel_st = 4;
{
M_E = 1;
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 1;
 __return_2878 = __retres1;
}
tmp = __return_2878;
{
int __retres1 ;
__retres1 = 0;
 __return_2892 = __retres1;
}
tmp___0 = __return_2892;
t1_st = 0;
}
{
M_E = 2;
if (T1_E == 1)
{
T1_E = 2;
goto label_2909;
}
else 
{
label_2909:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_2914;
}
else 
{
label_2914:; 
}
goto label_2684;
}
}
}
else 
{
goto label_2675;
}
}
else 
{
label_2675:; 
label_2684:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_3122;
}
else 
{
goto label_3113;
}
}
else 
{
label_3113:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_3122;
}
else 
{
goto label_3117;
}
}
else 
{
label_3117:; 
__retres1 = 0;
label_3122:; 
 __return_3127 = __retres1;
}
tmp = __return_3127;
if (tmp < 1)
{
if (tmp > -1)
{
__retres2 = 0;
goto label_3135;
}
else 
{
goto label_3130;
}
}
else 
{
label_3130:; 
__retres2 = 1;
label_3135:; 
 __return_3138 = __retres2;
}
tmp___0 = __return_3138;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
}
else 
{
goto label_3141;
}
__retres1 = 0;
 __return_3771 = __retres1;
return 1;
}
else 
{
label_3141:; 
kernel_st = 1;
{
int tmp ;
int tmp_ndt_2;
int tmp_ndt_1;
label_3171:; 
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_3189;
}
else 
{
goto label_3180;
}
}
else 
{
label_3180:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_3189;
}
else 
{
goto label_3184;
}
}
else 
{
label_3184:; 
__retres1 = 0;
label_3189:; 
 __return_3194 = __retres1;
}
tmp = __return_3194;
if (tmp < 1)
{
if (tmp > -1)
{
if (m_st < 1)
{
if (m_st > -1)
{
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 < 1)
{
if (tmp_ndt_1 > -1)
{
m_st = 1;
{
m_pc = 1;
m_st = 2;
}
goto label_3216;
}
else 
{
goto label_3213;
}
}
else 
{
label_3213:; 
label_3216:; 
goto label_3209;
}
}
else 
{
goto label_3206;
}
}
else 
{
label_3206:; 
label_3209:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 < 1)
{
if (tmp_ndt_2 > -1)
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
label_3260:; 
label_3261:; 
label_3262:; 
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_3282;
}
else 
{
goto label_3273;
}
}
else 
{
label_3273:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_3282;
}
else 
{
goto label_3277;
}
}
else 
{
label_3277:; 
__retres1 = 0;
label_3282:; 
 __return_3287 = __retres1;
}
tmp = __return_3287;
if (tmp < 1)
{
if (tmp > -1)
{
if (m_st < 1)
{
if (m_st > -1)
{
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 < 1)
{
if (tmp_ndt_1 > -1)
{
m_st = 1;
{
m_pc = 1;
m_st = 2;
}
goto label_3309;
}
else 
{
goto label_3306;
}
}
else 
{
label_3306:; 
label_3309:; 
goto label_3302;
}
}
else 
{
goto label_3299;
}
}
else 
{
label_3299:; 
label_3302:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 < 1)
{
if (tmp_ndt_2 > -1)
{
t1_st = 1;
{
{
__VERIFIER_error();
}
t1_pc = 1;
t1_st = 2;
}
goto label_3260;
}
else 
{
goto label_3336;
}
}
else 
{
label_3336:; 
goto label_3261;
}
}
else 
{
goto label_3329;
}
}
else 
{
label_3329:; 
goto label_3262;
}
}
}
else 
{
goto label_3290;
}
}
else 
{
label_3290:; 
}
goto label_1947;
}
}
}
else 
{
goto label_3243;
}
}
else 
{
label_3243:; 
goto label_3239;
}
}
else 
{
goto label_3236;
}
}
else 
{
label_3236:; 
label_3239:; 
goto label_3171;
}
}
}
else 
{
goto label_3197;
}
}
else 
{
label_3197:; 
}
goto label_2203;
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
else 
{
goto label_1786;
}
}
else 
{
label_1786:; 
goto label_1782;
}
}
else 
{
goto label_1779;
}
}
else 
{
label_1779:; 
label_1782:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 < 1)
{
if (tmp_ndt_2 > -1)
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
label_1895:; 
label_1910:; 
label_1911:; 
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_2031;
}
else 
{
goto label_2022;
}
}
else 
{
label_2022:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_2031;
}
else 
{
goto label_2026;
}
}
else 
{
label_2026:; 
__retres1 = 0;
label_2031:; 
 __return_2036 = __retres1;
}
tmp = __return_2036;
if (tmp < 1)
{
if (tmp > -1)
{
if (m_st < 1)
{
if (m_st > -1)
{
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 < 1)
{
if (tmp_ndt_1 > -1)
{
m_st = 1;
{
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2088 = __retres1;
}
tmp = __return_2088;
m_st = 0;
{
int __retres1 ;
if (E_1 < 2)
{
if (E_1 > 0)
{
__retres1 = 1;
 __return_2111 = __retres1;
}
else 
{
goto label_2102;
}
tmp___0 = __return_2111;
}
else 
{
label_2102:; 
__retres1 = 0;
 __return_2112 = __retres1;
}
label_2123:; 
tmp___0 = __return_2112;
t1_st = 0;
}
E_1 = 2;
m_pc = 1;
m_st = 2;
goto label_2123;
}
goto label_1976;
}
}
}
else 
{
goto label_2055;
}
}
else 
{
label_2055:; 
goto label_2051;
}
}
else 
{
goto label_2048;
}
}
else 
{
label_2048:; 
label_2051:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 < 1)
{
if (tmp_ndt_2 > -1)
{
t1_st = 1;
{
{
__VERIFIER_error();
}
t1_pc = 1;
t1_st = 2;
}
goto label_1895;
}
else 
{
goto label_2145;
}
}
else 
{
label_2145:; 
goto label_1910;
}
}
else 
{
goto label_2138;
}
}
else 
{
label_2138:; 
goto label_1911;
}
}
}
else 
{
goto label_2039;
}
}
else 
{
label_2039:; 
}
label_2045:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (T1_E == 0)
{
T1_E = 1;
goto label_2296;
}
else 
{
label_2296:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_2301;
}
else 
{
label_2301:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2382 = __retres1;
}
tmp = __return_2382;
m_st = 0;
{
int __retres1 ;
if (E_1 < 2)
{
if (E_1 > 0)
{
__retres1 = 1;
 __return_2405 = __retres1;
}
else 
{
goto label_2396;
}
tmp___0 = __return_2405;
}
else 
{
label_2396:; 
__retres1 = 0;
 __return_2406 = __retres1;
}
label_2417:; 
{
if (T1_E == 1)
{
T1_E = 2;
goto label_2554;
}
else 
{
label_2554:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_2559;
}
else 
{
label_2559:; 
}
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_2617;
}
else 
{
goto label_2608;
}
}
else 
{
label_2608:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_2617;
}
else 
{
goto label_2612;
}
}
else 
{
label_2612:; 
__retres1 = 0;
label_2617:; 
 __return_2622 = __retres1;
}
tmp = __return_2622;
if (tmp < 1)
{
if (tmp > -1)
{
kernel_st = 4;
{
M_E = 1;
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2823 = __retres1;
}
tmp = __return_2823;
m_st = 0;
{
int __retres1 ;
if (E_1 < 2)
{
if (E_1 > 0)
{
__retres1 = 1;
 __return_2846 = __retres1;
}
else 
{
goto label_2837;
}
tmp___0 = __return_2846;
}
else 
{
label_2837:; 
__retres1 = 0;
 __return_2847 = __retres1;
}
label_2858:; 
{
M_E = 2;
if (T1_E == 1)
{
T1_E = 2;
goto label_2928;
}
else 
{
label_2928:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_2933;
}
else 
{
label_2933:; 
}
goto label_2685;
}
}
tmp___0 = __return_2847;
t1_st = 0;
}
goto label_2858;
}
}
else 
{
goto label_2673;
}
}
else 
{
label_2673:; 
label_2685:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_3082;
}
else 
{
goto label_3073;
}
}
else 
{
label_3073:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_3082;
}
else 
{
goto label_3077;
}
}
else 
{
label_3077:; 
__retres1 = 0;
label_3082:; 
 __return_3087 = __retres1;
}
tmp = __return_3087;
if (tmp < 1)
{
if (tmp > -1)
{
__retres2 = 0;
goto label_3095;
}
else 
{
goto label_3090;
}
}
else 
{
label_3090:; 
__retres2 = 1;
label_3095:; 
 __return_3098 = __retres2;
}
tmp___0 = __return_3098;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
}
else 
{
goto label_3143;
}
__retres1 = 0;
 __return_3770 = __retres1;
return 1;
}
else 
{
label_3143:; 
kernel_st = 1;
{
int tmp ;
int tmp_ndt_2;
int tmp_ndt_1;
label_3374:; 
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_3392;
}
else 
{
goto label_3383;
}
}
else 
{
label_3383:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_3392;
}
else 
{
goto label_3387;
}
}
else 
{
label_3387:; 
__retres1 = 0;
label_3392:; 
 __return_3397 = __retres1;
}
tmp = __return_3397;
if (tmp < 1)
{
if (tmp > -1)
{
if (m_st < 1)
{
if (m_st > -1)
{
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 < 1)
{
if (tmp_ndt_1 > -1)
{
m_st = 1;
{
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_3449 = __retres1;
}
tmp = __return_3449;
m_st = 0;
{
int __retres1 ;
if (E_1 < 2)
{
if (E_1 > 0)
{
__retres1 = 1;
 __return_3472 = __retres1;
}
else 
{
goto label_3463;
}
tmp___0 = __return_3472;
}
else 
{
label_3463:; 
__retres1 = 0;
 __return_3473 = __retres1;
}
label_3484:; 
tmp___0 = __return_3473;
t1_st = 0;
}
E_1 = 2;
m_pc = 1;
m_st = 2;
goto label_3484;
}
label_3496:; 
label_3497:; 
label_3498:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 < 1)
{
if (tmp_ndt_2 > -1)
{
t1_st = 1;
{
{
__VERIFIER_error();
}
t1_pc = 1;
t1_st = 2;
}
goto label_3523;
}
else 
{
goto label_3515;
}
}
else 
{
label_3515:; 
label_3523:; 
goto label_3509;
}
}
else 
{
goto label_3501;
}
}
else 
{
label_3501:; 
label_3509:; 
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_3590;
}
else 
{
goto label_3581;
}
}
else 
{
label_3581:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_3590;
}
else 
{
goto label_3585;
}
}
else 
{
label_3585:; 
__retres1 = 0;
label_3590:; 
 __return_3595 = __retres1;
}
tmp = __return_3595;
if (tmp < 1)
{
if (tmp > -1)
{
if (m_st < 1)
{
if (m_st > -1)
{
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 < 1)
{
if (tmp_ndt_1 > -1)
{
m_st = 1;
{
m_pc = 1;
m_st = 2;
}
goto label_3496;
}
else 
{
goto label_3614;
}
}
else 
{
label_3614:; 
goto label_3497;
}
}
else 
{
goto label_3607;
}
}
else 
{
label_3607:; 
goto label_3498;
}
}
else 
{
goto label_3598;
}
}
else 
{
label_3598:; 
}
goto label_1947;
}
}
}
}
}
}
else 
{
goto label_3416;
}
}
else 
{
label_3416:; 
goto label_3412;
}
}
else 
{
goto label_3409;
}
}
else 
{
label_3409:; 
label_3412:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 < 1)
{
if (tmp_ndt_2 > -1)
{
t1_st = 1;
{
{
__VERIFIER_error();
}
t1_pc = 1;
t1_st = 2;
}
goto label_3522;
}
else 
{
goto label_3517;
}
}
else 
{
label_3517:; 
label_3522:; 
goto label_3508;
}
}
else 
{
goto label_3503;
}
}
else 
{
label_3503:; 
label_3508:; 
goto label_3374;
}
}
}
else 
{
goto label_3400;
}
}
else 
{
label_3400:; 
}
goto label_2045;
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
tmp___0 = __return_2406;
t1_st = 0;
}
goto label_2417;
}
}
}
}
}
}
else 
{
goto label_1874;
}
}
else 
{
label_1874:; 
goto label_1865;
}
}
else 
{
goto label_1860;
}
}
else 
{
label_1860:; 
label_1865:; 
goto label_1744;
}
}
}
else 
{
goto label_1770;
}
}
else 
{
label_1770:; 
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (T1_E == 0)
{
T1_E = 1;
goto label_2264;
}
else 
{
label_2264:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_2269;
}
else 
{
label_2269:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2492 = __retres1;
}
tmp = __return_2492;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2508 = __retres1;
}
tmp___0 = __return_2508;
t1_st = 0;
}
{
if (T1_E == 1)
{
T1_E = 2;
goto label_2522;
}
else 
{
label_2522:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_2527;
}
else 
{
label_2527:; 
}
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_2661;
}
else 
{
goto label_2652;
}
}
else 
{
label_2652:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_2661;
}
else 
{
goto label_2656;
}
}
else 
{
label_2656:; 
__retres1 = 0;
label_2661:; 
 __return_2666 = __retres1;
}
tmp = __return_2666;
if (tmp < 1)
{
if (tmp > -1)
{
kernel_st = 4;
{
M_E = 1;
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2729 = __retres1;
}
tmp = __return_2729;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2745 = __retres1;
}
tmp___0 = __return_2745;
t1_st = 0;
}
{
M_E = 2;
if (T1_E == 1)
{
T1_E = 2;
goto label_2966;
}
else 
{
label_2966:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_2971;
}
else 
{
label_2971:; 
}
goto label_2687;
}
}
}
else 
{
goto label_2669;
}
}
else 
{
label_2669:; 
label_2687:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_3002;
}
else 
{
goto label_2993;
}
}
else 
{
label_2993:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_3002;
}
else 
{
goto label_2997;
}
}
else 
{
label_2997:; 
__retres1 = 0;
label_3002:; 
 __return_3007 = __retres1;
}
tmp = __return_3007;
if (tmp < 1)
{
if (tmp > -1)
{
__retres2 = 0;
goto label_3015;
}
else 
{
goto label_3010;
}
}
else 
{
label_3010:; 
__retres2 = 1;
label_3015:; 
 __return_3018 = __retres2;
}
tmp___0 = __return_3018;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
}
else 
{
goto label_3147;
}
__retres1 = 0;
 __return_3768 = __retres1;
return 1;
}
else 
{
label_3147:; 
goto label_1733;
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
}
