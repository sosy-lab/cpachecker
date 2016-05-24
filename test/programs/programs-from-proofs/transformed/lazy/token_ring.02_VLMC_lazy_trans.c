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
int __return_30976;
int __return_30987;
int __return_30998;
int __return_31052;
int __return_31729;
int __return_31765;
int __return_31776;
int __return_31789;
int __return_31861;
int __return_31897;
int __return_31908;
int __return_31920;
int __return_32110;
int __return_31973;
int __return_31986;
int __return_31997;
int __return_32092;
int __return_32078;
int __return_32233;
int __return_32246;
int __return_32259;
int __return_32382;
int __return_32618;
int __return_32631;
int __return_32707;
int __return_32669;
int __return_32435;
int __return_32448;
int __return_32461;
int __return_31296;
int __return_31332;
int __return_31343;
int __return_31357;
int __return_31670;
int __return_31410;
int __return_31423;
int __return_31434;
int __return_31652;
int __return_31491;
int __return_31505;
int __return_31518;
int __return_31548;
int __return_31596;
int __return_31607;
int __return_31621;
int __return_31088;
int __return_31099;
int __return_31110;
int __return_32126;
int __return_31686;
int __return_31844;
int __return_31284;
int __return_32279;
int __return_32292;
int __return_32305;
int __return_32394;
int __return_32590;
int __return_32603;
int __return_32708;
int __return_32696;
int __return_32481;
int __return_32494;
int __return_32507;
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
if (!(M_E == 0))
{
label_30938:; 
if (!(T1_E == 0))
{
label_30945:; 
if (!(T2_E == 0))
{
label_30952:; 
}
else 
{
T2_E = 1;
goto label_30952;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_30976 = __retres1;
}
tmp = __return_30976;
{
int __retres1 ;
__retres1 = 0;
 __return_30987 = __retres1;
}
tmp___0 = __return_30987;
{
int __retres1 ;
__retres1 = 0;
 __return_30998 = __retres1;
}
tmp___1 = __return_30998;
}
{
if (!(M_E == 1))
{
label_31009:; 
if (!(T1_E == 1))
{
label_31016:; 
if (!(T2_E == 1))
{
label_31023:; 
}
else 
{
T2_E = 2;
goto label_31023;
}
kernel_st = 1;
{
int tmp ;
label_31043:; 
{
int __retres1 ;
__retres1 = 1;
 __return_31052 = __retres1;
}
tmp = __return_31052;
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
goto label_31043;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_31268:; 
{
int __retres1 ;
__retres1 = 1;
 __return_31729 = __retres1;
}
tmp = __return_31729;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_31268;
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_31377;
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
 __return_31765 = __retres1;
}
tmp = __return_31765;
{
int __retres1 ;
__retres1 = 0;
 __return_31776 = __retres1;
}
tmp___0 = __return_31776;
{
int __retres1 ;
__retres1 = 0;
 __return_31789 = __retres1;
}
tmp___1 = __return_31789;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_31691;
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
label_31174:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_31861 = __retres1;
}
tmp = __return_31861;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_31174;
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
 __return_31897 = __retres1;
}
tmp = __return_31897;
{
int __retres1 ;
__retres1 = 1;
 __return_31908 = __retres1;
}
tmp___0 = __return_31908;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_31920 = __retres1;
}
tmp___1 = __return_31920;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_31937:; 
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
 __return_32110 = __retres1;
}
tmp = __return_32110;
goto label_31937;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
goto label_31460;
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
 __return_31973 = __retres1;
}
tmp = __return_31973;
{
int __retres1 ;
__retres1 = 0;
 __return_31986 = __retres1;
}
tmp___0 = __return_31986;
{
int __retres1 ;
__retres1 = 0;
 __return_31997 = __retres1;
}
tmp___1 = __return_31997;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_32017:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_32092 = __retres1;
}
tmp = __return_32092;
goto label_32017;
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
 __return_32078 = __retres1;
}
tmp = __return_32078;
}
label_32140:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (!(M_E == 0))
{
label_32193:; 
if (!(T1_E == 0))
{
label_32200:; 
if (!(T2_E == 0))
{
label_32207:; 
}
else 
{
T2_E = 1;
goto label_32207;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_32233 = __retres1;
}
tmp = __return_32233;
{
int __retres1 ;
__retres1 = 0;
 __return_32246 = __retres1;
}
tmp___0 = __return_32246;
{
int __retres1 ;
__retres1 = 0;
 __return_32259 = __retres1;
}
tmp___1 = __return_32259;
}
{
if (!(M_E == 1))
{
label_32347:; 
if (!(T1_E == 1))
{
label_32354:; 
if (!(T2_E == 1))
{
label_32361:; 
}
else 
{
T2_E = 2;
goto label_32361;
}
{
int __retres1 ;
__retres1 = 0;
 __return_32382 = __retres1;
}
tmp = __return_32382;
if (!(tmp == 0))
{
label_32403:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 0;
 __return_32618 = __retres1;
}
tmp = __return_32618;
if (!(tmp == 0))
{
__retres2 = 0;
label_32626:; 
 __return_32631 = __retres2;
}
else 
{
__retres2 = 1;
goto label_32626;
}
tmp___0 = __return_32631;
if (!(tmp___0 == 0))
{
}
else 
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
__retres1 = 0;
 __return_32669 = __retres1;
}
tmp = __return_32669;
}
goto label_32140;
}
__retres1 = 0;
 __return_32707 = __retres1;
return 1;
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
{
int __retres1 ;
__retres1 = 0;
 __return_32435 = __retres1;
}
tmp = __return_32435;
{
int __retres1 ;
__retres1 = 0;
 __return_32448 = __retres1;
}
tmp___0 = __return_32448;
{
int __retres1 ;
__retres1 = 0;
 __return_32461 = __retres1;
}
tmp___1 = __return_32461;
}
{
if (!(M_E == 1))
{
label_32549:; 
if (!(T1_E == 1))
{
label_32556:; 
if (!(T2_E == 1))
{
label_32563:; 
}
else 
{
T2_E = 2;
goto label_32563;
}
goto label_32403;
}
else 
{
T1_E = 2;
goto label_32556;
}
}
else 
{
M_E = 2;
goto label_32549;
}
}
}
}
else 
{
T1_E = 2;
goto label_32354;
}
}
else 
{
M_E = 2;
goto label_32347;
}
}
}
else 
{
T1_E = 1;
goto label_32200;
}
}
else 
{
M_E = 1;
goto label_32193;
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
label_31270:; 
{
int __retres1 ;
__retres1 = 1;
 __return_31296 = __retres1;
}
tmp = __return_31296;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
label_31377:; 
goto label_31270;
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
 __return_31332 = __retres1;
}
tmp = __return_31332;
{
int __retres1 ;
__retres1 = 1;
 __return_31343 = __retres1;
}
tmp___0 = __return_31343;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_31357 = __retres1;
}
tmp___1 = __return_31357;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_31370:; 
label_31374:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
label_31460:; 
{
int __retres1 ;
__retres1 = 1;
 __return_31670 = __retres1;
}
tmp = __return_31670;
goto label_31374;
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
 __return_31410 = __retres1;
}
tmp = __return_31410;
{
int __retres1 ;
__retres1 = 0;
 __return_31423 = __retres1;
}
tmp___0 = __return_31423;
{
int __retres1 ;
__retres1 = 1;
 __return_31434 = __retres1;
}
tmp___1 = __return_31434;
t2_st = 0;
}
}
E_2 = 2;
t1_pc = 1;
t1_st = 2;
}
label_31455:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_31652 = __retres1;
}
tmp = __return_31652;
goto label_31455;
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
 __return_31491 = __retres1;
}
tmp = __return_31491;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_31505 = __retres1;
}
tmp___0 = __return_31505;
{
int __retres1 ;
__retres1 = 0;
 __return_31518 = __retres1;
}
tmp___1 = __return_31518;
}
}
E_M = 2;
t2_pc = 1;
t2_st = 2;
}
label_31538:; 
{
int __retres1 ;
__retres1 = 1;
 __return_31548 = __retres1;
}
tmp = __return_31548;
int tmp_ndt_1;
tmp_ndt_1 = __VERIFIER_nondet_int();
if (tmp_ndt_1 == 0)
{
goto label_31538;
}
else 
{
m_st = 1;
{
if (token != (local + 2))
{
{
}
goto label_31568;
}
else 
{
label_31568:; 
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
 __return_31596 = __retres1;
}
tmp = __return_31596;
{
int __retres1 ;
__retres1 = 1;
 __return_31607 = __retres1;
}
tmp___0 = __return_31607;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_31621 = __retres1;
}
tmp___1 = __return_31621;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
goto label_31370;
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
 __return_31088 = __retres1;
}
tmp = __return_31088;
{
int __retres1 ;
__retres1 = 0;
 __return_31099 = __retres1;
}
tmp___0 = __return_31099;
{
int __retres1 ;
__retres1 = 0;
 __return_31110 = __retres1;
}
tmp___1 = __return_31110;
}
}
E_1 = 2;
m_pc = 1;
m_st = 2;
}
label_31127:; 
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
 __return_32126 = __retres1;
}
tmp = __return_32126;
goto label_31127;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_31269:; 
{
int __retres1 ;
__retres1 = 1;
 __return_31686 = __retres1;
}
tmp = __return_31686;
label_31691:; 
int tmp_ndt_2;
tmp_ndt_2 = __VERIFIER_nondet_int();
if (tmp_ndt_2 == 0)
{
goto label_31269;
}
else 
{
t1_st = 1;
{
t1_pc = 1;
t1_st = 2;
}
goto label_31271;
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
label_31175:; 
int tmp_ndt_3;
tmp_ndt_3 = __VERIFIER_nondet_int();
if (tmp_ndt_3 == 0)
{
{
int __retres1 ;
__retres1 = 1;
 __return_31844 = __retres1;
}
tmp = __return_31844;
goto label_31175;
}
else 
{
t2_st = 1;
{
t2_pc = 1;
t2_st = 2;
}
label_31271:; 
{
int __retres1 ;
__retres1 = 0;
 __return_31284 = __retres1;
}
tmp = __return_31284;
}
label_32139:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (!(M_E == 0))
{
label_32162:; 
if (!(T1_E == 0))
{
label_32169:; 
if (!(T2_E == 0))
{
label_32176:; 
}
else 
{
T2_E = 1;
goto label_32176;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_32279 = __retres1;
}
tmp = __return_32279;
{
int __retres1 ;
__retres1 = 0;
 __return_32292 = __retres1;
}
tmp___0 = __return_32292;
{
int __retres1 ;
__retres1 = 0;
 __return_32305 = __retres1;
}
tmp___1 = __return_32305;
}
{
if (!(M_E == 1))
{
label_32316:; 
if (!(T1_E == 1))
{
label_32323:; 
if (!(T2_E == 1))
{
label_32330:; 
}
else 
{
T2_E = 2;
goto label_32330;
}
{
int __retres1 ;
__retres1 = 0;
 __return_32394 = __retres1;
}
tmp = __return_32394;
if (!(tmp == 0))
{
label_32404:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 0;
 __return_32590 = __retres1;
}
tmp = __return_32590;
if (!(tmp == 0))
{
__retres2 = 0;
label_32598:; 
 __return_32603 = __retres2;
}
else 
{
__retres2 = 1;
goto label_32598;
}
tmp___0 = __return_32603;
if (!(tmp___0 == 0))
{
}
else 
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
__retres1 = 0;
 __return_32696 = __retres1;
}
tmp = __return_32696;
}
goto label_32139;
}
__retres1 = 0;
 __return_32708 = __retres1;
return 1;
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
{
int __retres1 ;
__retres1 = 0;
 __return_32481 = __retres1;
}
tmp = __return_32481;
{
int __retres1 ;
__retres1 = 0;
 __return_32494 = __retres1;
}
tmp___0 = __return_32494;
{
int __retres1 ;
__retres1 = 0;
 __return_32507 = __retres1;
}
tmp___1 = __return_32507;
}
{
if (!(M_E == 1))
{
label_32518:; 
if (!(T1_E == 1))
{
label_32525:; 
if (!(T2_E == 1))
{
label_32532:; 
}
else 
{
T2_E = 2;
goto label_32532;
}
goto label_32404;
}
else 
{
T1_E = 2;
goto label_32525;
}
}
else 
{
M_E = 2;
goto label_32518;
}
}
}
}
else 
{
T1_E = 2;
goto label_32323;
}
}
else 
{
M_E = 2;
goto label_32316;
}
}
}
else 
{
T1_E = 1;
goto label_32169;
}
}
else 
{
M_E = 1;
goto label_32162;
}
}
}
}
}
}
else 
{
T1_E = 2;
goto label_31016;
}
}
else 
{
M_E = 2;
goto label_31009;
}
}
}
else 
{
T1_E = 1;
goto label_30945;
}
}
else 
{
M_E = 1;
goto label_30938;
}
}
}
}
