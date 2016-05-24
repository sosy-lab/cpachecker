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
int flag = 0;
void N_generate(void);
void S1_addsub(void);
void S2_presdbl(void);
void S3_zero(void);
void D_print(void);
void eval(void);
void start_simulation(void);
int main(void);
int main()
{
int count = __VERIFIER_nondet_int();
count = __VERIFIER_nondet_int();
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
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_12770;
}
else 
{
label_12770:; 
main_in1_req_up = 0;
goto label_12767;
}
}
else 
{
label_12767:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_12781;
}
else 
{
label_12781:; 
main_in2_req_up = 0;
goto label_12778;
}
}
else 
{
label_12778:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_12792;
}
else 
{
label_12792:; 
main_sum_req_up = 0;
goto label_12789;
}
}
else 
{
label_12789:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_12803;
}
else 
{
label_12803:; 
main_diff_req_up = 0;
goto label_12800;
}
}
else 
{
label_12800:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_12814;
}
else 
{
label_12814:; 
main_pres_req_up = 0;
goto label_12811;
}
}
else 
{
label_12811:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_12825;
}
else 
{
label_12825:; 
main_dbl_req_up = 0;
goto label_12822;
}
}
else 
{
label_12822:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_12836;
}
else 
{
label_12836:; 
main_zero_req_up = 0;
goto label_12833;
}
}
else 
{
label_12833:; 
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_12891;
}
else 
{
label_12891:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_12911;
}
else 
{
label_12911:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_12931;
}
else 
{
label_12931:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_12951;
}
else 
{
label_12951:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_12971;
}
else 
{
label_12971:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_12991;
}
else 
{
label_12991:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_13011;
}
else 
{
label_13011:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_13031;
}
else 
{
label_13031:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_13059;
}
else 
{
label_13059:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_13119;
}
else 
{
label_13119:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_13139;
}
else 
{
label_13139:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_13159;
}
else 
{
label_13159:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_13179;
}
else 
{
label_13179:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_13199;
}
else 
{
label_13199:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_13219;
}
else 
{
label_13219:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_13239;
}
else 
{
label_13239:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_13259;
}
else 
{
label_13259:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_13287;
}
else 
{
label_13287:; 
label_13302:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_13477:; 
if (((int)S1_addsub_st) == 0)
{
goto label_13491;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_13491:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_13497;
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
label_13517:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_13527;
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
label_13562:; 
}
label_13630:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_13711;
}
else 
{
label_13711:; 
main_in1_req_up = 0;
goto label_13678;
}
}
else 
{
label_13678:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_13810;
}
else 
{
label_13810:; 
main_in2_req_up = 0;
goto label_13777;
}
}
else 
{
label_13777:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_13909;
}
else 
{
label_13909:; 
main_sum_req_up = 0;
goto label_13876;
}
}
else 
{
label_13876:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_14008;
}
else 
{
label_14008:; 
main_diff_req_up = 0;
goto label_13975;
}
}
else 
{
label_13975:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_14107;
}
else 
{
label_14107:; 
main_pres_req_up = 0;
goto label_14074;
}
}
else 
{
label_14074:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_14206;
}
else 
{
label_14206:; 
main_dbl_req_up = 0;
goto label_14173;
}
}
else 
{
label_14173:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_14305;
}
else 
{
label_14305:; 
main_zero_req_up = 0;
goto label_14272;
}
}
else 
{
label_14272:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_14404;
}
else 
{
label_14404:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_14449;
}
else 
{
label_14449:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_14494;
}
else 
{
label_14494:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_14539;
}
else 
{
label_14539:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_14584;
}
else 
{
label_14584:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_14629;
}
else 
{
label_14629:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_14674;
}
else 
{
label_14674:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_14719;
}
else 
{
label_14719:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_14782;
}
else 
{
label_14782:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_14917;
}
else 
{
label_14917:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_14962;
}
else 
{
label_14962:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_15007;
}
else 
{
label_15007:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_15052;
}
else 
{
label_15052:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_15097;
}
else 
{
label_15097:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_15142;
}
else 
{
label_15142:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_15187;
}
else 
{
label_15187:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_15232;
}
else 
{
label_15232:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_15295;
}
else 
{
label_15295:; 
}
label_15562:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
label_15598:; 
count = count + 1;
if (count == 5)
{
flag = D_z;
if (D_z == 0)
{
goto label_15724;
}
else 
{
{
__VERIFIER_error();
}
label_15724:; 
count = 0;
goto label_15628;
}
}
else 
{
label_15628:; 
main_clk_val_t = 0;
main_clk_req_up = 1;
{
int kernel_st ;
kernel_st = 0;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_55225;
}
else 
{
label_55225:; 
main_in1_req_up = 0;
goto label_55222;
}
}
else 
{
label_55222:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_55236;
}
else 
{
label_55236:; 
main_in2_req_up = 0;
goto label_55233;
}
}
else 
{
label_55233:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_55247;
}
else 
{
label_55247:; 
main_sum_req_up = 0;
goto label_55244;
}
}
else 
{
label_55244:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_55258;
}
else 
{
label_55258:; 
main_diff_req_up = 0;
goto label_55255;
}
}
else 
{
label_55255:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_55269;
}
else 
{
label_55269:; 
main_pres_req_up = 0;
goto label_55266;
}
}
else 
{
label_55266:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_55280;
}
else 
{
label_55280:; 
main_dbl_req_up = 0;
goto label_55277;
}
}
else 
{
label_55277:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_55291;
}
else 
{
label_55291:; 
main_zero_req_up = 0;
goto label_55288;
}
}
else 
{
label_55288:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_55302;
}
else 
{
label_55302:; 
main_clk_req_up = 0;
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_55406;
}
else 
{
label_55406:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_55446;
}
else 
{
label_55446:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_55486;
}
else 
{
label_55486:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_55526;
}
else 
{
label_55526:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_55566;
}
else 
{
label_55566:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_55606;
}
else 
{
label_55606:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_55646;
}
else 
{
label_55646:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_55686;
}
else 
{
label_55686:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_55742;
}
else 
{
label_55742:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_55862;
}
else 
{
label_55862:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_55902;
}
else 
{
label_55902:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_55942;
}
else 
{
label_55942:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_55982;
}
else 
{
label_55982:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_56022;
}
else 
{
label_56022:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_56062;
}
else 
{
label_56062:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_56102;
}
else 
{
label_56102:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_56142;
}
else 
{
label_56142:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_56198;
}
else 
{
label_56198:; 
label_56229:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_56751:; 
if (((int)S1_addsub_st) == 0)
{
goto label_56765;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_56765:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_56771;
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
label_56791:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_56801;
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
label_56836:; 
}
label_56904:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57048;
}
else 
{
label_57048:; 
main_in1_req_up = 0;
goto label_56979;
}
}
else 
{
label_56979:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57246;
}
else 
{
label_57246:; 
main_in2_req_up = 0;
goto label_57177;
}
}
else 
{
label_57177:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57444;
}
else 
{
label_57444:; 
main_sum_req_up = 0;
goto label_57375;
}
}
else 
{
label_57375:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57642;
}
else 
{
label_57642:; 
main_diff_req_up = 0;
goto label_57573;
}
}
else 
{
label_57573:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57840;
}
else 
{
label_57840:; 
main_pres_req_up = 0;
goto label_57771;
}
}
else 
{
label_57771:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58038;
}
else 
{
label_58038:; 
main_dbl_req_up = 0;
goto label_57969;
}
}
else 
{
label_57969:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58236;
}
else 
{
label_58236:; 
main_zero_req_up = 0;
goto label_58167;
}
}
else 
{
label_58167:; 
label_58356:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58551;
}
else 
{
label_58551:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58641;
}
else 
{
label_58641:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58731;
}
else 
{
label_58731:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58821;
}
else 
{
label_58821:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58911;
}
else 
{
label_58911:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_59001;
}
else 
{
label_59001:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59091;
}
else 
{
label_59091:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59181;
}
else 
{
label_59181:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59307;
}
else 
{
label_59307:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59577;
}
else 
{
label_59577:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59667;
}
else 
{
label_59667:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59757;
}
else 
{
label_59757:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59847;
}
else 
{
label_59847:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59937;
}
else 
{
label_59937:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60027;
}
else 
{
label_60027:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60117;
}
else 
{
label_60117:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60207;
}
else 
{
label_60207:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60333;
}
else 
{
label_60333:; 
}
goto label_21083;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_56801:; 
if (((int)S3_zero_st) == 0)
{
goto label_56791;
}
else 
{
}
label_56917:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57049;
}
else 
{
label_57049:; 
main_in1_req_up = 0;
goto label_56978;
}
}
else 
{
label_56978:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57247;
}
else 
{
label_57247:; 
main_in2_req_up = 0;
goto label_57176;
}
}
else 
{
label_57176:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57445;
}
else 
{
label_57445:; 
main_sum_req_up = 0;
goto label_57374;
}
}
else 
{
label_57374:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57643;
}
else 
{
label_57643:; 
main_diff_req_up = 0;
goto label_57572;
}
}
else 
{
label_57572:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57841;
}
else 
{
label_57841:; 
main_pres_req_up = 0;
goto label_57770;
}
}
else 
{
label_57770:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58039;
}
else 
{
label_58039:; 
main_dbl_req_up = 0;
goto label_57968;
}
}
else 
{
label_57968:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58237;
}
else 
{
label_58237:; 
main_zero_req_up = 0;
goto label_58166;
}
}
else 
{
label_58166:; 
label_58355:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58552;
}
else 
{
label_58552:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58642;
}
else 
{
label_58642:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58732;
}
else 
{
label_58732:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58822;
}
else 
{
label_58822:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58912;
}
else 
{
label_58912:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_59002;
}
else 
{
label_59002:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59092;
}
else 
{
label_59092:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59182;
}
else 
{
label_59182:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59308;
}
else 
{
label_59308:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59578;
}
else 
{
label_59578:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59668;
}
else 
{
label_59668:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59758;
}
else 
{
label_59758:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59848;
}
else 
{
label_59848:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59938;
}
else 
{
label_59938:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60028;
}
else 
{
label_60028:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60118;
}
else 
{
label_60118:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60208;
}
else 
{
label_60208:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60334;
}
else 
{
label_60334:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_60793:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_60815;
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
}
goto label_56904;
}
else 
{
label_60815:; 
goto label_60793;
}
}
else 
{
}
goto label_56917;
}
}
else 
{
}
goto label_21082;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_56771:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_56800;
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
label_56837:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_56864;
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
goto label_56836;
}
}
else 
{
label_56864:; 
goto label_56837;
}
}
else 
{
}
label_56858:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57047;
}
else 
{
label_57047:; 
main_in1_req_up = 0;
goto label_56980;
}
}
else 
{
label_56980:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57245;
}
else 
{
label_57245:; 
main_in2_req_up = 0;
goto label_57178;
}
}
else 
{
label_57178:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57443;
}
else 
{
label_57443:; 
main_sum_req_up = 0;
goto label_57376;
}
}
else 
{
label_57376:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57641;
}
else 
{
label_57641:; 
main_diff_req_up = 0;
goto label_57574;
}
}
else 
{
label_57574:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57839;
}
else 
{
label_57839:; 
main_pres_req_up = 0;
goto label_57772;
}
}
else 
{
label_57772:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58037;
}
else 
{
label_58037:; 
main_dbl_req_up = 0;
goto label_57970;
}
}
else 
{
label_57970:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58235;
}
else 
{
label_58235:; 
main_zero_req_up = 0;
goto label_58168;
}
}
else 
{
label_58168:; 
label_58357:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58550;
}
else 
{
label_58550:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58640;
}
else 
{
label_58640:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58730;
}
else 
{
label_58730:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58820;
}
else 
{
label_58820:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58910;
}
else 
{
label_58910:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_59000;
}
else 
{
label_59000:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59090;
}
else 
{
label_59090:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59180;
}
else 
{
label_59180:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59306;
}
else 
{
label_59306:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59576;
}
else 
{
label_59576:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59666;
}
else 
{
label_59666:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59756;
}
else 
{
label_59756:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59846;
}
else 
{
label_59846:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59936;
}
else 
{
label_59936:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60026;
}
else 
{
label_60026:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60116;
}
else 
{
label_60116:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60206;
}
else 
{
label_60206:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60332;
}
else 
{
label_60332:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_60645:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_60663;
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
}
goto label_56904;
}
else 
{
label_60663:; 
goto label_60645;
}
}
else 
{
}
goto label_56858;
}
}
else 
{
}
goto label_21084;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_56800:; 
goto label_56751;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57046;
}
else 
{
label_57046:; 
main_in1_req_up = 0;
goto label_56981;
}
}
else 
{
label_56981:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57244;
}
else 
{
label_57244:; 
main_in2_req_up = 0;
goto label_57179;
}
}
else 
{
label_57179:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57442;
}
else 
{
label_57442:; 
main_sum_req_up = 0;
goto label_57377;
}
}
else 
{
label_57377:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57640;
}
else 
{
label_57640:; 
main_diff_req_up = 0;
goto label_57575;
}
}
else 
{
label_57575:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57838;
}
else 
{
label_57838:; 
main_pres_req_up = 0;
goto label_57773;
}
}
else 
{
label_57773:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58036;
}
else 
{
label_58036:; 
main_dbl_req_up = 0;
goto label_57971;
}
}
else 
{
label_57971:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58234;
}
else 
{
label_58234:; 
main_zero_req_up = 0;
goto label_58169;
}
}
else 
{
label_58169:; 
label_58358:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58549;
}
else 
{
label_58549:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58639;
}
else 
{
label_58639:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58729;
}
else 
{
label_58729:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58819;
}
else 
{
label_58819:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58909;
}
else 
{
label_58909:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_58999;
}
else 
{
label_58999:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59089;
}
else 
{
label_59089:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59179;
}
else 
{
label_59179:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59305;
}
else 
{
label_59305:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59575;
}
else 
{
label_59575:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59665;
}
else 
{
label_59665:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59755;
}
else 
{
label_59755:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59845;
}
else 
{
label_59845:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59935;
}
else 
{
label_59935:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60025;
}
else 
{
label_60025:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60115;
}
else 
{
label_60115:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60205;
}
else 
{
label_60205:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60331;
}
else 
{
label_60331:; 
if (((int)S1_addsub_st) == 0)
{
goto label_60546;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_60546:; 
goto label_56229;
}
else 
{
}
goto label_21085;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_55410;
}
else 
{
label_55410:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_55450;
}
else 
{
label_55450:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_55490;
}
else 
{
label_55490:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_55530;
}
else 
{
label_55530:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_55570;
}
else 
{
label_55570:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_55610;
}
else 
{
label_55610:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_55650;
}
else 
{
label_55650:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_55690;
}
else 
{
label_55690:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_55746;
}
else 
{
label_55746:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_55866;
}
else 
{
label_55866:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_55906;
}
else 
{
label_55906:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_55946;
}
else 
{
label_55946:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_55986;
}
else 
{
label_55986:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_56026;
}
else 
{
label_56026:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_56066;
}
else 
{
label_56066:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_56106;
}
else 
{
label_56106:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_56146;
}
else 
{
label_56146:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_56202;
}
else 
{
label_56202:; 
label_56225:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_56370:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_56388;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57037;
}
else 
{
label_57037:; 
main_in1_req_up = 0;
goto label_56990;
}
}
else 
{
label_56990:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57235;
}
else 
{
label_57235:; 
main_in2_req_up = 0;
goto label_57188;
}
}
else 
{
label_57188:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57433;
}
else 
{
label_57433:; 
main_sum_req_up = 0;
goto label_57386;
}
}
else 
{
label_57386:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57631;
}
else 
{
label_57631:; 
main_diff_req_up = 0;
goto label_57584;
}
}
else 
{
label_57584:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57829;
}
else 
{
label_57829:; 
main_pres_req_up = 0;
goto label_57782;
}
}
else 
{
label_57782:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58027;
}
else 
{
label_58027:; 
main_dbl_req_up = 0;
goto label_57980;
}
}
else 
{
label_57980:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58225;
}
else 
{
label_58225:; 
main_zero_req_up = 0;
goto label_58178;
}
}
else 
{
label_58178:; 
label_58367:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58540;
}
else 
{
label_58540:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58630;
}
else 
{
label_58630:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58720;
}
else 
{
label_58720:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58810;
}
else 
{
label_58810:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58900;
}
else 
{
label_58900:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_58990;
}
else 
{
label_58990:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59080;
}
else 
{
label_59080:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59170;
}
else 
{
label_59170:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59296;
}
else 
{
label_59296:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59566;
}
else 
{
label_59566:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59656;
}
else 
{
label_59656:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59746;
}
else 
{
label_59746:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59836;
}
else 
{
label_59836:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59926;
}
else 
{
label_59926:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60016;
}
else 
{
label_60016:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60106;
}
else 
{
label_60106:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60196;
}
else 
{
label_60196:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60322;
}
else 
{
label_60322:; 
}
goto label_21094;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_56388:; 
goto label_56370;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57036;
}
else 
{
label_57036:; 
main_in1_req_up = 0;
goto label_56991;
}
}
else 
{
label_56991:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57234;
}
else 
{
label_57234:; 
main_in2_req_up = 0;
goto label_57189;
}
}
else 
{
label_57189:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57432;
}
else 
{
label_57432:; 
main_sum_req_up = 0;
goto label_57387;
}
}
else 
{
label_57387:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57630;
}
else 
{
label_57630:; 
main_diff_req_up = 0;
goto label_57585;
}
}
else 
{
label_57585:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57828;
}
else 
{
label_57828:; 
main_pres_req_up = 0;
goto label_57783;
}
}
else 
{
label_57783:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58026;
}
else 
{
label_58026:; 
main_dbl_req_up = 0;
goto label_57981;
}
}
else 
{
label_57981:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58224;
}
else 
{
label_58224:; 
main_zero_req_up = 0;
goto label_58179;
}
}
else 
{
label_58179:; 
label_58368:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58539;
}
else 
{
label_58539:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58629;
}
else 
{
label_58629:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58719;
}
else 
{
label_58719:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58809;
}
else 
{
label_58809:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58899;
}
else 
{
label_58899:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_58989;
}
else 
{
label_58989:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59079;
}
else 
{
label_59079:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59169;
}
else 
{
label_59169:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59295;
}
else 
{
label_59295:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59565;
}
else 
{
label_59565:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59655;
}
else 
{
label_59655:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59745;
}
else 
{
label_59745:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59835;
}
else 
{
label_59835:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59925;
}
else 
{
label_59925:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60015;
}
else 
{
label_60015:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60105;
}
else 
{
label_60105:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60195;
}
else 
{
label_60195:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60321;
}
else 
{
label_60321:; 
if (((int)S1_addsub_st) == 0)
{
goto label_56225;
}
else 
{
}
goto label_21095;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_55408;
}
else 
{
label_55408:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_55448;
}
else 
{
label_55448:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_55488;
}
else 
{
label_55488:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_55528;
}
else 
{
label_55528:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_55568;
}
else 
{
label_55568:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_55608;
}
else 
{
label_55608:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_55648;
}
else 
{
label_55648:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_55688;
}
else 
{
label_55688:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_55744;
}
else 
{
label_55744:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_55864;
}
else 
{
label_55864:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_55904;
}
else 
{
label_55904:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_55944;
}
else 
{
label_55944:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_55984;
}
else 
{
label_55984:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_56024;
}
else 
{
label_56024:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_56064;
}
else 
{
label_56064:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_56104;
}
else 
{
label_56104:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_56144;
}
else 
{
label_56144:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_56200;
}
else 
{
label_56200:; 
label_56227:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_56506:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_56528;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57041;
}
else 
{
label_57041:; 
main_in1_req_up = 0;
goto label_56986;
}
}
else 
{
label_56986:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57239;
}
else 
{
label_57239:; 
main_in2_req_up = 0;
goto label_57184;
}
}
else 
{
label_57184:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57437;
}
else 
{
label_57437:; 
main_sum_req_up = 0;
goto label_57382;
}
}
else 
{
label_57382:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57635;
}
else 
{
label_57635:; 
main_diff_req_up = 0;
goto label_57580;
}
}
else 
{
label_57580:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57833;
}
else 
{
label_57833:; 
main_pres_req_up = 0;
goto label_57778;
}
}
else 
{
label_57778:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58031;
}
else 
{
label_58031:; 
main_dbl_req_up = 0;
goto label_57976;
}
}
else 
{
label_57976:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58229;
}
else 
{
label_58229:; 
main_zero_req_up = 0;
goto label_58174;
}
}
else 
{
label_58174:; 
label_58363:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58544;
}
else 
{
label_58544:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58634;
}
else 
{
label_58634:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58724;
}
else 
{
label_58724:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58814;
}
else 
{
label_58814:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58904;
}
else 
{
label_58904:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_58994;
}
else 
{
label_58994:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59084;
}
else 
{
label_59084:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59174;
}
else 
{
label_59174:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59300;
}
else 
{
label_59300:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59570;
}
else 
{
label_59570:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59660;
}
else 
{
label_59660:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59750;
}
else 
{
label_59750:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59840;
}
else 
{
label_59840:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59930;
}
else 
{
label_59930:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60020;
}
else 
{
label_60020:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60110;
}
else 
{
label_60110:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60200;
}
else 
{
label_60200:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60326;
}
else 
{
label_60326:; 
}
goto label_21090;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_56528:; 
goto label_56506;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57040;
}
else 
{
label_57040:; 
main_in1_req_up = 0;
goto label_56987;
}
}
else 
{
label_56987:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57238;
}
else 
{
label_57238:; 
main_in2_req_up = 0;
goto label_57185;
}
}
else 
{
label_57185:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57436;
}
else 
{
label_57436:; 
main_sum_req_up = 0;
goto label_57383;
}
}
else 
{
label_57383:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57634;
}
else 
{
label_57634:; 
main_diff_req_up = 0;
goto label_57581;
}
}
else 
{
label_57581:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57832;
}
else 
{
label_57832:; 
main_pres_req_up = 0;
goto label_57779;
}
}
else 
{
label_57779:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58030;
}
else 
{
label_58030:; 
main_dbl_req_up = 0;
goto label_57977;
}
}
else 
{
label_57977:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58228;
}
else 
{
label_58228:; 
main_zero_req_up = 0;
goto label_58175;
}
}
else 
{
label_58175:; 
label_58364:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58543;
}
else 
{
label_58543:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58633;
}
else 
{
label_58633:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58723;
}
else 
{
label_58723:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58813;
}
else 
{
label_58813:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58903;
}
else 
{
label_58903:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_58993;
}
else 
{
label_58993:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59083;
}
else 
{
label_59083:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59173;
}
else 
{
label_59173:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59299;
}
else 
{
label_59299:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59569;
}
else 
{
label_59569:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59659;
}
else 
{
label_59659:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59749;
}
else 
{
label_59749:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59839;
}
else 
{
label_59839:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59929;
}
else 
{
label_59929:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60019;
}
else 
{
label_60019:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60109;
}
else 
{
label_60109:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60199;
}
else 
{
label_60199:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60325;
}
else 
{
label_60325:; 
if (((int)S3_zero_st) == 0)
{
goto label_56227;
}
else 
{
}
goto label_21091;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_55412;
}
else 
{
label_55412:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_55452;
}
else 
{
label_55452:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_55492;
}
else 
{
label_55492:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_55532;
}
else 
{
label_55532:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_55572;
}
else 
{
label_55572:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_55612;
}
else 
{
label_55612:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_55652;
}
else 
{
label_55652:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_55692;
}
else 
{
label_55692:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_55748;
}
else 
{
label_55748:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_55868;
}
else 
{
label_55868:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_55908;
}
else 
{
label_55908:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_55948;
}
else 
{
label_55948:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_55988;
}
else 
{
label_55988:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_56028;
}
else 
{
label_56028:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_56068;
}
else 
{
label_56068:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_56108;
}
else 
{
label_56108:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_56148;
}
else 
{
label_56148:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_56204;
}
else 
{
label_56204:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57033;
}
else 
{
label_57033:; 
main_in1_req_up = 0;
goto label_56994;
}
}
else 
{
label_56994:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57231;
}
else 
{
label_57231:; 
main_in2_req_up = 0;
goto label_57192;
}
}
else 
{
label_57192:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57429;
}
else 
{
label_57429:; 
main_sum_req_up = 0;
goto label_57390;
}
}
else 
{
label_57390:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57627;
}
else 
{
label_57627:; 
main_diff_req_up = 0;
goto label_57588;
}
}
else 
{
label_57588:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57825;
}
else 
{
label_57825:; 
main_pres_req_up = 0;
goto label_57786;
}
}
else 
{
label_57786:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58023;
}
else 
{
label_58023:; 
main_dbl_req_up = 0;
goto label_57984;
}
}
else 
{
label_57984:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58221;
}
else 
{
label_58221:; 
main_zero_req_up = 0;
goto label_58182;
}
}
else 
{
label_58182:; 
label_58371:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58536;
}
else 
{
label_58536:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58626;
}
else 
{
label_58626:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58716;
}
else 
{
label_58716:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58806;
}
else 
{
label_58806:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58896;
}
else 
{
label_58896:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_58986;
}
else 
{
label_58986:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59076;
}
else 
{
label_59076:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59166;
}
else 
{
label_59166:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59292;
}
else 
{
label_59292:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59562;
}
else 
{
label_59562:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59652;
}
else 
{
label_59652:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59742;
}
else 
{
label_59742:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59832;
}
else 
{
label_59832:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59922;
}
else 
{
label_59922:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60012;
}
else 
{
label_60012:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60102;
}
else 
{
label_60102:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60192;
}
else 
{
label_60192:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60318;
}
else 
{
label_60318:; 
}
goto label_21098;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_55407;
}
else 
{
label_55407:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_55447;
}
else 
{
label_55447:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_55487;
}
else 
{
label_55487:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_55527;
}
else 
{
label_55527:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_55567;
}
else 
{
label_55567:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_55607;
}
else 
{
label_55607:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_55647;
}
else 
{
label_55647:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_55687;
}
else 
{
label_55687:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_55743;
}
else 
{
label_55743:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_55863;
}
else 
{
label_55863:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_55903;
}
else 
{
label_55903:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_55943;
}
else 
{
label_55943:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_55983;
}
else 
{
label_55983:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_56023;
}
else 
{
label_56023:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_56063;
}
else 
{
label_56063:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_56103;
}
else 
{
label_56103:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_56143;
}
else 
{
label_56143:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_56199;
}
else 
{
label_56199:; 
label_56228:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_56571:; 
if (((int)S1_addsub_st) == 0)
{
goto label_56585;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_56585:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_56591;
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
label_56611:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_56621;
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
label_56656:; 
}
label_56724:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57044;
}
else 
{
label_57044:; 
main_in1_req_up = 0;
goto label_56983;
}
}
else 
{
label_56983:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57242;
}
else 
{
label_57242:; 
main_in2_req_up = 0;
goto label_57181;
}
}
else 
{
label_57181:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57440;
}
else 
{
label_57440:; 
main_sum_req_up = 0;
goto label_57379;
}
}
else 
{
label_57379:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57638;
}
else 
{
label_57638:; 
main_diff_req_up = 0;
goto label_57577;
}
}
else 
{
label_57577:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57836;
}
else 
{
label_57836:; 
main_pres_req_up = 0;
goto label_57775;
}
}
else 
{
label_57775:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58034;
}
else 
{
label_58034:; 
main_dbl_req_up = 0;
goto label_57973;
}
}
else 
{
label_57973:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58232;
}
else 
{
label_58232:; 
main_zero_req_up = 0;
goto label_58171;
}
}
else 
{
label_58171:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_58398;
}
else 
{
label_58398:; 
main_clk_req_up = 0;
goto label_58356;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58547;
}
else 
{
label_58547:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58637;
}
else 
{
label_58637:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58727;
}
else 
{
label_58727:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58817;
}
else 
{
label_58817:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58907;
}
else 
{
label_58907:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_58997;
}
else 
{
label_58997:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59087;
}
else 
{
label_59087:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59177;
}
else 
{
label_59177:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59303;
}
else 
{
label_59303:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59573;
}
else 
{
label_59573:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59663;
}
else 
{
label_59663:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59753;
}
else 
{
label_59753:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59843;
}
else 
{
label_59843:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59933;
}
else 
{
label_59933:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60023;
}
else 
{
label_60023:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60113;
}
else 
{
label_60113:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60203;
}
else 
{
label_60203:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60329;
}
else 
{
label_60329:; 
}
goto label_21087;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_56621:; 
if (((int)S3_zero_st) == 0)
{
goto label_56611;
}
else 
{
}
label_56737:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57045;
}
else 
{
label_57045:; 
main_in1_req_up = 0;
goto label_56982;
}
}
else 
{
label_56982:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57243;
}
else 
{
label_57243:; 
main_in2_req_up = 0;
goto label_57180;
}
}
else 
{
label_57180:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57441;
}
else 
{
label_57441:; 
main_sum_req_up = 0;
goto label_57378;
}
}
else 
{
label_57378:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57639;
}
else 
{
label_57639:; 
main_diff_req_up = 0;
goto label_57576;
}
}
else 
{
label_57576:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57837;
}
else 
{
label_57837:; 
main_pres_req_up = 0;
goto label_57774;
}
}
else 
{
label_57774:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58035;
}
else 
{
label_58035:; 
main_dbl_req_up = 0;
goto label_57972;
}
}
else 
{
label_57972:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58233;
}
else 
{
label_58233:; 
main_zero_req_up = 0;
goto label_58170;
}
}
else 
{
label_58170:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_58399;
}
else 
{
label_58399:; 
main_clk_req_up = 0;
goto label_58355;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58548;
}
else 
{
label_58548:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58638;
}
else 
{
label_58638:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58728;
}
else 
{
label_58728:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58818;
}
else 
{
label_58818:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58908;
}
else 
{
label_58908:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_58998;
}
else 
{
label_58998:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59088;
}
else 
{
label_59088:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59178;
}
else 
{
label_59178:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59304;
}
else 
{
label_59304:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59574;
}
else 
{
label_59574:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59664;
}
else 
{
label_59664:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59754;
}
else 
{
label_59754:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59844;
}
else 
{
label_59844:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59934;
}
else 
{
label_59934:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60024;
}
else 
{
label_60024:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60114;
}
else 
{
label_60114:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60204;
}
else 
{
label_60204:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60330;
}
else 
{
label_60330:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_60723:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_60745;
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
}
goto label_56724;
}
else 
{
label_60745:; 
goto label_60723;
}
}
else 
{
}
goto label_56737;
}
}
else 
{
}
goto label_21086;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_56591:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_56620;
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
label_56657:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_56684;
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
goto label_56656;
}
}
else 
{
label_56684:; 
goto label_56657;
}
}
else 
{
}
label_56678:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57043;
}
else 
{
label_57043:; 
main_in1_req_up = 0;
goto label_56984;
}
}
else 
{
label_56984:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57241;
}
else 
{
label_57241:; 
main_in2_req_up = 0;
goto label_57182;
}
}
else 
{
label_57182:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57439;
}
else 
{
label_57439:; 
main_sum_req_up = 0;
goto label_57380;
}
}
else 
{
label_57380:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57637;
}
else 
{
label_57637:; 
main_diff_req_up = 0;
goto label_57578;
}
}
else 
{
label_57578:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57835;
}
else 
{
label_57835:; 
main_pres_req_up = 0;
goto label_57776;
}
}
else 
{
label_57776:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58033;
}
else 
{
label_58033:; 
main_dbl_req_up = 0;
goto label_57974;
}
}
else 
{
label_57974:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58231;
}
else 
{
label_58231:; 
main_zero_req_up = 0;
goto label_58172;
}
}
else 
{
label_58172:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_58397;
}
else 
{
label_58397:; 
main_clk_req_up = 0;
goto label_58357;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58546;
}
else 
{
label_58546:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58636;
}
else 
{
label_58636:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58726;
}
else 
{
label_58726:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58816;
}
else 
{
label_58816:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58906;
}
else 
{
label_58906:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_58996;
}
else 
{
label_58996:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59086;
}
else 
{
label_59086:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59176;
}
else 
{
label_59176:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59302;
}
else 
{
label_59302:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59572;
}
else 
{
label_59572:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59662;
}
else 
{
label_59662:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59752;
}
else 
{
label_59752:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59842;
}
else 
{
label_59842:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59932;
}
else 
{
label_59932:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60022;
}
else 
{
label_60022:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60112;
}
else 
{
label_60112:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60202;
}
else 
{
label_60202:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60328;
}
else 
{
label_60328:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_60570:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_60588;
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
}
goto label_56724;
}
else 
{
label_60588:; 
goto label_60570;
}
}
else 
{
}
goto label_56678;
}
}
else 
{
}
goto label_21088;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_56620:; 
goto label_56571;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57042;
}
else 
{
label_57042:; 
main_in1_req_up = 0;
goto label_56985;
}
}
else 
{
label_56985:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57240;
}
else 
{
label_57240:; 
main_in2_req_up = 0;
goto label_57183;
}
}
else 
{
label_57183:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57438;
}
else 
{
label_57438:; 
main_sum_req_up = 0;
goto label_57381;
}
}
else 
{
label_57381:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57636;
}
else 
{
label_57636:; 
main_diff_req_up = 0;
goto label_57579;
}
}
else 
{
label_57579:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57834;
}
else 
{
label_57834:; 
main_pres_req_up = 0;
goto label_57777;
}
}
else 
{
label_57777:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58032;
}
else 
{
label_58032:; 
main_dbl_req_up = 0;
goto label_57975;
}
}
else 
{
label_57975:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58230;
}
else 
{
label_58230:; 
main_zero_req_up = 0;
goto label_58173;
}
}
else 
{
label_58173:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_58396;
}
else 
{
label_58396:; 
main_clk_req_up = 0;
goto label_58358;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58545;
}
else 
{
label_58545:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58635;
}
else 
{
label_58635:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58725;
}
else 
{
label_58725:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58815;
}
else 
{
label_58815:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58905;
}
else 
{
label_58905:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_58995;
}
else 
{
label_58995:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59085;
}
else 
{
label_59085:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59175;
}
else 
{
label_59175:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59301;
}
else 
{
label_59301:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59571;
}
else 
{
label_59571:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59661;
}
else 
{
label_59661:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59751;
}
else 
{
label_59751:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59841;
}
else 
{
label_59841:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59931;
}
else 
{
label_59931:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60021;
}
else 
{
label_60021:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60111;
}
else 
{
label_60111:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60201;
}
else 
{
label_60201:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60327;
}
else 
{
label_60327:; 
if (((int)S1_addsub_st) == 0)
{
goto label_60548;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_60548:; 
goto label_56228;
}
else 
{
}
goto label_21089;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_55411;
}
else 
{
label_55411:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_55451;
}
else 
{
label_55451:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_55491;
}
else 
{
label_55491:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_55531;
}
else 
{
label_55531:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_55571;
}
else 
{
label_55571:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_55611;
}
else 
{
label_55611:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_55651;
}
else 
{
label_55651:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_55691;
}
else 
{
label_55691:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_55747;
}
else 
{
label_55747:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_55867;
}
else 
{
label_55867:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_55907;
}
else 
{
label_55907:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_55947;
}
else 
{
label_55947:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_55987;
}
else 
{
label_55987:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_56027;
}
else 
{
label_56027:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_56067;
}
else 
{
label_56067:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_56107;
}
else 
{
label_56107:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_56147;
}
else 
{
label_56147:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_56203;
}
else 
{
label_56203:; 
label_56224:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_56299:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_56317;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57035;
}
else 
{
label_57035:; 
main_in1_req_up = 0;
goto label_56992;
}
}
else 
{
label_56992:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57233;
}
else 
{
label_57233:; 
main_in2_req_up = 0;
goto label_57190;
}
}
else 
{
label_57190:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57431;
}
else 
{
label_57431:; 
main_sum_req_up = 0;
goto label_57388;
}
}
else 
{
label_57388:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57629;
}
else 
{
label_57629:; 
main_diff_req_up = 0;
goto label_57586;
}
}
else 
{
label_57586:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57827;
}
else 
{
label_57827:; 
main_pres_req_up = 0;
goto label_57784;
}
}
else 
{
label_57784:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58025;
}
else 
{
label_58025:; 
main_dbl_req_up = 0;
goto label_57982;
}
}
else 
{
label_57982:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58223;
}
else 
{
label_58223:; 
main_zero_req_up = 0;
goto label_58180;
}
}
else 
{
label_58180:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_58393;
}
else 
{
label_58393:; 
main_clk_req_up = 0;
goto label_58367;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58538;
}
else 
{
label_58538:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58628;
}
else 
{
label_58628:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58718;
}
else 
{
label_58718:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58808;
}
else 
{
label_58808:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58898;
}
else 
{
label_58898:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_58988;
}
else 
{
label_58988:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59078;
}
else 
{
label_59078:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59168;
}
else 
{
label_59168:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59294;
}
else 
{
label_59294:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59564;
}
else 
{
label_59564:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59654;
}
else 
{
label_59654:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59744;
}
else 
{
label_59744:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59834;
}
else 
{
label_59834:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59924;
}
else 
{
label_59924:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60014;
}
else 
{
label_60014:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60104;
}
else 
{
label_60104:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60194;
}
else 
{
label_60194:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60320;
}
else 
{
label_60320:; 
}
goto label_21096;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_56317:; 
goto label_56299;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57034;
}
else 
{
label_57034:; 
main_in1_req_up = 0;
goto label_56993;
}
}
else 
{
label_56993:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57232;
}
else 
{
label_57232:; 
main_in2_req_up = 0;
goto label_57191;
}
}
else 
{
label_57191:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57430;
}
else 
{
label_57430:; 
main_sum_req_up = 0;
goto label_57389;
}
}
else 
{
label_57389:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57628;
}
else 
{
label_57628:; 
main_diff_req_up = 0;
goto label_57587;
}
}
else 
{
label_57587:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57826;
}
else 
{
label_57826:; 
main_pres_req_up = 0;
goto label_57785;
}
}
else 
{
label_57785:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58024;
}
else 
{
label_58024:; 
main_dbl_req_up = 0;
goto label_57983;
}
}
else 
{
label_57983:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58222;
}
else 
{
label_58222:; 
main_zero_req_up = 0;
goto label_58181;
}
}
else 
{
label_58181:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_58392;
}
else 
{
label_58392:; 
main_clk_req_up = 0;
goto label_58368;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58537;
}
else 
{
label_58537:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58627;
}
else 
{
label_58627:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58717;
}
else 
{
label_58717:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58807;
}
else 
{
label_58807:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58897;
}
else 
{
label_58897:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_58987;
}
else 
{
label_58987:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59077;
}
else 
{
label_59077:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59167;
}
else 
{
label_59167:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59293;
}
else 
{
label_59293:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59563;
}
else 
{
label_59563:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59653;
}
else 
{
label_59653:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59743;
}
else 
{
label_59743:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59833;
}
else 
{
label_59833:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59923;
}
else 
{
label_59923:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60013;
}
else 
{
label_60013:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60103;
}
else 
{
label_60103:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60193;
}
else 
{
label_60193:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60319;
}
else 
{
label_60319:; 
if (((int)S1_addsub_st) == 0)
{
goto label_56224;
}
else 
{
}
goto label_21097;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_55409;
}
else 
{
label_55409:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_55449;
}
else 
{
label_55449:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_55489;
}
else 
{
label_55489:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_55529;
}
else 
{
label_55529:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_55569;
}
else 
{
label_55569:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_55609;
}
else 
{
label_55609:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_55649;
}
else 
{
label_55649:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_55689;
}
else 
{
label_55689:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_55745;
}
else 
{
label_55745:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_55865;
}
else 
{
label_55865:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_55905;
}
else 
{
label_55905:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_55945;
}
else 
{
label_55945:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_55985;
}
else 
{
label_55985:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_56025;
}
else 
{
label_56025:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_56065;
}
else 
{
label_56065:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_56105;
}
else 
{
label_56105:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_56145;
}
else 
{
label_56145:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_56201;
}
else 
{
label_56201:; 
label_56226:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_56441:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_56463;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57039;
}
else 
{
label_57039:; 
main_in1_req_up = 0;
goto label_56988;
}
}
else 
{
label_56988:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57237;
}
else 
{
label_57237:; 
main_in2_req_up = 0;
goto label_57186;
}
}
else 
{
label_57186:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57435;
}
else 
{
label_57435:; 
main_sum_req_up = 0;
goto label_57384;
}
}
else 
{
label_57384:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57633;
}
else 
{
label_57633:; 
main_diff_req_up = 0;
goto label_57582;
}
}
else 
{
label_57582:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57831;
}
else 
{
label_57831:; 
main_pres_req_up = 0;
goto label_57780;
}
}
else 
{
label_57780:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58029;
}
else 
{
label_58029:; 
main_dbl_req_up = 0;
goto label_57978;
}
}
else 
{
label_57978:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58227;
}
else 
{
label_58227:; 
main_zero_req_up = 0;
goto label_58176;
}
}
else 
{
label_58176:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_58395;
}
else 
{
label_58395:; 
main_clk_req_up = 0;
goto label_58363;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58542;
}
else 
{
label_58542:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58632;
}
else 
{
label_58632:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58722;
}
else 
{
label_58722:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58812;
}
else 
{
label_58812:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58902;
}
else 
{
label_58902:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_58992;
}
else 
{
label_58992:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59082;
}
else 
{
label_59082:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59172;
}
else 
{
label_59172:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59298;
}
else 
{
label_59298:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59568;
}
else 
{
label_59568:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59658;
}
else 
{
label_59658:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59748;
}
else 
{
label_59748:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59838;
}
else 
{
label_59838:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59928;
}
else 
{
label_59928:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60018;
}
else 
{
label_60018:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60108;
}
else 
{
label_60108:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60198;
}
else 
{
label_60198:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60324;
}
else 
{
label_60324:; 
}
goto label_21092;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_56463:; 
goto label_56441;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57038;
}
else 
{
label_57038:; 
main_in1_req_up = 0;
goto label_56989;
}
}
else 
{
label_56989:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57236;
}
else 
{
label_57236:; 
main_in2_req_up = 0;
goto label_57187;
}
}
else 
{
label_57187:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57434;
}
else 
{
label_57434:; 
main_sum_req_up = 0;
goto label_57385;
}
}
else 
{
label_57385:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57632;
}
else 
{
label_57632:; 
main_diff_req_up = 0;
goto label_57583;
}
}
else 
{
label_57583:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57830;
}
else 
{
label_57830:; 
main_pres_req_up = 0;
goto label_57781;
}
}
else 
{
label_57781:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58028;
}
else 
{
label_58028:; 
main_dbl_req_up = 0;
goto label_57979;
}
}
else 
{
label_57979:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58226;
}
else 
{
label_58226:; 
main_zero_req_up = 0;
goto label_58177;
}
}
else 
{
label_58177:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_58394;
}
else 
{
label_58394:; 
main_clk_req_up = 0;
goto label_58364;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58541;
}
else 
{
label_58541:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58631;
}
else 
{
label_58631:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58721;
}
else 
{
label_58721:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58811;
}
else 
{
label_58811:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58901;
}
else 
{
label_58901:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_58991;
}
else 
{
label_58991:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59081;
}
else 
{
label_59081:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59171;
}
else 
{
label_59171:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59297;
}
else 
{
label_59297:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59567;
}
else 
{
label_59567:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59657;
}
else 
{
label_59657:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59747;
}
else 
{
label_59747:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59837;
}
else 
{
label_59837:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59927;
}
else 
{
label_59927:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60017;
}
else 
{
label_60017:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60107;
}
else 
{
label_60107:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60197;
}
else 
{
label_60197:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60323;
}
else 
{
label_60323:; 
if (((int)S3_zero_st) == 0)
{
goto label_56226;
}
else 
{
}
goto label_21093;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_55413;
}
else 
{
label_55413:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_55453;
}
else 
{
label_55453:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_55493;
}
else 
{
label_55493:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_55533;
}
else 
{
label_55533:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_55573;
}
else 
{
label_55573:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_55613;
}
else 
{
label_55613:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_55653;
}
else 
{
label_55653:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_55693;
}
else 
{
label_55693:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_55749;
}
else 
{
label_55749:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_55869;
}
else 
{
label_55869:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_55909;
}
else 
{
label_55909:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_55949;
}
else 
{
label_55949:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_55989;
}
else 
{
label_55989:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_56029;
}
else 
{
label_56029:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_56069;
}
else 
{
label_56069:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_56109;
}
else 
{
label_56109:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_56149;
}
else 
{
label_56149:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_56205;
}
else 
{
label_56205:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_57032;
}
else 
{
label_57032:; 
main_in1_req_up = 0;
goto label_56995;
}
}
else 
{
label_56995:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_57230;
}
else 
{
label_57230:; 
main_in2_req_up = 0;
goto label_57193;
}
}
else 
{
label_57193:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_57428;
}
else 
{
label_57428:; 
main_sum_req_up = 0;
goto label_57391;
}
}
else 
{
label_57391:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_57626;
}
else 
{
label_57626:; 
main_diff_req_up = 0;
goto label_57589;
}
}
else 
{
label_57589:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_57824;
}
else 
{
label_57824:; 
main_pres_req_up = 0;
goto label_57787;
}
}
else 
{
label_57787:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_58022;
}
else 
{
label_58022:; 
main_dbl_req_up = 0;
goto label_57985;
}
}
else 
{
label_57985:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_58220;
}
else 
{
label_58220:; 
main_zero_req_up = 0;
goto label_58183;
}
}
else 
{
label_58183:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_58391;
}
else 
{
label_58391:; 
main_clk_req_up = 0;
goto label_58371;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_58535;
}
else 
{
label_58535:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_58625;
}
else 
{
label_58625:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_58715;
}
else 
{
label_58715:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_58805;
}
else 
{
label_58805:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_58895;
}
else 
{
label_58895:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_58985;
}
else 
{
label_58985:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_59075;
}
else 
{
label_59075:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_59165;
}
else 
{
label_59165:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_59291;
}
else 
{
label_59291:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_59561;
}
else 
{
label_59561:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_59651;
}
else 
{
label_59651:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_59741;
}
else 
{
label_59741:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_59831;
}
else 
{
label_59831:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_59921;
}
else 
{
label_59921:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_60011;
}
else 
{
label_60011:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_60101;
}
else 
{
label_60101:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_60191;
}
else 
{
label_60191:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_60317;
}
else 
{
label_60317:; 
}
goto label_21099;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_13527:; 
if (((int)S3_zero_st) == 0)
{
goto label_13517;
}
else 
{
}
label_13643:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_13712;
}
else 
{
label_13712:; 
main_in1_req_up = 0;
goto label_13677;
}
}
else 
{
label_13677:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_13811;
}
else 
{
label_13811:; 
main_in2_req_up = 0;
goto label_13776;
}
}
else 
{
label_13776:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_13910;
}
else 
{
label_13910:; 
main_sum_req_up = 0;
goto label_13875;
}
}
else 
{
label_13875:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_14009;
}
else 
{
label_14009:; 
main_diff_req_up = 0;
goto label_13974;
}
}
else 
{
label_13974:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_14108;
}
else 
{
label_14108:; 
main_pres_req_up = 0;
goto label_14073;
}
}
else 
{
label_14073:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_14207;
}
else 
{
label_14207:; 
main_dbl_req_up = 0;
goto label_14172;
}
}
else 
{
label_14172:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_14306;
}
else 
{
label_14306:; 
main_zero_req_up = 0;
goto label_14271;
}
}
else 
{
label_14271:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_14405;
}
else 
{
label_14405:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_14450;
}
else 
{
label_14450:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_14495;
}
else 
{
label_14495:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_14540;
}
else 
{
label_14540:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_14585;
}
else 
{
label_14585:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_14630;
}
else 
{
label_14630:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_14675;
}
else 
{
label_14675:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_14720;
}
else 
{
label_14720:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_14783;
}
else 
{
label_14783:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_14918;
}
else 
{
label_14918:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_14963;
}
else 
{
label_14963:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_15008;
}
else 
{
label_15008:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_15053;
}
else 
{
label_15053:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_15098;
}
else 
{
label_15098:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_15143;
}
else 
{
label_15143:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_15188;
}
else 
{
label_15188:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_15233;
}
else 
{
label_15233:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_15296;
}
else 
{
label_15296:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_15497:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_15519;
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
}
goto label_13630;
}
else 
{
label_15519:; 
goto label_15497;
}
}
else 
{
}
goto label_13643;
}
}
else 
{
}
label_15563:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
label_15599:; 
count = count + 1;
if (count == 5)
{
flag = D_z;
if (D_z == 0)
{
goto label_15725;
}
else 
{
{
__VERIFIER_error();
}
label_15725:; 
count = 0;
goto label_15627;
}
}
else 
{
label_15627:; 
main_clk_val_t = 0;
main_clk_req_up = 1;
{
int kernel_st ;
kernel_st = 0;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_60860;
}
else 
{
label_60860:; 
main_in1_req_up = 0;
goto label_60857;
}
}
else 
{
label_60857:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_60871;
}
else 
{
label_60871:; 
main_in2_req_up = 0;
goto label_60868;
}
}
else 
{
label_60868:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_60882;
}
else 
{
label_60882:; 
main_sum_req_up = 0;
goto label_60879;
}
}
else 
{
label_60879:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_60893;
}
else 
{
label_60893:; 
main_diff_req_up = 0;
goto label_60890;
}
}
else 
{
label_60890:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_60904;
}
else 
{
label_60904:; 
main_pres_req_up = 0;
goto label_60901;
}
}
else 
{
label_60901:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_60915;
}
else 
{
label_60915:; 
main_dbl_req_up = 0;
goto label_60912;
}
}
else 
{
label_60912:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_60926;
}
else 
{
label_60926:; 
main_zero_req_up = 0;
goto label_60923;
}
}
else 
{
label_60923:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_60937;
}
else 
{
label_60937:; 
main_clk_req_up = 0;
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_61041;
}
else 
{
label_61041:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_61081;
}
else 
{
label_61081:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_61121;
}
else 
{
label_61121:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_61161;
}
else 
{
label_61161:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_61201;
}
else 
{
label_61201:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_61241;
}
else 
{
label_61241:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_61281;
}
else 
{
label_61281:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_61321;
}
else 
{
label_61321:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_61377;
}
else 
{
label_61377:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_61497;
}
else 
{
label_61497:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_61537;
}
else 
{
label_61537:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_61577;
}
else 
{
label_61577:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_61617;
}
else 
{
label_61617:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_61657;
}
else 
{
label_61657:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_61697;
}
else 
{
label_61697:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_61737;
}
else 
{
label_61737:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_61777;
}
else 
{
label_61777:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_61833;
}
else 
{
label_61833:; 
label_61864:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_62386:; 
if (((int)S1_addsub_st) == 0)
{
goto label_62400;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_62400:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_62406;
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
label_62426:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_62436;
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
label_62471:; 
}
label_62539:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62683;
}
else 
{
label_62683:; 
main_in1_req_up = 0;
goto label_62614;
}
}
else 
{
label_62614:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62881;
}
else 
{
label_62881:; 
main_in2_req_up = 0;
goto label_62812;
}
}
else 
{
label_62812:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63079;
}
else 
{
label_63079:; 
main_sum_req_up = 0;
goto label_63010;
}
}
else 
{
label_63010:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63277;
}
else 
{
label_63277:; 
main_diff_req_up = 0;
goto label_63208;
}
}
else 
{
label_63208:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63475;
}
else 
{
label_63475:; 
main_pres_req_up = 0;
goto label_63406;
}
}
else 
{
label_63406:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63673;
}
else 
{
label_63673:; 
main_dbl_req_up = 0;
goto label_63604;
}
}
else 
{
label_63604:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63871;
}
else 
{
label_63871:; 
main_zero_req_up = 0;
goto label_63802;
}
}
else 
{
label_63802:; 
label_63991:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64186;
}
else 
{
label_64186:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64276;
}
else 
{
label_64276:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64366;
}
else 
{
label_64366:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64456;
}
else 
{
label_64456:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64546;
}
else 
{
label_64546:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64636;
}
else 
{
label_64636:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64726;
}
else 
{
label_64726:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64816;
}
else 
{
label_64816:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64942;
}
else 
{
label_64942:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65212;
}
else 
{
label_65212:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65302;
}
else 
{
label_65302:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65392;
}
else 
{
label_65392:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65482;
}
else 
{
label_65482:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65572;
}
else 
{
label_65572:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65662;
}
else 
{
label_65662:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65752;
}
else 
{
label_65752:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65842;
}
else 
{
label_65842:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65968;
}
else 
{
label_65968:; 
}
goto label_21083;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_62436:; 
if (((int)S3_zero_st) == 0)
{
goto label_62426;
}
else 
{
}
label_62552:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62684;
}
else 
{
label_62684:; 
main_in1_req_up = 0;
goto label_62613;
}
}
else 
{
label_62613:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62882;
}
else 
{
label_62882:; 
main_in2_req_up = 0;
goto label_62811;
}
}
else 
{
label_62811:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63080;
}
else 
{
label_63080:; 
main_sum_req_up = 0;
goto label_63009;
}
}
else 
{
label_63009:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63278;
}
else 
{
label_63278:; 
main_diff_req_up = 0;
goto label_63207;
}
}
else 
{
label_63207:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63476;
}
else 
{
label_63476:; 
main_pres_req_up = 0;
goto label_63405;
}
}
else 
{
label_63405:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63674;
}
else 
{
label_63674:; 
main_dbl_req_up = 0;
goto label_63603;
}
}
else 
{
label_63603:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63872;
}
else 
{
label_63872:; 
main_zero_req_up = 0;
goto label_63801;
}
}
else 
{
label_63801:; 
label_63990:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64187;
}
else 
{
label_64187:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64277;
}
else 
{
label_64277:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64367;
}
else 
{
label_64367:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64457;
}
else 
{
label_64457:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64547;
}
else 
{
label_64547:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64637;
}
else 
{
label_64637:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64727;
}
else 
{
label_64727:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64817;
}
else 
{
label_64817:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64943;
}
else 
{
label_64943:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65213;
}
else 
{
label_65213:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65303;
}
else 
{
label_65303:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65393;
}
else 
{
label_65393:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65483;
}
else 
{
label_65483:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65573;
}
else 
{
label_65573:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65663;
}
else 
{
label_65663:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65753;
}
else 
{
label_65753:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65843;
}
else 
{
label_65843:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65969;
}
else 
{
label_65969:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_66428:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_66450;
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
}
goto label_62539;
}
else 
{
label_66450:; 
goto label_66428;
}
}
else 
{
}
goto label_62552;
}
}
else 
{
}
goto label_21082;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_62406:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_62435;
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
label_62472:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_62499;
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
goto label_62471;
}
}
else 
{
label_62499:; 
goto label_62472;
}
}
else 
{
}
label_62493:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62682;
}
else 
{
label_62682:; 
main_in1_req_up = 0;
goto label_62615;
}
}
else 
{
label_62615:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62880;
}
else 
{
label_62880:; 
main_in2_req_up = 0;
goto label_62813;
}
}
else 
{
label_62813:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63078;
}
else 
{
label_63078:; 
main_sum_req_up = 0;
goto label_63011;
}
}
else 
{
label_63011:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63276;
}
else 
{
label_63276:; 
main_diff_req_up = 0;
goto label_63209;
}
}
else 
{
label_63209:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63474;
}
else 
{
label_63474:; 
main_pres_req_up = 0;
goto label_63407;
}
}
else 
{
label_63407:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63672;
}
else 
{
label_63672:; 
main_dbl_req_up = 0;
goto label_63605;
}
}
else 
{
label_63605:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63870;
}
else 
{
label_63870:; 
main_zero_req_up = 0;
goto label_63803;
}
}
else 
{
label_63803:; 
label_63992:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64185;
}
else 
{
label_64185:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64275;
}
else 
{
label_64275:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64365;
}
else 
{
label_64365:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64455;
}
else 
{
label_64455:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64545;
}
else 
{
label_64545:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64635;
}
else 
{
label_64635:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64725;
}
else 
{
label_64725:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64815;
}
else 
{
label_64815:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64941;
}
else 
{
label_64941:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65211;
}
else 
{
label_65211:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65301;
}
else 
{
label_65301:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65391;
}
else 
{
label_65391:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65481;
}
else 
{
label_65481:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65571;
}
else 
{
label_65571:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65661;
}
else 
{
label_65661:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65751;
}
else 
{
label_65751:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65841;
}
else 
{
label_65841:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65967;
}
else 
{
label_65967:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_66280:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_66298;
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
}
goto label_62539;
}
else 
{
label_66298:; 
goto label_66280;
}
}
else 
{
}
goto label_62493;
}
}
else 
{
}
goto label_21084;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_62435:; 
goto label_62386;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62681;
}
else 
{
label_62681:; 
main_in1_req_up = 0;
goto label_62616;
}
}
else 
{
label_62616:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62879;
}
else 
{
label_62879:; 
main_in2_req_up = 0;
goto label_62814;
}
}
else 
{
label_62814:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63077;
}
else 
{
label_63077:; 
main_sum_req_up = 0;
goto label_63012;
}
}
else 
{
label_63012:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63275;
}
else 
{
label_63275:; 
main_diff_req_up = 0;
goto label_63210;
}
}
else 
{
label_63210:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63473;
}
else 
{
label_63473:; 
main_pres_req_up = 0;
goto label_63408;
}
}
else 
{
label_63408:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63671;
}
else 
{
label_63671:; 
main_dbl_req_up = 0;
goto label_63606;
}
}
else 
{
label_63606:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63869;
}
else 
{
label_63869:; 
main_zero_req_up = 0;
goto label_63804;
}
}
else 
{
label_63804:; 
label_63993:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64184;
}
else 
{
label_64184:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64274;
}
else 
{
label_64274:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64364;
}
else 
{
label_64364:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64454;
}
else 
{
label_64454:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64544;
}
else 
{
label_64544:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64634;
}
else 
{
label_64634:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64724;
}
else 
{
label_64724:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64814;
}
else 
{
label_64814:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64940;
}
else 
{
label_64940:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65210;
}
else 
{
label_65210:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65300;
}
else 
{
label_65300:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65390;
}
else 
{
label_65390:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65480;
}
else 
{
label_65480:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65570;
}
else 
{
label_65570:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65660;
}
else 
{
label_65660:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65750;
}
else 
{
label_65750:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65840;
}
else 
{
label_65840:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65966;
}
else 
{
label_65966:; 
if (((int)S1_addsub_st) == 0)
{
goto label_66181;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_66181:; 
goto label_61864;
}
else 
{
}
goto label_21085;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_61045;
}
else 
{
label_61045:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_61085;
}
else 
{
label_61085:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_61125;
}
else 
{
label_61125:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_61165;
}
else 
{
label_61165:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_61205;
}
else 
{
label_61205:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_61245;
}
else 
{
label_61245:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_61285;
}
else 
{
label_61285:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_61325;
}
else 
{
label_61325:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_61381;
}
else 
{
label_61381:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_61501;
}
else 
{
label_61501:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_61541;
}
else 
{
label_61541:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_61581;
}
else 
{
label_61581:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_61621;
}
else 
{
label_61621:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_61661;
}
else 
{
label_61661:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_61701;
}
else 
{
label_61701:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_61741;
}
else 
{
label_61741:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_61781;
}
else 
{
label_61781:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_61837;
}
else 
{
label_61837:; 
label_61860:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_62005:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_62023;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62672;
}
else 
{
label_62672:; 
main_in1_req_up = 0;
goto label_62625;
}
}
else 
{
label_62625:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62870;
}
else 
{
label_62870:; 
main_in2_req_up = 0;
goto label_62823;
}
}
else 
{
label_62823:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63068;
}
else 
{
label_63068:; 
main_sum_req_up = 0;
goto label_63021;
}
}
else 
{
label_63021:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63266;
}
else 
{
label_63266:; 
main_diff_req_up = 0;
goto label_63219;
}
}
else 
{
label_63219:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63464;
}
else 
{
label_63464:; 
main_pres_req_up = 0;
goto label_63417;
}
}
else 
{
label_63417:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63662;
}
else 
{
label_63662:; 
main_dbl_req_up = 0;
goto label_63615;
}
}
else 
{
label_63615:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63860;
}
else 
{
label_63860:; 
main_zero_req_up = 0;
goto label_63813;
}
}
else 
{
label_63813:; 
label_64002:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64175;
}
else 
{
label_64175:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64265;
}
else 
{
label_64265:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64355;
}
else 
{
label_64355:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64445;
}
else 
{
label_64445:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64535;
}
else 
{
label_64535:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64625;
}
else 
{
label_64625:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64715;
}
else 
{
label_64715:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64805;
}
else 
{
label_64805:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64931;
}
else 
{
label_64931:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65201;
}
else 
{
label_65201:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65291;
}
else 
{
label_65291:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65381;
}
else 
{
label_65381:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65471;
}
else 
{
label_65471:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65561;
}
else 
{
label_65561:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65651;
}
else 
{
label_65651:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65741;
}
else 
{
label_65741:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65831;
}
else 
{
label_65831:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65957;
}
else 
{
label_65957:; 
}
goto label_21094;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_62023:; 
goto label_62005;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62671;
}
else 
{
label_62671:; 
main_in1_req_up = 0;
goto label_62626;
}
}
else 
{
label_62626:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62869;
}
else 
{
label_62869:; 
main_in2_req_up = 0;
goto label_62824;
}
}
else 
{
label_62824:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63067;
}
else 
{
label_63067:; 
main_sum_req_up = 0;
goto label_63022;
}
}
else 
{
label_63022:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63265;
}
else 
{
label_63265:; 
main_diff_req_up = 0;
goto label_63220;
}
}
else 
{
label_63220:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63463;
}
else 
{
label_63463:; 
main_pres_req_up = 0;
goto label_63418;
}
}
else 
{
label_63418:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63661;
}
else 
{
label_63661:; 
main_dbl_req_up = 0;
goto label_63616;
}
}
else 
{
label_63616:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63859;
}
else 
{
label_63859:; 
main_zero_req_up = 0;
goto label_63814;
}
}
else 
{
label_63814:; 
label_64003:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64174;
}
else 
{
label_64174:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64264;
}
else 
{
label_64264:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64354;
}
else 
{
label_64354:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64444;
}
else 
{
label_64444:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64534;
}
else 
{
label_64534:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64624;
}
else 
{
label_64624:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64714;
}
else 
{
label_64714:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64804;
}
else 
{
label_64804:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64930;
}
else 
{
label_64930:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65200;
}
else 
{
label_65200:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65290;
}
else 
{
label_65290:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65380;
}
else 
{
label_65380:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65470;
}
else 
{
label_65470:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65560;
}
else 
{
label_65560:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65650;
}
else 
{
label_65650:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65740;
}
else 
{
label_65740:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65830;
}
else 
{
label_65830:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65956;
}
else 
{
label_65956:; 
if (((int)S1_addsub_st) == 0)
{
goto label_61860;
}
else 
{
}
goto label_21095;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_61043;
}
else 
{
label_61043:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_61083;
}
else 
{
label_61083:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_61123;
}
else 
{
label_61123:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_61163;
}
else 
{
label_61163:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_61203;
}
else 
{
label_61203:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_61243;
}
else 
{
label_61243:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_61283;
}
else 
{
label_61283:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_61323;
}
else 
{
label_61323:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_61379;
}
else 
{
label_61379:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_61499;
}
else 
{
label_61499:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_61539;
}
else 
{
label_61539:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_61579;
}
else 
{
label_61579:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_61619;
}
else 
{
label_61619:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_61659;
}
else 
{
label_61659:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_61699;
}
else 
{
label_61699:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_61739;
}
else 
{
label_61739:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_61779;
}
else 
{
label_61779:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_61835;
}
else 
{
label_61835:; 
label_61862:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_62141:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_62163;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62676;
}
else 
{
label_62676:; 
main_in1_req_up = 0;
goto label_62621;
}
}
else 
{
label_62621:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62874;
}
else 
{
label_62874:; 
main_in2_req_up = 0;
goto label_62819;
}
}
else 
{
label_62819:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63072;
}
else 
{
label_63072:; 
main_sum_req_up = 0;
goto label_63017;
}
}
else 
{
label_63017:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63270;
}
else 
{
label_63270:; 
main_diff_req_up = 0;
goto label_63215;
}
}
else 
{
label_63215:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63468;
}
else 
{
label_63468:; 
main_pres_req_up = 0;
goto label_63413;
}
}
else 
{
label_63413:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63666;
}
else 
{
label_63666:; 
main_dbl_req_up = 0;
goto label_63611;
}
}
else 
{
label_63611:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63864;
}
else 
{
label_63864:; 
main_zero_req_up = 0;
goto label_63809;
}
}
else 
{
label_63809:; 
label_63998:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64179;
}
else 
{
label_64179:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64269;
}
else 
{
label_64269:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64359;
}
else 
{
label_64359:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64449;
}
else 
{
label_64449:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64539;
}
else 
{
label_64539:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64629;
}
else 
{
label_64629:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64719;
}
else 
{
label_64719:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64809;
}
else 
{
label_64809:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64935;
}
else 
{
label_64935:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65205;
}
else 
{
label_65205:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65295;
}
else 
{
label_65295:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65385;
}
else 
{
label_65385:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65475;
}
else 
{
label_65475:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65565;
}
else 
{
label_65565:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65655;
}
else 
{
label_65655:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65745;
}
else 
{
label_65745:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65835;
}
else 
{
label_65835:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65961;
}
else 
{
label_65961:; 
}
goto label_21090;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_62163:; 
goto label_62141;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62675;
}
else 
{
label_62675:; 
main_in1_req_up = 0;
goto label_62622;
}
}
else 
{
label_62622:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62873;
}
else 
{
label_62873:; 
main_in2_req_up = 0;
goto label_62820;
}
}
else 
{
label_62820:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63071;
}
else 
{
label_63071:; 
main_sum_req_up = 0;
goto label_63018;
}
}
else 
{
label_63018:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63269;
}
else 
{
label_63269:; 
main_diff_req_up = 0;
goto label_63216;
}
}
else 
{
label_63216:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63467;
}
else 
{
label_63467:; 
main_pres_req_up = 0;
goto label_63414;
}
}
else 
{
label_63414:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63665;
}
else 
{
label_63665:; 
main_dbl_req_up = 0;
goto label_63612;
}
}
else 
{
label_63612:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63863;
}
else 
{
label_63863:; 
main_zero_req_up = 0;
goto label_63810;
}
}
else 
{
label_63810:; 
label_63999:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64178;
}
else 
{
label_64178:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64268;
}
else 
{
label_64268:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64358;
}
else 
{
label_64358:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64448;
}
else 
{
label_64448:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64538;
}
else 
{
label_64538:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64628;
}
else 
{
label_64628:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64718;
}
else 
{
label_64718:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64808;
}
else 
{
label_64808:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64934;
}
else 
{
label_64934:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65204;
}
else 
{
label_65204:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65294;
}
else 
{
label_65294:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65384;
}
else 
{
label_65384:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65474;
}
else 
{
label_65474:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65564;
}
else 
{
label_65564:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65654;
}
else 
{
label_65654:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65744;
}
else 
{
label_65744:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65834;
}
else 
{
label_65834:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65960;
}
else 
{
label_65960:; 
if (((int)S3_zero_st) == 0)
{
goto label_61862;
}
else 
{
}
goto label_21091;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_61047;
}
else 
{
label_61047:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_61087;
}
else 
{
label_61087:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_61127;
}
else 
{
label_61127:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_61167;
}
else 
{
label_61167:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_61207;
}
else 
{
label_61207:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_61247;
}
else 
{
label_61247:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_61287;
}
else 
{
label_61287:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_61327;
}
else 
{
label_61327:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_61383;
}
else 
{
label_61383:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_61503;
}
else 
{
label_61503:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_61543;
}
else 
{
label_61543:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_61583;
}
else 
{
label_61583:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_61623;
}
else 
{
label_61623:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_61663;
}
else 
{
label_61663:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_61703;
}
else 
{
label_61703:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_61743;
}
else 
{
label_61743:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_61783;
}
else 
{
label_61783:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_61839;
}
else 
{
label_61839:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62668;
}
else 
{
label_62668:; 
main_in1_req_up = 0;
goto label_62629;
}
}
else 
{
label_62629:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62866;
}
else 
{
label_62866:; 
main_in2_req_up = 0;
goto label_62827;
}
}
else 
{
label_62827:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63064;
}
else 
{
label_63064:; 
main_sum_req_up = 0;
goto label_63025;
}
}
else 
{
label_63025:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63262;
}
else 
{
label_63262:; 
main_diff_req_up = 0;
goto label_63223;
}
}
else 
{
label_63223:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63460;
}
else 
{
label_63460:; 
main_pres_req_up = 0;
goto label_63421;
}
}
else 
{
label_63421:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63658;
}
else 
{
label_63658:; 
main_dbl_req_up = 0;
goto label_63619;
}
}
else 
{
label_63619:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63856;
}
else 
{
label_63856:; 
main_zero_req_up = 0;
goto label_63817;
}
}
else 
{
label_63817:; 
label_64006:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64171;
}
else 
{
label_64171:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64261;
}
else 
{
label_64261:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64351;
}
else 
{
label_64351:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64441;
}
else 
{
label_64441:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64531;
}
else 
{
label_64531:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64621;
}
else 
{
label_64621:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64711;
}
else 
{
label_64711:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64801;
}
else 
{
label_64801:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64927;
}
else 
{
label_64927:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65197;
}
else 
{
label_65197:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65287;
}
else 
{
label_65287:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65377;
}
else 
{
label_65377:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65467;
}
else 
{
label_65467:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65557;
}
else 
{
label_65557:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65647;
}
else 
{
label_65647:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65737;
}
else 
{
label_65737:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65827;
}
else 
{
label_65827:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65953;
}
else 
{
label_65953:; 
}
goto label_21098;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_61042;
}
else 
{
label_61042:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_61082;
}
else 
{
label_61082:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_61122;
}
else 
{
label_61122:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_61162;
}
else 
{
label_61162:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_61202;
}
else 
{
label_61202:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_61242;
}
else 
{
label_61242:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_61282;
}
else 
{
label_61282:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_61322;
}
else 
{
label_61322:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_61378;
}
else 
{
label_61378:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_61498;
}
else 
{
label_61498:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_61538;
}
else 
{
label_61538:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_61578;
}
else 
{
label_61578:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_61618;
}
else 
{
label_61618:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_61658;
}
else 
{
label_61658:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_61698;
}
else 
{
label_61698:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_61738;
}
else 
{
label_61738:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_61778;
}
else 
{
label_61778:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_61834;
}
else 
{
label_61834:; 
label_61863:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_62206:; 
if (((int)S1_addsub_st) == 0)
{
goto label_62220;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_62220:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_62226;
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
label_62246:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_62256;
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
label_62291:; 
}
label_62359:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62679;
}
else 
{
label_62679:; 
main_in1_req_up = 0;
goto label_62618;
}
}
else 
{
label_62618:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62877;
}
else 
{
label_62877:; 
main_in2_req_up = 0;
goto label_62816;
}
}
else 
{
label_62816:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63075;
}
else 
{
label_63075:; 
main_sum_req_up = 0;
goto label_63014;
}
}
else 
{
label_63014:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63273;
}
else 
{
label_63273:; 
main_diff_req_up = 0;
goto label_63212;
}
}
else 
{
label_63212:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63471;
}
else 
{
label_63471:; 
main_pres_req_up = 0;
goto label_63410;
}
}
else 
{
label_63410:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63669;
}
else 
{
label_63669:; 
main_dbl_req_up = 0;
goto label_63608;
}
}
else 
{
label_63608:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63867;
}
else 
{
label_63867:; 
main_zero_req_up = 0;
goto label_63806;
}
}
else 
{
label_63806:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_64033;
}
else 
{
label_64033:; 
main_clk_req_up = 0;
goto label_63991;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64182;
}
else 
{
label_64182:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64272;
}
else 
{
label_64272:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64362;
}
else 
{
label_64362:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64452;
}
else 
{
label_64452:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64542;
}
else 
{
label_64542:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64632;
}
else 
{
label_64632:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64722;
}
else 
{
label_64722:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64812;
}
else 
{
label_64812:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64938;
}
else 
{
label_64938:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65208;
}
else 
{
label_65208:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65298;
}
else 
{
label_65298:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65388;
}
else 
{
label_65388:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65478;
}
else 
{
label_65478:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65568;
}
else 
{
label_65568:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65658;
}
else 
{
label_65658:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65748;
}
else 
{
label_65748:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65838;
}
else 
{
label_65838:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65964;
}
else 
{
label_65964:; 
}
goto label_21087;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_62256:; 
if (((int)S3_zero_st) == 0)
{
goto label_62246;
}
else 
{
}
label_62372:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62680;
}
else 
{
label_62680:; 
main_in1_req_up = 0;
goto label_62617;
}
}
else 
{
label_62617:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62878;
}
else 
{
label_62878:; 
main_in2_req_up = 0;
goto label_62815;
}
}
else 
{
label_62815:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63076;
}
else 
{
label_63076:; 
main_sum_req_up = 0;
goto label_63013;
}
}
else 
{
label_63013:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63274;
}
else 
{
label_63274:; 
main_diff_req_up = 0;
goto label_63211;
}
}
else 
{
label_63211:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63472;
}
else 
{
label_63472:; 
main_pres_req_up = 0;
goto label_63409;
}
}
else 
{
label_63409:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63670;
}
else 
{
label_63670:; 
main_dbl_req_up = 0;
goto label_63607;
}
}
else 
{
label_63607:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63868;
}
else 
{
label_63868:; 
main_zero_req_up = 0;
goto label_63805;
}
}
else 
{
label_63805:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_64034;
}
else 
{
label_64034:; 
main_clk_req_up = 0;
goto label_63990;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64183;
}
else 
{
label_64183:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64273;
}
else 
{
label_64273:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64363;
}
else 
{
label_64363:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64453;
}
else 
{
label_64453:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64543;
}
else 
{
label_64543:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64633;
}
else 
{
label_64633:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64723;
}
else 
{
label_64723:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64813;
}
else 
{
label_64813:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64939;
}
else 
{
label_64939:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65209;
}
else 
{
label_65209:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65299;
}
else 
{
label_65299:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65389;
}
else 
{
label_65389:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65479;
}
else 
{
label_65479:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65569;
}
else 
{
label_65569:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65659;
}
else 
{
label_65659:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65749;
}
else 
{
label_65749:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65839;
}
else 
{
label_65839:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65965;
}
else 
{
label_65965:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_66358:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_66380;
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
}
goto label_62359;
}
else 
{
label_66380:; 
goto label_66358;
}
}
else 
{
}
goto label_62372;
}
}
else 
{
}
goto label_21086;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_62226:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_62255;
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
label_62292:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_62319;
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
goto label_62291;
}
}
else 
{
label_62319:; 
goto label_62292;
}
}
else 
{
}
label_62313:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62678;
}
else 
{
label_62678:; 
main_in1_req_up = 0;
goto label_62619;
}
}
else 
{
label_62619:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62876;
}
else 
{
label_62876:; 
main_in2_req_up = 0;
goto label_62817;
}
}
else 
{
label_62817:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63074;
}
else 
{
label_63074:; 
main_sum_req_up = 0;
goto label_63015;
}
}
else 
{
label_63015:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63272;
}
else 
{
label_63272:; 
main_diff_req_up = 0;
goto label_63213;
}
}
else 
{
label_63213:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63470;
}
else 
{
label_63470:; 
main_pres_req_up = 0;
goto label_63411;
}
}
else 
{
label_63411:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63668;
}
else 
{
label_63668:; 
main_dbl_req_up = 0;
goto label_63609;
}
}
else 
{
label_63609:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63866;
}
else 
{
label_63866:; 
main_zero_req_up = 0;
goto label_63807;
}
}
else 
{
label_63807:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_64032;
}
else 
{
label_64032:; 
main_clk_req_up = 0;
goto label_63992;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64181;
}
else 
{
label_64181:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64271;
}
else 
{
label_64271:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64361;
}
else 
{
label_64361:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64451;
}
else 
{
label_64451:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64541;
}
else 
{
label_64541:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64631;
}
else 
{
label_64631:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64721;
}
else 
{
label_64721:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64811;
}
else 
{
label_64811:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64937;
}
else 
{
label_64937:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65207;
}
else 
{
label_65207:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65297;
}
else 
{
label_65297:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65387;
}
else 
{
label_65387:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65477;
}
else 
{
label_65477:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65567;
}
else 
{
label_65567:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65657;
}
else 
{
label_65657:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65747;
}
else 
{
label_65747:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65837;
}
else 
{
label_65837:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65963;
}
else 
{
label_65963:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_66205:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_66223;
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
}
goto label_62359;
}
else 
{
label_66223:; 
goto label_66205;
}
}
else 
{
}
goto label_62313;
}
}
else 
{
}
goto label_21088;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_62255:; 
goto label_62206;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62677;
}
else 
{
label_62677:; 
main_in1_req_up = 0;
goto label_62620;
}
}
else 
{
label_62620:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62875;
}
else 
{
label_62875:; 
main_in2_req_up = 0;
goto label_62818;
}
}
else 
{
label_62818:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63073;
}
else 
{
label_63073:; 
main_sum_req_up = 0;
goto label_63016;
}
}
else 
{
label_63016:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63271;
}
else 
{
label_63271:; 
main_diff_req_up = 0;
goto label_63214;
}
}
else 
{
label_63214:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63469;
}
else 
{
label_63469:; 
main_pres_req_up = 0;
goto label_63412;
}
}
else 
{
label_63412:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63667;
}
else 
{
label_63667:; 
main_dbl_req_up = 0;
goto label_63610;
}
}
else 
{
label_63610:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63865;
}
else 
{
label_63865:; 
main_zero_req_up = 0;
goto label_63808;
}
}
else 
{
label_63808:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_64031;
}
else 
{
label_64031:; 
main_clk_req_up = 0;
goto label_63993;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64180;
}
else 
{
label_64180:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64270;
}
else 
{
label_64270:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64360;
}
else 
{
label_64360:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64450;
}
else 
{
label_64450:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64540;
}
else 
{
label_64540:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64630;
}
else 
{
label_64630:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64720;
}
else 
{
label_64720:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64810;
}
else 
{
label_64810:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64936;
}
else 
{
label_64936:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65206;
}
else 
{
label_65206:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65296;
}
else 
{
label_65296:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65386;
}
else 
{
label_65386:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65476;
}
else 
{
label_65476:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65566;
}
else 
{
label_65566:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65656;
}
else 
{
label_65656:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65746;
}
else 
{
label_65746:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65836;
}
else 
{
label_65836:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65962;
}
else 
{
label_65962:; 
if (((int)S1_addsub_st) == 0)
{
goto label_66183;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_66183:; 
goto label_61863;
}
else 
{
}
goto label_21089;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_61046;
}
else 
{
label_61046:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_61086;
}
else 
{
label_61086:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_61126;
}
else 
{
label_61126:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_61166;
}
else 
{
label_61166:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_61206;
}
else 
{
label_61206:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_61246;
}
else 
{
label_61246:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_61286;
}
else 
{
label_61286:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_61326;
}
else 
{
label_61326:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_61382;
}
else 
{
label_61382:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_61502;
}
else 
{
label_61502:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_61542;
}
else 
{
label_61542:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_61582;
}
else 
{
label_61582:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_61622;
}
else 
{
label_61622:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_61662;
}
else 
{
label_61662:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_61702;
}
else 
{
label_61702:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_61742;
}
else 
{
label_61742:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_61782;
}
else 
{
label_61782:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_61838;
}
else 
{
label_61838:; 
label_61859:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_61934:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_61952;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62670;
}
else 
{
label_62670:; 
main_in1_req_up = 0;
goto label_62627;
}
}
else 
{
label_62627:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62868;
}
else 
{
label_62868:; 
main_in2_req_up = 0;
goto label_62825;
}
}
else 
{
label_62825:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63066;
}
else 
{
label_63066:; 
main_sum_req_up = 0;
goto label_63023;
}
}
else 
{
label_63023:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63264;
}
else 
{
label_63264:; 
main_diff_req_up = 0;
goto label_63221;
}
}
else 
{
label_63221:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63462;
}
else 
{
label_63462:; 
main_pres_req_up = 0;
goto label_63419;
}
}
else 
{
label_63419:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63660;
}
else 
{
label_63660:; 
main_dbl_req_up = 0;
goto label_63617;
}
}
else 
{
label_63617:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63858;
}
else 
{
label_63858:; 
main_zero_req_up = 0;
goto label_63815;
}
}
else 
{
label_63815:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_64028;
}
else 
{
label_64028:; 
main_clk_req_up = 0;
goto label_64002;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64173;
}
else 
{
label_64173:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64263;
}
else 
{
label_64263:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64353;
}
else 
{
label_64353:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64443;
}
else 
{
label_64443:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64533;
}
else 
{
label_64533:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64623;
}
else 
{
label_64623:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64713;
}
else 
{
label_64713:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64803;
}
else 
{
label_64803:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64929;
}
else 
{
label_64929:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65199;
}
else 
{
label_65199:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65289;
}
else 
{
label_65289:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65379;
}
else 
{
label_65379:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65469;
}
else 
{
label_65469:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65559;
}
else 
{
label_65559:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65649;
}
else 
{
label_65649:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65739;
}
else 
{
label_65739:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65829;
}
else 
{
label_65829:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65955;
}
else 
{
label_65955:; 
}
goto label_21096;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_61952:; 
goto label_61934;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62669;
}
else 
{
label_62669:; 
main_in1_req_up = 0;
goto label_62628;
}
}
else 
{
label_62628:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62867;
}
else 
{
label_62867:; 
main_in2_req_up = 0;
goto label_62826;
}
}
else 
{
label_62826:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63065;
}
else 
{
label_63065:; 
main_sum_req_up = 0;
goto label_63024;
}
}
else 
{
label_63024:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63263;
}
else 
{
label_63263:; 
main_diff_req_up = 0;
goto label_63222;
}
}
else 
{
label_63222:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63461;
}
else 
{
label_63461:; 
main_pres_req_up = 0;
goto label_63420;
}
}
else 
{
label_63420:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63659;
}
else 
{
label_63659:; 
main_dbl_req_up = 0;
goto label_63618;
}
}
else 
{
label_63618:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63857;
}
else 
{
label_63857:; 
main_zero_req_up = 0;
goto label_63816;
}
}
else 
{
label_63816:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_64027;
}
else 
{
label_64027:; 
main_clk_req_up = 0;
goto label_64003;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64172;
}
else 
{
label_64172:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64262;
}
else 
{
label_64262:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64352;
}
else 
{
label_64352:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64442;
}
else 
{
label_64442:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64532;
}
else 
{
label_64532:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64622;
}
else 
{
label_64622:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64712;
}
else 
{
label_64712:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64802;
}
else 
{
label_64802:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64928;
}
else 
{
label_64928:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65198;
}
else 
{
label_65198:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65288;
}
else 
{
label_65288:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65378;
}
else 
{
label_65378:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65468;
}
else 
{
label_65468:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65558;
}
else 
{
label_65558:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65648;
}
else 
{
label_65648:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65738;
}
else 
{
label_65738:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65828;
}
else 
{
label_65828:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65954;
}
else 
{
label_65954:; 
if (((int)S1_addsub_st) == 0)
{
goto label_61859;
}
else 
{
}
goto label_21097;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_61044;
}
else 
{
label_61044:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_61084;
}
else 
{
label_61084:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_61124;
}
else 
{
label_61124:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_61164;
}
else 
{
label_61164:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_61204;
}
else 
{
label_61204:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_61244;
}
else 
{
label_61244:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_61284;
}
else 
{
label_61284:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_61324;
}
else 
{
label_61324:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_61380;
}
else 
{
label_61380:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_61500;
}
else 
{
label_61500:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_61540;
}
else 
{
label_61540:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_61580;
}
else 
{
label_61580:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_61620;
}
else 
{
label_61620:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_61660;
}
else 
{
label_61660:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_61700;
}
else 
{
label_61700:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_61740;
}
else 
{
label_61740:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_61780;
}
else 
{
label_61780:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_61836;
}
else 
{
label_61836:; 
label_61861:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_62076:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_62098;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62674;
}
else 
{
label_62674:; 
main_in1_req_up = 0;
goto label_62623;
}
}
else 
{
label_62623:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62872;
}
else 
{
label_62872:; 
main_in2_req_up = 0;
goto label_62821;
}
}
else 
{
label_62821:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63070;
}
else 
{
label_63070:; 
main_sum_req_up = 0;
goto label_63019;
}
}
else 
{
label_63019:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63268;
}
else 
{
label_63268:; 
main_diff_req_up = 0;
goto label_63217;
}
}
else 
{
label_63217:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63466;
}
else 
{
label_63466:; 
main_pres_req_up = 0;
goto label_63415;
}
}
else 
{
label_63415:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63664;
}
else 
{
label_63664:; 
main_dbl_req_up = 0;
goto label_63613;
}
}
else 
{
label_63613:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63862;
}
else 
{
label_63862:; 
main_zero_req_up = 0;
goto label_63811;
}
}
else 
{
label_63811:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_64030;
}
else 
{
label_64030:; 
main_clk_req_up = 0;
goto label_63998;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64177;
}
else 
{
label_64177:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64267;
}
else 
{
label_64267:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64357;
}
else 
{
label_64357:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64447;
}
else 
{
label_64447:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64537;
}
else 
{
label_64537:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64627;
}
else 
{
label_64627:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64717;
}
else 
{
label_64717:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64807;
}
else 
{
label_64807:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64933;
}
else 
{
label_64933:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65203;
}
else 
{
label_65203:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65293;
}
else 
{
label_65293:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65383;
}
else 
{
label_65383:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65473;
}
else 
{
label_65473:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65563;
}
else 
{
label_65563:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65653;
}
else 
{
label_65653:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65743;
}
else 
{
label_65743:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65833;
}
else 
{
label_65833:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65959;
}
else 
{
label_65959:; 
}
goto label_21092;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_62098:; 
goto label_62076;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62673;
}
else 
{
label_62673:; 
main_in1_req_up = 0;
goto label_62624;
}
}
else 
{
label_62624:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62871;
}
else 
{
label_62871:; 
main_in2_req_up = 0;
goto label_62822;
}
}
else 
{
label_62822:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63069;
}
else 
{
label_63069:; 
main_sum_req_up = 0;
goto label_63020;
}
}
else 
{
label_63020:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63267;
}
else 
{
label_63267:; 
main_diff_req_up = 0;
goto label_63218;
}
}
else 
{
label_63218:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63465;
}
else 
{
label_63465:; 
main_pres_req_up = 0;
goto label_63416;
}
}
else 
{
label_63416:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63663;
}
else 
{
label_63663:; 
main_dbl_req_up = 0;
goto label_63614;
}
}
else 
{
label_63614:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63861;
}
else 
{
label_63861:; 
main_zero_req_up = 0;
goto label_63812;
}
}
else 
{
label_63812:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_64029;
}
else 
{
label_64029:; 
main_clk_req_up = 0;
goto label_63999;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64176;
}
else 
{
label_64176:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64266;
}
else 
{
label_64266:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64356;
}
else 
{
label_64356:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64446;
}
else 
{
label_64446:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64536;
}
else 
{
label_64536:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64626;
}
else 
{
label_64626:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64716;
}
else 
{
label_64716:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64806;
}
else 
{
label_64806:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64932;
}
else 
{
label_64932:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65202;
}
else 
{
label_65202:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65292;
}
else 
{
label_65292:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65382;
}
else 
{
label_65382:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65472;
}
else 
{
label_65472:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65562;
}
else 
{
label_65562:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65652;
}
else 
{
label_65652:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65742;
}
else 
{
label_65742:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65832;
}
else 
{
label_65832:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65958;
}
else 
{
label_65958:; 
if (((int)S3_zero_st) == 0)
{
goto label_61861;
}
else 
{
}
goto label_21093;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_61048;
}
else 
{
label_61048:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_61088;
}
else 
{
label_61088:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_61128;
}
else 
{
label_61128:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_61168;
}
else 
{
label_61168:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_61208;
}
else 
{
label_61208:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_61248;
}
else 
{
label_61248:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_61288;
}
else 
{
label_61288:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_61328;
}
else 
{
label_61328:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_61384;
}
else 
{
label_61384:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_61504;
}
else 
{
label_61504:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_61544;
}
else 
{
label_61544:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_61584;
}
else 
{
label_61584:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_61624;
}
else 
{
label_61624:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_61664;
}
else 
{
label_61664:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_61704;
}
else 
{
label_61704:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_61744;
}
else 
{
label_61744:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_61784;
}
else 
{
label_61784:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_61840;
}
else 
{
label_61840:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_62667;
}
else 
{
label_62667:; 
main_in1_req_up = 0;
goto label_62630;
}
}
else 
{
label_62630:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_62865;
}
else 
{
label_62865:; 
main_in2_req_up = 0;
goto label_62828;
}
}
else 
{
label_62828:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_63063;
}
else 
{
label_63063:; 
main_sum_req_up = 0;
goto label_63026;
}
}
else 
{
label_63026:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_63261;
}
else 
{
label_63261:; 
main_diff_req_up = 0;
goto label_63224;
}
}
else 
{
label_63224:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_63459;
}
else 
{
label_63459:; 
main_pres_req_up = 0;
goto label_63422;
}
}
else 
{
label_63422:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_63657;
}
else 
{
label_63657:; 
main_dbl_req_up = 0;
goto label_63620;
}
}
else 
{
label_63620:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_63855;
}
else 
{
label_63855:; 
main_zero_req_up = 0;
goto label_63818;
}
}
else 
{
label_63818:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_64026;
}
else 
{
label_64026:; 
main_clk_req_up = 0;
goto label_64006;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_64170;
}
else 
{
label_64170:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_64260;
}
else 
{
label_64260:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_64350;
}
else 
{
label_64350:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_64440;
}
else 
{
label_64440:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_64530;
}
else 
{
label_64530:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_64620;
}
else 
{
label_64620:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_64710;
}
else 
{
label_64710:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_64800;
}
else 
{
label_64800:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_64926;
}
else 
{
label_64926:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_65196;
}
else 
{
label_65196:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_65286;
}
else 
{
label_65286:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_65376;
}
else 
{
label_65376:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_65466;
}
else 
{
label_65466:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_65556;
}
else 
{
label_65556:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_65646;
}
else 
{
label_65646:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_65736;
}
else 
{
label_65736:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_65826;
}
else 
{
label_65826:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_65952;
}
else 
{
label_65952:; 
}
goto label_21099;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_13497:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_13526;
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
label_13563:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_13590;
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
goto label_13562;
}
}
else 
{
label_13590:; 
goto label_13563;
}
}
else 
{
}
label_13584:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_13710;
}
else 
{
label_13710:; 
main_in1_req_up = 0;
goto label_13679;
}
}
else 
{
label_13679:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_13809;
}
else 
{
label_13809:; 
main_in2_req_up = 0;
goto label_13778;
}
}
else 
{
label_13778:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_13908;
}
else 
{
label_13908:; 
main_sum_req_up = 0;
goto label_13877;
}
}
else 
{
label_13877:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_14007;
}
else 
{
label_14007:; 
main_diff_req_up = 0;
goto label_13976;
}
}
else 
{
label_13976:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_14106;
}
else 
{
label_14106:; 
main_pres_req_up = 0;
goto label_14075;
}
}
else 
{
label_14075:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_14205;
}
else 
{
label_14205:; 
main_dbl_req_up = 0;
goto label_14174;
}
}
else 
{
label_14174:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_14304;
}
else 
{
label_14304:; 
main_zero_req_up = 0;
goto label_14273;
}
}
else 
{
label_14273:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_14403;
}
else 
{
label_14403:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_14448;
}
else 
{
label_14448:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_14493;
}
else 
{
label_14493:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_14538;
}
else 
{
label_14538:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_14583;
}
else 
{
label_14583:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_14628;
}
else 
{
label_14628:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_14673;
}
else 
{
label_14673:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_14718;
}
else 
{
label_14718:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_14781;
}
else 
{
label_14781:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_14916;
}
else 
{
label_14916:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_14961;
}
else 
{
label_14961:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_15006;
}
else 
{
label_15006:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_15051;
}
else 
{
label_15051:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_15096;
}
else 
{
label_15096:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_15141;
}
else 
{
label_15141:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_15186;
}
else 
{
label_15186:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_15231;
}
else 
{
label_15231:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_15294;
}
else 
{
label_15294:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_15420:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_15438;
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
}
goto label_13630;
}
else 
{
label_15438:; 
goto label_15420;
}
}
else 
{
}
goto label_13584;
}
}
else 
{
}
label_15561:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
label_15597:; 
count = count + 1;
if (count == 5)
{
flag = D_z;
if (D_z == 0)
{
goto label_15723;
}
else 
{
{
__VERIFIER_error();
}
label_15723:; 
count = 0;
goto label_15629;
}
}
else 
{
label_15629:; 
main_clk_val_t = 0;
main_clk_req_up = 1;
{
int kernel_st ;
kernel_st = 0;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_49590;
}
else 
{
label_49590:; 
main_in1_req_up = 0;
goto label_49587;
}
}
else 
{
label_49587:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_49601;
}
else 
{
label_49601:; 
main_in2_req_up = 0;
goto label_49598;
}
}
else 
{
label_49598:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_49612;
}
else 
{
label_49612:; 
main_sum_req_up = 0;
goto label_49609;
}
}
else 
{
label_49609:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_49623;
}
else 
{
label_49623:; 
main_diff_req_up = 0;
goto label_49620;
}
}
else 
{
label_49620:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_49634;
}
else 
{
label_49634:; 
main_pres_req_up = 0;
goto label_49631;
}
}
else 
{
label_49631:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_49645;
}
else 
{
label_49645:; 
main_dbl_req_up = 0;
goto label_49642;
}
}
else 
{
label_49642:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_49656;
}
else 
{
label_49656:; 
main_zero_req_up = 0;
goto label_49653;
}
}
else 
{
label_49653:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_49667;
}
else 
{
label_49667:; 
main_clk_req_up = 0;
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_49771;
}
else 
{
label_49771:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_49811;
}
else 
{
label_49811:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_49851;
}
else 
{
label_49851:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_49891;
}
else 
{
label_49891:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_49931;
}
else 
{
label_49931:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_49971;
}
else 
{
label_49971:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_50011;
}
else 
{
label_50011:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_50051;
}
else 
{
label_50051:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_50107;
}
else 
{
label_50107:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_50227;
}
else 
{
label_50227:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_50267;
}
else 
{
label_50267:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_50307;
}
else 
{
label_50307:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_50347;
}
else 
{
label_50347:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_50387;
}
else 
{
label_50387:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_50427;
}
else 
{
label_50427:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_50467;
}
else 
{
label_50467:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_50507;
}
else 
{
label_50507:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_50563;
}
else 
{
label_50563:; 
label_50594:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_51116:; 
if (((int)S1_addsub_st) == 0)
{
goto label_51130;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_51130:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_51136;
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
label_51156:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_51166;
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
label_51201:; 
}
label_51269:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51413;
}
else 
{
label_51413:; 
main_in1_req_up = 0;
goto label_51344;
}
}
else 
{
label_51344:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51611;
}
else 
{
label_51611:; 
main_in2_req_up = 0;
goto label_51542;
}
}
else 
{
label_51542:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51809;
}
else 
{
label_51809:; 
main_sum_req_up = 0;
goto label_51740;
}
}
else 
{
label_51740:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_52007;
}
else 
{
label_52007:; 
main_diff_req_up = 0;
goto label_51938;
}
}
else 
{
label_51938:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52205;
}
else 
{
label_52205:; 
main_pres_req_up = 0;
goto label_52136;
}
}
else 
{
label_52136:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52403;
}
else 
{
label_52403:; 
main_dbl_req_up = 0;
goto label_52334;
}
}
else 
{
label_52334:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52601;
}
else 
{
label_52601:; 
main_zero_req_up = 0;
goto label_52532;
}
}
else 
{
label_52532:; 
label_52721:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52916;
}
else 
{
label_52916:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_53006;
}
else 
{
label_53006:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53096;
}
else 
{
label_53096:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53186;
}
else 
{
label_53186:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53276;
}
else 
{
label_53276:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53366;
}
else 
{
label_53366:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53456;
}
else 
{
label_53456:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53546;
}
else 
{
label_53546:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53672;
}
else 
{
label_53672:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53942;
}
else 
{
label_53942:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54032;
}
else 
{
label_54032:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54122;
}
else 
{
label_54122:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54212;
}
else 
{
label_54212:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54302;
}
else 
{
label_54302:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54392;
}
else 
{
label_54392:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54482;
}
else 
{
label_54482:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54572;
}
else 
{
label_54572:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54698;
}
else 
{
label_54698:; 
}
goto label_21083;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_51166:; 
if (((int)S3_zero_st) == 0)
{
goto label_51156;
}
else 
{
}
label_51282:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51414;
}
else 
{
label_51414:; 
main_in1_req_up = 0;
goto label_51343;
}
}
else 
{
label_51343:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51612;
}
else 
{
label_51612:; 
main_in2_req_up = 0;
goto label_51541;
}
}
else 
{
label_51541:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51810;
}
else 
{
label_51810:; 
main_sum_req_up = 0;
goto label_51739;
}
}
else 
{
label_51739:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_52008;
}
else 
{
label_52008:; 
main_diff_req_up = 0;
goto label_51937;
}
}
else 
{
label_51937:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52206;
}
else 
{
label_52206:; 
main_pres_req_up = 0;
goto label_52135;
}
}
else 
{
label_52135:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52404;
}
else 
{
label_52404:; 
main_dbl_req_up = 0;
goto label_52333;
}
}
else 
{
label_52333:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52602;
}
else 
{
label_52602:; 
main_zero_req_up = 0;
goto label_52531;
}
}
else 
{
label_52531:; 
label_52720:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52917;
}
else 
{
label_52917:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_53007;
}
else 
{
label_53007:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53097;
}
else 
{
label_53097:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53187;
}
else 
{
label_53187:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53277;
}
else 
{
label_53277:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53367;
}
else 
{
label_53367:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53457;
}
else 
{
label_53457:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53547;
}
else 
{
label_53547:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53673;
}
else 
{
label_53673:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53943;
}
else 
{
label_53943:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54033;
}
else 
{
label_54033:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54123;
}
else 
{
label_54123:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54213;
}
else 
{
label_54213:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54303;
}
else 
{
label_54303:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54393;
}
else 
{
label_54393:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54483;
}
else 
{
label_54483:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54573;
}
else 
{
label_54573:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54699;
}
else 
{
label_54699:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_55158:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_55180;
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
}
goto label_51269;
}
else 
{
label_55180:; 
goto label_55158;
}
}
else 
{
}
goto label_51282;
}
}
else 
{
}
goto label_21082;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_51136:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_51165;
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
label_51202:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_51229;
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
goto label_51201;
}
}
else 
{
label_51229:; 
goto label_51202;
}
}
else 
{
}
label_51223:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51412;
}
else 
{
label_51412:; 
main_in1_req_up = 0;
goto label_51345;
}
}
else 
{
label_51345:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51610;
}
else 
{
label_51610:; 
main_in2_req_up = 0;
goto label_51543;
}
}
else 
{
label_51543:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51808;
}
else 
{
label_51808:; 
main_sum_req_up = 0;
goto label_51741;
}
}
else 
{
label_51741:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_52006;
}
else 
{
label_52006:; 
main_diff_req_up = 0;
goto label_51939;
}
}
else 
{
label_51939:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52204;
}
else 
{
label_52204:; 
main_pres_req_up = 0;
goto label_52137;
}
}
else 
{
label_52137:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52402;
}
else 
{
label_52402:; 
main_dbl_req_up = 0;
goto label_52335;
}
}
else 
{
label_52335:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52600;
}
else 
{
label_52600:; 
main_zero_req_up = 0;
goto label_52533;
}
}
else 
{
label_52533:; 
label_52722:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52915;
}
else 
{
label_52915:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_53005;
}
else 
{
label_53005:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53095;
}
else 
{
label_53095:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53185;
}
else 
{
label_53185:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53275;
}
else 
{
label_53275:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53365;
}
else 
{
label_53365:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53455;
}
else 
{
label_53455:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53545;
}
else 
{
label_53545:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53671;
}
else 
{
label_53671:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53941;
}
else 
{
label_53941:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54031;
}
else 
{
label_54031:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54121;
}
else 
{
label_54121:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54211;
}
else 
{
label_54211:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54301;
}
else 
{
label_54301:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54391;
}
else 
{
label_54391:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54481;
}
else 
{
label_54481:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54571;
}
else 
{
label_54571:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54697;
}
else 
{
label_54697:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_55010:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_55028;
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
}
goto label_51269;
}
else 
{
label_55028:; 
goto label_55010;
}
}
else 
{
}
goto label_51223;
}
}
else 
{
}
goto label_21084;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_51165:; 
goto label_51116;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51411;
}
else 
{
label_51411:; 
main_in1_req_up = 0;
goto label_51346;
}
}
else 
{
label_51346:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51609;
}
else 
{
label_51609:; 
main_in2_req_up = 0;
goto label_51544;
}
}
else 
{
label_51544:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51807;
}
else 
{
label_51807:; 
main_sum_req_up = 0;
goto label_51742;
}
}
else 
{
label_51742:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_52005;
}
else 
{
label_52005:; 
main_diff_req_up = 0;
goto label_51940;
}
}
else 
{
label_51940:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52203;
}
else 
{
label_52203:; 
main_pres_req_up = 0;
goto label_52138;
}
}
else 
{
label_52138:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52401;
}
else 
{
label_52401:; 
main_dbl_req_up = 0;
goto label_52336;
}
}
else 
{
label_52336:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52599;
}
else 
{
label_52599:; 
main_zero_req_up = 0;
goto label_52534;
}
}
else 
{
label_52534:; 
label_52723:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52914;
}
else 
{
label_52914:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_53004;
}
else 
{
label_53004:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53094;
}
else 
{
label_53094:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53184;
}
else 
{
label_53184:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53274;
}
else 
{
label_53274:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53364;
}
else 
{
label_53364:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53454;
}
else 
{
label_53454:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53544;
}
else 
{
label_53544:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53670;
}
else 
{
label_53670:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53940;
}
else 
{
label_53940:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54030;
}
else 
{
label_54030:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54120;
}
else 
{
label_54120:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54210;
}
else 
{
label_54210:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54300;
}
else 
{
label_54300:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54390;
}
else 
{
label_54390:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54480;
}
else 
{
label_54480:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54570;
}
else 
{
label_54570:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54696;
}
else 
{
label_54696:; 
if (((int)S1_addsub_st) == 0)
{
goto label_54911;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_54911:; 
goto label_50594;
}
else 
{
}
goto label_21085;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_49775;
}
else 
{
label_49775:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_49815;
}
else 
{
label_49815:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_49855;
}
else 
{
label_49855:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_49895;
}
else 
{
label_49895:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_49935;
}
else 
{
label_49935:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_49975;
}
else 
{
label_49975:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_50015;
}
else 
{
label_50015:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_50055;
}
else 
{
label_50055:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_50111;
}
else 
{
label_50111:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_50231;
}
else 
{
label_50231:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_50271;
}
else 
{
label_50271:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_50311;
}
else 
{
label_50311:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_50351;
}
else 
{
label_50351:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_50391;
}
else 
{
label_50391:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_50431;
}
else 
{
label_50431:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_50471;
}
else 
{
label_50471:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_50511;
}
else 
{
label_50511:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_50567;
}
else 
{
label_50567:; 
label_50590:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_50735:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_50753;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51402;
}
else 
{
label_51402:; 
main_in1_req_up = 0;
goto label_51355;
}
}
else 
{
label_51355:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51600;
}
else 
{
label_51600:; 
main_in2_req_up = 0;
goto label_51553;
}
}
else 
{
label_51553:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51798;
}
else 
{
label_51798:; 
main_sum_req_up = 0;
goto label_51751;
}
}
else 
{
label_51751:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_51996;
}
else 
{
label_51996:; 
main_diff_req_up = 0;
goto label_51949;
}
}
else 
{
label_51949:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52194;
}
else 
{
label_52194:; 
main_pres_req_up = 0;
goto label_52147;
}
}
else 
{
label_52147:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52392;
}
else 
{
label_52392:; 
main_dbl_req_up = 0;
goto label_52345;
}
}
else 
{
label_52345:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52590;
}
else 
{
label_52590:; 
main_zero_req_up = 0;
goto label_52543;
}
}
else 
{
label_52543:; 
label_52732:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52905;
}
else 
{
label_52905:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_52995;
}
else 
{
label_52995:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53085;
}
else 
{
label_53085:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53175;
}
else 
{
label_53175:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53265;
}
else 
{
label_53265:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53355;
}
else 
{
label_53355:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53445;
}
else 
{
label_53445:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53535;
}
else 
{
label_53535:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53661;
}
else 
{
label_53661:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53931;
}
else 
{
label_53931:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54021;
}
else 
{
label_54021:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54111;
}
else 
{
label_54111:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54201;
}
else 
{
label_54201:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54291;
}
else 
{
label_54291:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54381;
}
else 
{
label_54381:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54471;
}
else 
{
label_54471:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54561;
}
else 
{
label_54561:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54687;
}
else 
{
label_54687:; 
}
goto label_21094;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_50753:; 
goto label_50735;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51401;
}
else 
{
label_51401:; 
main_in1_req_up = 0;
goto label_51356;
}
}
else 
{
label_51356:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51599;
}
else 
{
label_51599:; 
main_in2_req_up = 0;
goto label_51554;
}
}
else 
{
label_51554:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51797;
}
else 
{
label_51797:; 
main_sum_req_up = 0;
goto label_51752;
}
}
else 
{
label_51752:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_51995;
}
else 
{
label_51995:; 
main_diff_req_up = 0;
goto label_51950;
}
}
else 
{
label_51950:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52193;
}
else 
{
label_52193:; 
main_pres_req_up = 0;
goto label_52148;
}
}
else 
{
label_52148:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52391;
}
else 
{
label_52391:; 
main_dbl_req_up = 0;
goto label_52346;
}
}
else 
{
label_52346:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52589;
}
else 
{
label_52589:; 
main_zero_req_up = 0;
goto label_52544;
}
}
else 
{
label_52544:; 
label_52733:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52904;
}
else 
{
label_52904:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_52994;
}
else 
{
label_52994:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53084;
}
else 
{
label_53084:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53174;
}
else 
{
label_53174:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53264;
}
else 
{
label_53264:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53354;
}
else 
{
label_53354:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53444;
}
else 
{
label_53444:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53534;
}
else 
{
label_53534:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53660;
}
else 
{
label_53660:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53930;
}
else 
{
label_53930:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54020;
}
else 
{
label_54020:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54110;
}
else 
{
label_54110:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54200;
}
else 
{
label_54200:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54290;
}
else 
{
label_54290:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54380;
}
else 
{
label_54380:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54470;
}
else 
{
label_54470:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54560;
}
else 
{
label_54560:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54686;
}
else 
{
label_54686:; 
if (((int)S1_addsub_st) == 0)
{
goto label_50590;
}
else 
{
}
goto label_21095;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_49773;
}
else 
{
label_49773:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_49813;
}
else 
{
label_49813:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_49853;
}
else 
{
label_49853:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_49893;
}
else 
{
label_49893:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_49933;
}
else 
{
label_49933:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_49973;
}
else 
{
label_49973:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_50013;
}
else 
{
label_50013:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_50053;
}
else 
{
label_50053:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_50109;
}
else 
{
label_50109:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_50229;
}
else 
{
label_50229:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_50269;
}
else 
{
label_50269:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_50309;
}
else 
{
label_50309:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_50349;
}
else 
{
label_50349:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_50389;
}
else 
{
label_50389:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_50429;
}
else 
{
label_50429:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_50469;
}
else 
{
label_50469:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_50509;
}
else 
{
label_50509:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_50565;
}
else 
{
label_50565:; 
label_50592:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_50871:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_50893;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51406;
}
else 
{
label_51406:; 
main_in1_req_up = 0;
goto label_51351;
}
}
else 
{
label_51351:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51604;
}
else 
{
label_51604:; 
main_in2_req_up = 0;
goto label_51549;
}
}
else 
{
label_51549:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51802;
}
else 
{
label_51802:; 
main_sum_req_up = 0;
goto label_51747;
}
}
else 
{
label_51747:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_52000;
}
else 
{
label_52000:; 
main_diff_req_up = 0;
goto label_51945;
}
}
else 
{
label_51945:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52198;
}
else 
{
label_52198:; 
main_pres_req_up = 0;
goto label_52143;
}
}
else 
{
label_52143:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52396;
}
else 
{
label_52396:; 
main_dbl_req_up = 0;
goto label_52341;
}
}
else 
{
label_52341:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52594;
}
else 
{
label_52594:; 
main_zero_req_up = 0;
goto label_52539;
}
}
else 
{
label_52539:; 
label_52728:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52909;
}
else 
{
label_52909:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_52999;
}
else 
{
label_52999:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53089;
}
else 
{
label_53089:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53179;
}
else 
{
label_53179:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53269;
}
else 
{
label_53269:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53359;
}
else 
{
label_53359:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53449;
}
else 
{
label_53449:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53539;
}
else 
{
label_53539:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53665;
}
else 
{
label_53665:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53935;
}
else 
{
label_53935:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54025;
}
else 
{
label_54025:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54115;
}
else 
{
label_54115:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54205;
}
else 
{
label_54205:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54295;
}
else 
{
label_54295:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54385;
}
else 
{
label_54385:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54475;
}
else 
{
label_54475:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54565;
}
else 
{
label_54565:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54691;
}
else 
{
label_54691:; 
}
goto label_21090;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_50893:; 
goto label_50871;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51405;
}
else 
{
label_51405:; 
main_in1_req_up = 0;
goto label_51352;
}
}
else 
{
label_51352:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51603;
}
else 
{
label_51603:; 
main_in2_req_up = 0;
goto label_51550;
}
}
else 
{
label_51550:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51801;
}
else 
{
label_51801:; 
main_sum_req_up = 0;
goto label_51748;
}
}
else 
{
label_51748:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_51999;
}
else 
{
label_51999:; 
main_diff_req_up = 0;
goto label_51946;
}
}
else 
{
label_51946:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52197;
}
else 
{
label_52197:; 
main_pres_req_up = 0;
goto label_52144;
}
}
else 
{
label_52144:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52395;
}
else 
{
label_52395:; 
main_dbl_req_up = 0;
goto label_52342;
}
}
else 
{
label_52342:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52593;
}
else 
{
label_52593:; 
main_zero_req_up = 0;
goto label_52540;
}
}
else 
{
label_52540:; 
label_52729:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52908;
}
else 
{
label_52908:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_52998;
}
else 
{
label_52998:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53088;
}
else 
{
label_53088:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53178;
}
else 
{
label_53178:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53268;
}
else 
{
label_53268:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53358;
}
else 
{
label_53358:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53448;
}
else 
{
label_53448:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53538;
}
else 
{
label_53538:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53664;
}
else 
{
label_53664:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53934;
}
else 
{
label_53934:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54024;
}
else 
{
label_54024:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54114;
}
else 
{
label_54114:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54204;
}
else 
{
label_54204:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54294;
}
else 
{
label_54294:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54384;
}
else 
{
label_54384:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54474;
}
else 
{
label_54474:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54564;
}
else 
{
label_54564:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54690;
}
else 
{
label_54690:; 
if (((int)S3_zero_st) == 0)
{
goto label_50592;
}
else 
{
}
goto label_21091;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_49777;
}
else 
{
label_49777:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_49817;
}
else 
{
label_49817:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_49857;
}
else 
{
label_49857:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_49897;
}
else 
{
label_49897:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_49937;
}
else 
{
label_49937:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_49977;
}
else 
{
label_49977:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_50017;
}
else 
{
label_50017:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_50057;
}
else 
{
label_50057:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_50113;
}
else 
{
label_50113:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_50233;
}
else 
{
label_50233:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_50273;
}
else 
{
label_50273:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_50313;
}
else 
{
label_50313:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_50353;
}
else 
{
label_50353:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_50393;
}
else 
{
label_50393:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_50433;
}
else 
{
label_50433:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_50473;
}
else 
{
label_50473:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_50513;
}
else 
{
label_50513:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_50569;
}
else 
{
label_50569:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51398;
}
else 
{
label_51398:; 
main_in1_req_up = 0;
goto label_51359;
}
}
else 
{
label_51359:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51596;
}
else 
{
label_51596:; 
main_in2_req_up = 0;
goto label_51557;
}
}
else 
{
label_51557:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51794;
}
else 
{
label_51794:; 
main_sum_req_up = 0;
goto label_51755;
}
}
else 
{
label_51755:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_51992;
}
else 
{
label_51992:; 
main_diff_req_up = 0;
goto label_51953;
}
}
else 
{
label_51953:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52190;
}
else 
{
label_52190:; 
main_pres_req_up = 0;
goto label_52151;
}
}
else 
{
label_52151:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52388;
}
else 
{
label_52388:; 
main_dbl_req_up = 0;
goto label_52349;
}
}
else 
{
label_52349:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52586;
}
else 
{
label_52586:; 
main_zero_req_up = 0;
goto label_52547;
}
}
else 
{
label_52547:; 
label_52736:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52901;
}
else 
{
label_52901:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_52991;
}
else 
{
label_52991:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53081;
}
else 
{
label_53081:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53171;
}
else 
{
label_53171:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53261;
}
else 
{
label_53261:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53351;
}
else 
{
label_53351:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53441;
}
else 
{
label_53441:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53531;
}
else 
{
label_53531:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53657;
}
else 
{
label_53657:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53927;
}
else 
{
label_53927:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54017;
}
else 
{
label_54017:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54107;
}
else 
{
label_54107:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54197;
}
else 
{
label_54197:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54287;
}
else 
{
label_54287:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54377;
}
else 
{
label_54377:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54467;
}
else 
{
label_54467:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54557;
}
else 
{
label_54557:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54683;
}
else 
{
label_54683:; 
}
goto label_21098;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_49772;
}
else 
{
label_49772:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_49812;
}
else 
{
label_49812:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_49852;
}
else 
{
label_49852:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_49892;
}
else 
{
label_49892:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_49932;
}
else 
{
label_49932:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_49972;
}
else 
{
label_49972:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_50012;
}
else 
{
label_50012:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_50052;
}
else 
{
label_50052:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_50108;
}
else 
{
label_50108:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_50228;
}
else 
{
label_50228:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_50268;
}
else 
{
label_50268:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_50308;
}
else 
{
label_50308:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_50348;
}
else 
{
label_50348:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_50388;
}
else 
{
label_50388:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_50428;
}
else 
{
label_50428:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_50468;
}
else 
{
label_50468:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_50508;
}
else 
{
label_50508:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_50564;
}
else 
{
label_50564:; 
label_50593:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_50936:; 
if (((int)S1_addsub_st) == 0)
{
goto label_50950;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_50950:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_50956;
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
label_50976:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_50986;
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
label_51021:; 
}
label_51089:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51409;
}
else 
{
label_51409:; 
main_in1_req_up = 0;
goto label_51348;
}
}
else 
{
label_51348:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51607;
}
else 
{
label_51607:; 
main_in2_req_up = 0;
goto label_51546;
}
}
else 
{
label_51546:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51805;
}
else 
{
label_51805:; 
main_sum_req_up = 0;
goto label_51744;
}
}
else 
{
label_51744:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_52003;
}
else 
{
label_52003:; 
main_diff_req_up = 0;
goto label_51942;
}
}
else 
{
label_51942:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52201;
}
else 
{
label_52201:; 
main_pres_req_up = 0;
goto label_52140;
}
}
else 
{
label_52140:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52399;
}
else 
{
label_52399:; 
main_dbl_req_up = 0;
goto label_52338;
}
}
else 
{
label_52338:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52597;
}
else 
{
label_52597:; 
main_zero_req_up = 0;
goto label_52536;
}
}
else 
{
label_52536:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_52763;
}
else 
{
label_52763:; 
main_clk_req_up = 0;
goto label_52721;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52912;
}
else 
{
label_52912:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_53002;
}
else 
{
label_53002:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53092;
}
else 
{
label_53092:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53182;
}
else 
{
label_53182:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53272;
}
else 
{
label_53272:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53362;
}
else 
{
label_53362:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53452;
}
else 
{
label_53452:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53542;
}
else 
{
label_53542:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53668;
}
else 
{
label_53668:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53938;
}
else 
{
label_53938:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54028;
}
else 
{
label_54028:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54118;
}
else 
{
label_54118:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54208;
}
else 
{
label_54208:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54298;
}
else 
{
label_54298:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54388;
}
else 
{
label_54388:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54478;
}
else 
{
label_54478:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54568;
}
else 
{
label_54568:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54694;
}
else 
{
label_54694:; 
}
goto label_21087;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_50986:; 
if (((int)S3_zero_st) == 0)
{
goto label_50976;
}
else 
{
}
label_51102:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51410;
}
else 
{
label_51410:; 
main_in1_req_up = 0;
goto label_51347;
}
}
else 
{
label_51347:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51608;
}
else 
{
label_51608:; 
main_in2_req_up = 0;
goto label_51545;
}
}
else 
{
label_51545:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51806;
}
else 
{
label_51806:; 
main_sum_req_up = 0;
goto label_51743;
}
}
else 
{
label_51743:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_52004;
}
else 
{
label_52004:; 
main_diff_req_up = 0;
goto label_51941;
}
}
else 
{
label_51941:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52202;
}
else 
{
label_52202:; 
main_pres_req_up = 0;
goto label_52139;
}
}
else 
{
label_52139:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52400;
}
else 
{
label_52400:; 
main_dbl_req_up = 0;
goto label_52337;
}
}
else 
{
label_52337:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52598;
}
else 
{
label_52598:; 
main_zero_req_up = 0;
goto label_52535;
}
}
else 
{
label_52535:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_52764;
}
else 
{
label_52764:; 
main_clk_req_up = 0;
goto label_52720;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52913;
}
else 
{
label_52913:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_53003;
}
else 
{
label_53003:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53093;
}
else 
{
label_53093:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53183;
}
else 
{
label_53183:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53273;
}
else 
{
label_53273:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53363;
}
else 
{
label_53363:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53453;
}
else 
{
label_53453:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53543;
}
else 
{
label_53543:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53669;
}
else 
{
label_53669:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53939;
}
else 
{
label_53939:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54029;
}
else 
{
label_54029:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54119;
}
else 
{
label_54119:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54209;
}
else 
{
label_54209:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54299;
}
else 
{
label_54299:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54389;
}
else 
{
label_54389:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54479;
}
else 
{
label_54479:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54569;
}
else 
{
label_54569:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54695;
}
else 
{
label_54695:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_55088:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_55110;
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
}
goto label_51089;
}
else 
{
label_55110:; 
goto label_55088;
}
}
else 
{
}
goto label_51102;
}
}
else 
{
}
goto label_21086;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_50956:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_50985;
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
label_51022:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_51049;
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
goto label_51021;
}
}
else 
{
label_51049:; 
goto label_51022;
}
}
else 
{
}
label_51043:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51408;
}
else 
{
label_51408:; 
main_in1_req_up = 0;
goto label_51349;
}
}
else 
{
label_51349:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51606;
}
else 
{
label_51606:; 
main_in2_req_up = 0;
goto label_51547;
}
}
else 
{
label_51547:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51804;
}
else 
{
label_51804:; 
main_sum_req_up = 0;
goto label_51745;
}
}
else 
{
label_51745:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_52002;
}
else 
{
label_52002:; 
main_diff_req_up = 0;
goto label_51943;
}
}
else 
{
label_51943:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52200;
}
else 
{
label_52200:; 
main_pres_req_up = 0;
goto label_52141;
}
}
else 
{
label_52141:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52398;
}
else 
{
label_52398:; 
main_dbl_req_up = 0;
goto label_52339;
}
}
else 
{
label_52339:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52596;
}
else 
{
label_52596:; 
main_zero_req_up = 0;
goto label_52537;
}
}
else 
{
label_52537:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_52762;
}
else 
{
label_52762:; 
main_clk_req_up = 0;
goto label_52722;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52911;
}
else 
{
label_52911:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_53001;
}
else 
{
label_53001:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53091;
}
else 
{
label_53091:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53181;
}
else 
{
label_53181:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53271;
}
else 
{
label_53271:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53361;
}
else 
{
label_53361:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53451;
}
else 
{
label_53451:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53541;
}
else 
{
label_53541:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53667;
}
else 
{
label_53667:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53937;
}
else 
{
label_53937:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54027;
}
else 
{
label_54027:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54117;
}
else 
{
label_54117:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54207;
}
else 
{
label_54207:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54297;
}
else 
{
label_54297:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54387;
}
else 
{
label_54387:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54477;
}
else 
{
label_54477:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54567;
}
else 
{
label_54567:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54693;
}
else 
{
label_54693:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_54935:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_54953;
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
}
goto label_51089;
}
else 
{
label_54953:; 
goto label_54935;
}
}
else 
{
}
goto label_51043;
}
}
else 
{
}
goto label_21088;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_50985:; 
goto label_50936;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51407;
}
else 
{
label_51407:; 
main_in1_req_up = 0;
goto label_51350;
}
}
else 
{
label_51350:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51605;
}
else 
{
label_51605:; 
main_in2_req_up = 0;
goto label_51548;
}
}
else 
{
label_51548:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51803;
}
else 
{
label_51803:; 
main_sum_req_up = 0;
goto label_51746;
}
}
else 
{
label_51746:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_52001;
}
else 
{
label_52001:; 
main_diff_req_up = 0;
goto label_51944;
}
}
else 
{
label_51944:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52199;
}
else 
{
label_52199:; 
main_pres_req_up = 0;
goto label_52142;
}
}
else 
{
label_52142:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52397;
}
else 
{
label_52397:; 
main_dbl_req_up = 0;
goto label_52340;
}
}
else 
{
label_52340:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52595;
}
else 
{
label_52595:; 
main_zero_req_up = 0;
goto label_52538;
}
}
else 
{
label_52538:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_52761;
}
else 
{
label_52761:; 
main_clk_req_up = 0;
goto label_52723;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52910;
}
else 
{
label_52910:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_53000;
}
else 
{
label_53000:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53090;
}
else 
{
label_53090:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53180;
}
else 
{
label_53180:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53270;
}
else 
{
label_53270:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53360;
}
else 
{
label_53360:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53450;
}
else 
{
label_53450:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53540;
}
else 
{
label_53540:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53666;
}
else 
{
label_53666:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53936;
}
else 
{
label_53936:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54026;
}
else 
{
label_54026:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54116;
}
else 
{
label_54116:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54206;
}
else 
{
label_54206:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54296;
}
else 
{
label_54296:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54386;
}
else 
{
label_54386:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54476;
}
else 
{
label_54476:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54566;
}
else 
{
label_54566:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54692;
}
else 
{
label_54692:; 
if (((int)S1_addsub_st) == 0)
{
goto label_54913;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_54913:; 
goto label_50593;
}
else 
{
}
goto label_21089;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_49776;
}
else 
{
label_49776:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_49816;
}
else 
{
label_49816:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_49856;
}
else 
{
label_49856:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_49896;
}
else 
{
label_49896:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_49936;
}
else 
{
label_49936:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_49976;
}
else 
{
label_49976:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_50016;
}
else 
{
label_50016:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_50056;
}
else 
{
label_50056:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_50112;
}
else 
{
label_50112:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_50232;
}
else 
{
label_50232:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_50272;
}
else 
{
label_50272:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_50312;
}
else 
{
label_50312:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_50352;
}
else 
{
label_50352:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_50392;
}
else 
{
label_50392:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_50432;
}
else 
{
label_50432:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_50472;
}
else 
{
label_50472:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_50512;
}
else 
{
label_50512:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_50568;
}
else 
{
label_50568:; 
label_50589:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_50664:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_50682;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51400;
}
else 
{
label_51400:; 
main_in1_req_up = 0;
goto label_51357;
}
}
else 
{
label_51357:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51598;
}
else 
{
label_51598:; 
main_in2_req_up = 0;
goto label_51555;
}
}
else 
{
label_51555:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51796;
}
else 
{
label_51796:; 
main_sum_req_up = 0;
goto label_51753;
}
}
else 
{
label_51753:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_51994;
}
else 
{
label_51994:; 
main_diff_req_up = 0;
goto label_51951;
}
}
else 
{
label_51951:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52192;
}
else 
{
label_52192:; 
main_pres_req_up = 0;
goto label_52149;
}
}
else 
{
label_52149:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52390;
}
else 
{
label_52390:; 
main_dbl_req_up = 0;
goto label_52347;
}
}
else 
{
label_52347:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52588;
}
else 
{
label_52588:; 
main_zero_req_up = 0;
goto label_52545;
}
}
else 
{
label_52545:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_52758;
}
else 
{
label_52758:; 
main_clk_req_up = 0;
goto label_52732;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52903;
}
else 
{
label_52903:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_52993;
}
else 
{
label_52993:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53083;
}
else 
{
label_53083:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53173;
}
else 
{
label_53173:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53263;
}
else 
{
label_53263:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53353;
}
else 
{
label_53353:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53443;
}
else 
{
label_53443:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53533;
}
else 
{
label_53533:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53659;
}
else 
{
label_53659:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53929;
}
else 
{
label_53929:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54019;
}
else 
{
label_54019:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54109;
}
else 
{
label_54109:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54199;
}
else 
{
label_54199:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54289;
}
else 
{
label_54289:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54379;
}
else 
{
label_54379:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54469;
}
else 
{
label_54469:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54559;
}
else 
{
label_54559:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54685;
}
else 
{
label_54685:; 
}
goto label_21096;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_50682:; 
goto label_50664;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51399;
}
else 
{
label_51399:; 
main_in1_req_up = 0;
goto label_51358;
}
}
else 
{
label_51358:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51597;
}
else 
{
label_51597:; 
main_in2_req_up = 0;
goto label_51556;
}
}
else 
{
label_51556:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51795;
}
else 
{
label_51795:; 
main_sum_req_up = 0;
goto label_51754;
}
}
else 
{
label_51754:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_51993;
}
else 
{
label_51993:; 
main_diff_req_up = 0;
goto label_51952;
}
}
else 
{
label_51952:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52191;
}
else 
{
label_52191:; 
main_pres_req_up = 0;
goto label_52150;
}
}
else 
{
label_52150:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52389;
}
else 
{
label_52389:; 
main_dbl_req_up = 0;
goto label_52348;
}
}
else 
{
label_52348:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52587;
}
else 
{
label_52587:; 
main_zero_req_up = 0;
goto label_52546;
}
}
else 
{
label_52546:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_52757;
}
else 
{
label_52757:; 
main_clk_req_up = 0;
goto label_52733;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52902;
}
else 
{
label_52902:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_52992;
}
else 
{
label_52992:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53082;
}
else 
{
label_53082:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53172;
}
else 
{
label_53172:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53262;
}
else 
{
label_53262:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53352;
}
else 
{
label_53352:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53442;
}
else 
{
label_53442:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53532;
}
else 
{
label_53532:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53658;
}
else 
{
label_53658:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53928;
}
else 
{
label_53928:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54018;
}
else 
{
label_54018:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54108;
}
else 
{
label_54108:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54198;
}
else 
{
label_54198:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54288;
}
else 
{
label_54288:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54378;
}
else 
{
label_54378:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54468;
}
else 
{
label_54468:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54558;
}
else 
{
label_54558:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54684;
}
else 
{
label_54684:; 
if (((int)S1_addsub_st) == 0)
{
goto label_50589;
}
else 
{
}
goto label_21097;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_49774;
}
else 
{
label_49774:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_49814;
}
else 
{
label_49814:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_49854;
}
else 
{
label_49854:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_49894;
}
else 
{
label_49894:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_49934;
}
else 
{
label_49934:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_49974;
}
else 
{
label_49974:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_50014;
}
else 
{
label_50014:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_50054;
}
else 
{
label_50054:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_50110;
}
else 
{
label_50110:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_50230;
}
else 
{
label_50230:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_50270;
}
else 
{
label_50270:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_50310;
}
else 
{
label_50310:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_50350;
}
else 
{
label_50350:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_50390;
}
else 
{
label_50390:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_50430;
}
else 
{
label_50430:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_50470;
}
else 
{
label_50470:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_50510;
}
else 
{
label_50510:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_50566;
}
else 
{
label_50566:; 
label_50591:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_50806:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_50828;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51404;
}
else 
{
label_51404:; 
main_in1_req_up = 0;
goto label_51353;
}
}
else 
{
label_51353:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51602;
}
else 
{
label_51602:; 
main_in2_req_up = 0;
goto label_51551;
}
}
else 
{
label_51551:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51800;
}
else 
{
label_51800:; 
main_sum_req_up = 0;
goto label_51749;
}
}
else 
{
label_51749:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_51998;
}
else 
{
label_51998:; 
main_diff_req_up = 0;
goto label_51947;
}
}
else 
{
label_51947:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52196;
}
else 
{
label_52196:; 
main_pres_req_up = 0;
goto label_52145;
}
}
else 
{
label_52145:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52394;
}
else 
{
label_52394:; 
main_dbl_req_up = 0;
goto label_52343;
}
}
else 
{
label_52343:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52592;
}
else 
{
label_52592:; 
main_zero_req_up = 0;
goto label_52541;
}
}
else 
{
label_52541:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_52760;
}
else 
{
label_52760:; 
main_clk_req_up = 0;
goto label_52728;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52907;
}
else 
{
label_52907:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_52997;
}
else 
{
label_52997:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53087;
}
else 
{
label_53087:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53177;
}
else 
{
label_53177:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53267;
}
else 
{
label_53267:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53357;
}
else 
{
label_53357:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53447;
}
else 
{
label_53447:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53537;
}
else 
{
label_53537:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53663;
}
else 
{
label_53663:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53933;
}
else 
{
label_53933:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54023;
}
else 
{
label_54023:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54113;
}
else 
{
label_54113:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54203;
}
else 
{
label_54203:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54293;
}
else 
{
label_54293:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54383;
}
else 
{
label_54383:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54473;
}
else 
{
label_54473:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54563;
}
else 
{
label_54563:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54689;
}
else 
{
label_54689:; 
}
goto label_21092;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_50828:; 
goto label_50806;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51403;
}
else 
{
label_51403:; 
main_in1_req_up = 0;
goto label_51354;
}
}
else 
{
label_51354:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51601;
}
else 
{
label_51601:; 
main_in2_req_up = 0;
goto label_51552;
}
}
else 
{
label_51552:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51799;
}
else 
{
label_51799:; 
main_sum_req_up = 0;
goto label_51750;
}
}
else 
{
label_51750:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_51997;
}
else 
{
label_51997:; 
main_diff_req_up = 0;
goto label_51948;
}
}
else 
{
label_51948:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52195;
}
else 
{
label_52195:; 
main_pres_req_up = 0;
goto label_52146;
}
}
else 
{
label_52146:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52393;
}
else 
{
label_52393:; 
main_dbl_req_up = 0;
goto label_52344;
}
}
else 
{
label_52344:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52591;
}
else 
{
label_52591:; 
main_zero_req_up = 0;
goto label_52542;
}
}
else 
{
label_52542:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_52759;
}
else 
{
label_52759:; 
main_clk_req_up = 0;
goto label_52729;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52906;
}
else 
{
label_52906:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_52996;
}
else 
{
label_52996:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53086;
}
else 
{
label_53086:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53176;
}
else 
{
label_53176:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53266;
}
else 
{
label_53266:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53356;
}
else 
{
label_53356:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53446;
}
else 
{
label_53446:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53536;
}
else 
{
label_53536:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53662;
}
else 
{
label_53662:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53932;
}
else 
{
label_53932:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54022;
}
else 
{
label_54022:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54112;
}
else 
{
label_54112:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54202;
}
else 
{
label_54202:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54292;
}
else 
{
label_54292:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54382;
}
else 
{
label_54382:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54472;
}
else 
{
label_54472:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54562;
}
else 
{
label_54562:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54688;
}
else 
{
label_54688:; 
if (((int)S3_zero_st) == 0)
{
goto label_50591;
}
else 
{
}
goto label_21093;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_49778;
}
else 
{
label_49778:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_49818;
}
else 
{
label_49818:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_49858;
}
else 
{
label_49858:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_49898;
}
else 
{
label_49898:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_49938;
}
else 
{
label_49938:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_49978;
}
else 
{
label_49978:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_50018;
}
else 
{
label_50018:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_50058;
}
else 
{
label_50058:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_50114;
}
else 
{
label_50114:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_50234;
}
else 
{
label_50234:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_50274;
}
else 
{
label_50274:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_50314;
}
else 
{
label_50314:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_50354;
}
else 
{
label_50354:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_50394;
}
else 
{
label_50394:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_50434;
}
else 
{
label_50434:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_50474;
}
else 
{
label_50474:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_50514;
}
else 
{
label_50514:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_50570;
}
else 
{
label_50570:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_51397;
}
else 
{
label_51397:; 
main_in1_req_up = 0;
goto label_51360;
}
}
else 
{
label_51360:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_51595;
}
else 
{
label_51595:; 
main_in2_req_up = 0;
goto label_51558;
}
}
else 
{
label_51558:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_51793;
}
else 
{
label_51793:; 
main_sum_req_up = 0;
goto label_51756;
}
}
else 
{
label_51756:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_51991;
}
else 
{
label_51991:; 
main_diff_req_up = 0;
goto label_51954;
}
}
else 
{
label_51954:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_52189;
}
else 
{
label_52189:; 
main_pres_req_up = 0;
goto label_52152;
}
}
else 
{
label_52152:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_52387;
}
else 
{
label_52387:; 
main_dbl_req_up = 0;
goto label_52350;
}
}
else 
{
label_52350:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_52585;
}
else 
{
label_52585:; 
main_zero_req_up = 0;
goto label_52548;
}
}
else 
{
label_52548:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_52756;
}
else 
{
label_52756:; 
main_clk_req_up = 0;
goto label_52736;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_52900;
}
else 
{
label_52900:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_52990;
}
else 
{
label_52990:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_53080;
}
else 
{
label_53080:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_53170;
}
else 
{
label_53170:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_53260;
}
else 
{
label_53260:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_53350;
}
else 
{
label_53350:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_53440;
}
else 
{
label_53440:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_53530;
}
else 
{
label_53530:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_53656;
}
else 
{
label_53656:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_53926;
}
else 
{
label_53926:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_54016;
}
else 
{
label_54016:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_54106;
}
else 
{
label_54106:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_54196;
}
else 
{
label_54196:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_54286;
}
else 
{
label_54286:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_54376;
}
else 
{
label_54376:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_54466;
}
else 
{
label_54466:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_54556;
}
else 
{
label_54556:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_54682;
}
else 
{
label_54682:; 
}
goto label_21099;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_13526:; 
goto label_13477;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_13709;
}
else 
{
label_13709:; 
main_in1_req_up = 0;
goto label_13680;
}
}
else 
{
label_13680:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_13808;
}
else 
{
label_13808:; 
main_in2_req_up = 0;
goto label_13779;
}
}
else 
{
label_13779:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_13907;
}
else 
{
label_13907:; 
main_sum_req_up = 0;
goto label_13878;
}
}
else 
{
label_13878:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_14006;
}
else 
{
label_14006:; 
main_diff_req_up = 0;
goto label_13977;
}
}
else 
{
label_13977:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_14105;
}
else 
{
label_14105:; 
main_pres_req_up = 0;
goto label_14076;
}
}
else 
{
label_14076:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_14204;
}
else 
{
label_14204:; 
main_dbl_req_up = 0;
goto label_14175;
}
}
else 
{
label_14175:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_14303;
}
else 
{
label_14303:; 
main_zero_req_up = 0;
goto label_14274;
}
}
else 
{
label_14274:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_14402;
}
else 
{
label_14402:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_14447;
}
else 
{
label_14447:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_14492;
}
else 
{
label_14492:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_14537;
}
else 
{
label_14537:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_14582;
}
else 
{
label_14582:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_14627;
}
else 
{
label_14627:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_14672;
}
else 
{
label_14672:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_14717;
}
else 
{
label_14717:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_14780;
}
else 
{
label_14780:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_14915;
}
else 
{
label_14915:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_14960;
}
else 
{
label_14960:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_15005;
}
else 
{
label_15005:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_15050;
}
else 
{
label_15050:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_15095;
}
else 
{
label_15095:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_15140;
}
else 
{
label_15140:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_15185;
}
else 
{
label_15185:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_15230;
}
else 
{
label_15230:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_15293;
}
else 
{
label_15293:; 
if (((int)S1_addsub_st) == 0)
{
goto label_15403;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_15403:; 
goto label_13302;
}
else 
{
}
label_15560:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
label_15596:; 
count = count + 1;
if (count == 5)
{
flag = D_z;
if (D_z == 0)
{
goto label_15722;
}
else 
{
{
__VERIFIER_error();
}
label_15722:; 
count = 0;
goto label_15630;
}
}
else 
{
label_15630:; 
main_clk_val_t = 0;
main_clk_req_up = 1;
{
int kernel_st ;
kernel_st = 0;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_43955;
}
else 
{
label_43955:; 
main_in1_req_up = 0;
goto label_43952;
}
}
else 
{
label_43952:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_43966;
}
else 
{
label_43966:; 
main_in2_req_up = 0;
goto label_43963;
}
}
else 
{
label_43963:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_43977;
}
else 
{
label_43977:; 
main_sum_req_up = 0;
goto label_43974;
}
}
else 
{
label_43974:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_43988;
}
else 
{
label_43988:; 
main_diff_req_up = 0;
goto label_43985;
}
}
else 
{
label_43985:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_43999;
}
else 
{
label_43999:; 
main_pres_req_up = 0;
goto label_43996;
}
}
else 
{
label_43996:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_44010;
}
else 
{
label_44010:; 
main_dbl_req_up = 0;
goto label_44007;
}
}
else 
{
label_44007:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_44021;
}
else 
{
label_44021:; 
main_zero_req_up = 0;
goto label_44018;
}
}
else 
{
label_44018:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_44032;
}
else 
{
label_44032:; 
main_clk_req_up = 0;
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_44136;
}
else 
{
label_44136:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_44176;
}
else 
{
label_44176:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_44216;
}
else 
{
label_44216:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_44256;
}
else 
{
label_44256:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_44296;
}
else 
{
label_44296:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_44336;
}
else 
{
label_44336:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_44376;
}
else 
{
label_44376:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_44416;
}
else 
{
label_44416:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_44472;
}
else 
{
label_44472:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_44592;
}
else 
{
label_44592:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_44632;
}
else 
{
label_44632:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_44672;
}
else 
{
label_44672:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_44712;
}
else 
{
label_44712:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_44752;
}
else 
{
label_44752:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_44792;
}
else 
{
label_44792:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_44832;
}
else 
{
label_44832:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_44872;
}
else 
{
label_44872:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_44928;
}
else 
{
label_44928:; 
label_44959:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_45481:; 
if (((int)S1_addsub_st) == 0)
{
goto label_45495;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_45495:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_45501;
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
label_45521:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_45531;
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
label_45566:; 
}
label_45634:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45778;
}
else 
{
label_45778:; 
main_in1_req_up = 0;
goto label_45709;
}
}
else 
{
label_45709:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45976;
}
else 
{
label_45976:; 
main_in2_req_up = 0;
goto label_45907;
}
}
else 
{
label_45907:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46174;
}
else 
{
label_46174:; 
main_sum_req_up = 0;
goto label_46105;
}
}
else 
{
label_46105:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46372;
}
else 
{
label_46372:; 
main_diff_req_up = 0;
goto label_46303;
}
}
else 
{
label_46303:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46570;
}
else 
{
label_46570:; 
main_pres_req_up = 0;
goto label_46501;
}
}
else 
{
label_46501:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46768;
}
else 
{
label_46768:; 
main_dbl_req_up = 0;
goto label_46699;
}
}
else 
{
label_46699:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46966;
}
else 
{
label_46966:; 
main_zero_req_up = 0;
goto label_46897;
}
}
else 
{
label_46897:; 
label_47086:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47281;
}
else 
{
label_47281:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47371;
}
else 
{
label_47371:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47461;
}
else 
{
label_47461:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47551;
}
else 
{
label_47551:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47641;
}
else 
{
label_47641:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47731;
}
else 
{
label_47731:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47821;
}
else 
{
label_47821:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47911;
}
else 
{
label_47911:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48037;
}
else 
{
label_48037:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48307;
}
else 
{
label_48307:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48397;
}
else 
{
label_48397:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48487;
}
else 
{
label_48487:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48577;
}
else 
{
label_48577:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48667;
}
else 
{
label_48667:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48757;
}
else 
{
label_48757:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48847;
}
else 
{
label_48847:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48937;
}
else 
{
label_48937:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49063;
}
else 
{
label_49063:; 
}
goto label_21083;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_45531:; 
if (((int)S3_zero_st) == 0)
{
goto label_45521;
}
else 
{
}
label_45647:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45779;
}
else 
{
label_45779:; 
main_in1_req_up = 0;
goto label_45708;
}
}
else 
{
label_45708:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45977;
}
else 
{
label_45977:; 
main_in2_req_up = 0;
goto label_45906;
}
}
else 
{
label_45906:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46175;
}
else 
{
label_46175:; 
main_sum_req_up = 0;
goto label_46104;
}
}
else 
{
label_46104:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46373;
}
else 
{
label_46373:; 
main_diff_req_up = 0;
goto label_46302;
}
}
else 
{
label_46302:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46571;
}
else 
{
label_46571:; 
main_pres_req_up = 0;
goto label_46500;
}
}
else 
{
label_46500:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46769;
}
else 
{
label_46769:; 
main_dbl_req_up = 0;
goto label_46698;
}
}
else 
{
label_46698:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46967;
}
else 
{
label_46967:; 
main_zero_req_up = 0;
goto label_46896;
}
}
else 
{
label_46896:; 
label_47085:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47282;
}
else 
{
label_47282:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47372;
}
else 
{
label_47372:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47462;
}
else 
{
label_47462:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47552;
}
else 
{
label_47552:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47642;
}
else 
{
label_47642:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47732;
}
else 
{
label_47732:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47822;
}
else 
{
label_47822:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47912;
}
else 
{
label_47912:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48038;
}
else 
{
label_48038:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48308;
}
else 
{
label_48308:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48398;
}
else 
{
label_48398:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48488;
}
else 
{
label_48488:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48578;
}
else 
{
label_48578:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48668;
}
else 
{
label_48668:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48758;
}
else 
{
label_48758:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48848;
}
else 
{
label_48848:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48938;
}
else 
{
label_48938:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49064;
}
else 
{
label_49064:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_49523:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_49545;
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
}
goto label_45634;
}
else 
{
label_49545:; 
goto label_49523;
}
}
else 
{
}
goto label_45647;
}
}
else 
{
}
goto label_21082;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_45501:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_45530;
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
label_45567:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_45594;
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
goto label_45566;
}
}
else 
{
label_45594:; 
goto label_45567;
}
}
else 
{
}
label_45588:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45777;
}
else 
{
label_45777:; 
main_in1_req_up = 0;
goto label_45710;
}
}
else 
{
label_45710:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45975;
}
else 
{
label_45975:; 
main_in2_req_up = 0;
goto label_45908;
}
}
else 
{
label_45908:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46173;
}
else 
{
label_46173:; 
main_sum_req_up = 0;
goto label_46106;
}
}
else 
{
label_46106:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46371;
}
else 
{
label_46371:; 
main_diff_req_up = 0;
goto label_46304;
}
}
else 
{
label_46304:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46569;
}
else 
{
label_46569:; 
main_pres_req_up = 0;
goto label_46502;
}
}
else 
{
label_46502:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46767;
}
else 
{
label_46767:; 
main_dbl_req_up = 0;
goto label_46700;
}
}
else 
{
label_46700:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46965;
}
else 
{
label_46965:; 
main_zero_req_up = 0;
goto label_46898;
}
}
else 
{
label_46898:; 
label_47087:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47280;
}
else 
{
label_47280:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47370;
}
else 
{
label_47370:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47460;
}
else 
{
label_47460:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47550;
}
else 
{
label_47550:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47640;
}
else 
{
label_47640:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47730;
}
else 
{
label_47730:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47820;
}
else 
{
label_47820:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47910;
}
else 
{
label_47910:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48036;
}
else 
{
label_48036:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48306;
}
else 
{
label_48306:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48396;
}
else 
{
label_48396:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48486;
}
else 
{
label_48486:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48576;
}
else 
{
label_48576:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48666;
}
else 
{
label_48666:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48756;
}
else 
{
label_48756:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48846;
}
else 
{
label_48846:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48936;
}
else 
{
label_48936:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49062;
}
else 
{
label_49062:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_49375:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_49393;
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
}
goto label_45634;
}
else 
{
label_49393:; 
goto label_49375;
}
}
else 
{
}
goto label_45588;
}
}
else 
{
}
goto label_21084;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_45530:; 
goto label_45481;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45776;
}
else 
{
label_45776:; 
main_in1_req_up = 0;
goto label_45711;
}
}
else 
{
label_45711:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45974;
}
else 
{
label_45974:; 
main_in2_req_up = 0;
goto label_45909;
}
}
else 
{
label_45909:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46172;
}
else 
{
label_46172:; 
main_sum_req_up = 0;
goto label_46107;
}
}
else 
{
label_46107:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46370;
}
else 
{
label_46370:; 
main_diff_req_up = 0;
goto label_46305;
}
}
else 
{
label_46305:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46568;
}
else 
{
label_46568:; 
main_pres_req_up = 0;
goto label_46503;
}
}
else 
{
label_46503:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46766;
}
else 
{
label_46766:; 
main_dbl_req_up = 0;
goto label_46701;
}
}
else 
{
label_46701:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46964;
}
else 
{
label_46964:; 
main_zero_req_up = 0;
goto label_46899;
}
}
else 
{
label_46899:; 
label_47088:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47279;
}
else 
{
label_47279:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47369;
}
else 
{
label_47369:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47459;
}
else 
{
label_47459:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47549;
}
else 
{
label_47549:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47639;
}
else 
{
label_47639:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47729;
}
else 
{
label_47729:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47819;
}
else 
{
label_47819:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47909;
}
else 
{
label_47909:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48035;
}
else 
{
label_48035:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48305;
}
else 
{
label_48305:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48395;
}
else 
{
label_48395:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48485;
}
else 
{
label_48485:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48575;
}
else 
{
label_48575:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48665;
}
else 
{
label_48665:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48755;
}
else 
{
label_48755:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48845;
}
else 
{
label_48845:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48935;
}
else 
{
label_48935:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49061;
}
else 
{
label_49061:; 
if (((int)S1_addsub_st) == 0)
{
goto label_49276;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_49276:; 
goto label_44959;
}
else 
{
}
goto label_21085;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_44140;
}
else 
{
label_44140:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_44180;
}
else 
{
label_44180:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_44220;
}
else 
{
label_44220:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_44260;
}
else 
{
label_44260:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_44300;
}
else 
{
label_44300:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_44340;
}
else 
{
label_44340:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_44380;
}
else 
{
label_44380:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_44420;
}
else 
{
label_44420:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_44476;
}
else 
{
label_44476:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_44596;
}
else 
{
label_44596:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_44636;
}
else 
{
label_44636:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_44676;
}
else 
{
label_44676:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_44716;
}
else 
{
label_44716:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_44756;
}
else 
{
label_44756:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_44796;
}
else 
{
label_44796:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_44836;
}
else 
{
label_44836:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_44876;
}
else 
{
label_44876:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_44932;
}
else 
{
label_44932:; 
label_44955:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_45100:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_45118;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45767;
}
else 
{
label_45767:; 
main_in1_req_up = 0;
goto label_45720;
}
}
else 
{
label_45720:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45965;
}
else 
{
label_45965:; 
main_in2_req_up = 0;
goto label_45918;
}
}
else 
{
label_45918:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46163;
}
else 
{
label_46163:; 
main_sum_req_up = 0;
goto label_46116;
}
}
else 
{
label_46116:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46361;
}
else 
{
label_46361:; 
main_diff_req_up = 0;
goto label_46314;
}
}
else 
{
label_46314:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46559;
}
else 
{
label_46559:; 
main_pres_req_up = 0;
goto label_46512;
}
}
else 
{
label_46512:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46757;
}
else 
{
label_46757:; 
main_dbl_req_up = 0;
goto label_46710;
}
}
else 
{
label_46710:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46955;
}
else 
{
label_46955:; 
main_zero_req_up = 0;
goto label_46908;
}
}
else 
{
label_46908:; 
label_47097:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47270;
}
else 
{
label_47270:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47360;
}
else 
{
label_47360:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47450;
}
else 
{
label_47450:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47540;
}
else 
{
label_47540:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47630;
}
else 
{
label_47630:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47720;
}
else 
{
label_47720:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47810;
}
else 
{
label_47810:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47900;
}
else 
{
label_47900:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48026;
}
else 
{
label_48026:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48296;
}
else 
{
label_48296:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48386;
}
else 
{
label_48386:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48476;
}
else 
{
label_48476:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48566;
}
else 
{
label_48566:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48656;
}
else 
{
label_48656:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48746;
}
else 
{
label_48746:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48836;
}
else 
{
label_48836:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48926;
}
else 
{
label_48926:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49052;
}
else 
{
label_49052:; 
}
goto label_21094;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_45118:; 
goto label_45100;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45766;
}
else 
{
label_45766:; 
main_in1_req_up = 0;
goto label_45721;
}
}
else 
{
label_45721:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45964;
}
else 
{
label_45964:; 
main_in2_req_up = 0;
goto label_45919;
}
}
else 
{
label_45919:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46162;
}
else 
{
label_46162:; 
main_sum_req_up = 0;
goto label_46117;
}
}
else 
{
label_46117:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46360;
}
else 
{
label_46360:; 
main_diff_req_up = 0;
goto label_46315;
}
}
else 
{
label_46315:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46558;
}
else 
{
label_46558:; 
main_pres_req_up = 0;
goto label_46513;
}
}
else 
{
label_46513:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46756;
}
else 
{
label_46756:; 
main_dbl_req_up = 0;
goto label_46711;
}
}
else 
{
label_46711:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46954;
}
else 
{
label_46954:; 
main_zero_req_up = 0;
goto label_46909;
}
}
else 
{
label_46909:; 
label_47098:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47269;
}
else 
{
label_47269:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47359;
}
else 
{
label_47359:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47449;
}
else 
{
label_47449:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47539;
}
else 
{
label_47539:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47629;
}
else 
{
label_47629:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47719;
}
else 
{
label_47719:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47809;
}
else 
{
label_47809:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47899;
}
else 
{
label_47899:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48025;
}
else 
{
label_48025:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48295;
}
else 
{
label_48295:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48385;
}
else 
{
label_48385:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48475;
}
else 
{
label_48475:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48565;
}
else 
{
label_48565:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48655;
}
else 
{
label_48655:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48745;
}
else 
{
label_48745:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48835;
}
else 
{
label_48835:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48925;
}
else 
{
label_48925:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49051;
}
else 
{
label_49051:; 
if (((int)S1_addsub_st) == 0)
{
goto label_44955;
}
else 
{
}
goto label_21095;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_44138;
}
else 
{
label_44138:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_44178;
}
else 
{
label_44178:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_44218;
}
else 
{
label_44218:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_44258;
}
else 
{
label_44258:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_44298;
}
else 
{
label_44298:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_44338;
}
else 
{
label_44338:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_44378;
}
else 
{
label_44378:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_44418;
}
else 
{
label_44418:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_44474;
}
else 
{
label_44474:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_44594;
}
else 
{
label_44594:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_44634;
}
else 
{
label_44634:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_44674;
}
else 
{
label_44674:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_44714;
}
else 
{
label_44714:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_44754;
}
else 
{
label_44754:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_44794;
}
else 
{
label_44794:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_44834;
}
else 
{
label_44834:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_44874;
}
else 
{
label_44874:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_44930;
}
else 
{
label_44930:; 
label_44957:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_45236:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_45258;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45771;
}
else 
{
label_45771:; 
main_in1_req_up = 0;
goto label_45716;
}
}
else 
{
label_45716:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45969;
}
else 
{
label_45969:; 
main_in2_req_up = 0;
goto label_45914;
}
}
else 
{
label_45914:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46167;
}
else 
{
label_46167:; 
main_sum_req_up = 0;
goto label_46112;
}
}
else 
{
label_46112:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46365;
}
else 
{
label_46365:; 
main_diff_req_up = 0;
goto label_46310;
}
}
else 
{
label_46310:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46563;
}
else 
{
label_46563:; 
main_pres_req_up = 0;
goto label_46508;
}
}
else 
{
label_46508:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46761;
}
else 
{
label_46761:; 
main_dbl_req_up = 0;
goto label_46706;
}
}
else 
{
label_46706:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46959;
}
else 
{
label_46959:; 
main_zero_req_up = 0;
goto label_46904;
}
}
else 
{
label_46904:; 
label_47093:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47274;
}
else 
{
label_47274:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47364;
}
else 
{
label_47364:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47454;
}
else 
{
label_47454:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47544;
}
else 
{
label_47544:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47634;
}
else 
{
label_47634:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47724;
}
else 
{
label_47724:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47814;
}
else 
{
label_47814:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47904;
}
else 
{
label_47904:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48030;
}
else 
{
label_48030:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48300;
}
else 
{
label_48300:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48390;
}
else 
{
label_48390:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48480;
}
else 
{
label_48480:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48570;
}
else 
{
label_48570:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48660;
}
else 
{
label_48660:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48750;
}
else 
{
label_48750:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48840;
}
else 
{
label_48840:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48930;
}
else 
{
label_48930:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49056;
}
else 
{
label_49056:; 
}
goto label_21090;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_45258:; 
goto label_45236;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45770;
}
else 
{
label_45770:; 
main_in1_req_up = 0;
goto label_45717;
}
}
else 
{
label_45717:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45968;
}
else 
{
label_45968:; 
main_in2_req_up = 0;
goto label_45915;
}
}
else 
{
label_45915:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46166;
}
else 
{
label_46166:; 
main_sum_req_up = 0;
goto label_46113;
}
}
else 
{
label_46113:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46364;
}
else 
{
label_46364:; 
main_diff_req_up = 0;
goto label_46311;
}
}
else 
{
label_46311:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46562;
}
else 
{
label_46562:; 
main_pres_req_up = 0;
goto label_46509;
}
}
else 
{
label_46509:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46760;
}
else 
{
label_46760:; 
main_dbl_req_up = 0;
goto label_46707;
}
}
else 
{
label_46707:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46958;
}
else 
{
label_46958:; 
main_zero_req_up = 0;
goto label_46905;
}
}
else 
{
label_46905:; 
label_47094:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47273;
}
else 
{
label_47273:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47363;
}
else 
{
label_47363:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47453;
}
else 
{
label_47453:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47543;
}
else 
{
label_47543:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47633;
}
else 
{
label_47633:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47723;
}
else 
{
label_47723:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47813;
}
else 
{
label_47813:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47903;
}
else 
{
label_47903:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48029;
}
else 
{
label_48029:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48299;
}
else 
{
label_48299:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48389;
}
else 
{
label_48389:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48479;
}
else 
{
label_48479:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48569;
}
else 
{
label_48569:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48659;
}
else 
{
label_48659:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48749;
}
else 
{
label_48749:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48839;
}
else 
{
label_48839:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48929;
}
else 
{
label_48929:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49055;
}
else 
{
label_49055:; 
if (((int)S3_zero_st) == 0)
{
goto label_44957;
}
else 
{
}
goto label_21091;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_44142;
}
else 
{
label_44142:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_44182;
}
else 
{
label_44182:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_44222;
}
else 
{
label_44222:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_44262;
}
else 
{
label_44262:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_44302;
}
else 
{
label_44302:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_44342;
}
else 
{
label_44342:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_44382;
}
else 
{
label_44382:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_44422;
}
else 
{
label_44422:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_44478;
}
else 
{
label_44478:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_44598;
}
else 
{
label_44598:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_44638;
}
else 
{
label_44638:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_44678;
}
else 
{
label_44678:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_44718;
}
else 
{
label_44718:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_44758;
}
else 
{
label_44758:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_44798;
}
else 
{
label_44798:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_44838;
}
else 
{
label_44838:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_44878;
}
else 
{
label_44878:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_44934;
}
else 
{
label_44934:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45763;
}
else 
{
label_45763:; 
main_in1_req_up = 0;
goto label_45724;
}
}
else 
{
label_45724:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45961;
}
else 
{
label_45961:; 
main_in2_req_up = 0;
goto label_45922;
}
}
else 
{
label_45922:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46159;
}
else 
{
label_46159:; 
main_sum_req_up = 0;
goto label_46120;
}
}
else 
{
label_46120:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46357;
}
else 
{
label_46357:; 
main_diff_req_up = 0;
goto label_46318;
}
}
else 
{
label_46318:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46555;
}
else 
{
label_46555:; 
main_pres_req_up = 0;
goto label_46516;
}
}
else 
{
label_46516:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46753;
}
else 
{
label_46753:; 
main_dbl_req_up = 0;
goto label_46714;
}
}
else 
{
label_46714:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46951;
}
else 
{
label_46951:; 
main_zero_req_up = 0;
goto label_46912;
}
}
else 
{
label_46912:; 
label_47101:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47266;
}
else 
{
label_47266:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47356;
}
else 
{
label_47356:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47446;
}
else 
{
label_47446:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47536;
}
else 
{
label_47536:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47626;
}
else 
{
label_47626:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47716;
}
else 
{
label_47716:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47806;
}
else 
{
label_47806:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47896;
}
else 
{
label_47896:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48022;
}
else 
{
label_48022:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48292;
}
else 
{
label_48292:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48382;
}
else 
{
label_48382:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48472;
}
else 
{
label_48472:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48562;
}
else 
{
label_48562:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48652;
}
else 
{
label_48652:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48742;
}
else 
{
label_48742:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48832;
}
else 
{
label_48832:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48922;
}
else 
{
label_48922:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49048;
}
else 
{
label_49048:; 
}
goto label_21098;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_44137;
}
else 
{
label_44137:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_44177;
}
else 
{
label_44177:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_44217;
}
else 
{
label_44217:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_44257;
}
else 
{
label_44257:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_44297;
}
else 
{
label_44297:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_44337;
}
else 
{
label_44337:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_44377;
}
else 
{
label_44377:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_44417;
}
else 
{
label_44417:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_44473;
}
else 
{
label_44473:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_44593;
}
else 
{
label_44593:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_44633;
}
else 
{
label_44633:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_44673;
}
else 
{
label_44673:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_44713;
}
else 
{
label_44713:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_44753;
}
else 
{
label_44753:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_44793;
}
else 
{
label_44793:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_44833;
}
else 
{
label_44833:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_44873;
}
else 
{
label_44873:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_44929;
}
else 
{
label_44929:; 
label_44958:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_45301:; 
if (((int)S1_addsub_st) == 0)
{
goto label_45315;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_45315:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_45321;
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
label_45341:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_45351;
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
label_45386:; 
}
label_45454:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45774;
}
else 
{
label_45774:; 
main_in1_req_up = 0;
goto label_45713;
}
}
else 
{
label_45713:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45972;
}
else 
{
label_45972:; 
main_in2_req_up = 0;
goto label_45911;
}
}
else 
{
label_45911:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46170;
}
else 
{
label_46170:; 
main_sum_req_up = 0;
goto label_46109;
}
}
else 
{
label_46109:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46368;
}
else 
{
label_46368:; 
main_diff_req_up = 0;
goto label_46307;
}
}
else 
{
label_46307:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46566;
}
else 
{
label_46566:; 
main_pres_req_up = 0;
goto label_46505;
}
}
else 
{
label_46505:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46764;
}
else 
{
label_46764:; 
main_dbl_req_up = 0;
goto label_46703;
}
}
else 
{
label_46703:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46962;
}
else 
{
label_46962:; 
main_zero_req_up = 0;
goto label_46901;
}
}
else 
{
label_46901:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_47128;
}
else 
{
label_47128:; 
main_clk_req_up = 0;
goto label_47086;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47277;
}
else 
{
label_47277:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47367;
}
else 
{
label_47367:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47457;
}
else 
{
label_47457:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47547;
}
else 
{
label_47547:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47637;
}
else 
{
label_47637:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47727;
}
else 
{
label_47727:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47817;
}
else 
{
label_47817:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47907;
}
else 
{
label_47907:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48033;
}
else 
{
label_48033:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48303;
}
else 
{
label_48303:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48393;
}
else 
{
label_48393:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48483;
}
else 
{
label_48483:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48573;
}
else 
{
label_48573:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48663;
}
else 
{
label_48663:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48753;
}
else 
{
label_48753:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48843;
}
else 
{
label_48843:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48933;
}
else 
{
label_48933:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49059;
}
else 
{
label_49059:; 
}
goto label_21087;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_45351:; 
if (((int)S3_zero_st) == 0)
{
goto label_45341;
}
else 
{
}
label_45467:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45775;
}
else 
{
label_45775:; 
main_in1_req_up = 0;
goto label_45712;
}
}
else 
{
label_45712:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45973;
}
else 
{
label_45973:; 
main_in2_req_up = 0;
goto label_45910;
}
}
else 
{
label_45910:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46171;
}
else 
{
label_46171:; 
main_sum_req_up = 0;
goto label_46108;
}
}
else 
{
label_46108:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46369;
}
else 
{
label_46369:; 
main_diff_req_up = 0;
goto label_46306;
}
}
else 
{
label_46306:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46567;
}
else 
{
label_46567:; 
main_pres_req_up = 0;
goto label_46504;
}
}
else 
{
label_46504:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46765;
}
else 
{
label_46765:; 
main_dbl_req_up = 0;
goto label_46702;
}
}
else 
{
label_46702:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46963;
}
else 
{
label_46963:; 
main_zero_req_up = 0;
goto label_46900;
}
}
else 
{
label_46900:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_47129;
}
else 
{
label_47129:; 
main_clk_req_up = 0;
goto label_47085;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47278;
}
else 
{
label_47278:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47368;
}
else 
{
label_47368:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47458;
}
else 
{
label_47458:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47548;
}
else 
{
label_47548:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47638;
}
else 
{
label_47638:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47728;
}
else 
{
label_47728:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47818;
}
else 
{
label_47818:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47908;
}
else 
{
label_47908:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48034;
}
else 
{
label_48034:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48304;
}
else 
{
label_48304:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48394;
}
else 
{
label_48394:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48484;
}
else 
{
label_48484:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48574;
}
else 
{
label_48574:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48664;
}
else 
{
label_48664:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48754;
}
else 
{
label_48754:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48844;
}
else 
{
label_48844:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48934;
}
else 
{
label_48934:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49060;
}
else 
{
label_49060:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_49453:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_49475;
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
}
goto label_45454;
}
else 
{
label_49475:; 
goto label_49453;
}
}
else 
{
}
goto label_45467;
}
}
else 
{
}
goto label_21086;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_45321:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_45350;
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
label_45387:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_45414;
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
goto label_45386;
}
}
else 
{
label_45414:; 
goto label_45387;
}
}
else 
{
}
label_45408:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45773;
}
else 
{
label_45773:; 
main_in1_req_up = 0;
goto label_45714;
}
}
else 
{
label_45714:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45971;
}
else 
{
label_45971:; 
main_in2_req_up = 0;
goto label_45912;
}
}
else 
{
label_45912:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46169;
}
else 
{
label_46169:; 
main_sum_req_up = 0;
goto label_46110;
}
}
else 
{
label_46110:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46367;
}
else 
{
label_46367:; 
main_diff_req_up = 0;
goto label_46308;
}
}
else 
{
label_46308:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46565;
}
else 
{
label_46565:; 
main_pres_req_up = 0;
goto label_46506;
}
}
else 
{
label_46506:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46763;
}
else 
{
label_46763:; 
main_dbl_req_up = 0;
goto label_46704;
}
}
else 
{
label_46704:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46961;
}
else 
{
label_46961:; 
main_zero_req_up = 0;
goto label_46902;
}
}
else 
{
label_46902:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_47127;
}
else 
{
label_47127:; 
main_clk_req_up = 0;
goto label_47087;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47276;
}
else 
{
label_47276:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47366;
}
else 
{
label_47366:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47456;
}
else 
{
label_47456:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47546;
}
else 
{
label_47546:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47636;
}
else 
{
label_47636:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47726;
}
else 
{
label_47726:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47816;
}
else 
{
label_47816:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47906;
}
else 
{
label_47906:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48032;
}
else 
{
label_48032:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48302;
}
else 
{
label_48302:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48392;
}
else 
{
label_48392:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48482;
}
else 
{
label_48482:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48572;
}
else 
{
label_48572:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48662;
}
else 
{
label_48662:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48752;
}
else 
{
label_48752:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48842;
}
else 
{
label_48842:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48932;
}
else 
{
label_48932:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49058;
}
else 
{
label_49058:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_49300:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_49318;
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
}
goto label_45454;
}
else 
{
label_49318:; 
goto label_49300;
}
}
else 
{
}
goto label_45408;
}
}
else 
{
}
goto label_21088;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_45350:; 
goto label_45301;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45772;
}
else 
{
label_45772:; 
main_in1_req_up = 0;
goto label_45715;
}
}
else 
{
label_45715:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45970;
}
else 
{
label_45970:; 
main_in2_req_up = 0;
goto label_45913;
}
}
else 
{
label_45913:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46168;
}
else 
{
label_46168:; 
main_sum_req_up = 0;
goto label_46111;
}
}
else 
{
label_46111:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46366;
}
else 
{
label_46366:; 
main_diff_req_up = 0;
goto label_46309;
}
}
else 
{
label_46309:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46564;
}
else 
{
label_46564:; 
main_pres_req_up = 0;
goto label_46507;
}
}
else 
{
label_46507:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46762;
}
else 
{
label_46762:; 
main_dbl_req_up = 0;
goto label_46705;
}
}
else 
{
label_46705:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46960;
}
else 
{
label_46960:; 
main_zero_req_up = 0;
goto label_46903;
}
}
else 
{
label_46903:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_47126;
}
else 
{
label_47126:; 
main_clk_req_up = 0;
goto label_47088;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47275;
}
else 
{
label_47275:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47365;
}
else 
{
label_47365:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47455;
}
else 
{
label_47455:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47545;
}
else 
{
label_47545:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47635;
}
else 
{
label_47635:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47725;
}
else 
{
label_47725:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47815;
}
else 
{
label_47815:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47905;
}
else 
{
label_47905:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48031;
}
else 
{
label_48031:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48301;
}
else 
{
label_48301:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48391;
}
else 
{
label_48391:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48481;
}
else 
{
label_48481:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48571;
}
else 
{
label_48571:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48661;
}
else 
{
label_48661:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48751;
}
else 
{
label_48751:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48841;
}
else 
{
label_48841:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48931;
}
else 
{
label_48931:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49057;
}
else 
{
label_49057:; 
if (((int)S1_addsub_st) == 0)
{
goto label_49278;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_49278:; 
goto label_44958;
}
else 
{
}
goto label_21089;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_44141;
}
else 
{
label_44141:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_44181;
}
else 
{
label_44181:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_44221;
}
else 
{
label_44221:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_44261;
}
else 
{
label_44261:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_44301;
}
else 
{
label_44301:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_44341;
}
else 
{
label_44341:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_44381;
}
else 
{
label_44381:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_44421;
}
else 
{
label_44421:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_44477;
}
else 
{
label_44477:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_44597;
}
else 
{
label_44597:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_44637;
}
else 
{
label_44637:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_44677;
}
else 
{
label_44677:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_44717;
}
else 
{
label_44717:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_44757;
}
else 
{
label_44757:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_44797;
}
else 
{
label_44797:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_44837;
}
else 
{
label_44837:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_44877;
}
else 
{
label_44877:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_44933;
}
else 
{
label_44933:; 
label_44954:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_45029:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_45047;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45765;
}
else 
{
label_45765:; 
main_in1_req_up = 0;
goto label_45722;
}
}
else 
{
label_45722:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45963;
}
else 
{
label_45963:; 
main_in2_req_up = 0;
goto label_45920;
}
}
else 
{
label_45920:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46161;
}
else 
{
label_46161:; 
main_sum_req_up = 0;
goto label_46118;
}
}
else 
{
label_46118:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46359;
}
else 
{
label_46359:; 
main_diff_req_up = 0;
goto label_46316;
}
}
else 
{
label_46316:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46557;
}
else 
{
label_46557:; 
main_pres_req_up = 0;
goto label_46514;
}
}
else 
{
label_46514:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46755;
}
else 
{
label_46755:; 
main_dbl_req_up = 0;
goto label_46712;
}
}
else 
{
label_46712:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46953;
}
else 
{
label_46953:; 
main_zero_req_up = 0;
goto label_46910;
}
}
else 
{
label_46910:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_47123;
}
else 
{
label_47123:; 
main_clk_req_up = 0;
goto label_47097;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47268;
}
else 
{
label_47268:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47358;
}
else 
{
label_47358:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47448;
}
else 
{
label_47448:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47538;
}
else 
{
label_47538:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47628;
}
else 
{
label_47628:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47718;
}
else 
{
label_47718:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47808;
}
else 
{
label_47808:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47898;
}
else 
{
label_47898:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48024;
}
else 
{
label_48024:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48294;
}
else 
{
label_48294:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48384;
}
else 
{
label_48384:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48474;
}
else 
{
label_48474:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48564;
}
else 
{
label_48564:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48654;
}
else 
{
label_48654:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48744;
}
else 
{
label_48744:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48834;
}
else 
{
label_48834:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48924;
}
else 
{
label_48924:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49050;
}
else 
{
label_49050:; 
}
goto label_21096;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_45047:; 
goto label_45029;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45764;
}
else 
{
label_45764:; 
main_in1_req_up = 0;
goto label_45723;
}
}
else 
{
label_45723:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45962;
}
else 
{
label_45962:; 
main_in2_req_up = 0;
goto label_45921;
}
}
else 
{
label_45921:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46160;
}
else 
{
label_46160:; 
main_sum_req_up = 0;
goto label_46119;
}
}
else 
{
label_46119:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46358;
}
else 
{
label_46358:; 
main_diff_req_up = 0;
goto label_46317;
}
}
else 
{
label_46317:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46556;
}
else 
{
label_46556:; 
main_pres_req_up = 0;
goto label_46515;
}
}
else 
{
label_46515:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46754;
}
else 
{
label_46754:; 
main_dbl_req_up = 0;
goto label_46713;
}
}
else 
{
label_46713:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46952;
}
else 
{
label_46952:; 
main_zero_req_up = 0;
goto label_46911;
}
}
else 
{
label_46911:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_47122;
}
else 
{
label_47122:; 
main_clk_req_up = 0;
goto label_47098;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47267;
}
else 
{
label_47267:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47357;
}
else 
{
label_47357:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47447;
}
else 
{
label_47447:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47537;
}
else 
{
label_47537:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47627;
}
else 
{
label_47627:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47717;
}
else 
{
label_47717:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47807;
}
else 
{
label_47807:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47897;
}
else 
{
label_47897:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48023;
}
else 
{
label_48023:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48293;
}
else 
{
label_48293:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48383;
}
else 
{
label_48383:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48473;
}
else 
{
label_48473:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48563;
}
else 
{
label_48563:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48653;
}
else 
{
label_48653:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48743;
}
else 
{
label_48743:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48833;
}
else 
{
label_48833:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48923;
}
else 
{
label_48923:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49049;
}
else 
{
label_49049:; 
if (((int)S1_addsub_st) == 0)
{
goto label_44954;
}
else 
{
}
goto label_21097;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_44139;
}
else 
{
label_44139:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_44179;
}
else 
{
label_44179:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_44219;
}
else 
{
label_44219:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_44259;
}
else 
{
label_44259:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_44299;
}
else 
{
label_44299:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_44339;
}
else 
{
label_44339:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_44379;
}
else 
{
label_44379:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_44419;
}
else 
{
label_44419:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_44475;
}
else 
{
label_44475:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_44595;
}
else 
{
label_44595:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_44635;
}
else 
{
label_44635:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_44675;
}
else 
{
label_44675:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_44715;
}
else 
{
label_44715:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_44755;
}
else 
{
label_44755:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_44795;
}
else 
{
label_44795:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_44835;
}
else 
{
label_44835:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_44875;
}
else 
{
label_44875:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_44931;
}
else 
{
label_44931:; 
label_44956:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_45171:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_45193;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45769;
}
else 
{
label_45769:; 
main_in1_req_up = 0;
goto label_45718;
}
}
else 
{
label_45718:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45967;
}
else 
{
label_45967:; 
main_in2_req_up = 0;
goto label_45916;
}
}
else 
{
label_45916:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46165;
}
else 
{
label_46165:; 
main_sum_req_up = 0;
goto label_46114;
}
}
else 
{
label_46114:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46363;
}
else 
{
label_46363:; 
main_diff_req_up = 0;
goto label_46312;
}
}
else 
{
label_46312:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46561;
}
else 
{
label_46561:; 
main_pres_req_up = 0;
goto label_46510;
}
}
else 
{
label_46510:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46759;
}
else 
{
label_46759:; 
main_dbl_req_up = 0;
goto label_46708;
}
}
else 
{
label_46708:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46957;
}
else 
{
label_46957:; 
main_zero_req_up = 0;
goto label_46906;
}
}
else 
{
label_46906:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_47125;
}
else 
{
label_47125:; 
main_clk_req_up = 0;
goto label_47093;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47272;
}
else 
{
label_47272:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47362;
}
else 
{
label_47362:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47452;
}
else 
{
label_47452:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47542;
}
else 
{
label_47542:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47632;
}
else 
{
label_47632:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47722;
}
else 
{
label_47722:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47812;
}
else 
{
label_47812:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47902;
}
else 
{
label_47902:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48028;
}
else 
{
label_48028:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48298;
}
else 
{
label_48298:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48388;
}
else 
{
label_48388:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48478;
}
else 
{
label_48478:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48568;
}
else 
{
label_48568:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48658;
}
else 
{
label_48658:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48748;
}
else 
{
label_48748:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48838;
}
else 
{
label_48838:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48928;
}
else 
{
label_48928:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49054;
}
else 
{
label_49054:; 
}
goto label_21092;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_45193:; 
goto label_45171;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45768;
}
else 
{
label_45768:; 
main_in1_req_up = 0;
goto label_45719;
}
}
else 
{
label_45719:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45966;
}
else 
{
label_45966:; 
main_in2_req_up = 0;
goto label_45917;
}
}
else 
{
label_45917:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46164;
}
else 
{
label_46164:; 
main_sum_req_up = 0;
goto label_46115;
}
}
else 
{
label_46115:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46362;
}
else 
{
label_46362:; 
main_diff_req_up = 0;
goto label_46313;
}
}
else 
{
label_46313:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46560;
}
else 
{
label_46560:; 
main_pres_req_up = 0;
goto label_46511;
}
}
else 
{
label_46511:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46758;
}
else 
{
label_46758:; 
main_dbl_req_up = 0;
goto label_46709;
}
}
else 
{
label_46709:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46956;
}
else 
{
label_46956:; 
main_zero_req_up = 0;
goto label_46907;
}
}
else 
{
label_46907:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_47124;
}
else 
{
label_47124:; 
main_clk_req_up = 0;
goto label_47094;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47271;
}
else 
{
label_47271:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47361;
}
else 
{
label_47361:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47451;
}
else 
{
label_47451:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47541;
}
else 
{
label_47541:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47631;
}
else 
{
label_47631:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47721;
}
else 
{
label_47721:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47811;
}
else 
{
label_47811:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47901;
}
else 
{
label_47901:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48027;
}
else 
{
label_48027:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48297;
}
else 
{
label_48297:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48387;
}
else 
{
label_48387:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48477;
}
else 
{
label_48477:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48567;
}
else 
{
label_48567:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48657;
}
else 
{
label_48657:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48747;
}
else 
{
label_48747:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48837;
}
else 
{
label_48837:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48927;
}
else 
{
label_48927:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49053;
}
else 
{
label_49053:; 
if (((int)S3_zero_st) == 0)
{
goto label_44956;
}
else 
{
}
goto label_21093;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_44143;
}
else 
{
label_44143:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_44183;
}
else 
{
label_44183:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_44223;
}
else 
{
label_44223:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_44263;
}
else 
{
label_44263:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_44303;
}
else 
{
label_44303:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_44343;
}
else 
{
label_44343:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_44383;
}
else 
{
label_44383:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_44423;
}
else 
{
label_44423:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_44479;
}
else 
{
label_44479:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_44599;
}
else 
{
label_44599:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_44639;
}
else 
{
label_44639:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_44679;
}
else 
{
label_44679:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_44719;
}
else 
{
label_44719:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_44759;
}
else 
{
label_44759:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_44799;
}
else 
{
label_44799:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_44839;
}
else 
{
label_44839:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_44879;
}
else 
{
label_44879:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_44935;
}
else 
{
label_44935:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_45762;
}
else 
{
label_45762:; 
main_in1_req_up = 0;
goto label_45725;
}
}
else 
{
label_45725:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_45960;
}
else 
{
label_45960:; 
main_in2_req_up = 0;
goto label_45923;
}
}
else 
{
label_45923:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_46158;
}
else 
{
label_46158:; 
main_sum_req_up = 0;
goto label_46121;
}
}
else 
{
label_46121:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_46356;
}
else 
{
label_46356:; 
main_diff_req_up = 0;
goto label_46319;
}
}
else 
{
label_46319:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_46554;
}
else 
{
label_46554:; 
main_pres_req_up = 0;
goto label_46517;
}
}
else 
{
label_46517:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_46752;
}
else 
{
label_46752:; 
main_dbl_req_up = 0;
goto label_46715;
}
}
else 
{
label_46715:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_46950;
}
else 
{
label_46950:; 
main_zero_req_up = 0;
goto label_46913;
}
}
else 
{
label_46913:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_47121;
}
else 
{
label_47121:; 
main_clk_req_up = 0;
goto label_47101;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_47265;
}
else 
{
label_47265:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_47355;
}
else 
{
label_47355:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_47445;
}
else 
{
label_47445:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_47535;
}
else 
{
label_47535:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_47625;
}
else 
{
label_47625:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_47715;
}
else 
{
label_47715:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_47805;
}
else 
{
label_47805:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_47895;
}
else 
{
label_47895:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_48021;
}
else 
{
label_48021:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_48291;
}
else 
{
label_48291:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_48381;
}
else 
{
label_48381:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_48471;
}
else 
{
label_48471:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_48561;
}
else 
{
label_48561:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_48651;
}
else 
{
label_48651:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_48741;
}
else 
{
label_48741:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_48831;
}
else 
{
label_48831:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_48921;
}
else 
{
label_48921:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_49047;
}
else 
{
label_49047:; 
}
goto label_21099;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_12893;
}
else 
{
label_12893:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_12913;
}
else 
{
label_12913:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_12933;
}
else 
{
label_12933:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_12953;
}
else 
{
label_12953:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_12973;
}
else 
{
label_12973:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_12993;
}
else 
{
label_12993:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_13013;
}
else 
{
label_13013:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_13033;
}
else 
{
label_13033:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_13061;
}
else 
{
label_13061:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_13121;
}
else 
{
label_13121:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_13141;
}
else 
{
label_13141:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_13161;
}
else 
{
label_13161:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_13181;
}
else 
{
label_13181:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_13201;
}
else 
{
label_13201:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_13221;
}
else 
{
label_13221:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_13241;
}
else 
{
label_13241:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_13261;
}
else 
{
label_13261:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_13289;
}
else 
{
label_13289:; 
label_13300:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_13341:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_13359;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_13706;
}
else 
{
label_13706:; 
main_in1_req_up = 0;
goto label_13683;
}
}
else 
{
label_13683:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_13805;
}
else 
{
label_13805:; 
main_in2_req_up = 0;
goto label_13782;
}
}
else 
{
label_13782:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_13904;
}
else 
{
label_13904:; 
main_sum_req_up = 0;
goto label_13881;
}
}
else 
{
label_13881:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_14003;
}
else 
{
label_14003:; 
main_diff_req_up = 0;
goto label_13980;
}
}
else 
{
label_13980:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_14102;
}
else 
{
label_14102:; 
main_pres_req_up = 0;
goto label_14079;
}
}
else 
{
label_14079:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_14201;
}
else 
{
label_14201:; 
main_dbl_req_up = 0;
goto label_14178;
}
}
else 
{
label_14178:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_14300;
}
else 
{
label_14300:; 
main_zero_req_up = 0;
goto label_14277;
}
}
else 
{
label_14277:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_14399;
}
else 
{
label_14399:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_14444;
}
else 
{
label_14444:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_14489;
}
else 
{
label_14489:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_14534;
}
else 
{
label_14534:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_14579;
}
else 
{
label_14579:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_14624;
}
else 
{
label_14624:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_14669;
}
else 
{
label_14669:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_14714;
}
else 
{
label_14714:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_14777;
}
else 
{
label_14777:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_14912;
}
else 
{
label_14912:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_14957;
}
else 
{
label_14957:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_15002;
}
else 
{
label_15002:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_15047;
}
else 
{
label_15047:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_15092;
}
else 
{
label_15092:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_15137;
}
else 
{
label_15137:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_15182;
}
else 
{
label_15182:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_15227;
}
else 
{
label_15227:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_15290;
}
else 
{
label_15290:; 
}
label_15557:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
label_15593:; 
count = count + 1;
if (count == 5)
{
flag = D_z;
if (D_z == 0)
{
goto label_15719;
}
else 
{
{
__VERIFIER_error();
}
label_15719:; 
count = 0;
goto label_15633;
}
}
else 
{
label_15633:; 
main_clk_val_t = 0;
main_clk_req_up = 1;
{
int kernel_st ;
kernel_st = 0;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_27050;
}
else 
{
label_27050:; 
main_in1_req_up = 0;
goto label_27047;
}
}
else 
{
label_27047:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_27061;
}
else 
{
label_27061:; 
main_in2_req_up = 0;
goto label_27058;
}
}
else 
{
label_27058:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_27072;
}
else 
{
label_27072:; 
main_sum_req_up = 0;
goto label_27069;
}
}
else 
{
label_27069:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_27083;
}
else 
{
label_27083:; 
main_diff_req_up = 0;
goto label_27080;
}
}
else 
{
label_27080:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_27094;
}
else 
{
label_27094:; 
main_pres_req_up = 0;
goto label_27091;
}
}
else 
{
label_27091:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_27105;
}
else 
{
label_27105:; 
main_dbl_req_up = 0;
goto label_27102;
}
}
else 
{
label_27102:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_27116;
}
else 
{
label_27116:; 
main_zero_req_up = 0;
goto label_27113;
}
}
else 
{
label_27113:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_27127;
}
else 
{
label_27127:; 
main_clk_req_up = 0;
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_27231;
}
else 
{
label_27231:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_27271;
}
else 
{
label_27271:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_27311;
}
else 
{
label_27311:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_27351;
}
else 
{
label_27351:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_27391;
}
else 
{
label_27391:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_27431;
}
else 
{
label_27431:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_27471;
}
else 
{
label_27471:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_27511;
}
else 
{
label_27511:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_27567;
}
else 
{
label_27567:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_27687;
}
else 
{
label_27687:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_27727;
}
else 
{
label_27727:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_27767;
}
else 
{
label_27767:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_27807;
}
else 
{
label_27807:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_27847;
}
else 
{
label_27847:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_27887;
}
else 
{
label_27887:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_27927;
}
else 
{
label_27927:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_27967;
}
else 
{
label_27967:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_28023;
}
else 
{
label_28023:; 
label_28054:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_28576:; 
if (((int)S1_addsub_st) == 0)
{
goto label_28590;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_28590:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_28596;
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
label_28616:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_28626;
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
label_28661:; 
}
label_28729:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28873;
}
else 
{
label_28873:; 
main_in1_req_up = 0;
goto label_28804;
}
}
else 
{
label_28804:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29071;
}
else 
{
label_29071:; 
main_in2_req_up = 0;
goto label_29002;
}
}
else 
{
label_29002:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29269;
}
else 
{
label_29269:; 
main_sum_req_up = 0;
goto label_29200;
}
}
else 
{
label_29200:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29467;
}
else 
{
label_29467:; 
main_diff_req_up = 0;
goto label_29398;
}
}
else 
{
label_29398:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29665;
}
else 
{
label_29665:; 
main_pres_req_up = 0;
goto label_29596;
}
}
else 
{
label_29596:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29863;
}
else 
{
label_29863:; 
main_dbl_req_up = 0;
goto label_29794;
}
}
else 
{
label_29794:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30061;
}
else 
{
label_30061:; 
main_zero_req_up = 0;
goto label_29992;
}
}
else 
{
label_29992:; 
label_30181:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30376;
}
else 
{
label_30376:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30466;
}
else 
{
label_30466:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30556;
}
else 
{
label_30556:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30646;
}
else 
{
label_30646:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30736;
}
else 
{
label_30736:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30826;
}
else 
{
label_30826:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30916;
}
else 
{
label_30916:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_31006;
}
else 
{
label_31006:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31132;
}
else 
{
label_31132:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31402;
}
else 
{
label_31402:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31492;
}
else 
{
label_31492:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31582;
}
else 
{
label_31582:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31672;
}
else 
{
label_31672:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31762;
}
else 
{
label_31762:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31852;
}
else 
{
label_31852:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31942;
}
else 
{
label_31942:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32032;
}
else 
{
label_32032:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32158;
}
else 
{
label_32158:; 
}
goto label_21083;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_28626:; 
if (((int)S3_zero_st) == 0)
{
goto label_28616;
}
else 
{
}
label_28742:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28874;
}
else 
{
label_28874:; 
main_in1_req_up = 0;
goto label_28803;
}
}
else 
{
label_28803:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29072;
}
else 
{
label_29072:; 
main_in2_req_up = 0;
goto label_29001;
}
}
else 
{
label_29001:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29270;
}
else 
{
label_29270:; 
main_sum_req_up = 0;
goto label_29199;
}
}
else 
{
label_29199:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29468;
}
else 
{
label_29468:; 
main_diff_req_up = 0;
goto label_29397;
}
}
else 
{
label_29397:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29666;
}
else 
{
label_29666:; 
main_pres_req_up = 0;
goto label_29595;
}
}
else 
{
label_29595:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29864;
}
else 
{
label_29864:; 
main_dbl_req_up = 0;
goto label_29793;
}
}
else 
{
label_29793:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30062;
}
else 
{
label_30062:; 
main_zero_req_up = 0;
goto label_29991;
}
}
else 
{
label_29991:; 
label_30180:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30377;
}
else 
{
label_30377:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30467;
}
else 
{
label_30467:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30557;
}
else 
{
label_30557:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30647;
}
else 
{
label_30647:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30737;
}
else 
{
label_30737:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30827;
}
else 
{
label_30827:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30917;
}
else 
{
label_30917:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_31007;
}
else 
{
label_31007:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31133;
}
else 
{
label_31133:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31403;
}
else 
{
label_31403:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31493;
}
else 
{
label_31493:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31583;
}
else 
{
label_31583:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31673;
}
else 
{
label_31673:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31763;
}
else 
{
label_31763:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31853;
}
else 
{
label_31853:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31943;
}
else 
{
label_31943:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32033;
}
else 
{
label_32033:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32159;
}
else 
{
label_32159:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_32618:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_32640;
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
}
goto label_28729;
}
else 
{
label_32640:; 
goto label_32618;
}
}
else 
{
}
goto label_28742;
}
}
else 
{
}
goto label_21082;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_28596:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_28625;
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
label_28662:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_28689;
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
goto label_28661;
}
}
else 
{
label_28689:; 
goto label_28662;
}
}
else 
{
}
label_28683:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28872;
}
else 
{
label_28872:; 
main_in1_req_up = 0;
goto label_28805;
}
}
else 
{
label_28805:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29070;
}
else 
{
label_29070:; 
main_in2_req_up = 0;
goto label_29003;
}
}
else 
{
label_29003:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29268;
}
else 
{
label_29268:; 
main_sum_req_up = 0;
goto label_29201;
}
}
else 
{
label_29201:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29466;
}
else 
{
label_29466:; 
main_diff_req_up = 0;
goto label_29399;
}
}
else 
{
label_29399:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29664;
}
else 
{
label_29664:; 
main_pres_req_up = 0;
goto label_29597;
}
}
else 
{
label_29597:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29862;
}
else 
{
label_29862:; 
main_dbl_req_up = 0;
goto label_29795;
}
}
else 
{
label_29795:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30060;
}
else 
{
label_30060:; 
main_zero_req_up = 0;
goto label_29993;
}
}
else 
{
label_29993:; 
label_30182:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30375;
}
else 
{
label_30375:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30465;
}
else 
{
label_30465:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30555;
}
else 
{
label_30555:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30645;
}
else 
{
label_30645:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30735;
}
else 
{
label_30735:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30825;
}
else 
{
label_30825:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30915;
}
else 
{
label_30915:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_31005;
}
else 
{
label_31005:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31131;
}
else 
{
label_31131:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31401;
}
else 
{
label_31401:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31491;
}
else 
{
label_31491:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31581;
}
else 
{
label_31581:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31671;
}
else 
{
label_31671:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31761;
}
else 
{
label_31761:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31851;
}
else 
{
label_31851:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31941;
}
else 
{
label_31941:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32031;
}
else 
{
label_32031:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32157;
}
else 
{
label_32157:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_32470:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_32488;
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
}
goto label_28729;
}
else 
{
label_32488:; 
goto label_32470;
}
}
else 
{
}
goto label_28683;
}
}
else 
{
}
goto label_21084;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_28625:; 
goto label_28576;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28871;
}
else 
{
label_28871:; 
main_in1_req_up = 0;
goto label_28806;
}
}
else 
{
label_28806:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29069;
}
else 
{
label_29069:; 
main_in2_req_up = 0;
goto label_29004;
}
}
else 
{
label_29004:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29267;
}
else 
{
label_29267:; 
main_sum_req_up = 0;
goto label_29202;
}
}
else 
{
label_29202:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29465;
}
else 
{
label_29465:; 
main_diff_req_up = 0;
goto label_29400;
}
}
else 
{
label_29400:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29663;
}
else 
{
label_29663:; 
main_pres_req_up = 0;
goto label_29598;
}
}
else 
{
label_29598:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29861;
}
else 
{
label_29861:; 
main_dbl_req_up = 0;
goto label_29796;
}
}
else 
{
label_29796:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30059;
}
else 
{
label_30059:; 
main_zero_req_up = 0;
goto label_29994;
}
}
else 
{
label_29994:; 
label_30183:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30374;
}
else 
{
label_30374:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30464;
}
else 
{
label_30464:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30554;
}
else 
{
label_30554:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30644;
}
else 
{
label_30644:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30734;
}
else 
{
label_30734:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30824;
}
else 
{
label_30824:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30914;
}
else 
{
label_30914:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_31004;
}
else 
{
label_31004:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31130;
}
else 
{
label_31130:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31400;
}
else 
{
label_31400:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31490;
}
else 
{
label_31490:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31580;
}
else 
{
label_31580:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31670;
}
else 
{
label_31670:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31760;
}
else 
{
label_31760:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31850;
}
else 
{
label_31850:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31940;
}
else 
{
label_31940:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32030;
}
else 
{
label_32030:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32156;
}
else 
{
label_32156:; 
if (((int)S1_addsub_st) == 0)
{
goto label_32371;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_32371:; 
goto label_28054;
}
else 
{
}
goto label_21085;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_27235;
}
else 
{
label_27235:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_27275;
}
else 
{
label_27275:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_27315;
}
else 
{
label_27315:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_27355;
}
else 
{
label_27355:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_27395;
}
else 
{
label_27395:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_27435;
}
else 
{
label_27435:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_27475;
}
else 
{
label_27475:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_27515;
}
else 
{
label_27515:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_27571;
}
else 
{
label_27571:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_27691;
}
else 
{
label_27691:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_27731;
}
else 
{
label_27731:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_27771;
}
else 
{
label_27771:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_27811;
}
else 
{
label_27811:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_27851;
}
else 
{
label_27851:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_27891;
}
else 
{
label_27891:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_27931;
}
else 
{
label_27931:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_27971;
}
else 
{
label_27971:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_28027;
}
else 
{
label_28027:; 
label_28050:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_28195:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_28213;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28862;
}
else 
{
label_28862:; 
main_in1_req_up = 0;
goto label_28815;
}
}
else 
{
label_28815:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29060;
}
else 
{
label_29060:; 
main_in2_req_up = 0;
goto label_29013;
}
}
else 
{
label_29013:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29258;
}
else 
{
label_29258:; 
main_sum_req_up = 0;
goto label_29211;
}
}
else 
{
label_29211:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29456;
}
else 
{
label_29456:; 
main_diff_req_up = 0;
goto label_29409;
}
}
else 
{
label_29409:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29654;
}
else 
{
label_29654:; 
main_pres_req_up = 0;
goto label_29607;
}
}
else 
{
label_29607:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29852;
}
else 
{
label_29852:; 
main_dbl_req_up = 0;
goto label_29805;
}
}
else 
{
label_29805:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30050;
}
else 
{
label_30050:; 
main_zero_req_up = 0;
goto label_30003;
}
}
else 
{
label_30003:; 
label_30192:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30365;
}
else 
{
label_30365:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30455;
}
else 
{
label_30455:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30545;
}
else 
{
label_30545:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30635;
}
else 
{
label_30635:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30725;
}
else 
{
label_30725:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30815;
}
else 
{
label_30815:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30905;
}
else 
{
label_30905:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_30995;
}
else 
{
label_30995:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31121;
}
else 
{
label_31121:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31391;
}
else 
{
label_31391:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31481;
}
else 
{
label_31481:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31571;
}
else 
{
label_31571:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31661;
}
else 
{
label_31661:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31751;
}
else 
{
label_31751:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31841;
}
else 
{
label_31841:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31931;
}
else 
{
label_31931:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32021;
}
else 
{
label_32021:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32147;
}
else 
{
label_32147:; 
}
goto label_21094;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_28213:; 
goto label_28195;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28861;
}
else 
{
label_28861:; 
main_in1_req_up = 0;
goto label_28816;
}
}
else 
{
label_28816:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29059;
}
else 
{
label_29059:; 
main_in2_req_up = 0;
goto label_29014;
}
}
else 
{
label_29014:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29257;
}
else 
{
label_29257:; 
main_sum_req_up = 0;
goto label_29212;
}
}
else 
{
label_29212:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29455;
}
else 
{
label_29455:; 
main_diff_req_up = 0;
goto label_29410;
}
}
else 
{
label_29410:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29653;
}
else 
{
label_29653:; 
main_pres_req_up = 0;
goto label_29608;
}
}
else 
{
label_29608:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29851;
}
else 
{
label_29851:; 
main_dbl_req_up = 0;
goto label_29806;
}
}
else 
{
label_29806:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30049;
}
else 
{
label_30049:; 
main_zero_req_up = 0;
goto label_30004;
}
}
else 
{
label_30004:; 
label_30193:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30364;
}
else 
{
label_30364:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30454;
}
else 
{
label_30454:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30544;
}
else 
{
label_30544:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30634;
}
else 
{
label_30634:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30724;
}
else 
{
label_30724:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30814;
}
else 
{
label_30814:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30904;
}
else 
{
label_30904:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_30994;
}
else 
{
label_30994:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31120;
}
else 
{
label_31120:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31390;
}
else 
{
label_31390:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31480;
}
else 
{
label_31480:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31570;
}
else 
{
label_31570:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31660;
}
else 
{
label_31660:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31750;
}
else 
{
label_31750:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31840;
}
else 
{
label_31840:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31930;
}
else 
{
label_31930:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32020;
}
else 
{
label_32020:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32146;
}
else 
{
label_32146:; 
if (((int)S1_addsub_st) == 0)
{
goto label_28050;
}
else 
{
}
goto label_21095;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_27233;
}
else 
{
label_27233:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_27273;
}
else 
{
label_27273:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_27313;
}
else 
{
label_27313:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_27353;
}
else 
{
label_27353:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_27393;
}
else 
{
label_27393:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_27433;
}
else 
{
label_27433:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_27473;
}
else 
{
label_27473:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_27513;
}
else 
{
label_27513:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_27569;
}
else 
{
label_27569:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_27689;
}
else 
{
label_27689:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_27729;
}
else 
{
label_27729:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_27769;
}
else 
{
label_27769:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_27809;
}
else 
{
label_27809:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_27849;
}
else 
{
label_27849:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_27889;
}
else 
{
label_27889:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_27929;
}
else 
{
label_27929:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_27969;
}
else 
{
label_27969:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_28025;
}
else 
{
label_28025:; 
label_28052:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_28331:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_28353;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28866;
}
else 
{
label_28866:; 
main_in1_req_up = 0;
goto label_28811;
}
}
else 
{
label_28811:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29064;
}
else 
{
label_29064:; 
main_in2_req_up = 0;
goto label_29009;
}
}
else 
{
label_29009:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29262;
}
else 
{
label_29262:; 
main_sum_req_up = 0;
goto label_29207;
}
}
else 
{
label_29207:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29460;
}
else 
{
label_29460:; 
main_diff_req_up = 0;
goto label_29405;
}
}
else 
{
label_29405:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29658;
}
else 
{
label_29658:; 
main_pres_req_up = 0;
goto label_29603;
}
}
else 
{
label_29603:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29856;
}
else 
{
label_29856:; 
main_dbl_req_up = 0;
goto label_29801;
}
}
else 
{
label_29801:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30054;
}
else 
{
label_30054:; 
main_zero_req_up = 0;
goto label_29999;
}
}
else 
{
label_29999:; 
label_30188:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30369;
}
else 
{
label_30369:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30459;
}
else 
{
label_30459:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30549;
}
else 
{
label_30549:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30639;
}
else 
{
label_30639:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30729;
}
else 
{
label_30729:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30819;
}
else 
{
label_30819:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30909;
}
else 
{
label_30909:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_30999;
}
else 
{
label_30999:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31125;
}
else 
{
label_31125:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31395;
}
else 
{
label_31395:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31485;
}
else 
{
label_31485:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31575;
}
else 
{
label_31575:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31665;
}
else 
{
label_31665:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31755;
}
else 
{
label_31755:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31845;
}
else 
{
label_31845:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31935;
}
else 
{
label_31935:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32025;
}
else 
{
label_32025:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32151;
}
else 
{
label_32151:; 
}
goto label_21090;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_28353:; 
goto label_28331;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28865;
}
else 
{
label_28865:; 
main_in1_req_up = 0;
goto label_28812;
}
}
else 
{
label_28812:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29063;
}
else 
{
label_29063:; 
main_in2_req_up = 0;
goto label_29010;
}
}
else 
{
label_29010:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29261;
}
else 
{
label_29261:; 
main_sum_req_up = 0;
goto label_29208;
}
}
else 
{
label_29208:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29459;
}
else 
{
label_29459:; 
main_diff_req_up = 0;
goto label_29406;
}
}
else 
{
label_29406:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29657;
}
else 
{
label_29657:; 
main_pres_req_up = 0;
goto label_29604;
}
}
else 
{
label_29604:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29855;
}
else 
{
label_29855:; 
main_dbl_req_up = 0;
goto label_29802;
}
}
else 
{
label_29802:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30053;
}
else 
{
label_30053:; 
main_zero_req_up = 0;
goto label_30000;
}
}
else 
{
label_30000:; 
label_30189:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30368;
}
else 
{
label_30368:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30458;
}
else 
{
label_30458:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30548;
}
else 
{
label_30548:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30638;
}
else 
{
label_30638:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30728;
}
else 
{
label_30728:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30818;
}
else 
{
label_30818:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30908;
}
else 
{
label_30908:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_30998;
}
else 
{
label_30998:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31124;
}
else 
{
label_31124:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31394;
}
else 
{
label_31394:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31484;
}
else 
{
label_31484:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31574;
}
else 
{
label_31574:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31664;
}
else 
{
label_31664:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31754;
}
else 
{
label_31754:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31844;
}
else 
{
label_31844:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31934;
}
else 
{
label_31934:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32024;
}
else 
{
label_32024:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32150;
}
else 
{
label_32150:; 
if (((int)S3_zero_st) == 0)
{
goto label_28052;
}
else 
{
}
goto label_21091;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_27237;
}
else 
{
label_27237:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_27277;
}
else 
{
label_27277:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_27317;
}
else 
{
label_27317:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_27357;
}
else 
{
label_27357:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_27397;
}
else 
{
label_27397:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_27437;
}
else 
{
label_27437:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_27477;
}
else 
{
label_27477:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_27517;
}
else 
{
label_27517:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_27573;
}
else 
{
label_27573:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_27693;
}
else 
{
label_27693:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_27733;
}
else 
{
label_27733:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_27773;
}
else 
{
label_27773:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_27813;
}
else 
{
label_27813:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_27853;
}
else 
{
label_27853:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_27893;
}
else 
{
label_27893:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_27933;
}
else 
{
label_27933:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_27973;
}
else 
{
label_27973:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_28029;
}
else 
{
label_28029:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28858;
}
else 
{
label_28858:; 
main_in1_req_up = 0;
goto label_28819;
}
}
else 
{
label_28819:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29056;
}
else 
{
label_29056:; 
main_in2_req_up = 0;
goto label_29017;
}
}
else 
{
label_29017:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29254;
}
else 
{
label_29254:; 
main_sum_req_up = 0;
goto label_29215;
}
}
else 
{
label_29215:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29452;
}
else 
{
label_29452:; 
main_diff_req_up = 0;
goto label_29413;
}
}
else 
{
label_29413:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29650;
}
else 
{
label_29650:; 
main_pres_req_up = 0;
goto label_29611;
}
}
else 
{
label_29611:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29848;
}
else 
{
label_29848:; 
main_dbl_req_up = 0;
goto label_29809;
}
}
else 
{
label_29809:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30046;
}
else 
{
label_30046:; 
main_zero_req_up = 0;
goto label_30007;
}
}
else 
{
label_30007:; 
label_30196:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30361;
}
else 
{
label_30361:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30451;
}
else 
{
label_30451:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30541;
}
else 
{
label_30541:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30631;
}
else 
{
label_30631:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30721;
}
else 
{
label_30721:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30811;
}
else 
{
label_30811:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30901;
}
else 
{
label_30901:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_30991;
}
else 
{
label_30991:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31117;
}
else 
{
label_31117:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31387;
}
else 
{
label_31387:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31477;
}
else 
{
label_31477:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31567;
}
else 
{
label_31567:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31657;
}
else 
{
label_31657:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31747;
}
else 
{
label_31747:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31837;
}
else 
{
label_31837:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31927;
}
else 
{
label_31927:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32017;
}
else 
{
label_32017:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32143;
}
else 
{
label_32143:; 
}
goto label_21098;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_27232;
}
else 
{
label_27232:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_27272;
}
else 
{
label_27272:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_27312;
}
else 
{
label_27312:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_27352;
}
else 
{
label_27352:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_27392;
}
else 
{
label_27392:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_27432;
}
else 
{
label_27432:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_27472;
}
else 
{
label_27472:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_27512;
}
else 
{
label_27512:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_27568;
}
else 
{
label_27568:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_27688;
}
else 
{
label_27688:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_27728;
}
else 
{
label_27728:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_27768;
}
else 
{
label_27768:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_27808;
}
else 
{
label_27808:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_27848;
}
else 
{
label_27848:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_27888;
}
else 
{
label_27888:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_27928;
}
else 
{
label_27928:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_27968;
}
else 
{
label_27968:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_28024;
}
else 
{
label_28024:; 
label_28053:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_28396:; 
if (((int)S1_addsub_st) == 0)
{
goto label_28410;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_28410:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_28416;
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
label_28436:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_28446;
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
label_28481:; 
}
label_28549:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28869;
}
else 
{
label_28869:; 
main_in1_req_up = 0;
goto label_28808;
}
}
else 
{
label_28808:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29067;
}
else 
{
label_29067:; 
main_in2_req_up = 0;
goto label_29006;
}
}
else 
{
label_29006:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29265;
}
else 
{
label_29265:; 
main_sum_req_up = 0;
goto label_29204;
}
}
else 
{
label_29204:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29463;
}
else 
{
label_29463:; 
main_diff_req_up = 0;
goto label_29402;
}
}
else 
{
label_29402:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29661;
}
else 
{
label_29661:; 
main_pres_req_up = 0;
goto label_29600;
}
}
else 
{
label_29600:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29859;
}
else 
{
label_29859:; 
main_dbl_req_up = 0;
goto label_29798;
}
}
else 
{
label_29798:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30057;
}
else 
{
label_30057:; 
main_zero_req_up = 0;
goto label_29996;
}
}
else 
{
label_29996:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_30223;
}
else 
{
label_30223:; 
main_clk_req_up = 0;
goto label_30181;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30372;
}
else 
{
label_30372:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30462;
}
else 
{
label_30462:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30552;
}
else 
{
label_30552:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30642;
}
else 
{
label_30642:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30732;
}
else 
{
label_30732:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30822;
}
else 
{
label_30822:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30912;
}
else 
{
label_30912:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_31002;
}
else 
{
label_31002:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31128;
}
else 
{
label_31128:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31398;
}
else 
{
label_31398:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31488;
}
else 
{
label_31488:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31578;
}
else 
{
label_31578:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31668;
}
else 
{
label_31668:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31758;
}
else 
{
label_31758:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31848;
}
else 
{
label_31848:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31938;
}
else 
{
label_31938:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32028;
}
else 
{
label_32028:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32154;
}
else 
{
label_32154:; 
}
goto label_21087;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_28446:; 
if (((int)S3_zero_st) == 0)
{
goto label_28436;
}
else 
{
}
label_28562:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28870;
}
else 
{
label_28870:; 
main_in1_req_up = 0;
goto label_28807;
}
}
else 
{
label_28807:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29068;
}
else 
{
label_29068:; 
main_in2_req_up = 0;
goto label_29005;
}
}
else 
{
label_29005:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29266;
}
else 
{
label_29266:; 
main_sum_req_up = 0;
goto label_29203;
}
}
else 
{
label_29203:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29464;
}
else 
{
label_29464:; 
main_diff_req_up = 0;
goto label_29401;
}
}
else 
{
label_29401:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29662;
}
else 
{
label_29662:; 
main_pres_req_up = 0;
goto label_29599;
}
}
else 
{
label_29599:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29860;
}
else 
{
label_29860:; 
main_dbl_req_up = 0;
goto label_29797;
}
}
else 
{
label_29797:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30058;
}
else 
{
label_30058:; 
main_zero_req_up = 0;
goto label_29995;
}
}
else 
{
label_29995:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_30224;
}
else 
{
label_30224:; 
main_clk_req_up = 0;
goto label_30180;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30373;
}
else 
{
label_30373:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30463;
}
else 
{
label_30463:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30553;
}
else 
{
label_30553:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30643;
}
else 
{
label_30643:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30733;
}
else 
{
label_30733:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30823;
}
else 
{
label_30823:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30913;
}
else 
{
label_30913:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_31003;
}
else 
{
label_31003:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31129;
}
else 
{
label_31129:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31399;
}
else 
{
label_31399:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31489;
}
else 
{
label_31489:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31579;
}
else 
{
label_31579:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31669;
}
else 
{
label_31669:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31759;
}
else 
{
label_31759:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31849;
}
else 
{
label_31849:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31939;
}
else 
{
label_31939:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32029;
}
else 
{
label_32029:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32155;
}
else 
{
label_32155:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_32548:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_32570;
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
}
goto label_28549;
}
else 
{
label_32570:; 
goto label_32548;
}
}
else 
{
}
goto label_28562;
}
}
else 
{
}
goto label_21086;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_28416:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_28445;
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
label_28482:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_28509;
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
goto label_28481;
}
}
else 
{
label_28509:; 
goto label_28482;
}
}
else 
{
}
label_28503:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28868;
}
else 
{
label_28868:; 
main_in1_req_up = 0;
goto label_28809;
}
}
else 
{
label_28809:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29066;
}
else 
{
label_29066:; 
main_in2_req_up = 0;
goto label_29007;
}
}
else 
{
label_29007:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29264;
}
else 
{
label_29264:; 
main_sum_req_up = 0;
goto label_29205;
}
}
else 
{
label_29205:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29462;
}
else 
{
label_29462:; 
main_diff_req_up = 0;
goto label_29403;
}
}
else 
{
label_29403:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29660;
}
else 
{
label_29660:; 
main_pres_req_up = 0;
goto label_29601;
}
}
else 
{
label_29601:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29858;
}
else 
{
label_29858:; 
main_dbl_req_up = 0;
goto label_29799;
}
}
else 
{
label_29799:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30056;
}
else 
{
label_30056:; 
main_zero_req_up = 0;
goto label_29997;
}
}
else 
{
label_29997:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_30222;
}
else 
{
label_30222:; 
main_clk_req_up = 0;
goto label_30182;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30371;
}
else 
{
label_30371:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30461;
}
else 
{
label_30461:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30551;
}
else 
{
label_30551:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30641;
}
else 
{
label_30641:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30731;
}
else 
{
label_30731:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30821;
}
else 
{
label_30821:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30911;
}
else 
{
label_30911:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_31001;
}
else 
{
label_31001:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31127;
}
else 
{
label_31127:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31397;
}
else 
{
label_31397:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31487;
}
else 
{
label_31487:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31577;
}
else 
{
label_31577:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31667;
}
else 
{
label_31667:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31757;
}
else 
{
label_31757:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31847;
}
else 
{
label_31847:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31937;
}
else 
{
label_31937:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32027;
}
else 
{
label_32027:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32153;
}
else 
{
label_32153:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_32395:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_32413;
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
}
goto label_28549;
}
else 
{
label_32413:; 
goto label_32395;
}
}
else 
{
}
goto label_28503;
}
}
else 
{
}
goto label_21088;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_28445:; 
goto label_28396;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28867;
}
else 
{
label_28867:; 
main_in1_req_up = 0;
goto label_28810;
}
}
else 
{
label_28810:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29065;
}
else 
{
label_29065:; 
main_in2_req_up = 0;
goto label_29008;
}
}
else 
{
label_29008:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29263;
}
else 
{
label_29263:; 
main_sum_req_up = 0;
goto label_29206;
}
}
else 
{
label_29206:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29461;
}
else 
{
label_29461:; 
main_diff_req_up = 0;
goto label_29404;
}
}
else 
{
label_29404:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29659;
}
else 
{
label_29659:; 
main_pres_req_up = 0;
goto label_29602;
}
}
else 
{
label_29602:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29857;
}
else 
{
label_29857:; 
main_dbl_req_up = 0;
goto label_29800;
}
}
else 
{
label_29800:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30055;
}
else 
{
label_30055:; 
main_zero_req_up = 0;
goto label_29998;
}
}
else 
{
label_29998:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_30221;
}
else 
{
label_30221:; 
main_clk_req_up = 0;
goto label_30183;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30370;
}
else 
{
label_30370:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30460;
}
else 
{
label_30460:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30550;
}
else 
{
label_30550:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30640;
}
else 
{
label_30640:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30730;
}
else 
{
label_30730:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30820;
}
else 
{
label_30820:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30910;
}
else 
{
label_30910:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_31000;
}
else 
{
label_31000:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31126;
}
else 
{
label_31126:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31396;
}
else 
{
label_31396:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31486;
}
else 
{
label_31486:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31576;
}
else 
{
label_31576:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31666;
}
else 
{
label_31666:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31756;
}
else 
{
label_31756:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31846;
}
else 
{
label_31846:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31936;
}
else 
{
label_31936:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32026;
}
else 
{
label_32026:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32152;
}
else 
{
label_32152:; 
if (((int)S1_addsub_st) == 0)
{
goto label_32373;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_32373:; 
goto label_28053;
}
else 
{
}
goto label_21089;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_27236;
}
else 
{
label_27236:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_27276;
}
else 
{
label_27276:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_27316;
}
else 
{
label_27316:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_27356;
}
else 
{
label_27356:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_27396;
}
else 
{
label_27396:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_27436;
}
else 
{
label_27436:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_27476;
}
else 
{
label_27476:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_27516;
}
else 
{
label_27516:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_27572;
}
else 
{
label_27572:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_27692;
}
else 
{
label_27692:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_27732;
}
else 
{
label_27732:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_27772;
}
else 
{
label_27772:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_27812;
}
else 
{
label_27812:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_27852;
}
else 
{
label_27852:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_27892;
}
else 
{
label_27892:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_27932;
}
else 
{
label_27932:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_27972;
}
else 
{
label_27972:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_28028;
}
else 
{
label_28028:; 
label_28049:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_28124:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_28142;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28860;
}
else 
{
label_28860:; 
main_in1_req_up = 0;
goto label_28817;
}
}
else 
{
label_28817:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29058;
}
else 
{
label_29058:; 
main_in2_req_up = 0;
goto label_29015;
}
}
else 
{
label_29015:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29256;
}
else 
{
label_29256:; 
main_sum_req_up = 0;
goto label_29213;
}
}
else 
{
label_29213:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29454;
}
else 
{
label_29454:; 
main_diff_req_up = 0;
goto label_29411;
}
}
else 
{
label_29411:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29652;
}
else 
{
label_29652:; 
main_pres_req_up = 0;
goto label_29609;
}
}
else 
{
label_29609:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29850;
}
else 
{
label_29850:; 
main_dbl_req_up = 0;
goto label_29807;
}
}
else 
{
label_29807:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30048;
}
else 
{
label_30048:; 
main_zero_req_up = 0;
goto label_30005;
}
}
else 
{
label_30005:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_30218;
}
else 
{
label_30218:; 
main_clk_req_up = 0;
goto label_30192;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30363;
}
else 
{
label_30363:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30453;
}
else 
{
label_30453:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30543;
}
else 
{
label_30543:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30633;
}
else 
{
label_30633:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30723;
}
else 
{
label_30723:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30813;
}
else 
{
label_30813:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30903;
}
else 
{
label_30903:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_30993;
}
else 
{
label_30993:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31119;
}
else 
{
label_31119:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31389;
}
else 
{
label_31389:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31479;
}
else 
{
label_31479:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31569;
}
else 
{
label_31569:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31659;
}
else 
{
label_31659:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31749;
}
else 
{
label_31749:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31839;
}
else 
{
label_31839:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31929;
}
else 
{
label_31929:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32019;
}
else 
{
label_32019:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32145;
}
else 
{
label_32145:; 
}
goto label_21096;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_28142:; 
goto label_28124;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28859;
}
else 
{
label_28859:; 
main_in1_req_up = 0;
goto label_28818;
}
}
else 
{
label_28818:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29057;
}
else 
{
label_29057:; 
main_in2_req_up = 0;
goto label_29016;
}
}
else 
{
label_29016:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29255;
}
else 
{
label_29255:; 
main_sum_req_up = 0;
goto label_29214;
}
}
else 
{
label_29214:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29453;
}
else 
{
label_29453:; 
main_diff_req_up = 0;
goto label_29412;
}
}
else 
{
label_29412:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29651;
}
else 
{
label_29651:; 
main_pres_req_up = 0;
goto label_29610;
}
}
else 
{
label_29610:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29849;
}
else 
{
label_29849:; 
main_dbl_req_up = 0;
goto label_29808;
}
}
else 
{
label_29808:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30047;
}
else 
{
label_30047:; 
main_zero_req_up = 0;
goto label_30006;
}
}
else 
{
label_30006:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_30217;
}
else 
{
label_30217:; 
main_clk_req_up = 0;
goto label_30193;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30362;
}
else 
{
label_30362:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30452;
}
else 
{
label_30452:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30542;
}
else 
{
label_30542:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30632;
}
else 
{
label_30632:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30722;
}
else 
{
label_30722:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30812;
}
else 
{
label_30812:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30902;
}
else 
{
label_30902:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_30992;
}
else 
{
label_30992:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31118;
}
else 
{
label_31118:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31388;
}
else 
{
label_31388:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31478;
}
else 
{
label_31478:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31568;
}
else 
{
label_31568:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31658;
}
else 
{
label_31658:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31748;
}
else 
{
label_31748:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31838;
}
else 
{
label_31838:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31928;
}
else 
{
label_31928:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32018;
}
else 
{
label_32018:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32144;
}
else 
{
label_32144:; 
if (((int)S1_addsub_st) == 0)
{
goto label_28049;
}
else 
{
}
goto label_21097;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_27234;
}
else 
{
label_27234:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_27274;
}
else 
{
label_27274:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_27314;
}
else 
{
label_27314:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_27354;
}
else 
{
label_27354:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_27394;
}
else 
{
label_27394:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_27434;
}
else 
{
label_27434:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_27474;
}
else 
{
label_27474:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_27514;
}
else 
{
label_27514:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_27570;
}
else 
{
label_27570:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_27690;
}
else 
{
label_27690:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_27730;
}
else 
{
label_27730:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_27770;
}
else 
{
label_27770:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_27810;
}
else 
{
label_27810:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_27850;
}
else 
{
label_27850:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_27890;
}
else 
{
label_27890:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_27930;
}
else 
{
label_27930:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_27970;
}
else 
{
label_27970:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_28026;
}
else 
{
label_28026:; 
label_28051:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_28266:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_28288;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28864;
}
else 
{
label_28864:; 
main_in1_req_up = 0;
goto label_28813;
}
}
else 
{
label_28813:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29062;
}
else 
{
label_29062:; 
main_in2_req_up = 0;
goto label_29011;
}
}
else 
{
label_29011:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29260;
}
else 
{
label_29260:; 
main_sum_req_up = 0;
goto label_29209;
}
}
else 
{
label_29209:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29458;
}
else 
{
label_29458:; 
main_diff_req_up = 0;
goto label_29407;
}
}
else 
{
label_29407:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29656;
}
else 
{
label_29656:; 
main_pres_req_up = 0;
goto label_29605;
}
}
else 
{
label_29605:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29854;
}
else 
{
label_29854:; 
main_dbl_req_up = 0;
goto label_29803;
}
}
else 
{
label_29803:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30052;
}
else 
{
label_30052:; 
main_zero_req_up = 0;
goto label_30001;
}
}
else 
{
label_30001:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_30220;
}
else 
{
label_30220:; 
main_clk_req_up = 0;
goto label_30188;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30367;
}
else 
{
label_30367:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30457;
}
else 
{
label_30457:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30547;
}
else 
{
label_30547:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30637;
}
else 
{
label_30637:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30727;
}
else 
{
label_30727:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30817;
}
else 
{
label_30817:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30907;
}
else 
{
label_30907:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_30997;
}
else 
{
label_30997:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31123;
}
else 
{
label_31123:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31393;
}
else 
{
label_31393:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31483;
}
else 
{
label_31483:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31573;
}
else 
{
label_31573:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31663;
}
else 
{
label_31663:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31753;
}
else 
{
label_31753:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31843;
}
else 
{
label_31843:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31933;
}
else 
{
label_31933:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32023;
}
else 
{
label_32023:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32149;
}
else 
{
label_32149:; 
}
goto label_21092;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_28288:; 
goto label_28266;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28863;
}
else 
{
label_28863:; 
main_in1_req_up = 0;
goto label_28814;
}
}
else 
{
label_28814:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29061;
}
else 
{
label_29061:; 
main_in2_req_up = 0;
goto label_29012;
}
}
else 
{
label_29012:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29259;
}
else 
{
label_29259:; 
main_sum_req_up = 0;
goto label_29210;
}
}
else 
{
label_29210:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29457;
}
else 
{
label_29457:; 
main_diff_req_up = 0;
goto label_29408;
}
}
else 
{
label_29408:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29655;
}
else 
{
label_29655:; 
main_pres_req_up = 0;
goto label_29606;
}
}
else 
{
label_29606:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29853;
}
else 
{
label_29853:; 
main_dbl_req_up = 0;
goto label_29804;
}
}
else 
{
label_29804:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30051;
}
else 
{
label_30051:; 
main_zero_req_up = 0;
goto label_30002;
}
}
else 
{
label_30002:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_30219;
}
else 
{
label_30219:; 
main_clk_req_up = 0;
goto label_30189;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30366;
}
else 
{
label_30366:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30456;
}
else 
{
label_30456:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30546;
}
else 
{
label_30546:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30636;
}
else 
{
label_30636:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30726;
}
else 
{
label_30726:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30816;
}
else 
{
label_30816:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30906;
}
else 
{
label_30906:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_30996;
}
else 
{
label_30996:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31122;
}
else 
{
label_31122:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31392;
}
else 
{
label_31392:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31482;
}
else 
{
label_31482:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31572;
}
else 
{
label_31572:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31662;
}
else 
{
label_31662:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31752;
}
else 
{
label_31752:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31842;
}
else 
{
label_31842:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31932;
}
else 
{
label_31932:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32022;
}
else 
{
label_32022:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32148;
}
else 
{
label_32148:; 
if (((int)S3_zero_st) == 0)
{
goto label_28051;
}
else 
{
}
goto label_21093;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_27238;
}
else 
{
label_27238:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_27278;
}
else 
{
label_27278:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_27318;
}
else 
{
label_27318:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_27358;
}
else 
{
label_27358:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_27398;
}
else 
{
label_27398:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_27438;
}
else 
{
label_27438:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_27478;
}
else 
{
label_27478:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_27518;
}
else 
{
label_27518:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_27574;
}
else 
{
label_27574:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_27694;
}
else 
{
label_27694:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_27734;
}
else 
{
label_27734:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_27774;
}
else 
{
label_27774:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_27814;
}
else 
{
label_27814:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_27854;
}
else 
{
label_27854:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_27894;
}
else 
{
label_27894:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_27934;
}
else 
{
label_27934:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_27974;
}
else 
{
label_27974:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_28030;
}
else 
{
label_28030:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_28857;
}
else 
{
label_28857:; 
main_in1_req_up = 0;
goto label_28820;
}
}
else 
{
label_28820:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_29055;
}
else 
{
label_29055:; 
main_in2_req_up = 0;
goto label_29018;
}
}
else 
{
label_29018:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_29253;
}
else 
{
label_29253:; 
main_sum_req_up = 0;
goto label_29216;
}
}
else 
{
label_29216:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_29451;
}
else 
{
label_29451:; 
main_diff_req_up = 0;
goto label_29414;
}
}
else 
{
label_29414:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_29649;
}
else 
{
label_29649:; 
main_pres_req_up = 0;
goto label_29612;
}
}
else 
{
label_29612:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_29847;
}
else 
{
label_29847:; 
main_dbl_req_up = 0;
goto label_29810;
}
}
else 
{
label_29810:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_30045;
}
else 
{
label_30045:; 
main_zero_req_up = 0;
goto label_30008;
}
}
else 
{
label_30008:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_30216;
}
else 
{
label_30216:; 
main_clk_req_up = 0;
goto label_30196;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_30360;
}
else 
{
label_30360:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_30450;
}
else 
{
label_30450:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_30540;
}
else 
{
label_30540:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_30630;
}
else 
{
label_30630:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_30720;
}
else 
{
label_30720:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_30810;
}
else 
{
label_30810:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_30900;
}
else 
{
label_30900:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_30990;
}
else 
{
label_30990:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_31116;
}
else 
{
label_31116:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_31386;
}
else 
{
label_31386:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_31476;
}
else 
{
label_31476:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_31566;
}
else 
{
label_31566:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_31656;
}
else 
{
label_31656:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_31746;
}
else 
{
label_31746:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_31836;
}
else 
{
label_31836:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_31926;
}
else 
{
label_31926:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_32016;
}
else 
{
label_32016:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_32142;
}
else 
{
label_32142:; 
}
goto label_21099;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_13359:; 
goto label_13341;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_13705;
}
else 
{
label_13705:; 
main_in1_req_up = 0;
goto label_13684;
}
}
else 
{
label_13684:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_13804;
}
else 
{
label_13804:; 
main_in2_req_up = 0;
goto label_13783;
}
}
else 
{
label_13783:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_13903;
}
else 
{
label_13903:; 
main_sum_req_up = 0;
goto label_13882;
}
}
else 
{
label_13882:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_14002;
}
else 
{
label_14002:; 
main_diff_req_up = 0;
goto label_13981;
}
}
else 
{
label_13981:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_14101;
}
else 
{
label_14101:; 
main_pres_req_up = 0;
goto label_14080;
}
}
else 
{
label_14080:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_14200;
}
else 
{
label_14200:; 
main_dbl_req_up = 0;
goto label_14179;
}
}
else 
{
label_14179:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_14299;
}
else 
{
label_14299:; 
main_zero_req_up = 0;
goto label_14278;
}
}
else 
{
label_14278:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_14398;
}
else 
{
label_14398:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_14443;
}
else 
{
label_14443:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_14488;
}
else 
{
label_14488:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_14533;
}
else 
{
label_14533:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_14578;
}
else 
{
label_14578:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_14623;
}
else 
{
label_14623:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_14668;
}
else 
{
label_14668:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_14713;
}
else 
{
label_14713:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_14776;
}
else 
{
label_14776:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_14911;
}
else 
{
label_14911:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_14956;
}
else 
{
label_14956:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_15001;
}
else 
{
label_15001:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_15046;
}
else 
{
label_15046:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_15091;
}
else 
{
label_15091:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_15136;
}
else 
{
label_15136:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_15181;
}
else 
{
label_15181:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_15226;
}
else 
{
label_15226:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_15289;
}
else 
{
label_15289:; 
if (((int)S1_addsub_st) == 0)
{
goto label_13300;
}
else 
{
}
label_15556:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
label_15592:; 
count = count + 1;
if (count == 5)
{
flag = D_z;
if (D_z == 0)
{
goto label_15718;
}
else 
{
{
__VERIFIER_error();
}
label_15718:; 
count = 0;
goto label_15634;
}
}
else 
{
label_15634:; 
main_clk_val_t = 0;
main_clk_req_up = 1;
{
int kernel_st ;
kernel_st = 0;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_21415;
}
else 
{
label_21415:; 
main_in1_req_up = 0;
goto label_21412;
}
}
else 
{
label_21412:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_21426;
}
else 
{
label_21426:; 
main_in2_req_up = 0;
goto label_21423;
}
}
else 
{
label_21423:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_21437;
}
else 
{
label_21437:; 
main_sum_req_up = 0;
goto label_21434;
}
}
else 
{
label_21434:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_21448;
}
else 
{
label_21448:; 
main_diff_req_up = 0;
goto label_21445;
}
}
else 
{
label_21445:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_21459;
}
else 
{
label_21459:; 
main_pres_req_up = 0;
goto label_21456;
}
}
else 
{
label_21456:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_21470;
}
else 
{
label_21470:; 
main_dbl_req_up = 0;
goto label_21467;
}
}
else 
{
label_21467:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_21481;
}
else 
{
label_21481:; 
main_zero_req_up = 0;
goto label_21478;
}
}
else 
{
label_21478:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_21492;
}
else 
{
label_21492:; 
main_clk_req_up = 0;
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_21596;
}
else 
{
label_21596:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_21636;
}
else 
{
label_21636:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_21676;
}
else 
{
label_21676:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_21716;
}
else 
{
label_21716:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_21756;
}
else 
{
label_21756:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_21796;
}
else 
{
label_21796:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_21836;
}
else 
{
label_21836:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_21876;
}
else 
{
label_21876:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_21932;
}
else 
{
label_21932:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_22052;
}
else 
{
label_22052:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_22092;
}
else 
{
label_22092:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_22132;
}
else 
{
label_22132:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_22172;
}
else 
{
label_22172:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_22212;
}
else 
{
label_22212:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_22252;
}
else 
{
label_22252:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_22292;
}
else 
{
label_22292:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_22332;
}
else 
{
label_22332:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_22388;
}
else 
{
label_22388:; 
label_22419:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_22941:; 
if (((int)S1_addsub_st) == 0)
{
goto label_22955;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_22955:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_22961;
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
label_22981:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_22991;
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
label_23026:; 
}
label_23094:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23238;
}
else 
{
label_23238:; 
main_in1_req_up = 0;
goto label_23169;
}
}
else 
{
label_23169:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23436;
}
else 
{
label_23436:; 
main_in2_req_up = 0;
goto label_23367;
}
}
else 
{
label_23367:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23634;
}
else 
{
label_23634:; 
main_sum_req_up = 0;
goto label_23565;
}
}
else 
{
label_23565:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23832;
}
else 
{
label_23832:; 
main_diff_req_up = 0;
goto label_23763;
}
}
else 
{
label_23763:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24030;
}
else 
{
label_24030:; 
main_pres_req_up = 0;
goto label_23961;
}
}
else 
{
label_23961:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24228;
}
else 
{
label_24228:; 
main_dbl_req_up = 0;
goto label_24159;
}
}
else 
{
label_24159:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24426;
}
else 
{
label_24426:; 
main_zero_req_up = 0;
goto label_24357;
}
}
else 
{
label_24357:; 
label_24546:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24741;
}
else 
{
label_24741:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24831;
}
else 
{
label_24831:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24921;
}
else 
{
label_24921:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_25011;
}
else 
{
label_25011:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25101;
}
else 
{
label_25101:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25191;
}
else 
{
label_25191:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25281;
}
else 
{
label_25281:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25371;
}
else 
{
label_25371:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25497;
}
else 
{
label_25497:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25767;
}
else 
{
label_25767:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25857;
}
else 
{
label_25857:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25947;
}
else 
{
label_25947:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26037;
}
else 
{
label_26037:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26127;
}
else 
{
label_26127:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26217;
}
else 
{
label_26217:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26307;
}
else 
{
label_26307:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26397;
}
else 
{
label_26397:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26523;
}
else 
{
label_26523:; 
}
goto label_21083;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_22991:; 
if (((int)S3_zero_st) == 0)
{
goto label_22981;
}
else 
{
}
label_23107:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23239;
}
else 
{
label_23239:; 
main_in1_req_up = 0;
goto label_23168;
}
}
else 
{
label_23168:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23437;
}
else 
{
label_23437:; 
main_in2_req_up = 0;
goto label_23366;
}
}
else 
{
label_23366:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23635;
}
else 
{
label_23635:; 
main_sum_req_up = 0;
goto label_23564;
}
}
else 
{
label_23564:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23833;
}
else 
{
label_23833:; 
main_diff_req_up = 0;
goto label_23762;
}
}
else 
{
label_23762:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24031;
}
else 
{
label_24031:; 
main_pres_req_up = 0;
goto label_23960;
}
}
else 
{
label_23960:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24229;
}
else 
{
label_24229:; 
main_dbl_req_up = 0;
goto label_24158;
}
}
else 
{
label_24158:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24427;
}
else 
{
label_24427:; 
main_zero_req_up = 0;
goto label_24356;
}
}
else 
{
label_24356:; 
label_24545:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24742;
}
else 
{
label_24742:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24832;
}
else 
{
label_24832:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24922;
}
else 
{
label_24922:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_25012;
}
else 
{
label_25012:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25102;
}
else 
{
label_25102:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25192;
}
else 
{
label_25192:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25282;
}
else 
{
label_25282:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25372;
}
else 
{
label_25372:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25498;
}
else 
{
label_25498:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25768;
}
else 
{
label_25768:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25858;
}
else 
{
label_25858:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25948;
}
else 
{
label_25948:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26038;
}
else 
{
label_26038:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26128;
}
else 
{
label_26128:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26218;
}
else 
{
label_26218:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26308;
}
else 
{
label_26308:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26398;
}
else 
{
label_26398:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26524;
}
else 
{
label_26524:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_26983:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_27005;
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
}
goto label_23094;
}
else 
{
label_27005:; 
goto label_26983;
}
}
else 
{
}
goto label_23107;
}
}
else 
{
}
goto label_21082;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_22961:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_22990;
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
label_23027:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_23054;
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
goto label_23026;
}
}
else 
{
label_23054:; 
goto label_23027;
}
}
else 
{
}
label_23048:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23237;
}
else 
{
label_23237:; 
main_in1_req_up = 0;
goto label_23170;
}
}
else 
{
label_23170:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23435;
}
else 
{
label_23435:; 
main_in2_req_up = 0;
goto label_23368;
}
}
else 
{
label_23368:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23633;
}
else 
{
label_23633:; 
main_sum_req_up = 0;
goto label_23566;
}
}
else 
{
label_23566:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23831;
}
else 
{
label_23831:; 
main_diff_req_up = 0;
goto label_23764;
}
}
else 
{
label_23764:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24029;
}
else 
{
label_24029:; 
main_pres_req_up = 0;
goto label_23962;
}
}
else 
{
label_23962:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24227;
}
else 
{
label_24227:; 
main_dbl_req_up = 0;
goto label_24160;
}
}
else 
{
label_24160:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24425;
}
else 
{
label_24425:; 
main_zero_req_up = 0;
goto label_24358;
}
}
else 
{
label_24358:; 
label_24547:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24740;
}
else 
{
label_24740:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24830;
}
else 
{
label_24830:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24920;
}
else 
{
label_24920:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_25010;
}
else 
{
label_25010:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25100;
}
else 
{
label_25100:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25190;
}
else 
{
label_25190:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25280;
}
else 
{
label_25280:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25370;
}
else 
{
label_25370:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25496;
}
else 
{
label_25496:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25766;
}
else 
{
label_25766:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25856;
}
else 
{
label_25856:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25946;
}
else 
{
label_25946:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26036;
}
else 
{
label_26036:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26126;
}
else 
{
label_26126:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26216;
}
else 
{
label_26216:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26306;
}
else 
{
label_26306:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26396;
}
else 
{
label_26396:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26522;
}
else 
{
label_26522:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_26835:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_26853;
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
}
goto label_23094;
}
else 
{
label_26853:; 
goto label_26835;
}
}
else 
{
}
goto label_23048;
}
}
else 
{
}
goto label_21084;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_22990:; 
goto label_22941;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23236;
}
else 
{
label_23236:; 
main_in1_req_up = 0;
goto label_23171;
}
}
else 
{
label_23171:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23434;
}
else 
{
label_23434:; 
main_in2_req_up = 0;
goto label_23369;
}
}
else 
{
label_23369:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23632;
}
else 
{
label_23632:; 
main_sum_req_up = 0;
goto label_23567;
}
}
else 
{
label_23567:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23830;
}
else 
{
label_23830:; 
main_diff_req_up = 0;
goto label_23765;
}
}
else 
{
label_23765:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24028;
}
else 
{
label_24028:; 
main_pres_req_up = 0;
goto label_23963;
}
}
else 
{
label_23963:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24226;
}
else 
{
label_24226:; 
main_dbl_req_up = 0;
goto label_24161;
}
}
else 
{
label_24161:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24424;
}
else 
{
label_24424:; 
main_zero_req_up = 0;
goto label_24359;
}
}
else 
{
label_24359:; 
label_24548:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24739;
}
else 
{
label_24739:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24829;
}
else 
{
label_24829:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24919;
}
else 
{
label_24919:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_25009;
}
else 
{
label_25009:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25099;
}
else 
{
label_25099:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25189;
}
else 
{
label_25189:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25279;
}
else 
{
label_25279:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25369;
}
else 
{
label_25369:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25495;
}
else 
{
label_25495:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25765;
}
else 
{
label_25765:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25855;
}
else 
{
label_25855:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25945;
}
else 
{
label_25945:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26035;
}
else 
{
label_26035:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26125;
}
else 
{
label_26125:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26215;
}
else 
{
label_26215:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26305;
}
else 
{
label_26305:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26395;
}
else 
{
label_26395:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26521;
}
else 
{
label_26521:; 
if (((int)S1_addsub_st) == 0)
{
goto label_26736;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_26736:; 
goto label_22419;
}
else 
{
}
goto label_21085;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_21600;
}
else 
{
label_21600:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_21640;
}
else 
{
label_21640:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_21680;
}
else 
{
label_21680:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_21720;
}
else 
{
label_21720:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_21760;
}
else 
{
label_21760:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_21800;
}
else 
{
label_21800:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_21840;
}
else 
{
label_21840:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_21880;
}
else 
{
label_21880:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_21936;
}
else 
{
label_21936:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_22056;
}
else 
{
label_22056:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_22096;
}
else 
{
label_22096:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_22136;
}
else 
{
label_22136:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_22176;
}
else 
{
label_22176:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_22216;
}
else 
{
label_22216:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_22256;
}
else 
{
label_22256:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_22296;
}
else 
{
label_22296:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_22336;
}
else 
{
label_22336:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_22392;
}
else 
{
label_22392:; 
label_22415:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_22560:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_22578;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23227;
}
else 
{
label_23227:; 
main_in1_req_up = 0;
goto label_23180;
}
}
else 
{
label_23180:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23425;
}
else 
{
label_23425:; 
main_in2_req_up = 0;
goto label_23378;
}
}
else 
{
label_23378:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23623;
}
else 
{
label_23623:; 
main_sum_req_up = 0;
goto label_23576;
}
}
else 
{
label_23576:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23821;
}
else 
{
label_23821:; 
main_diff_req_up = 0;
goto label_23774;
}
}
else 
{
label_23774:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24019;
}
else 
{
label_24019:; 
main_pres_req_up = 0;
goto label_23972;
}
}
else 
{
label_23972:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24217;
}
else 
{
label_24217:; 
main_dbl_req_up = 0;
goto label_24170;
}
}
else 
{
label_24170:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24415;
}
else 
{
label_24415:; 
main_zero_req_up = 0;
goto label_24368;
}
}
else 
{
label_24368:; 
label_24557:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24730;
}
else 
{
label_24730:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24820;
}
else 
{
label_24820:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24910;
}
else 
{
label_24910:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_25000;
}
else 
{
label_25000:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25090;
}
else 
{
label_25090:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25180;
}
else 
{
label_25180:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25270;
}
else 
{
label_25270:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25360;
}
else 
{
label_25360:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25486;
}
else 
{
label_25486:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25756;
}
else 
{
label_25756:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25846;
}
else 
{
label_25846:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25936;
}
else 
{
label_25936:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26026;
}
else 
{
label_26026:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26116;
}
else 
{
label_26116:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26206;
}
else 
{
label_26206:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26296;
}
else 
{
label_26296:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26386;
}
else 
{
label_26386:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26512;
}
else 
{
label_26512:; 
}
goto label_21094;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_22578:; 
goto label_22560;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23226;
}
else 
{
label_23226:; 
main_in1_req_up = 0;
goto label_23181;
}
}
else 
{
label_23181:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23424;
}
else 
{
label_23424:; 
main_in2_req_up = 0;
goto label_23379;
}
}
else 
{
label_23379:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23622;
}
else 
{
label_23622:; 
main_sum_req_up = 0;
goto label_23577;
}
}
else 
{
label_23577:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23820;
}
else 
{
label_23820:; 
main_diff_req_up = 0;
goto label_23775;
}
}
else 
{
label_23775:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24018;
}
else 
{
label_24018:; 
main_pres_req_up = 0;
goto label_23973;
}
}
else 
{
label_23973:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24216;
}
else 
{
label_24216:; 
main_dbl_req_up = 0;
goto label_24171;
}
}
else 
{
label_24171:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24414;
}
else 
{
label_24414:; 
main_zero_req_up = 0;
goto label_24369;
}
}
else 
{
label_24369:; 
label_24558:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24729;
}
else 
{
label_24729:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24819;
}
else 
{
label_24819:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24909;
}
else 
{
label_24909:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_24999;
}
else 
{
label_24999:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25089;
}
else 
{
label_25089:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25179;
}
else 
{
label_25179:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25269;
}
else 
{
label_25269:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25359;
}
else 
{
label_25359:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25485;
}
else 
{
label_25485:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25755;
}
else 
{
label_25755:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25845;
}
else 
{
label_25845:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25935;
}
else 
{
label_25935:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26025;
}
else 
{
label_26025:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26115;
}
else 
{
label_26115:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26205;
}
else 
{
label_26205:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26295;
}
else 
{
label_26295:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26385;
}
else 
{
label_26385:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26511;
}
else 
{
label_26511:; 
if (((int)S1_addsub_st) == 0)
{
goto label_22415;
}
else 
{
}
goto label_21095;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_21598;
}
else 
{
label_21598:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_21638;
}
else 
{
label_21638:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_21678;
}
else 
{
label_21678:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_21718;
}
else 
{
label_21718:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_21758;
}
else 
{
label_21758:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_21798;
}
else 
{
label_21798:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_21838;
}
else 
{
label_21838:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_21878;
}
else 
{
label_21878:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_21934;
}
else 
{
label_21934:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_22054;
}
else 
{
label_22054:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_22094;
}
else 
{
label_22094:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_22134;
}
else 
{
label_22134:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_22174;
}
else 
{
label_22174:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_22214;
}
else 
{
label_22214:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_22254;
}
else 
{
label_22254:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_22294;
}
else 
{
label_22294:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_22334;
}
else 
{
label_22334:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_22390;
}
else 
{
label_22390:; 
label_22417:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_22696:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_22718;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23231;
}
else 
{
label_23231:; 
main_in1_req_up = 0;
goto label_23176;
}
}
else 
{
label_23176:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23429;
}
else 
{
label_23429:; 
main_in2_req_up = 0;
goto label_23374;
}
}
else 
{
label_23374:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23627;
}
else 
{
label_23627:; 
main_sum_req_up = 0;
goto label_23572;
}
}
else 
{
label_23572:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23825;
}
else 
{
label_23825:; 
main_diff_req_up = 0;
goto label_23770;
}
}
else 
{
label_23770:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24023;
}
else 
{
label_24023:; 
main_pres_req_up = 0;
goto label_23968;
}
}
else 
{
label_23968:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24221;
}
else 
{
label_24221:; 
main_dbl_req_up = 0;
goto label_24166;
}
}
else 
{
label_24166:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24419;
}
else 
{
label_24419:; 
main_zero_req_up = 0;
goto label_24364;
}
}
else 
{
label_24364:; 
label_24553:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24734;
}
else 
{
label_24734:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24824;
}
else 
{
label_24824:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24914;
}
else 
{
label_24914:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_25004;
}
else 
{
label_25004:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25094;
}
else 
{
label_25094:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25184;
}
else 
{
label_25184:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25274;
}
else 
{
label_25274:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25364;
}
else 
{
label_25364:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25490;
}
else 
{
label_25490:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25760;
}
else 
{
label_25760:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25850;
}
else 
{
label_25850:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25940;
}
else 
{
label_25940:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26030;
}
else 
{
label_26030:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26120;
}
else 
{
label_26120:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26210;
}
else 
{
label_26210:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26300;
}
else 
{
label_26300:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26390;
}
else 
{
label_26390:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26516;
}
else 
{
label_26516:; 
}
goto label_21090;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_22718:; 
goto label_22696;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23230;
}
else 
{
label_23230:; 
main_in1_req_up = 0;
goto label_23177;
}
}
else 
{
label_23177:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23428;
}
else 
{
label_23428:; 
main_in2_req_up = 0;
goto label_23375;
}
}
else 
{
label_23375:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23626;
}
else 
{
label_23626:; 
main_sum_req_up = 0;
goto label_23573;
}
}
else 
{
label_23573:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23824;
}
else 
{
label_23824:; 
main_diff_req_up = 0;
goto label_23771;
}
}
else 
{
label_23771:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24022;
}
else 
{
label_24022:; 
main_pres_req_up = 0;
goto label_23969;
}
}
else 
{
label_23969:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24220;
}
else 
{
label_24220:; 
main_dbl_req_up = 0;
goto label_24167;
}
}
else 
{
label_24167:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24418;
}
else 
{
label_24418:; 
main_zero_req_up = 0;
goto label_24365;
}
}
else 
{
label_24365:; 
label_24554:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24733;
}
else 
{
label_24733:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24823;
}
else 
{
label_24823:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24913;
}
else 
{
label_24913:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_25003;
}
else 
{
label_25003:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25093;
}
else 
{
label_25093:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25183;
}
else 
{
label_25183:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25273;
}
else 
{
label_25273:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25363;
}
else 
{
label_25363:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25489;
}
else 
{
label_25489:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25759;
}
else 
{
label_25759:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25849;
}
else 
{
label_25849:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25939;
}
else 
{
label_25939:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26029;
}
else 
{
label_26029:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26119;
}
else 
{
label_26119:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26209;
}
else 
{
label_26209:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26299;
}
else 
{
label_26299:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26389;
}
else 
{
label_26389:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26515;
}
else 
{
label_26515:; 
if (((int)S3_zero_st) == 0)
{
goto label_22417;
}
else 
{
}
goto label_21091;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_21602;
}
else 
{
label_21602:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_21642;
}
else 
{
label_21642:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_21682;
}
else 
{
label_21682:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_21722;
}
else 
{
label_21722:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_21762;
}
else 
{
label_21762:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_21802;
}
else 
{
label_21802:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_21842;
}
else 
{
label_21842:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_21882;
}
else 
{
label_21882:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_21938;
}
else 
{
label_21938:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_22058;
}
else 
{
label_22058:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_22098;
}
else 
{
label_22098:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_22138;
}
else 
{
label_22138:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_22178;
}
else 
{
label_22178:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_22218;
}
else 
{
label_22218:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_22258;
}
else 
{
label_22258:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_22298;
}
else 
{
label_22298:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_22338;
}
else 
{
label_22338:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_22394;
}
else 
{
label_22394:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23223;
}
else 
{
label_23223:; 
main_in1_req_up = 0;
goto label_23184;
}
}
else 
{
label_23184:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23421;
}
else 
{
label_23421:; 
main_in2_req_up = 0;
goto label_23382;
}
}
else 
{
label_23382:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23619;
}
else 
{
label_23619:; 
main_sum_req_up = 0;
goto label_23580;
}
}
else 
{
label_23580:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23817;
}
else 
{
label_23817:; 
main_diff_req_up = 0;
goto label_23778;
}
}
else 
{
label_23778:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24015;
}
else 
{
label_24015:; 
main_pres_req_up = 0;
goto label_23976;
}
}
else 
{
label_23976:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24213;
}
else 
{
label_24213:; 
main_dbl_req_up = 0;
goto label_24174;
}
}
else 
{
label_24174:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24411;
}
else 
{
label_24411:; 
main_zero_req_up = 0;
goto label_24372;
}
}
else 
{
label_24372:; 
label_24561:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24726;
}
else 
{
label_24726:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24816;
}
else 
{
label_24816:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24906;
}
else 
{
label_24906:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_24996;
}
else 
{
label_24996:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25086;
}
else 
{
label_25086:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25176;
}
else 
{
label_25176:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25266;
}
else 
{
label_25266:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25356;
}
else 
{
label_25356:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25482;
}
else 
{
label_25482:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25752;
}
else 
{
label_25752:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25842;
}
else 
{
label_25842:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25932;
}
else 
{
label_25932:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26022;
}
else 
{
label_26022:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26112;
}
else 
{
label_26112:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26202;
}
else 
{
label_26202:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26292;
}
else 
{
label_26292:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26382;
}
else 
{
label_26382:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26508;
}
else 
{
label_26508:; 
}
goto label_21098;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_21597;
}
else 
{
label_21597:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_21637;
}
else 
{
label_21637:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_21677;
}
else 
{
label_21677:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_21717;
}
else 
{
label_21717:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_21757;
}
else 
{
label_21757:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_21797;
}
else 
{
label_21797:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_21837;
}
else 
{
label_21837:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_21877;
}
else 
{
label_21877:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_21933;
}
else 
{
label_21933:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_22053;
}
else 
{
label_22053:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_22093;
}
else 
{
label_22093:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_22133;
}
else 
{
label_22133:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_22173;
}
else 
{
label_22173:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_22213;
}
else 
{
label_22213:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_22253;
}
else 
{
label_22253:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_22293;
}
else 
{
label_22293:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_22333;
}
else 
{
label_22333:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_22389;
}
else 
{
label_22389:; 
label_22418:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_22761:; 
if (((int)S1_addsub_st) == 0)
{
goto label_22775;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_22775:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_22781;
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
label_22801:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_22811;
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
label_22846:; 
}
label_22914:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23234;
}
else 
{
label_23234:; 
main_in1_req_up = 0;
goto label_23173;
}
}
else 
{
label_23173:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23432;
}
else 
{
label_23432:; 
main_in2_req_up = 0;
goto label_23371;
}
}
else 
{
label_23371:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23630;
}
else 
{
label_23630:; 
main_sum_req_up = 0;
goto label_23569;
}
}
else 
{
label_23569:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23828;
}
else 
{
label_23828:; 
main_diff_req_up = 0;
goto label_23767;
}
}
else 
{
label_23767:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24026;
}
else 
{
label_24026:; 
main_pres_req_up = 0;
goto label_23965;
}
}
else 
{
label_23965:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24224;
}
else 
{
label_24224:; 
main_dbl_req_up = 0;
goto label_24163;
}
}
else 
{
label_24163:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24422;
}
else 
{
label_24422:; 
main_zero_req_up = 0;
goto label_24361;
}
}
else 
{
label_24361:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_24588;
}
else 
{
label_24588:; 
main_clk_req_up = 0;
goto label_24546;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24737;
}
else 
{
label_24737:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24827;
}
else 
{
label_24827:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24917;
}
else 
{
label_24917:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_25007;
}
else 
{
label_25007:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25097;
}
else 
{
label_25097:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25187;
}
else 
{
label_25187:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25277;
}
else 
{
label_25277:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25367;
}
else 
{
label_25367:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25493;
}
else 
{
label_25493:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25763;
}
else 
{
label_25763:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25853;
}
else 
{
label_25853:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25943;
}
else 
{
label_25943:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26033;
}
else 
{
label_26033:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26123;
}
else 
{
label_26123:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26213;
}
else 
{
label_26213:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26303;
}
else 
{
label_26303:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26393;
}
else 
{
label_26393:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26519;
}
else 
{
label_26519:; 
}
goto label_21087;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_22811:; 
if (((int)S3_zero_st) == 0)
{
goto label_22801;
}
else 
{
}
label_22927:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23235;
}
else 
{
label_23235:; 
main_in1_req_up = 0;
goto label_23172;
}
}
else 
{
label_23172:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23433;
}
else 
{
label_23433:; 
main_in2_req_up = 0;
goto label_23370;
}
}
else 
{
label_23370:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23631;
}
else 
{
label_23631:; 
main_sum_req_up = 0;
goto label_23568;
}
}
else 
{
label_23568:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23829;
}
else 
{
label_23829:; 
main_diff_req_up = 0;
goto label_23766;
}
}
else 
{
label_23766:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24027;
}
else 
{
label_24027:; 
main_pres_req_up = 0;
goto label_23964;
}
}
else 
{
label_23964:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24225;
}
else 
{
label_24225:; 
main_dbl_req_up = 0;
goto label_24162;
}
}
else 
{
label_24162:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24423;
}
else 
{
label_24423:; 
main_zero_req_up = 0;
goto label_24360;
}
}
else 
{
label_24360:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_24589;
}
else 
{
label_24589:; 
main_clk_req_up = 0;
goto label_24545;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24738;
}
else 
{
label_24738:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24828;
}
else 
{
label_24828:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24918;
}
else 
{
label_24918:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_25008;
}
else 
{
label_25008:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25098;
}
else 
{
label_25098:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25188;
}
else 
{
label_25188:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25278;
}
else 
{
label_25278:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25368;
}
else 
{
label_25368:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25494;
}
else 
{
label_25494:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25764;
}
else 
{
label_25764:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25854;
}
else 
{
label_25854:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25944;
}
else 
{
label_25944:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26034;
}
else 
{
label_26034:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26124;
}
else 
{
label_26124:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26214;
}
else 
{
label_26214:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26304;
}
else 
{
label_26304:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26394;
}
else 
{
label_26394:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26520;
}
else 
{
label_26520:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_26913:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_26935;
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
}
goto label_22914;
}
else 
{
label_26935:; 
goto label_26913;
}
}
else 
{
}
goto label_22927;
}
}
else 
{
}
goto label_21086;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_22781:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_22810;
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
label_22847:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_22874;
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
goto label_22846;
}
}
else 
{
label_22874:; 
goto label_22847;
}
}
else 
{
}
label_22868:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23233;
}
else 
{
label_23233:; 
main_in1_req_up = 0;
goto label_23174;
}
}
else 
{
label_23174:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23431;
}
else 
{
label_23431:; 
main_in2_req_up = 0;
goto label_23372;
}
}
else 
{
label_23372:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23629;
}
else 
{
label_23629:; 
main_sum_req_up = 0;
goto label_23570;
}
}
else 
{
label_23570:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23827;
}
else 
{
label_23827:; 
main_diff_req_up = 0;
goto label_23768;
}
}
else 
{
label_23768:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24025;
}
else 
{
label_24025:; 
main_pres_req_up = 0;
goto label_23966;
}
}
else 
{
label_23966:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24223;
}
else 
{
label_24223:; 
main_dbl_req_up = 0;
goto label_24164;
}
}
else 
{
label_24164:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24421;
}
else 
{
label_24421:; 
main_zero_req_up = 0;
goto label_24362;
}
}
else 
{
label_24362:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_24587;
}
else 
{
label_24587:; 
main_clk_req_up = 0;
goto label_24547;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24736;
}
else 
{
label_24736:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24826;
}
else 
{
label_24826:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24916;
}
else 
{
label_24916:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_25006;
}
else 
{
label_25006:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25096;
}
else 
{
label_25096:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25186;
}
else 
{
label_25186:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25276;
}
else 
{
label_25276:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25366;
}
else 
{
label_25366:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25492;
}
else 
{
label_25492:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25762;
}
else 
{
label_25762:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25852;
}
else 
{
label_25852:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25942;
}
else 
{
label_25942:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26032;
}
else 
{
label_26032:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26122;
}
else 
{
label_26122:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26212;
}
else 
{
label_26212:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26302;
}
else 
{
label_26302:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26392;
}
else 
{
label_26392:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26518;
}
else 
{
label_26518:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_26760:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_26778;
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
}
goto label_22914;
}
else 
{
label_26778:; 
goto label_26760;
}
}
else 
{
}
goto label_22868;
}
}
else 
{
}
goto label_21088;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_22810:; 
goto label_22761;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23232;
}
else 
{
label_23232:; 
main_in1_req_up = 0;
goto label_23175;
}
}
else 
{
label_23175:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23430;
}
else 
{
label_23430:; 
main_in2_req_up = 0;
goto label_23373;
}
}
else 
{
label_23373:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23628;
}
else 
{
label_23628:; 
main_sum_req_up = 0;
goto label_23571;
}
}
else 
{
label_23571:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23826;
}
else 
{
label_23826:; 
main_diff_req_up = 0;
goto label_23769;
}
}
else 
{
label_23769:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24024;
}
else 
{
label_24024:; 
main_pres_req_up = 0;
goto label_23967;
}
}
else 
{
label_23967:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24222;
}
else 
{
label_24222:; 
main_dbl_req_up = 0;
goto label_24165;
}
}
else 
{
label_24165:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24420;
}
else 
{
label_24420:; 
main_zero_req_up = 0;
goto label_24363;
}
}
else 
{
label_24363:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_24586;
}
else 
{
label_24586:; 
main_clk_req_up = 0;
goto label_24548;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24735;
}
else 
{
label_24735:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24825;
}
else 
{
label_24825:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24915;
}
else 
{
label_24915:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_25005;
}
else 
{
label_25005:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25095;
}
else 
{
label_25095:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25185;
}
else 
{
label_25185:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25275;
}
else 
{
label_25275:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25365;
}
else 
{
label_25365:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25491;
}
else 
{
label_25491:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25761;
}
else 
{
label_25761:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25851;
}
else 
{
label_25851:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25941;
}
else 
{
label_25941:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26031;
}
else 
{
label_26031:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26121;
}
else 
{
label_26121:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26211;
}
else 
{
label_26211:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26301;
}
else 
{
label_26301:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26391;
}
else 
{
label_26391:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26517;
}
else 
{
label_26517:; 
if (((int)S1_addsub_st) == 0)
{
goto label_26738;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_26738:; 
goto label_22418;
}
else 
{
}
goto label_21089;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_21601;
}
else 
{
label_21601:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_21641;
}
else 
{
label_21641:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_21681;
}
else 
{
label_21681:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_21721;
}
else 
{
label_21721:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_21761;
}
else 
{
label_21761:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_21801;
}
else 
{
label_21801:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_21841;
}
else 
{
label_21841:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_21881;
}
else 
{
label_21881:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_21937;
}
else 
{
label_21937:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_22057;
}
else 
{
label_22057:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_22097;
}
else 
{
label_22097:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_22137;
}
else 
{
label_22137:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_22177;
}
else 
{
label_22177:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_22217;
}
else 
{
label_22217:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_22257;
}
else 
{
label_22257:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_22297;
}
else 
{
label_22297:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_22337;
}
else 
{
label_22337:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_22393;
}
else 
{
label_22393:; 
label_22414:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_22489:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_22507;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23225;
}
else 
{
label_23225:; 
main_in1_req_up = 0;
goto label_23182;
}
}
else 
{
label_23182:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23423;
}
else 
{
label_23423:; 
main_in2_req_up = 0;
goto label_23380;
}
}
else 
{
label_23380:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23621;
}
else 
{
label_23621:; 
main_sum_req_up = 0;
goto label_23578;
}
}
else 
{
label_23578:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23819;
}
else 
{
label_23819:; 
main_diff_req_up = 0;
goto label_23776;
}
}
else 
{
label_23776:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24017;
}
else 
{
label_24017:; 
main_pres_req_up = 0;
goto label_23974;
}
}
else 
{
label_23974:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24215;
}
else 
{
label_24215:; 
main_dbl_req_up = 0;
goto label_24172;
}
}
else 
{
label_24172:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24413;
}
else 
{
label_24413:; 
main_zero_req_up = 0;
goto label_24370;
}
}
else 
{
label_24370:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_24583;
}
else 
{
label_24583:; 
main_clk_req_up = 0;
goto label_24557;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24728;
}
else 
{
label_24728:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24818;
}
else 
{
label_24818:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24908;
}
else 
{
label_24908:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_24998;
}
else 
{
label_24998:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25088;
}
else 
{
label_25088:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25178;
}
else 
{
label_25178:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25268;
}
else 
{
label_25268:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25358;
}
else 
{
label_25358:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25484;
}
else 
{
label_25484:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25754;
}
else 
{
label_25754:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25844;
}
else 
{
label_25844:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25934;
}
else 
{
label_25934:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26024;
}
else 
{
label_26024:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26114;
}
else 
{
label_26114:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26204;
}
else 
{
label_26204:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26294;
}
else 
{
label_26294:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26384;
}
else 
{
label_26384:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26510;
}
else 
{
label_26510:; 
}
goto label_21096;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_22507:; 
goto label_22489;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23224;
}
else 
{
label_23224:; 
main_in1_req_up = 0;
goto label_23183;
}
}
else 
{
label_23183:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23422;
}
else 
{
label_23422:; 
main_in2_req_up = 0;
goto label_23381;
}
}
else 
{
label_23381:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23620;
}
else 
{
label_23620:; 
main_sum_req_up = 0;
goto label_23579;
}
}
else 
{
label_23579:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23818;
}
else 
{
label_23818:; 
main_diff_req_up = 0;
goto label_23777;
}
}
else 
{
label_23777:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24016;
}
else 
{
label_24016:; 
main_pres_req_up = 0;
goto label_23975;
}
}
else 
{
label_23975:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24214;
}
else 
{
label_24214:; 
main_dbl_req_up = 0;
goto label_24173;
}
}
else 
{
label_24173:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24412;
}
else 
{
label_24412:; 
main_zero_req_up = 0;
goto label_24371;
}
}
else 
{
label_24371:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_24582;
}
else 
{
label_24582:; 
main_clk_req_up = 0;
goto label_24558;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24727;
}
else 
{
label_24727:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24817;
}
else 
{
label_24817:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24907;
}
else 
{
label_24907:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_24997;
}
else 
{
label_24997:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25087;
}
else 
{
label_25087:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25177;
}
else 
{
label_25177:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25267;
}
else 
{
label_25267:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25357;
}
else 
{
label_25357:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25483;
}
else 
{
label_25483:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25753;
}
else 
{
label_25753:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25843;
}
else 
{
label_25843:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25933;
}
else 
{
label_25933:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26023;
}
else 
{
label_26023:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26113;
}
else 
{
label_26113:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26203;
}
else 
{
label_26203:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26293;
}
else 
{
label_26293:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26383;
}
else 
{
label_26383:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26509;
}
else 
{
label_26509:; 
if (((int)S1_addsub_st) == 0)
{
goto label_22414;
}
else 
{
}
goto label_21097;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_21599;
}
else 
{
label_21599:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_21639;
}
else 
{
label_21639:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_21679;
}
else 
{
label_21679:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_21719;
}
else 
{
label_21719:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_21759;
}
else 
{
label_21759:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_21799;
}
else 
{
label_21799:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_21839;
}
else 
{
label_21839:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_21879;
}
else 
{
label_21879:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_21935;
}
else 
{
label_21935:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_22055;
}
else 
{
label_22055:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_22095;
}
else 
{
label_22095:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_22135;
}
else 
{
label_22135:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_22175;
}
else 
{
label_22175:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_22215;
}
else 
{
label_22215:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_22255;
}
else 
{
label_22255:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_22295;
}
else 
{
label_22295:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_22335;
}
else 
{
label_22335:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_22391;
}
else 
{
label_22391:; 
label_22416:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_22631:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_22653;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23229;
}
else 
{
label_23229:; 
main_in1_req_up = 0;
goto label_23178;
}
}
else 
{
label_23178:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23427;
}
else 
{
label_23427:; 
main_in2_req_up = 0;
goto label_23376;
}
}
else 
{
label_23376:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23625;
}
else 
{
label_23625:; 
main_sum_req_up = 0;
goto label_23574;
}
}
else 
{
label_23574:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23823;
}
else 
{
label_23823:; 
main_diff_req_up = 0;
goto label_23772;
}
}
else 
{
label_23772:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24021;
}
else 
{
label_24021:; 
main_pres_req_up = 0;
goto label_23970;
}
}
else 
{
label_23970:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24219;
}
else 
{
label_24219:; 
main_dbl_req_up = 0;
goto label_24168;
}
}
else 
{
label_24168:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24417;
}
else 
{
label_24417:; 
main_zero_req_up = 0;
goto label_24366;
}
}
else 
{
label_24366:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_24585;
}
else 
{
label_24585:; 
main_clk_req_up = 0;
goto label_24553;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24732;
}
else 
{
label_24732:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24822;
}
else 
{
label_24822:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24912;
}
else 
{
label_24912:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_25002;
}
else 
{
label_25002:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25092;
}
else 
{
label_25092:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25182;
}
else 
{
label_25182:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25272;
}
else 
{
label_25272:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25362;
}
else 
{
label_25362:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25488;
}
else 
{
label_25488:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25758;
}
else 
{
label_25758:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25848;
}
else 
{
label_25848:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25938;
}
else 
{
label_25938:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26028;
}
else 
{
label_26028:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26118;
}
else 
{
label_26118:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26208;
}
else 
{
label_26208:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26298;
}
else 
{
label_26298:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26388;
}
else 
{
label_26388:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26514;
}
else 
{
label_26514:; 
}
goto label_21092;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_22653:; 
goto label_22631;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23228;
}
else 
{
label_23228:; 
main_in1_req_up = 0;
goto label_23179;
}
}
else 
{
label_23179:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23426;
}
else 
{
label_23426:; 
main_in2_req_up = 0;
goto label_23377;
}
}
else 
{
label_23377:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23624;
}
else 
{
label_23624:; 
main_sum_req_up = 0;
goto label_23575;
}
}
else 
{
label_23575:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23822;
}
else 
{
label_23822:; 
main_diff_req_up = 0;
goto label_23773;
}
}
else 
{
label_23773:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24020;
}
else 
{
label_24020:; 
main_pres_req_up = 0;
goto label_23971;
}
}
else 
{
label_23971:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24218;
}
else 
{
label_24218:; 
main_dbl_req_up = 0;
goto label_24169;
}
}
else 
{
label_24169:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24416;
}
else 
{
label_24416:; 
main_zero_req_up = 0;
goto label_24367;
}
}
else 
{
label_24367:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_24584;
}
else 
{
label_24584:; 
main_clk_req_up = 0;
goto label_24554;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24731;
}
else 
{
label_24731:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24821;
}
else 
{
label_24821:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24911;
}
else 
{
label_24911:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_25001;
}
else 
{
label_25001:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25091;
}
else 
{
label_25091:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25181;
}
else 
{
label_25181:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25271;
}
else 
{
label_25271:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25361;
}
else 
{
label_25361:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25487;
}
else 
{
label_25487:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25757;
}
else 
{
label_25757:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25847;
}
else 
{
label_25847:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25937;
}
else 
{
label_25937:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26027;
}
else 
{
label_26027:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26117;
}
else 
{
label_26117:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26207;
}
else 
{
label_26207:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26297;
}
else 
{
label_26297:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26387;
}
else 
{
label_26387:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26513;
}
else 
{
label_26513:; 
if (((int)S3_zero_st) == 0)
{
goto label_22416;
}
else 
{
}
goto label_21093;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_21603;
}
else 
{
label_21603:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_21643;
}
else 
{
label_21643:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_21683;
}
else 
{
label_21683:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_21723;
}
else 
{
label_21723:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_21763;
}
else 
{
label_21763:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_21803;
}
else 
{
label_21803:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_21843;
}
else 
{
label_21843:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_21883;
}
else 
{
label_21883:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_21939;
}
else 
{
label_21939:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_22059;
}
else 
{
label_22059:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_22099;
}
else 
{
label_22099:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_22139;
}
else 
{
label_22139:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_22179;
}
else 
{
label_22179:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_22219;
}
else 
{
label_22219:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_22259;
}
else 
{
label_22259:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_22299;
}
else 
{
label_22299:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_22339;
}
else 
{
label_22339:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_22395;
}
else 
{
label_22395:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_23222;
}
else 
{
label_23222:; 
main_in1_req_up = 0;
goto label_23185;
}
}
else 
{
label_23185:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_23420;
}
else 
{
label_23420:; 
main_in2_req_up = 0;
goto label_23383;
}
}
else 
{
label_23383:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_23618;
}
else 
{
label_23618:; 
main_sum_req_up = 0;
goto label_23581;
}
}
else 
{
label_23581:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_23816;
}
else 
{
label_23816:; 
main_diff_req_up = 0;
goto label_23779;
}
}
else 
{
label_23779:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_24014;
}
else 
{
label_24014:; 
main_pres_req_up = 0;
goto label_23977;
}
}
else 
{
label_23977:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_24212;
}
else 
{
label_24212:; 
main_dbl_req_up = 0;
goto label_24175;
}
}
else 
{
label_24175:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_24410;
}
else 
{
label_24410:; 
main_zero_req_up = 0;
goto label_24373;
}
}
else 
{
label_24373:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_24581;
}
else 
{
label_24581:; 
main_clk_req_up = 0;
goto label_24561;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_24725;
}
else 
{
label_24725:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_24815;
}
else 
{
label_24815:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_24905;
}
else 
{
label_24905:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_24995;
}
else 
{
label_24995:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_25085;
}
else 
{
label_25085:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_25175;
}
else 
{
label_25175:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_25265;
}
else 
{
label_25265:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_25355;
}
else 
{
label_25355:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_25481;
}
else 
{
label_25481:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_25751;
}
else 
{
label_25751:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_25841;
}
else 
{
label_25841:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_25931;
}
else 
{
label_25931:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_26021;
}
else 
{
label_26021:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_26111;
}
else 
{
label_26111:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_26201;
}
else 
{
label_26201:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_26291;
}
else 
{
label_26291:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_26381;
}
else 
{
label_26381:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_26507;
}
else 
{
label_26507:; 
}
goto label_21099;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_12892;
}
else 
{
label_12892:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_12912;
}
else 
{
label_12912:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_12932;
}
else 
{
label_12932:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_12952;
}
else 
{
label_12952:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_12972;
}
else 
{
label_12972:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_12992;
}
else 
{
label_12992:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_13012;
}
else 
{
label_13012:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_13032;
}
else 
{
label_13032:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_13060;
}
else 
{
label_13060:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_13120;
}
else 
{
label_13120:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_13140;
}
else 
{
label_13140:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_13160;
}
else 
{
label_13160:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_13180;
}
else 
{
label_13180:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_13200;
}
else 
{
label_13200:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_13220;
}
else 
{
label_13220:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_13240;
}
else 
{
label_13240:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_13260;
}
else 
{
label_13260:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_13288;
}
else 
{
label_13288:; 
label_13301:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_13412:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_13434;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_13708;
}
else 
{
label_13708:; 
main_in1_req_up = 0;
goto label_13681;
}
}
else 
{
label_13681:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_13807;
}
else 
{
label_13807:; 
main_in2_req_up = 0;
goto label_13780;
}
}
else 
{
label_13780:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_13906;
}
else 
{
label_13906:; 
main_sum_req_up = 0;
goto label_13879;
}
}
else 
{
label_13879:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_14005;
}
else 
{
label_14005:; 
main_diff_req_up = 0;
goto label_13978;
}
}
else 
{
label_13978:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_14104;
}
else 
{
label_14104:; 
main_pres_req_up = 0;
goto label_14077;
}
}
else 
{
label_14077:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_14203;
}
else 
{
label_14203:; 
main_dbl_req_up = 0;
goto label_14176;
}
}
else 
{
label_14176:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_14302;
}
else 
{
label_14302:; 
main_zero_req_up = 0;
goto label_14275;
}
}
else 
{
label_14275:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_14401;
}
else 
{
label_14401:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_14446;
}
else 
{
label_14446:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_14491;
}
else 
{
label_14491:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_14536;
}
else 
{
label_14536:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_14581;
}
else 
{
label_14581:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_14626;
}
else 
{
label_14626:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_14671;
}
else 
{
label_14671:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_14716;
}
else 
{
label_14716:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_14779;
}
else 
{
label_14779:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_14914;
}
else 
{
label_14914:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_14959;
}
else 
{
label_14959:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_15004;
}
else 
{
label_15004:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_15049;
}
else 
{
label_15049:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_15094;
}
else 
{
label_15094:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_15139;
}
else 
{
label_15139:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_15184;
}
else 
{
label_15184:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_15229;
}
else 
{
label_15229:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_15292;
}
else 
{
label_15292:; 
}
label_15559:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
label_15595:; 
count = count + 1;
if (count == 5)
{
flag = D_z;
if (D_z == 0)
{
goto label_15721;
}
else 
{
{
__VERIFIER_error();
}
label_15721:; 
count = 0;
goto label_15631;
}
}
else 
{
label_15631:; 
main_clk_val_t = 0;
main_clk_req_up = 1;
{
int kernel_st ;
kernel_st = 0;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_38320;
}
else 
{
label_38320:; 
main_in1_req_up = 0;
goto label_38317;
}
}
else 
{
label_38317:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_38331;
}
else 
{
label_38331:; 
main_in2_req_up = 0;
goto label_38328;
}
}
else 
{
label_38328:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_38342;
}
else 
{
label_38342:; 
main_sum_req_up = 0;
goto label_38339;
}
}
else 
{
label_38339:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_38353;
}
else 
{
label_38353:; 
main_diff_req_up = 0;
goto label_38350;
}
}
else 
{
label_38350:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_38364;
}
else 
{
label_38364:; 
main_pres_req_up = 0;
goto label_38361;
}
}
else 
{
label_38361:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_38375;
}
else 
{
label_38375:; 
main_dbl_req_up = 0;
goto label_38372;
}
}
else 
{
label_38372:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_38386;
}
else 
{
label_38386:; 
main_zero_req_up = 0;
goto label_38383;
}
}
else 
{
label_38383:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_38397;
}
else 
{
label_38397:; 
main_clk_req_up = 0;
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_38501;
}
else 
{
label_38501:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_38541;
}
else 
{
label_38541:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_38581;
}
else 
{
label_38581:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_38621;
}
else 
{
label_38621:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_38661;
}
else 
{
label_38661:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_38701;
}
else 
{
label_38701:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_38741;
}
else 
{
label_38741:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_38781;
}
else 
{
label_38781:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_38837;
}
else 
{
label_38837:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_38957;
}
else 
{
label_38957:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_38997;
}
else 
{
label_38997:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_39037;
}
else 
{
label_39037:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_39077;
}
else 
{
label_39077:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_39117;
}
else 
{
label_39117:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_39157;
}
else 
{
label_39157:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_39197;
}
else 
{
label_39197:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_39237;
}
else 
{
label_39237:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_39293;
}
else 
{
label_39293:; 
label_39324:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_39846:; 
if (((int)S1_addsub_st) == 0)
{
goto label_39860;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_39860:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_39866;
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
label_39886:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_39896;
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
label_39931:; 
}
label_39999:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40143;
}
else 
{
label_40143:; 
main_in1_req_up = 0;
goto label_40074;
}
}
else 
{
label_40074:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40341;
}
else 
{
label_40341:; 
main_in2_req_up = 0;
goto label_40272;
}
}
else 
{
label_40272:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40539;
}
else 
{
label_40539:; 
main_sum_req_up = 0;
goto label_40470;
}
}
else 
{
label_40470:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40737;
}
else 
{
label_40737:; 
main_diff_req_up = 0;
goto label_40668;
}
}
else 
{
label_40668:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40935;
}
else 
{
label_40935:; 
main_pres_req_up = 0;
goto label_40866;
}
}
else 
{
label_40866:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41133;
}
else 
{
label_41133:; 
main_dbl_req_up = 0;
goto label_41064;
}
}
else 
{
label_41064:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41331;
}
else 
{
label_41331:; 
main_zero_req_up = 0;
goto label_41262;
}
}
else 
{
label_41262:; 
label_41451:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41646;
}
else 
{
label_41646:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41736;
}
else 
{
label_41736:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41826;
}
else 
{
label_41826:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41916;
}
else 
{
label_41916:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_42006;
}
else 
{
label_42006:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42096;
}
else 
{
label_42096:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42186;
}
else 
{
label_42186:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42276;
}
else 
{
label_42276:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42402;
}
else 
{
label_42402:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42672;
}
else 
{
label_42672:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42762;
}
else 
{
label_42762:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42852;
}
else 
{
label_42852:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42942;
}
else 
{
label_42942:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43032;
}
else 
{
label_43032:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43122;
}
else 
{
label_43122:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43212;
}
else 
{
label_43212:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43302;
}
else 
{
label_43302:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43428;
}
else 
{
label_43428:; 
}
goto label_21083;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_39896:; 
if (((int)S3_zero_st) == 0)
{
goto label_39886;
}
else 
{
}
label_40012:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40144;
}
else 
{
label_40144:; 
main_in1_req_up = 0;
goto label_40073;
}
}
else 
{
label_40073:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40342;
}
else 
{
label_40342:; 
main_in2_req_up = 0;
goto label_40271;
}
}
else 
{
label_40271:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40540;
}
else 
{
label_40540:; 
main_sum_req_up = 0;
goto label_40469;
}
}
else 
{
label_40469:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40738;
}
else 
{
label_40738:; 
main_diff_req_up = 0;
goto label_40667;
}
}
else 
{
label_40667:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40936;
}
else 
{
label_40936:; 
main_pres_req_up = 0;
goto label_40865;
}
}
else 
{
label_40865:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41134;
}
else 
{
label_41134:; 
main_dbl_req_up = 0;
goto label_41063;
}
}
else 
{
label_41063:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41332;
}
else 
{
label_41332:; 
main_zero_req_up = 0;
goto label_41261;
}
}
else 
{
label_41261:; 
label_41450:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41647;
}
else 
{
label_41647:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41737;
}
else 
{
label_41737:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41827;
}
else 
{
label_41827:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41917;
}
else 
{
label_41917:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_42007;
}
else 
{
label_42007:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42097;
}
else 
{
label_42097:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42187;
}
else 
{
label_42187:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42277;
}
else 
{
label_42277:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42403;
}
else 
{
label_42403:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42673;
}
else 
{
label_42673:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42763;
}
else 
{
label_42763:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42853;
}
else 
{
label_42853:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42943;
}
else 
{
label_42943:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43033;
}
else 
{
label_43033:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43123;
}
else 
{
label_43123:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43213;
}
else 
{
label_43213:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43303;
}
else 
{
label_43303:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43429;
}
else 
{
label_43429:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_43888:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_43910;
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
}
goto label_39999;
}
else 
{
label_43910:; 
goto label_43888;
}
}
else 
{
}
goto label_40012;
}
}
else 
{
}
goto label_21082;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_39866:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_39895;
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
label_39932:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_39959;
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
goto label_39931;
}
}
else 
{
label_39959:; 
goto label_39932;
}
}
else 
{
}
label_39953:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40142;
}
else 
{
label_40142:; 
main_in1_req_up = 0;
goto label_40075;
}
}
else 
{
label_40075:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40340;
}
else 
{
label_40340:; 
main_in2_req_up = 0;
goto label_40273;
}
}
else 
{
label_40273:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40538;
}
else 
{
label_40538:; 
main_sum_req_up = 0;
goto label_40471;
}
}
else 
{
label_40471:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40736;
}
else 
{
label_40736:; 
main_diff_req_up = 0;
goto label_40669;
}
}
else 
{
label_40669:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40934;
}
else 
{
label_40934:; 
main_pres_req_up = 0;
goto label_40867;
}
}
else 
{
label_40867:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41132;
}
else 
{
label_41132:; 
main_dbl_req_up = 0;
goto label_41065;
}
}
else 
{
label_41065:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41330;
}
else 
{
label_41330:; 
main_zero_req_up = 0;
goto label_41263;
}
}
else 
{
label_41263:; 
label_41452:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41645;
}
else 
{
label_41645:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41735;
}
else 
{
label_41735:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41825;
}
else 
{
label_41825:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41915;
}
else 
{
label_41915:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_42005;
}
else 
{
label_42005:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42095;
}
else 
{
label_42095:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42185;
}
else 
{
label_42185:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42275;
}
else 
{
label_42275:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42401;
}
else 
{
label_42401:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42671;
}
else 
{
label_42671:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42761;
}
else 
{
label_42761:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42851;
}
else 
{
label_42851:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42941;
}
else 
{
label_42941:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43031;
}
else 
{
label_43031:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43121;
}
else 
{
label_43121:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43211;
}
else 
{
label_43211:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43301;
}
else 
{
label_43301:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43427;
}
else 
{
label_43427:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_43740:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_43758;
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
}
goto label_39999;
}
else 
{
label_43758:; 
goto label_43740;
}
}
else 
{
}
goto label_39953;
}
}
else 
{
}
goto label_21084;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_39895:; 
goto label_39846;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40141;
}
else 
{
label_40141:; 
main_in1_req_up = 0;
goto label_40076;
}
}
else 
{
label_40076:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40339;
}
else 
{
label_40339:; 
main_in2_req_up = 0;
goto label_40274;
}
}
else 
{
label_40274:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40537;
}
else 
{
label_40537:; 
main_sum_req_up = 0;
goto label_40472;
}
}
else 
{
label_40472:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40735;
}
else 
{
label_40735:; 
main_diff_req_up = 0;
goto label_40670;
}
}
else 
{
label_40670:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40933;
}
else 
{
label_40933:; 
main_pres_req_up = 0;
goto label_40868;
}
}
else 
{
label_40868:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41131;
}
else 
{
label_41131:; 
main_dbl_req_up = 0;
goto label_41066;
}
}
else 
{
label_41066:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41329;
}
else 
{
label_41329:; 
main_zero_req_up = 0;
goto label_41264;
}
}
else 
{
label_41264:; 
label_41453:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41644;
}
else 
{
label_41644:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41734;
}
else 
{
label_41734:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41824;
}
else 
{
label_41824:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41914;
}
else 
{
label_41914:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_42004;
}
else 
{
label_42004:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42094;
}
else 
{
label_42094:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42184;
}
else 
{
label_42184:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42274;
}
else 
{
label_42274:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42400;
}
else 
{
label_42400:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42670;
}
else 
{
label_42670:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42760;
}
else 
{
label_42760:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42850;
}
else 
{
label_42850:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42940;
}
else 
{
label_42940:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43030;
}
else 
{
label_43030:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43120;
}
else 
{
label_43120:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43210;
}
else 
{
label_43210:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43300;
}
else 
{
label_43300:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43426;
}
else 
{
label_43426:; 
if (((int)S1_addsub_st) == 0)
{
goto label_43641;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_43641:; 
goto label_39324;
}
else 
{
}
goto label_21085;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_38505;
}
else 
{
label_38505:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_38545;
}
else 
{
label_38545:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_38585;
}
else 
{
label_38585:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_38625;
}
else 
{
label_38625:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_38665;
}
else 
{
label_38665:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_38705;
}
else 
{
label_38705:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_38745;
}
else 
{
label_38745:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_38785;
}
else 
{
label_38785:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_38841;
}
else 
{
label_38841:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_38961;
}
else 
{
label_38961:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_39001;
}
else 
{
label_39001:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_39041;
}
else 
{
label_39041:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_39081;
}
else 
{
label_39081:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_39121;
}
else 
{
label_39121:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_39161;
}
else 
{
label_39161:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_39201;
}
else 
{
label_39201:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_39241;
}
else 
{
label_39241:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_39297;
}
else 
{
label_39297:; 
label_39320:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_39465:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_39483;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40132;
}
else 
{
label_40132:; 
main_in1_req_up = 0;
goto label_40085;
}
}
else 
{
label_40085:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40330;
}
else 
{
label_40330:; 
main_in2_req_up = 0;
goto label_40283;
}
}
else 
{
label_40283:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40528;
}
else 
{
label_40528:; 
main_sum_req_up = 0;
goto label_40481;
}
}
else 
{
label_40481:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40726;
}
else 
{
label_40726:; 
main_diff_req_up = 0;
goto label_40679;
}
}
else 
{
label_40679:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40924;
}
else 
{
label_40924:; 
main_pres_req_up = 0;
goto label_40877;
}
}
else 
{
label_40877:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41122;
}
else 
{
label_41122:; 
main_dbl_req_up = 0;
goto label_41075;
}
}
else 
{
label_41075:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41320;
}
else 
{
label_41320:; 
main_zero_req_up = 0;
goto label_41273;
}
}
else 
{
label_41273:; 
label_41462:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41635;
}
else 
{
label_41635:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41725;
}
else 
{
label_41725:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41815;
}
else 
{
label_41815:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41905;
}
else 
{
label_41905:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_41995;
}
else 
{
label_41995:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42085;
}
else 
{
label_42085:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42175;
}
else 
{
label_42175:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42265;
}
else 
{
label_42265:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42391;
}
else 
{
label_42391:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42661;
}
else 
{
label_42661:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42751;
}
else 
{
label_42751:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42841;
}
else 
{
label_42841:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42931;
}
else 
{
label_42931:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43021;
}
else 
{
label_43021:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43111;
}
else 
{
label_43111:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43201;
}
else 
{
label_43201:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43291;
}
else 
{
label_43291:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43417;
}
else 
{
label_43417:; 
}
goto label_21094;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_39483:; 
goto label_39465;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40131;
}
else 
{
label_40131:; 
main_in1_req_up = 0;
goto label_40086;
}
}
else 
{
label_40086:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40329;
}
else 
{
label_40329:; 
main_in2_req_up = 0;
goto label_40284;
}
}
else 
{
label_40284:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40527;
}
else 
{
label_40527:; 
main_sum_req_up = 0;
goto label_40482;
}
}
else 
{
label_40482:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40725;
}
else 
{
label_40725:; 
main_diff_req_up = 0;
goto label_40680;
}
}
else 
{
label_40680:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40923;
}
else 
{
label_40923:; 
main_pres_req_up = 0;
goto label_40878;
}
}
else 
{
label_40878:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41121;
}
else 
{
label_41121:; 
main_dbl_req_up = 0;
goto label_41076;
}
}
else 
{
label_41076:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41319;
}
else 
{
label_41319:; 
main_zero_req_up = 0;
goto label_41274;
}
}
else 
{
label_41274:; 
label_41463:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41634;
}
else 
{
label_41634:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41724;
}
else 
{
label_41724:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41814;
}
else 
{
label_41814:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41904;
}
else 
{
label_41904:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_41994;
}
else 
{
label_41994:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42084;
}
else 
{
label_42084:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42174;
}
else 
{
label_42174:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42264;
}
else 
{
label_42264:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42390;
}
else 
{
label_42390:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42660;
}
else 
{
label_42660:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42750;
}
else 
{
label_42750:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42840;
}
else 
{
label_42840:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42930;
}
else 
{
label_42930:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43020;
}
else 
{
label_43020:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43110;
}
else 
{
label_43110:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43200;
}
else 
{
label_43200:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43290;
}
else 
{
label_43290:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43416;
}
else 
{
label_43416:; 
if (((int)S1_addsub_st) == 0)
{
goto label_39320;
}
else 
{
}
goto label_21095;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_38503;
}
else 
{
label_38503:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_38543;
}
else 
{
label_38543:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_38583;
}
else 
{
label_38583:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_38623;
}
else 
{
label_38623:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_38663;
}
else 
{
label_38663:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_38703;
}
else 
{
label_38703:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_38743;
}
else 
{
label_38743:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_38783;
}
else 
{
label_38783:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_38839;
}
else 
{
label_38839:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_38959;
}
else 
{
label_38959:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_38999;
}
else 
{
label_38999:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_39039;
}
else 
{
label_39039:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_39079;
}
else 
{
label_39079:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_39119;
}
else 
{
label_39119:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_39159;
}
else 
{
label_39159:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_39199;
}
else 
{
label_39199:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_39239;
}
else 
{
label_39239:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_39295;
}
else 
{
label_39295:; 
label_39322:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_39601:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_39623;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40136;
}
else 
{
label_40136:; 
main_in1_req_up = 0;
goto label_40081;
}
}
else 
{
label_40081:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40334;
}
else 
{
label_40334:; 
main_in2_req_up = 0;
goto label_40279;
}
}
else 
{
label_40279:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40532;
}
else 
{
label_40532:; 
main_sum_req_up = 0;
goto label_40477;
}
}
else 
{
label_40477:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40730;
}
else 
{
label_40730:; 
main_diff_req_up = 0;
goto label_40675;
}
}
else 
{
label_40675:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40928;
}
else 
{
label_40928:; 
main_pres_req_up = 0;
goto label_40873;
}
}
else 
{
label_40873:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41126;
}
else 
{
label_41126:; 
main_dbl_req_up = 0;
goto label_41071;
}
}
else 
{
label_41071:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41324;
}
else 
{
label_41324:; 
main_zero_req_up = 0;
goto label_41269;
}
}
else 
{
label_41269:; 
label_41458:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41639;
}
else 
{
label_41639:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41729;
}
else 
{
label_41729:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41819;
}
else 
{
label_41819:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41909;
}
else 
{
label_41909:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_41999;
}
else 
{
label_41999:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42089;
}
else 
{
label_42089:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42179;
}
else 
{
label_42179:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42269;
}
else 
{
label_42269:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42395;
}
else 
{
label_42395:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42665;
}
else 
{
label_42665:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42755;
}
else 
{
label_42755:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42845;
}
else 
{
label_42845:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42935;
}
else 
{
label_42935:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43025;
}
else 
{
label_43025:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43115;
}
else 
{
label_43115:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43205;
}
else 
{
label_43205:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43295;
}
else 
{
label_43295:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43421;
}
else 
{
label_43421:; 
}
goto label_21090;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_39623:; 
goto label_39601;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40135;
}
else 
{
label_40135:; 
main_in1_req_up = 0;
goto label_40082;
}
}
else 
{
label_40082:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40333;
}
else 
{
label_40333:; 
main_in2_req_up = 0;
goto label_40280;
}
}
else 
{
label_40280:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40531;
}
else 
{
label_40531:; 
main_sum_req_up = 0;
goto label_40478;
}
}
else 
{
label_40478:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40729;
}
else 
{
label_40729:; 
main_diff_req_up = 0;
goto label_40676;
}
}
else 
{
label_40676:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40927;
}
else 
{
label_40927:; 
main_pres_req_up = 0;
goto label_40874;
}
}
else 
{
label_40874:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41125;
}
else 
{
label_41125:; 
main_dbl_req_up = 0;
goto label_41072;
}
}
else 
{
label_41072:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41323;
}
else 
{
label_41323:; 
main_zero_req_up = 0;
goto label_41270;
}
}
else 
{
label_41270:; 
label_41459:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41638;
}
else 
{
label_41638:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41728;
}
else 
{
label_41728:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41818;
}
else 
{
label_41818:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41908;
}
else 
{
label_41908:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_41998;
}
else 
{
label_41998:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42088;
}
else 
{
label_42088:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42178;
}
else 
{
label_42178:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42268;
}
else 
{
label_42268:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42394;
}
else 
{
label_42394:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42664;
}
else 
{
label_42664:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42754;
}
else 
{
label_42754:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42844;
}
else 
{
label_42844:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42934;
}
else 
{
label_42934:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43024;
}
else 
{
label_43024:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43114;
}
else 
{
label_43114:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43204;
}
else 
{
label_43204:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43294;
}
else 
{
label_43294:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43420;
}
else 
{
label_43420:; 
if (((int)S3_zero_st) == 0)
{
goto label_39322;
}
else 
{
}
goto label_21091;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_38507;
}
else 
{
label_38507:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_38547;
}
else 
{
label_38547:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_38587;
}
else 
{
label_38587:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_38627;
}
else 
{
label_38627:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_38667;
}
else 
{
label_38667:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_38707;
}
else 
{
label_38707:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_38747;
}
else 
{
label_38747:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_38787;
}
else 
{
label_38787:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_38843;
}
else 
{
label_38843:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_38963;
}
else 
{
label_38963:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_39003;
}
else 
{
label_39003:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_39043;
}
else 
{
label_39043:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_39083;
}
else 
{
label_39083:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_39123;
}
else 
{
label_39123:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_39163;
}
else 
{
label_39163:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_39203;
}
else 
{
label_39203:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_39243;
}
else 
{
label_39243:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_39299;
}
else 
{
label_39299:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40128;
}
else 
{
label_40128:; 
main_in1_req_up = 0;
goto label_40089;
}
}
else 
{
label_40089:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40326;
}
else 
{
label_40326:; 
main_in2_req_up = 0;
goto label_40287;
}
}
else 
{
label_40287:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40524;
}
else 
{
label_40524:; 
main_sum_req_up = 0;
goto label_40485;
}
}
else 
{
label_40485:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40722;
}
else 
{
label_40722:; 
main_diff_req_up = 0;
goto label_40683;
}
}
else 
{
label_40683:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40920;
}
else 
{
label_40920:; 
main_pres_req_up = 0;
goto label_40881;
}
}
else 
{
label_40881:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41118;
}
else 
{
label_41118:; 
main_dbl_req_up = 0;
goto label_41079;
}
}
else 
{
label_41079:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41316;
}
else 
{
label_41316:; 
main_zero_req_up = 0;
goto label_41277;
}
}
else 
{
label_41277:; 
label_41466:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41631;
}
else 
{
label_41631:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41721;
}
else 
{
label_41721:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41811;
}
else 
{
label_41811:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41901;
}
else 
{
label_41901:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_41991;
}
else 
{
label_41991:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42081;
}
else 
{
label_42081:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42171;
}
else 
{
label_42171:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42261;
}
else 
{
label_42261:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42387;
}
else 
{
label_42387:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42657;
}
else 
{
label_42657:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42747;
}
else 
{
label_42747:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42837;
}
else 
{
label_42837:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42927;
}
else 
{
label_42927:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43017;
}
else 
{
label_43017:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43107;
}
else 
{
label_43107:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43197;
}
else 
{
label_43197:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43287;
}
else 
{
label_43287:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43413;
}
else 
{
label_43413:; 
}
goto label_21098;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_38502;
}
else 
{
label_38502:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_38542;
}
else 
{
label_38542:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_38582;
}
else 
{
label_38582:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_38622;
}
else 
{
label_38622:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_38662;
}
else 
{
label_38662:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_38702;
}
else 
{
label_38702:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_38742;
}
else 
{
label_38742:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_38782;
}
else 
{
label_38782:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_38838;
}
else 
{
label_38838:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_38958;
}
else 
{
label_38958:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_38998;
}
else 
{
label_38998:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_39038;
}
else 
{
label_39038:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_39078;
}
else 
{
label_39078:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_39118;
}
else 
{
label_39118:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_39158;
}
else 
{
label_39158:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_39198;
}
else 
{
label_39198:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_39238;
}
else 
{
label_39238:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_39294;
}
else 
{
label_39294:; 
label_39323:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_39666:; 
if (((int)S1_addsub_st) == 0)
{
goto label_39680;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_39680:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_39686;
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
label_39706:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_39716;
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
label_39751:; 
}
label_39819:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40139;
}
else 
{
label_40139:; 
main_in1_req_up = 0;
goto label_40078;
}
}
else 
{
label_40078:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40337;
}
else 
{
label_40337:; 
main_in2_req_up = 0;
goto label_40276;
}
}
else 
{
label_40276:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40535;
}
else 
{
label_40535:; 
main_sum_req_up = 0;
goto label_40474;
}
}
else 
{
label_40474:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40733;
}
else 
{
label_40733:; 
main_diff_req_up = 0;
goto label_40672;
}
}
else 
{
label_40672:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40931;
}
else 
{
label_40931:; 
main_pres_req_up = 0;
goto label_40870;
}
}
else 
{
label_40870:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41129;
}
else 
{
label_41129:; 
main_dbl_req_up = 0;
goto label_41068;
}
}
else 
{
label_41068:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41327;
}
else 
{
label_41327:; 
main_zero_req_up = 0;
goto label_41266;
}
}
else 
{
label_41266:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_41493;
}
else 
{
label_41493:; 
main_clk_req_up = 0;
goto label_41451;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41642;
}
else 
{
label_41642:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41732;
}
else 
{
label_41732:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41822;
}
else 
{
label_41822:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41912;
}
else 
{
label_41912:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_42002;
}
else 
{
label_42002:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42092;
}
else 
{
label_42092:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42182;
}
else 
{
label_42182:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42272;
}
else 
{
label_42272:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42398;
}
else 
{
label_42398:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42668;
}
else 
{
label_42668:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42758;
}
else 
{
label_42758:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42848;
}
else 
{
label_42848:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42938;
}
else 
{
label_42938:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43028;
}
else 
{
label_43028:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43118;
}
else 
{
label_43118:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43208;
}
else 
{
label_43208:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43298;
}
else 
{
label_43298:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43424;
}
else 
{
label_43424:; 
}
goto label_21087;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_39716:; 
if (((int)S3_zero_st) == 0)
{
goto label_39706;
}
else 
{
}
label_39832:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40140;
}
else 
{
label_40140:; 
main_in1_req_up = 0;
goto label_40077;
}
}
else 
{
label_40077:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40338;
}
else 
{
label_40338:; 
main_in2_req_up = 0;
goto label_40275;
}
}
else 
{
label_40275:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40536;
}
else 
{
label_40536:; 
main_sum_req_up = 0;
goto label_40473;
}
}
else 
{
label_40473:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40734;
}
else 
{
label_40734:; 
main_diff_req_up = 0;
goto label_40671;
}
}
else 
{
label_40671:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40932;
}
else 
{
label_40932:; 
main_pres_req_up = 0;
goto label_40869;
}
}
else 
{
label_40869:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41130;
}
else 
{
label_41130:; 
main_dbl_req_up = 0;
goto label_41067;
}
}
else 
{
label_41067:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41328;
}
else 
{
label_41328:; 
main_zero_req_up = 0;
goto label_41265;
}
}
else 
{
label_41265:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_41494;
}
else 
{
label_41494:; 
main_clk_req_up = 0;
goto label_41450;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41643;
}
else 
{
label_41643:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41733;
}
else 
{
label_41733:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41823;
}
else 
{
label_41823:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41913;
}
else 
{
label_41913:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_42003;
}
else 
{
label_42003:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42093;
}
else 
{
label_42093:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42183;
}
else 
{
label_42183:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42273;
}
else 
{
label_42273:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42399;
}
else 
{
label_42399:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42669;
}
else 
{
label_42669:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42759;
}
else 
{
label_42759:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42849;
}
else 
{
label_42849:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42939;
}
else 
{
label_42939:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43029;
}
else 
{
label_43029:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43119;
}
else 
{
label_43119:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43209;
}
else 
{
label_43209:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43299;
}
else 
{
label_43299:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43425;
}
else 
{
label_43425:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_43818:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_43840;
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
}
goto label_39819;
}
else 
{
label_43840:; 
goto label_43818;
}
}
else 
{
}
goto label_39832;
}
}
else 
{
}
goto label_21086;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_39686:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_39715;
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
label_39752:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_39779;
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
goto label_39751;
}
}
else 
{
label_39779:; 
goto label_39752;
}
}
else 
{
}
label_39773:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40138;
}
else 
{
label_40138:; 
main_in1_req_up = 0;
goto label_40079;
}
}
else 
{
label_40079:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40336;
}
else 
{
label_40336:; 
main_in2_req_up = 0;
goto label_40277;
}
}
else 
{
label_40277:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40534;
}
else 
{
label_40534:; 
main_sum_req_up = 0;
goto label_40475;
}
}
else 
{
label_40475:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40732;
}
else 
{
label_40732:; 
main_diff_req_up = 0;
goto label_40673;
}
}
else 
{
label_40673:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40930;
}
else 
{
label_40930:; 
main_pres_req_up = 0;
goto label_40871;
}
}
else 
{
label_40871:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41128;
}
else 
{
label_41128:; 
main_dbl_req_up = 0;
goto label_41069;
}
}
else 
{
label_41069:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41326;
}
else 
{
label_41326:; 
main_zero_req_up = 0;
goto label_41267;
}
}
else 
{
label_41267:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_41492;
}
else 
{
label_41492:; 
main_clk_req_up = 0;
goto label_41452;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41641;
}
else 
{
label_41641:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41731;
}
else 
{
label_41731:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41821;
}
else 
{
label_41821:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41911;
}
else 
{
label_41911:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_42001;
}
else 
{
label_42001:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42091;
}
else 
{
label_42091:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42181;
}
else 
{
label_42181:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42271;
}
else 
{
label_42271:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42397;
}
else 
{
label_42397:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42667;
}
else 
{
label_42667:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42757;
}
else 
{
label_42757:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42847;
}
else 
{
label_42847:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42937;
}
else 
{
label_42937:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43027;
}
else 
{
label_43027:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43117;
}
else 
{
label_43117:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43207;
}
else 
{
label_43207:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43297;
}
else 
{
label_43297:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43423;
}
else 
{
label_43423:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_43665:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_43683;
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
}
goto label_39819;
}
else 
{
label_43683:; 
goto label_43665;
}
}
else 
{
}
goto label_39773;
}
}
else 
{
}
goto label_21088;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_39715:; 
goto label_39666;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40137;
}
else 
{
label_40137:; 
main_in1_req_up = 0;
goto label_40080;
}
}
else 
{
label_40080:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40335;
}
else 
{
label_40335:; 
main_in2_req_up = 0;
goto label_40278;
}
}
else 
{
label_40278:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40533;
}
else 
{
label_40533:; 
main_sum_req_up = 0;
goto label_40476;
}
}
else 
{
label_40476:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40731;
}
else 
{
label_40731:; 
main_diff_req_up = 0;
goto label_40674;
}
}
else 
{
label_40674:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40929;
}
else 
{
label_40929:; 
main_pres_req_up = 0;
goto label_40872;
}
}
else 
{
label_40872:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41127;
}
else 
{
label_41127:; 
main_dbl_req_up = 0;
goto label_41070;
}
}
else 
{
label_41070:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41325;
}
else 
{
label_41325:; 
main_zero_req_up = 0;
goto label_41268;
}
}
else 
{
label_41268:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_41491;
}
else 
{
label_41491:; 
main_clk_req_up = 0;
goto label_41453;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41640;
}
else 
{
label_41640:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41730;
}
else 
{
label_41730:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41820;
}
else 
{
label_41820:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41910;
}
else 
{
label_41910:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_42000;
}
else 
{
label_42000:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42090;
}
else 
{
label_42090:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42180;
}
else 
{
label_42180:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42270;
}
else 
{
label_42270:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42396;
}
else 
{
label_42396:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42666;
}
else 
{
label_42666:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42756;
}
else 
{
label_42756:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42846;
}
else 
{
label_42846:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42936;
}
else 
{
label_42936:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43026;
}
else 
{
label_43026:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43116;
}
else 
{
label_43116:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43206;
}
else 
{
label_43206:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43296;
}
else 
{
label_43296:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43422;
}
else 
{
label_43422:; 
if (((int)S1_addsub_st) == 0)
{
goto label_43643;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_43643:; 
goto label_39323;
}
else 
{
}
goto label_21089;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_38506;
}
else 
{
label_38506:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_38546;
}
else 
{
label_38546:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_38586;
}
else 
{
label_38586:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_38626;
}
else 
{
label_38626:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_38666;
}
else 
{
label_38666:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_38706;
}
else 
{
label_38706:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_38746;
}
else 
{
label_38746:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_38786;
}
else 
{
label_38786:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_38842;
}
else 
{
label_38842:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_38962;
}
else 
{
label_38962:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_39002;
}
else 
{
label_39002:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_39042;
}
else 
{
label_39042:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_39082;
}
else 
{
label_39082:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_39122;
}
else 
{
label_39122:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_39162;
}
else 
{
label_39162:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_39202;
}
else 
{
label_39202:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_39242;
}
else 
{
label_39242:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_39298;
}
else 
{
label_39298:; 
label_39319:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_39394:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_39412;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40130;
}
else 
{
label_40130:; 
main_in1_req_up = 0;
goto label_40087;
}
}
else 
{
label_40087:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40328;
}
else 
{
label_40328:; 
main_in2_req_up = 0;
goto label_40285;
}
}
else 
{
label_40285:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40526;
}
else 
{
label_40526:; 
main_sum_req_up = 0;
goto label_40483;
}
}
else 
{
label_40483:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40724;
}
else 
{
label_40724:; 
main_diff_req_up = 0;
goto label_40681;
}
}
else 
{
label_40681:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40922;
}
else 
{
label_40922:; 
main_pres_req_up = 0;
goto label_40879;
}
}
else 
{
label_40879:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41120;
}
else 
{
label_41120:; 
main_dbl_req_up = 0;
goto label_41077;
}
}
else 
{
label_41077:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41318;
}
else 
{
label_41318:; 
main_zero_req_up = 0;
goto label_41275;
}
}
else 
{
label_41275:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_41488;
}
else 
{
label_41488:; 
main_clk_req_up = 0;
goto label_41462;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41633;
}
else 
{
label_41633:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41723;
}
else 
{
label_41723:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41813;
}
else 
{
label_41813:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41903;
}
else 
{
label_41903:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_41993;
}
else 
{
label_41993:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42083;
}
else 
{
label_42083:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42173;
}
else 
{
label_42173:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42263;
}
else 
{
label_42263:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42389;
}
else 
{
label_42389:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42659;
}
else 
{
label_42659:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42749;
}
else 
{
label_42749:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42839;
}
else 
{
label_42839:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42929;
}
else 
{
label_42929:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43019;
}
else 
{
label_43019:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43109;
}
else 
{
label_43109:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43199;
}
else 
{
label_43199:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43289;
}
else 
{
label_43289:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43415;
}
else 
{
label_43415:; 
}
goto label_21096;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_39412:; 
goto label_39394;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40129;
}
else 
{
label_40129:; 
main_in1_req_up = 0;
goto label_40088;
}
}
else 
{
label_40088:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40327;
}
else 
{
label_40327:; 
main_in2_req_up = 0;
goto label_40286;
}
}
else 
{
label_40286:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40525;
}
else 
{
label_40525:; 
main_sum_req_up = 0;
goto label_40484;
}
}
else 
{
label_40484:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40723;
}
else 
{
label_40723:; 
main_diff_req_up = 0;
goto label_40682;
}
}
else 
{
label_40682:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40921;
}
else 
{
label_40921:; 
main_pres_req_up = 0;
goto label_40880;
}
}
else 
{
label_40880:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41119;
}
else 
{
label_41119:; 
main_dbl_req_up = 0;
goto label_41078;
}
}
else 
{
label_41078:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41317;
}
else 
{
label_41317:; 
main_zero_req_up = 0;
goto label_41276;
}
}
else 
{
label_41276:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_41487;
}
else 
{
label_41487:; 
main_clk_req_up = 0;
goto label_41463;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41632;
}
else 
{
label_41632:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41722;
}
else 
{
label_41722:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41812;
}
else 
{
label_41812:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41902;
}
else 
{
label_41902:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_41992;
}
else 
{
label_41992:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42082;
}
else 
{
label_42082:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42172;
}
else 
{
label_42172:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42262;
}
else 
{
label_42262:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42388;
}
else 
{
label_42388:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42658;
}
else 
{
label_42658:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42748;
}
else 
{
label_42748:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42838;
}
else 
{
label_42838:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42928;
}
else 
{
label_42928:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43018;
}
else 
{
label_43018:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43108;
}
else 
{
label_43108:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43198;
}
else 
{
label_43198:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43288;
}
else 
{
label_43288:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43414;
}
else 
{
label_43414:; 
if (((int)S1_addsub_st) == 0)
{
goto label_39319;
}
else 
{
}
goto label_21097;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_38504;
}
else 
{
label_38504:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_38544;
}
else 
{
label_38544:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_38584;
}
else 
{
label_38584:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_38624;
}
else 
{
label_38624:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_38664;
}
else 
{
label_38664:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_38704;
}
else 
{
label_38704:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_38744;
}
else 
{
label_38744:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_38784;
}
else 
{
label_38784:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_38840;
}
else 
{
label_38840:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_38960;
}
else 
{
label_38960:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_39000;
}
else 
{
label_39000:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_39040;
}
else 
{
label_39040:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_39080;
}
else 
{
label_39080:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_39120;
}
else 
{
label_39120:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_39160;
}
else 
{
label_39160:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_39200;
}
else 
{
label_39200:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_39240;
}
else 
{
label_39240:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_39296;
}
else 
{
label_39296:; 
label_39321:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_39536:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_39558;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40134;
}
else 
{
label_40134:; 
main_in1_req_up = 0;
goto label_40083;
}
}
else 
{
label_40083:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40332;
}
else 
{
label_40332:; 
main_in2_req_up = 0;
goto label_40281;
}
}
else 
{
label_40281:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40530;
}
else 
{
label_40530:; 
main_sum_req_up = 0;
goto label_40479;
}
}
else 
{
label_40479:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40728;
}
else 
{
label_40728:; 
main_diff_req_up = 0;
goto label_40677;
}
}
else 
{
label_40677:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40926;
}
else 
{
label_40926:; 
main_pres_req_up = 0;
goto label_40875;
}
}
else 
{
label_40875:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41124;
}
else 
{
label_41124:; 
main_dbl_req_up = 0;
goto label_41073;
}
}
else 
{
label_41073:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41322;
}
else 
{
label_41322:; 
main_zero_req_up = 0;
goto label_41271;
}
}
else 
{
label_41271:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_41490;
}
else 
{
label_41490:; 
main_clk_req_up = 0;
goto label_41458;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41637;
}
else 
{
label_41637:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41727;
}
else 
{
label_41727:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41817;
}
else 
{
label_41817:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41907;
}
else 
{
label_41907:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_41997;
}
else 
{
label_41997:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42087;
}
else 
{
label_42087:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42177;
}
else 
{
label_42177:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42267;
}
else 
{
label_42267:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42393;
}
else 
{
label_42393:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42663;
}
else 
{
label_42663:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42753;
}
else 
{
label_42753:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42843;
}
else 
{
label_42843:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42933;
}
else 
{
label_42933:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43023;
}
else 
{
label_43023:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43113;
}
else 
{
label_43113:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43203;
}
else 
{
label_43203:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43293;
}
else 
{
label_43293:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43419;
}
else 
{
label_43419:; 
}
goto label_21092;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_39558:; 
goto label_39536;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40133;
}
else 
{
label_40133:; 
main_in1_req_up = 0;
goto label_40084;
}
}
else 
{
label_40084:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40331;
}
else 
{
label_40331:; 
main_in2_req_up = 0;
goto label_40282;
}
}
else 
{
label_40282:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40529;
}
else 
{
label_40529:; 
main_sum_req_up = 0;
goto label_40480;
}
}
else 
{
label_40480:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40727;
}
else 
{
label_40727:; 
main_diff_req_up = 0;
goto label_40678;
}
}
else 
{
label_40678:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40925;
}
else 
{
label_40925:; 
main_pres_req_up = 0;
goto label_40876;
}
}
else 
{
label_40876:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41123;
}
else 
{
label_41123:; 
main_dbl_req_up = 0;
goto label_41074;
}
}
else 
{
label_41074:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41321;
}
else 
{
label_41321:; 
main_zero_req_up = 0;
goto label_41272;
}
}
else 
{
label_41272:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_41489;
}
else 
{
label_41489:; 
main_clk_req_up = 0;
goto label_41459;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41636;
}
else 
{
label_41636:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41726;
}
else 
{
label_41726:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41816;
}
else 
{
label_41816:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41906;
}
else 
{
label_41906:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_41996;
}
else 
{
label_41996:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42086;
}
else 
{
label_42086:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42176;
}
else 
{
label_42176:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42266;
}
else 
{
label_42266:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42392;
}
else 
{
label_42392:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42662;
}
else 
{
label_42662:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42752;
}
else 
{
label_42752:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42842;
}
else 
{
label_42842:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42932;
}
else 
{
label_42932:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43022;
}
else 
{
label_43022:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43112;
}
else 
{
label_43112:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43202;
}
else 
{
label_43202:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43292;
}
else 
{
label_43292:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43418;
}
else 
{
label_43418:; 
if (((int)S3_zero_st) == 0)
{
goto label_39321;
}
else 
{
}
goto label_21093;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_38508;
}
else 
{
label_38508:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_38548;
}
else 
{
label_38548:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_38588;
}
else 
{
label_38588:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_38628;
}
else 
{
label_38628:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_38668;
}
else 
{
label_38668:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_38708;
}
else 
{
label_38708:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_38748;
}
else 
{
label_38748:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_38788;
}
else 
{
label_38788:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_38844;
}
else 
{
label_38844:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_38964;
}
else 
{
label_38964:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_39004;
}
else 
{
label_39004:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_39044;
}
else 
{
label_39044:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_39084;
}
else 
{
label_39084:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_39124;
}
else 
{
label_39124:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_39164;
}
else 
{
label_39164:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_39204;
}
else 
{
label_39204:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_39244;
}
else 
{
label_39244:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_39300;
}
else 
{
label_39300:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_40127;
}
else 
{
label_40127:; 
main_in1_req_up = 0;
goto label_40090;
}
}
else 
{
label_40090:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_40325;
}
else 
{
label_40325:; 
main_in2_req_up = 0;
goto label_40288;
}
}
else 
{
label_40288:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_40523;
}
else 
{
label_40523:; 
main_sum_req_up = 0;
goto label_40486;
}
}
else 
{
label_40486:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_40721;
}
else 
{
label_40721:; 
main_diff_req_up = 0;
goto label_40684;
}
}
else 
{
label_40684:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_40919;
}
else 
{
label_40919:; 
main_pres_req_up = 0;
goto label_40882;
}
}
else 
{
label_40882:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_41117;
}
else 
{
label_41117:; 
main_dbl_req_up = 0;
goto label_41080;
}
}
else 
{
label_41080:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_41315;
}
else 
{
label_41315:; 
main_zero_req_up = 0;
goto label_41278;
}
}
else 
{
label_41278:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_41486;
}
else 
{
label_41486:; 
main_clk_req_up = 0;
goto label_41466;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_41630;
}
else 
{
label_41630:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_41720;
}
else 
{
label_41720:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_41810;
}
else 
{
label_41810:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_41900;
}
else 
{
label_41900:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_41990;
}
else 
{
label_41990:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_42080;
}
else 
{
label_42080:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_42170;
}
else 
{
label_42170:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_42260;
}
else 
{
label_42260:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_42386;
}
else 
{
label_42386:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_42656;
}
else 
{
label_42656:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_42746;
}
else 
{
label_42746:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_42836;
}
else 
{
label_42836:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_42926;
}
else 
{
label_42926:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_43016;
}
else 
{
label_43016:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_43106;
}
else 
{
label_43106:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_43196;
}
else 
{
label_43196:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_43286;
}
else 
{
label_43286:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_43412;
}
else 
{
label_43412:; 
}
goto label_21099;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_13434:; 
goto label_13412;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_13707;
}
else 
{
label_13707:; 
main_in1_req_up = 0;
goto label_13682;
}
}
else 
{
label_13682:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_13806;
}
else 
{
label_13806:; 
main_in2_req_up = 0;
goto label_13781;
}
}
else 
{
label_13781:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_13905;
}
else 
{
label_13905:; 
main_sum_req_up = 0;
goto label_13880;
}
}
else 
{
label_13880:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_14004;
}
else 
{
label_14004:; 
main_diff_req_up = 0;
goto label_13979;
}
}
else 
{
label_13979:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_14103;
}
else 
{
label_14103:; 
main_pres_req_up = 0;
goto label_14078;
}
}
else 
{
label_14078:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_14202;
}
else 
{
label_14202:; 
main_dbl_req_up = 0;
goto label_14177;
}
}
else 
{
label_14177:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_14301;
}
else 
{
label_14301:; 
main_zero_req_up = 0;
goto label_14276;
}
}
else 
{
label_14276:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_14400;
}
else 
{
label_14400:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_14445;
}
else 
{
label_14445:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_14490;
}
else 
{
label_14490:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_14535;
}
else 
{
label_14535:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_14580;
}
else 
{
label_14580:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_14625;
}
else 
{
label_14625:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_14670;
}
else 
{
label_14670:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_14715;
}
else 
{
label_14715:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_14778;
}
else 
{
label_14778:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_14913;
}
else 
{
label_14913:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_14958;
}
else 
{
label_14958:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_15003;
}
else 
{
label_15003:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_15048;
}
else 
{
label_15048:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_15093;
}
else 
{
label_15093:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_15138;
}
else 
{
label_15138:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_15183;
}
else 
{
label_15183:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_15228;
}
else 
{
label_15228:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_15291;
}
else 
{
label_15291:; 
if (((int)S3_zero_st) == 0)
{
goto label_13301;
}
else 
{
}
label_15558:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
label_15594:; 
count = count + 1;
if (count == 5)
{
flag = D_z;
if (D_z == 0)
{
goto label_15720;
}
else 
{
{
__VERIFIER_error();
}
label_15720:; 
count = 0;
goto label_15632;
}
}
else 
{
label_15632:; 
main_clk_val_t = 0;
main_clk_req_up = 1;
{
int kernel_st ;
kernel_st = 0;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_32685;
}
else 
{
label_32685:; 
main_in1_req_up = 0;
goto label_32682;
}
}
else 
{
label_32682:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_32696;
}
else 
{
label_32696:; 
main_in2_req_up = 0;
goto label_32693;
}
}
else 
{
label_32693:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_32707;
}
else 
{
label_32707:; 
main_sum_req_up = 0;
goto label_32704;
}
}
else 
{
label_32704:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_32718;
}
else 
{
label_32718:; 
main_diff_req_up = 0;
goto label_32715;
}
}
else 
{
label_32715:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_32729;
}
else 
{
label_32729:; 
main_pres_req_up = 0;
goto label_32726;
}
}
else 
{
label_32726:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_32740;
}
else 
{
label_32740:; 
main_dbl_req_up = 0;
goto label_32737;
}
}
else 
{
label_32737:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_32751;
}
else 
{
label_32751:; 
main_zero_req_up = 0;
goto label_32748;
}
}
else 
{
label_32748:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_32762;
}
else 
{
label_32762:; 
main_clk_req_up = 0;
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_32866;
}
else 
{
label_32866:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_32906;
}
else 
{
label_32906:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_32946;
}
else 
{
label_32946:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_32986;
}
else 
{
label_32986:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_33026;
}
else 
{
label_33026:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_33066;
}
else 
{
label_33066:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_33106;
}
else 
{
label_33106:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_33146;
}
else 
{
label_33146:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_33202;
}
else 
{
label_33202:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_33322;
}
else 
{
label_33322:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_33362;
}
else 
{
label_33362:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_33402;
}
else 
{
label_33402:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_33442;
}
else 
{
label_33442:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_33482;
}
else 
{
label_33482:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_33522;
}
else 
{
label_33522:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_33562;
}
else 
{
label_33562:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_33602;
}
else 
{
label_33602:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_33658;
}
else 
{
label_33658:; 
label_33689:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_34211:; 
if (((int)S1_addsub_st) == 0)
{
goto label_34225;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_34225:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_34231;
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
label_34251:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_34261;
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
label_34296:; 
}
label_34364:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34508;
}
else 
{
label_34508:; 
main_in1_req_up = 0;
goto label_34439;
}
}
else 
{
label_34439:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34706;
}
else 
{
label_34706:; 
main_in2_req_up = 0;
goto label_34637;
}
}
else 
{
label_34637:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34904;
}
else 
{
label_34904:; 
main_sum_req_up = 0;
goto label_34835;
}
}
else 
{
label_34835:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35102;
}
else 
{
label_35102:; 
main_diff_req_up = 0;
goto label_35033;
}
}
else 
{
label_35033:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35300;
}
else 
{
label_35300:; 
main_pres_req_up = 0;
goto label_35231;
}
}
else 
{
label_35231:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35498;
}
else 
{
label_35498:; 
main_dbl_req_up = 0;
goto label_35429;
}
}
else 
{
label_35429:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35696;
}
else 
{
label_35696:; 
main_zero_req_up = 0;
goto label_35627;
}
}
else 
{
label_35627:; 
label_35816:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_36011;
}
else 
{
label_36011:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36101;
}
else 
{
label_36101:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36191;
}
else 
{
label_36191:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36281;
}
else 
{
label_36281:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36371;
}
else 
{
label_36371:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36461;
}
else 
{
label_36461:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36551;
}
else 
{
label_36551:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36641;
}
else 
{
label_36641:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36767;
}
else 
{
label_36767:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37037;
}
else 
{
label_37037:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37127;
}
else 
{
label_37127:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37217;
}
else 
{
label_37217:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37307;
}
else 
{
label_37307:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37397;
}
else 
{
label_37397:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37487;
}
else 
{
label_37487:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37577;
}
else 
{
label_37577:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37667;
}
else 
{
label_37667:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37793;
}
else 
{
label_37793:; 
}
goto label_21083;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_34261:; 
if (((int)S3_zero_st) == 0)
{
goto label_34251;
}
else 
{
}
label_34377:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34509;
}
else 
{
label_34509:; 
main_in1_req_up = 0;
goto label_34438;
}
}
else 
{
label_34438:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34707;
}
else 
{
label_34707:; 
main_in2_req_up = 0;
goto label_34636;
}
}
else 
{
label_34636:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34905;
}
else 
{
label_34905:; 
main_sum_req_up = 0;
goto label_34834;
}
}
else 
{
label_34834:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35103;
}
else 
{
label_35103:; 
main_diff_req_up = 0;
goto label_35032;
}
}
else 
{
label_35032:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35301;
}
else 
{
label_35301:; 
main_pres_req_up = 0;
goto label_35230;
}
}
else 
{
label_35230:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35499;
}
else 
{
label_35499:; 
main_dbl_req_up = 0;
goto label_35428;
}
}
else 
{
label_35428:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35697;
}
else 
{
label_35697:; 
main_zero_req_up = 0;
goto label_35626;
}
}
else 
{
label_35626:; 
label_35815:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_36012;
}
else 
{
label_36012:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36102;
}
else 
{
label_36102:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36192;
}
else 
{
label_36192:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36282;
}
else 
{
label_36282:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36372;
}
else 
{
label_36372:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36462;
}
else 
{
label_36462:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36552;
}
else 
{
label_36552:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36642;
}
else 
{
label_36642:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36768;
}
else 
{
label_36768:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37038;
}
else 
{
label_37038:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37128;
}
else 
{
label_37128:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37218;
}
else 
{
label_37218:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37308;
}
else 
{
label_37308:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37398;
}
else 
{
label_37398:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37488;
}
else 
{
label_37488:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37578;
}
else 
{
label_37578:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37668;
}
else 
{
label_37668:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37794;
}
else 
{
label_37794:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_38253:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_38275;
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
}
goto label_34364;
}
else 
{
label_38275:; 
goto label_38253;
}
}
else 
{
}
goto label_34377;
}
}
else 
{
}
goto label_21082;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_34231:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_34260;
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
label_34297:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_34324;
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
goto label_34296;
}
}
else 
{
label_34324:; 
goto label_34297;
}
}
else 
{
}
label_34318:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34507;
}
else 
{
label_34507:; 
main_in1_req_up = 0;
goto label_34440;
}
}
else 
{
label_34440:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34705;
}
else 
{
label_34705:; 
main_in2_req_up = 0;
goto label_34638;
}
}
else 
{
label_34638:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34903;
}
else 
{
label_34903:; 
main_sum_req_up = 0;
goto label_34836;
}
}
else 
{
label_34836:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35101;
}
else 
{
label_35101:; 
main_diff_req_up = 0;
goto label_35034;
}
}
else 
{
label_35034:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35299;
}
else 
{
label_35299:; 
main_pres_req_up = 0;
goto label_35232;
}
}
else 
{
label_35232:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35497;
}
else 
{
label_35497:; 
main_dbl_req_up = 0;
goto label_35430;
}
}
else 
{
label_35430:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35695;
}
else 
{
label_35695:; 
main_zero_req_up = 0;
goto label_35628;
}
}
else 
{
label_35628:; 
label_35817:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_36010;
}
else 
{
label_36010:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36100;
}
else 
{
label_36100:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36190;
}
else 
{
label_36190:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36280;
}
else 
{
label_36280:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36370;
}
else 
{
label_36370:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36460;
}
else 
{
label_36460:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36550;
}
else 
{
label_36550:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36640;
}
else 
{
label_36640:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36766;
}
else 
{
label_36766:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37036;
}
else 
{
label_37036:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37126;
}
else 
{
label_37126:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37216;
}
else 
{
label_37216:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37306;
}
else 
{
label_37306:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37396;
}
else 
{
label_37396:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37486;
}
else 
{
label_37486:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37576;
}
else 
{
label_37576:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37666;
}
else 
{
label_37666:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37792;
}
else 
{
label_37792:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_38105:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_38123;
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
}
goto label_34364;
}
else 
{
label_38123:; 
goto label_38105;
}
}
else 
{
}
goto label_34318;
}
}
else 
{
}
goto label_21084;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_34260:; 
goto label_34211;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34506;
}
else 
{
label_34506:; 
main_in1_req_up = 0;
goto label_34441;
}
}
else 
{
label_34441:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34704;
}
else 
{
label_34704:; 
main_in2_req_up = 0;
goto label_34639;
}
}
else 
{
label_34639:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34902;
}
else 
{
label_34902:; 
main_sum_req_up = 0;
goto label_34837;
}
}
else 
{
label_34837:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35100;
}
else 
{
label_35100:; 
main_diff_req_up = 0;
goto label_35035;
}
}
else 
{
label_35035:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35298;
}
else 
{
label_35298:; 
main_pres_req_up = 0;
goto label_35233;
}
}
else 
{
label_35233:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35496;
}
else 
{
label_35496:; 
main_dbl_req_up = 0;
goto label_35431;
}
}
else 
{
label_35431:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35694;
}
else 
{
label_35694:; 
main_zero_req_up = 0;
goto label_35629;
}
}
else 
{
label_35629:; 
label_35818:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_36009;
}
else 
{
label_36009:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36099;
}
else 
{
label_36099:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36189;
}
else 
{
label_36189:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36279;
}
else 
{
label_36279:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36369;
}
else 
{
label_36369:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36459;
}
else 
{
label_36459:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36549;
}
else 
{
label_36549:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36639;
}
else 
{
label_36639:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36765;
}
else 
{
label_36765:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37035;
}
else 
{
label_37035:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37125;
}
else 
{
label_37125:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37215;
}
else 
{
label_37215:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37305;
}
else 
{
label_37305:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37395;
}
else 
{
label_37395:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37485;
}
else 
{
label_37485:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37575;
}
else 
{
label_37575:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37665;
}
else 
{
label_37665:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37791;
}
else 
{
label_37791:; 
if (((int)S1_addsub_st) == 0)
{
goto label_38006;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_38006:; 
goto label_33689;
}
else 
{
}
goto label_21085;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_32870;
}
else 
{
label_32870:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_32910;
}
else 
{
label_32910:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_32950;
}
else 
{
label_32950:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_32990;
}
else 
{
label_32990:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_33030;
}
else 
{
label_33030:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_33070;
}
else 
{
label_33070:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_33110;
}
else 
{
label_33110:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_33150;
}
else 
{
label_33150:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_33206;
}
else 
{
label_33206:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_33326;
}
else 
{
label_33326:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_33366;
}
else 
{
label_33366:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_33406;
}
else 
{
label_33406:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_33446;
}
else 
{
label_33446:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_33486;
}
else 
{
label_33486:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_33526;
}
else 
{
label_33526:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_33566;
}
else 
{
label_33566:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_33606;
}
else 
{
label_33606:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_33662;
}
else 
{
label_33662:; 
label_33685:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_33830:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_33848;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34497;
}
else 
{
label_34497:; 
main_in1_req_up = 0;
goto label_34450;
}
}
else 
{
label_34450:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34695;
}
else 
{
label_34695:; 
main_in2_req_up = 0;
goto label_34648;
}
}
else 
{
label_34648:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34893;
}
else 
{
label_34893:; 
main_sum_req_up = 0;
goto label_34846;
}
}
else 
{
label_34846:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35091;
}
else 
{
label_35091:; 
main_diff_req_up = 0;
goto label_35044;
}
}
else 
{
label_35044:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35289;
}
else 
{
label_35289:; 
main_pres_req_up = 0;
goto label_35242;
}
}
else 
{
label_35242:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35487;
}
else 
{
label_35487:; 
main_dbl_req_up = 0;
goto label_35440;
}
}
else 
{
label_35440:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35685;
}
else 
{
label_35685:; 
main_zero_req_up = 0;
goto label_35638;
}
}
else 
{
label_35638:; 
label_35827:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_36000;
}
else 
{
label_36000:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36090;
}
else 
{
label_36090:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36180;
}
else 
{
label_36180:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36270;
}
else 
{
label_36270:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36360;
}
else 
{
label_36360:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36450;
}
else 
{
label_36450:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36540;
}
else 
{
label_36540:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36630;
}
else 
{
label_36630:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36756;
}
else 
{
label_36756:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37026;
}
else 
{
label_37026:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37116;
}
else 
{
label_37116:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37206;
}
else 
{
label_37206:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37296;
}
else 
{
label_37296:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37386;
}
else 
{
label_37386:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37476;
}
else 
{
label_37476:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37566;
}
else 
{
label_37566:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37656;
}
else 
{
label_37656:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37782;
}
else 
{
label_37782:; 
}
goto label_21094;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_33848:; 
goto label_33830;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34496;
}
else 
{
label_34496:; 
main_in1_req_up = 0;
goto label_34451;
}
}
else 
{
label_34451:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34694;
}
else 
{
label_34694:; 
main_in2_req_up = 0;
goto label_34649;
}
}
else 
{
label_34649:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34892;
}
else 
{
label_34892:; 
main_sum_req_up = 0;
goto label_34847;
}
}
else 
{
label_34847:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35090;
}
else 
{
label_35090:; 
main_diff_req_up = 0;
goto label_35045;
}
}
else 
{
label_35045:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35288;
}
else 
{
label_35288:; 
main_pres_req_up = 0;
goto label_35243;
}
}
else 
{
label_35243:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35486;
}
else 
{
label_35486:; 
main_dbl_req_up = 0;
goto label_35441;
}
}
else 
{
label_35441:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35684;
}
else 
{
label_35684:; 
main_zero_req_up = 0;
goto label_35639;
}
}
else 
{
label_35639:; 
label_35828:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_35999;
}
else 
{
label_35999:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36089;
}
else 
{
label_36089:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36179;
}
else 
{
label_36179:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36269;
}
else 
{
label_36269:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36359;
}
else 
{
label_36359:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36449;
}
else 
{
label_36449:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36539;
}
else 
{
label_36539:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36629;
}
else 
{
label_36629:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36755;
}
else 
{
label_36755:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37025;
}
else 
{
label_37025:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37115;
}
else 
{
label_37115:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37205;
}
else 
{
label_37205:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37295;
}
else 
{
label_37295:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37385;
}
else 
{
label_37385:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37475;
}
else 
{
label_37475:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37565;
}
else 
{
label_37565:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37655;
}
else 
{
label_37655:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37781;
}
else 
{
label_37781:; 
if (((int)S1_addsub_st) == 0)
{
goto label_33685;
}
else 
{
}
goto label_21095;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_32868;
}
else 
{
label_32868:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_32908;
}
else 
{
label_32908:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_32948;
}
else 
{
label_32948:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_32988;
}
else 
{
label_32988:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_33028;
}
else 
{
label_33028:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_33068;
}
else 
{
label_33068:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_33108;
}
else 
{
label_33108:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_33148;
}
else 
{
label_33148:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_33204;
}
else 
{
label_33204:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_33324;
}
else 
{
label_33324:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_33364;
}
else 
{
label_33364:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_33404;
}
else 
{
label_33404:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_33444;
}
else 
{
label_33444:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_33484;
}
else 
{
label_33484:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_33524;
}
else 
{
label_33524:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_33564;
}
else 
{
label_33564:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_33604;
}
else 
{
label_33604:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_33660;
}
else 
{
label_33660:; 
label_33687:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_33966:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_33988;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34501;
}
else 
{
label_34501:; 
main_in1_req_up = 0;
goto label_34446;
}
}
else 
{
label_34446:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34699;
}
else 
{
label_34699:; 
main_in2_req_up = 0;
goto label_34644;
}
}
else 
{
label_34644:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34897;
}
else 
{
label_34897:; 
main_sum_req_up = 0;
goto label_34842;
}
}
else 
{
label_34842:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35095;
}
else 
{
label_35095:; 
main_diff_req_up = 0;
goto label_35040;
}
}
else 
{
label_35040:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35293;
}
else 
{
label_35293:; 
main_pres_req_up = 0;
goto label_35238;
}
}
else 
{
label_35238:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35491;
}
else 
{
label_35491:; 
main_dbl_req_up = 0;
goto label_35436;
}
}
else 
{
label_35436:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35689;
}
else 
{
label_35689:; 
main_zero_req_up = 0;
goto label_35634;
}
}
else 
{
label_35634:; 
label_35823:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_36004;
}
else 
{
label_36004:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36094;
}
else 
{
label_36094:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36184;
}
else 
{
label_36184:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36274;
}
else 
{
label_36274:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36364;
}
else 
{
label_36364:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36454;
}
else 
{
label_36454:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36544;
}
else 
{
label_36544:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36634;
}
else 
{
label_36634:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36760;
}
else 
{
label_36760:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37030;
}
else 
{
label_37030:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37120;
}
else 
{
label_37120:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37210;
}
else 
{
label_37210:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37300;
}
else 
{
label_37300:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37390;
}
else 
{
label_37390:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37480;
}
else 
{
label_37480:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37570;
}
else 
{
label_37570:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37660;
}
else 
{
label_37660:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37786;
}
else 
{
label_37786:; 
}
goto label_21090;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_33988:; 
goto label_33966;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34500;
}
else 
{
label_34500:; 
main_in1_req_up = 0;
goto label_34447;
}
}
else 
{
label_34447:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34698;
}
else 
{
label_34698:; 
main_in2_req_up = 0;
goto label_34645;
}
}
else 
{
label_34645:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34896;
}
else 
{
label_34896:; 
main_sum_req_up = 0;
goto label_34843;
}
}
else 
{
label_34843:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35094;
}
else 
{
label_35094:; 
main_diff_req_up = 0;
goto label_35041;
}
}
else 
{
label_35041:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35292;
}
else 
{
label_35292:; 
main_pres_req_up = 0;
goto label_35239;
}
}
else 
{
label_35239:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35490;
}
else 
{
label_35490:; 
main_dbl_req_up = 0;
goto label_35437;
}
}
else 
{
label_35437:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35688;
}
else 
{
label_35688:; 
main_zero_req_up = 0;
goto label_35635;
}
}
else 
{
label_35635:; 
label_35824:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_36003;
}
else 
{
label_36003:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36093;
}
else 
{
label_36093:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36183;
}
else 
{
label_36183:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36273;
}
else 
{
label_36273:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36363;
}
else 
{
label_36363:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36453;
}
else 
{
label_36453:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36543;
}
else 
{
label_36543:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36633;
}
else 
{
label_36633:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36759;
}
else 
{
label_36759:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37029;
}
else 
{
label_37029:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37119;
}
else 
{
label_37119:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37209;
}
else 
{
label_37209:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37299;
}
else 
{
label_37299:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37389;
}
else 
{
label_37389:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37479;
}
else 
{
label_37479:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37569;
}
else 
{
label_37569:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37659;
}
else 
{
label_37659:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37785;
}
else 
{
label_37785:; 
if (((int)S3_zero_st) == 0)
{
goto label_33687;
}
else 
{
}
goto label_21091;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_32872;
}
else 
{
label_32872:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_32912;
}
else 
{
label_32912:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_32952;
}
else 
{
label_32952:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_32992;
}
else 
{
label_32992:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_33032;
}
else 
{
label_33032:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_33072;
}
else 
{
label_33072:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_33112;
}
else 
{
label_33112:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_33152;
}
else 
{
label_33152:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_33208;
}
else 
{
label_33208:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_33328;
}
else 
{
label_33328:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_33368;
}
else 
{
label_33368:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_33408;
}
else 
{
label_33408:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_33448;
}
else 
{
label_33448:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_33488;
}
else 
{
label_33488:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_33528;
}
else 
{
label_33528:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_33568;
}
else 
{
label_33568:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_33608;
}
else 
{
label_33608:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_33664;
}
else 
{
label_33664:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34493;
}
else 
{
label_34493:; 
main_in1_req_up = 0;
goto label_34454;
}
}
else 
{
label_34454:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34691;
}
else 
{
label_34691:; 
main_in2_req_up = 0;
goto label_34652;
}
}
else 
{
label_34652:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34889;
}
else 
{
label_34889:; 
main_sum_req_up = 0;
goto label_34850;
}
}
else 
{
label_34850:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35087;
}
else 
{
label_35087:; 
main_diff_req_up = 0;
goto label_35048;
}
}
else 
{
label_35048:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35285;
}
else 
{
label_35285:; 
main_pres_req_up = 0;
goto label_35246;
}
}
else 
{
label_35246:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35483;
}
else 
{
label_35483:; 
main_dbl_req_up = 0;
goto label_35444;
}
}
else 
{
label_35444:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35681;
}
else 
{
label_35681:; 
main_zero_req_up = 0;
goto label_35642;
}
}
else 
{
label_35642:; 
label_35831:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_35996;
}
else 
{
label_35996:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36086;
}
else 
{
label_36086:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36176;
}
else 
{
label_36176:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36266;
}
else 
{
label_36266:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36356;
}
else 
{
label_36356:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36446;
}
else 
{
label_36446:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36536;
}
else 
{
label_36536:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36626;
}
else 
{
label_36626:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36752;
}
else 
{
label_36752:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37022;
}
else 
{
label_37022:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37112;
}
else 
{
label_37112:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37202;
}
else 
{
label_37202:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37292;
}
else 
{
label_37292:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37382;
}
else 
{
label_37382:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37472;
}
else 
{
label_37472:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37562;
}
else 
{
label_37562:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37652;
}
else 
{
label_37652:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37778;
}
else 
{
label_37778:; 
}
goto label_21098;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_32867;
}
else 
{
label_32867:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_32907;
}
else 
{
label_32907:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_32947;
}
else 
{
label_32947:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_32987;
}
else 
{
label_32987:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_33027;
}
else 
{
label_33027:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_33067;
}
else 
{
label_33067:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_33107;
}
else 
{
label_33107:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_33147;
}
else 
{
label_33147:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_33203;
}
else 
{
label_33203:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_33323;
}
else 
{
label_33323:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_33363;
}
else 
{
label_33363:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_33403;
}
else 
{
label_33403:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_33443;
}
else 
{
label_33443:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_33483;
}
else 
{
label_33483:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_33523;
}
else 
{
label_33523:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_33563;
}
else 
{
label_33563:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_33603;
}
else 
{
label_33603:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_33659;
}
else 
{
label_33659:; 
label_33688:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_34031:; 
if (((int)S1_addsub_st) == 0)
{
goto label_34045;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_34045:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_34051;
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
label_34071:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_34081;
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
label_34116:; 
}
label_34184:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34504;
}
else 
{
label_34504:; 
main_in1_req_up = 0;
goto label_34443;
}
}
else 
{
label_34443:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34702;
}
else 
{
label_34702:; 
main_in2_req_up = 0;
goto label_34641;
}
}
else 
{
label_34641:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34900;
}
else 
{
label_34900:; 
main_sum_req_up = 0;
goto label_34839;
}
}
else 
{
label_34839:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35098;
}
else 
{
label_35098:; 
main_diff_req_up = 0;
goto label_35037;
}
}
else 
{
label_35037:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35296;
}
else 
{
label_35296:; 
main_pres_req_up = 0;
goto label_35235;
}
}
else 
{
label_35235:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35494;
}
else 
{
label_35494:; 
main_dbl_req_up = 0;
goto label_35433;
}
}
else 
{
label_35433:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35692;
}
else 
{
label_35692:; 
main_zero_req_up = 0;
goto label_35631;
}
}
else 
{
label_35631:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_35858;
}
else 
{
label_35858:; 
main_clk_req_up = 0;
goto label_35816;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_36007;
}
else 
{
label_36007:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36097;
}
else 
{
label_36097:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36187;
}
else 
{
label_36187:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36277;
}
else 
{
label_36277:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36367;
}
else 
{
label_36367:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36457;
}
else 
{
label_36457:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36547;
}
else 
{
label_36547:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36637;
}
else 
{
label_36637:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36763;
}
else 
{
label_36763:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37033;
}
else 
{
label_37033:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37123;
}
else 
{
label_37123:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37213;
}
else 
{
label_37213:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37303;
}
else 
{
label_37303:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37393;
}
else 
{
label_37393:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37483;
}
else 
{
label_37483:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37573;
}
else 
{
label_37573:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37663;
}
else 
{
label_37663:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37789;
}
else 
{
label_37789:; 
}
goto label_21087;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_34081:; 
if (((int)S3_zero_st) == 0)
{
goto label_34071;
}
else 
{
}
label_34197:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34505;
}
else 
{
label_34505:; 
main_in1_req_up = 0;
goto label_34442;
}
}
else 
{
label_34442:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34703;
}
else 
{
label_34703:; 
main_in2_req_up = 0;
goto label_34640;
}
}
else 
{
label_34640:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34901;
}
else 
{
label_34901:; 
main_sum_req_up = 0;
goto label_34838;
}
}
else 
{
label_34838:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35099;
}
else 
{
label_35099:; 
main_diff_req_up = 0;
goto label_35036;
}
}
else 
{
label_35036:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35297;
}
else 
{
label_35297:; 
main_pres_req_up = 0;
goto label_35234;
}
}
else 
{
label_35234:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35495;
}
else 
{
label_35495:; 
main_dbl_req_up = 0;
goto label_35432;
}
}
else 
{
label_35432:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35693;
}
else 
{
label_35693:; 
main_zero_req_up = 0;
goto label_35630;
}
}
else 
{
label_35630:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_35859;
}
else 
{
label_35859:; 
main_clk_req_up = 0;
goto label_35815;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_36008;
}
else 
{
label_36008:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36098;
}
else 
{
label_36098:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36188;
}
else 
{
label_36188:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36278;
}
else 
{
label_36278:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36368;
}
else 
{
label_36368:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36458;
}
else 
{
label_36458:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36548;
}
else 
{
label_36548:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36638;
}
else 
{
label_36638:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36764;
}
else 
{
label_36764:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37034;
}
else 
{
label_37034:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37124;
}
else 
{
label_37124:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37214;
}
else 
{
label_37214:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37304;
}
else 
{
label_37304:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37394;
}
else 
{
label_37394:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37484;
}
else 
{
label_37484:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37574;
}
else 
{
label_37574:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37664;
}
else 
{
label_37664:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37790;
}
else 
{
label_37790:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_38183:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_38205;
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
}
goto label_34184;
}
else 
{
label_38205:; 
goto label_38183;
}
}
else 
{
}
goto label_34197;
}
}
else 
{
}
goto label_21086;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_34051:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_34080;
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
label_34117:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_34144;
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
goto label_34116;
}
}
else 
{
label_34144:; 
goto label_34117;
}
}
else 
{
}
label_34138:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34503;
}
else 
{
label_34503:; 
main_in1_req_up = 0;
goto label_34444;
}
}
else 
{
label_34444:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34701;
}
else 
{
label_34701:; 
main_in2_req_up = 0;
goto label_34642;
}
}
else 
{
label_34642:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34899;
}
else 
{
label_34899:; 
main_sum_req_up = 0;
goto label_34840;
}
}
else 
{
label_34840:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35097;
}
else 
{
label_35097:; 
main_diff_req_up = 0;
goto label_35038;
}
}
else 
{
label_35038:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35295;
}
else 
{
label_35295:; 
main_pres_req_up = 0;
goto label_35236;
}
}
else 
{
label_35236:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35493;
}
else 
{
label_35493:; 
main_dbl_req_up = 0;
goto label_35434;
}
}
else 
{
label_35434:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35691;
}
else 
{
label_35691:; 
main_zero_req_up = 0;
goto label_35632;
}
}
else 
{
label_35632:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_35857;
}
else 
{
label_35857:; 
main_clk_req_up = 0;
goto label_35817;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_36006;
}
else 
{
label_36006:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36096;
}
else 
{
label_36096:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36186;
}
else 
{
label_36186:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36276;
}
else 
{
label_36276:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36366;
}
else 
{
label_36366:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36456;
}
else 
{
label_36456:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36546;
}
else 
{
label_36546:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36636;
}
else 
{
label_36636:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36762;
}
else 
{
label_36762:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37032;
}
else 
{
label_37032:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37122;
}
else 
{
label_37122:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37212;
}
else 
{
label_37212:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37302;
}
else 
{
label_37302:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37392;
}
else 
{
label_37392:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37482;
}
else 
{
label_37482:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37572;
}
else 
{
label_37572:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37662;
}
else 
{
label_37662:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37788;
}
else 
{
label_37788:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_38030:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_38048;
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
}
goto label_34184;
}
else 
{
label_38048:; 
goto label_38030;
}
}
else 
{
}
goto label_34138;
}
}
else 
{
}
goto label_21088;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_34080:; 
goto label_34031;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34502;
}
else 
{
label_34502:; 
main_in1_req_up = 0;
goto label_34445;
}
}
else 
{
label_34445:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34700;
}
else 
{
label_34700:; 
main_in2_req_up = 0;
goto label_34643;
}
}
else 
{
label_34643:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34898;
}
else 
{
label_34898:; 
main_sum_req_up = 0;
goto label_34841;
}
}
else 
{
label_34841:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35096;
}
else 
{
label_35096:; 
main_diff_req_up = 0;
goto label_35039;
}
}
else 
{
label_35039:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35294;
}
else 
{
label_35294:; 
main_pres_req_up = 0;
goto label_35237;
}
}
else 
{
label_35237:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35492;
}
else 
{
label_35492:; 
main_dbl_req_up = 0;
goto label_35435;
}
}
else 
{
label_35435:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35690;
}
else 
{
label_35690:; 
main_zero_req_up = 0;
goto label_35633;
}
}
else 
{
label_35633:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_35856;
}
else 
{
label_35856:; 
main_clk_req_up = 0;
goto label_35818;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_36005;
}
else 
{
label_36005:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36095;
}
else 
{
label_36095:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36185;
}
else 
{
label_36185:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36275;
}
else 
{
label_36275:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36365;
}
else 
{
label_36365:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36455;
}
else 
{
label_36455:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36545;
}
else 
{
label_36545:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36635;
}
else 
{
label_36635:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36761;
}
else 
{
label_36761:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37031;
}
else 
{
label_37031:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37121;
}
else 
{
label_37121:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37211;
}
else 
{
label_37211:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37301;
}
else 
{
label_37301:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37391;
}
else 
{
label_37391:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37481;
}
else 
{
label_37481:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37571;
}
else 
{
label_37571:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37661;
}
else 
{
label_37661:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37787;
}
else 
{
label_37787:; 
if (((int)S1_addsub_st) == 0)
{
goto label_38008;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_38008:; 
goto label_33688;
}
else 
{
}
goto label_21089;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_32871;
}
else 
{
label_32871:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_32911;
}
else 
{
label_32911:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_32951;
}
else 
{
label_32951:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_32991;
}
else 
{
label_32991:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_33031;
}
else 
{
label_33031:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_33071;
}
else 
{
label_33071:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_33111;
}
else 
{
label_33111:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_33151;
}
else 
{
label_33151:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_33207;
}
else 
{
label_33207:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_33327;
}
else 
{
label_33327:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_33367;
}
else 
{
label_33367:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_33407;
}
else 
{
label_33407:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_33447;
}
else 
{
label_33447:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_33487;
}
else 
{
label_33487:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_33527;
}
else 
{
label_33527:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_33567;
}
else 
{
label_33567:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_33607;
}
else 
{
label_33607:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_33663;
}
else 
{
label_33663:; 
label_33684:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_33759:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_33777;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34495;
}
else 
{
label_34495:; 
main_in1_req_up = 0;
goto label_34452;
}
}
else 
{
label_34452:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34693;
}
else 
{
label_34693:; 
main_in2_req_up = 0;
goto label_34650;
}
}
else 
{
label_34650:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34891;
}
else 
{
label_34891:; 
main_sum_req_up = 0;
goto label_34848;
}
}
else 
{
label_34848:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35089;
}
else 
{
label_35089:; 
main_diff_req_up = 0;
goto label_35046;
}
}
else 
{
label_35046:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35287;
}
else 
{
label_35287:; 
main_pres_req_up = 0;
goto label_35244;
}
}
else 
{
label_35244:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35485;
}
else 
{
label_35485:; 
main_dbl_req_up = 0;
goto label_35442;
}
}
else 
{
label_35442:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35683;
}
else 
{
label_35683:; 
main_zero_req_up = 0;
goto label_35640;
}
}
else 
{
label_35640:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_35853;
}
else 
{
label_35853:; 
main_clk_req_up = 0;
goto label_35827;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_35998;
}
else 
{
label_35998:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36088;
}
else 
{
label_36088:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36178;
}
else 
{
label_36178:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36268;
}
else 
{
label_36268:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36358;
}
else 
{
label_36358:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36448;
}
else 
{
label_36448:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36538;
}
else 
{
label_36538:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36628;
}
else 
{
label_36628:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36754;
}
else 
{
label_36754:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37024;
}
else 
{
label_37024:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37114;
}
else 
{
label_37114:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37204;
}
else 
{
label_37204:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37294;
}
else 
{
label_37294:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37384;
}
else 
{
label_37384:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37474;
}
else 
{
label_37474:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37564;
}
else 
{
label_37564:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37654;
}
else 
{
label_37654:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37780;
}
else 
{
label_37780:; 
}
goto label_21096;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_33777:; 
goto label_33759;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34494;
}
else 
{
label_34494:; 
main_in1_req_up = 0;
goto label_34453;
}
}
else 
{
label_34453:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34692;
}
else 
{
label_34692:; 
main_in2_req_up = 0;
goto label_34651;
}
}
else 
{
label_34651:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34890;
}
else 
{
label_34890:; 
main_sum_req_up = 0;
goto label_34849;
}
}
else 
{
label_34849:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35088;
}
else 
{
label_35088:; 
main_diff_req_up = 0;
goto label_35047;
}
}
else 
{
label_35047:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35286;
}
else 
{
label_35286:; 
main_pres_req_up = 0;
goto label_35245;
}
}
else 
{
label_35245:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35484;
}
else 
{
label_35484:; 
main_dbl_req_up = 0;
goto label_35443;
}
}
else 
{
label_35443:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35682;
}
else 
{
label_35682:; 
main_zero_req_up = 0;
goto label_35641;
}
}
else 
{
label_35641:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_35852;
}
else 
{
label_35852:; 
main_clk_req_up = 0;
goto label_35828;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_35997;
}
else 
{
label_35997:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36087;
}
else 
{
label_36087:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36177;
}
else 
{
label_36177:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36267;
}
else 
{
label_36267:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36357;
}
else 
{
label_36357:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36447;
}
else 
{
label_36447:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36537;
}
else 
{
label_36537:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36627;
}
else 
{
label_36627:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36753;
}
else 
{
label_36753:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37023;
}
else 
{
label_37023:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37113;
}
else 
{
label_37113:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37203;
}
else 
{
label_37203:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37293;
}
else 
{
label_37293:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37383;
}
else 
{
label_37383:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37473;
}
else 
{
label_37473:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37563;
}
else 
{
label_37563:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37653;
}
else 
{
label_37653:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37779;
}
else 
{
label_37779:; 
if (((int)S1_addsub_st) == 0)
{
goto label_33684;
}
else 
{
}
goto label_21097;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_32869;
}
else 
{
label_32869:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_32909;
}
else 
{
label_32909:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_32949;
}
else 
{
label_32949:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_32989;
}
else 
{
label_32989:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_33029;
}
else 
{
label_33029:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_33069;
}
else 
{
label_33069:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_33109;
}
else 
{
label_33109:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_33149;
}
else 
{
label_33149:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_33205;
}
else 
{
label_33205:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_33325;
}
else 
{
label_33325:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_33365;
}
else 
{
label_33365:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_33405;
}
else 
{
label_33405:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_33445;
}
else 
{
label_33445:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_33485;
}
else 
{
label_33485:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_33525;
}
else 
{
label_33525:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_33565;
}
else 
{
label_33565:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_33605;
}
else 
{
label_33605:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_33661;
}
else 
{
label_33661:; 
label_33686:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_33901:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_33923;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34499;
}
else 
{
label_34499:; 
main_in1_req_up = 0;
goto label_34448;
}
}
else 
{
label_34448:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34697;
}
else 
{
label_34697:; 
main_in2_req_up = 0;
goto label_34646;
}
}
else 
{
label_34646:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34895;
}
else 
{
label_34895:; 
main_sum_req_up = 0;
goto label_34844;
}
}
else 
{
label_34844:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35093;
}
else 
{
label_35093:; 
main_diff_req_up = 0;
goto label_35042;
}
}
else 
{
label_35042:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35291;
}
else 
{
label_35291:; 
main_pres_req_up = 0;
goto label_35240;
}
}
else 
{
label_35240:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35489;
}
else 
{
label_35489:; 
main_dbl_req_up = 0;
goto label_35438;
}
}
else 
{
label_35438:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35687;
}
else 
{
label_35687:; 
main_zero_req_up = 0;
goto label_35636;
}
}
else 
{
label_35636:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_35855;
}
else 
{
label_35855:; 
main_clk_req_up = 0;
goto label_35823;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_36002;
}
else 
{
label_36002:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36092;
}
else 
{
label_36092:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36182;
}
else 
{
label_36182:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36272;
}
else 
{
label_36272:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36362;
}
else 
{
label_36362:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36452;
}
else 
{
label_36452:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36542;
}
else 
{
label_36542:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36632;
}
else 
{
label_36632:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36758;
}
else 
{
label_36758:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37028;
}
else 
{
label_37028:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37118;
}
else 
{
label_37118:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37208;
}
else 
{
label_37208:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37298;
}
else 
{
label_37298:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37388;
}
else 
{
label_37388:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37478;
}
else 
{
label_37478:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37568;
}
else 
{
label_37568:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37658;
}
else 
{
label_37658:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37784;
}
else 
{
label_37784:; 
}
goto label_21092;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_33923:; 
goto label_33901;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34498;
}
else 
{
label_34498:; 
main_in1_req_up = 0;
goto label_34449;
}
}
else 
{
label_34449:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34696;
}
else 
{
label_34696:; 
main_in2_req_up = 0;
goto label_34647;
}
}
else 
{
label_34647:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34894;
}
else 
{
label_34894:; 
main_sum_req_up = 0;
goto label_34845;
}
}
else 
{
label_34845:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35092;
}
else 
{
label_35092:; 
main_diff_req_up = 0;
goto label_35043;
}
}
else 
{
label_35043:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35290;
}
else 
{
label_35290:; 
main_pres_req_up = 0;
goto label_35241;
}
}
else 
{
label_35241:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35488;
}
else 
{
label_35488:; 
main_dbl_req_up = 0;
goto label_35439;
}
}
else 
{
label_35439:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35686;
}
else 
{
label_35686:; 
main_zero_req_up = 0;
goto label_35637;
}
}
else 
{
label_35637:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_35854;
}
else 
{
label_35854:; 
main_clk_req_up = 0;
goto label_35824;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_36001;
}
else 
{
label_36001:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36091;
}
else 
{
label_36091:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36181;
}
else 
{
label_36181:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36271;
}
else 
{
label_36271:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36361;
}
else 
{
label_36361:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36451;
}
else 
{
label_36451:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36541;
}
else 
{
label_36541:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36631;
}
else 
{
label_36631:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36757;
}
else 
{
label_36757:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37027;
}
else 
{
label_37027:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37117;
}
else 
{
label_37117:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37207;
}
else 
{
label_37207:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37297;
}
else 
{
label_37297:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37387;
}
else 
{
label_37387:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37477;
}
else 
{
label_37477:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37567;
}
else 
{
label_37567:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37657;
}
else 
{
label_37657:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37783;
}
else 
{
label_37783:; 
if (((int)S3_zero_st) == 0)
{
goto label_33686;
}
else 
{
}
goto label_21093;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_32873;
}
else 
{
label_32873:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_32913;
}
else 
{
label_32913:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_32953;
}
else 
{
label_32953:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_32993;
}
else 
{
label_32993:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_33033;
}
else 
{
label_33033:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_33073;
}
else 
{
label_33073:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_33113;
}
else 
{
label_33113:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_33153;
}
else 
{
label_33153:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_33209;
}
else 
{
label_33209:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_33329;
}
else 
{
label_33329:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_33369;
}
else 
{
label_33369:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_33409;
}
else 
{
label_33409:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_33449;
}
else 
{
label_33449:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_33489;
}
else 
{
label_33489:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_33529;
}
else 
{
label_33529:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_33569;
}
else 
{
label_33569:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_33609;
}
else 
{
label_33609:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_33665;
}
else 
{
label_33665:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_34492;
}
else 
{
label_34492:; 
main_in1_req_up = 0;
goto label_34455;
}
}
else 
{
label_34455:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_34690;
}
else 
{
label_34690:; 
main_in2_req_up = 0;
goto label_34653;
}
}
else 
{
label_34653:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_34888;
}
else 
{
label_34888:; 
main_sum_req_up = 0;
goto label_34851;
}
}
else 
{
label_34851:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_35086;
}
else 
{
label_35086:; 
main_diff_req_up = 0;
goto label_35049;
}
}
else 
{
label_35049:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_35284;
}
else 
{
label_35284:; 
main_pres_req_up = 0;
goto label_35247;
}
}
else 
{
label_35247:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_35482;
}
else 
{
label_35482:; 
main_dbl_req_up = 0;
goto label_35445;
}
}
else 
{
label_35445:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_35680;
}
else 
{
label_35680:; 
main_zero_req_up = 0;
goto label_35643;
}
}
else 
{
label_35643:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_35851;
}
else 
{
label_35851:; 
main_clk_req_up = 0;
goto label_35831;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_35995;
}
else 
{
label_35995:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_36085;
}
else 
{
label_36085:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_36175;
}
else 
{
label_36175:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_36265;
}
else 
{
label_36265:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_36355;
}
else 
{
label_36355:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_36445;
}
else 
{
label_36445:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_36535;
}
else 
{
label_36535:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_36625;
}
else 
{
label_36625:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_36751;
}
else 
{
label_36751:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_37021;
}
else 
{
label_37021:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_37111;
}
else 
{
label_37111:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_37201;
}
else 
{
label_37201:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_37291;
}
else 
{
label_37291:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_37381;
}
else 
{
label_37381:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_37471;
}
else 
{
label_37471:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_37561;
}
else 
{
label_37561:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_37651;
}
else 
{
label_37651:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_37777;
}
else 
{
label_37777:; 
}
goto label_21099;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_12894;
}
else 
{
label_12894:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_12914;
}
else 
{
label_12914:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_12934;
}
else 
{
label_12934:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_12954;
}
else 
{
label_12954:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_12974;
}
else 
{
label_12974:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_12994;
}
else 
{
label_12994:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_13014;
}
else 
{
label_13014:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_13034;
}
else 
{
label_13034:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_13062;
}
else 
{
label_13062:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_13122;
}
else 
{
label_13122:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_13142;
}
else 
{
label_13142:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_13162;
}
else 
{
label_13162:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_13182;
}
else 
{
label_13182:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_13202;
}
else 
{
label_13202:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_13222;
}
else 
{
label_13222:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_13242;
}
else 
{
label_13242:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_13262;
}
else 
{
label_13262:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_13290;
}
else 
{
label_13290:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_13704;
}
else 
{
label_13704:; 
main_in1_req_up = 0;
goto label_13685;
}
}
else 
{
label_13685:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_13803;
}
else 
{
label_13803:; 
main_in2_req_up = 0;
goto label_13784;
}
}
else 
{
label_13784:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_13902;
}
else 
{
label_13902:; 
main_sum_req_up = 0;
goto label_13883;
}
}
else 
{
label_13883:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_14001;
}
else 
{
label_14001:; 
main_diff_req_up = 0;
goto label_13982;
}
}
else 
{
label_13982:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_14100;
}
else 
{
label_14100:; 
main_pres_req_up = 0;
goto label_14081;
}
}
else 
{
label_14081:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_14199;
}
else 
{
label_14199:; 
main_dbl_req_up = 0;
goto label_14180;
}
}
else 
{
label_14180:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_14298;
}
else 
{
label_14298:; 
main_zero_req_up = 0;
goto label_14279;
}
}
else 
{
label_14279:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_14397;
}
else 
{
label_14397:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_14442;
}
else 
{
label_14442:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_14487;
}
else 
{
label_14487:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_14532;
}
else 
{
label_14532:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_14577;
}
else 
{
label_14577:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_14622;
}
else 
{
label_14622:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_14667;
}
else 
{
label_14667:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_14712;
}
else 
{
label_14712:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_14775;
}
else 
{
label_14775:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_14910;
}
else 
{
label_14910:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_14955;
}
else 
{
label_14955:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_15000;
}
else 
{
label_15000:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_15045;
}
else 
{
label_15045:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_15090;
}
else 
{
label_15090:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_15135;
}
else 
{
label_15135:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_15180;
}
else 
{
label_15180:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_15225;
}
else 
{
label_15225:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_15288;
}
else 
{
label_15288:; 
}
label_15555:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
label_15591:; 
count = count + 1;
if (count == 5)
{
flag = D_z;
if (D_z == 0)
{
goto label_15717;
}
else 
{
{
__VERIFIER_error();
}
label_15717:; 
count = 0;
goto label_15635;
}
}
else 
{
label_15635:; 
main_clk_val_t = 0;
main_clk_req_up = 1;
{
int kernel_st ;
kernel_st = 0;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_15780;
}
else 
{
label_15780:; 
main_in1_req_up = 0;
goto label_15777;
}
}
else 
{
label_15777:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_15791;
}
else 
{
label_15791:; 
main_in2_req_up = 0;
goto label_15788;
}
}
else 
{
label_15788:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_15802;
}
else 
{
label_15802:; 
main_sum_req_up = 0;
goto label_15799;
}
}
else 
{
label_15799:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_15813;
}
else 
{
label_15813:; 
main_diff_req_up = 0;
goto label_15810;
}
}
else 
{
label_15810:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_15824;
}
else 
{
label_15824:; 
main_pres_req_up = 0;
goto label_15821;
}
}
else 
{
label_15821:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_15835;
}
else 
{
label_15835:; 
main_dbl_req_up = 0;
goto label_15832;
}
}
else 
{
label_15832:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_15846;
}
else 
{
label_15846:; 
main_zero_req_up = 0;
goto label_15843;
}
}
else 
{
label_15843:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_15857;
}
else 
{
label_15857:; 
main_clk_req_up = 0;
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_15961;
}
else 
{
label_15961:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_16001;
}
else 
{
label_16001:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_16041;
}
else 
{
label_16041:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_16081;
}
else 
{
label_16081:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_16121;
}
else 
{
label_16121:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_16161;
}
else 
{
label_16161:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_16201;
}
else 
{
label_16201:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_16241;
}
else 
{
label_16241:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_16297;
}
else 
{
label_16297:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_16417;
}
else 
{
label_16417:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_16457;
}
else 
{
label_16457:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_16497;
}
else 
{
label_16497:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_16537;
}
else 
{
label_16537:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_16577;
}
else 
{
label_16577:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_16617;
}
else 
{
label_16617:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_16657;
}
else 
{
label_16657:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_16697;
}
else 
{
label_16697:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_16753;
}
else 
{
label_16753:; 
label_16784:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_17306:; 
if (((int)S1_addsub_st) == 0)
{
goto label_17320;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_17320:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_17326;
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
label_17346:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_17356;
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
label_17391:; 
}
label_17459:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17603;
}
else 
{
label_17603:; 
main_in1_req_up = 0;
goto label_17534;
}
}
else 
{
label_17534:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17801;
}
else 
{
label_17801:; 
main_in2_req_up = 0;
goto label_17732;
}
}
else 
{
label_17732:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17999;
}
else 
{
label_17999:; 
main_sum_req_up = 0;
goto label_17930;
}
}
else 
{
label_17930:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18197;
}
else 
{
label_18197:; 
main_diff_req_up = 0;
goto label_18128;
}
}
else 
{
label_18128:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18395;
}
else 
{
label_18395:; 
main_pres_req_up = 0;
goto label_18326;
}
}
else 
{
label_18326:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18593;
}
else 
{
label_18593:; 
main_dbl_req_up = 0;
goto label_18524;
}
}
else 
{
label_18524:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18791;
}
else 
{
label_18791:; 
main_zero_req_up = 0;
goto label_18722;
}
}
else 
{
label_18722:; 
label_18911:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19106;
}
else 
{
label_19106:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19196;
}
else 
{
label_19196:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19286;
}
else 
{
label_19286:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19376;
}
else 
{
label_19376:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19466;
}
else 
{
label_19466:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19556;
}
else 
{
label_19556:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19646;
}
else 
{
label_19646:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19736;
}
else 
{
label_19736:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19862;
}
else 
{
label_19862:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20132;
}
else 
{
label_20132:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20222;
}
else 
{
label_20222:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20312;
}
else 
{
label_20312:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20402;
}
else 
{
label_20402:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20492;
}
else 
{
label_20492:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20582;
}
else 
{
label_20582:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20672;
}
else 
{
label_20672:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20762;
}
else 
{
label_20762:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20888;
}
else 
{
label_20888:; 
}
label_21083:; 
goto label_15562;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_17356:; 
if (((int)S3_zero_st) == 0)
{
goto label_17346;
}
else 
{
}
label_17472:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17604;
}
else 
{
label_17604:; 
main_in1_req_up = 0;
goto label_17533;
}
}
else 
{
label_17533:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17802;
}
else 
{
label_17802:; 
main_in2_req_up = 0;
goto label_17731;
}
}
else 
{
label_17731:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_18000;
}
else 
{
label_18000:; 
main_sum_req_up = 0;
goto label_17929;
}
}
else 
{
label_17929:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18198;
}
else 
{
label_18198:; 
main_diff_req_up = 0;
goto label_18127;
}
}
else 
{
label_18127:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18396;
}
else 
{
label_18396:; 
main_pres_req_up = 0;
goto label_18325;
}
}
else 
{
label_18325:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18594;
}
else 
{
label_18594:; 
main_dbl_req_up = 0;
goto label_18523;
}
}
else 
{
label_18523:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18792;
}
else 
{
label_18792:; 
main_zero_req_up = 0;
goto label_18721;
}
}
else 
{
label_18721:; 
label_18910:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19107;
}
else 
{
label_19107:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19197;
}
else 
{
label_19197:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19287;
}
else 
{
label_19287:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19377;
}
else 
{
label_19377:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19467;
}
else 
{
label_19467:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19557;
}
else 
{
label_19557:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19647;
}
else 
{
label_19647:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19737;
}
else 
{
label_19737:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19863;
}
else 
{
label_19863:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20133;
}
else 
{
label_20133:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20223;
}
else 
{
label_20223:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20313;
}
else 
{
label_20313:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20403;
}
else 
{
label_20403:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20493;
}
else 
{
label_20493:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20583;
}
else 
{
label_20583:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20673;
}
else 
{
label_20673:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20763;
}
else 
{
label_20763:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20889;
}
else 
{
label_20889:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_21348:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_21370;
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
}
goto label_17459;
}
else 
{
label_21370:; 
goto label_21348;
}
}
else 
{
}
goto label_17472;
}
}
else 
{
}
label_21082:; 
goto label_15563;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_17326:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_17355;
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
label_17392:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_17419;
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
goto label_17391;
}
}
else 
{
label_17419:; 
goto label_17392;
}
}
else 
{
}
label_17413:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17602;
}
else 
{
label_17602:; 
main_in1_req_up = 0;
goto label_17535;
}
}
else 
{
label_17535:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17800;
}
else 
{
label_17800:; 
main_in2_req_up = 0;
goto label_17733;
}
}
else 
{
label_17733:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17998;
}
else 
{
label_17998:; 
main_sum_req_up = 0;
goto label_17931;
}
}
else 
{
label_17931:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18196;
}
else 
{
label_18196:; 
main_diff_req_up = 0;
goto label_18129;
}
}
else 
{
label_18129:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18394;
}
else 
{
label_18394:; 
main_pres_req_up = 0;
goto label_18327;
}
}
else 
{
label_18327:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18592;
}
else 
{
label_18592:; 
main_dbl_req_up = 0;
goto label_18525;
}
}
else 
{
label_18525:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18790;
}
else 
{
label_18790:; 
main_zero_req_up = 0;
goto label_18723;
}
}
else 
{
label_18723:; 
label_18912:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19105;
}
else 
{
label_19105:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19195;
}
else 
{
label_19195:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19285;
}
else 
{
label_19285:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19375;
}
else 
{
label_19375:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19465;
}
else 
{
label_19465:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19555;
}
else 
{
label_19555:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19645;
}
else 
{
label_19645:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19735;
}
else 
{
label_19735:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19861;
}
else 
{
label_19861:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20131;
}
else 
{
label_20131:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20221;
}
else 
{
label_20221:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20311;
}
else 
{
label_20311:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20401;
}
else 
{
label_20401:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20491;
}
else 
{
label_20491:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20581;
}
else 
{
label_20581:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20671;
}
else 
{
label_20671:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20761;
}
else 
{
label_20761:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20887;
}
else 
{
label_20887:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_21200:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_21218;
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
}
goto label_17459;
}
else 
{
label_21218:; 
goto label_21200;
}
}
else 
{
}
goto label_17413;
}
}
else 
{
}
label_21084:; 
goto label_15561;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_17355:; 
goto label_17306;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17601;
}
else 
{
label_17601:; 
main_in1_req_up = 0;
goto label_17536;
}
}
else 
{
label_17536:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17799;
}
else 
{
label_17799:; 
main_in2_req_up = 0;
goto label_17734;
}
}
else 
{
label_17734:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17997;
}
else 
{
label_17997:; 
main_sum_req_up = 0;
goto label_17932;
}
}
else 
{
label_17932:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18195;
}
else 
{
label_18195:; 
main_diff_req_up = 0;
goto label_18130;
}
}
else 
{
label_18130:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18393;
}
else 
{
label_18393:; 
main_pres_req_up = 0;
goto label_18328;
}
}
else 
{
label_18328:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18591;
}
else 
{
label_18591:; 
main_dbl_req_up = 0;
goto label_18526;
}
}
else 
{
label_18526:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18789;
}
else 
{
label_18789:; 
main_zero_req_up = 0;
goto label_18724;
}
}
else 
{
label_18724:; 
label_18913:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19104;
}
else 
{
label_19104:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19194;
}
else 
{
label_19194:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19284;
}
else 
{
label_19284:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19374;
}
else 
{
label_19374:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19464;
}
else 
{
label_19464:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19554;
}
else 
{
label_19554:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19644;
}
else 
{
label_19644:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19734;
}
else 
{
label_19734:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19860;
}
else 
{
label_19860:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20130;
}
else 
{
label_20130:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20220;
}
else 
{
label_20220:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20310;
}
else 
{
label_20310:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20400;
}
else 
{
label_20400:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20490;
}
else 
{
label_20490:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20580;
}
else 
{
label_20580:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20670;
}
else 
{
label_20670:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20760;
}
else 
{
label_20760:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20886;
}
else 
{
label_20886:; 
if (((int)S1_addsub_st) == 0)
{
goto label_21101;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_21101:; 
goto label_16784;
}
else 
{
}
label_21085:; 
goto label_15560;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_15965;
}
else 
{
label_15965:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_16005;
}
else 
{
label_16005:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_16045;
}
else 
{
label_16045:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_16085;
}
else 
{
label_16085:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_16125;
}
else 
{
label_16125:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_16165;
}
else 
{
label_16165:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_16205;
}
else 
{
label_16205:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_16245;
}
else 
{
label_16245:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_16301;
}
else 
{
label_16301:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_16421;
}
else 
{
label_16421:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_16461;
}
else 
{
label_16461:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_16501;
}
else 
{
label_16501:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_16541;
}
else 
{
label_16541:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_16581;
}
else 
{
label_16581:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_16621;
}
else 
{
label_16621:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_16661;
}
else 
{
label_16661:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_16701;
}
else 
{
label_16701:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_16757;
}
else 
{
label_16757:; 
label_16780:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_16925:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_16943;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17592;
}
else 
{
label_17592:; 
main_in1_req_up = 0;
goto label_17545;
}
}
else 
{
label_17545:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17790;
}
else 
{
label_17790:; 
main_in2_req_up = 0;
goto label_17743;
}
}
else 
{
label_17743:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17988;
}
else 
{
label_17988:; 
main_sum_req_up = 0;
goto label_17941;
}
}
else 
{
label_17941:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18186;
}
else 
{
label_18186:; 
main_diff_req_up = 0;
goto label_18139;
}
}
else 
{
label_18139:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18384;
}
else 
{
label_18384:; 
main_pres_req_up = 0;
goto label_18337;
}
}
else 
{
label_18337:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18582;
}
else 
{
label_18582:; 
main_dbl_req_up = 0;
goto label_18535;
}
}
else 
{
label_18535:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18780;
}
else 
{
label_18780:; 
main_zero_req_up = 0;
goto label_18733;
}
}
else 
{
label_18733:; 
label_18922:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19095;
}
else 
{
label_19095:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19185;
}
else 
{
label_19185:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19275;
}
else 
{
label_19275:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19365;
}
else 
{
label_19365:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19455;
}
else 
{
label_19455:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19545;
}
else 
{
label_19545:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19635;
}
else 
{
label_19635:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19725;
}
else 
{
label_19725:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19851;
}
else 
{
label_19851:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20121;
}
else 
{
label_20121:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20211;
}
else 
{
label_20211:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20301;
}
else 
{
label_20301:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20391;
}
else 
{
label_20391:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20481;
}
else 
{
label_20481:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20571;
}
else 
{
label_20571:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20661;
}
else 
{
label_20661:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20751;
}
else 
{
label_20751:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20877;
}
else 
{
label_20877:; 
}
label_21094:; 
goto label_15557;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_16943:; 
goto label_16925;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17591;
}
else 
{
label_17591:; 
main_in1_req_up = 0;
goto label_17546;
}
}
else 
{
label_17546:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17789;
}
else 
{
label_17789:; 
main_in2_req_up = 0;
goto label_17744;
}
}
else 
{
label_17744:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17987;
}
else 
{
label_17987:; 
main_sum_req_up = 0;
goto label_17942;
}
}
else 
{
label_17942:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18185;
}
else 
{
label_18185:; 
main_diff_req_up = 0;
goto label_18140;
}
}
else 
{
label_18140:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18383;
}
else 
{
label_18383:; 
main_pres_req_up = 0;
goto label_18338;
}
}
else 
{
label_18338:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18581;
}
else 
{
label_18581:; 
main_dbl_req_up = 0;
goto label_18536;
}
}
else 
{
label_18536:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18779;
}
else 
{
label_18779:; 
main_zero_req_up = 0;
goto label_18734;
}
}
else 
{
label_18734:; 
label_18923:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19094;
}
else 
{
label_19094:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19184;
}
else 
{
label_19184:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19274;
}
else 
{
label_19274:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19364;
}
else 
{
label_19364:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19454;
}
else 
{
label_19454:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19544;
}
else 
{
label_19544:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19634;
}
else 
{
label_19634:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19724;
}
else 
{
label_19724:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19850;
}
else 
{
label_19850:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20120;
}
else 
{
label_20120:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20210;
}
else 
{
label_20210:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20300;
}
else 
{
label_20300:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20390;
}
else 
{
label_20390:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20480;
}
else 
{
label_20480:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20570;
}
else 
{
label_20570:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20660;
}
else 
{
label_20660:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20750;
}
else 
{
label_20750:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20876;
}
else 
{
label_20876:; 
if (((int)S1_addsub_st) == 0)
{
goto label_16780;
}
else 
{
}
label_21095:; 
goto label_15556;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_15963;
}
else 
{
label_15963:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_16003;
}
else 
{
label_16003:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_16043;
}
else 
{
label_16043:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_16083;
}
else 
{
label_16083:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_16123;
}
else 
{
label_16123:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_16163;
}
else 
{
label_16163:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_16203;
}
else 
{
label_16203:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_16243;
}
else 
{
label_16243:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_16299;
}
else 
{
label_16299:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_16419;
}
else 
{
label_16419:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_16459;
}
else 
{
label_16459:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_16499;
}
else 
{
label_16499:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_16539;
}
else 
{
label_16539:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_16579;
}
else 
{
label_16579:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_16619;
}
else 
{
label_16619:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_16659;
}
else 
{
label_16659:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_16699;
}
else 
{
label_16699:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_16755;
}
else 
{
label_16755:; 
label_16782:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_17061:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_17083;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17596;
}
else 
{
label_17596:; 
main_in1_req_up = 0;
goto label_17541;
}
}
else 
{
label_17541:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17794;
}
else 
{
label_17794:; 
main_in2_req_up = 0;
goto label_17739;
}
}
else 
{
label_17739:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17992;
}
else 
{
label_17992:; 
main_sum_req_up = 0;
goto label_17937;
}
}
else 
{
label_17937:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18190;
}
else 
{
label_18190:; 
main_diff_req_up = 0;
goto label_18135;
}
}
else 
{
label_18135:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18388;
}
else 
{
label_18388:; 
main_pres_req_up = 0;
goto label_18333;
}
}
else 
{
label_18333:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18586;
}
else 
{
label_18586:; 
main_dbl_req_up = 0;
goto label_18531;
}
}
else 
{
label_18531:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18784;
}
else 
{
label_18784:; 
main_zero_req_up = 0;
goto label_18729;
}
}
else 
{
label_18729:; 
label_18918:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19099;
}
else 
{
label_19099:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19189;
}
else 
{
label_19189:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19279;
}
else 
{
label_19279:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19369;
}
else 
{
label_19369:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19459;
}
else 
{
label_19459:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19549;
}
else 
{
label_19549:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19639;
}
else 
{
label_19639:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19729;
}
else 
{
label_19729:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19855;
}
else 
{
label_19855:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20125;
}
else 
{
label_20125:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20215;
}
else 
{
label_20215:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20305;
}
else 
{
label_20305:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20395;
}
else 
{
label_20395:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20485;
}
else 
{
label_20485:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20575;
}
else 
{
label_20575:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20665;
}
else 
{
label_20665:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20755;
}
else 
{
label_20755:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20881;
}
else 
{
label_20881:; 
}
label_21090:; 
goto label_15559;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_17083:; 
goto label_17061;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17595;
}
else 
{
label_17595:; 
main_in1_req_up = 0;
goto label_17542;
}
}
else 
{
label_17542:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17793;
}
else 
{
label_17793:; 
main_in2_req_up = 0;
goto label_17740;
}
}
else 
{
label_17740:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17991;
}
else 
{
label_17991:; 
main_sum_req_up = 0;
goto label_17938;
}
}
else 
{
label_17938:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18189;
}
else 
{
label_18189:; 
main_diff_req_up = 0;
goto label_18136;
}
}
else 
{
label_18136:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18387;
}
else 
{
label_18387:; 
main_pres_req_up = 0;
goto label_18334;
}
}
else 
{
label_18334:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18585;
}
else 
{
label_18585:; 
main_dbl_req_up = 0;
goto label_18532;
}
}
else 
{
label_18532:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18783;
}
else 
{
label_18783:; 
main_zero_req_up = 0;
goto label_18730;
}
}
else 
{
label_18730:; 
label_18919:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19098;
}
else 
{
label_19098:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19188;
}
else 
{
label_19188:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19278;
}
else 
{
label_19278:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19368;
}
else 
{
label_19368:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19458;
}
else 
{
label_19458:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19548;
}
else 
{
label_19548:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19638;
}
else 
{
label_19638:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19728;
}
else 
{
label_19728:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19854;
}
else 
{
label_19854:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20124;
}
else 
{
label_20124:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20214;
}
else 
{
label_20214:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20304;
}
else 
{
label_20304:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20394;
}
else 
{
label_20394:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20484;
}
else 
{
label_20484:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20574;
}
else 
{
label_20574:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20664;
}
else 
{
label_20664:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20754;
}
else 
{
label_20754:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20880;
}
else 
{
label_20880:; 
if (((int)S3_zero_st) == 0)
{
goto label_16782;
}
else 
{
}
label_21091:; 
goto label_15558;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_15967;
}
else 
{
label_15967:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_16007;
}
else 
{
label_16007:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_16047;
}
else 
{
label_16047:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_16087;
}
else 
{
label_16087:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_16127;
}
else 
{
label_16127:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_16167;
}
else 
{
label_16167:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_16207;
}
else 
{
label_16207:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_16247;
}
else 
{
label_16247:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_16303;
}
else 
{
label_16303:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_16423;
}
else 
{
label_16423:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_16463;
}
else 
{
label_16463:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_16503;
}
else 
{
label_16503:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_16543;
}
else 
{
label_16543:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_16583;
}
else 
{
label_16583:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_16623;
}
else 
{
label_16623:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_16663;
}
else 
{
label_16663:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_16703;
}
else 
{
label_16703:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_16759;
}
else 
{
label_16759:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17588;
}
else 
{
label_17588:; 
main_in1_req_up = 0;
goto label_17549;
}
}
else 
{
label_17549:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17786;
}
else 
{
label_17786:; 
main_in2_req_up = 0;
goto label_17747;
}
}
else 
{
label_17747:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17984;
}
else 
{
label_17984:; 
main_sum_req_up = 0;
goto label_17945;
}
}
else 
{
label_17945:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18182;
}
else 
{
label_18182:; 
main_diff_req_up = 0;
goto label_18143;
}
}
else 
{
label_18143:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18380;
}
else 
{
label_18380:; 
main_pres_req_up = 0;
goto label_18341;
}
}
else 
{
label_18341:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18578;
}
else 
{
label_18578:; 
main_dbl_req_up = 0;
goto label_18539;
}
}
else 
{
label_18539:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18776;
}
else 
{
label_18776:; 
main_zero_req_up = 0;
goto label_18737;
}
}
else 
{
label_18737:; 
label_18926:; 
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19091;
}
else 
{
label_19091:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19181;
}
else 
{
label_19181:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19271;
}
else 
{
label_19271:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19361;
}
else 
{
label_19361:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19451;
}
else 
{
label_19451:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19541;
}
else 
{
label_19541:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19631;
}
else 
{
label_19631:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19721;
}
else 
{
label_19721:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19847;
}
else 
{
label_19847:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20117;
}
else 
{
label_20117:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20207;
}
else 
{
label_20207:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20297;
}
else 
{
label_20297:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20387;
}
else 
{
label_20387:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20477;
}
else 
{
label_20477:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20567;
}
else 
{
label_20567:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20657;
}
else 
{
label_20657:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20747;
}
else 
{
label_20747:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20873;
}
else 
{
label_20873:; 
}
label_21098:; 
goto label_15555;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
N_generate_st = 2;
if (((int)S1_addsub_i) == 1)
{
S1_addsub_st = 0;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_15962;
}
else 
{
label_15962:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_16002;
}
else 
{
label_16002:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_16042;
}
else 
{
label_16042:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_16082;
}
else 
{
label_16082:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_16122;
}
else 
{
label_16122:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_16162;
}
else 
{
label_16162:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_16202;
}
else 
{
label_16202:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_16242;
}
else 
{
label_16242:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_16298;
}
else 
{
label_16298:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_16418;
}
else 
{
label_16418:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_16458;
}
else 
{
label_16458:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_16498;
}
else 
{
label_16498:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_16538;
}
else 
{
label_16538:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_16578;
}
else 
{
label_16578:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_16618;
}
else 
{
label_16618:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_16658;
}
else 
{
label_16658:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_16698;
}
else 
{
label_16698:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_16754;
}
else 
{
label_16754:; 
label_16783:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_17126:; 
if (((int)S1_addsub_st) == 0)
{
goto label_17140;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_17140:; 
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_17146;
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
label_17166:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_17176;
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
label_17211:; 
}
label_17279:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17599;
}
else 
{
label_17599:; 
main_in1_req_up = 0;
goto label_17538;
}
}
else 
{
label_17538:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17797;
}
else 
{
label_17797:; 
main_in2_req_up = 0;
goto label_17736;
}
}
else 
{
label_17736:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17995;
}
else 
{
label_17995:; 
main_sum_req_up = 0;
goto label_17934;
}
}
else 
{
label_17934:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18193;
}
else 
{
label_18193:; 
main_diff_req_up = 0;
goto label_18132;
}
}
else 
{
label_18132:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18391;
}
else 
{
label_18391:; 
main_pres_req_up = 0;
goto label_18330;
}
}
else 
{
label_18330:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18589;
}
else 
{
label_18589:; 
main_dbl_req_up = 0;
goto label_18528;
}
}
else 
{
label_18528:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18787;
}
else 
{
label_18787:; 
main_zero_req_up = 0;
goto label_18726;
}
}
else 
{
label_18726:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_18953;
}
else 
{
label_18953:; 
main_clk_req_up = 0;
goto label_18911;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19102;
}
else 
{
label_19102:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19192;
}
else 
{
label_19192:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19282;
}
else 
{
label_19282:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19372;
}
else 
{
label_19372:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19462;
}
else 
{
label_19462:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19552;
}
else 
{
label_19552:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19642;
}
else 
{
label_19642:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19732;
}
else 
{
label_19732:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19858;
}
else 
{
label_19858:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20128;
}
else 
{
label_20128:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20218;
}
else 
{
label_20218:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20308;
}
else 
{
label_20308:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20398;
}
else 
{
label_20398:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20488;
}
else 
{
label_20488:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20578;
}
else 
{
label_20578:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20668;
}
else 
{
label_20668:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20758;
}
else 
{
label_20758:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20884;
}
else 
{
label_20884:; 
}
label_21087:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
goto label_15598;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_17176:; 
if (((int)S3_zero_st) == 0)
{
goto label_17166;
}
else 
{
}
label_17292:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17600;
}
else 
{
label_17600:; 
main_in1_req_up = 0;
goto label_17537;
}
}
else 
{
label_17537:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17798;
}
else 
{
label_17798:; 
main_in2_req_up = 0;
goto label_17735;
}
}
else 
{
label_17735:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17996;
}
else 
{
label_17996:; 
main_sum_req_up = 0;
goto label_17933;
}
}
else 
{
label_17933:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18194;
}
else 
{
label_18194:; 
main_diff_req_up = 0;
goto label_18131;
}
}
else 
{
label_18131:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18392;
}
else 
{
label_18392:; 
main_pres_req_up = 0;
goto label_18329;
}
}
else 
{
label_18329:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18590;
}
else 
{
label_18590:; 
main_dbl_req_up = 0;
goto label_18527;
}
}
else 
{
label_18527:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18788;
}
else 
{
label_18788:; 
main_zero_req_up = 0;
goto label_18725;
}
}
else 
{
label_18725:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_18954;
}
else 
{
label_18954:; 
main_clk_req_up = 0;
goto label_18910;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19103;
}
else 
{
label_19103:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19193;
}
else 
{
label_19193:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19283;
}
else 
{
label_19283:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19373;
}
else 
{
label_19373:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19463;
}
else 
{
label_19463:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19553;
}
else 
{
label_19553:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19643;
}
else 
{
label_19643:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19733;
}
else 
{
label_19733:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19859;
}
else 
{
label_19859:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20129;
}
else 
{
label_20129:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20219;
}
else 
{
label_20219:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20309;
}
else 
{
label_20309:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20399;
}
else 
{
label_20399:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20489;
}
else 
{
label_20489:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20579;
}
else 
{
label_20579:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20669;
}
else 
{
label_20669:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20759;
}
else 
{
label_20759:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20885;
}
else 
{
label_20885:; 
if (((int)S3_zero_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_21278:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_21300;
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
}
goto label_17279;
}
else 
{
label_21300:; 
goto label_21278;
}
}
else 
{
}
goto label_17292;
}
}
else 
{
}
label_21086:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
goto label_15599;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_17146:; 
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_17175;
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
label_17212:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_17239;
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
goto label_17211;
}
}
else 
{
label_17239:; 
goto label_17212;
}
}
else 
{
}
label_17233:; 
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17598;
}
else 
{
label_17598:; 
main_in1_req_up = 0;
goto label_17539;
}
}
else 
{
label_17539:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17796;
}
else 
{
label_17796:; 
main_in2_req_up = 0;
goto label_17737;
}
}
else 
{
label_17737:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17994;
}
else 
{
label_17994:; 
main_sum_req_up = 0;
goto label_17935;
}
}
else 
{
label_17935:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18192;
}
else 
{
label_18192:; 
main_diff_req_up = 0;
goto label_18133;
}
}
else 
{
label_18133:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18390;
}
else 
{
label_18390:; 
main_pres_req_up = 0;
goto label_18331;
}
}
else 
{
label_18331:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18588;
}
else 
{
label_18588:; 
main_dbl_req_up = 0;
goto label_18529;
}
}
else 
{
label_18529:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18786;
}
else 
{
label_18786:; 
main_zero_req_up = 0;
goto label_18727;
}
}
else 
{
label_18727:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_18952;
}
else 
{
label_18952:; 
main_clk_req_up = 0;
goto label_18912;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19101;
}
else 
{
label_19101:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19191;
}
else 
{
label_19191:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19281;
}
else 
{
label_19281:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19371;
}
else 
{
label_19371:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19461;
}
else 
{
label_19461:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19551;
}
else 
{
label_19551:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19641;
}
else 
{
label_19641:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19731;
}
else 
{
label_19731:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19857;
}
else 
{
label_19857:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20127;
}
else 
{
label_20127:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20217;
}
else 
{
label_20217:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20307;
}
else 
{
label_20307:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20397;
}
else 
{
label_20397:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20487;
}
else 
{
label_20487:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20577;
}
else 
{
label_20577:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20667;
}
else 
{
label_20667:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20757;
}
else 
{
label_20757:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20883;
}
else 
{
label_20883:; 
if (((int)S1_addsub_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_21125:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_21143;
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
}
goto label_17279;
}
else 
{
label_21143:; 
goto label_21125;
}
}
else 
{
}
goto label_17233;
}
}
else 
{
}
label_21088:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
goto label_15597;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_17175:; 
goto label_17126;
}
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17597;
}
else 
{
label_17597:; 
main_in1_req_up = 0;
goto label_17540;
}
}
else 
{
label_17540:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17795;
}
else 
{
label_17795:; 
main_in2_req_up = 0;
goto label_17738;
}
}
else 
{
label_17738:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17993;
}
else 
{
label_17993:; 
main_sum_req_up = 0;
goto label_17936;
}
}
else 
{
label_17936:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18191;
}
else 
{
label_18191:; 
main_diff_req_up = 0;
goto label_18134;
}
}
else 
{
label_18134:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18389;
}
else 
{
label_18389:; 
main_pres_req_up = 0;
goto label_18332;
}
}
else 
{
label_18332:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18587;
}
else 
{
label_18587:; 
main_dbl_req_up = 0;
goto label_18530;
}
}
else 
{
label_18530:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18785;
}
else 
{
label_18785:; 
main_zero_req_up = 0;
goto label_18728;
}
}
else 
{
label_18728:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_18951;
}
else 
{
label_18951:; 
main_clk_req_up = 0;
goto label_18913;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19100;
}
else 
{
label_19100:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19190;
}
else 
{
label_19190:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19280;
}
else 
{
label_19280:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19370;
}
else 
{
label_19370:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19460;
}
else 
{
label_19460:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19550;
}
else 
{
label_19550:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19640;
}
else 
{
label_19640:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19730;
}
else 
{
label_19730:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19856;
}
else 
{
label_19856:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20126;
}
else 
{
label_20126:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20216;
}
else 
{
label_20216:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20306;
}
else 
{
label_20306:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20396;
}
else 
{
label_20396:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20486;
}
else 
{
label_20486:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20576;
}
else 
{
label_20576:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20666;
}
else 
{
label_20666:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20756;
}
else 
{
label_20756:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20882;
}
else 
{
label_20882:; 
if (((int)S1_addsub_st) == 0)
{
goto label_21103;
}
else 
{
if (((int)S3_zero_st) == 0)
{
label_21103:; 
goto label_16783;
}
else 
{
}
label_21089:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
goto label_15596;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_15966;
}
else 
{
label_15966:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_16006;
}
else 
{
label_16006:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_16046;
}
else 
{
label_16046:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_16086;
}
else 
{
label_16086:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_16126;
}
else 
{
label_16126:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_16166;
}
else 
{
label_16166:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_16206;
}
else 
{
label_16206:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_16246;
}
else 
{
label_16246:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_16302;
}
else 
{
label_16302:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_16422;
}
else 
{
label_16422:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_16462;
}
else 
{
label_16462:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_16502;
}
else 
{
label_16502:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_16542;
}
else 
{
label_16542:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_16582;
}
else 
{
label_16582:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_16622;
}
else 
{
label_16622:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_16662;
}
else 
{
label_16662:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_16702;
}
else 
{
label_16702:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_16758;
}
else 
{
label_16758:; 
label_16779:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_16854:; 
if (((int)S1_addsub_st) == 0)
{
if (((int)S1_addsub_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_16872;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17590;
}
else 
{
label_17590:; 
main_in1_req_up = 0;
goto label_17547;
}
}
else 
{
label_17547:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17788;
}
else 
{
label_17788:; 
main_in2_req_up = 0;
goto label_17745;
}
}
else 
{
label_17745:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17986;
}
else 
{
label_17986:; 
main_sum_req_up = 0;
goto label_17943;
}
}
else 
{
label_17943:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18184;
}
else 
{
label_18184:; 
main_diff_req_up = 0;
goto label_18141;
}
}
else 
{
label_18141:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18382;
}
else 
{
label_18382:; 
main_pres_req_up = 0;
goto label_18339;
}
}
else 
{
label_18339:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18580;
}
else 
{
label_18580:; 
main_dbl_req_up = 0;
goto label_18537;
}
}
else 
{
label_18537:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18778;
}
else 
{
label_18778:; 
main_zero_req_up = 0;
goto label_18735;
}
}
else 
{
label_18735:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_18948;
}
else 
{
label_18948:; 
main_clk_req_up = 0;
goto label_18922;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19093;
}
else 
{
label_19093:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19183;
}
else 
{
label_19183:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19273;
}
else 
{
label_19273:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19363;
}
else 
{
label_19363:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19453;
}
else 
{
label_19453:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19543;
}
else 
{
label_19543:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19633;
}
else 
{
label_19633:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19723;
}
else 
{
label_19723:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19849;
}
else 
{
label_19849:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20119;
}
else 
{
label_20119:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20209;
}
else 
{
label_20209:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20299;
}
else 
{
label_20299:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20389;
}
else 
{
label_20389:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20479;
}
else 
{
label_20479:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20569;
}
else 
{
label_20569:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20659;
}
else 
{
label_20659:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20749;
}
else 
{
label_20749:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20875;
}
else 
{
label_20875:; 
}
label_21096:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
goto label_15593;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_16872:; 
goto label_16854;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17589;
}
else 
{
label_17589:; 
main_in1_req_up = 0;
goto label_17548;
}
}
else 
{
label_17548:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17787;
}
else 
{
label_17787:; 
main_in2_req_up = 0;
goto label_17746;
}
}
else 
{
label_17746:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17985;
}
else 
{
label_17985:; 
main_sum_req_up = 0;
goto label_17944;
}
}
else 
{
label_17944:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18183;
}
else 
{
label_18183:; 
main_diff_req_up = 0;
goto label_18142;
}
}
else 
{
label_18142:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18381;
}
else 
{
label_18381:; 
main_pres_req_up = 0;
goto label_18340;
}
}
else 
{
label_18340:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18579;
}
else 
{
label_18579:; 
main_dbl_req_up = 0;
goto label_18538;
}
}
else 
{
label_18538:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18777;
}
else 
{
label_18777:; 
main_zero_req_up = 0;
goto label_18736;
}
}
else 
{
label_18736:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_18947;
}
else 
{
label_18947:; 
main_clk_req_up = 0;
goto label_18923;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19092;
}
else 
{
label_19092:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19182;
}
else 
{
label_19182:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19272;
}
else 
{
label_19272:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19362;
}
else 
{
label_19362:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19452;
}
else 
{
label_19452:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19542;
}
else 
{
label_19542:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19632;
}
else 
{
label_19632:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19722;
}
else 
{
label_19722:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19848;
}
else 
{
label_19848:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20118;
}
else 
{
label_20118:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20208;
}
else 
{
label_20208:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20298;
}
else 
{
label_20298:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20388;
}
else 
{
label_20388:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20478;
}
else 
{
label_20478:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20568;
}
else 
{
label_20568:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20658;
}
else 
{
label_20658:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20748;
}
else 
{
label_20748:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20874;
}
else 
{
label_20874:; 
if (((int)S1_addsub_st) == 0)
{
goto label_16779;
}
else 
{
}
label_21097:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
goto label_15592;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S1_addsub_st = 2;
S2_presdbl_st = 2;
if (((int)S3_zero_i) == 1)
{
S3_zero_st = 0;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_15964;
}
else 
{
label_15964:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_16004;
}
else 
{
label_16004:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_16044;
}
else 
{
label_16044:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_16084;
}
else 
{
label_16084:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_16124;
}
else 
{
label_16124:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_16164;
}
else 
{
label_16164:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_16204;
}
else 
{
label_16204:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_16244;
}
else 
{
label_16244:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_16300;
}
else 
{
label_16300:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_16420;
}
else 
{
label_16420:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_16460;
}
else 
{
label_16460:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_16500;
}
else 
{
label_16500:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_16540;
}
else 
{
label_16540:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_16580;
}
else 
{
label_16580:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_16620;
}
else 
{
label_16620:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_16660;
}
else 
{
label_16660:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_16700;
}
else 
{
label_16700:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_16756;
}
else 
{
label_16756:; 
label_16781:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
label_16996:; 
if (((int)S3_zero_st) == 0)
{
if (((int)S3_zero_st) == 0)
{
tmp___2 = __VERIFIER_nondet_int();
if (tmp___2 == 0)
{
goto label_17018;
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
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17594;
}
else 
{
label_17594:; 
main_in1_req_up = 0;
goto label_17543;
}
}
else 
{
label_17543:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17792;
}
else 
{
label_17792:; 
main_in2_req_up = 0;
goto label_17741;
}
}
else 
{
label_17741:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17990;
}
else 
{
label_17990:; 
main_sum_req_up = 0;
goto label_17939;
}
}
else 
{
label_17939:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18188;
}
else 
{
label_18188:; 
main_diff_req_up = 0;
goto label_18137;
}
}
else 
{
label_18137:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18386;
}
else 
{
label_18386:; 
main_pres_req_up = 0;
goto label_18335;
}
}
else 
{
label_18335:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18584;
}
else 
{
label_18584:; 
main_dbl_req_up = 0;
goto label_18533;
}
}
else 
{
label_18533:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18782;
}
else 
{
label_18782:; 
main_zero_req_up = 0;
goto label_18731;
}
}
else 
{
label_18731:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_18950;
}
else 
{
label_18950:; 
main_clk_req_up = 0;
goto label_18918;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19097;
}
else 
{
label_19097:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19187;
}
else 
{
label_19187:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19277;
}
else 
{
label_19277:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19367;
}
else 
{
label_19367:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19457;
}
else 
{
label_19457:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19547;
}
else 
{
label_19547:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19637;
}
else 
{
label_19637:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19727;
}
else 
{
label_19727:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19853;
}
else 
{
label_19853:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20123;
}
else 
{
label_20123:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20213;
}
else 
{
label_20213:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20303;
}
else 
{
label_20303:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20393;
}
else 
{
label_20393:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20483;
}
else 
{
label_20483:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20573;
}
else 
{
label_20573:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20663;
}
else 
{
label_20663:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20753;
}
else 
{
label_20753:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20879;
}
else 
{
label_20879:; 
}
label_21092:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
goto label_15595;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
label_17018:; 
goto label_16996;
}
}
else 
{
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17593;
}
else 
{
label_17593:; 
main_in1_req_up = 0;
goto label_17544;
}
}
else 
{
label_17544:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17791;
}
else 
{
label_17791:; 
main_in2_req_up = 0;
goto label_17742;
}
}
else 
{
label_17742:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17989;
}
else 
{
label_17989:; 
main_sum_req_up = 0;
goto label_17940;
}
}
else 
{
label_17940:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18187;
}
else 
{
label_18187:; 
main_diff_req_up = 0;
goto label_18138;
}
}
else 
{
label_18138:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18385;
}
else 
{
label_18385:; 
main_pres_req_up = 0;
goto label_18336;
}
}
else 
{
label_18336:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18583;
}
else 
{
label_18583:; 
main_dbl_req_up = 0;
goto label_18534;
}
}
else 
{
label_18534:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18781;
}
else 
{
label_18781:; 
main_zero_req_up = 0;
goto label_18732;
}
}
else 
{
label_18732:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_18949;
}
else 
{
label_18949:; 
main_clk_req_up = 0;
goto label_18919;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19096;
}
else 
{
label_19096:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19186;
}
else 
{
label_19186:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19276;
}
else 
{
label_19276:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19366;
}
else 
{
label_19366:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19456;
}
else 
{
label_19456:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19546;
}
else 
{
label_19546:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19636;
}
else 
{
label_19636:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19726;
}
else 
{
label_19726:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19852;
}
else 
{
label_19852:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20122;
}
else 
{
label_20122:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20212;
}
else 
{
label_20212:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20302;
}
else 
{
label_20302:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20392;
}
else 
{
label_20392:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20482;
}
else 
{
label_20482:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20572;
}
else 
{
label_20572:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20662;
}
else 
{
label_20662:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20752;
}
else 
{
label_20752:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20878;
}
else 
{
label_20878:; 
if (((int)S3_zero_st) == 0)
{
goto label_16781;
}
else 
{
}
label_21093:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
goto label_15594;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
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
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_15968;
}
else 
{
label_15968:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_16008;
}
else 
{
label_16008:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_16048;
}
else 
{
label_16048:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_16088;
}
else 
{
label_16088:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_16128;
}
else 
{
label_16128:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_16168;
}
else 
{
label_16168:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_16208;
}
else 
{
label_16208:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_16248;
}
else 
{
label_16248:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_16304;
}
else 
{
label_16304:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_16424;
}
else 
{
label_16424:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_16464;
}
else 
{
label_16464:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_16504;
}
else 
{
label_16504:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_16544;
}
else 
{
label_16544:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_16584;
}
else 
{
label_16584:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_16624;
}
else 
{
label_16624:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_16664;
}
else 
{
label_16664:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_16704;
}
else 
{
label_16704:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_16760;
}
else 
{
label_16760:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
if (((int)main_in1_req_up) == 1)
{
if (main_in1_val != main_in1_val_t)
{
main_in1_val = main_in1_val_t;
main_in1_ev = 0;
goto label_17587;
}
else 
{
label_17587:; 
main_in1_req_up = 0;
goto label_17550;
}
}
else 
{
label_17550:; 
if (((int)main_in2_req_up) == 1)
{
if (main_in2_val != main_in2_val_t)
{
main_in2_val = main_in2_val_t;
main_in2_ev = 0;
goto label_17785;
}
else 
{
label_17785:; 
main_in2_req_up = 0;
goto label_17748;
}
}
else 
{
label_17748:; 
if (((int)main_sum_req_up) == 1)
{
if (main_sum_val != main_sum_val_t)
{
main_sum_val = main_sum_val_t;
main_sum_ev = 0;
goto label_17983;
}
else 
{
label_17983:; 
main_sum_req_up = 0;
goto label_17946;
}
}
else 
{
label_17946:; 
if (((int)main_diff_req_up) == 1)
{
if (main_diff_val != main_diff_val_t)
{
main_diff_val = main_diff_val_t;
main_diff_ev = 0;
goto label_18181;
}
else 
{
label_18181:; 
main_diff_req_up = 0;
goto label_18144;
}
}
else 
{
label_18144:; 
if (((int)main_pres_req_up) == 1)
{
if (main_pres_val != main_pres_val_t)
{
main_pres_val = main_pres_val_t;
main_pres_ev = 0;
goto label_18379;
}
else 
{
label_18379:; 
main_pres_req_up = 0;
goto label_18342;
}
}
else 
{
label_18342:; 
if (((int)main_dbl_req_up) == 1)
{
if (main_dbl_val != main_dbl_val_t)
{
main_dbl_val = main_dbl_val_t;
main_dbl_ev = 0;
goto label_18577;
}
else 
{
label_18577:; 
main_dbl_req_up = 0;
goto label_18540;
}
}
else 
{
label_18540:; 
if (((int)main_zero_req_up) == 1)
{
if (main_zero_val != main_zero_val_t)
{
main_zero_val = main_zero_val_t;
main_zero_ev = 0;
goto label_18775;
}
else 
{
label_18775:; 
main_zero_req_up = 0;
goto label_18738;
}
}
else 
{
label_18738:; 
if (((int)main_clk_req_up) == 1)
{
if (((int)main_clk_val) != ((int)main_clk_val_t))
{
main_clk_val = main_clk_val_t;
main_clk_ev = 0;
main_clk_neg_edge = 0;
main_clk_pos_edge = 2;
goto label_18946;
}
else 
{
label_18946:; 
main_clk_req_up = 0;
goto label_18926;
}
}
else 
{
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_19090;
}
else 
{
label_19090:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_19180;
}
else 
{
label_19180:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_19270;
}
else 
{
label_19270:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_19360;
}
else 
{
label_19360:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_19450;
}
else 
{
label_19450:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_19540;
}
else 
{
label_19540:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_19630;
}
else 
{
label_19630:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_19720;
}
else 
{
label_19720:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_19846;
}
else 
{
label_19846:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_20116;
}
else 
{
label_20116:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_20206;
}
else 
{
label_20206:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_20296;
}
else 
{
label_20296:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_20386;
}
else 
{
label_20386:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_20476;
}
else 
{
label_20476:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_20566;
}
else 
{
label_20566:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_20656;
}
else 
{
label_20656:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_20746;
}
else 
{
label_20746:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_20872;
}
else 
{
label_20872:; 
}
label_21099:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
goto label_15591;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
