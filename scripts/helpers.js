endpoint.sendMessage = function(channel, message) {
    var params = {
        channel: channel,
        message: message
    };
    return endpoint._sendMessage(params);
};