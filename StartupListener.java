
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;


/**
 * @description 系统启动监听器
 */
public class StartupListener extends ContextLoaderListener {

    private static final Logger logger = LoggerFactory.getLogger(StartupListener.class);

    @Override
    public void contextInitialized(ServletContextEvent event) {
        logger.info("load startup listener...");
        // 获取Servlet上下文
        ServletContext sct = event.getServletContext();
        // 获取Spring应用上下文
        ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(sct);
        // 静态常量持有Spring上下文
        SpringUtil.initial(sct, ctx);

        // 加载算法
        FaceRecognitionAlgorithmLoader.getInstance().loadAlgorithmLibrary();
        logger.info("load algorithm ok");

        // 初始化zk
        LeaderService leaderService = getServiceBean(LeaderServiceImpl.class);
        assert leaderService != null;
        leaderService.init();

        // 启动上电流程
        WorkflowControllerService workflowControllerService = SpringUtil.getServiceBean(WorkflowControllerServiceImpl
                .class);
        assert workflowControllerService != null;
        workflowControllerService.start();

        // 上电完成之后再启动指令执行线程

        CommandExecutor commandExecutor = getServiceBean(CommandExecutor.class);
        assert commandExecutor != null;
        commandExecutor.init();

        logger.info("load startup listener succeed!");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        logger.info("contextDestroyed start ...");
        long loadBeginTime = System.currentTimeMillis();
        logger.info("start destroyed faceSearchNative...");

        // 获取workflow service
        WorkflowControllerService workflowControllerService = getServiceBean(WorkflowControllerServiceImpl.class);
        assert workflowControllerService != null;

        // 测试节点启动
        workflowControllerService.stop();

        logger.info("end  destroyed faceSearchNative costs: {} ms!", (System.currentTimeMillis() - loadBeginTime));
        logger.info("contextDestroyed finish ...");
    }
}
