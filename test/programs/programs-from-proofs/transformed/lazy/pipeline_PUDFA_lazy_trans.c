extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void error(void);
int main_in1_val  ;
int main_in1_val_t  ;
int main_in1_ev  ;
int main_in1_req_up  ;
int main_in2_val  ;
int main_in2_val_t  ;
int main_in2_ev  ;
int main_in2_req_up  ;
int main_diff_val  ;
int main_diff_val_t  ;
int main_diff_ev  ;
int main_diff_req_up ;
int main_sum_val  ;
int main_sum_val_t  ;
int main_sum_ev  ;
int main_sum_req_up ;
int main_pres_val  ;
int main_pres_val_t  ;
int main_pres_ev  ;
int main_pres_req_up  ;
int main_dbl_val  ;
int main_dbl_val_t  ;
int main_dbl_ev  ;
int main_dbl_req_up  ;
int main_zero_val  ;
int main_zero_val_t  ;
int main_zero_ev  ;
int main_zero_req_up  ;
int main_clk_val  ;
int main_clk_val_t  ;
int main_clk_ev  ;
int main_clk_req_up  ;
int main_clk_pos_edge  ;
int main_clk_neg_edge  ;
int N_generate_st  ;
int N_generate_i  ;
int S1_addsub_st  ;
int S1_addsub_i  ;
int S2_presdbl_st  ;
int S2_presdbl_i  ;
int S3_zero_st  ;
int S3_zero_i  ;
int D_z  ;
int D_print_st  ;
int D_print_i  ;
void N_generate(void);
void S1_addsub(void);
void S2_presdbl(void);
void S3_zero(void);
void D_print(void);
void eval(void);
void start_simulation(void);
int main(void);
int __return_248362;
int main()
{
int count ;
int __retres2 ;
main_in1_ev = 2;
main_in1_req_up = 0;
main_in2_ev = 2;
main_in2_req_up = 0;
main_diff_ev = 2;
main_diff_req_up = 0;
main_sum_ev = 2;
main_sum_req_up = 0;
main_pres_ev = 2;
main_pres_req_up = 0;
main_dbl_ev = 2;
main_dbl_req_up = 0;
main_zero_ev = 2;
main_zero_req_up = 0;
main_clk_val = 0;
main_clk_ev = 2;
main_clk_req_up = 0;
main_clk_pos_edge = 2;
main_clk_neg_edge = 2;
count = 0;
N_generate_i = 0;
S1_addsub_i = 0;
S2_presdbl_i = 0;
S3_zero_i = 0;
D_print_i = 0;
{
int kernel_st ;
kernel_st = 0;
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
goto label_158643;
}
else 
{
S1_addsub_st = 2;
label_158643:; 
if (((int)S2_presdbl_i) == 1)
{
S2_presdbl_st = 0;
goto label_158651;
}
else 
{
S2_presdbl_st = 2;
label_158651:; 
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
goto label_158659;
}
else 
{
S3_zero_st = 2;
label_158659:; 
if (((int)D_print_i) == 1)
{
D_print_st = 0;
goto label_158667;
}
else 
{
D_print_st = 2;
label_158667:; 
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_158674;
}
else 
{
label_158674:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_158681;
}
else 
{
label_158681:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_158688;
}
else 
{
label_158688:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_158695;
}
else 
{
label_158695:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_158702;
}
else 
{
label_158702:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_158709;
}
else 
{
label_158709:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_158716;
}
else 
{
label_158716:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_158723;
}
else 
{
label_158723:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_224717;
}
else 
{
label_224717:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_224754;
}
else 
{
label_224754:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_224761;
}
else 
{
label_224761:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_224768;
}
else 
{
label_224768:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_224775;
}
else 
{
label_224775:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_224782;
}
else 
{
label_224782:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_224789;
}
else 
{
label_224789:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_224796;
}
else 
{
label_224796:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_224803;
}
else 
{
label_224803:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_224816;
}
else 
{
label_224816:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_224834:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
goto label_224834;
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242706;
}
}
else 
{
label_242706:; 
goto label_224834;
}
}
else 
{
S3_zero_st = 1;
{
int a ;
int b ;
a = main_pres_val;
b = main_dbl_val;
main_zero_val_t = b - (a + a);
main_zero_req_up = 1;
}
label_242646:; 
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_242646;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_243974;
}
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
label_244180:; 
label_244182:; 
label_244188:; 
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_242648;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_243956;
}
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242718;
}
}
else 
{
label_242718:; 
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_242646;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
label_244954:; 
label_244956:; 
label_244962:; 
goto label_242642;
}
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_244780;
}
}
}
}
else 
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
goto label_224834;
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242696;
}
}
else 
{
label_242696:; 
goto label_224834;
}
}
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
label_242468:; 
label_242470:; 
label_242484:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_242470;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_242468;
}
}
else 
{
goto label_242484;
}
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
label_243536:; 
label_243538:; 
label_243544:; 
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_242472;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_242450;
}
}
else 
{
goto label_242482;
}
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242702;
}
}
else 
{
label_242702:; 
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_242470;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_242468;
}
}
else 
{
goto label_242484;
}
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_245460;
}
}
}
else 
{
S3_zero_st = 1;
{
int a ;
int b ;
a = main_pres_val;
b = main_dbl_val;
main_zero_val_t = b - (a + a);
main_zero_req_up = 1;
}
label_242642:; 
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_243976;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
label_243974:; 
label_243976:; 
goto label_242642;
}
}
else 
{
goto label_242642;
}
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
label_243914:; 
label_243916:; 
label_243922:; 
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_243978;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
label_243956:; 
label_243978:; 
goto label_242644;
}
}
else 
{
goto label_242644;
}
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242714;
}
}
else 
{
label_242714:; 
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_244956;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_244954;
}
}
else 
{
goto label_244962;
}
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_245057;
}
}
}
}
else 
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_242470;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_242468;
}
}
else 
{
goto label_242484;
}
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_243319;
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242700;
}
}
else 
{
label_242700:; 
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_242470;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_242468;
}
}
else 
{
goto label_242484;
}
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
label_245677:; 
label_245679:; 
label_245685:; 
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_242472;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_242450;
}
}
else 
{
goto label_242482;
}
}
}
}
}
}
else 
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
goto label_224834;
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242712;
}
}
else 
{
label_242712:; 
goto label_224834;
}
}
else 
{
S3_zero_st = 1;
{
int a ;
int b ;
a = main_pres_val;
b = main_dbl_val;
main_zero_val_t = b - (a + a);
main_zero_req_up = 1;
}
label_242652:; 
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_242652;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_244418;
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242724;
}
}
else 
{
label_242724:; 
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_242652;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
label_244600:; 
label_244602:; 
goto label_242650;
}
}
}
}
else 
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
goto label_224834;
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242690;
}
}
else 
{
label_242690:; 
goto label_224834;
}
}
}
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
label_242408:; 
label_242410:; 
label_242416:; 
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_242410;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_242408;
}
}
else 
{
goto label_242416;
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242708;
}
}
else 
{
label_242708:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_242410;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_242408;
}
}
else 
{
goto label_242416;
}
}
}
else 
{
S3_zero_st = 1;
{
int a ;
int b ;
a = main_pres_val;
b = main_dbl_val;
main_zero_val_t = b - (a + a);
main_zero_req_up = 1;
}
label_242648:; 
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_244182;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_244180;
}
}
else 
{
goto label_244188;
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242720;
}
}
else 
{
label_242720:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_244783;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
label_244780:; 
label_244783:; 
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_244846:; 
goto label_242648;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
label_244820:; 
label_244840:; 
label_244850:; 
goto label_242644;
}
}
}
else 
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_244846;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_244820;
}
}
}
}
}
else 
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_242410;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_242408;
}
}
else 
{
goto label_242416;
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242694;
}
}
else 
{
label_242694:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_242410;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_242408;
}
}
else 
{
goto label_242416;
}
}
}
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
label_242450:; 
label_242472:; 
label_242482:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_243538;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_243536;
}
}
else 
{
if (((int)S2_presdbl_st) == 0)
{
goto label_243544;
}
else 
{
goto label_243544;
}
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242704;
}
}
else 
{
label_242704:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_245464;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
label_245460:; 
label_245464:; 
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_242472;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_242450;
}
}
else 
{
goto label_242482;
}
}
}
else 
{
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_242472;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_242450;
}
}
else 
{
goto label_242482;
}
}
}
}
else 
{
S3_zero_st = 1;
{
int a ;
int b ;
a = main_pres_val;
b = main_dbl_val;
main_zero_val_t = b - (a + a);
main_zero_req_up = 1;
}
label_242644:; 
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_243916;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_243914;
}
}
else 
{
if (((int)S2_presdbl_st) == 0)
{
goto label_243922;
}
else 
{
goto label_243922;
}
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242716;
}
}
else 
{
label_242716:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_245060;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
label_245057:; 
label_245060:; 
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_244840;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_244820;
}
}
else 
{
goto label_244850;
}
}
}
else 
{
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_244840;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_244820;
}
}
else 
{
}
kernel_st = 2;
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_246068;
}
else 
{
label_246068:; 
main_sum_req_up = 0;
goto label_247503;
}
}
}
}
}
else 
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_243323;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
label_243319:; 
label_243323:; 
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_242472;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_242450;
}
}
else 
{
goto label_242482;
}
}
}
else 
{
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_242472;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_242450;
}
}
else 
{
goto label_242482;
}
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242698;
}
}
else 
{
label_242698:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_245679;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_245677;
}
}
else 
{
if (((int)S2_presdbl_st) == 0)
{
goto label_245685;
}
else 
{
}
kernel_st = 2;
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_246070;
}
else 
{
label_246070:; 
main_sum_req_up = 0;
goto label_247501;
}
}
}
}
}
}
else 
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_242410;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_242408;
}
}
else 
{
goto label_242416;
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242710;
}
}
else 
{
label_242710:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_242410;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_242408;
}
}
else 
{
goto label_242416;
}
}
}
else 
{
S3_zero_st = 1;
{
int a ;
int b ;
a = main_pres_val;
b = main_dbl_val;
main_zero_val_t = b - (a + a);
main_zero_req_up = 1;
}
label_242650:; 
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_244421;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
label_244418:; 
label_244421:; 
goto label_242650;
}
}
else 
{
goto label_242650;
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242722;
}
}
else 
{
label_242722:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_244602;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_244600;
}
}
else 
{
}
kernel_st = 2;
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_246066;
}
else 
{
label_246066:; 
main_sum_req_up = 0;
goto label_248109;
}
}
}
}
else 
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_242410;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_242408;
}
}
else 
{
goto label_242416;
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_242692;
}
}
else 
{
label_242692:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_242410;
}
else 
{
S1_addsub_st = 1;
{
int a ;
int b ;
a = main_in1_val;
b = main_in2_val;
main_sum_val_t = a + b;
main_sum_req_up = 1;
main_diff_val_t = a - b;
main_diff_req_up = 1;
}
goto label_242408;
}
}
else 
{
}
kernel_st = 2;
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_246072;
}
else 
{
label_246072:; 
main_sum_req_up = 0;
goto label_241948;
}
}
}
}
}
}
else 
{
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
goto label_224834;
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_246571;
}
}
else 
{
label_246571:; 
goto label_224834;
}
}
else 
{
S3_zero_st = 1;
{
int a ;
int b ;
a = main_pres_val;
b = main_dbl_val;
main_zero_val_t = b - (a + a);
main_zero_req_up = 1;
}
label_246551:; 
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_246551;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_246995;
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_246575;
}
}
else 
{
label_246575:; 
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_246551;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
label_247175:; 
label_247177:; 
goto label_246549;
}
}
}
}
else 
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
goto label_224834;
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_246565;
}
}
else 
{
label_246565:; 
goto label_224834;
}
}
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
label_246487:; 
label_246489:; 
label_246495:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_246489;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_246487;
}
}
else 
{
goto label_246495;
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_246569;
}
}
else 
{
label_246569:; 
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_246489;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_246487;
}
}
else 
{
goto label_246495;
}
}
}
else 
{
S3_zero_st = 1;
{
int a ;
int b ;
a = main_pres_val;
b = main_dbl_val;
main_zero_val_t = b - (a + a);
main_zero_req_up = 1;
}
label_246549:; 
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_246998;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
label_246995:; 
label_246998:; 
goto label_246549;
}
}
else 
{
goto label_246549;
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_246573;
}
}
else 
{
label_246573:; 
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_247177;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_247175;
}
}
else 
{
}
kernel_st = 2;
label_247503:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_247533;
}
else 
{
label_247533:; 
main_diff_req_up = 0;
goto label_247527;
}
}
else 
{
label_247527:; 
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_247561;
}
else 
{
label_247561:; 
main_pres_req_up = 0;
goto label_248136;
}
}
}
}
}
else 
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_246489;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_246487;
}
}
else 
{
goto label_246495;
}
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_246567;
}
}
else 
{
label_246567:; 
if (((int)S2_presdbl_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_246489;
}
else 
{
S2_presdbl_st = 1;
{
int a ;
int b ;
int c ;
int d ;
a = main_sum_val;
b = main_diff_val;
main_pres_val_t = a;
main_pres_req_up = 1;
c = a + b;
d = a - b;
main_dbl_val_t = c + d;
main_dbl_req_up = 1;
}
goto label_246487;
}
}
else 
{
}
kernel_st = 2;
label_247501:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_247535;
}
else 
{
label_247535:; 
main_diff_req_up = 0;
goto label_247525;
}
}
else 
{
label_247525:; 
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_247563;
}
else 
{
label_247563:; 
main_pres_req_up = 0;
goto label_241988;
}
}
}
}
}
}
else 
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
goto label_224834;
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_247952;
}
}
else 
{
label_247952:; 
goto label_224834;
}
}
else 
{
S3_zero_st = 1;
{
int a ;
int b ;
a = main_pres_val;
b = main_dbl_val;
main_zero_val_t = b - (a + a);
main_zero_req_up = 1;
}
label_247946:; 
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
goto label_247946;
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_247954;
}
}
else 
{
label_247954:; 
}
kernel_st = 2;
label_248109:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_248125;
}
else 
{
label_248125:; 
main_diff_req_up = 0;
goto label_248121;
}
}
else 
{
label_248121:; 
label_248136:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_248152;
}
else 
{
label_248152:; 
main_dbl_req_up = 0;
goto label_248148;
}
}
else 
{
label_248148:; 
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_248166;
}
else 
{
label_248166:; 
main_zero_req_up = 0;
goto label_242037;
}
}
}
}
}
else 
{
if (((int)D_print_st) == 0)
{
tmp___3 = __VERIFIER_nondet_int();
if (tmp___3 == 0)
{
goto label_224834;
}
else 
{
D_print_st = 1;
{
D_z = main_zero_val;
}
goto label_224834;
}
}
else 
{
}
kernel_st = 2;
label_241948:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_246102;
}
else 
{
label_246102:; 
main_diff_req_up = 0;
goto label_241962;
}
}
else 
{
label_241962:; 
label_241988:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_247579;
}
else 
{
label_247579:; 
main_dbl_req_up = 0;
goto label_242010;
}
}
else 
{
label_242010:; 
label_242037:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_248175;
}
else 
{
label_248175:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_248181;
}
else 
{
label_248181:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_248188;
}
else 
{
label_248188:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_248195;
}
else 
{
label_248195:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_248202;
}
else 
{
label_248202:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_248209;
}
else 
{
label_248209:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_248216;
}
else 
{
label_248216:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_248223;
}
else 
{
label_248223:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_248236;
}
else 
{
label_248236:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_248273;
}
else 
{
label_248273:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_248280;
}
else 
{
label_248280:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_248287;
}
else 
{
label_248287:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_248294;
}
else 
{
label_248294:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_248301;
}
else 
{
label_248301:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_248308;
}
else 
{
label_248308:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_248315;
}
else 
{
label_248315:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_248322;
}
else 
{
label_248322:; 
if (((int)main_clk_pos_edge) == 1)
{
main_clk_pos_edge = 2;
goto label_248329;
}
else 
{
label_248329:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_248336;
}
else 
{
label_248336:; 
}
__retres2 = 0;
 __return_248362 = __retres2;
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
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
