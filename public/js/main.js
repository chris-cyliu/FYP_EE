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
        case "push_agg_data":
            update_metric(json.data)
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

/**
 * Foreach "class = metric"
 *  get data-agg
 *  get date-type
 *  from input[agg][type] update
 */
var update_metric = function(data){
    $(".metric").each(function(){
        var agg = $(this).data("agg");
        var type = $(this).data("type");
        if(typeof data[agg][type]!="undefined"){
            $(this).html(data[agg][type])
        }
    })
}


var Query_module = function(date_from_container , date_to_container , value_type_radio_name ,table_container, query_button){

    var current_table = null;
    var get_and_verify_input = function(){
        try {
            var from = date_from_container.date().toDate().getTime();
            var to = date_to_container.date().toDate().getTime();
            var value_type = $("input[name=\"" + value_type_radio_name + "\"]:checked").val();
        }catch(e){}

        if(from=="undefined"){
            from = new Date().getMilliseconds() - 24*60*60*1000;
        }
        if(to =="undefined"){
            to = new Date().getMilliseconds();
        }
        if(value_type == "undefined"){
            value_type == 0;
        }

        return {
            from:from,
            to:to,
            value_type:value_type
        }

    }
    var click_query_handler = function(){
        var query = get_and_verify_input();
        if(current_table!= null){
            current_table.destroy()
        }
        current_table = $(table_container).DataTable({
            ajax: {
                "url": base_url + "/query?from=" + query.from + "&to=" + query.to + "&value_type=" + query.value_type
            }
        });

    }

    $(query_button).click(click_query_handler)
}

