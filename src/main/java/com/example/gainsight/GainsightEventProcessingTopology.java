package com.example.gainsight;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.storm.hdfs.bolt.HdfsBolt;
import org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat;
import org.apache.storm.hdfs.bolt.format.DelimitedRecordFormat;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.format.RecordFormat;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;
import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.TopologyBuilder;

public class GainsightEventProcessingTopology {

  
  private static final String KAFKA_SPOUT_ID = "kafkaSpout"; 
  private static final String HDFS_BOLT_ID = "hdfsBolt";
  private static final String MONITOR_BOLT_ID = "monitorBolt";
  private static final String HBASE_BOLT_ID = "hbaseBolt"; 
  private static final String LOG_TRUCK_BOLT_ID = "logGainsEventBolt";

  
  protected Properties topologyConfig;

  public GainsightEventProcessingTopology(String configFileLocation){
    
    System.setProperty("storm.jar", "/Users/kamalsingh/.m2/repository/org/apache/storm/storm-core/0.9.1.2.1.1.0-385/storm-core-0.9.1.2.1.1.0-385.jar");
    topologyConfig = new Properties();
    try {
      topologyConfig.load(ClassLoader.getSystemResourceAsStream(configFileLocation));
      System.out.println(topologyConfig.getProperty("kafka.zkRoot"));
    } catch (FileNotFoundException e) {
     // LOG.error("Encountered error while reading configuration properties: "
       //   + e.getMessage());
      //throw e;
    } catch (IOException e) {
      //LOG.error("Encountered error while reading configuration properties: "
        //  + e.getMessage());
      //throw e;
    } 

  }
  
  private SpoutConfig constructKafkaSpoutConf() 
  {
      BrokerHosts hosts = new ZkHosts(topologyConfig.getProperty("kafka.zookeeper.host.port"));
      String topic = topologyConfig.getProperty("kafka.topic");
      String zkRoot = topologyConfig.getProperty("kafka.zkRoot");
      String consumerGroupId = "StormSpout";

      SpoutConfig spoutConfig = new SpoutConfig(hosts, topic, zkRoot, consumerGroupId);

      /* Custom TruckScheme that will take Kafka message of single truckEvent 
       * and emit a 2-tuple consisting of truckId and truckEvent. This driverId
       * is required to do a fieldsSorting so that all driver events are sent to the set of bolts */
      //spoutConfig.scheme = new SchemeAsMultiScheme(new TruckScheme());

      spoutConfig.scheme = new SchemeAsMultiScheme(new EventScheme());
      
      return spoutConfig;
  }

  
  public void configureKafkaSpout(TopologyBuilder builder) 
  {
      KafkaSpout kafkaSpout = new KafkaSpout(constructKafkaSpoutConf());
      int spoutCount = Integer.valueOf(topologyConfig.getProperty("spout.thread.count"));
      builder.setSpout(KAFKA_SPOUT_ID, kafkaSpout);
  }
  
  
  
  public void buildAndSubmit() throws Exception
  {
      TopologyBuilder builder = new TopologyBuilder();
      configureKafkaSpout(builder);
      //configureLogTruckEventBolt(builder);
     // configureHDFSBolt(builder);
      
      configureHBaseBolt(builder);
      
      Config conf = new Config();
      conf.setDebug(true);
      
      StormSubmitter.submitTopology("gains-event-processor", 
                                  conf, builder.createTopology());
  }
  
  public  void configureHBaseBolt(TopologyBuilder builder) {
    // TODO Auto-generated method stub
    
  //  TruckHBaseBolt hbaseBolt = new TruckHBaseBolt(topologyConfig);
   // builder.setBolt(HBASE_BOLT_ID, hbaseBolt, 2).shuffleGrouping(KAFKA_SPOUT_ID);
    
    EventsHBaseBolt hbaseBolt = new EventsHBaseBolt(topologyConfig);
    builder.setBolt(HBASE_BOLT_ID, hbaseBolt, 2).shuffleGrouping(KAFKA_SPOUT_ID); 
  }

  public  void configureHDFSBolt(TopologyBuilder builder) {
    // TODO Auto-generated method stub
   
    String rootPath = topologyConfig.getProperty("hdfs.path");
    String prefix = topologyConfig.getProperty("hdfs.file.prefix");
    String fsUrl = topologyConfig.getProperty("hdfs.url");
    String sourceMetastoreUrl = topologyConfig.getProperty("hive.metastore.url");
    String hiveStagingTableName = topologyConfig.getProperty("hive.staging.table.name");
    String databaseName = topologyConfig.getProperty("hive.database.name");
    Float rotationTimeInMinutes = Float.valueOf(topologyConfig.getProperty("hdfs.file.rotation.time.minutes"));

    RecordFormat format = new DelimitedRecordFormat().withFieldDelimiter(",");

    //Synchronize data buffer with the filesystem every 100 tuples
    SyncPolicy syncPolicy = new CountSyncPolicy(100);

    // Rotate data files when they reach five MB
    //FileRotationPolicy rotationPolicy = new FileSizeRotationPolicy(5.0f, Units.MB);

    //Rotate every X minutes
    FileTimeRotationPolicy rotationPolicy = new FileTimeRotationPolicy
                (rotationTimeInMinutes, FileTimeRotationPolicy.Units.MINUTES);

    //Hive Partition Action
    HiveTablePartitionAction hivePartitionAction = new HiveTablePartitionAction
               (sourceMetastoreUrl, hiveStagingTableName, databaseName, fsUrl);

    //MoveFileAction moveFileAction = new MoveFileAction().toDestination(rootPath + "/working");



    FileNameFormat fileNameFormat = new DefaultFileNameFormat()
                    .withPath(rootPath + "/staging")
                    .withPrefix(prefix);

    // Instantiate the HdfsBolt
    HdfsBolt hdfsBolt = new HdfsBolt()
                     .withFsUrl(fsUrl)
             .withFileNameFormat(fileNameFormat)
             .withRecordFormat(format)
             .withRotationPolicy(rotationPolicy)
             .withSyncPolicy(syncPolicy)
             .addRotationAction(hivePartitionAction);

   // int hdfsBoltCount = Integer.valueOf(topologyConfig.getProperty("hdfsbolt.thread.count"));
    builder.setBolt(HDFS_BOLT_ID, hdfsBolt, 2).shuffleGrouping(KAFKA_SPOUT_ID);
    
  }

  public static void main(String[] args) throws Exception {
    // TODO Auto-generated method stub
    
    String configFileLocation = "gains_event_topology.properties";
    
    GainsightEventProcessingTopology gs = new GainsightEventProcessingTopology(configFileLocation);
    
    //TopologyBuilder builder = new TopologyBuilder();
    gs.buildAndSubmit();
    
    
  }

}
