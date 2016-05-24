extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void error(void);
int m_pc  =    0;
int t1_pc  =    0;
int t2_pc  =    0;
int t3_pc  =    0;
int m_st  ;
int t1_st  ;
int t2_st  ;
int t3_st  ;
int m_i  ;
int t1_i  ;
int t2_i  ;
int t3_i  ;
int M_E  =    2;
int T1_E  =    2;
int T2_E  =    2;
int T3_E  =    2;
int E_1  =    2;
int E_2  =    2;
int E_3  =    2;
int is_master_triggered(void) ;
int is_transmit1_triggered(void) ;
int is_transmit2_triggered(void) ;
int is_transmit3_triggered(void) ;
void immediate_notify(void) ;
void master(void);
void transmit1(void);
void transmit2(void);
void transmit3(void);
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
int __return_665;
int __return_692;
int __return_719;
int __return_746;
int __return_847;
int __return_1219;
int __return_1246;
int __return_1273;
int __return_1300;
int __return_1061;
int __return_1088;
int __return_1115;
int __return_1142;
int __return_905;
int __return_932;
int __return_959;
int __return_986;
int __return_1455;
int __return_1482;
int __return_1509;
int __return_1536;
int __return_1626;
int __return_1836;
int __return_1848;
int __return_1861;
int __return_1659;
int __return_1686;
int __return_1713;
int __return_1740;
int main()
{
int __retres1 ;
{
m_i = 1;
t1_i = 1;
t2_i = 1;
t3_i = 1;
}
{
int kernel_st ;
int tmp ;
int tmp___0 ;
kernel_st = 0;
{
}
{
if (!(m_i == 1))
{
m_st = 2;
label_565:; 
if (!(t1_i == 1))
{
t1_st = 2;
label_574:; 
if (!(t2_i == 1))
{
t2_st = 2;
label_583:; 
if (!(t3_i == 1))
{
t3_st = 2;
label_592:; 
}
else 
{
t3_st = 0;
goto label_592;
}
{
if (!(T1_E == 0))
{
label_605:; 
if (!(T2_E == 0))
{
label_612:; 
if (!(T3_E == 0))
{
label_619:; 
if (!(E_1 == 0))
{
label_626:; 
if (!(E_2 == 0))
{
label_633:; 
if (!(E_3 == 0))
{
label_640:; 
}
else 
{
E_3 = 1;
goto label_640;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (!(m_pc == 1))
{
label_658:; 
__retres1 = 0;
 __return_665 = __retres1;
}
else 
{
goto label_658;
}
tmp = __return_665;
if (!(tmp == 0))
{
m_st = 0;
label_672:; 
{
int __retres1 ;
if (!(t1_pc == 1))
{
label_680:; 
__retres1 = 0;
label_688:; 
 __return_692 = __retres1;
}
else 
{
if (!(E_1 == 1))
{
goto label_680;
}
else 
{
__retres1 = 1;
goto label_688;
}
}
tmp___0 = __return_692;
if (!(tmp___0 == 0))
{
t1_st = 0;
label_699:; 
{
int __retres1 ;
if (!(t2_pc == 1))
{
label_707:; 
__retres1 = 0;
label_715:; 
 __return_719 = __retres1;
}
else 
{
if (!(E_2 == 1))
{
goto label_707;
}
else 
{
__retres1 = 1;
goto label_715;
}
}
tmp___1 = __return_719;
if (!(tmp___1 == 0))
{
t2_st = 0;
label_726:; 
{
int __retres1 ;
if (!(t3_pc == 1))
{
label_734:; 
__retres1 = 0;
label_742:; 
 __return_746 = __retres1;
}
else 
{
if (!(E_3 == 1))
{
goto label_734;
}
else 
{
__retres1 = 1;
goto label_742;
}
}
tmp___2 = __return_746;
if (!(tmp___2 == 0))
{
t3_st = 0;
label_753:; 
}
else 
{
goto label_753;
}
{
if (!(T1_E == 1))
{
label_764:; 
if (!(T2_E == 1))
{
label_771:; 
if (!(T3_E == 1))
{
label_778:; 
if (!(E_1 == 1))
{
label_785:; 
if (!(E_2 == 1))
{
label_792:; 
if (!(E_3 == 1))
{
label_799:; 
}
else 
{
E_3 = 2;
goto label_799;
}
label_805:; 
kernel_st = 1;
{
int tmp ;
label_813:; 
{
int __retres1 ;
if (!(m_st == 0))
{
if (!(t1_st == 0))
{
if (!(t2_st == 0))
{
if (!(t3_st == 0))
{
__retres1 = 0;
label_834:; 
 __return_847 = __retres1;
}
else 
{
__retres1 = 1;
goto label_834;
}
tmp = __return_847;
if (!(tmp == 0))
{
if (!(m_st == 0))
{
label_856:; 
if (!(t1_st == 0))
{
label_1017:; 
if (!(t2_st == 0))
{
label_1175:; 
if (!(t3_st == 0))
{
label_1333:; 
goto label_813;
}
else 
{
int tmp_ndt_4;
tmp_ndt_4 = __VERIFIER_nondet_int();
if (tmp_ndt_4 == 0)
{
goto label_1372;
}
else 
{
t3_st = 1;
{
if (!(t3_pc == 0))
{
if (!(t3_pc == 1))
{
label_1351:; 
goto label_1360;
}
else 
{
{
__VERIFIER_error();
}
label_1360:; 
t3_pc = 1;
t3_st = 2;
}
label_1372:; 
goto label_1333;
}
else 
{
goto label_1351;
}
}
}
}
}
else 
{
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
goto label_1327;
}
else 
{
t2_st = 1;
{
if (!(t2_pc == 0))
{
if (!(t2_pc == 1))
{
label_1193:; 
goto label_1315;
}
else 
{
E_3 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (!(m_pc == 1))
{
label_1212:; 
__retres1 = 0;
 __return_1219 = __retres1;
}
else 
{
goto label_1212;
}
tmp = __return_1219;
if (!(tmp == 0))
{
m_st = 0;
label_1226:; 
{
int __retres1 ;
if (!(t1_pc == 1))
{
label_1234:; 
__retres1 = 0;
label_1242:; 
 __return_1246 = __retres1;
}
else 
{
if (!(E_1 == 1))
{
goto label_1234;
}
else 
{
__retres1 = 1;
goto label_1242;
}
}
tmp___0 = __return_1246;
if (!(tmp___0 == 0))
{
t1_st = 0;
label_1253:; 
{
int __retres1 ;
if (!(t2_pc == 1))
{
label_1261:; 
__retres1 = 0;
label_1269:; 
 __return_1273 = __retres1;
}
else 
{
if (!(E_2 == 1))
{
goto label_1261;
}
else 
{
__retres1 = 1;
goto label_1269;
}
}
tmp___1 = __return_1273;
if (!(tmp___1 == 0))
{
t2_st = 0;
label_1280:; 
{
int __retres1 ;
if (!(t3_pc == 1))
{
label_1288:; 
__retres1 = 0;
label_1296:; 
 __return_1300 = __retres1;
}
else 
{
if (!(E_3 == 1))
{
goto label_1288;
}
else 
{
__retres1 = 1;
goto label_1296;
}
}
tmp___2 = __return_1300;
if (!(tmp___2 == 0))
{
t3_st = 0;
label_1307:; 
}
else 
{
goto label_1307;
}
}
E_3 = 2;
label_1315:; 
t2_pc = 1;
t2_st = 2;
}
else 
{
goto label_1280;
}
label_1327:; 
goto label_1175;
}
}
else 
{
goto label_1253;
}
}
}
else 
{
goto label_1226;
}
}
}
}
}
}
else 
{
goto label_1193;
}
}
}
}
}
else 
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_1169;
}
else 
{
t1_st = 1;
{
if (!(t1_pc == 0))
{
if (!(t1_pc == 1))
{
label_1035:; 
goto label_1157;
}
else 
{
E_2 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (!(m_pc == 1))
{
label_1054:; 
__retres1 = 0;
 __return_1061 = __retres1;
}
else 
{
goto label_1054;
}
tmp = __return_1061;
if (!(tmp == 0))
{
m_st = 0;
label_1068:; 
{
int __retres1 ;
if (!(t1_pc == 1))
{
label_1076:; 
__retres1 = 0;
label_1084:; 
 __return_1088 = __retres1;
}
else 
{
if (!(E_1 == 1))
{
goto label_1076;
}
else 
{
__retres1 = 1;
goto label_1084;
}
}
tmp___0 = __return_1088;
if (!(tmp___0 == 0))
{
t1_st = 0;
label_1095:; 
{
int __retres1 ;
if (!(t2_pc == 1))
{
label_1103:; 
__retres1 = 0;
label_1111:; 
 __return_1115 = __retres1;
}
else 
{
if (!(E_2 == 1))
{
goto label_1103;
}
else 
{
__retres1 = 1;
goto label_1111;
}
}
tmp___1 = __return_1115;
if (!(tmp___1 == 0))
{
t2_st = 0;
label_1122:; 
{
int __retres1 ;
if (!(t3_pc == 1))
{
label_1130:; 
__retres1 = 0;
label_1138:; 
 __return_1142 = __retres1;
}
else 
{
if (!(E_3 == 1))
{
goto label_1130;
}
else 
{
__retres1 = 1;
goto label_1138;
}
}
tmp___2 = __return_1142;
if (!(tmp___2 == 0))
{
t3_st = 0;
label_1149:; 
}
else 
{
goto label_1149;
}
}
E_2 = 2;
label_1157:; 
t1_pc = 1;
t1_st = 2;
}
else 
{
goto label_1122;
}
label_1169:; 
goto label_1017;
}
}
else 
{
goto label_1095;
}
}
}
else 
{
goto label_1068;
}
}
}
}
}
}
else 
{
goto label_1035;
}
}
}
}
}
else 
{
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_1011;
}
else 
{
m_st = 1;
{
if (!(m_pc == 0))
{
if (!(m_pc == 1))
{
label_874:; 
E_1 = 1;
{
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (!(m_pc == 1))
{
label_898:; 
__retres1 = 0;
 __return_905 = __retres1;
}
else 
{
goto label_898;
}
tmp = __return_905;
if (!(tmp == 0))
{
m_st = 0;
label_912:; 
{
int __retres1 ;
if (!(t1_pc == 1))
{
label_920:; 
__retres1 = 0;
label_928:; 
 __return_932 = __retres1;
}
else 
{
if (!(E_1 == 1))
{
goto label_920;
}
else 
{
__retres1 = 1;
goto label_928;
}
}
tmp___0 = __return_932;
if (!(tmp___0 == 0))
{
t1_st = 0;
label_939:; 
{
int __retres1 ;
if (!(t2_pc == 1))
{
label_947:; 
__retres1 = 0;
label_955:; 
 __return_959 = __retres1;
}
else 
{
if (!(E_2 == 1))
{
goto label_947;
}
else 
{
__retres1 = 1;
goto label_955;
}
}
tmp___1 = __return_959;
if (!(tmp___1 == 0))
{
t2_st = 0;
label_966:; 
{
int __retres1 ;
if (!(t3_pc == 1))
{
label_974:; 
__retres1 = 0;
label_982:; 
 __return_986 = __retres1;
}
else 
{
if (!(E_3 == 1))
{
goto label_974;
}
else 
{
__retres1 = 1;
goto label_982;
}
}
tmp___2 = __return_986;
if (!(tmp___2 == 0))
{
t3_st = 0;
label_993:; 
}
else 
{
goto label_993;
}
}
E_1 = 2;
goto label_877;
}
else 
{
goto label_966;
}
}
}
else 
{
goto label_939;
}
}
}
else 
{
goto label_912;
}
}
}
}
}
else 
{
label_877:; 
m_pc = 1;
m_st = 2;
}
label_1011:; 
goto label_856;
}
else 
{
goto label_874;
}
}
}
}
}
else 
{
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (!(T1_E == 0))
{
label_1395:; 
if (!(T2_E == 0))
{
label_1402:; 
if (!(T3_E == 0))
{
label_1409:; 
if (!(E_1 == 0))
{
label_1416:; 
if (!(E_2 == 0))
{
label_1423:; 
if (!(E_3 == 0))
{
label_1430:; 
}
else 
{
E_3 = 1;
goto label_1430;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (!(m_pc == 1))
{
label_1448:; 
__retres1 = 0;
 __return_1455 = __retres1;
}
else 
{
goto label_1448;
}
tmp = __return_1455;
if (!(tmp == 0))
{
m_st = 0;
label_1462:; 
{
int __retres1 ;
if (!(t1_pc == 1))
{
label_1470:; 
__retres1 = 0;
label_1478:; 
 __return_1482 = __retres1;
}
else 
{
if (!(E_1 == 1))
{
goto label_1470;
}
else 
{
__retres1 = 1;
goto label_1478;
}
}
tmp___0 = __return_1482;
if (!(tmp___0 == 0))
{
t1_st = 0;
label_1489:; 
{
int __retres1 ;
if (!(t2_pc == 1))
{
label_1497:; 
__retres1 = 0;
label_1505:; 
 __return_1509 = __retres1;
}
else 
{
if (!(E_2 == 1))
{
goto label_1497;
}
else 
{
__retres1 = 1;
goto label_1505;
}
}
tmp___1 = __return_1509;
if (!(tmp___1 == 0))
{
t2_st = 0;
label_1516:; 
{
int __retres1 ;
if (!(t3_pc == 1))
{
label_1524:; 
__retres1 = 0;
label_1532:; 
 __return_1536 = __retres1;
}
else 
{
if (!(E_3 == 1))
{
goto label_1524;
}
else 
{
__retres1 = 1;
goto label_1532;
}
}
tmp___2 = __return_1536;
if (!(tmp___2 == 0))
{
t3_st = 0;
label_1543:; 
}
else 
{
goto label_1543;
}
{
if (!(T1_E == 1))
{
label_1554:; 
if (!(T2_E == 1))
{
label_1561:; 
if (!(T3_E == 1))
{
label_1568:; 
if (!(E_1 == 1))
{
label_1575:; 
if (!(E_2 == 1))
{
label_1582:; 
if (!(E_3 == 1))
{
label_1589:; 
}
else 
{
E_3 = 2;
goto label_1589;
}
{
int __retres1 ;
if (!(m_st == 0))
{
if (!(t1_st == 0))
{
if (!(t2_st == 0))
{
if (!(t3_st == 0))
{
__retres1 = 0;
label_1613:; 
 __return_1626 = __retres1;
}
else 
{
__retres1 = 1;
goto label_1613;
}
tmp = __return_1626;
if (!(tmp == 0))
{
label_1631:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
if (!(m_st == 0))
{
if (!(t1_st == 0))
{
if (!(t2_st == 0))
{
if (!(t3_st == 0))
{
__retres1 = 0;
label_1823:; 
 __return_1836 = __retres1;
}
else 
{
__retres1 = 1;
goto label_1823;
}
tmp = __return_1836;
if (!(tmp == 0))
{
__retres2 = 0;
label_1843:; 
 __return_1848 = __retres2;
}
else 
{
__retres2 = 1;
goto label_1843;
}
tmp___0 = __return_1848;
if (!(tmp___0 == 0))
{
}
else 
{
goto label_805;
}
__retres1 = 0;
 __return_1861 = __retres1;
return 1;
}
else 
{
__retres1 = 1;
goto label_1823;
}
}
else 
{
__retres1 = 1;
goto label_1823;
}
}
else 
{
__retres1 = 1;
goto label_1823;
}
}
}
}
else 
{
kernel_st = 4;
{
M_E = 1;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (!(m_pc == 1))
{
__retres1 = 0;
label_1655:; 
 __return_1659 = __retres1;
}
else 
{
__retres1 = 1;
goto label_1655;
}
tmp = __return_1659;
if (!(tmp == 0))
{
m_st = 0;
label_1666:; 
{
int __retres1 ;
if (!(t1_pc == 1))
{
label_1674:; 
__retres1 = 0;
label_1682:; 
 __return_1686 = __retres1;
}
else 
{
if (!(E_1 == 1))
{
goto label_1674;
}
else 
{
__retres1 = 1;
goto label_1682;
}
}
tmp___0 = __return_1686;
if (!(tmp___0 == 0))
{
t1_st = 0;
label_1693:; 
{
int __retres1 ;
if (!(t2_pc == 1))
{
label_1701:; 
__retres1 = 0;
label_1709:; 
 __return_1713 = __retres1;
}
else 
{
if (!(E_2 == 1))
{
goto label_1701;
}
else 
{
__retres1 = 1;
goto label_1709;
}
}
tmp___1 = __return_1713;
if (!(tmp___1 == 0))
{
t2_st = 0;
label_1720:; 
{
int __retres1 ;
if (!(t3_pc == 1))
{
label_1728:; 
__retres1 = 0;
label_1736:; 
 __return_1740 = __retres1;
}
else 
{
if (!(E_3 == 1))
{
goto label_1728;
}
else 
{
__retres1 = 1;
goto label_1736;
}
}
tmp___2 = __return_1740;
if (!(tmp___2 == 0))
{
t3_st = 0;
label_1747:; 
}
else 
{
goto label_1747;
}
{
M_E = 2;
if (!(T1_E == 1))
{
label_1759:; 
if (!(T2_E == 1))
{
label_1766:; 
if (!(T3_E == 1))
{
label_1773:; 
if (!(E_1 == 1))
{
label_1780:; 
if (!(E_2 == 1))
{
label_1787:; 
if (!(E_3 == 1))
{
label_1794:; 
}
else 
{
E_3 = 2;
goto label_1794;
}
goto label_1631;
}
else 
{
E_2 = 2;
goto label_1787;
}
}
else 
{
E_1 = 2;
goto label_1780;
}
}
else 
{
T3_E = 2;
goto label_1773;
}
}
else 
{
T2_E = 2;
goto label_1766;
}
}
else 
{
T1_E = 2;
goto label_1759;
}
}
}
}
else 
{
goto label_1720;
}
}
}
else 
{
goto label_1693;
}
}
}
else 
{
goto label_1666;
}
}
}
}
}
else 
{
__retres1 = 1;
goto label_1613;
}
}
else 
{
__retres1 = 1;
goto label_1613;
}
}
else 
{
__retres1 = 1;
goto label_1613;
}
}
}
else 
{
E_2 = 2;
goto label_1582;
}
}
else 
{
E_1 = 2;
goto label_1575;
}
}
else 
{
T3_E = 2;
goto label_1568;
}
}
else 
{
T2_E = 2;
goto label_1561;
}
}
else 
{
T1_E = 2;
goto label_1554;
}
}
}
}
else 
{
goto label_1516;
}
}
}
else 
{
goto label_1489;
}
}
}
else 
{
goto label_1462;
}
}
}
}
else 
{
E_2 = 1;
goto label_1423;
}
}
else 
{
E_1 = 1;
goto label_1416;
}
}
else 
{
T3_E = 1;
goto label_1409;
}
}
else 
{
T2_E = 1;
goto label_1402;
}
}
else 
{
T1_E = 1;
goto label_1395;
}
}
}
else 
{
__retres1 = 1;
goto label_834;
}
}
else 
{
__retres1 = 1;
goto label_834;
}
}
else 
{
__retres1 = 1;
goto label_834;
}
}
}
}
else 
{
E_2 = 2;
goto label_792;
}
}
else 
{
E_1 = 2;
goto label_785;
}
}
else 
{
T3_E = 2;
goto label_778;
}
}
else 
{
T2_E = 2;
goto label_771;
}
}
else 
{
T1_E = 2;
goto label_764;
}
}
}
}
else 
{
goto label_726;
}
}
}
else 
{
goto label_699;
}
}
}
else 
{
goto label_672;
}
}
}
}
else 
{
E_2 = 1;
goto label_633;
}
}
else 
{
E_1 = 1;
goto label_626;
}
}
else 
{
T3_E = 1;
goto label_619;
}
}
else 
{
T2_E = 1;
goto label_612;
}
}
else 
{
T1_E = 1;
goto label_605;
}
}
}
else 
{
t2_st = 0;
goto label_583;
}
}
else 
{
t1_st = 0;
goto label_574;
}
}
else 
{
m_st = 0;
goto label_565;
}
}
}
}
