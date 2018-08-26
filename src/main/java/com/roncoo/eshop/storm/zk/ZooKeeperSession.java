package com.roncoo.eshop.storm.zk;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

/**
 * @program: eshopstorm
 * @description: ${description}
 * @author: Li YangLin
 * @create: 2018-08-25 16:30
 */
public class ZooKeeperSession {
    private static CountDownLatch countDownLatch = new CountDownLatch(1);
    private ZooKeeper zookeeper;

    public ZooKeeperSession() {
        try {
            zookeeper = new ZooKeeper("192.168.0.3:2181,192.168.0.4:2181,192.168.0.5:2181",
                    50000,
                    new ZooKeeperWatcher());
            System.out.println(zookeeper.getState());
            // CountDownLatch
            // java多线程并发同步的一个工具类
            // 会传递进去一些数字，比如说1,2 ，3 都可以
            // 然后await()，如果数字不是0，那么久卡住，等待
            // 其他的线程可以调用coutnDown()，减1
            // 如果数字减到0，那么之前所有在await的线程，都会逃出阻塞的状态
            // 继续向下运行
            countDownLatch.await();
            System.out.println("ZooKeeper session established......");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取分布式锁
     */
    public void acquireDistributedLock() {
        String path = "/taskid-list-lock";
        try {
            zookeeper.create(path, "".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (Exception e) {
            // 如果那个商品对应的锁的node，已经存在了，就是已经被别人加锁了，那么就这里就会报错
            // NodeExistsException
            int count = 0;
            while (true) {
                try {
                    Thread.sleep(1000);
                    zookeeper.create(path, "".getBytes(),
                            Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                } catch (Exception e2) {
                    count++;
                    System.out.println("the " + count + " times try to acquire lock for taskid-list-lock......");
                    continue;
                }
                System.out.println("success to acquire lock for taskid-list-lock after " + count + " times try......");
                break;
            }
        }
    }
    /**
     * 释放掉一个分布式锁
     */
    public void releaseDistributedLock() {
        String path = "/taskid-list-lock";
        try {
            zookeeper.delete(path, -1);
            System.out.println("release the lock for taskid-list-lock......");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String getNodeData() {
        try {
            return new String(zookeeper.getData("/taskid-list", false, new Stat()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public void setNodeData(String path, String data) {
        try {
            createNodeData( path);
            zookeeper.setData(path, data.getBytes(), -1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
public void createNodeData(String path){
    try {
        zookeeper.create(path,"".getBytes(),Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
    /**
     * 建立zk session的watcher
     *
     * @author Administrator
     */
    private class ZooKeeperWatcher implements Watcher {

        public void process(WatchedEvent event) {
            System.out.println("Receive watched event: " + event.getState());
            if (Event.KeeperState.SyncConnected == event.getState()) {
                countDownLatch.countDown();
            }
        }

    }

    /**
     * 封装单例的静态内部类
     * @author Administrator
     *
     */
    private static class Singleton {

        private static ZooKeeperSession instance;

        static {
            instance = new ZooKeeperSession();
        }

        public static ZooKeeperSession getInstance() {
            return instance;
        }

    }

    /**
     * 获取单例
     * @return
     */
    public static ZooKeeperSession getInstance() {
        return Singleton.getInstance();
    }

    /**
     * 初始化单例的便捷方法
     */
    public static void init() {
        getInstance();
    }
}
