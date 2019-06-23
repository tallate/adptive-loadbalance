package com.aliware.tianchi.cluster;

import com.aliware.RandomUtil;
import com.aliware.cluster.Cluster;
import com.aliware.cluster.Server;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.aliware.config.LoadConfig.EXTEND_FACTOR;
import static com.aliware.config.LoadConfig.LOAD_THRESHOLD;

/**
 * 集群为繁忙状态
 * 1. 按每个服务器的负载比率分配请求
 */
public class BusyState implements ClusterState {

    private static final Logger logger = LoggerFactory.getLogger(BusyState.class);

    @Override
    public boolean match(Cluster cluster) {
        // 超出
        return cluster.getAvgLoad() - 1 >= LOAD_THRESHOLD;
    }

    /**
     * 为了让超出阈值的负载在负载均衡时更明显，将这些负载扩大
     */
    private double extendLoad(double load) {
        return load <= 1 ?
                load :
                1 + (load - 1) * EXTEND_FACTOR;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Server select(Cluster cluster) {
        // LOG: 记录算法切换过程
        // logger.info("BusyState selected");
        Set<Map.Entry<Byte, Server>> entrySet = cluster.getServerMap().entrySet();
        Map.Entry<Byte, Server>[] entries = entrySet
                .toArray(new Map.Entry[0]);
        // 通过每个服务器负载计算出负载时的权重
        List<Double> weights = Arrays.stream(entries)
                .map(entry -> {
                    // 大部分情况下过载在数值上不会特别明显，extend过程扩大了这部分的影响
                    double load = extendLoad(entry.getValue().getLoad());
                    return entry.getValue().getLoad() == 0 ?
                            1 :
                            1.0 / load;
                })
                .collect(Collectors.toList());
        // 计算赋权随机数
        int pos = RandomUtil.randOne(weights);
        return entries[pos].getValue();
    }
}
