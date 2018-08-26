package com.roncoo.eshop.storm.bolt;

import com.alibaba.fastjson.JSONObject;
import com.roncoo.eshop.storm.spout.AccessLogKafkaSpout;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @program: eshopstorm
 * @description: ${description}
 * @author: Li YangLin
 * @create: 2018-08-25 12:10
 */
public class LogParseBolt extends BaseRichBolt {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogParseBolt.class);
    private OutputCollector collector;

    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
    }

    public void execute(Tuple tuple) {
        String message = tuple.getStringByField("message");
        JSONObject messageJSON = JSONObject.parseObject(message);
        JSONObject uriArgsJSON = messageJSON.getJSONObject("uri_args");
        Long productId = uriArgsJSON.getLong("productId");
        if (productId != null) {
            collector.emit(new Values(productId));
            LOGGER.info("【LogParseBolt发射出去一个商品id】productId=" + productId);
        }

    }

    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("productId"));
    }
}
