package es.upv.pros.pvalderas.compositioncoordinator.bpmn;

import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.spring.ProcessEngineFactoryBean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.context.annotation.Bean;
import java.io.IOException;
import org.springframework.core.io.Resource;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.camunda.bpm.engine.spring.SpringProcessEngineServicesConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;

@Configuration
@Import({ SpringProcessEngineServicesConfiguration.class })
public class CamundaConfiguration
{
    @Value("${camunda.bpm.history-level:none}")
    private String historyLevel;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private ResourcePatternResolver resourceLoader;
    
    @Bean
    public SpringProcessEngineConfiguration processEngineConfiguration() throws IOException {
        final SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();
        config.setDataSource(this.dataSource);
        config.setDatabaseSchemaUpdate("true");
        config.setTransactionManager(this.transactionManager());
        config.setHistory(this.historyLevel);
        config.setJobExecutorActivate(true);
        config.setMetricsEnabled(false);
        final Resource[] resources = this.resourceLoader.getResources("file:" + System.getProperty("user.dir") + "/fragments/*/*.bpmn");
        System.out.println("Loaded Fragments: "+resources.length);
        config.setDeploymentResources(resources);
        return config;
    }
    
    @Bean
    public PlatformTransactionManager transactionManager() {
        return (PlatformTransactionManager)new DataSourceTransactionManager(this.dataSource);
    }
    
    @Bean
    public ProcessEngineFactoryBean processEngine() throws IOException {
        final ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration((ProcessEngineConfigurationImpl)this.processEngineConfiguration());
        return factoryBean;
    }
}
