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
int __return_358;
int __return_387;
int __return_448;
int __return_516;
int __return_545;
int __return_659;
int __return_688;
int __return_735;
int __return_774;
int __return_803;
int __return_861;
int __return_872;
int __return_885;
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
if (m_i < 2)
{
if (m_i > 0)
{
m_st = 0;
goto label_307;
}
else 
{
goto label_303;
}
}
else 
{
label_303:; 
m_st = 2;
label_307:; 
if (t1_i < 2)
{
if (t1_i > 0)
{
t1_st = 0;
goto label_315;
}
else 
{
goto label_311;
}
}
else 
{
label_311:; 
t1_st = 2;
label_315:; 
}
{
if (T1_E == 0)
{
T1_E = 1;
goto label_326;
}
else 
{
label_326:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_331;
}
else 
{
label_331:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
if (m_pc < 2)
{
if (m_pc > 0)
{
goto label_352;
}
else 
{
goto label_349;
}
}
else 
{
label_349:; 
label_352:; 
__retres1 = 0;
 __return_358 = __retres1;
}
tmp = __return_358;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_364;
}
else 
{
goto label_361;
}
}
else 
{
label_361:; 
label_364:; 
{
int __retres1 ;
if (t1_pc < 2)
{
if (t1_pc > 0)
{
if (E_1 < 2)
{
if (E_1 > 0)
{
__retres1 = 1;
goto label_384;
}
else 
{
goto label_378;
}
}
else 
{
label_378:; 
goto label_376;
}
}
else 
{
goto label_373;
}
}
else 
{
label_373:; 
label_376:; 
__retres1 = 0;
label_384:; 
 __return_387 = __retres1;
}
tmp___0 = __return_387;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_393;
}
else 
{
goto label_390;
}
}
else 
{
label_390:; 
label_393:; 
}
{
if (T1_E == 1)
{
T1_E = 2;
goto label_404;
}
else 
{
label_404:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_409;
}
else 
{
label_409:; 
}
label_414:; 
kernel_st = 1;
{
int tmp ;
int tmp_ndt_2;
int tmp_ndt_1;
label_425:; 
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_443;
}
else 
{
goto label_434;
}
}
else 
{
label_434:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_443;
}
else 
{
goto label_438;
}
}
else 
{
label_438:; 
__retres1 = 0;
label_443:; 
 __return_448 = __retres1;
}
tmp = __return_448;
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
if (m_pc < 1)
{
if (m_pc > -1)
{
goto label_483;
}
else 
{
goto label_475;
}
}
else 
{
label_475:; 
if (m_pc < 2)
{
if (m_pc > 0)
{
label_486:; 
m_pc = 1;
m_st = 2;
}
else 
{
goto label_479;
}
goto label_470;
}
else 
{
label_479:; 
label_483:; 
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
if (m_pc < 2)
{
if (m_pc > 0)
{
goto label_510;
}
else 
{
goto label_507;
}
}
else 
{
label_507:; 
label_510:; 
__retres1 = 0;
 __return_516 = __retres1;
}
tmp = __return_516;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_522;
}
else 
{
goto label_519;
}
}
else 
{
label_519:; 
label_522:; 
{
int __retres1 ;
if (t1_pc < 2)
{
if (t1_pc > 0)
{
if (E_1 < 2)
{
if (E_1 > 0)
{
__retres1 = 1;
goto label_542;
}
else 
{
goto label_536;
}
}
else 
{
label_536:; 
goto label_534;
}
}
else 
{
goto label_531;
}
}
else 
{
label_531:; 
label_534:; 
__retres1 = 0;
label_542:; 
 __return_545 = __retres1;
}
tmp___0 = __return_545;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_551;
}
else 
{
goto label_548;
}
}
else 
{
label_548:; 
label_551:; 
}
}
E_1 = 2;
goto label_486;
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
goto label_467;
}
}
else 
{
label_467:; 
label_470:; 
goto label_463;
}
}
else 
{
goto label_460;
}
}
else 
{
label_460:; 
label_463:; 
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
if (t1_pc < 1)
{
if (t1_pc > -1)
{
goto label_593;
}
else 
{
goto label_585;
}
}
else 
{
label_585:; 
if (t1_pc < 2)
{
if (t1_pc > 0)
{
{
__VERIFIER_error();
}
label_602:; 
t1_pc = 1;
t1_st = 2;
}
else 
{
goto label_589;
}
goto label_580;
}
else 
{
label_589:; 
label_593:; 
goto label_602;
}
}
}
}
else 
{
goto label_577;
}
}
else 
{
label_577:; 
label_580:; 
goto label_573;
}
}
else 
{
goto label_570;
}
}
else 
{
label_570:; 
label_573:; 
goto label_425;
}
}
}
else 
{
goto label_451;
}
}
else 
{
label_451:; 
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (T1_E == 0)
{
T1_E = 1;
goto label_627;
}
else 
{
label_627:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_632;
}
else 
{
label_632:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
if (m_pc < 2)
{
if (m_pc > 0)
{
goto label_653;
}
else 
{
goto label_650;
}
}
else 
{
label_650:; 
label_653:; 
__retres1 = 0;
 __return_659 = __retres1;
}
tmp = __return_659;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_665;
}
else 
{
goto label_662;
}
}
else 
{
label_662:; 
label_665:; 
{
int __retres1 ;
if (t1_pc < 2)
{
if (t1_pc > 0)
{
if (E_1 < 2)
{
if (E_1 > 0)
{
__retres1 = 1;
goto label_685;
}
else 
{
goto label_679;
}
}
else 
{
label_679:; 
goto label_677;
}
}
else 
{
goto label_674;
}
}
else 
{
label_674:; 
label_677:; 
__retres1 = 0;
label_685:; 
 __return_688 = __retres1;
}
tmp___0 = __return_688;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_694;
}
else 
{
goto label_691;
}
}
else 
{
label_691:; 
label_694:; 
}
{
if (T1_E == 1)
{
T1_E = 2;
goto label_705;
}
else 
{
label_705:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_710;
}
else 
{
label_710:; 
}
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_730;
}
else 
{
goto label_721;
}
}
else 
{
label_721:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_730;
}
else 
{
goto label_725;
}
}
else 
{
label_725:; 
__retres1 = 0;
label_730:; 
 __return_735 = __retres1;
}
tmp = __return_735;
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
if (m_pc < 2)
{
if (m_pc > 0)
{
if (m_pc > 0)
{
__retres1 = 1;
goto label_771;
}
else 
{
goto label_764;
}
}
else 
{
goto label_761;
}
}
else 
{
label_761:; 
label_764:; 
__retres1 = 0;
label_771:; 
 __return_774 = __retres1;
}
tmp = __return_774;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_780;
}
else 
{
goto label_777;
}
}
else 
{
label_777:; 
label_780:; 
{
int __retres1 ;
if (t1_pc < 2)
{
if (t1_pc > 0)
{
if (E_1 < 2)
{
if (E_1 > 0)
{
__retres1 = 1;
goto label_800;
}
else 
{
goto label_794;
}
}
else 
{
label_794:; 
goto label_792;
}
}
else 
{
goto label_789;
}
}
else 
{
label_789:; 
label_792:; 
__retres1 = 0;
label_800:; 
 __return_803 = __retres1;
}
tmp___0 = __return_803;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_809;
}
else 
{
goto label_806;
}
}
else 
{
label_806:; 
label_809:; 
}
{
M_E = 2;
if (T1_E == 1)
{
T1_E = 2;
goto label_823;
}
else 
{
label_823:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_828;
}
else 
{
label_828:; 
}
goto label_741;
}
}
}
}
}
}
}
else 
{
goto label_738;
}
}
else 
{
label_738:; 
label_741:; 
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
goto label_856;
}
else 
{
goto label_847;
}
}
else 
{
label_847:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_856;
}
else 
{
goto label_851;
}
}
else 
{
label_851:; 
__retres1 = 0;
label_856:; 
 __return_861 = __retres1;
}
tmp = __return_861;
if (tmp < 1)
{
if (tmp > -1)
{
__retres2 = 0;
goto label_869;
}
else 
{
goto label_864;
}
}
else 
{
label_864:; 
__retres2 = 1;
label_869:; 
 __return_872 = __retres2;
}
tmp___0 = __return_872;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
}
else 
{
goto label_875;
}
__retres1 = 0;
 __return_885 = __retres1;
return 1;
}
else 
{
label_875:; 
goto label_414;
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
