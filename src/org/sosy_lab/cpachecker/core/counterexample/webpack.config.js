// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

const HtmlWebpackPlugin = require("html-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const TerserPlugin = require("terser-webpack-plugin");
const CssMinimizerPlugin = require("css-minimizer-webpack-plugin");
const path = require("path");
const devFolder = "development_data/";
const licenseFolder = "dependency_data/";

module.exports = {
  mode: "production",
  entry: "./report.js",
  output: {
    filename: "bundle.js",
    chunkFilename: "[name].js",
    path: path.join(__dirname + "/build"),
  },
  devServer: {
    compress: true,
    open: true,
    port: 8080,
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: devFolder + "index.html",
    }),
    new MiniCssExtractPlugin({
      filename: "bundle.css",
      chunkFilename: "[name].css",
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
        // Don't extract license comments, we bundle them separately
        extractComments: false,
      }),
      new CssMinimizerPlugin(),
    ],
    runtimeChunk: false,
    splitChunks: {
      chunks: "all",
      cacheGroups: {
        vendors: {
          chunks: "all",
          name: "vendors",
          test: /(node_modules|src\/dependency_licenses\/licenses\.json)/,
        },
      },
    },
    // Make vendor bundle change less often even if our own code changes.
    occurrenceOrder: false,
  },
  resolve: {
    alias: {
      devData: path.resolve(__dirname, devFolder + "data.json"),
      dependencies: path.resolve(
        __dirname,
        licenseFolder + "dependencies.json"
      ),
    },
  },
};
