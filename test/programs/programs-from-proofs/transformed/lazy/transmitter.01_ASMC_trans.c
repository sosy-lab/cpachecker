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
int __return_1361;
int __return_1375;
int __return_1259;
int __return_1273;
int __return_1421;
int __return_1461;
int __return_1475;
int __return_1511;
int __return_1535;
int __return_1549;
int __return_1590;
int __return_1600;
int __return_1601;
int __return_1623;
int __return_1327;
int __return_1341;
int __return_1293;
int __return_1307;
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
m_st = 0;
if (t1_i < 2)
{
t1_st = 0;
}
else 
{
t1_st = 2;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_1191;
}
else 
{
label_1191:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_1361 = __retres1;
}
tmp = __return_1361;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_1375 = __retres1;
}
tmp___0 = __return_1375;
t1_st = 0;
}
goto label_1280;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_1236;
}
else 
{
label_1236:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_1259 = __retres1;
}
tmp = __return_1259;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_1273 = __retres1;
}
tmp___0 = __return_1273;
t1_st = 0;
}
label_1280:; 
{
if (M_E < 2)
{
M_E = 2;
goto label_1387;
}
else 
{
label_1387:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_1394;
}
else 
{
label_1394:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_1399;
}
else 
{
label_1399:; 
}
kernel_st = 1;
label_1407:; 
{
int tmp ;
{
int __retres1 ;
__retres1 = 1;
 __return_1421 = __retres1;
}
tmp = __return_1421;
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E < 1)
{
M_E = 1;
goto label_1438;
}
else 
{
label_1438:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_1461 = __retres1;
}
tmp = __return_1461;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_1475 = __retres1;
}
tmp___0 = __return_1475;
t1_st = 0;
}
{
if (M_E < 2)
{
M_E = 2;
goto label_1487;
}
else 
{
label_1487:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_1494;
}
else 
{
label_1494:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_1499;
}
else 
{
label_1499:; 
}
{
int __retres1 ;
__retres1 = 1;
 __return_1511 = __retres1;
}
tmp = __return_1511;
if (tmp < 1)
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
 __return_1535 = __retres1;
}
tmp = __return_1535;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_1549 = __retres1;
}
tmp___0 = __return_1549;
t1_st = 0;
}
{
if (M_E < 2)
{
M_E = 2;
goto label_1561;
}
else 
{
label_1561:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_1568;
}
else 
{
label_1568:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_1573;
}
else 
{
label_1573:; 
}
goto label_1516;
}
}
}
}
else 
{
label_1516:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 1;
 __return_1590 = __retres1;
}
tmp = __return_1590;
if (tmp < 1)
{
__retres2 = 0;
 __return_1600 = __retres2;
}
else 
{
__retres2 = 1;
 __return_1601 = __retres2;
}
tmp___0 = __return_1600;
tmp___0 = __return_1601;
if (tmp___0 < 1)
{
}
else 
{
kernel_st = 1;
goto label_1407;
}
label_1620:; 
__retres1 = 0;
 __return_1623 = __retres1;
return 1;
}
goto label_1620;
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
m_st = 2;
if (t1_i < 2)
{
t1_st = 0;
}
else 
{
t1_st = 2;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_1206;
}
else 
{
label_1206:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_1327 = __retres1;
}
tmp = __return_1327;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_1341 = __retres1;
}
tmp___0 = __return_1341;
t1_st = 0;
}
goto label_1280;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_1221;
}
else 
{
label_1221:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_1293 = __retres1;
}
tmp = __return_1293;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_1307 = __retres1;
}
tmp___0 = __return_1307;
t1_st = 0;
}
goto label_1280;
}
}
}
}
}
