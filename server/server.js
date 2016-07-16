'use strict'
const WebSocketServer = require('ws').Server;
const keypress = require('keypress');

const wss = new WebSocketServer({port: 9001});

let lat = -6.175;
let long = 106.8272;
let alt = 8;

function getServerIp() {

    var os = require('os');
    var ifaces = os.networkInterfaces();
    var values = Object.keys(ifaces).map(function (name) {
        return ifaces[name];
    });
    values = [].concat.apply([], values).filter(function (val) {
        return val.family == 'IPv4' && val.internal == false;
    });

    return values.length ? values[0].address : '0.0.0.0';
}

keypress(process.stdin);
process.on('SIGINT', function () {
    console.log("Caught interrupt signal");

    if (true)
        process.exit();
})
console.log("Server Online on : " + getServerIp() + " port : " + 9001);
wss.on('connection', ws => {
    console.log('Client connected');
    ws.on('message', function (data, flags) {
        // flags.binary will be set if a binary data is received.
        // flags.masked will be set if the data was masked.
        console.log("Got Message : " + data);
        var dataString = data.toString().split(':');

        lat = parseFloat(dataString[0]);
        long = parseFloat(dataString[1]);
        alt = parseFloat(dataString[2]);

    });
    ws.on('close', () => process.exit(0));
    process.stdin.on('keypress', (ch, key) => {
        console.log(`Got keypress: ${JSON.stringify(key)}`);
        if (key && key.ctrl && key.name === 'c') {
            process.exit(0);
        }

        var movementValue = "0.0000" + (20 + ( Math.floor(Math.random() * (20 - 0 + 1) + 0) % 20));
        console.log("Movement Value : " + movementValue);
        switch (key.name) {
            case 'up':
                lat += parseFloat(movementValue);
                ws.send(`${lat}:${long}:${alt}`);
                break;
            case 'down':
                lat -= parseFloat(movementValue);//0.000040;
                ws.send(`${lat}:${long}:${alt}`);
                break;
            case 'left':
                long -= parseFloat(movementValue);
                ws.send(`${lat}:${long}:${alt}`);
                break;
            case 'right':
                long += parseFloat(movementValue);
                ws.send(`${lat}:${long}:${alt}`);
                break;
        }
    });
});


process.stdin.setRawMode(true);
process.stdin.resume();
