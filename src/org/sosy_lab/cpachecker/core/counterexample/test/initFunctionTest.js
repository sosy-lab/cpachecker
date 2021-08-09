// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2018 Lokesh Nandanwar
// SPDX-FileCopyrightText: 2018-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

describe("Init action handler", () => {
  describe("function init", () => {
    it("Should be defined", () => {
      expect(init).not.toBeUndefined();
    });
  });

  describe("argJson variable initialization", () => {
    it("argJson Should be defined", () => {
      expect(window.argJson).not.toBeUndefined();
    });
  });

  describe("sourceFiles variable initialization", () => {
    it(" sourceFiles Should be defined", () => {
      expect(window.sourceFiles).not.toBeUndefined();
    });
  });

  describe("cfaJson variable initialization", () => {
    it("cfaJson Should be defined", () => {
      expect(window.cfaJson).not.toBeUndefined();
    });
  });

  describe("functions variable initialization  and declaration", () => {
    it("functions Should be defined", () => {
      expect(functions).toEqual(cfaJson.functionNames);
    });
  });

  describe("functionCallEdges variable initialization and declaration", () => {
    it("functionCallEdges Should be defined", () => {
      expect(functionCallEdges).toEqual(cfaJson.functionCallEdges);
    });
  });

  describe("graphSplitThreshold variable initialization", () => {
    it("graphSplitThreshold Should be defined", () => {
      expect(graphSplitThreshold).not.toBeUndefined();
    });
  });

  describe("graphSplitThreshold variable declaration", () => {
    it("graphSplitThreshold Should equal 700", () => {
      expect(graphSplitThreshold).toEqual(700);
    });
  });

  describe("zoomEnabled variable declaration", () => {
    it("zoomEnabled Should defined", () => {
      expect(zoomEnabled).not.toBeUndefined();
    });
  });

  describe("zoomEnabled variable declaration", () => {
    it("zoomEnabled Should equal false initially", () => {
      expect(zoomEnabled).toEqual(false);
    });
  });

  describe("render variable declaration", () => {
    it("render Should be defined", () => {
      expect(render).not.toBeUndefined();
    });
  });

  describe("margin variable declaration", () => {
    it("margin Should be defined", () => {
      expect(margin).not.toBeUndefined();
    });
  });

  describe("margin variable initialization", () => {
    it("margin Should be 20", () => {
      expect(margin).toEqual(20);
    });
  });

  describe("cfaSplit variable declaration", () => {
    it("cfaSplit Should be defined", () => {
      expect(cfaSplit).not.toBeUndefined();
    });
  });

  describe("cfaSplit variable initialization", () => {
    it("cfaSplit Should equal false initially", () => {
      expect(cfaSplit).toEqual(false);
    });
  });

  describe("argTabDisabled variable declaration", () => {
    it("argTabDisabled Should be defined", () => {
      expect(argTabDisabled).not.toBeUndefined();
    });
  });

  describe("argTabDisabled variable declaration", () => {
    it("argTabDisabled Should equal false initially", () => {
      expect(argTabDisabled).toEqual(false);
    });
  });
});
