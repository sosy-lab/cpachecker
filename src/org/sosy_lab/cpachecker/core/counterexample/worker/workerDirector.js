// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

import { argWorkerData, cfaWorkerData } from "workerData";
import { Buffer } from "buffer";

const pako = require("pako");

// Remove worker-instance specific callback functions
const deleteCallbacks = (worker) => {
  delete worker.callback;
  delete worker.onErrorCallback;
};

const handleWorkerMessage = (msg, worker, callback) => {
  worker.busy = false;
  deleteCallbacks(worker);
  if (callback) {
    callback(msg);
  }
};

const base64Header = "data:text/plain;base64,";

const argWorkerDataDecompressed =
  base64Header +
  Buffer.from(pako.inflate(Buffer.from(argWorkerData, "base64"))).toString(
    "base64"
  );
const cfaWorkerDataDecompressed =
  base64Header +
  Buffer.from(pako.inflate(Buffer.from(cfaWorkerData, "base64"))).toString(
    "base64"
  );

const argWorker = new Worker(argWorkerDataDecompressed);
argWorker.workerName = "argWorker";
argWorker.onmessage = (result) =>
  handleWorkerMessage(result.data, argWorker, argWorker.callback);
argWorker.onerror = (err) =>
  handleWorkerMessage(err.message, argWorker, argWorker.onErrorCallback);

const cfaWorker = new Worker(cfaWorkerDataDecompressed);
cfaWorker.workerName = "cfaWorker";
cfaWorker.onmessage = (result) =>
  handleWorkerMessage(result.data, cfaWorker, cfaWorker.callback);
cfaWorker.onerror = (err) =>
  handleWorkerMessage(err.message, cfaWorker, cfaWorker.onErrorCallback);

const workerPool = { argWorker, cfaWorker };

// FIFO queue for the jobs that will be executed by the workers
const jobQueue = [];

// Gets the first idle worker and reserves it for job dispatch in case one is available
const reserveWorker = (workerName) => {
  const worker = workerPool[workerName];
  if (!worker.busy) {
    worker.busy = true;
    return worker;
  }
  return undefined;
};

// Executes the first job of the queue
const processQueue = () => {
  const job = jobQueue.shift();
  if (job) {
    const reservedWorker = reserveWorker(job.workerName);
    if (reservedWorker) {
      reservedWorker.callback = job.callback;
      reservedWorker.onErrorCallback = job.onErrorCallback;
      reservedWorker.postMessage(job.data);
    } else {
      jobQueue.unshift(job);
    }
    setTimeout(processQueue, 0);
  }
};

/**
 * Registers a new job request.
 *
 * @param workerName name of the worker to use
 * @param data data that is passed to the worker
 */
const enqueue = async (workerName, data) =>
  new Promise((resolve, reject) => {
    jobQueue.push({
      workerName,
      data,
      callback: resolve,
      onErrorCallback: reject,
    });
    setTimeout(processQueue, 0);
  });

export default enqueue;
