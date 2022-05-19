
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @date 2018/8/20
 * @description
 */
public class InitIndexTask implements Runnable {

    private FaceSearchNativeService faceSearchNativeService;

    private RegisterFaceService registerFaceService;

    private static final Logger LOGGER = LoggerFactory.getLogger(InitIndexTask.class);

    private String groupId;

    public InitIndexTask(String groupId) {
        this.groupId = groupId;
        faceSearchNativeService = SpringUtil.getServiceBean(FaceSearchNativeServiceImpl.class);
        registerFaceService = SpringUtil.getServiceBean(RegisterFaceServiceImpl.class);
    }

    @Override
    public void run() {

        long begin = System.currentTimeMillis();
        LOGGER.info("thread {} start to init index group", Thread.currentThread().getId());

        // 初始化索引库
        faceSearchNativeService.init(groupId);

        // 根据库Id查询库中人脸图片
        List<String> ctfnos = registerFaceService.listCtfnos(groupId);
        if (CollectionUtils.isNotEmpty(ctfnos)) {

            int listSize = ctfnos.size();
            int num = 10000;
            for (int i = 0; i < ctfnos.size(); i += num) {
                if (i + num > listSize) {
                    num = listSize - i;
                }
                List<String> subCtfnos = ctfnos.subList(i, i + num);
                List<RegisterFaceVO> registerFaceVOs = registerFaceService.listFace(subCtfnos, groupId);
                if (CollectionUtils.isNotEmpty(registerFaceVOs)) {
                    for (RegisterFaceVO registerFaceVO : registerFaceVOs) {
                        String ctfno = registerFaceVO.getCtfno();
                        String featureBase64 = registerFaceVO.getFeature();
                        float[] featureFloat = FeatureUtil.getFeatureArray(featureBase64);
                        faceSearchNativeService.add(groupId, featureFloat, ctfno);
                    }
                }
            }
        }
        LOGGER.info("init index group success ! cost time {} ms!", (System.currentTimeMillis() - begin));
    }
}
