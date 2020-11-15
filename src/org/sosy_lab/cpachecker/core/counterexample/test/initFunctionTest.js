// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2018 Lokesh Nandanwar
// SPDX-FileCopyrightText: 2018-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

describe("Init action handler", function () {

    describe("function init", function () {
        it("Should be defined", function () {
            expect(init).not.toBeUndefined();
        })
    })

    describe("argJson variable initialization", function () {
        it("argJson Should be defined", function () {
            expect(argJson).not.toBeUndefined();
        })
    })

    describe("sourceFiles variable initialization", function () {
        it(" sourceFiles Should be defined", function () {
            expect(sourceFiles).not.toBeUndefined();
        })
    })

    describe("cfaJson variable initialization", function () {
        it("cfaJson Should be defined", function () {
            expect(cfaJson).not.toBeUndefined();
        })
    })

    describe("functions variable initialization  and declaration", function () {
        it("functions Should be defined", function () {
            expect(functions).toEqual(cfaJson.functionNames);
        })
    })

    describe("functionCallEdges variable initialization and declaration", function () {
        it("functionCallEdges Should be defined", function () {
            expect(functionCallEdges).toEqual(cfaJson.functionCallEdges);
        })
    })

    describe("graphSplitThreshold variable initialization", function () {
        it("graphSplitThreshold Should be defined", function () {
            expect(graphSplitThreshold).not.toBeUndefined();
        })
    })

    describe("graphSplitThreshold variable declaration", function () {
        it("graphSplitThreshold Should equal 700", function () {
            expect(graphSplitThreshold).toEqual(700);
        })
    })

    describe("zoomEnabled variable declaration", function () {
        it("zoomEnabled Should defined", function () {
            expect(zoomEnabled).not.toBeUndefined();
        })
    })

    describe("zoomEnabled variable declaration", function () {
        it("zoomEnabled Should equal false initially", function () {
            expect(zoomEnabled).toEqual(false);
        })
    })

    describe("render variable declaration", function () {
        it("render Should be defined", function () {
            expect(render).not.toBeUndefined();
        })
    })

    describe("margin variable declaration", function () {
        it("margin Should be defined", function () {
            expect(margin).not.toBeUndefined();
        })
    })

    describe("margin variable initialization", function () {
        it("margin Should be 20", function () {
            expect(margin).toEqual(20);
        })
    })

    describe("cfaSplit variable declaration", function () {
        it("cfaSplit Should be defined", function () {
            expect(cfaSplit).not.toBeUndefined();
        })
    })

    describe("cfaSplit variable initialization", function () {
        it("cfaSplit Should equal false initially", function () {
            expect(cfaSplit).toEqual(false);
        })
    })

    describe("argTabDisabled variable declaration", function () {
        it("argTabDisabled Should be defined", function () {
            expect(argTabDisabled).not.toBeUndefined();
        })
    })

    describe("argTabDisabled variable declaration", function () {
        it("argTabDisabled Should equal false initially", function () {
            expect(argTabDisabled).toEqual(false);
        })
    })

})