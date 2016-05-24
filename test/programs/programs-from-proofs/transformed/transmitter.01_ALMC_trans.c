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
int __return_370;
int __return_399;
int __return_460;
int __return_528;
int __return_557;
int __return_671;
int __return_700;
int __return_747;
int __return_786;
int __return_815;
int __return_873;
int __return_884;
int __return_897;
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
goto label_319;
}
else 
{
goto label_315;
}
}
else 
{
label_315:; 
m_st = 2;
label_319:; 
if (t1_i < 2)
{
if (t1_i > 0)
{
t1_st = 0;
goto label_327;
}
else 
{
goto label_323;
}
}
else 
{
label_323:; 
t1_st = 2;
label_327:; 
}
{
if (T1_E == 0)
{
T1_E = 1;
goto label_338;
}
else 
{
label_338:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_343;
}
else 
{
label_343:; 
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
__retres1 = 0;
 __return_370 = __retres1;
}
tmp = __return_370;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_376;
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
goto label_396;
}
else 
{
goto label_390;
}
}
else 
{
label_390:; 
goto label_388;
}
}
else 
{
goto label_385;
}
}
else 
{
label_385:; 
label_388:; 
__retres1 = 0;
label_396:; 
 __return_399 = __retres1;
}
tmp___0 = __return_399;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_405;
}
else 
{
goto label_402;
}
}
else 
{
label_402:; 
label_405:; 
}
{
if (T1_E == 1)
{
T1_E = 2;
goto label_416;
}
else 
{
label_416:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_421;
}
else 
{
label_421:; 
}
label_426:; 
kernel_st = 1;
{
int tmp ;
int tmp_ndt_2;
int tmp_ndt_1;
label_437:; 
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_455;
}
else 
{
goto label_446;
}
}
else 
{
label_446:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_455;
}
else 
{
goto label_450;
}
}
else 
{
label_450:; 
__retres1 = 0;
label_455:; 
 __return_460 = __retres1;
}
tmp = __return_460;
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
goto label_495;
}
else 
{
goto label_487;
}
}
else 
{
label_487:; 
if (m_pc < 2)
{
if (m_pc > 0)
{
label_498:; 
m_pc = 1;
m_st = 2;
}
else 
{
goto label_491;
}
goto label_482;
}
else 
{
label_491:; 
label_495:; 
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
__retres1 = 0;
 __return_528 = __retres1;
}
tmp = __return_528;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_534;
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
goto label_554;
}
else 
{
goto label_548;
}
}
else 
{
label_548:; 
goto label_546;
}
}
else 
{
goto label_543;
}
}
else 
{
label_543:; 
label_546:; 
__retres1 = 0;
label_554:; 
 __return_557 = __retres1;
}
tmp___0 = __return_557;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_563;
}
else 
{
goto label_560;
}
}
else 
{
label_560:; 
label_563:; 
}
}
E_1 = 2;
goto label_498;
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
goto label_479;
}
}
else 
{
label_479:; 
label_482:; 
goto label_475;
}
}
else 
{
goto label_472;
}
}
else 
{
label_472:; 
label_475:; 
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
goto label_605;
}
else 
{
goto label_597;
}
}
else 
{
label_597:; 
if (t1_pc < 2)
{
if (t1_pc > 0)
{
{
__VERIFIER_error();
}
label_614:; 
t1_pc = 1;
t1_st = 2;
}
else 
{
goto label_601;
}
goto label_592;
}
else 
{
label_601:; 
label_605:; 
goto label_614;
}
}
}
}
else 
{
goto label_589;
}
}
else 
{
label_589:; 
label_592:; 
goto label_585;
}
}
else 
{
goto label_582;
}
}
else 
{
label_582:; 
label_585:; 
goto label_437;
}
}
}
else 
{
goto label_463;
}
}
else 
{
label_463:; 
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (T1_E == 0)
{
T1_E = 1;
goto label_639;
}
else 
{
label_639:; 
if (E_1 == 0)
{
E_1 = 1;
goto label_644;
}
else 
{
label_644:; 
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
__retres1 = 0;
 __return_671 = __retres1;
}
tmp = __return_671;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_677;
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
goto label_697;
}
else 
{
goto label_691;
}
}
else 
{
label_691:; 
goto label_689;
}
}
else 
{
goto label_686;
}
}
else 
{
label_686:; 
label_689:; 
__retres1 = 0;
label_697:; 
 __return_700 = __retres1;
}
tmp___0 = __return_700;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_706;
}
else 
{
goto label_703;
}
}
else 
{
label_703:; 
label_706:; 
}
{
if (T1_E == 1)
{
T1_E = 2;
goto label_717;
}
else 
{
label_717:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_722;
}
else 
{
label_722:; 
}
{
int __retres1 ;
if (m_st < 1)
{
if (m_st > -1)
{
__retres1 = 1;
goto label_742;
}
else 
{
goto label_733;
}
}
else 
{
label_733:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_742;
}
else 
{
goto label_737;
}
}
else 
{
label_737:; 
__retres1 = 0;
label_742:; 
 __return_747 = __retres1;
}
tmp = __return_747;
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
goto label_783;
}
else 
{
goto label_776;
}
}
else 
{
goto label_773;
}
}
else 
{
label_773:; 
label_776:; 
__retres1 = 0;
label_783:; 
 __return_786 = __retres1;
}
tmp = __return_786;
if (tmp < 1)
{
if (tmp > -1)
{
m_st = 0;
goto label_792;
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
goto label_812;
}
else 
{
goto label_806;
}
}
else 
{
label_806:; 
goto label_804;
}
}
else 
{
goto label_801;
}
}
else 
{
label_801:; 
label_804:; 
__retres1 = 0;
label_812:; 
 __return_815 = __retres1;
}
tmp___0 = __return_815;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
t1_st = 0;
goto label_821;
}
else 
{
goto label_818;
}
}
else 
{
label_818:; 
label_821:; 
}
{
M_E = 2;
if (T1_E == 1)
{
T1_E = 2;
goto label_835;
}
else 
{
label_835:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_840;
}
else 
{
label_840:; 
}
goto label_753;
}
}
}
}
}
}
}
else 
{
goto label_750;
}
}
else 
{
label_750:; 
label_753:; 
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
goto label_868;
}
else 
{
goto label_859;
}
}
else 
{
label_859:; 
if (t1_st < 1)
{
if (t1_st > -1)
{
__retres1 = 1;
goto label_868;
}
else 
{
goto label_863;
}
}
else 
{
label_863:; 
__retres1 = 0;
label_868:; 
 __return_873 = __retres1;
}
tmp = __return_873;
if (tmp < 1)
{
if (tmp > -1)
{
__retres2 = 0;
goto label_881;
}
else 
{
goto label_876;
}
}
else 
{
label_876:; 
__retres2 = 1;
label_881:; 
 __return_884 = __retres2;
}
tmp___0 = __return_884;
if (tmp___0 < 1)
{
if (tmp___0 > -1)
{
}
else 
{
goto label_887;
}
__retres1 = 0;
 __return_897 = __retres1;
return 1;
}
else 
{
label_887:; 
goto label_426;
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
