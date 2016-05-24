extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void error(void);
int m_run_st  ;
int m_run_i  ;
int m_run_pc  ;
int s_memory0  ;
int s_run_st  ;
int s_run_i  ;
int s_run_pc  ;
int c_m_lock  ;
int c_m_ev  ;
int c_req_type  ;
int c_req_a  ;
int c_req_d  ;
int c_rsp_type  ;
int c_rsp_status  ;
int c_rsp_d  ;
int c_empty_req  ;
int c_empty_rsp  ;
int c_read_req_ev  ;
int c_write_req_ev  ;
int c_read_rsp_ev  ;
int c_write_rsp_ev  ;
static int d_t  ;
static int a_t  ;
static int req_t_type  ;
static int req_t_a  ;
static int req_t_d  ;
static int rsp_t_type  ;
static int rsp_t_status  ;
static int rsp_t_d  ;
static int req_tt_type  ;
static int req_tt_a  ;
static int req_tt_d  ;
static int rsp_tt_type  ;
static int rsp_tt_status  ;
static int rsp_tt_d  ;
int s_memory_read(int i);
void s_memory_write(int i, int v);
void m_run(void);
static int req_t_type___0  ;
static int req_t_a___0  ;
static int req_t_d___0  ;
static int rsp_t_type___0  ;
static int rsp_t_status___0  ;
static int rsp_t_d___0  ;
void s_run(void);
void eval(void);
void start_simulation(void);
int main(void);
int __return_1208119;
int __return_1208120;
int __return_1211706;
int __return_1211707;
int __return_1213707;
int __return_1213708;
int __return_1214207;
int __return_1214208;
int __return_1217134;
int __return_1217135;
int __return_1219157;
int __return_1219158;
int __return_1222114;
int __return_1222115;
int __return_1219614;
int __return_1219615;
int __return_1221618;
int __return_1221619;
int __return_1217591;
int __return_1217592;
int __return_1209444;
int __return_1209445;
int __return_1209947;
int __return_1209948;
int __return_1224352;
int __return_1224353;
int __return_1226366;
int __return_1226367;
int __return_1228367;
int __return_1228368;
int __return_1224809;
int __return_1224810;
int __return_1234283;
int __return_1229530;
int __return_1229531;
int __return_1231262;
int __return_1231263;
int __return_1233263;
int __return_1233264;
int __return_1233800;
int __return_1233801;
int __return_1234281;
int main()
{
int __retres1 ;
c_m_lock = 0;
c_m_ev = 2;
m_run_i = 1;
m_run_pc = 0;
s_run_i = 1;
s_run_pc = 0;
c_empty_req = 1;
c_empty_rsp = 1;
c_read_req_ev = 2;
c_write_req_ev = 2;
c_read_rsp_ev = 2;
c_write_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 2;
{
int kernel_st ;
kernel_st = 0;
if (((int)m_run_i) == 1)
{
m_run_st = 0;
goto label_1207135;
}
else 
{
m_run_st = 2;
label_1207135:; 
if (((int)s_run_i) == 1)
{
s_run_st = 0;
goto label_1207142;
}
else 
{
s_run_st = 2;
label_1207142:; 
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1207208;
}
else 
{
goto label_1207149;
}
}
else 
{
label_1207149:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1207208;
}
else 
{
goto label_1207156;
}
}
else 
{
label_1207156:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1207208;
}
else 
{
goto label_1207163;
}
}
else 
{
label_1207163:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1207208;
}
else 
{
goto label_1207170;
}
}
else 
{
label_1207170:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1207208;
}
else 
{
goto label_1207177;
}
}
else 
{
label_1207177:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1207186;
}
else 
{
label_1207186:; 
goto label_1207208;
}
}
else 
{
label_1207208:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1207230;
}
else 
{
goto label_1207215;
}
}
else 
{
label_1207215:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1207224;
}
else 
{
label_1207224:; 
goto label_1207230;
}
}
else 
{
label_1207230:; 
label_1207232:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1207242:; 
if (((int)m_run_st) == 0)
{
goto label_1207256;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1207256:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1207738;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
goto label_1207364;
}
else 
{
label_1207364:; 
a = 0;
req_type = 1;
req_a = a;
req_d = a + 50;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1207467;
}
else 
{
goto label_1207408;
}
}
else 
{
label_1207408:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1207467;
}
else 
{
goto label_1207415;
}
}
else 
{
label_1207415:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1207467;
}
else 
{
goto label_1207422;
}
}
else 
{
label_1207422:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1207467;
}
else 
{
goto label_1207429;
}
}
else 
{
label_1207429:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1207467;
}
else 
{
goto label_1207436;
}
}
else 
{
label_1207436:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1207445;
}
else 
{
label_1207445:; 
goto label_1207467;
}
}
else 
{
label_1207467:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1207489;
}
else 
{
goto label_1207474;
}
}
else 
{
label_1207474:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1207483;
}
else 
{
label_1207483:; 
goto label_1207489;
}
}
else 
{
label_1207489:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1207569;
}
else 
{
goto label_1207510;
}
}
else 
{
label_1207510:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1207569;
}
else 
{
goto label_1207517;
}
}
else 
{
label_1207517:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1207569;
}
else 
{
goto label_1207524;
}
}
else 
{
label_1207524:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1207569;
}
else 
{
goto label_1207531;
}
}
else 
{
label_1207531:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1207569;
}
else 
{
goto label_1207538;
}
}
else 
{
label_1207538:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1207547;
}
else 
{
label_1207547:; 
goto label_1207569;
}
}
else 
{
label_1207569:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1207591;
}
else 
{
goto label_1207576;
}
}
else 
{
label_1207576:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1207585;
}
else 
{
label_1207585:; 
goto label_1207591;
}
}
else 
{
label_1207591:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1207664;
}
else 
{
goto label_1207605;
}
}
else 
{
label_1207605:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1207664;
}
else 
{
goto label_1207612;
}
}
else 
{
label_1207612:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1207664;
}
else 
{
goto label_1207619;
}
}
else 
{
label_1207619:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1207664;
}
else 
{
goto label_1207626;
}
}
else 
{
label_1207626:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1207664;
}
else 
{
goto label_1207633;
}
}
else 
{
label_1207633:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1207642;
}
else 
{
label_1207642:; 
goto label_1207664;
}
}
else 
{
label_1207664:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1207671;
}
}
else 
{
label_1207671:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1207680;
}
else 
{
label_1207680:; 
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
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1208560;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1207965;
}
else 
{
label_1207965:; 
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1208047;
}
else 
{
goto label_1207988;
}
}
else 
{
label_1207988:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1208047;
}
else 
{
goto label_1207995;
}
}
else 
{
label_1207995:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1208047;
}
else 
{
goto label_1208002;
}
}
else 
{
label_1208002:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1208047;
}
else 
{
goto label_1208009;
}
}
else 
{
label_1208009:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1208047;
}
else 
{
goto label_1208016;
}
}
else 
{
label_1208016:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1208025;
}
else 
{
label_1208025:; 
goto label_1208047;
}
}
else 
{
label_1208047:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1208069;
}
else 
{
goto label_1208054;
}
}
else 
{
label_1208054:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1208063;
}
else 
{
label_1208063:; 
goto label_1208069;
}
}
else 
{
label_1208069:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_1 = req_a;
int i = __tmp_1;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1208119 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1208120 = x;
}
rsp_d = __return_1208119;
goto label_1208122;
rsp_d = __return_1208120;
label_1208122:; 
rsp_status = 1;
label_1208128:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1208209;
}
else 
{
goto label_1208150;
}
}
else 
{
label_1208150:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1208209;
}
else 
{
goto label_1208157;
}
}
else 
{
label_1208157:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1208209;
}
else 
{
goto label_1208164;
}
}
else 
{
label_1208164:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1208209;
}
else 
{
goto label_1208171;
}
}
else 
{
label_1208171:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1208209;
}
else 
{
goto label_1208178;
}
}
else 
{
label_1208178:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1208187;
}
else 
{
label_1208187:; 
goto label_1208209;
}
}
else 
{
label_1208209:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1208231;
}
else 
{
goto label_1208216;
}
}
else 
{
label_1208216:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1208225;
}
else 
{
label_1208225:; 
goto label_1208231;
}
}
else 
{
label_1208231:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1208314;
}
else 
{
goto label_1208255;
}
}
else 
{
label_1208255:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1208314;
}
else 
{
goto label_1208262;
}
}
else 
{
label_1208262:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1208314;
}
else 
{
goto label_1208269;
}
}
else 
{
label_1208269:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1208314;
}
else 
{
goto label_1208276;
}
}
else 
{
label_1208276:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1208314;
}
else 
{
goto label_1208283;
}
}
else 
{
label_1208283:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1208292;
}
else 
{
label_1208292:; 
goto label_1208314;
}
}
else 
{
label_1208314:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1208321;
}
}
else 
{
label_1208321:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1208330;
}
else 
{
label_1208330:; 
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
label_1208384:; 
label_1210424:; 
if (((int)m_run_st) == 0)
{
goto label_1210437;
}
else 
{
label_1210437:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1211499;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1210625;
}
else 
{
goto label_1210566;
}
}
else 
{
label_1210566:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1210625;
}
else 
{
goto label_1210573;
}
}
else 
{
label_1210573:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1210625;
}
else 
{
goto label_1210580;
}
}
else 
{
label_1210580:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1210625;
}
else 
{
goto label_1210587;
}
}
else 
{
label_1210587:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1210625;
}
else 
{
goto label_1210594;
}
}
else 
{
label_1210594:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1210603;
}
else 
{
label_1210603:; 
goto label_1210625;
}
}
else 
{
label_1210625:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1210647;
}
else 
{
goto label_1210632;
}
}
else 
{
label_1210632:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1210641;
}
else 
{
label_1210641:; 
goto label_1210647;
}
}
else 
{
label_1210647:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1210720;
}
else 
{
goto label_1210661;
}
}
else 
{
label_1210661:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1210720;
}
else 
{
goto label_1210668;
}
}
else 
{
label_1210668:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1210720;
}
else 
{
goto label_1210675;
}
}
else 
{
label_1210675:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1210720;
}
else 
{
goto label_1210682;
}
}
else 
{
label_1210682:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1210720;
}
else 
{
goto label_1210689;
}
}
else 
{
label_1210689:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1210698;
}
else 
{
label_1210698:; 
goto label_1210720;
}
}
else 
{
label_1210720:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1210742;
}
else 
{
goto label_1210727;
}
}
else 
{
label_1210727:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1210736;
}
else 
{
label_1210736:; 
goto label_1210742;
}
}
else 
{
label_1210742:; 
c_m_ev = 2;
a = a + 1;
if (a < 1)
{
req_type = 1;
req_a = a;
req_d = a + 50;
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1211207;
}
else 
{
goto label_1211148;
}
}
else 
{
label_1211148:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1211207;
}
else 
{
goto label_1211155;
}
}
else 
{
label_1211155:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1211207;
}
else 
{
goto label_1211162;
}
}
else 
{
label_1211162:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1211207;
}
else 
{
goto label_1211169;
}
}
else 
{
label_1211169:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1211207;
}
else 
{
goto label_1211176;
}
}
else 
{
label_1211176:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1211185;
}
else 
{
label_1211185:; 
goto label_1211207;
}
}
else 
{
label_1211207:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1211229;
}
else 
{
goto label_1211214;
}
}
else 
{
label_1211214:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1211223;
}
else 
{
label_1211223:; 
goto label_1211229;
}
}
else 
{
label_1211229:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1211309;
}
else 
{
goto label_1211250;
}
}
else 
{
label_1211250:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1211309;
}
else 
{
goto label_1211257;
}
}
else 
{
label_1211257:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1211309;
}
else 
{
goto label_1211264;
}
}
else 
{
label_1211264:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1211309;
}
else 
{
goto label_1211271;
}
}
else 
{
label_1211271:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1211309;
}
else 
{
goto label_1211278;
}
}
else 
{
label_1211278:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1211287;
}
else 
{
label_1211287:; 
goto label_1211309;
}
}
else 
{
label_1211309:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1211331;
}
else 
{
goto label_1211316;
}
}
else 
{
label_1211316:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1211325;
}
else 
{
label_1211325:; 
goto label_1211331;
}
}
else 
{
label_1211331:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1211404;
}
else 
{
goto label_1211345;
}
}
else 
{
label_1211345:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1211404;
}
else 
{
goto label_1211352;
}
}
else 
{
label_1211352:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1211404;
}
else 
{
goto label_1211359;
}
}
else 
{
label_1211359:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1211404;
}
else 
{
goto label_1211366;
}
}
else 
{
label_1211366:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1211404;
}
else 
{
goto label_1211373;
}
}
else 
{
label_1211373:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1211382;
}
else 
{
label_1211382:; 
goto label_1211404;
}
}
else 
{
label_1211404:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1211411;
}
}
else 
{
label_1211411:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1211420;
}
else 
{
label_1211420:; 
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
goto label_1209055;
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
a = 0;
req_type___0 = 0;
req_a___0 = a;
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1210853;
}
else 
{
goto label_1210794;
}
}
else 
{
label_1210794:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1210853;
}
else 
{
goto label_1210801;
}
}
else 
{
label_1210801:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1210853;
}
else 
{
goto label_1210808;
}
}
else 
{
label_1210808:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1210853;
}
else 
{
goto label_1210815;
}
}
else 
{
label_1210815:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1210853;
}
else 
{
goto label_1210822;
}
}
else 
{
label_1210822:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1210831;
}
else 
{
label_1210831:; 
goto label_1210853;
}
}
else 
{
label_1210853:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1210875;
}
else 
{
goto label_1210860;
}
}
else 
{
label_1210860:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1210869;
}
else 
{
label_1210869:; 
goto label_1210875;
}
}
else 
{
label_1210875:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1210955;
}
else 
{
goto label_1210896;
}
}
else 
{
label_1210896:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1210955;
}
else 
{
goto label_1210903;
}
}
else 
{
label_1210903:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1210955;
}
else 
{
goto label_1210910;
}
}
else 
{
label_1210910:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1210955;
}
else 
{
goto label_1210917;
}
}
else 
{
label_1210917:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1210955;
}
else 
{
goto label_1210924;
}
}
else 
{
label_1210924:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1210933;
}
else 
{
label_1210933:; 
goto label_1210955;
}
}
else 
{
label_1210955:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1210977;
}
else 
{
goto label_1210962;
}
}
else 
{
label_1210962:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1210971;
}
else 
{
label_1210971:; 
goto label_1210977;
}
}
else 
{
label_1210977:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1211050;
}
else 
{
goto label_1210991;
}
}
else 
{
label_1210991:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1211050;
}
else 
{
goto label_1210998;
}
}
else 
{
label_1210998:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1211050;
}
else 
{
goto label_1211005;
}
}
else 
{
label_1211005:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1211050;
}
else 
{
goto label_1211012;
}
}
else 
{
label_1211012:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1211050;
}
else 
{
goto label_1211019;
}
}
else 
{
label_1211019:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1211028;
}
else 
{
label_1211028:; 
goto label_1211050;
}
}
else 
{
label_1211050:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1211057;
}
}
else 
{
label_1211057:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1211066;
}
else 
{
label_1211066:; 
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
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1211977;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1211634;
}
else 
{
goto label_1211575;
}
}
else 
{
label_1211575:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1211634;
}
else 
{
goto label_1211582;
}
}
else 
{
label_1211582:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1211634;
}
else 
{
goto label_1211589;
}
}
else 
{
label_1211589:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1211634;
}
else 
{
goto label_1211596;
}
}
else 
{
label_1211596:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1211634;
}
else 
{
goto label_1211603;
}
}
else 
{
label_1211603:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1211612;
}
else 
{
label_1211612:; 
goto label_1211634;
}
}
else 
{
label_1211634:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1211656;
}
else 
{
goto label_1211641;
}
}
else 
{
label_1211641:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1211650;
}
else 
{
label_1211650:; 
goto label_1211656;
}
}
else 
{
label_1211656:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_2 = req_a;
int i = __tmp_2;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1211706 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1211707 = x;
}
rsp_d = __return_1211706;
goto label_1211709;
rsp_d = __return_1211707;
label_1211709:; 
rsp_status = 1;
label_1211715:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1211796;
}
else 
{
goto label_1211737;
}
}
else 
{
label_1211737:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1211796;
}
else 
{
goto label_1211744;
}
}
else 
{
label_1211744:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1211796;
}
else 
{
goto label_1211751;
}
}
else 
{
label_1211751:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1211796;
}
else 
{
goto label_1211758;
}
}
else 
{
label_1211758:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1211796;
}
else 
{
goto label_1211765;
}
}
else 
{
label_1211765:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1211774;
}
else 
{
label_1211774:; 
goto label_1211796;
}
}
else 
{
label_1211796:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1211818;
}
else 
{
goto label_1211803;
}
}
else 
{
label_1211803:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1211812;
}
else 
{
label_1211812:; 
goto label_1211818;
}
}
else 
{
label_1211818:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1211901;
}
else 
{
goto label_1211842;
}
}
else 
{
label_1211842:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1211901;
}
else 
{
goto label_1211849;
}
}
else 
{
label_1211849:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1211901;
}
else 
{
goto label_1211856;
}
}
else 
{
label_1211856:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1211901;
}
else 
{
goto label_1211863;
}
}
else 
{
label_1211863:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1211901;
}
else 
{
goto label_1211870;
}
}
else 
{
label_1211870:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1211879;
}
else 
{
label_1211879:; 
goto label_1211901;
}
}
else 
{
label_1211901:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1211908;
}
}
else 
{
label_1211908:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1211917;
}
else 
{
label_1211917:; 
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
label_1211971:; 
label_1211982:; 
if (((int)m_run_st) == 0)
{
goto label_1211995;
}
else 
{
label_1211995:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1212351;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1212183;
}
else 
{
goto label_1212124;
}
}
else 
{
label_1212124:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1212183;
}
else 
{
goto label_1212131;
}
}
else 
{
label_1212131:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1212183;
}
else 
{
goto label_1212138;
}
}
else 
{
label_1212138:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1212183;
}
else 
{
goto label_1212145;
}
}
else 
{
label_1212145:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1212183;
}
else 
{
goto label_1212152;
}
}
else 
{
label_1212152:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1212161;
}
else 
{
label_1212161:; 
goto label_1212183;
}
}
else 
{
label_1212183:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1212205;
}
else 
{
goto label_1212190;
}
}
else 
{
label_1212190:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1212199;
}
else 
{
label_1212199:; 
goto label_1212205;
}
}
else 
{
label_1212205:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1212278;
}
else 
{
goto label_1212219;
}
}
else 
{
label_1212219:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1212278;
}
else 
{
goto label_1212226;
}
}
else 
{
label_1212226:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1212278;
}
else 
{
goto label_1212233;
}
}
else 
{
label_1212233:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1212278;
}
else 
{
goto label_1212240;
}
}
else 
{
label_1212240:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1212278;
}
else 
{
goto label_1212247;
}
}
else 
{
label_1212247:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1212256;
}
else 
{
label_1212256:; 
goto label_1212278;
}
}
else 
{
label_1212278:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1212300;
}
else 
{
goto label_1212285;
}
}
else 
{
label_1212285:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1212294;
}
else 
{
label_1212294:; 
goto label_1212300;
}
}
else 
{
label_1212300:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
a = a + 1;
goto label_1212317;
}
else 
{
{
__VERIFIER_error();
}
a = a + 1;
label_1212317:; 
}
label_1212346:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1212662;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1212405;
}
else 
{
if (((int)s_run_pc) == 2)
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1212564;
}
else 
{
goto label_1212446;
}
}
else 
{
label_1212446:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1212564;
}
else 
{
goto label_1212458;
}
}
else 
{
label_1212458:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1212564;
}
else 
{
goto label_1212474;
}
}
else 
{
label_1212474:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1212564;
}
else 
{
goto label_1212486;
}
}
else 
{
label_1212486:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1212564;
}
else 
{
goto label_1212502;
}
}
else 
{
label_1212502:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1212520;
}
else 
{
label_1212520:; 
goto label_1212564;
}
}
else 
{
label_1212564:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1212576;
}
}
else 
{
label_1212576:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1212594;
}
else 
{
label_1212594:; 
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
goto label_1212654;
}
else 
{
label_1212405:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1212562;
}
else 
{
goto label_1212444;
}
}
else 
{
label_1212444:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1212562;
}
else 
{
goto label_1212460;
}
}
else 
{
label_1212460:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1212562;
}
else 
{
goto label_1212472;
}
}
else 
{
label_1212472:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1212562;
}
else 
{
goto label_1212488;
}
}
else 
{
label_1212488:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1212562;
}
else 
{
goto label_1212500;
}
}
else 
{
label_1212500:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1212518;
}
else 
{
label_1212518:; 
goto label_1212562;
}
}
else 
{
label_1212562:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1212578;
}
}
else 
{
label_1212578:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1212596;
}
else 
{
label_1212596:; 
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
label_1212654:; 
label_1212667:; 
if (((int)m_run_st) == 0)
{
goto label_1212681;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1212681:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1212914;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1212869;
}
else 
{
goto label_1212810;
}
}
else 
{
label_1212810:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1212869;
}
else 
{
goto label_1212817;
}
}
else 
{
label_1212817:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1212869;
}
else 
{
goto label_1212824;
}
}
else 
{
label_1212824:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1212869;
}
else 
{
goto label_1212831;
}
}
else 
{
label_1212831:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1212869;
}
else 
{
goto label_1212838;
}
}
else 
{
label_1212838:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1212847;
}
else 
{
label_1212847:; 
goto label_1212869;
}
}
else 
{
label_1212869:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1212876;
}
}
else 
{
label_1212876:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1212885;
}
else 
{
label_1212885:; 
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
goto label_1212346;
}
}
}
}
else 
{
label_1212914:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_1212925;
}
else 
{
label_1212925:; 
goto label_1212667;
}
}
}
else 
{
}
label_1212678:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215218;
}
else 
{
goto label_1214746;
}
}
else 
{
label_1214746:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215218;
}
else 
{
goto label_1214804;
}
}
else 
{
label_1214804:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215218;
}
else 
{
goto label_1214858;
}
}
else 
{
label_1214858:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215218;
}
else 
{
goto label_1214916;
}
}
else 
{
label_1214916:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215218;
}
else 
{
goto label_1214970;
}
}
else 
{
label_1214970:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215042;
}
else 
{
label_1215042:; 
goto label_1215218;
}
}
else 
{
label_1215218:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1215396;
}
else 
{
goto label_1215276;
}
}
else 
{
label_1215276:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1215348;
}
else 
{
label_1215348:; 
goto label_1215396;
}
}
else 
{
label_1215396:; 
if (((int)m_run_st) == 0)
{
goto label_1215492;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1215492:; 
goto label_1222390;
}
else 
{
}
goto label_1215460;
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
label_1212662:; 
label_1212931:; 
if (((int)m_run_st) == 0)
{
goto label_1212945;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1212945:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1213178;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1213133;
}
else 
{
goto label_1213074;
}
}
else 
{
label_1213074:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1213133;
}
else 
{
goto label_1213081;
}
}
else 
{
label_1213081:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1213133;
}
else 
{
goto label_1213088;
}
}
else 
{
label_1213088:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1213133;
}
else 
{
goto label_1213095;
}
}
else 
{
label_1213095:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1213133;
}
else 
{
goto label_1213102;
}
}
else 
{
label_1213102:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1213111;
}
else 
{
label_1213111:; 
goto label_1213133;
}
}
else 
{
label_1213133:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1213140;
}
}
else 
{
label_1213140:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1213149;
}
else 
{
label_1213149:; 
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
goto label_1212346;
}
}
}
}
else 
{
label_1213178:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1213478;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1213224;
}
else 
{
if (((int)s_run_pc) == 2)
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1213383;
}
else 
{
goto label_1213265;
}
}
else 
{
label_1213265:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1213383;
}
else 
{
goto label_1213277;
}
}
else 
{
label_1213277:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1213383;
}
else 
{
goto label_1213293;
}
}
else 
{
label_1213293:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1213383;
}
else 
{
goto label_1213305;
}
}
else 
{
label_1213305:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1213383;
}
else 
{
goto label_1213321;
}
}
else 
{
label_1213321:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1213339;
}
else 
{
label_1213339:; 
goto label_1213383;
}
}
else 
{
label_1213383:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1213395;
}
}
else 
{
label_1213395:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1213413;
}
else 
{
label_1213413:; 
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
goto label_1212654;
}
else 
{
label_1213224:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1213381;
}
else 
{
goto label_1213263;
}
}
else 
{
label_1213263:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1213381;
}
else 
{
goto label_1213279;
}
}
else 
{
label_1213279:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1213381;
}
else 
{
goto label_1213291;
}
}
else 
{
label_1213291:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1213381;
}
else 
{
goto label_1213307;
}
}
else 
{
label_1213307:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1213381;
}
else 
{
goto label_1213319;
}
}
else 
{
label_1213319:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1213337;
}
else 
{
label_1213337:; 
goto label_1213381;
}
}
else 
{
label_1213381:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1213397;
}
}
else 
{
label_1213397:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1213415;
}
else 
{
label_1213415:; 
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
goto label_1212654;
}
}
}
}
}
else 
{
label_1213478:; 
goto label_1212931;
}
}
}
else 
{
}
label_1212942:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215216;
}
else 
{
goto label_1214744;
}
}
else 
{
label_1214744:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215216;
}
else 
{
goto label_1214806;
}
}
else 
{
label_1214806:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215216;
}
else 
{
goto label_1214856;
}
}
else 
{
label_1214856:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215216;
}
else 
{
goto label_1214918;
}
}
else 
{
label_1214918:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215216;
}
else 
{
goto label_1214968;
}
}
else 
{
label_1214968:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215040;
}
else 
{
label_1215040:; 
goto label_1215216;
}
}
else 
{
label_1215216:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1215398;
}
else 
{
goto label_1215278;
}
}
else 
{
label_1215278:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1215350;
}
else 
{
label_1215350:; 
goto label_1215398;
}
}
else 
{
label_1215398:; 
if (((int)m_run_st) == 0)
{
goto label_1215494;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1215494:; 
label_1222390:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1222400:; 
if (((int)m_run_st) == 0)
{
goto label_1222414;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1222414:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1222648;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1222602;
}
else 
{
goto label_1222543;
}
}
else 
{
label_1222543:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1222602;
}
else 
{
goto label_1222550;
}
}
else 
{
label_1222550:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1222602;
}
else 
{
goto label_1222557;
}
}
else 
{
label_1222557:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1222602;
}
else 
{
goto label_1222564;
}
}
else 
{
label_1222564:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1222602;
}
else 
{
goto label_1222571;
}
}
else 
{
label_1222571:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1222580;
}
else 
{
label_1222580:; 
goto label_1222602;
}
}
else 
{
label_1222602:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1222609;
}
}
else 
{
label_1222609:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1222618;
}
else 
{
label_1222618:; 
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
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1223249;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1222702;
}
else 
{
if (((int)s_run_pc) == 2)
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1222861;
}
else 
{
goto label_1222743;
}
}
else 
{
label_1222743:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1222861;
}
else 
{
goto label_1222755;
}
}
else 
{
label_1222755:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1222861;
}
else 
{
goto label_1222771;
}
}
else 
{
label_1222771:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1222861;
}
else 
{
goto label_1222783;
}
}
else 
{
label_1222783:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1222861;
}
else 
{
goto label_1222799;
}
}
else 
{
label_1222799:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1222817;
}
else 
{
label_1222817:; 
goto label_1222861;
}
}
else 
{
label_1222861:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1222873;
}
}
else 
{
label_1222873:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1222891;
}
else 
{
label_1222891:; 
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
goto label_1222951;
}
else 
{
label_1222702:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1222859;
}
else 
{
goto label_1222741;
}
}
else 
{
label_1222741:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1222859;
}
else 
{
goto label_1222757;
}
}
else 
{
label_1222757:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1222859;
}
else 
{
goto label_1222769;
}
}
else 
{
label_1222769:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1222859;
}
else 
{
goto label_1222785;
}
}
else 
{
label_1222785:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1222859;
}
else 
{
goto label_1222797;
}
}
else 
{
label_1222797:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1222815;
}
else 
{
label_1222815:; 
goto label_1222859;
}
}
else 
{
label_1222859:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1222875;
}
}
else 
{
label_1222875:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1222893;
}
else 
{
label_1222893:; 
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
label_1222951:; 
goto label_1222400;
}
}
}
}
}
else 
{
label_1223249:; 
goto label_1222400;
}
}
}
}
}
else 
{
label_1222648:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1223251;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1222992;
}
else 
{
if (((int)s_run_pc) == 2)
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1223151;
}
else 
{
goto label_1223033;
}
}
else 
{
label_1223033:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1223151;
}
else 
{
goto label_1223045;
}
}
else 
{
label_1223045:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1223151;
}
else 
{
goto label_1223061;
}
}
else 
{
label_1223061:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1223151;
}
else 
{
goto label_1223073;
}
}
else 
{
label_1223073:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1223151;
}
else 
{
goto label_1223089;
}
}
else 
{
label_1223089:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1223107;
}
else 
{
label_1223107:; 
goto label_1223151;
}
}
else 
{
label_1223151:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1223163;
}
}
else 
{
label_1223163:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1223181;
}
else 
{
label_1223181:; 
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
goto label_1223241;
}
else 
{
label_1222992:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1223149;
}
else 
{
goto label_1223031;
}
}
else 
{
label_1223031:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1223149;
}
else 
{
goto label_1223047;
}
}
else 
{
label_1223047:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1223149;
}
else 
{
goto label_1223059;
}
}
else 
{
label_1223059:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1223149;
}
else 
{
goto label_1223075;
}
}
else 
{
label_1223075:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1223149;
}
else 
{
goto label_1223087;
}
}
else 
{
label_1223087:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1223105;
}
else 
{
label_1223105:; 
goto label_1223149;
}
}
else 
{
label_1223149:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1223165;
}
}
else 
{
label_1223165:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1223183;
}
else 
{
label_1223183:; 
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
label_1223241:; 
goto label_1222400;
}
}
}
}
}
else 
{
label_1223251:; 
goto label_1222400;
}
}
}
else 
{
}
goto label_1212942;
}
}
}
else 
{
}
goto label_1215460;
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
}
else 
{
label_1212351:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_1212664;
}
else 
{
label_1212664:; 
goto label_1211982;
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
if (((int)req_type) == 1)
{
{
int __tmp_3 = req_a;
int __tmp_4 = req_d;
int i = __tmp_3;
int v = __tmp_4;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1211684;
label_1211684:; 
rsp_status = 1;
goto label_1211715;
}
}
else 
{
rsp_status = 0;
goto label_1211715;
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
label_1211977:; 
label_1213484:; 
if (((int)m_run_st) == 0)
{
goto label_1213497;
}
else 
{
label_1213497:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1213508;
}
else 
{
label_1213508:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1213975;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1213635;
}
else 
{
goto label_1213576;
}
}
else 
{
label_1213576:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1213635;
}
else 
{
goto label_1213583;
}
}
else 
{
label_1213583:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1213635;
}
else 
{
goto label_1213590;
}
}
else 
{
label_1213590:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1213635;
}
else 
{
goto label_1213597;
}
}
else 
{
label_1213597:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1213635;
}
else 
{
goto label_1213604;
}
}
else 
{
label_1213604:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1213613;
}
else 
{
label_1213613:; 
goto label_1213635;
}
}
else 
{
label_1213635:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1213657;
}
else 
{
goto label_1213642;
}
}
else 
{
label_1213642:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1213651;
}
else 
{
label_1213651:; 
goto label_1213657;
}
}
else 
{
label_1213657:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_5 = req_a;
int i = __tmp_5;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1213707 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1213708 = x;
}
rsp_d = __return_1213707;
goto label_1213710;
rsp_d = __return_1213708;
label_1213710:; 
rsp_status = 1;
label_1213716:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1213797;
}
else 
{
goto label_1213738;
}
}
else 
{
label_1213738:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1213797;
}
else 
{
goto label_1213745;
}
}
else 
{
label_1213745:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1213797;
}
else 
{
goto label_1213752;
}
}
else 
{
label_1213752:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1213797;
}
else 
{
goto label_1213759;
}
}
else 
{
label_1213759:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1213797;
}
else 
{
goto label_1213766;
}
}
else 
{
label_1213766:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1213775;
}
else 
{
label_1213775:; 
goto label_1213797;
}
}
else 
{
label_1213797:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1213819;
}
else 
{
goto label_1213804;
}
}
else 
{
label_1213804:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1213813;
}
else 
{
label_1213813:; 
goto label_1213819;
}
}
else 
{
label_1213819:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1213902;
}
else 
{
goto label_1213843;
}
}
else 
{
label_1213843:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1213902;
}
else 
{
goto label_1213850;
}
}
else 
{
label_1213850:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1213902;
}
else 
{
goto label_1213857;
}
}
else 
{
label_1213857:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1213902;
}
else 
{
goto label_1213864;
}
}
else 
{
label_1213864:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1213902;
}
else 
{
goto label_1213871;
}
}
else 
{
label_1213871:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1213880;
}
else 
{
label_1213880:; 
goto label_1213902;
}
}
else 
{
label_1213902:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1213909;
}
}
else 
{
label_1213909:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1213918;
}
else 
{
label_1213918:; 
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
goto label_1211971;
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
if (((int)req_type) == 1)
{
{
int __tmp_6 = req_a;
int __tmp_7 = req_d;
int i = __tmp_6;
int v = __tmp_7;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1213685;
label_1213685:; 
rsp_status = 1;
goto label_1213716;
}
}
else 
{
rsp_status = 0;
goto label_1213716;
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
label_1213975:; 
goto label_1213484;
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
}
}
}
else 
{
label_1211499:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_1211979;
}
else 
{
label_1211979:; 
goto label_1210424;
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
if (((int)req_type) == 1)
{
{
int __tmp_8 = req_a;
int __tmp_9 = req_d;
int i = __tmp_8;
int v = __tmp_9;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1208097;
label_1208097:; 
rsp_status = 1;
goto label_1208128;
}
}
else 
{
rsp_status = 0;
goto label_1208128;
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
label_1208560:; 
label_1213983:; 
if (((int)m_run_st) == 0)
{
goto label_1213997;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1213997:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1214008;
}
else 
{
label_1214008:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1214475;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1214053;
}
else 
{
label_1214053:; 
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1214135;
}
else 
{
goto label_1214076;
}
}
else 
{
label_1214076:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1214135;
}
else 
{
goto label_1214083;
}
}
else 
{
label_1214083:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1214135;
}
else 
{
goto label_1214090;
}
}
else 
{
label_1214090:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1214135;
}
else 
{
goto label_1214097;
}
}
else 
{
label_1214097:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1214135;
}
else 
{
goto label_1214104;
}
}
else 
{
label_1214104:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1214113;
}
else 
{
label_1214113:; 
goto label_1214135;
}
}
else 
{
label_1214135:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1214157;
}
else 
{
goto label_1214142;
}
}
else 
{
label_1214142:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1214151;
}
else 
{
label_1214151:; 
goto label_1214157;
}
}
else 
{
label_1214157:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_10 = req_a;
int i = __tmp_10;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1214207 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1214208 = x;
}
rsp_d = __return_1214207;
goto label_1214210;
rsp_d = __return_1214208;
label_1214210:; 
rsp_status = 1;
label_1214216:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1214297;
}
else 
{
goto label_1214238;
}
}
else 
{
label_1214238:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1214297;
}
else 
{
goto label_1214245;
}
}
else 
{
label_1214245:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1214297;
}
else 
{
goto label_1214252;
}
}
else 
{
label_1214252:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1214297;
}
else 
{
goto label_1214259;
}
}
else 
{
label_1214259:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1214297;
}
else 
{
goto label_1214266;
}
}
else 
{
label_1214266:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1214275;
}
else 
{
label_1214275:; 
goto label_1214297;
}
}
else 
{
label_1214297:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1214319;
}
else 
{
goto label_1214304;
}
}
else 
{
label_1214304:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1214313;
}
else 
{
label_1214313:; 
goto label_1214319;
}
}
else 
{
label_1214319:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1214402;
}
else 
{
goto label_1214343;
}
}
else 
{
label_1214343:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1214402;
}
else 
{
goto label_1214350;
}
}
else 
{
label_1214350:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1214402;
}
else 
{
goto label_1214357;
}
}
else 
{
label_1214357:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1214402;
}
else 
{
goto label_1214364;
}
}
else 
{
label_1214364:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1214402;
}
else 
{
goto label_1214371;
}
}
else 
{
label_1214371:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1214380;
}
else 
{
label_1214380:; 
goto label_1214402;
}
}
else 
{
label_1214402:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1214409;
}
}
else 
{
label_1214409:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1214418;
}
else 
{
label_1214418:; 
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
goto label_1208384;
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
if (((int)req_type) == 1)
{
{
int __tmp_11 = req_a;
int __tmp_12 = req_d;
int i = __tmp_11;
int v = __tmp_12;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1214185;
label_1214185:; 
rsp_status = 1;
goto label_1214216;
}
}
else 
{
rsp_status = 0;
goto label_1214216;
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
label_1214475:; 
goto label_1213983;
}
}
}
else 
{
}
label_1213994:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215214;
}
else 
{
goto label_1214742;
}
}
else 
{
label_1214742:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215214;
}
else 
{
goto label_1214808;
}
}
else 
{
label_1214808:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215214;
}
else 
{
goto label_1214854;
}
}
else 
{
label_1214854:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215214;
}
else 
{
goto label_1214920;
}
}
else 
{
label_1214920:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215214;
}
else 
{
goto label_1214966;
}
}
else 
{
label_1214966:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215038;
}
else 
{
label_1215038:; 
goto label_1215214;
}
}
else 
{
label_1215214:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1215400;
}
else 
{
goto label_1215280;
}
}
else 
{
label_1215280:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1215352;
}
else 
{
label_1215352:; 
goto label_1215400;
}
}
else 
{
label_1215400:; 
if (((int)m_run_st) == 0)
{
goto label_1215496;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1215496:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1216586:; 
if (((int)m_run_st) == 0)
{
goto label_1216600;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1216600:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1216927;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1216788;
}
else 
{
goto label_1216729;
}
}
else 
{
label_1216729:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1216788;
}
else 
{
goto label_1216736;
}
}
else 
{
label_1216736:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1216788;
}
else 
{
goto label_1216743;
}
}
else 
{
label_1216743:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1216788;
}
else 
{
goto label_1216750;
}
}
else 
{
label_1216750:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1216788;
}
else 
{
goto label_1216757;
}
}
else 
{
label_1216757:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1216766;
}
else 
{
label_1216766:; 
goto label_1216788;
}
}
else 
{
label_1216788:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1216810;
}
else 
{
goto label_1216795;
}
}
else 
{
label_1216795:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1216804;
}
else 
{
label_1216804:; 
goto label_1216810;
}
}
else 
{
label_1216810:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1216883;
}
else 
{
goto label_1216824;
}
}
else 
{
label_1216824:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1216883;
}
else 
{
goto label_1216831;
}
}
else 
{
label_1216831:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1216883;
}
else 
{
goto label_1216838;
}
}
else 
{
label_1216838:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1216883;
}
else 
{
goto label_1216845;
}
}
else 
{
label_1216845:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1216883;
}
else 
{
goto label_1216852;
}
}
else 
{
label_1216852:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1216861;
}
else 
{
label_1216861:; 
goto label_1216883;
}
}
else 
{
label_1216883:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1216890;
}
}
else 
{
label_1216890:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1216899;
}
else 
{
label_1216899:; 
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
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1217861;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1216980;
}
else 
{
label_1216980:; 
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1217062;
}
else 
{
goto label_1217003;
}
}
else 
{
label_1217003:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1217062;
}
else 
{
goto label_1217010;
}
}
else 
{
label_1217010:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1217062;
}
else 
{
goto label_1217017;
}
}
else 
{
label_1217017:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1217062;
}
else 
{
goto label_1217024;
}
}
else 
{
label_1217024:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1217062;
}
else 
{
goto label_1217031;
}
}
else 
{
label_1217031:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1217040;
}
else 
{
label_1217040:; 
goto label_1217062;
}
}
else 
{
label_1217062:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1217084;
}
else 
{
goto label_1217069;
}
}
else 
{
label_1217069:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1217078;
}
else 
{
label_1217078:; 
goto label_1217084;
}
}
else 
{
label_1217084:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_13 = req_a;
int i = __tmp_13;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1217134 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1217135 = x;
}
rsp_d = __return_1217134;
goto label_1217137;
rsp_d = __return_1217135;
label_1217137:; 
rsp_status = 1;
label_1217143:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1217224;
}
else 
{
goto label_1217165;
}
}
else 
{
label_1217165:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1217224;
}
else 
{
goto label_1217172;
}
}
else 
{
label_1217172:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1217224;
}
else 
{
goto label_1217179;
}
}
else 
{
label_1217179:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1217224;
}
else 
{
goto label_1217186;
}
}
else 
{
label_1217186:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1217224;
}
else 
{
goto label_1217193;
}
}
else 
{
label_1217193:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1217202;
}
else 
{
label_1217202:; 
goto label_1217224;
}
}
else 
{
label_1217224:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1217246;
}
else 
{
goto label_1217231;
}
}
else 
{
label_1217231:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1217240;
}
else 
{
label_1217240:; 
goto label_1217246;
}
}
else 
{
label_1217246:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1217329;
}
else 
{
goto label_1217270;
}
}
else 
{
label_1217270:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1217329;
}
else 
{
goto label_1217277;
}
}
else 
{
label_1217277:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1217329;
}
else 
{
goto label_1217284;
}
}
else 
{
label_1217284:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1217329;
}
else 
{
goto label_1217291;
}
}
else 
{
label_1217291:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1217329;
}
else 
{
goto label_1217298;
}
}
else 
{
label_1217298:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1217307;
}
else 
{
label_1217307:; 
goto label_1217329;
}
}
else 
{
label_1217329:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1217336;
}
}
else 
{
label_1217336:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1217345;
}
else 
{
label_1217345:; 
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
label_1217399:; 
label_1217866:; 
if (((int)m_run_st) == 0)
{
goto label_1217879;
}
else 
{
label_1217879:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1218942;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218067;
}
else 
{
goto label_1218008;
}
}
else 
{
label_1218008:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218067;
}
else 
{
goto label_1218015;
}
}
else 
{
label_1218015:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218067;
}
else 
{
goto label_1218022;
}
}
else 
{
label_1218022:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218067;
}
else 
{
goto label_1218029;
}
}
else 
{
label_1218029:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218067;
}
else 
{
goto label_1218036;
}
}
else 
{
label_1218036:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218045;
}
else 
{
label_1218045:; 
goto label_1218067;
}
}
else 
{
label_1218067:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1218089;
}
else 
{
goto label_1218074;
}
}
else 
{
label_1218074:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1218083;
}
else 
{
label_1218083:; 
goto label_1218089;
}
}
else 
{
label_1218089:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218162;
}
else 
{
goto label_1218103;
}
}
else 
{
label_1218103:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218162;
}
else 
{
goto label_1218110;
}
}
else 
{
label_1218110:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218162;
}
else 
{
goto label_1218117;
}
}
else 
{
label_1218117:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218162;
}
else 
{
goto label_1218124;
}
}
else 
{
label_1218124:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218162;
}
else 
{
goto label_1218131;
}
}
else 
{
label_1218131:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218140;
}
else 
{
label_1218140:; 
goto label_1218162;
}
}
else 
{
label_1218162:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1218184;
}
else 
{
goto label_1218169;
}
}
else 
{
label_1218169:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1218178;
}
else 
{
label_1218178:; 
goto label_1218184;
}
}
else 
{
label_1218184:; 
c_m_ev = 2;
a = a + 1;
if (a < 1)
{
req_type = 1;
req_a = a;
req_d = a + 50;
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218649;
}
else 
{
goto label_1218590;
}
}
else 
{
label_1218590:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218649;
}
else 
{
goto label_1218597;
}
}
else 
{
label_1218597:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218649;
}
else 
{
goto label_1218604;
}
}
else 
{
label_1218604:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218649;
}
else 
{
goto label_1218611;
}
}
else 
{
label_1218611:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218649;
}
else 
{
goto label_1218618;
}
}
else 
{
label_1218618:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218627;
}
else 
{
label_1218627:; 
goto label_1218649;
}
}
else 
{
label_1218649:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1218671;
}
else 
{
goto label_1218656;
}
}
else 
{
label_1218656:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1218665;
}
else 
{
label_1218665:; 
goto label_1218671;
}
}
else 
{
label_1218671:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218751;
}
else 
{
goto label_1218692;
}
}
else 
{
label_1218692:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218751;
}
else 
{
goto label_1218699;
}
}
else 
{
label_1218699:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218751;
}
else 
{
goto label_1218706;
}
}
else 
{
label_1218706:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218751;
}
else 
{
goto label_1218713;
}
}
else 
{
label_1218713:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218751;
}
else 
{
goto label_1218720;
}
}
else 
{
label_1218720:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218729;
}
else 
{
label_1218729:; 
goto label_1218751;
}
}
else 
{
label_1218751:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1218773;
}
else 
{
goto label_1218758;
}
}
else 
{
label_1218758:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1218767;
}
else 
{
label_1218767:; 
goto label_1218773;
}
}
else 
{
label_1218773:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218846;
}
else 
{
goto label_1218787;
}
}
else 
{
label_1218787:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218846;
}
else 
{
goto label_1218794;
}
}
else 
{
label_1218794:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218846;
}
else 
{
goto label_1218801;
}
}
else 
{
label_1218801:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218846;
}
else 
{
goto label_1218808;
}
}
else 
{
label_1218808:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218846;
}
else 
{
goto label_1218815;
}
}
else 
{
label_1218815:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218824;
}
else 
{
label_1218824:; 
goto label_1218846;
}
}
else 
{
label_1218846:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1218853;
}
}
else 
{
label_1218853:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1218862;
}
else 
{
label_1218862:; 
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
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1219886;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1219085;
}
else 
{
goto label_1219026;
}
}
else 
{
label_1219026:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1219085;
}
else 
{
goto label_1219033;
}
}
else 
{
label_1219033:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1219085;
}
else 
{
goto label_1219040;
}
}
else 
{
label_1219040:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1219085;
}
else 
{
goto label_1219047;
}
}
else 
{
label_1219047:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1219085;
}
else 
{
goto label_1219054;
}
}
else 
{
label_1219054:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1219063;
}
else 
{
label_1219063:; 
goto label_1219085;
}
}
else 
{
label_1219085:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1219107;
}
else 
{
goto label_1219092;
}
}
else 
{
label_1219092:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1219101;
}
else 
{
label_1219101:; 
goto label_1219107;
}
}
else 
{
label_1219107:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_14 = req_a;
int i = __tmp_14;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1219157 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1219158 = x;
}
rsp_d = __return_1219157;
goto label_1219160;
rsp_d = __return_1219158;
label_1219160:; 
rsp_status = 1;
label_1219166:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1219247;
}
else 
{
goto label_1219188;
}
}
else 
{
label_1219188:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1219247;
}
else 
{
goto label_1219195;
}
}
else 
{
label_1219195:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1219247;
}
else 
{
goto label_1219202;
}
}
else 
{
label_1219202:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1219247;
}
else 
{
goto label_1219209;
}
}
else 
{
label_1219209:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1219247;
}
else 
{
goto label_1219216;
}
}
else 
{
label_1219216:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1219225;
}
else 
{
label_1219225:; 
goto label_1219247;
}
}
else 
{
label_1219247:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1219269;
}
else 
{
goto label_1219254;
}
}
else 
{
label_1219254:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1219263;
}
else 
{
label_1219263:; 
goto label_1219269;
}
}
else 
{
label_1219269:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1219352;
}
else 
{
goto label_1219293;
}
}
else 
{
label_1219293:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1219352;
}
else 
{
goto label_1219300;
}
}
else 
{
label_1219300:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1219352;
}
else 
{
goto label_1219307;
}
}
else 
{
label_1219307:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1219352;
}
else 
{
goto label_1219314;
}
}
else 
{
label_1219314:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1219352;
}
else 
{
goto label_1219321;
}
}
else 
{
label_1219321:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1219330;
}
else 
{
label_1219330:; 
goto label_1219352;
}
}
else 
{
label_1219352:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1219359;
}
}
else 
{
label_1219359:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1219368;
}
else 
{
label_1219368:; 
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
goto label_1217399;
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
if (((int)req_type) == 1)
{
{
int __tmp_15 = req_a;
int __tmp_16 = req_d;
int i = __tmp_15;
int v = __tmp_16;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1219135;
label_1219135:; 
rsp_status = 1;
goto label_1219166;
}
}
else 
{
rsp_status = 0;
goto label_1219166;
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
label_1219886:; 
label_1221890:; 
if (((int)m_run_st) == 0)
{
goto label_1221904;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1221904:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1221915;
}
else 
{
label_1221915:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1222382;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1222042;
}
else 
{
goto label_1221983;
}
}
else 
{
label_1221983:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1222042;
}
else 
{
goto label_1221990;
}
}
else 
{
label_1221990:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1222042;
}
else 
{
goto label_1221997;
}
}
else 
{
label_1221997:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1222042;
}
else 
{
goto label_1222004;
}
}
else 
{
label_1222004:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1222042;
}
else 
{
goto label_1222011;
}
}
else 
{
label_1222011:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1222020;
}
else 
{
label_1222020:; 
goto label_1222042;
}
}
else 
{
label_1222042:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1222064;
}
else 
{
goto label_1222049;
}
}
else 
{
label_1222049:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1222058;
}
else 
{
label_1222058:; 
goto label_1222064;
}
}
else 
{
label_1222064:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_17 = req_a;
int i = __tmp_17;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1222114 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1222115 = x;
}
rsp_d = __return_1222114;
goto label_1222117;
rsp_d = __return_1222115;
label_1222117:; 
rsp_status = 1;
label_1222123:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1222204;
}
else 
{
goto label_1222145;
}
}
else 
{
label_1222145:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1222204;
}
else 
{
goto label_1222152;
}
}
else 
{
label_1222152:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1222204;
}
else 
{
goto label_1222159;
}
}
else 
{
label_1222159:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1222204;
}
else 
{
goto label_1222166;
}
}
else 
{
label_1222166:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1222204;
}
else 
{
goto label_1222173;
}
}
else 
{
label_1222173:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1222182;
}
else 
{
label_1222182:; 
goto label_1222204;
}
}
else 
{
label_1222204:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1222226;
}
else 
{
goto label_1222211;
}
}
else 
{
label_1222211:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1222220;
}
else 
{
label_1222220:; 
goto label_1222226;
}
}
else 
{
label_1222226:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1222309;
}
else 
{
goto label_1222250;
}
}
else 
{
label_1222250:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1222309;
}
else 
{
goto label_1222257;
}
}
else 
{
label_1222257:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1222309;
}
else 
{
goto label_1222264;
}
}
else 
{
label_1222264:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1222309;
}
else 
{
goto label_1222271;
}
}
else 
{
label_1222271:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1222309;
}
else 
{
goto label_1222278;
}
}
else 
{
label_1222278:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1222287;
}
else 
{
label_1222287:; 
goto label_1222309;
}
}
else 
{
label_1222309:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1222316;
}
}
else 
{
label_1222316:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1222325;
}
else 
{
label_1222325:; 
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
goto label_1217399;
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
if (((int)req_type) == 1)
{
{
int __tmp_18 = req_a;
int __tmp_19 = req_d;
int i = __tmp_18;
int v = __tmp_19;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1222092;
label_1222092:; 
rsp_status = 1;
goto label_1222123;
}
}
else 
{
rsp_status = 0;
goto label_1222123;
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
label_1222382:; 
goto label_1221890;
}
}
}
else 
{
}
goto label_1209734;
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
a = 0;
req_type___0 = 0;
req_a___0 = a;
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218295;
}
else 
{
goto label_1218236;
}
}
else 
{
label_1218236:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218295;
}
else 
{
goto label_1218243;
}
}
else 
{
label_1218243:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218295;
}
else 
{
goto label_1218250;
}
}
else 
{
label_1218250:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218295;
}
else 
{
goto label_1218257;
}
}
else 
{
label_1218257:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218295;
}
else 
{
goto label_1218264;
}
}
else 
{
label_1218264:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218273;
}
else 
{
label_1218273:; 
goto label_1218295;
}
}
else 
{
label_1218295:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1218317;
}
else 
{
goto label_1218302;
}
}
else 
{
label_1218302:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1218311;
}
else 
{
label_1218311:; 
goto label_1218317;
}
}
else 
{
label_1218317:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218397;
}
else 
{
goto label_1218338;
}
}
else 
{
label_1218338:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218397;
}
else 
{
goto label_1218345;
}
}
else 
{
label_1218345:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218397;
}
else 
{
goto label_1218352;
}
}
else 
{
label_1218352:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218397;
}
else 
{
goto label_1218359;
}
}
else 
{
label_1218359:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218397;
}
else 
{
goto label_1218366;
}
}
else 
{
label_1218366:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218375;
}
else 
{
label_1218375:; 
goto label_1218397;
}
}
else 
{
label_1218397:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1218419;
}
else 
{
goto label_1218404;
}
}
else 
{
label_1218404:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1218413;
}
else 
{
label_1218413:; 
goto label_1218419;
}
}
else 
{
label_1218419:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218492;
}
else 
{
goto label_1218433;
}
}
else 
{
label_1218433:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218492;
}
else 
{
goto label_1218440;
}
}
else 
{
label_1218440:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218492;
}
else 
{
goto label_1218447;
}
}
else 
{
label_1218447:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1218492;
}
else 
{
goto label_1218454;
}
}
else 
{
label_1218454:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1218492;
}
else 
{
goto label_1218461;
}
}
else 
{
label_1218461:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1218470;
}
else 
{
label_1218470:; 
goto label_1218492;
}
}
else 
{
label_1218492:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1218499;
}
}
else 
{
label_1218499:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1218508;
}
else 
{
label_1218508:; 
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
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1219888;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1219542;
}
else 
{
goto label_1219483;
}
}
else 
{
label_1219483:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1219542;
}
else 
{
goto label_1219490;
}
}
else 
{
label_1219490:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1219542;
}
else 
{
goto label_1219497;
}
}
else 
{
label_1219497:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1219542;
}
else 
{
goto label_1219504;
}
}
else 
{
label_1219504:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1219542;
}
else 
{
goto label_1219511;
}
}
else 
{
label_1219511:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1219520;
}
else 
{
label_1219520:; 
goto label_1219542;
}
}
else 
{
label_1219542:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1219564;
}
else 
{
goto label_1219549;
}
}
else 
{
label_1219549:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1219558;
}
else 
{
label_1219558:; 
goto label_1219564;
}
}
else 
{
label_1219564:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_20 = req_a;
int i = __tmp_20;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1219614 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1219615 = x;
}
rsp_d = __return_1219614;
goto label_1219617;
rsp_d = __return_1219615;
label_1219617:; 
rsp_status = 1;
label_1219623:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1219704;
}
else 
{
goto label_1219645;
}
}
else 
{
label_1219645:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1219704;
}
else 
{
goto label_1219652;
}
}
else 
{
label_1219652:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1219704;
}
else 
{
goto label_1219659;
}
}
else 
{
label_1219659:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1219704;
}
else 
{
goto label_1219666;
}
}
else 
{
label_1219666:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1219704;
}
else 
{
goto label_1219673;
}
}
else 
{
label_1219673:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1219682;
}
else 
{
label_1219682:; 
goto label_1219704;
}
}
else 
{
label_1219704:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1219726;
}
else 
{
goto label_1219711;
}
}
else 
{
label_1219711:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1219720;
}
else 
{
label_1219720:; 
goto label_1219726;
}
}
else 
{
label_1219726:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1219809;
}
else 
{
goto label_1219750;
}
}
else 
{
label_1219750:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1219809;
}
else 
{
goto label_1219757;
}
}
else 
{
label_1219757:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1219809;
}
else 
{
goto label_1219764;
}
}
else 
{
label_1219764:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1219809;
}
else 
{
goto label_1219771;
}
}
else 
{
label_1219771:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1219809;
}
else 
{
goto label_1219778;
}
}
else 
{
label_1219778:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1219787;
}
else 
{
label_1219787:; 
goto label_1219809;
}
}
else 
{
label_1219809:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1219816;
}
}
else 
{
label_1219816:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1219825;
}
else 
{
label_1219825:; 
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
label_1219879:; 
label_1219893:; 
if (((int)m_run_st) == 0)
{
goto label_1219906;
}
else 
{
label_1219906:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1220262;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1220094;
}
else 
{
goto label_1220035;
}
}
else 
{
label_1220035:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1220094;
}
else 
{
goto label_1220042;
}
}
else 
{
label_1220042:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1220094;
}
else 
{
goto label_1220049;
}
}
else 
{
label_1220049:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1220094;
}
else 
{
goto label_1220056;
}
}
else 
{
label_1220056:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1220094;
}
else 
{
goto label_1220063;
}
}
else 
{
label_1220063:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1220072;
}
else 
{
label_1220072:; 
goto label_1220094;
}
}
else 
{
label_1220094:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1220116;
}
else 
{
goto label_1220101;
}
}
else 
{
label_1220101:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1220110;
}
else 
{
label_1220110:; 
goto label_1220116;
}
}
else 
{
label_1220116:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1220189;
}
else 
{
goto label_1220130;
}
}
else 
{
label_1220130:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1220189;
}
else 
{
goto label_1220137;
}
}
else 
{
label_1220137:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1220189;
}
else 
{
goto label_1220144;
}
}
else 
{
label_1220144:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1220189;
}
else 
{
goto label_1220151;
}
}
else 
{
label_1220151:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1220189;
}
else 
{
goto label_1220158;
}
}
else 
{
label_1220158:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1220167;
}
else 
{
label_1220167:; 
goto label_1220189;
}
}
else 
{
label_1220189:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1220211;
}
else 
{
goto label_1220196;
}
}
else 
{
label_1220196:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1220205;
}
else 
{
label_1220205:; 
goto label_1220211;
}
}
else 
{
label_1220211:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
a = a + 1;
goto label_1220228;
}
else 
{
{
__VERIFIER_error();
}
a = a + 1;
label_1220228:; 
}
label_1220257:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1220573;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1220316;
}
else 
{
if (((int)s_run_pc) == 2)
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1220475;
}
else 
{
goto label_1220357;
}
}
else 
{
label_1220357:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1220475;
}
else 
{
goto label_1220369;
}
}
else 
{
label_1220369:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1220475;
}
else 
{
goto label_1220385;
}
}
else 
{
label_1220385:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1220475;
}
else 
{
goto label_1220397;
}
}
else 
{
label_1220397:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1220475;
}
else 
{
goto label_1220413;
}
}
else 
{
label_1220413:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1220431;
}
else 
{
label_1220431:; 
goto label_1220475;
}
}
else 
{
label_1220475:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1220487;
}
}
else 
{
label_1220487:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1220505;
}
else 
{
label_1220505:; 
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
goto label_1220565;
}
else 
{
label_1220316:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1220473;
}
else 
{
goto label_1220355;
}
}
else 
{
label_1220355:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1220473;
}
else 
{
goto label_1220371;
}
}
else 
{
label_1220371:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1220473;
}
else 
{
goto label_1220383;
}
}
else 
{
label_1220383:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1220473;
}
else 
{
goto label_1220399;
}
}
else 
{
label_1220399:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1220473;
}
else 
{
goto label_1220411;
}
}
else 
{
label_1220411:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1220429;
}
else 
{
label_1220429:; 
goto label_1220473;
}
}
else 
{
label_1220473:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1220489;
}
}
else 
{
label_1220489:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1220507;
}
else 
{
label_1220507:; 
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
label_1220565:; 
label_1220578:; 
if (((int)m_run_st) == 0)
{
goto label_1220592;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1220592:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1220825;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1220780;
}
else 
{
goto label_1220721;
}
}
else 
{
label_1220721:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1220780;
}
else 
{
goto label_1220728;
}
}
else 
{
label_1220728:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1220780;
}
else 
{
goto label_1220735;
}
}
else 
{
label_1220735:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1220780;
}
else 
{
goto label_1220742;
}
}
else 
{
label_1220742:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1220780;
}
else 
{
goto label_1220749;
}
}
else 
{
label_1220749:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1220758;
}
else 
{
label_1220758:; 
goto label_1220780;
}
}
else 
{
label_1220780:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1220787;
}
}
else 
{
label_1220787:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1220796;
}
else 
{
label_1220796:; 
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
goto label_1220257;
}
}
}
}
else 
{
label_1220825:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_1220836;
}
else 
{
label_1220836:; 
goto label_1220578;
}
}
}
else 
{
}
goto label_1212678;
}
}
}
}
}
}
else 
{
label_1220573:; 
label_1220842:; 
if (((int)m_run_st) == 0)
{
goto label_1220856;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1220856:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1221089;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1221044;
}
else 
{
goto label_1220985;
}
}
else 
{
label_1220985:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1221044;
}
else 
{
goto label_1220992;
}
}
else 
{
label_1220992:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1221044;
}
else 
{
goto label_1220999;
}
}
else 
{
label_1220999:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1221044;
}
else 
{
goto label_1221006;
}
}
else 
{
label_1221006:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1221044;
}
else 
{
goto label_1221013;
}
}
else 
{
label_1221013:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1221022;
}
else 
{
label_1221022:; 
goto label_1221044;
}
}
else 
{
label_1221044:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1221051;
}
}
else 
{
label_1221051:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1221060;
}
else 
{
label_1221060:; 
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
goto label_1220257;
}
}
}
}
else 
{
label_1221089:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1221389;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1221135;
}
else 
{
if (((int)s_run_pc) == 2)
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1221294;
}
else 
{
goto label_1221176;
}
}
else 
{
label_1221176:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1221294;
}
else 
{
goto label_1221188;
}
}
else 
{
label_1221188:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1221294;
}
else 
{
goto label_1221204;
}
}
else 
{
label_1221204:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1221294;
}
else 
{
goto label_1221216;
}
}
else 
{
label_1221216:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1221294;
}
else 
{
goto label_1221232;
}
}
else 
{
label_1221232:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1221250;
}
else 
{
label_1221250:; 
goto label_1221294;
}
}
else 
{
label_1221294:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1221306;
}
}
else 
{
label_1221306:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1221324;
}
else 
{
label_1221324:; 
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
goto label_1220565;
}
else 
{
label_1221135:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1221292;
}
else 
{
goto label_1221174;
}
}
else 
{
label_1221174:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1221292;
}
else 
{
goto label_1221190;
}
}
else 
{
label_1221190:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1221292;
}
else 
{
goto label_1221202;
}
}
else 
{
label_1221202:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1221292;
}
else 
{
goto label_1221218;
}
}
else 
{
label_1221218:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1221292;
}
else 
{
goto label_1221230;
}
}
else 
{
label_1221230:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1221248;
}
else 
{
label_1221248:; 
goto label_1221292;
}
}
else 
{
label_1221292:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1221308;
}
}
else 
{
label_1221308:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1221326;
}
else 
{
label_1221326:; 
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
goto label_1220565;
}
}
}
}
}
else 
{
label_1221389:; 
goto label_1220842;
}
}
}
else 
{
}
goto label_1212942;
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
else 
{
label_1220262:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_1220575;
}
else 
{
label_1220575:; 
goto label_1219893;
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
if (((int)req_type) == 1)
{
{
int __tmp_21 = req_a;
int __tmp_22 = req_d;
int i = __tmp_21;
int v = __tmp_22;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1219592;
label_1219592:; 
rsp_status = 1;
goto label_1219623;
}
}
else 
{
rsp_status = 0;
goto label_1219623;
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
label_1219888:; 
label_1221395:; 
if (((int)m_run_st) == 0)
{
goto label_1221408;
}
else 
{
label_1221408:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1221419;
}
else 
{
label_1221419:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1221886;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1221546;
}
else 
{
goto label_1221487;
}
}
else 
{
label_1221487:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1221546;
}
else 
{
goto label_1221494;
}
}
else 
{
label_1221494:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1221546;
}
else 
{
goto label_1221501;
}
}
else 
{
label_1221501:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1221546;
}
else 
{
goto label_1221508;
}
}
else 
{
label_1221508:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1221546;
}
else 
{
goto label_1221515;
}
}
else 
{
label_1221515:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1221524;
}
else 
{
label_1221524:; 
goto label_1221546;
}
}
else 
{
label_1221546:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1221568;
}
else 
{
goto label_1221553;
}
}
else 
{
label_1221553:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1221562;
}
else 
{
label_1221562:; 
goto label_1221568;
}
}
else 
{
label_1221568:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_23 = req_a;
int i = __tmp_23;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1221618 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1221619 = x;
}
rsp_d = __return_1221618;
goto label_1221621;
rsp_d = __return_1221619;
label_1221621:; 
rsp_status = 1;
label_1221627:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1221708;
}
else 
{
goto label_1221649;
}
}
else 
{
label_1221649:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1221708;
}
else 
{
goto label_1221656;
}
}
else 
{
label_1221656:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1221708;
}
else 
{
goto label_1221663;
}
}
else 
{
label_1221663:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1221708;
}
else 
{
goto label_1221670;
}
}
else 
{
label_1221670:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1221708;
}
else 
{
goto label_1221677;
}
}
else 
{
label_1221677:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1221686;
}
else 
{
label_1221686:; 
goto label_1221708;
}
}
else 
{
label_1221708:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1221730;
}
else 
{
goto label_1221715;
}
}
else 
{
label_1221715:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1221724;
}
else 
{
label_1221724:; 
goto label_1221730;
}
}
else 
{
label_1221730:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1221813;
}
else 
{
goto label_1221754;
}
}
else 
{
label_1221754:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1221813;
}
else 
{
goto label_1221761;
}
}
else 
{
label_1221761:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1221813;
}
else 
{
goto label_1221768;
}
}
else 
{
label_1221768:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1221813;
}
else 
{
goto label_1221775;
}
}
else 
{
label_1221775:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1221813;
}
else 
{
goto label_1221782;
}
}
else 
{
label_1221782:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1221791;
}
else 
{
label_1221791:; 
goto label_1221813;
}
}
else 
{
label_1221813:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1221820;
}
}
else 
{
label_1221820:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1221829;
}
else 
{
label_1221829:; 
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
goto label_1219879;
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
if (((int)req_type) == 1)
{
{
int __tmp_24 = req_a;
int __tmp_25 = req_d;
int i = __tmp_24;
int v = __tmp_25;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1221596;
label_1221596:; 
rsp_status = 1;
goto label_1221627;
}
}
else 
{
rsp_status = 0;
goto label_1221627;
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
label_1221886:; 
goto label_1221395;
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
}
}
}
else 
{
label_1218942:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_1219890;
}
else 
{
label_1219890:; 
goto label_1217866;
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
if (((int)req_type) == 1)
{
{
int __tmp_26 = req_a;
int __tmp_27 = req_d;
int i = __tmp_26;
int v = __tmp_27;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1217112;
label_1217112:; 
rsp_status = 1;
goto label_1217143;
}
}
else 
{
rsp_status = 0;
goto label_1217143;
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
label_1217861:; 
goto label_1216586;
}
}
}
}
}
else 
{
label_1216927:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1217863;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1217437;
}
else 
{
label_1217437:; 
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1217519;
}
else 
{
goto label_1217460;
}
}
else 
{
label_1217460:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1217519;
}
else 
{
goto label_1217467;
}
}
else 
{
label_1217467:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1217519;
}
else 
{
goto label_1217474;
}
}
else 
{
label_1217474:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1217519;
}
else 
{
goto label_1217481;
}
}
else 
{
label_1217481:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1217519;
}
else 
{
goto label_1217488;
}
}
else 
{
label_1217488:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1217497;
}
else 
{
label_1217497:; 
goto label_1217519;
}
}
else 
{
label_1217519:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1217541;
}
else 
{
goto label_1217526;
}
}
else 
{
label_1217526:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1217535;
}
else 
{
label_1217535:; 
goto label_1217541;
}
}
else 
{
label_1217541:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_28 = req_a;
int i = __tmp_28;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1217591 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1217592 = x;
}
rsp_d = __return_1217591;
goto label_1217594;
rsp_d = __return_1217592;
label_1217594:; 
rsp_status = 1;
label_1217600:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1217681;
}
else 
{
goto label_1217622;
}
}
else 
{
label_1217622:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1217681;
}
else 
{
goto label_1217629;
}
}
else 
{
label_1217629:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1217681;
}
else 
{
goto label_1217636;
}
}
else 
{
label_1217636:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1217681;
}
else 
{
goto label_1217643;
}
}
else 
{
label_1217643:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1217681;
}
else 
{
goto label_1217650;
}
}
else 
{
label_1217650:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1217659;
}
else 
{
label_1217659:; 
goto label_1217681;
}
}
else 
{
label_1217681:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1217703;
}
else 
{
goto label_1217688;
}
}
else 
{
label_1217688:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1217697;
}
else 
{
label_1217697:; 
goto label_1217703;
}
}
else 
{
label_1217703:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1217786;
}
else 
{
goto label_1217727;
}
}
else 
{
label_1217727:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1217786;
}
else 
{
goto label_1217734;
}
}
else 
{
label_1217734:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1217786;
}
else 
{
goto label_1217741;
}
}
else 
{
label_1217741:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1217786;
}
else 
{
goto label_1217748;
}
}
else 
{
label_1217748:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1217786;
}
else 
{
goto label_1217755;
}
}
else 
{
label_1217755:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1217764;
}
else 
{
label_1217764:; 
goto label_1217786;
}
}
else 
{
label_1217786:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1217793;
}
}
else 
{
label_1217793:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1217802;
}
else 
{
label_1217802:; 
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
goto label_1217399;
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
if (((int)req_type) == 1)
{
{
int __tmp_29 = req_a;
int __tmp_30 = req_d;
int i = __tmp_29;
int v = __tmp_30;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1217569;
label_1217569:; 
rsp_status = 1;
goto label_1217600;
}
}
else 
{
rsp_status = 0;
goto label_1217600;
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
label_1217863:; 
goto label_1216586;
}
}
}
else 
{
}
goto label_1213994;
}
}
}
else 
{
}
goto label_1215464;
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
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1208558;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1207799;
}
else 
{
label_1207799:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1207881;
}
else 
{
goto label_1207822;
}
}
else 
{
label_1207822:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1207881;
}
else 
{
goto label_1207829;
}
}
else 
{
label_1207829:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1207881;
}
else 
{
goto label_1207836;
}
}
else 
{
label_1207836:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1207881;
}
else 
{
goto label_1207843;
}
}
else 
{
label_1207843:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1207881;
}
else 
{
goto label_1207850;
}
}
else 
{
label_1207850:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1207859;
}
else 
{
label_1207859:; 
goto label_1207881;
}
}
else 
{
label_1207881:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1207888;
}
}
else 
{
label_1207888:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1207897;
}
else 
{
label_1207897:; 
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
label_1207927:; 
goto label_1210219;
}
}
}
}
else 
{
label_1208558:; 
label_1214479:; 
if (((int)m_run_st) == 0)
{
goto label_1214493;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1214493:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1214504;
}
else 
{
label_1214504:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1214680;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1214549;
}
else 
{
label_1214549:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1214631;
}
else 
{
goto label_1214572;
}
}
else 
{
label_1214572:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1214631;
}
else 
{
goto label_1214579;
}
}
else 
{
label_1214579:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1214631;
}
else 
{
goto label_1214586;
}
}
else 
{
label_1214586:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1214631;
}
else 
{
goto label_1214593;
}
}
else 
{
label_1214593:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1214631;
}
else 
{
goto label_1214600;
}
}
else 
{
label_1214600:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1214609;
}
else 
{
label_1214609:; 
goto label_1214631;
}
}
else 
{
label_1214631:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1214638;
}
}
else 
{
label_1214638:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1214647;
}
else 
{
label_1214647:; 
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
goto label_1207927;
}
}
}
}
else 
{
label_1214680:; 
goto label_1214479;
}
}
}
else 
{
}
label_1214490:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215212;
}
else 
{
goto label_1214740;
}
}
else 
{
label_1214740:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215212;
}
else 
{
goto label_1214810;
}
}
else 
{
label_1214810:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215212;
}
else 
{
goto label_1214852;
}
}
else 
{
label_1214852:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215212;
}
else 
{
goto label_1214922;
}
}
else 
{
label_1214922:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215212;
}
else 
{
goto label_1214964;
}
}
else 
{
label_1214964:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215036;
}
else 
{
label_1215036:; 
goto label_1215212;
}
}
else 
{
label_1215212:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1215402;
}
else 
{
goto label_1215282;
}
}
else 
{
label_1215282:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1215354;
}
else 
{
label_1215354:; 
goto label_1215402;
}
}
else 
{
label_1215402:; 
if (((int)m_run_st) == 0)
{
goto label_1215498;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1215498:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1215510:; 
if (((int)m_run_st) == 0)
{
goto label_1215524;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1215524:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1215662;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1216015;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1215715;
}
else 
{
label_1215715:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215797;
}
else 
{
goto label_1215738;
}
}
else 
{
label_1215738:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215797;
}
else 
{
goto label_1215745;
}
}
else 
{
label_1215745:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215797;
}
else 
{
goto label_1215752;
}
}
else 
{
label_1215752:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215797;
}
else 
{
goto label_1215759;
}
}
else 
{
label_1215759:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215797;
}
else 
{
goto label_1215766;
}
}
else 
{
label_1215766:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215775;
}
else 
{
label_1215775:; 
goto label_1215797;
}
}
else 
{
label_1215797:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1215804;
}
}
else 
{
label_1215804:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1215813;
}
else 
{
label_1215813:; 
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
label_1215843:; 
goto label_1216021;
}
}
}
}
else 
{
label_1216015:; 
goto label_1215510;
}
}
}
}
else 
{
label_1215662:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1216017;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1215881;
}
else 
{
label_1215881:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215963;
}
else 
{
goto label_1215904;
}
}
else 
{
label_1215904:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215963;
}
else 
{
goto label_1215911;
}
}
else 
{
label_1215911:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215963;
}
else 
{
goto label_1215918;
}
}
else 
{
label_1215918:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215963;
}
else 
{
goto label_1215925;
}
}
else 
{
label_1215925:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215963;
}
else 
{
goto label_1215932;
}
}
else 
{
label_1215932:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215941;
}
else 
{
label_1215941:; 
goto label_1215963;
}
}
else 
{
label_1215963:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1215970;
}
}
else 
{
label_1215970:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1215979;
}
else 
{
label_1215979:; 
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
label_1216021:; 
if (((int)m_run_st) == 0)
{
goto label_1216035;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1216035:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1216173;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1216359;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1216308;
}
else 
{
goto label_1216249;
}
}
else 
{
label_1216249:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1216308;
}
else 
{
goto label_1216256;
}
}
else 
{
label_1216256:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1216308;
}
else 
{
goto label_1216263;
}
}
else 
{
label_1216263:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1216308;
}
else 
{
goto label_1216270;
}
}
else 
{
label_1216270:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1216308;
}
else 
{
goto label_1216277;
}
}
else 
{
label_1216277:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1216286;
}
else 
{
label_1216286:; 
goto label_1216308;
}
}
else 
{
label_1216308:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1216315;
}
}
else 
{
label_1216315:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1216324;
}
else 
{
label_1216324:; 
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
goto label_1215843;
}
}
}
}
else 
{
label_1216359:; 
label_1216365:; 
if (((int)m_run_st) == 0)
{
goto label_1216379;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1216379:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1216390;
}
else 
{
label_1216390:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1216566;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1216517;
}
else 
{
goto label_1216458;
}
}
else 
{
label_1216458:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1216517;
}
else 
{
goto label_1216465;
}
}
else 
{
label_1216465:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1216517;
}
else 
{
goto label_1216472;
}
}
else 
{
label_1216472:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1216517;
}
else 
{
goto label_1216479;
}
}
else 
{
label_1216479:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1216517;
}
else 
{
goto label_1216486;
}
}
else 
{
label_1216486:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1216495;
}
else 
{
label_1216495:; 
goto label_1216517;
}
}
else 
{
label_1216517:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1216524;
}
}
else 
{
label_1216524:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1216533;
}
else 
{
label_1216533:; 
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
goto label_1215843;
}
}
}
}
else 
{
label_1216566:; 
goto label_1216365;
}
}
}
else 
{
}
goto label_1210230;
}
}
}
}
}
else 
{
label_1216173:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_1216361;
}
else 
{
label_1216361:; 
goto label_1216021;
}
}
}
else 
{
}
goto label_1210230;
}
}
}
}
}
else 
{
label_1216017:; 
goto label_1215510;
}
}
}
else 
{
}
goto label_1214490;
}
}
}
else 
{
}
goto label_1215460;
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
label_1207738:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1208562;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1208422;
}
else 
{
label_1208422:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1208504;
}
else 
{
goto label_1208445;
}
}
else 
{
label_1208445:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1208504;
}
else 
{
goto label_1208452;
}
}
else 
{
label_1208452:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1208504;
}
else 
{
goto label_1208459;
}
}
else 
{
label_1208459:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1208504;
}
else 
{
goto label_1208466;
}
}
else 
{
label_1208466:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1208504;
}
else 
{
goto label_1208473;
}
}
else 
{
label_1208473:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1208482;
}
else 
{
label_1208482:; 
goto label_1208504;
}
}
else 
{
label_1208504:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1208511;
}
}
else 
{
label_1208511:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1208520;
}
else 
{
label_1208520:; 
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
label_1208567:; 
if (((int)m_run_st) == 0)
{
goto label_1208581;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1208581:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1209063;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
goto label_1208689;
}
else 
{
label_1208689:; 
a = 0;
req_type = 1;
req_a = a;
req_d = a + 50;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1208792;
}
else 
{
goto label_1208733;
}
}
else 
{
label_1208733:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1208792;
}
else 
{
goto label_1208740;
}
}
else 
{
label_1208740:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1208792;
}
else 
{
goto label_1208747;
}
}
else 
{
label_1208747:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1208792;
}
else 
{
goto label_1208754;
}
}
else 
{
label_1208754:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1208792;
}
else 
{
goto label_1208761;
}
}
else 
{
label_1208761:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1208770;
}
else 
{
label_1208770:; 
goto label_1208792;
}
}
else 
{
label_1208792:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1208814;
}
else 
{
goto label_1208799;
}
}
else 
{
label_1208799:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1208808;
}
else 
{
label_1208808:; 
goto label_1208814;
}
}
else 
{
label_1208814:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1208894;
}
else 
{
goto label_1208835;
}
}
else 
{
label_1208835:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1208894;
}
else 
{
goto label_1208842;
}
}
else 
{
label_1208842:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1208894;
}
else 
{
goto label_1208849;
}
}
else 
{
label_1208849:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1208894;
}
else 
{
goto label_1208856;
}
}
else 
{
label_1208856:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1208894;
}
else 
{
goto label_1208863;
}
}
else 
{
label_1208863:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1208872;
}
else 
{
label_1208872:; 
goto label_1208894;
}
}
else 
{
label_1208894:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1208916;
}
else 
{
goto label_1208901;
}
}
else 
{
label_1208901:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1208910;
}
else 
{
label_1208910:; 
goto label_1208916;
}
}
else 
{
label_1208916:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1208989;
}
else 
{
goto label_1208930;
}
}
else 
{
label_1208930:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1208989;
}
else 
{
goto label_1208937;
}
}
else 
{
label_1208937:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1208989;
}
else 
{
goto label_1208944;
}
}
else 
{
label_1208944:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1208989;
}
else 
{
goto label_1208951;
}
}
else 
{
label_1208951:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1208989;
}
else 
{
goto label_1208958;
}
}
else 
{
label_1208958:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1208967;
}
else 
{
label_1208967:; 
goto label_1208989;
}
}
else 
{
label_1208989:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1208996;
}
}
else 
{
label_1208996:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1209005;
}
else 
{
label_1209005:; 
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
label_1209055:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1209717;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1209372;
}
else 
{
goto label_1209313;
}
}
else 
{
label_1209313:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1209372;
}
else 
{
goto label_1209320;
}
}
else 
{
label_1209320:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1209372;
}
else 
{
goto label_1209327;
}
}
else 
{
label_1209327:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1209372;
}
else 
{
goto label_1209334;
}
}
else 
{
label_1209334:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1209372;
}
else 
{
goto label_1209341;
}
}
else 
{
label_1209341:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1209350;
}
else 
{
label_1209350:; 
goto label_1209372;
}
}
else 
{
label_1209372:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1209394;
}
else 
{
goto label_1209379;
}
}
else 
{
label_1209379:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1209388;
}
else 
{
label_1209388:; 
goto label_1209394;
}
}
else 
{
label_1209394:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_31 = req_a;
int i = __tmp_31;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1209444 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1209445 = x;
}
rsp_d = __return_1209444;
goto label_1209447;
rsp_d = __return_1209445;
label_1209447:; 
rsp_status = 1;
label_1209453:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1209534;
}
else 
{
goto label_1209475;
}
}
else 
{
label_1209475:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1209534;
}
else 
{
goto label_1209482;
}
}
else 
{
label_1209482:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1209534;
}
else 
{
goto label_1209489;
}
}
else 
{
label_1209489:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1209534;
}
else 
{
goto label_1209496;
}
}
else 
{
label_1209496:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1209534;
}
else 
{
goto label_1209503;
}
}
else 
{
label_1209503:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1209512;
}
else 
{
label_1209512:; 
goto label_1209534;
}
}
else 
{
label_1209534:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1209556;
}
else 
{
goto label_1209541;
}
}
else 
{
label_1209541:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1209550;
}
else 
{
label_1209550:; 
goto label_1209556;
}
}
else 
{
label_1209556:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1209639;
}
else 
{
goto label_1209580;
}
}
else 
{
label_1209580:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1209639;
}
else 
{
goto label_1209587;
}
}
else 
{
label_1209587:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1209639;
}
else 
{
goto label_1209594;
}
}
else 
{
label_1209594:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1209639;
}
else 
{
goto label_1209601;
}
}
else 
{
label_1209601:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1209639;
}
else 
{
goto label_1209608;
}
}
else 
{
label_1209608:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1209617;
}
else 
{
label_1209617:; 
goto label_1209639;
}
}
else 
{
label_1209639:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1209646;
}
}
else 
{
label_1209646:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1209655;
}
else 
{
label_1209655:; 
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
goto label_1208384;
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
if (((int)req_type) == 1)
{
{
int __tmp_32 = req_a;
int __tmp_33 = req_d;
int i = __tmp_32;
int v = __tmp_33;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1209422;
label_1209422:; 
rsp_status = 1;
goto label_1209453;
}
}
else 
{
rsp_status = 0;
goto label_1209453;
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
label_1209717:; 
label_1209723:; 
if (((int)m_run_st) == 0)
{
goto label_1209737;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1209737:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1209748;
}
else 
{
label_1209748:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1210215;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1209875;
}
else 
{
goto label_1209816;
}
}
else 
{
label_1209816:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1209875;
}
else 
{
goto label_1209823;
}
}
else 
{
label_1209823:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1209875;
}
else 
{
goto label_1209830;
}
}
else 
{
label_1209830:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1209875;
}
else 
{
goto label_1209837;
}
}
else 
{
label_1209837:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1209875;
}
else 
{
goto label_1209844;
}
}
else 
{
label_1209844:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1209853;
}
else 
{
label_1209853:; 
goto label_1209875;
}
}
else 
{
label_1209875:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1209897;
}
else 
{
goto label_1209882;
}
}
else 
{
label_1209882:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1209891;
}
else 
{
label_1209891:; 
goto label_1209897;
}
}
else 
{
label_1209897:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_34 = req_a;
int i = __tmp_34;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1209947 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1209948 = x;
}
rsp_d = __return_1209947;
goto label_1209950;
rsp_d = __return_1209948;
label_1209950:; 
rsp_status = 1;
label_1209956:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1210037;
}
else 
{
goto label_1209978;
}
}
else 
{
label_1209978:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1210037;
}
else 
{
goto label_1209985;
}
}
else 
{
label_1209985:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1210037;
}
else 
{
goto label_1209992;
}
}
else 
{
label_1209992:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1210037;
}
else 
{
goto label_1209999;
}
}
else 
{
label_1209999:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1210037;
}
else 
{
goto label_1210006;
}
}
else 
{
label_1210006:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1210015;
}
else 
{
label_1210015:; 
goto label_1210037;
}
}
else 
{
label_1210037:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1210059;
}
else 
{
goto label_1210044;
}
}
else 
{
label_1210044:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1210053;
}
else 
{
label_1210053:; 
goto label_1210059;
}
}
else 
{
label_1210059:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1210142;
}
else 
{
goto label_1210083;
}
}
else 
{
label_1210083:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1210142;
}
else 
{
goto label_1210090;
}
}
else 
{
label_1210090:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1210142;
}
else 
{
goto label_1210097;
}
}
else 
{
label_1210097:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1210142;
}
else 
{
goto label_1210104;
}
}
else 
{
label_1210104:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1210142;
}
else 
{
goto label_1210111;
}
}
else 
{
label_1210111:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1210120;
}
else 
{
label_1210120:; 
goto label_1210142;
}
}
else 
{
label_1210142:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1210149;
}
}
else 
{
label_1210149:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1210158;
}
else 
{
label_1210158:; 
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
goto label_1208384;
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
if (((int)req_type) == 1)
{
{
int __tmp_35 = req_a;
int __tmp_36 = req_d;
int i = __tmp_35;
int v = __tmp_36;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1209925;
label_1209925:; 
rsp_status = 1;
goto label_1209956;
}
}
else 
{
rsp_status = 0;
goto label_1209956;
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
label_1210215:; 
goto label_1209723;
}
}
}
else 
{
}
label_1209734:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215222;
}
else 
{
goto label_1214750;
}
}
else 
{
label_1214750:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215222;
}
else 
{
goto label_1214800;
}
}
else 
{
label_1214800:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215222;
}
else 
{
goto label_1214862;
}
}
else 
{
label_1214862:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215222;
}
else 
{
goto label_1214912;
}
}
else 
{
label_1214912:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215222;
}
else 
{
goto label_1214974;
}
}
else 
{
label_1214974:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215046;
}
else 
{
label_1215046:; 
goto label_1215222;
}
}
else 
{
label_1215222:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1215392;
}
else 
{
goto label_1215272;
}
}
else 
{
label_1215272:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1215344;
}
else 
{
label_1215344:; 
goto label_1215392;
}
}
else 
{
label_1215392:; 
if (((int)m_run_st) == 0)
{
goto label_1215488;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1215488:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1223804:; 
if (((int)m_run_st) == 0)
{
goto label_1223818;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1223818:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1224145;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1224006;
}
else 
{
goto label_1223947;
}
}
else 
{
label_1223947:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1224006;
}
else 
{
goto label_1223954;
}
}
else 
{
label_1223954:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1224006;
}
else 
{
goto label_1223961;
}
}
else 
{
label_1223961:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1224006;
}
else 
{
goto label_1223968;
}
}
else 
{
label_1223968:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1224006;
}
else 
{
goto label_1223975;
}
}
else 
{
label_1223975:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1223984;
}
else 
{
label_1223984:; 
goto label_1224006;
}
}
else 
{
label_1224006:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1224028;
}
else 
{
goto label_1224013;
}
}
else 
{
label_1224013:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1224022;
}
else 
{
label_1224022:; 
goto label_1224028;
}
}
else 
{
label_1224028:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1224101;
}
else 
{
goto label_1224042;
}
}
else 
{
label_1224042:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1224101;
}
else 
{
goto label_1224049;
}
}
else 
{
label_1224049:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1224101;
}
else 
{
goto label_1224056;
}
}
else 
{
label_1224056:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1224101;
}
else 
{
goto label_1224063;
}
}
else 
{
label_1224063:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1224101;
}
else 
{
goto label_1224070;
}
}
else 
{
label_1224070:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1224079;
}
else 
{
label_1224079:; 
goto label_1224101;
}
}
else 
{
label_1224101:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1224108;
}
}
else 
{
label_1224108:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1224117;
}
else 
{
label_1224117:; 
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
label_1224141:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1225079;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1224280;
}
else 
{
goto label_1224221;
}
}
else 
{
label_1224221:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1224280;
}
else 
{
goto label_1224228;
}
}
else 
{
label_1224228:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1224280;
}
else 
{
goto label_1224235;
}
}
else 
{
label_1224235:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1224280;
}
else 
{
goto label_1224242;
}
}
else 
{
label_1224242:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1224280;
}
else 
{
goto label_1224249;
}
}
else 
{
label_1224249:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1224258;
}
else 
{
label_1224258:; 
goto label_1224280;
}
}
else 
{
label_1224280:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1224302;
}
else 
{
goto label_1224287;
}
}
else 
{
label_1224287:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1224296;
}
else 
{
label_1224296:; 
goto label_1224302;
}
}
else 
{
label_1224302:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_37 = req_a;
int i = __tmp_37;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1224352 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1224353 = x;
}
rsp_d = __return_1224352;
goto label_1224355;
rsp_d = __return_1224353;
label_1224355:; 
rsp_status = 1;
label_1224361:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1224442;
}
else 
{
goto label_1224383;
}
}
else 
{
label_1224383:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1224442;
}
else 
{
goto label_1224390;
}
}
else 
{
label_1224390:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1224442;
}
else 
{
goto label_1224397;
}
}
else 
{
label_1224397:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1224442;
}
else 
{
goto label_1224404;
}
}
else 
{
label_1224404:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1224442;
}
else 
{
goto label_1224411;
}
}
else 
{
label_1224411:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1224420;
}
else 
{
label_1224420:; 
goto label_1224442;
}
}
else 
{
label_1224442:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1224464;
}
else 
{
goto label_1224449;
}
}
else 
{
label_1224449:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1224458;
}
else 
{
label_1224458:; 
goto label_1224464;
}
}
else 
{
label_1224464:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1224547;
}
else 
{
goto label_1224488;
}
}
else 
{
label_1224488:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1224547;
}
else 
{
goto label_1224495;
}
}
else 
{
label_1224495:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1224547;
}
else 
{
goto label_1224502;
}
}
else 
{
label_1224502:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1224547;
}
else 
{
goto label_1224509;
}
}
else 
{
label_1224509:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1224547;
}
else 
{
goto label_1224516;
}
}
else 
{
label_1224516:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1224525;
}
else 
{
label_1224525:; 
goto label_1224547;
}
}
else 
{
label_1224547:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1224554;
}
}
else 
{
label_1224554:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1224563;
}
else 
{
label_1224563:; 
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
label_1224617:; 
label_1225084:; 
if (((int)m_run_st) == 0)
{
goto label_1225097;
}
else 
{
label_1225097:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1226159;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225285;
}
else 
{
goto label_1225226;
}
}
else 
{
label_1225226:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225285;
}
else 
{
goto label_1225233;
}
}
else 
{
label_1225233:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1225285;
}
else 
{
goto label_1225240;
}
}
else 
{
label_1225240:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225285;
}
else 
{
goto label_1225247;
}
}
else 
{
label_1225247:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225285;
}
else 
{
goto label_1225254;
}
}
else 
{
label_1225254:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1225263;
}
else 
{
label_1225263:; 
goto label_1225285;
}
}
else 
{
label_1225285:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1225307;
}
else 
{
goto label_1225292;
}
}
else 
{
label_1225292:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1225301;
}
else 
{
label_1225301:; 
goto label_1225307;
}
}
else 
{
label_1225307:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225380;
}
else 
{
goto label_1225321;
}
}
else 
{
label_1225321:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225380;
}
else 
{
goto label_1225328;
}
}
else 
{
label_1225328:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1225380;
}
else 
{
goto label_1225335;
}
}
else 
{
label_1225335:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225380;
}
else 
{
goto label_1225342;
}
}
else 
{
label_1225342:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225380;
}
else 
{
goto label_1225349;
}
}
else 
{
label_1225349:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1225358;
}
else 
{
label_1225358:; 
goto label_1225380;
}
}
else 
{
label_1225380:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1225402;
}
else 
{
goto label_1225387;
}
}
else 
{
label_1225387:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1225396;
}
else 
{
label_1225396:; 
goto label_1225402;
}
}
else 
{
label_1225402:; 
c_m_ev = 2;
a = a + 1;
if (a < 1)
{
req_type = 1;
req_a = a;
req_d = a + 50;
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225867;
}
else 
{
goto label_1225808;
}
}
else 
{
label_1225808:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225867;
}
else 
{
goto label_1225815;
}
}
else 
{
label_1225815:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1225867;
}
else 
{
goto label_1225822;
}
}
else 
{
label_1225822:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225867;
}
else 
{
goto label_1225829;
}
}
else 
{
label_1225829:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225867;
}
else 
{
goto label_1225836;
}
}
else 
{
label_1225836:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1225845;
}
else 
{
label_1225845:; 
goto label_1225867;
}
}
else 
{
label_1225867:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1225889;
}
else 
{
goto label_1225874;
}
}
else 
{
label_1225874:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1225883;
}
else 
{
label_1225883:; 
goto label_1225889;
}
}
else 
{
label_1225889:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225969;
}
else 
{
goto label_1225910;
}
}
else 
{
label_1225910:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225969;
}
else 
{
goto label_1225917;
}
}
else 
{
label_1225917:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1225969;
}
else 
{
goto label_1225924;
}
}
else 
{
label_1225924:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225969;
}
else 
{
goto label_1225931;
}
}
else 
{
label_1225931:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225969;
}
else 
{
goto label_1225938;
}
}
else 
{
label_1225938:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1225947;
}
else 
{
label_1225947:; 
goto label_1225969;
}
}
else 
{
label_1225969:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1225991;
}
else 
{
goto label_1225976;
}
}
else 
{
label_1225976:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1225985;
}
else 
{
label_1225985:; 
goto label_1225991;
}
}
else 
{
label_1225991:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1226064;
}
else 
{
goto label_1226005;
}
}
else 
{
label_1226005:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1226064;
}
else 
{
goto label_1226012;
}
}
else 
{
label_1226012:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1226064;
}
else 
{
goto label_1226019;
}
}
else 
{
label_1226019:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1226064;
}
else 
{
goto label_1226026;
}
}
else 
{
label_1226026:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1226064;
}
else 
{
goto label_1226033;
}
}
else 
{
label_1226033:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1226042;
}
else 
{
label_1226042:; 
goto label_1226064;
}
}
else 
{
label_1226064:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1226071;
}
}
else 
{
label_1226071:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1226080;
}
else 
{
label_1226080:; 
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
goto label_1224141;
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
a = 0;
req_type___0 = 0;
req_a___0 = a;
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225513;
}
else 
{
goto label_1225454;
}
}
else 
{
label_1225454:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225513;
}
else 
{
goto label_1225461;
}
}
else 
{
label_1225461:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1225513;
}
else 
{
goto label_1225468;
}
}
else 
{
label_1225468:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225513;
}
else 
{
goto label_1225475;
}
}
else 
{
label_1225475:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225513;
}
else 
{
goto label_1225482;
}
}
else 
{
label_1225482:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1225491;
}
else 
{
label_1225491:; 
goto label_1225513;
}
}
else 
{
label_1225513:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1225535;
}
else 
{
goto label_1225520;
}
}
else 
{
label_1225520:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1225529;
}
else 
{
label_1225529:; 
goto label_1225535;
}
}
else 
{
label_1225535:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225615;
}
else 
{
goto label_1225556;
}
}
else 
{
label_1225556:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225615;
}
else 
{
goto label_1225563;
}
}
else 
{
label_1225563:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1225615;
}
else 
{
goto label_1225570;
}
}
else 
{
label_1225570:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225615;
}
else 
{
goto label_1225577;
}
}
else 
{
label_1225577:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225615;
}
else 
{
goto label_1225584;
}
}
else 
{
label_1225584:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1225593;
}
else 
{
label_1225593:; 
goto label_1225615;
}
}
else 
{
label_1225615:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1225637;
}
else 
{
goto label_1225622;
}
}
else 
{
label_1225622:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1225631;
}
else 
{
label_1225631:; 
goto label_1225637;
}
}
else 
{
label_1225637:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225710;
}
else 
{
goto label_1225651;
}
}
else 
{
label_1225651:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225710;
}
else 
{
goto label_1225658;
}
}
else 
{
label_1225658:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1225710;
}
else 
{
goto label_1225665;
}
}
else 
{
label_1225665:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225710;
}
else 
{
goto label_1225672;
}
}
else 
{
label_1225672:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225710;
}
else 
{
goto label_1225679;
}
}
else 
{
label_1225679:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1225688;
}
else 
{
label_1225688:; 
goto label_1225710;
}
}
else 
{
label_1225710:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1225717;
}
}
else 
{
label_1225717:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1225726;
}
else 
{
label_1225726:; 
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
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1226637;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1226294;
}
else 
{
goto label_1226235;
}
}
else 
{
label_1226235:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1226294;
}
else 
{
goto label_1226242;
}
}
else 
{
label_1226242:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1226294;
}
else 
{
goto label_1226249;
}
}
else 
{
label_1226249:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1226294;
}
else 
{
goto label_1226256;
}
}
else 
{
label_1226256:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1226294;
}
else 
{
goto label_1226263;
}
}
else 
{
label_1226263:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1226272;
}
else 
{
label_1226272:; 
goto label_1226294;
}
}
else 
{
label_1226294:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1226316;
}
else 
{
goto label_1226301;
}
}
else 
{
label_1226301:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1226310;
}
else 
{
label_1226310:; 
goto label_1226316;
}
}
else 
{
label_1226316:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_38 = req_a;
int i = __tmp_38;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1226366 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1226367 = x;
}
rsp_d = __return_1226366;
goto label_1226369;
rsp_d = __return_1226367;
label_1226369:; 
rsp_status = 1;
label_1226375:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1226456;
}
else 
{
goto label_1226397;
}
}
else 
{
label_1226397:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1226456;
}
else 
{
goto label_1226404;
}
}
else 
{
label_1226404:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1226456;
}
else 
{
goto label_1226411;
}
}
else 
{
label_1226411:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1226456;
}
else 
{
goto label_1226418;
}
}
else 
{
label_1226418:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1226456;
}
else 
{
goto label_1226425;
}
}
else 
{
label_1226425:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1226434;
}
else 
{
label_1226434:; 
goto label_1226456;
}
}
else 
{
label_1226456:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1226478;
}
else 
{
goto label_1226463;
}
}
else 
{
label_1226463:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1226472;
}
else 
{
label_1226472:; 
goto label_1226478;
}
}
else 
{
label_1226478:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1226561;
}
else 
{
goto label_1226502;
}
}
else 
{
label_1226502:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1226561;
}
else 
{
goto label_1226509;
}
}
else 
{
label_1226509:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1226561;
}
else 
{
goto label_1226516;
}
}
else 
{
label_1226516:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1226561;
}
else 
{
goto label_1226523;
}
}
else 
{
label_1226523:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1226561;
}
else 
{
goto label_1226530;
}
}
else 
{
label_1226530:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1226539;
}
else 
{
label_1226539:; 
goto label_1226561;
}
}
else 
{
label_1226561:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1226568;
}
}
else 
{
label_1226568:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1226577;
}
else 
{
label_1226577:; 
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
label_1226631:; 
label_1226642:; 
if (((int)m_run_st) == 0)
{
goto label_1226655;
}
else 
{
label_1226655:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1227011;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1226843;
}
else 
{
goto label_1226784;
}
}
else 
{
label_1226784:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1226843;
}
else 
{
goto label_1226791;
}
}
else 
{
label_1226791:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1226843;
}
else 
{
goto label_1226798;
}
}
else 
{
label_1226798:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1226843;
}
else 
{
goto label_1226805;
}
}
else 
{
label_1226805:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1226843;
}
else 
{
goto label_1226812;
}
}
else 
{
label_1226812:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1226821;
}
else 
{
label_1226821:; 
goto label_1226843;
}
}
else 
{
label_1226843:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1226865;
}
else 
{
goto label_1226850;
}
}
else 
{
label_1226850:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1226859;
}
else 
{
label_1226859:; 
goto label_1226865;
}
}
else 
{
label_1226865:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1226938;
}
else 
{
goto label_1226879;
}
}
else 
{
label_1226879:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1226938;
}
else 
{
goto label_1226886;
}
}
else 
{
label_1226886:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1226938;
}
else 
{
goto label_1226893;
}
}
else 
{
label_1226893:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1226938;
}
else 
{
goto label_1226900;
}
}
else 
{
label_1226900:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1226938;
}
else 
{
goto label_1226907;
}
}
else 
{
label_1226907:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1226916;
}
else 
{
label_1226916:; 
goto label_1226938;
}
}
else 
{
label_1226938:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1226960;
}
else 
{
goto label_1226945;
}
}
else 
{
label_1226945:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1226954;
}
else 
{
label_1226954:; 
goto label_1226960;
}
}
else 
{
label_1226960:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
a = a + 1;
goto label_1226977;
}
else 
{
{
__VERIFIER_error();
}
a = a + 1;
label_1226977:; 
}
label_1227006:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1227322;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1227065;
}
else 
{
if (((int)s_run_pc) == 2)
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1227224;
}
else 
{
goto label_1227106;
}
}
else 
{
label_1227106:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1227224;
}
else 
{
goto label_1227118;
}
}
else 
{
label_1227118:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1227224;
}
else 
{
goto label_1227134;
}
}
else 
{
label_1227134:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1227224;
}
else 
{
goto label_1227146;
}
}
else 
{
label_1227146:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1227224;
}
else 
{
goto label_1227162;
}
}
else 
{
label_1227162:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1227180;
}
else 
{
label_1227180:; 
goto label_1227224;
}
}
else 
{
label_1227224:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1227236;
}
}
else 
{
label_1227236:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1227254;
}
else 
{
label_1227254:; 
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
goto label_1227314;
}
else 
{
label_1227065:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1227222;
}
else 
{
goto label_1227104;
}
}
else 
{
label_1227104:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1227222;
}
else 
{
goto label_1227120;
}
}
else 
{
label_1227120:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1227222;
}
else 
{
goto label_1227132;
}
}
else 
{
label_1227132:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1227222;
}
else 
{
goto label_1227148;
}
}
else 
{
label_1227148:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1227222;
}
else 
{
goto label_1227160;
}
}
else 
{
label_1227160:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1227178;
}
else 
{
label_1227178:; 
goto label_1227222;
}
}
else 
{
label_1227222:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1227238;
}
}
else 
{
label_1227238:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1227256;
}
else 
{
label_1227256:; 
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
label_1227314:; 
label_1227327:; 
if (((int)m_run_st) == 0)
{
goto label_1227341;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1227341:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1227574;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1227529;
}
else 
{
goto label_1227470;
}
}
else 
{
label_1227470:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1227529;
}
else 
{
goto label_1227477;
}
}
else 
{
label_1227477:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1227529;
}
else 
{
goto label_1227484;
}
}
else 
{
label_1227484:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1227529;
}
else 
{
goto label_1227491;
}
}
else 
{
label_1227491:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1227529;
}
else 
{
goto label_1227498;
}
}
else 
{
label_1227498:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1227507;
}
else 
{
label_1227507:; 
goto label_1227529;
}
}
else 
{
label_1227529:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1227536;
}
}
else 
{
label_1227536:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1227545;
}
else 
{
label_1227545:; 
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
goto label_1227006;
}
}
}
}
else 
{
label_1227574:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_1227585;
}
else 
{
label_1227585:; 
goto label_1227327;
}
}
}
else 
{
}
goto label_1212678;
}
}
}
}
}
}
else 
{
label_1227322:; 
label_1227591:; 
if (((int)m_run_st) == 0)
{
goto label_1227605;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1227605:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1227838;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1227793;
}
else 
{
goto label_1227734;
}
}
else 
{
label_1227734:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1227793;
}
else 
{
goto label_1227741;
}
}
else 
{
label_1227741:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1227793;
}
else 
{
goto label_1227748;
}
}
else 
{
label_1227748:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1227793;
}
else 
{
goto label_1227755;
}
}
else 
{
label_1227755:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1227793;
}
else 
{
goto label_1227762;
}
}
else 
{
label_1227762:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1227771;
}
else 
{
label_1227771:; 
goto label_1227793;
}
}
else 
{
label_1227793:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1227800;
}
}
else 
{
label_1227800:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1227809;
}
else 
{
label_1227809:; 
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
goto label_1227006;
}
}
}
}
else 
{
label_1227838:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1228138;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1227884;
}
else 
{
if (((int)s_run_pc) == 2)
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1228043;
}
else 
{
goto label_1227925;
}
}
else 
{
label_1227925:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1228043;
}
else 
{
goto label_1227937;
}
}
else 
{
label_1227937:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1228043;
}
else 
{
goto label_1227953;
}
}
else 
{
label_1227953:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1228043;
}
else 
{
goto label_1227965;
}
}
else 
{
label_1227965:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1228043;
}
else 
{
goto label_1227981;
}
}
else 
{
label_1227981:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1227999;
}
else 
{
label_1227999:; 
goto label_1228043;
}
}
else 
{
label_1228043:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1228055;
}
}
else 
{
label_1228055:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1228073;
}
else 
{
label_1228073:; 
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
goto label_1227314;
}
else 
{
label_1227884:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1228041;
}
else 
{
goto label_1227923;
}
}
else 
{
label_1227923:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1228041;
}
else 
{
goto label_1227939;
}
}
else 
{
label_1227939:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1228041;
}
else 
{
goto label_1227951;
}
}
else 
{
label_1227951:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1228041;
}
else 
{
goto label_1227967;
}
}
else 
{
label_1227967:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1228041;
}
else 
{
goto label_1227979;
}
}
else 
{
label_1227979:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1227997;
}
else 
{
label_1227997:; 
goto label_1228041;
}
}
else 
{
label_1228041:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1228057;
}
}
else 
{
label_1228057:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1228075;
}
else 
{
label_1228075:; 
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
goto label_1227314;
}
}
}
}
}
else 
{
label_1228138:; 
goto label_1227591;
}
}
}
else 
{
}
goto label_1212942;
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
else 
{
label_1227011:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_1227324;
}
else 
{
label_1227324:; 
goto label_1226642;
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
if (((int)req_type) == 1)
{
{
int __tmp_39 = req_a;
int __tmp_40 = req_d;
int i = __tmp_39;
int v = __tmp_40;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1226344;
label_1226344:; 
rsp_status = 1;
goto label_1226375;
}
}
else 
{
rsp_status = 0;
goto label_1226375;
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
label_1226637:; 
label_1228144:; 
if (((int)m_run_st) == 0)
{
goto label_1228157;
}
else 
{
label_1228157:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1228168;
}
else 
{
label_1228168:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1228635;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1228295;
}
else 
{
goto label_1228236;
}
}
else 
{
label_1228236:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1228295;
}
else 
{
goto label_1228243;
}
}
else 
{
label_1228243:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1228295;
}
else 
{
goto label_1228250;
}
}
else 
{
label_1228250:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1228295;
}
else 
{
goto label_1228257;
}
}
else 
{
label_1228257:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1228295;
}
else 
{
goto label_1228264;
}
}
else 
{
label_1228264:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1228273;
}
else 
{
label_1228273:; 
goto label_1228295;
}
}
else 
{
label_1228295:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1228317;
}
else 
{
goto label_1228302;
}
}
else 
{
label_1228302:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1228311;
}
else 
{
label_1228311:; 
goto label_1228317;
}
}
else 
{
label_1228317:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_41 = req_a;
int i = __tmp_41;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1228367 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1228368 = x;
}
rsp_d = __return_1228367;
goto label_1228370;
rsp_d = __return_1228368;
label_1228370:; 
rsp_status = 1;
label_1228376:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1228457;
}
else 
{
goto label_1228398;
}
}
else 
{
label_1228398:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1228457;
}
else 
{
goto label_1228405;
}
}
else 
{
label_1228405:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1228457;
}
else 
{
goto label_1228412;
}
}
else 
{
label_1228412:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1228457;
}
else 
{
goto label_1228419;
}
}
else 
{
label_1228419:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1228457;
}
else 
{
goto label_1228426;
}
}
else 
{
label_1228426:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1228435;
}
else 
{
label_1228435:; 
goto label_1228457;
}
}
else 
{
label_1228457:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1228479;
}
else 
{
goto label_1228464;
}
}
else 
{
label_1228464:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1228473;
}
else 
{
label_1228473:; 
goto label_1228479;
}
}
else 
{
label_1228479:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1228562;
}
else 
{
goto label_1228503;
}
}
else 
{
label_1228503:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1228562;
}
else 
{
goto label_1228510;
}
}
else 
{
label_1228510:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1228562;
}
else 
{
goto label_1228517;
}
}
else 
{
label_1228517:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1228562;
}
else 
{
goto label_1228524;
}
}
else 
{
label_1228524:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1228562;
}
else 
{
goto label_1228531;
}
}
else 
{
label_1228531:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1228540;
}
else 
{
label_1228540:; 
goto label_1228562;
}
}
else 
{
label_1228562:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1228569;
}
}
else 
{
label_1228569:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1228578;
}
else 
{
label_1228578:; 
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
goto label_1226631;
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
if (((int)req_type) == 1)
{
{
int __tmp_42 = req_a;
int __tmp_43 = req_d;
int i = __tmp_42;
int v = __tmp_43;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1228345;
label_1228345:; 
rsp_status = 1;
goto label_1228376;
}
}
else 
{
rsp_status = 0;
goto label_1228376;
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
label_1228635:; 
goto label_1228144;
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
}
}
}
else 
{
label_1226159:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_1226639;
}
else 
{
label_1226639:; 
goto label_1225084;
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
if (((int)req_type) == 1)
{
{
int __tmp_44 = req_a;
int __tmp_45 = req_d;
int i = __tmp_44;
int v = __tmp_45;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1224330;
label_1224330:; 
rsp_status = 1;
goto label_1224361;
}
}
else 
{
rsp_status = 0;
goto label_1224361;
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
label_1225079:; 
goto label_1223804;
}
}
}
}
}
else 
{
label_1224145:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1225081;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1224737;
}
else 
{
goto label_1224678;
}
}
else 
{
label_1224678:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1224737;
}
else 
{
goto label_1224685;
}
}
else 
{
label_1224685:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1224737;
}
else 
{
goto label_1224692;
}
}
else 
{
label_1224692:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1224737;
}
else 
{
goto label_1224699;
}
}
else 
{
label_1224699:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1224737;
}
else 
{
goto label_1224706;
}
}
else 
{
label_1224706:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1224715;
}
else 
{
label_1224715:; 
goto label_1224737;
}
}
else 
{
label_1224737:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1224759;
}
else 
{
goto label_1224744;
}
}
else 
{
label_1224744:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1224753;
}
else 
{
label_1224753:; 
goto label_1224759;
}
}
else 
{
label_1224759:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_46 = req_a;
int i = __tmp_46;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1224809 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1224810 = x;
}
rsp_d = __return_1224809;
goto label_1224812;
rsp_d = __return_1224810;
label_1224812:; 
rsp_status = 1;
label_1224818:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1224899;
}
else 
{
goto label_1224840;
}
}
else 
{
label_1224840:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1224899;
}
else 
{
goto label_1224847;
}
}
else 
{
label_1224847:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1224899;
}
else 
{
goto label_1224854;
}
}
else 
{
label_1224854:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1224899;
}
else 
{
goto label_1224861;
}
}
else 
{
label_1224861:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1224899;
}
else 
{
goto label_1224868;
}
}
else 
{
label_1224868:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1224877;
}
else 
{
label_1224877:; 
goto label_1224899;
}
}
else 
{
label_1224899:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1224921;
}
else 
{
goto label_1224906;
}
}
else 
{
label_1224906:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1224915;
}
else 
{
label_1224915:; 
goto label_1224921;
}
}
else 
{
label_1224921:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225004;
}
else 
{
goto label_1224945;
}
}
else 
{
label_1224945:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225004;
}
else 
{
goto label_1224952;
}
}
else 
{
label_1224952:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1225004;
}
else 
{
goto label_1224959;
}
}
else 
{
label_1224959:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1225004;
}
else 
{
goto label_1224966;
}
}
else 
{
label_1224966:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1225004;
}
else 
{
goto label_1224973;
}
}
else 
{
label_1224973:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1224982;
}
else 
{
label_1224982:; 
goto label_1225004;
}
}
else 
{
label_1225004:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1225011;
}
}
else 
{
label_1225011:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1225020;
}
else 
{
label_1225020:; 
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
goto label_1224617;
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
if (((int)req_type) == 1)
{
{
int __tmp_47 = req_a;
int __tmp_48 = req_d;
int i = __tmp_47;
int v = __tmp_48;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1224787;
label_1224787:; 
rsp_status = 1;
goto label_1224818;
}
}
else 
{
rsp_status = 0;
goto label_1224818;
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
label_1225081:; 
goto label_1223804;
}
}
}
else 
{
}
goto label_1209734;
}
}
}
else 
{
}
label_1215464:; 
__retres1 = 0;
 __return_1234283 = __retres1;
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
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1209715;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1209206;
}
else 
{
goto label_1209147;
}
}
else 
{
label_1209147:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1209206;
}
else 
{
goto label_1209154;
}
}
else 
{
label_1209154:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1209206;
}
else 
{
goto label_1209161;
}
}
else 
{
label_1209161:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1209206;
}
else 
{
goto label_1209168;
}
}
else 
{
label_1209168:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1209206;
}
else 
{
goto label_1209175;
}
}
else 
{
label_1209175:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1209184;
}
else 
{
label_1209184:; 
goto label_1209206;
}
}
else 
{
label_1209206:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1209213;
}
}
else 
{
label_1209213:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1209222;
}
else 
{
label_1209222:; 
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
goto label_1207927;
}
}
}
}
else 
{
label_1209715:; 
label_1210219:; 
if (((int)m_run_st) == 0)
{
goto label_1210233;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1210233:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1210244;
}
else 
{
label_1210244:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1210420;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1210371;
}
else 
{
goto label_1210312;
}
}
else 
{
label_1210312:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1210371;
}
else 
{
goto label_1210319;
}
}
else 
{
label_1210319:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1210371;
}
else 
{
goto label_1210326;
}
}
else 
{
label_1210326:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1210371;
}
else 
{
goto label_1210333;
}
}
else 
{
label_1210333:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1210371;
}
else 
{
goto label_1210340;
}
}
else 
{
label_1210340:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1210349;
}
else 
{
label_1210349:; 
goto label_1210371;
}
}
else 
{
label_1210371:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1210378;
}
}
else 
{
label_1210378:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1210387;
}
else 
{
label_1210387:; 
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
goto label_1207927;
}
}
}
}
else 
{
label_1210420:; 
goto label_1210219;
}
}
}
else 
{
}
label_1210230:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215220;
}
else 
{
goto label_1214748;
}
}
else 
{
label_1214748:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215220;
}
else 
{
goto label_1214802;
}
}
else 
{
label_1214802:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215220;
}
else 
{
goto label_1214860;
}
}
else 
{
label_1214860:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215220;
}
else 
{
goto label_1214914;
}
}
else 
{
label_1214914:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215220;
}
else 
{
goto label_1214972;
}
}
else 
{
label_1214972:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215044;
}
else 
{
label_1215044:; 
goto label_1215220;
}
}
else 
{
label_1215220:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1215394;
}
else 
{
goto label_1215274;
}
}
else 
{
label_1215274:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1215346;
}
else 
{
label_1215346:; 
goto label_1215394;
}
}
else 
{
label_1215394:; 
if (((int)m_run_st) == 0)
{
goto label_1215490;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1215490:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1223275:; 
if (((int)m_run_st) == 0)
{
goto label_1223289;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1223289:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1223427;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1223780;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1223562;
}
else 
{
goto label_1223503;
}
}
else 
{
label_1223503:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1223562;
}
else 
{
goto label_1223510;
}
}
else 
{
label_1223510:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1223562;
}
else 
{
goto label_1223517;
}
}
else 
{
label_1223517:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1223562;
}
else 
{
goto label_1223524;
}
}
else 
{
label_1223524:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1223562;
}
else 
{
goto label_1223531;
}
}
else 
{
label_1223531:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1223540;
}
else 
{
label_1223540:; 
goto label_1223562;
}
}
else 
{
label_1223562:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1223569;
}
}
else 
{
label_1223569:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1223578;
}
else 
{
label_1223578:; 
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
goto label_1223275;
}
}
}
}
else 
{
label_1223780:; 
goto label_1223275;
}
}
}
}
else 
{
label_1223427:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1223782;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1223728;
}
else 
{
goto label_1223669;
}
}
else 
{
label_1223669:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1223728;
}
else 
{
goto label_1223676;
}
}
else 
{
label_1223676:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1223728;
}
else 
{
goto label_1223683;
}
}
else 
{
label_1223683:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1223728;
}
else 
{
goto label_1223690;
}
}
else 
{
label_1223690:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1223728;
}
else 
{
goto label_1223697;
}
}
else 
{
label_1223697:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1223706;
}
else 
{
label_1223706:; 
goto label_1223728;
}
}
else 
{
label_1223728:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1223735;
}
}
else 
{
label_1223735:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1223744;
}
else 
{
label_1223744:; 
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
goto label_1223275;
}
}
}
}
else 
{
label_1223782:; 
goto label_1223275;
}
}
}
else 
{
}
goto label_1210230;
}
}
}
else 
{
}
goto label_1215460;
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
label_1209063:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_1209719;
}
else 
{
label_1209719:; 
goto label_1208567;
}
}
}
else 
{
}
label_1208578:; 
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215224;
}
else 
{
goto label_1214752;
}
}
else 
{
label_1214752:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215224;
}
else 
{
goto label_1214798;
}
}
else 
{
label_1214798:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215224;
}
else 
{
goto label_1214864;
}
}
else 
{
label_1214864:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215224;
}
else 
{
goto label_1214910;
}
}
else 
{
label_1214910:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215224;
}
else 
{
goto label_1214976;
}
}
else 
{
label_1214976:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215048;
}
else 
{
label_1215048:; 
goto label_1215224;
}
}
else 
{
label_1215224:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1215390;
}
else 
{
goto label_1215270;
}
}
else 
{
label_1215270:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1215342;
}
else 
{
label_1215342:; 
goto label_1215390;
}
}
else 
{
label_1215390:; 
if (((int)m_run_st) == 0)
{
goto label_1215486;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1215486:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_1228653:; 
if (((int)m_run_st) == 0)
{
goto label_1228667;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1228667:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1229149;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
goto label_1228775;
}
else 
{
label_1228775:; 
a = 0;
req_type = 1;
req_a = a;
req_d = a + 50;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1228878;
}
else 
{
goto label_1228819;
}
}
else 
{
label_1228819:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1228878;
}
else 
{
goto label_1228826;
}
}
else 
{
label_1228826:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1228878;
}
else 
{
goto label_1228833;
}
}
else 
{
label_1228833:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1228878;
}
else 
{
goto label_1228840;
}
}
else 
{
label_1228840:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1228878;
}
else 
{
goto label_1228847;
}
}
else 
{
label_1228847:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1228856;
}
else 
{
label_1228856:; 
goto label_1228878;
}
}
else 
{
label_1228878:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1228900;
}
else 
{
goto label_1228885;
}
}
else 
{
label_1228885:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1228894;
}
else 
{
label_1228894:; 
goto label_1228900;
}
}
else 
{
label_1228900:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1228980;
}
else 
{
goto label_1228921;
}
}
else 
{
label_1228921:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1228980;
}
else 
{
goto label_1228928;
}
}
else 
{
label_1228928:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1228980;
}
else 
{
goto label_1228935;
}
}
else 
{
label_1228935:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1228980;
}
else 
{
goto label_1228942;
}
}
else 
{
label_1228942:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1228980;
}
else 
{
goto label_1228949;
}
}
else 
{
label_1228949:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1228958;
}
else 
{
label_1228958:; 
goto label_1228980;
}
}
else 
{
label_1228980:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1229002;
}
else 
{
goto label_1228987;
}
}
else 
{
label_1228987:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1228996;
}
else 
{
label_1228996:; 
goto label_1229002;
}
}
else 
{
label_1229002:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1229075;
}
else 
{
goto label_1229016;
}
}
else 
{
label_1229016:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1229075;
}
else 
{
goto label_1229023;
}
}
else 
{
label_1229023:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1229075;
}
else 
{
goto label_1229030;
}
}
else 
{
label_1229030:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1229075;
}
else 
{
goto label_1229037;
}
}
else 
{
label_1229037:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1229075;
}
else 
{
goto label_1229044;
}
}
else 
{
label_1229044:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1229053;
}
else 
{
label_1229053:; 
goto label_1229075;
}
}
else 
{
label_1229075:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1229082;
}
}
else 
{
label_1229082:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1229091;
}
else 
{
label_1229091:; 
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
label_1229141:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1229971;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1229458;
}
else 
{
goto label_1229399;
}
}
else 
{
label_1229399:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1229458;
}
else 
{
goto label_1229406;
}
}
else 
{
label_1229406:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1229458;
}
else 
{
goto label_1229413;
}
}
else 
{
label_1229413:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1229458;
}
else 
{
goto label_1229420;
}
}
else 
{
label_1229420:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1229458;
}
else 
{
goto label_1229427;
}
}
else 
{
label_1229427:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1229436;
}
else 
{
label_1229436:; 
goto label_1229458;
}
}
else 
{
label_1229458:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1229480;
}
else 
{
goto label_1229465;
}
}
else 
{
label_1229465:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1229474;
}
else 
{
label_1229474:; 
goto label_1229480;
}
}
else 
{
label_1229480:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_49 = req_a;
int i = __tmp_49;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1229530 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1229531 = x;
}
rsp_d = __return_1229530;
goto label_1229533;
rsp_d = __return_1229531;
label_1229533:; 
rsp_status = 1;
label_1229539:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1229620;
}
else 
{
goto label_1229561;
}
}
else 
{
label_1229561:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1229620;
}
else 
{
goto label_1229568;
}
}
else 
{
label_1229568:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1229620;
}
else 
{
goto label_1229575;
}
}
else 
{
label_1229575:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1229620;
}
else 
{
goto label_1229582;
}
}
else 
{
label_1229582:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1229620;
}
else 
{
goto label_1229589;
}
}
else 
{
label_1229589:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1229598;
}
else 
{
label_1229598:; 
goto label_1229620;
}
}
else 
{
label_1229620:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1229642;
}
else 
{
goto label_1229627;
}
}
else 
{
label_1229627:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1229636;
}
else 
{
label_1229636:; 
goto label_1229642;
}
}
else 
{
label_1229642:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1229725;
}
else 
{
goto label_1229666;
}
}
else 
{
label_1229666:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1229725;
}
else 
{
goto label_1229673;
}
}
else 
{
label_1229673:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1229725;
}
else 
{
goto label_1229680;
}
}
else 
{
label_1229680:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1229725;
}
else 
{
goto label_1229687;
}
}
else 
{
label_1229687:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1229725;
}
else 
{
goto label_1229694;
}
}
else 
{
label_1229694:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1229703;
}
else 
{
label_1229703:; 
goto label_1229725;
}
}
else 
{
label_1229725:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1229732;
}
}
else 
{
label_1229732:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1229741;
}
else 
{
label_1229741:; 
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
label_1229795:; 
label_1229980:; 
if (((int)m_run_st) == 0)
{
goto label_1229993;
}
else 
{
label_1229993:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1231055;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230181;
}
else 
{
goto label_1230122;
}
}
else 
{
label_1230122:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230181;
}
else 
{
goto label_1230129;
}
}
else 
{
label_1230129:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230181;
}
else 
{
goto label_1230136;
}
}
else 
{
label_1230136:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230181;
}
else 
{
goto label_1230143;
}
}
else 
{
label_1230143:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230181;
}
else 
{
goto label_1230150;
}
}
else 
{
label_1230150:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230159;
}
else 
{
label_1230159:; 
goto label_1230181;
}
}
else 
{
label_1230181:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1230203;
}
else 
{
goto label_1230188;
}
}
else 
{
label_1230188:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1230197;
}
else 
{
label_1230197:; 
goto label_1230203;
}
}
else 
{
label_1230203:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230276;
}
else 
{
goto label_1230217;
}
}
else 
{
label_1230217:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230276;
}
else 
{
goto label_1230224;
}
}
else 
{
label_1230224:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230276;
}
else 
{
goto label_1230231;
}
}
else 
{
label_1230231:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230276;
}
else 
{
goto label_1230238;
}
}
else 
{
label_1230238:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230276;
}
else 
{
goto label_1230245;
}
}
else 
{
label_1230245:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230254;
}
else 
{
label_1230254:; 
goto label_1230276;
}
}
else 
{
label_1230276:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1230298;
}
else 
{
goto label_1230283;
}
}
else 
{
label_1230283:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1230292;
}
else 
{
label_1230292:; 
goto label_1230298;
}
}
else 
{
label_1230298:; 
c_m_ev = 2;
a = a + 1;
if (a < 1)
{
req_type = 1;
req_a = a;
req_d = a + 50;
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230763;
}
else 
{
goto label_1230704;
}
}
else 
{
label_1230704:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230763;
}
else 
{
goto label_1230711;
}
}
else 
{
label_1230711:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230763;
}
else 
{
goto label_1230718;
}
}
else 
{
label_1230718:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230763;
}
else 
{
goto label_1230725;
}
}
else 
{
label_1230725:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230763;
}
else 
{
goto label_1230732;
}
}
else 
{
label_1230732:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230741;
}
else 
{
label_1230741:; 
goto label_1230763;
}
}
else 
{
label_1230763:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1230785;
}
else 
{
goto label_1230770;
}
}
else 
{
label_1230770:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1230779;
}
else 
{
label_1230779:; 
goto label_1230785;
}
}
else 
{
label_1230785:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230865;
}
else 
{
goto label_1230806;
}
}
else 
{
label_1230806:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230865;
}
else 
{
goto label_1230813;
}
}
else 
{
label_1230813:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230865;
}
else 
{
goto label_1230820;
}
}
else 
{
label_1230820:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230865;
}
else 
{
goto label_1230827;
}
}
else 
{
label_1230827:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230865;
}
else 
{
goto label_1230834;
}
}
else 
{
label_1230834:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230843;
}
else 
{
label_1230843:; 
goto label_1230865;
}
}
else 
{
label_1230865:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1230887;
}
else 
{
goto label_1230872;
}
}
else 
{
label_1230872:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1230881;
}
else 
{
label_1230881:; 
goto label_1230887;
}
}
else 
{
label_1230887:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230960;
}
else 
{
goto label_1230901;
}
}
else 
{
label_1230901:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230960;
}
else 
{
goto label_1230908;
}
}
else 
{
label_1230908:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230960;
}
else 
{
goto label_1230915;
}
}
else 
{
label_1230915:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230960;
}
else 
{
goto label_1230922;
}
}
else 
{
label_1230922:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230960;
}
else 
{
goto label_1230929;
}
}
else 
{
label_1230929:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230938;
}
else 
{
label_1230938:; 
goto label_1230960;
}
}
else 
{
label_1230960:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1230967;
}
}
else 
{
label_1230967:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1230976;
}
else 
{
label_1230976:; 
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
goto label_1229141;
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
a = 0;
req_type___0 = 0;
req_a___0 = a;
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230409;
}
else 
{
goto label_1230350;
}
}
else 
{
label_1230350:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230409;
}
else 
{
goto label_1230357;
}
}
else 
{
label_1230357:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230409;
}
else 
{
goto label_1230364;
}
}
else 
{
label_1230364:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230409;
}
else 
{
goto label_1230371;
}
}
else 
{
label_1230371:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230409;
}
else 
{
goto label_1230378;
}
}
else 
{
label_1230378:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230387;
}
else 
{
label_1230387:; 
goto label_1230409;
}
}
else 
{
label_1230409:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1230431;
}
else 
{
goto label_1230416;
}
}
else 
{
label_1230416:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1230425;
}
else 
{
label_1230425:; 
goto label_1230431;
}
}
else 
{
label_1230431:; 
c_write_req_ev = 2;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230511;
}
else 
{
goto label_1230452;
}
}
else 
{
label_1230452:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230511;
}
else 
{
goto label_1230459;
}
}
else 
{
label_1230459:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230511;
}
else 
{
goto label_1230466;
}
}
else 
{
label_1230466:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230511;
}
else 
{
goto label_1230473;
}
}
else 
{
label_1230473:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230511;
}
else 
{
goto label_1230480;
}
}
else 
{
label_1230480:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230489;
}
else 
{
label_1230489:; 
goto label_1230511;
}
}
else 
{
label_1230511:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1230533;
}
else 
{
goto label_1230518;
}
}
else 
{
label_1230518:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1230527;
}
else 
{
label_1230527:; 
goto label_1230533;
}
}
else 
{
label_1230533:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230606;
}
else 
{
goto label_1230547;
}
}
else 
{
label_1230547:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230606;
}
else 
{
goto label_1230554;
}
}
else 
{
label_1230554:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230606;
}
else 
{
goto label_1230561;
}
}
else 
{
label_1230561:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1230606;
}
else 
{
goto label_1230568;
}
}
else 
{
label_1230568:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1230606;
}
else 
{
goto label_1230575;
}
}
else 
{
label_1230575:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1230584;
}
else 
{
label_1230584:; 
goto label_1230606;
}
}
else 
{
label_1230606:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1230613;
}
}
else 
{
label_1230613:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1230622;
}
else 
{
label_1230622:; 
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
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1231533;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1231190;
}
else 
{
goto label_1231131;
}
}
else 
{
label_1231131:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1231190;
}
else 
{
goto label_1231138;
}
}
else 
{
label_1231138:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1231190;
}
else 
{
goto label_1231145;
}
}
else 
{
label_1231145:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1231190;
}
else 
{
goto label_1231152;
}
}
else 
{
label_1231152:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1231190;
}
else 
{
goto label_1231159;
}
}
else 
{
label_1231159:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1231168;
}
else 
{
label_1231168:; 
goto label_1231190;
}
}
else 
{
label_1231190:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1231212;
}
else 
{
goto label_1231197;
}
}
else 
{
label_1231197:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1231206;
}
else 
{
label_1231206:; 
goto label_1231212;
}
}
else 
{
label_1231212:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_50 = req_a;
int i = __tmp_50;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1231262 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1231263 = x;
}
rsp_d = __return_1231262;
goto label_1231265;
rsp_d = __return_1231263;
label_1231265:; 
rsp_status = 1;
label_1231271:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1231352;
}
else 
{
goto label_1231293;
}
}
else 
{
label_1231293:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1231352;
}
else 
{
goto label_1231300;
}
}
else 
{
label_1231300:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1231352;
}
else 
{
goto label_1231307;
}
}
else 
{
label_1231307:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1231352;
}
else 
{
goto label_1231314;
}
}
else 
{
label_1231314:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1231352;
}
else 
{
goto label_1231321;
}
}
else 
{
label_1231321:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1231330;
}
else 
{
label_1231330:; 
goto label_1231352;
}
}
else 
{
label_1231352:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1231374;
}
else 
{
goto label_1231359;
}
}
else 
{
label_1231359:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1231368;
}
else 
{
label_1231368:; 
goto label_1231374;
}
}
else 
{
label_1231374:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1231457;
}
else 
{
goto label_1231398;
}
}
else 
{
label_1231398:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1231457;
}
else 
{
goto label_1231405;
}
}
else 
{
label_1231405:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1231457;
}
else 
{
goto label_1231412;
}
}
else 
{
label_1231412:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1231457;
}
else 
{
goto label_1231419;
}
}
else 
{
label_1231419:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1231457;
}
else 
{
goto label_1231426;
}
}
else 
{
label_1231426:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1231435;
}
else 
{
label_1231435:; 
goto label_1231457;
}
}
else 
{
label_1231457:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1231464;
}
}
else 
{
label_1231464:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1231473;
}
else 
{
label_1231473:; 
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
label_1231527:; 
label_1231538:; 
if (((int)m_run_st) == 0)
{
goto label_1231551;
}
else 
{
label_1231551:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1231907;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1231739;
}
else 
{
goto label_1231680;
}
}
else 
{
label_1231680:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1231739;
}
else 
{
goto label_1231687;
}
}
else 
{
label_1231687:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1231739;
}
else 
{
goto label_1231694;
}
}
else 
{
label_1231694:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1231739;
}
else 
{
goto label_1231701;
}
}
else 
{
label_1231701:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1231739;
}
else 
{
goto label_1231708;
}
}
else 
{
label_1231708:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1231717;
}
else 
{
label_1231717:; 
goto label_1231739;
}
}
else 
{
label_1231739:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1231761;
}
else 
{
goto label_1231746;
}
}
else 
{
label_1231746:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1231755;
}
else 
{
label_1231755:; 
goto label_1231761;
}
}
else 
{
label_1231761:; 
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1231834;
}
else 
{
goto label_1231775;
}
}
else 
{
label_1231775:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1231834;
}
else 
{
goto label_1231782;
}
}
else 
{
label_1231782:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1231834;
}
else 
{
goto label_1231789;
}
}
else 
{
label_1231789:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1231834;
}
else 
{
goto label_1231796;
}
}
else 
{
label_1231796:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1231834;
}
else 
{
goto label_1231803;
}
}
else 
{
label_1231803:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1231812;
}
else 
{
label_1231812:; 
goto label_1231834;
}
}
else 
{
label_1231834:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1231856;
}
else 
{
goto label_1231841;
}
}
else 
{
label_1231841:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1231850;
}
else 
{
label_1231850:; 
goto label_1231856;
}
}
else 
{
label_1231856:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
a = a + 1;
goto label_1231873;
}
else 
{
{
__VERIFIER_error();
}
a = a + 1;
label_1231873:; 
}
label_1231902:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1232218;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1231961;
}
else 
{
if (((int)s_run_pc) == 2)
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1232120;
}
else 
{
goto label_1232002;
}
}
else 
{
label_1232002:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1232120;
}
else 
{
goto label_1232014;
}
}
else 
{
label_1232014:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1232120;
}
else 
{
goto label_1232030;
}
}
else 
{
label_1232030:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1232120;
}
else 
{
goto label_1232042;
}
}
else 
{
label_1232042:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1232120;
}
else 
{
goto label_1232058;
}
}
else 
{
label_1232058:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1232076;
}
else 
{
label_1232076:; 
goto label_1232120;
}
}
else 
{
label_1232120:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1232132;
}
}
else 
{
label_1232132:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1232150;
}
else 
{
label_1232150:; 
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
goto label_1232210;
}
else 
{
label_1231961:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1232118;
}
else 
{
goto label_1232000;
}
}
else 
{
label_1232000:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1232118;
}
else 
{
goto label_1232016;
}
}
else 
{
label_1232016:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1232118;
}
else 
{
goto label_1232028;
}
}
else 
{
label_1232028:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1232118;
}
else 
{
goto label_1232044;
}
}
else 
{
label_1232044:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1232118;
}
else 
{
goto label_1232056;
}
}
else 
{
label_1232056:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1232074;
}
else 
{
label_1232074:; 
goto label_1232118;
}
}
else 
{
label_1232118:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1232134;
}
}
else 
{
label_1232134:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1232152;
}
else 
{
label_1232152:; 
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
label_1232210:; 
label_1232223:; 
if (((int)m_run_st) == 0)
{
goto label_1232237;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1232237:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1232470;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1232425;
}
else 
{
goto label_1232366;
}
}
else 
{
label_1232366:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1232425;
}
else 
{
goto label_1232373;
}
}
else 
{
label_1232373:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1232425;
}
else 
{
goto label_1232380;
}
}
else 
{
label_1232380:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1232425;
}
else 
{
goto label_1232387;
}
}
else 
{
label_1232387:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1232425;
}
else 
{
goto label_1232394;
}
}
else 
{
label_1232394:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1232403;
}
else 
{
label_1232403:; 
goto label_1232425;
}
}
else 
{
label_1232425:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1232432;
}
}
else 
{
label_1232432:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1232441;
}
else 
{
label_1232441:; 
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
goto label_1231902;
}
}
}
}
else 
{
label_1232470:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_1232481;
}
else 
{
label_1232481:; 
goto label_1232223;
}
}
}
else 
{
}
goto label_1212678;
}
}
}
}
}
}
else 
{
label_1232218:; 
label_1232487:; 
if (((int)m_run_st) == 0)
{
goto label_1232501;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1232501:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_1232734;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
if (((int)m_run_pc) == 0)
{
return 1;
}
else 
{
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
if (((int)c_empty_rsp) == 1)
{
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1232689;
}
else 
{
goto label_1232630;
}
}
else 
{
label_1232630:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1232689;
}
else 
{
goto label_1232637;
}
}
else 
{
label_1232637:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1232689;
}
else 
{
goto label_1232644;
}
}
else 
{
label_1232644:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1232689;
}
else 
{
goto label_1232651;
}
}
else 
{
label_1232651:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1232689;
}
else 
{
goto label_1232658;
}
}
else 
{
label_1232658:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1232667;
}
else 
{
label_1232667:; 
goto label_1232689;
}
}
else 
{
label_1232689:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1232696;
}
}
else 
{
label_1232696:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1232705;
}
else 
{
label_1232705:; 
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
goto label_1231902;
}
}
}
}
else 
{
label_1232734:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1233034;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
goto label_1232780;
}
else 
{
if (((int)s_run_pc) == 2)
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1232939;
}
else 
{
goto label_1232821;
}
}
else 
{
label_1232821:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1232939;
}
else 
{
goto label_1232833;
}
}
else 
{
label_1232833:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1232939;
}
else 
{
goto label_1232849;
}
}
else 
{
label_1232849:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1232939;
}
else 
{
goto label_1232861;
}
}
else 
{
label_1232861:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1232939;
}
else 
{
goto label_1232877;
}
}
else 
{
label_1232877:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1232895;
}
else 
{
label_1232895:; 
goto label_1232939;
}
}
else 
{
label_1232939:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1232951;
}
}
else 
{
label_1232951:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1232969;
}
else 
{
label_1232969:; 
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
goto label_1232210;
}
else 
{
label_1232780:; 
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1232937;
}
else 
{
goto label_1232819;
}
}
else 
{
label_1232819:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1232937;
}
else 
{
goto label_1232835;
}
}
else 
{
label_1232835:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1232937;
}
else 
{
goto label_1232847;
}
}
else 
{
label_1232847:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1232937;
}
else 
{
goto label_1232863;
}
}
else 
{
label_1232863:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1232937;
}
else 
{
goto label_1232875;
}
}
else 
{
label_1232875:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1232893;
}
else 
{
label_1232893:; 
goto label_1232937;
}
}
else 
{
label_1232937:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1232953;
}
}
else 
{
label_1232953:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1232971;
}
else 
{
label_1232971:; 
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
goto label_1232210;
}
}
}
}
}
else 
{
label_1233034:; 
goto label_1232487;
}
}
}
else 
{
}
goto label_1212942;
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
else 
{
label_1231907:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_1232220;
}
else 
{
label_1232220:; 
goto label_1231538;
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
if (((int)req_type) == 1)
{
{
int __tmp_51 = req_a;
int __tmp_52 = req_d;
int i = __tmp_51;
int v = __tmp_52;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1231240;
label_1231240:; 
rsp_status = 1;
goto label_1231271;
}
}
else 
{
rsp_status = 0;
goto label_1231271;
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
label_1231533:; 
label_1233040:; 
if (((int)m_run_st) == 0)
{
goto label_1233053;
}
else 
{
label_1233053:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1233064;
}
else 
{
label_1233064:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1233531;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1233191;
}
else 
{
goto label_1233132;
}
}
else 
{
label_1233132:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1233191;
}
else 
{
goto label_1233139;
}
}
else 
{
label_1233139:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1233191;
}
else 
{
goto label_1233146;
}
}
else 
{
label_1233146:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1233191;
}
else 
{
goto label_1233153;
}
}
else 
{
label_1233153:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1233191;
}
else 
{
goto label_1233160;
}
}
else 
{
label_1233160:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1233169;
}
else 
{
label_1233169:; 
goto label_1233191;
}
}
else 
{
label_1233191:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1233213;
}
else 
{
goto label_1233198;
}
}
else 
{
label_1233198:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1233207;
}
else 
{
label_1233207:; 
goto label_1233213;
}
}
else 
{
label_1233213:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_53 = req_a;
int i = __tmp_53;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1233263 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1233264 = x;
}
rsp_d = __return_1233263;
goto label_1233266;
rsp_d = __return_1233264;
label_1233266:; 
rsp_status = 1;
label_1233272:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1233353;
}
else 
{
goto label_1233294;
}
}
else 
{
label_1233294:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1233353;
}
else 
{
goto label_1233301;
}
}
else 
{
label_1233301:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1233353;
}
else 
{
goto label_1233308;
}
}
else 
{
label_1233308:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1233353;
}
else 
{
goto label_1233315;
}
}
else 
{
label_1233315:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1233353;
}
else 
{
goto label_1233322;
}
}
else 
{
label_1233322:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1233331;
}
else 
{
label_1233331:; 
goto label_1233353;
}
}
else 
{
label_1233353:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1233375;
}
else 
{
goto label_1233360;
}
}
else 
{
label_1233360:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1233369;
}
else 
{
label_1233369:; 
goto label_1233375;
}
}
else 
{
label_1233375:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1233458;
}
else 
{
goto label_1233399;
}
}
else 
{
label_1233399:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1233458;
}
else 
{
goto label_1233406;
}
}
else 
{
label_1233406:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1233458;
}
else 
{
goto label_1233413;
}
}
else 
{
label_1233413:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1233458;
}
else 
{
goto label_1233420;
}
}
else 
{
label_1233420:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1233458;
}
else 
{
goto label_1233427;
}
}
else 
{
label_1233427:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1233436;
}
else 
{
label_1233436:; 
goto label_1233458;
}
}
else 
{
label_1233458:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1233465;
}
}
else 
{
label_1233465:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1233474;
}
else 
{
label_1233474:; 
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
goto label_1231527;
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
if (((int)req_type) == 1)
{
{
int __tmp_54 = req_a;
int __tmp_55 = req_d;
int i = __tmp_54;
int v = __tmp_55;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1233241;
label_1233241:; 
rsp_status = 1;
goto label_1233272;
}
}
else 
{
rsp_status = 0;
goto label_1233272;
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
label_1233531:; 
goto label_1233040;
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
}
}
}
else 
{
label_1231055:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
goto label_1231535;
}
else 
{
label_1231535:; 
goto label_1229980;
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
if (((int)req_type) == 1)
{
{
int __tmp_56 = req_a;
int __tmp_57 = req_d;
int i = __tmp_56;
int v = __tmp_57;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1229508;
label_1229508:; 
rsp_status = 1;
goto label_1229539;
}
}
else 
{
rsp_status = 0;
goto label_1229539;
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
label_1229971:; 
label_1233576:; 
if (((int)m_run_st) == 0)
{
goto label_1233590;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1233590:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1233601;
}
else 
{
label_1233601:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1234068;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1233728;
}
else 
{
goto label_1233669;
}
}
else 
{
label_1233669:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1233728;
}
else 
{
goto label_1233676;
}
}
else 
{
label_1233676:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1233728;
}
else 
{
goto label_1233683;
}
}
else 
{
label_1233683:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1233728;
}
else 
{
goto label_1233690;
}
}
else 
{
label_1233690:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1233728;
}
else 
{
goto label_1233697;
}
}
else 
{
label_1233697:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1233706;
}
else 
{
label_1233706:; 
goto label_1233728;
}
}
else 
{
label_1233728:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1233750;
}
else 
{
goto label_1233735;
}
}
else 
{
label_1233735:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1233744;
}
else 
{
label_1233744:; 
goto label_1233750;
}
}
else 
{
label_1233750:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_58 = req_a;
int i = __tmp_58;
int x;
if (i == 0)
{
x = s_memory0;
 __return_1233800 = x;
}
else 
{
{
__VERIFIER_error();
}
 __return_1233801 = x;
}
rsp_d = __return_1233800;
goto label_1233803;
rsp_d = __return_1233801;
label_1233803:; 
rsp_status = 1;
label_1233809:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1233890;
}
else 
{
goto label_1233831;
}
}
else 
{
label_1233831:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1233890;
}
else 
{
goto label_1233838;
}
}
else 
{
label_1233838:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1233890;
}
else 
{
goto label_1233845;
}
}
else 
{
label_1233845:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1233890;
}
else 
{
goto label_1233852;
}
}
else 
{
label_1233852:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1233890;
}
else 
{
goto label_1233859;
}
}
else 
{
label_1233859:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1233868;
}
else 
{
label_1233868:; 
goto label_1233890;
}
}
else 
{
label_1233890:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1233912;
}
else 
{
goto label_1233897;
}
}
else 
{
label_1233897:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1233906;
}
else 
{
label_1233906:; 
goto label_1233912;
}
}
else 
{
label_1233912:; 
c_write_rsp_ev = 2;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1233995;
}
else 
{
goto label_1233936;
}
}
else 
{
label_1233936:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1233995;
}
else 
{
goto label_1233943;
}
}
else 
{
label_1233943:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1233995;
}
else 
{
goto label_1233950;
}
}
else 
{
label_1233950:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1233995;
}
else 
{
goto label_1233957;
}
}
else 
{
label_1233957:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1233995;
}
else 
{
goto label_1233964;
}
}
else 
{
label_1233964:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1233973;
}
else 
{
label_1233973:; 
goto label_1233995;
}
}
else 
{
label_1233995:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1234002;
}
}
else 
{
label_1234002:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1234011;
}
else 
{
label_1234011:; 
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
goto label_1229795;
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
if (((int)req_type) == 1)
{
{
int __tmp_59 = req_a;
int __tmp_60 = req_d;
int i = __tmp_59;
int v = __tmp_60;
if (i == 0)
{
s_memory0 = v;
}
else 
{
{
__VERIFIER_error();
}
}
goto label_1233778;
label_1233778:; 
rsp_status = 1;
goto label_1233809;
}
}
else 
{
rsp_status = 0;
goto label_1233809;
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
label_1234068:; 
goto label_1233576;
}
}
}
else 
{
}
goto label_1209734;
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
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1229969;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1229292;
}
else 
{
goto label_1229233;
}
}
else 
{
label_1229233:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1229292;
}
else 
{
goto label_1229240;
}
}
else 
{
label_1229240:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1229292;
}
else 
{
goto label_1229247;
}
}
else 
{
label_1229247:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1229292;
}
else 
{
goto label_1229254;
}
}
else 
{
label_1229254:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1229292;
}
else 
{
goto label_1229261;
}
}
else 
{
label_1229261:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1229270;
}
else 
{
label_1229270:; 
goto label_1229292;
}
}
else 
{
label_1229292:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1229299;
}
}
else 
{
label_1229299:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1229308;
}
else 
{
label_1229308:; 
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
label_1229338:; 
if (((int)m_run_st) == 0)
{
goto label_1233549;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1233549:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1233560;
}
else 
{
label_1233560:; 
tmp___0 = __VERIFIER_nondet_int();
return 1;
}
}
else 
{
}
goto label_1210230;
}
}
}
}
}
else 
{
label_1229969:; 
label_1234072:; 
if (((int)m_run_st) == 0)
{
goto label_1234086;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1234086:; 
if (((int)m_run_st) == 0)
{
tmp = __VERIFIER_nondet_int();
goto label_1234097;
}
else 
{
label_1234097:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1234273;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1234224;
}
else 
{
goto label_1234165;
}
}
else 
{
label_1234165:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1234224;
}
else 
{
goto label_1234172;
}
}
else 
{
label_1234172:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1234224;
}
else 
{
goto label_1234179;
}
}
else 
{
label_1234179:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1234224;
}
else 
{
goto label_1234186;
}
}
else 
{
label_1234186:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1234224;
}
else 
{
goto label_1234193;
}
}
else 
{
label_1234193:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1234202;
}
else 
{
label_1234202:; 
goto label_1234224;
}
}
else 
{
label_1234224:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1234231;
}
}
else 
{
label_1234231:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1234240;
}
else 
{
label_1234240:; 
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
goto label_1229338;
}
}
}
}
else 
{
label_1234273:; 
goto label_1234072;
}
}
}
else 
{
}
goto label_1210230;
}
}
}
}
}
}
else 
{
label_1229149:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_1229973;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
if (((int)s_run_pc) == 0)
{
return 1;
}
else 
{
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
if (((int)c_empty_req) == 1)
{
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
else 
{
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1229915;
}
else 
{
goto label_1229856;
}
}
else 
{
label_1229856:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1229915;
}
else 
{
goto label_1229863;
}
}
else 
{
label_1229863:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1229915;
}
else 
{
goto label_1229870;
}
}
else 
{
label_1229870:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1229915;
}
else 
{
goto label_1229877;
}
}
else 
{
label_1229877:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1229915;
}
else 
{
goto label_1229884;
}
}
else 
{
label_1229884:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1229893;
}
else 
{
label_1229893:; 
goto label_1229915;
}
}
else 
{
label_1229915:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
return 1;
}
else 
{
goto label_1229922;
}
}
else 
{
label_1229922:; 
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1229931;
}
else 
{
label_1229931:; 
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
goto label_1228653;
}
}
}
}
else 
{
label_1229973:; 
goto label_1228653;
}
}
}
else 
{
}
goto label_1208578;
}
}
}
else 
{
}
goto label_1215460;
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
label_1208562:; 
goto label_1207242;
}
}
}
else 
{
}
kernel_st = 2;
kernel_st = 3;
if (((int)m_run_pc) == 1)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215226;
}
else 
{
goto label_1214754;
}
}
else 
{
label_1214754:; 
if (((int)m_run_pc) == 2)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215226;
}
else 
{
goto label_1214796;
}
}
else 
{
label_1214796:; 
if (((int)m_run_pc) == 3)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215226;
}
else 
{
goto label_1214866;
}
}
else 
{
label_1214866:; 
if (((int)m_run_pc) == 4)
{
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
goto label_1215226;
}
else 
{
goto label_1214908;
}
}
else 
{
label_1214908:; 
if (((int)m_run_pc) == 5)
{
if (((int)c_read_req_ev) == 1)
{
m_run_st = 0;
goto label_1215226;
}
else 
{
goto label_1214978;
}
}
else 
{
label_1214978:; 
if (((int)m_run_pc) == 6)
{
if (((int)c_write_rsp_ev) == 1)
{
m_run_st = 0;
goto label_1215050;
}
else 
{
label_1215050:; 
goto label_1215226;
}
}
else 
{
label_1215226:; 
if (((int)s_run_pc) == 2)
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_1215388;
}
else 
{
goto label_1215268;
}
}
else 
{
label_1215268:; 
if (((int)s_run_pc) == 1)
{
if (((int)c_read_rsp_ev) == 1)
{
s_run_st = 0;
goto label_1215340;
}
else 
{
label_1215340:; 
goto label_1215388;
}
}
else 
{
label_1215388:; 
if (((int)m_run_st) == 0)
{
goto label_1215484;
}
else 
{
if (((int)s_run_st) == 0)
{
label_1215484:; 
goto label_1207232;
}
else 
{
}
label_1215460:; 
__retres1 = 0;
 __return_1234281 = __retres1;
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
}
}
}
}
