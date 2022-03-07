// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Person {

  private String age;

  @SuppressWarnings("unused")
  private String firstName;

  @SuppressWarnings("unused")
  private String surName;

  private String fullName;
  private String gender;

  public Person(String pFirstName, String pSurname, String pGender, String pAge) {
    this.age = pAge;
    this.firstName = pFirstName;
    this.surName = pSurname;
    this.gender = pGender;
    this.fullName = pFirstName + " " + pSurname;
  }

  public String getAge() {
    return age;
  }

  public String getGender() {
    return gender;
  }

  public String toString() {
    String result = "(Full Name:" + fullName + ", Age:" + age + ", Gender:" + gender + ")";
    return result;
  }
}
