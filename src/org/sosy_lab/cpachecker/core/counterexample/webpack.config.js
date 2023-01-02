// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const TerserPlugin = require("terser-webpack-plugin");
const CssMinimizerPlugin = require("css-minimizer-webpack-plugin");
const path = require("path");

const devFolder = "development_data/";
const vendorFolder = "vendor/";
const buildTmpFolder = "build_tmp/";

module.exports = {
  mode: "production",
  entry: "./report.js",
  output: {
    filename: "[name].js",
    path: path.join(`${__dirname}/build`),
  },
  devServer: {
    compress: true,
    open: true,
    port: 8080,
    historyApiFallback: {
      index: `${devFolder}index.html`,
    },
  },
  plugins: [
    new MiniCssExtractPlugin({
      filename: "[name].css",
    }),
  ],
  module: {
    rules: [
      {
        test: /\.css$/i,
        use: [MiniCssExtractPlugin.loader, "css-loader"],
      },
      {
        test: /\.(png|jpg|gif)$/i,
        use: [
          {
            loader: "url-loader",
            options: {
              limit: 8192,
            },
          },
        ],
      },
    ],
  },
  optimization: {
    minimize: true,
    minimizer: [
      new TerserPlugin({
        terserOptions: {
          output: {
            comments: false,
          },
        },
        // Don't extract license comments, we bundle them separately
        extractComments: false,
      }),
      new CssMinimizerPlugin({
        minimizerOptions: {
          preset: [
            "default",
            {
              discardComments: {
                removeAll: true,
              },
            },
          ],
        },
      }),
    ],
    runtimeChunk: false,
    splitChunks: {
      chunks: "all",
      cacheGroups: {
        defaultVendors: {
          chunks: "all",
          name: "vendors",
          test: /(node_modules)|(build_tmp\/dependencies\.json)|(vendor)/,
        },
        workerData: {
          chunks: "all",
          name: "workerData",
          test: /(build_tmp\/workerData\.js)/,
        },
      },
    },
  },
  resolve: {
    alias: {
      devData: path.resolve(__dirname, `${devFolder}data.json`),
      d3: path.resolve(__dirname, `${vendorFolder}d3.min.js`),
      "dagre-d3": path.resolve(__dirname, `${vendorFolder}dagre-d3.min.js`),
      dependencies: path.resolve(
        __dirname,
        `${buildTmpFolder}dependencies.json`
      ),
      workerData: path.resolve(__dirname, `${buildTmpFolder}workerData.js`),
    },
  },
};
