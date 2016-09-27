
network.on("dragEnd", function (params) {
    for (var i = 0; i < params.nodes.length; i++) {
        var nodeId = params.nodes[i];
        nodes.update({id: nodeId, fixed: {x: true, y: true} });
    }
});

network.on("dragStart", function(params) {
    for (var i = 0; i < params.nodes.length; i++) {
        var nodeId = params.nodes[i];
        nodes.update({id: nodeId, fixed: {x: false, y: false} });
    }
});