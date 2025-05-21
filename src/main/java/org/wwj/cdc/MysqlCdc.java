package org.wwj.cdc;

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.source.MySqlSourceBuilder;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Properties;

/**
 * 项目名称：wwj-mysql-flink-cdc
 * 类名称：MysqlCdc
 * 类描述：
 * 创建人：wuwenjin
 * 创建时间：2024/12/17
 * 修改人：
 * 修改时间：
 * 修改备注：
 *
 * @version 1.0
 */
@Component
public class MysqlCdc implements ApplicationRunner, Serializable {
    @Override
    public void run(ApplicationArguments arg0) throws Exception {

        Properties debeziumProperties = new Properties();
        debeziumProperties.put("decimal.handling.mode", "String");
        // 日期格式后到处理
        MySqlSourceBuilder<String> sourceBuilder = MySqlSource.<String>builder()
                .hostname("localhost")
                .port(3306)
                .databaseList("wwj")
                .tableList("wwj.studnet")
                .username("root")
                .password("111111")
                .scanNewlyAddedTableEnabled(true)
                .debeziumProperties(debeziumProperties)
                .deserializer(new JsonDebeziumDeserializationSchema());

        Configuration configuration = new Configuration();
        // 从最新位置开始获取日志
        sourceBuilder.startupOptions(StartupOptions.latest());
        // 避免flink集群akka超时
        configuration.setString("akka.ask.timeout", "120s");
        configuration.setString("web.timeout", "300000");
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(configuration);
        // 设置 checkpoint 保存频次 30S/次
        env.enableCheckpointing(30000);
        // 设置checkpoint路径
        env.getCheckpointConfig().setCheckpointStorage("file:///E:/ff-Log");
        // 配置数据源，设置并行度
        DataStreamSource<String> streamSource = env
                .fromSource(sourceBuilder.build(), WatermarkStrategy.noWatermarks(), "mysql-cdc-source")
                .setParallelism(4);
        streamSource.print();
        try {
            env.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
