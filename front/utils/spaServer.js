/**
 * Script used by the `Dockerfile` to serve the built project.
 */
const serverFactory = require('spa-server');
const devWebpackConfig = require('../webpack.dev.js');


const server = serverFactory.create({
    path: devWebpackConfig.devServer.contentBase,
    port: devWebpackConfig.devServer.port,
    fallback: 'index.html',
});

server.start();
