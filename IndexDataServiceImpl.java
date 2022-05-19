package com.suning.snfe.service.impl;

import com.suning.snfe.replication.SlaveIncrementalSyncReq;
import com.suning.snfe.service.ClusterManageService;
import com.suning.snfe.service.FaceSearchNativeService;
import com.suning.snfe.service.IndexDataService;
import com.suning.snfe.service.RedisService;
import com.suning.snfe.vo.GroupVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author 18070959
 * @create 2019-01-28 9:35 AM
 * @desc
 **/
@Service
public class IndexDataServiceImpl implements IndexDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexDataServiceImpl.class);

    private static final int MEMORY_TREAD_POOL_SIZE = 10;

    @Autowired
    private GroupServiceImpl groupService;

    @Autowired
    private RedisService redisService;

    private ExecutorService executor;

    @Autowired
    private ClusterManageService clusterManageService;

    @Autowired
    private FaceSearchNativeService faceSearchNativeService;

    @Autowired
    private SlaveIncrementalSyncReq slaveIncrementalSyncReq;

    @Override
    public void initIndexData() {
        long begin = System.currentTimeMillis();
        LOGGER.info("start to init index group");

        // 创建线程池
        executor = Executors.newFixedThreadPool(MEMORY_TREAD_POOL_SIZE);

        // 获取集群编号
        String clusterId = clusterManageService.getClusterId();

        // redis的seq设置为本机已处理seq
        if (redisService.exists(clusterId)) {
            BigInteger redisNum = new BigInteger((redisService.get(clusterId).toString()));
            slaveIncrementalSyncReq.getSeq().set(redisNum.longValue());
        }

        // 根据集群编号（老机器集群编号与IP一致）获取group数据
        List<GroupVO> groupVOs = groupService.listByIpAddress(clusterId);

        // 每个groupId采用一个线程进行数据同步
        if (CollectionUtils.isNotEmpty(groupVOs)) {
            groupVOs.forEach(groupVO -> executor.execute(new InitIndexTask(groupVO.getGroupId())));
        }

        // 等待所有线程执行完毕
        executor.shutdown();
        try {
            executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("init index group fail", e);
            Thread.currentThread().interrupt();
        }

        LOGGER.info("init index group success ! cost time {} ms!", (System.currentTimeMillis() - begin));
    }

    @Override
    public void relaseIndexData() {
        long begin = System.currentTimeMillis();
        LOGGER.info("start to relase index group");

        // 销毁索引库
        faceSearchNativeService.destroyedFaceSearchNative();

        LOGGER.info("relase index group success ! cost time {} ms!", (System.currentTimeMillis() - begin));
    }
}