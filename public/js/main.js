var chart_container = "area-chart";

//in format http://example
var base_url = $("body").data("base_url")

var url_websocket = "ws://"+base_url.substr(7)+"/ws"

var chart_map = [
    {
        "v_type":0,
        "type":"humanity",
        "container":"humanity_chart",
        "color" :"#337ab7"
    },
    {
        "v_type":1,
        "type":"temperature",
        "container":"temperature_chart",
        "color":"#d9534f"
    }]


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
    chart_map[json_data.v_type].chart_obj.series[0].addPoint([json_data.date,json_data.value]);
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

    for(x in chart_map)
        chart_map[x].chart_obj = new Highcharts.StockChart({
            chart:{
                renderTo:chart_map[x].container
            },
            rangeSelector: {
                buttons: [
                {
                    count: 10,
                    type: 'second',
                    text: '10S'
                },
                {
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
                selected: false
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
                data : [],
                color :chart_map[x].color
            }]
        });
})

