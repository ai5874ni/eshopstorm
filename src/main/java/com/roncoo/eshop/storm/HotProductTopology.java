package com.roncoo.eshop.storm;

import com.roncoo.eshop.storm.bolt.LogParseBolt;
import com.roncoo.eshop.storm.bolt.ProductCountBolt;
import com.roncoo.eshop.storm.spout.AccessLogKafkaSpout;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.apache.storm.utils.Utils;

/**
 * @program: eshopstorm
 * @description: Prewarm Storm
 * @author: Li YangLin
 * @create: 2018-08-25 16:06
 */
public class HotProductTopology {
    public static void main(String args[]) {
        TopologyBuilder topologyBuilder = new TopologyBuilder();
        topologyBuilder.setSpout("AccessLogKafkaSpout", new AccessLogKafkaSpout(), 1);
        topologyBuilder.setBolt("LogParseBolt", new LogParseBolt(), 2)
                .setNumTasks(2)
                .shuffleGrouping("AccessLogKafkaSpout");
        topologyBuilder.setBolt("ProductCountBolt", new ProductCountBolt(), 2)
                .setNumTasks(2)
                .fieldsGrouping("LogParseBolt", new Fields("productId"));
        Config config = new Config();
        if (args != null && args.length > 0) {
            config.setNumWorkers(2);
            try {
                StormSubmitter.submitTopology(args[0], config, topologyBuilder.createTopology());
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("HotProductTopology", config, topologyBuilder.createTopology());
            Utils.sleep(30000);
            cluster.shutdown();

        }


    }
}
