var express = require("express");

var dir = process.argv[2];

function simulateLatency(res, latencyInSecs) {
    res.write("Coming through");
    setTimeout(function () {
        res.end();
    }, latencyInSecs * 1000);
}

var port = 8000;

express()
    .use(express.static(dir))
    .get('/connectTimeout4s', function(req, res, next) {
        simulateLatency(res, 4);
    })
    .get('/connectTimeout5s', function(req, res, next) {
        simulateLatency(res, 5);
    })
    .get('/found', function (req, res, next) {
        res.send();
    })
    .get('/redirect', function(req, res, next) {
        res.status(301).redirect('/found');
    })
    .listen(port, function (err) {
        if (err) {
            console.error(err);
            return;
        }
        console.log("Running on port", port);
    });
