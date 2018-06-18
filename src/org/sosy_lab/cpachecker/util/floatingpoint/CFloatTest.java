/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.floatingpoint;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class CFloatTest {

  /**
   * Following are test cases as provided by a x86_64 amd architecture with a 64-bit linux (ubuntu),
   * compiled using gcc in c11 compliance.
   */

  @SuppressWarnings("deprecation")
  @Test
  public void infTest() {
    CFloat f_n1 = new CFloatNative("-1", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat d_n0 = new CFloatNative("-0.0", CFloatNativeAPI.FP_TYPE_DOUBLE);

    CFloat f_1 = new CFloatNative(CFloatNativeAPI.ONE_SINGLE, CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat d_1 = new CFloatNative(CFloatNativeAPI.ONE_DOUBLE, CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat ld_1 = new CFloatNative(CFloatNativeAPI.ONE_LONG_DOUBLE, CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    CFloat cf_f = new CFloatNative("0.0", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat cf_d = new CFloatNative("0.0", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat cf_ld = new CFloatNative("0.0", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);


    assertThat(f_1.divideBy(cf_f).toString()).isEqualTo("inf");
    assertThat(f_1.divideBy(cf_d).toString()).isEqualTo("inf");
    assertThat(f_1.divideBy(cf_ld).toString()).isEqualTo("inf");

    assertThat(d_1.divideBy(cf_f).toString()).isEqualTo("inf");
    assertThat(d_1.divideBy(cf_d).toString()).isEqualTo("inf");
    assertThat(d_1.divideBy(cf_ld).toString()).isEqualTo("inf");

    assertThat(ld_1.divideBy(cf_f).toString()).isEqualTo("inf");
    assertThat(ld_1.divideBy(cf_d).toString()).isEqualTo("inf");
    assertThat(ld_1.divideBy(cf_ld).toString()).isEqualTo("inf");

    assertThat(f_n1.divideBy(cf_f).toString()).isEqualTo("-inf");
    assertThat(f_n1.divideBy(cf_d).toString()).isEqualTo("-inf");
    assertThat(f_n1.divideBy(cf_ld).toString()).isEqualTo("-inf");

    assertThat(f_1.divideBy(d_n0).toString()).isEqualTo("-inf");
    assertThat(d_1.divideBy(d_n0).toString()).isEqualTo("-inf");
    assertThat(ld_1.divideBy(d_n0).toString()).isEqualTo("-inf");

    CFloat inf_f = f_1.divideBy(cf_f);
    CFloat inf_nf = f_n1.divideBy(cf_f);
    assertThat(inf_f.add(inf_f).toString()).isEqualTo("inf");
    assertThat(inf_nf.add(inf_nf).toString()).isEqualTo("-inf");
    assertThat(inf_f.subtract(inf_nf).toString()).isEqualTo("inf");
    assertThat(inf_nf.subtract(inf_f).toString()).isEqualTo("-inf");
  }

  @SuppressWarnings("deprecation")
  @Test
  public void nanTest() {
    CFloat cf_f = new CFloatNative("0.0", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat cf_d = new CFloatNative("0.0", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat cf_ld = new CFloatNative("0.0", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);


    assertThat(cf_f.divideBy(cf_f).toString()).isEqualTo("-nan");
    assertThat(cf_f.divideBy(cf_d).toString()).isEqualTo("-nan");
    assertThat(cf_f.divideBy(cf_ld).toString()).isEqualTo("-nan");

    assertThat(cf_d.divideBy(cf_f).toString()).isEqualTo("-nan");
    assertThat(cf_d.divideBy(cf_d).toString()).isEqualTo("-nan");
    assertThat(cf_d.divideBy(cf_ld).toString()).isEqualTo("-nan");

    assertThat(cf_ld.divideBy(cf_f).toString()).isEqualTo("-nan");
    assertThat(cf_ld.divideBy(cf_d).toString()).isEqualTo("-nan");
    assertThat(cf_ld.divideBy(cf_ld).toString()).isEqualTo("-nan");

    CFloat f_1 = new CFloatNative(CFloatNativeAPI.ONE_SINGLE, CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat f_n1 = new CFloatNative("-1", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat inf_f = f_1.divideBy(cf_f);
    CFloat inf_nf = f_n1.divideBy(cf_f);

    assertThat(inf_f.subtract(inf_f).toString()).isEqualTo("-nan");
    assertThat(inf_nf.subtract(inf_nf).toString()).isEqualTo("-nan");
    assertThat(inf_f.add(inf_nf).toString()).isEqualTo("-nan");
    assertThat(inf_nf.add(inf_f).toString()).isEqualTo("-nan");

    cf_f = cf_f.divideBy(cf_f);
    assertThat(cf_f.add(f_1).toString()).isEqualTo("-nan");
  }

  @SuppressWarnings("deprecation")
  @Test
  public void formatTest() {
    CFloat cf_f = new CFloatNative("71236.262625", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat cf_d = new CFloatNative("7891274812.82489681243896484375", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat cf_ld =
        new CFloatNative(
            "82173928379128.897125244140625",
            CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    CFloat cf_f2 = new CFloatNative("10.0", CFloatNativeAPI.FP_TYPE_SINGLE);

    assertThat(cf_f.toString()).isEqualTo("71236.265625");
    assertThat(cf_f2.toString()).isEqualTo("10.0");
    assertThat(cf_d.toString()).isEqualTo("7891274812.82489681243896484375");
    assertThat(cf_ld.toString()).isEqualTo("82173928379128.897125244140625");

    CFloatNative two = new CFloatNative("2.0", CFloatNativeAPI.FP_TYPE_SINGLE);
    cf_ld = cf_ld.multiply(two);

    assertThat(cf_ld.toString()).isEqualTo("164347856758257.79425048828125");

    for (int i = 0; i < 10; i++) {
      cf_ld = cf_ld.multiply(two);
    }
    assertThat(cf_ld.toString()).isEqualTo("168292205320455981.3125");

    for (int i = 0; i < 10; i++) {
      cf_ld = cf_ld.multiply(two);
    }
    assertThat(cf_ld.toString()).isEqualTo("172331218248146924864.0");

    for (int i = 0; i < 10; i++) {
      cf_ld = cf_ld.multiply(two);
    }
    assertThat(cf_ld.toString()).isEqualTo("176467167486102451060736.0");

    for (int i = 0; i < 800; i++) {
      cf_ld = cf_ld.multiply(two);
    }
    assertThat(cf_ld.toString()).isEqualTo(
        "1176685619726757694650973653036889804515652541394935327945104207967319975396405043723449926929245309944011749663826014608357167360348998884812228642560158430137541411941079758241360655682022168784213489289673637327812997677674819356237568534523669580117177832308736.0");

    for (int i = 0; i < 15506; i++) {
      cf_ld = cf_ld.multiply(two);
    }
    assertThat(cf_ld.toString()).isEqualTo(
        "694661471029285501212945771332116283189597310134176279871043189542821592361034290078606668822051846516619797487990244598469777314072213775651740997048986477928169371715229655255359206162402789701556285027901731276951276932966299893756710604647716529685676844817520947850222003470298462271502609956078769849589900408767485724485967733599194870111268572665770915385732745533077935584814887246079891275768358592541249141208098888465601496657768977980719104688305805446337127467507655600805010440069686957667093439005930743710899465835693162496930922042219965565755872345088979745951059544410264590885533059802634911610112359072280643717850034447378889483354616356534072050883426144896485391839271548200637934784358423828361948121933169485446537718082323096878768326042779833555104832040698943211166866424754054058518687473286474067948292828214615505961268255909034200141046284021014576712140193597581626713649928012759984522574528083050285415720613046688185859795637548551514445633267671655826926175614480816809095786215643662930033739743457417532921030288466815508794482486385556026749829923189097288118396937096410891290928194250769813296387818031308061421013699955828562315519439982067347353925339756023987581503234833828283545902961339322113952589831569641527330790502240134789694938008747222640520135657293347309106779646491458431122937330634928614238016681904060576387983823642493996674208885147238684518016402962625484639057682915438581028172712163704500077695878867640029346320782445488797900101702716568106462610025654794142243666380061530725147800898964271744942918698143898638677164669373415496340898955825715641315678595790331151935905439190907272988737897140776341551472533780997375933937280166567585546743417096516847812195640709165790520762176180645358796498294497937404592297711082877765435291230286449583290834014773608422738804265973087281653073568570051219143973910598323202339617831684547261405588501034825543136062625740678347521636328626362325934600058707526730757605612347610346212727713445867537697290954889015840832006481552544354178598796024443289767222437288540384480913881790894681495830618486454777010193932844297424215592577430441244105631112284969397475586241350252719801955697719248263291576677400674564535012124466537817216674499601502843427706380322070207536266111849178732613493037133143914501656258394138427949341147498750043592117864861596291386955437525765875792978815200711703086031773265094062090433776474312925188037192978252390118103889764695250098400244382524632823218243298009716454263131170334953697375330234224834781529277158940732501033393627628292312518166039974135205583324016714039552252629662543435069986204212642024264748258088696773792510522901517507147404584504975919932787426188933702059345208175186414713367396753093710889099933396533310908624159283761910931570929031203239686518771465176191244443044838834439123817697621124371319307459407210656775350861659923351214137817620919405807574682209639907041498911534486791680147248174965353762937170834652169113699719970650209429514481731839607352056178750857322027687164428090119048609946821756584153887527951824142714772071015481449220282242859175402042471364900016874449405119766458202737518073575124390460229466164486486763731134790535338425159647362988989102415661368726931304389708098839253559241268159331162251596344133526786246671926477664370370204892975840991346964500139158435599256060845711019902324906317364112623636594144730900393698675674366435769737226028433017218491798546804160179402915827547646124889736393626326956158058847406745332212306275800009936633898125529945356843416313313501275104561978090672919846164480810915703489417353646940510996425915489531252616528416221473099427215939525288115469518510884813536389509309975809114935485012665444248884560381299604705974350152817929550833988410426558267784658496954992911562603970289791474655990552071036799681461338387116372476956571874717859729511104422791089338367448599166202680369739168769388326694208316406853213013832919099103563404858174925582486163566094997172082576169828886731599018830716649789663649445507892365369040297910847183802163649290889687899776076477303597955828244302748529695657718469885311506327204991597781008685208927343341458933797554504751392613129528016892940210384002022271833758521477022845254905080750452568603937516959398361005202759397145084274883419089761733733647216599798582928442438858339726851504835078826199077797972437788669165168797349587348145797789965900525131835010904232570351280328538495302926825096721212439857569699203144367206323634988578019367872848819173425685208948688233960869907377512223872994463655157441362032426157102214890200914510170508584825733648066487606047134622221180356046988009722071281017552038723432403711375708998205704619531394674018103728990824986210336710292853665742993819868990892512234593540492795363955983517552859998485133932437974378822687631687953586593586671266279562696266730495279104.0");

    cf_ld = cf_ld.multiply(two);
    assertThat(cf_ld.toString()).isEqualTo("inf");
  }

  @SuppressWarnings("deprecation")
  @Test
  public void zeroTest() {
    CFloatNative zero = new CFloatNative("0.0", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloatNative nZero = new CFloatNative("-0.0", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloatNative nOne = new CFloatNative("-1.0", CFloatNativeAPI.FP_TYPE_DOUBLE);

    assertThat(zero.add(nZero).toString()).isEqualTo("0.0");
    assertThat(nZero.add(zero).toString()).isEqualTo("0.0");
    assertThat(nZero.add(nZero).toString()).isEqualTo("-0.0");
    assertThat(nZero.subtract(zero).toString()).isEqualTo("-0.0");
    assertThat(zero.subtract(nZero).toString()).isEqualTo("0.0");
    assertThat(nZero.subtract(nZero).toString()).isEqualTo("0.0");
    assertThat(zero.subtract(zero).toString()).isEqualTo("0.0");
    assertThat(nOne.multiply(zero).toString()).isEqualTo("-0.0");
    assertThat(nOne.multiply(nZero).toString()).isEqualTo("0.0");
    assertThat(nZero.divideBy(nOne).toString()).isEqualTo("0.0");
    assertThat(zero.divideBy(nOne).toString()).isEqualTo("-0.0");
    assertThat(zero.multiply(nZero).toString()).isEqualTo("-0.0");
  }

  @SuppressWarnings("deprecation")
  @Test
  public void additionTest() {
    CFloat ten = new CFloatImpl("10", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat five = new CFloatImpl("5", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat nOne = new CFloatImpl("-1", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    CFloatNative p = new CFloatNative(ten.copyWrapper(), ten.getType());
    CFloatNative q = new CFloatNative(five.copyWrapper(), five.getType());
    assertThat(p.toString()).isEqualTo("10.0");
    assertThat(q.toString()).isEqualTo("5.0");

    CFloat res = ten.add(ten);
    p = new CFloatNative(res.copyWrapper(), CFloatNativeAPI.FP_TYPE_DOUBLE);
    assertThat(p.toString()).isEqualTo("20.0");

    res = res.add(ten, ten, five);
    p = new CFloatNative(res.copyWrapper(), CFloatNativeAPI.FP_TYPE_DOUBLE);
    assertThat(p.toString()).isEqualTo("45.0");

    res = res.add(nOne, nOne, nOne);
    p = new CFloatNative(res.copyWrapper(), CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    assertThat(p.toString()).isEqualTo("42.0");
  }
}
