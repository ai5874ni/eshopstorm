package com.roncoo.eshop.storm.spout;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;


public class AccessLogKafkaSpout extends BaseRichSpout {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessLogKafkaSpout.class);
    private ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(1000);
    private SpoutOutputCollector spoutOutputCollector;

    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        this.spoutOutputCollector = spoutOutputCollector;
        startKafkaConsumer();
    }

    private void startKafkaConsumer() {
        Properties properties = new Properties();
        properties.put("zookeeper.connect", "192.168.0.3:2181,192.168.0.4:2181,192.168.0.5:2181");
        properties.put("group.id", "eshop-cache-group");
        properties.put("zookeeper.senssion.timeout.ms", "40000");
        properties.put("zookeeper.sync.time.ms", "200");
        properties.put("auto.commit.interval.ms", "1000");
        ConsumerConfig consumerConfig = new ConsumerConfig(properties);
        ConsumerConnector consumerConnector = Consumer.createJavaConsumerConnector(consumerConfig);
        String topic = "access-log";
        Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(topic, 1);
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap =
                consumerConnector.createMessageStreams(topicCountMap);
        List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);
        for (KafkaStream stream : streams) {
            new Thread(new KafkaMessageProcessor(stream)).start();
        }

    }

    public class KafkaMessageProcessor implements Runnable {

        private KafkaStream kafkaStream;

        public KafkaMessageProcessor(KafkaStream kafkaStream) {
            this.kafkaStream = kafkaStream;
        }

        public void run() {
            ConsumerIterator<byte[], byte[]> it = kafkaStream.iterator();
            while (it.hasNext()) {
                String message = new String(it.next().message());
                LOGGER.info("【==========================AccessLogKafkaSpout中的Kafka消费者接收到一条日志】message=" + message);
                try {
                    queue.put(message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void nextTuple() {
        if (queue.size() > 0) {
            try {
            String message = queue.take();
                spoutOutputCollector.emit(new Values(message));
                LOGGER.info("【AccessLogKafkaSpout发射出去一条日志】message=" + message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {
            Utils.sleep(100);
        }
    }

    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("message"));
    }
}
