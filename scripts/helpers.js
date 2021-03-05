endpoint.sendHl7Message = function(channel, message) {
    var params = {
        channel: channel,
        message: message
    };
    return endpoint._sendHl7Message(params);
};