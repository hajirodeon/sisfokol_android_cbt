cordova.define("cordova-unique-device-id.UniqueDeviceID", function(require, exports, module) { var exec = require('cordova/exec');

module.exports = {
    
    get: function(success, fail) {
        cordova.exec(success, fail, 'UniqueDeviceID', 'get', []);
    }

};
});
