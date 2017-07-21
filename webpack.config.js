const path = require('path');
const webpack = require('webpack');

module.exports = {
    entry: './src/index.js',
    plugins: [
        new webpack.HotModuleReplacementPlugin() // Enable HMR
    ],
    output: {
        path: path.resolve(__dirname, 'dist'),
        filename: 'bundle.js'
    },
    module: {
        rules: [
            { test: /\.js$/, exclude: /node_modules/, loader: "babel-loader" }
        ]
    }
};