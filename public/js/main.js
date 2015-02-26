var chart_container = "area-chart";

var chart_obj;

var url_websocket = "ws://127.0.0.1:8080/ws"

/**
 * Message handler
 * @param data
 */
var ws_message_handler = function(data) {
    console.dir("Received data : "+data)
    var json = JSON.parse(data);
    switch (json.event){
        case  "push_data":
            push_data_to_graph(json.data)
            break;
        default :
            console.error("Unknown event type : "+json.event)
    }
}

/**
 * json_data:{
 *  timestamp
 *  device_id
 *  v_type
 *  value
 * }
 * @param json_data
 */
var push_data_to_graph = function(json_data){
    chart_obj.series[0].addPoint([json_data.date,json_data.value]);
}

Highcharts.setOptions({
    global : {
        useUTC : false
    }
});

$(function(){
    //init
    var ws = new WebSocket(url_websocket);
    ws.onmessage = function(event){
        var m = event.data;
        ws_message_handler(m);
    }
    chart_obj = new Highcharts.StockChart({
        chart:{
            renderTo:chart_container
        },
        rangeSelector: {
            buttons: [{
                count: 1,
                type: 'minute',
                text: '1M'
            }, {
                count: 5,
                type: 'minute',
                text: '5M'
            }, {
                type: 'all',
                text: 'All'
            }],
            inputEnabled: false,
            selected: 0
        },

        title : {
            style: {
                display: 'none'
            }
        },

        exporting: {
            enabled: false
        },

        series : [{
            name : 'Temperature',
            data : []
        }]
    });
})

