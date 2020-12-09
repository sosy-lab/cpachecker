const HtmlWebpackPlugin = require('html-webpack-plugin');;
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const TerserPlugin = require("terser-webpack-plugin");
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const path = require("path");
const devFolder = "development_data/";

module.exports = {
    mode: 'production',
    entry: './report.js',
    output: {
        filename: 'bundle.js',
        path: path.join(__dirname + '/build')
    },
    devServer: {
        compress: true,
        open: true,
        port: 8080
    },
    plugins: [
        new HtmlWebpackPlugin({
            "template": devFolder + "index.html"
        }),
        new MiniCssExtractPlugin({
            "filename": "bundle.css"
        })
    ],
    module: {
        rules: [{
                test: /\.css$/i,
                use: [MiniCssExtractPlugin.loader, 'css-loader'],
            },
            {
                test: /\.(png|jpg|gif)$/i,
                use: [{
                    loader: 'url-loader',
                    options: {
                        limit: 8192,
                    }
                }]
            }
        ],
    },
    optimization: {
        minimize: true,
        minimizer: [
            new TerserPlugin(),
            new CssMinimizerPlugin(),
        ],
    },
    resolve: {
        alias: {
            devData: path.resolve(__dirname, devFolder + "data.json")
        }
    }
};
