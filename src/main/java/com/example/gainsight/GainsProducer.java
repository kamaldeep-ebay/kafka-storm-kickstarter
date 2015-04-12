package com.example.gainsight;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.ResourceBundle;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import org.apache.log4j.Logger;

import com.sun.research.ws.wadl.Resource;

public class GainsProducer {

  //private static final Logger LOG = Logger.getLogger(GainsProducer.class);

  
  public static void main(String[] args) throws IOException {
    // TODO Auto-generated method stub

    Properties props = new Properties();
   
    String broker = "localhost:9092";
    String zk = "localhost:2181";
   
    props.put("metadata.broker.list", broker);  
    props.put("zk.connect", zk);  
    props.put("serializer.class", "kafka.serializer.StringEncoder");  
    props.put("request.required.acks", "1");
    props.put("replica.fetch.max.bytes", "1048576");
    
    
    String TOPIC = "gainsightevents";  
    
    ProducerConfig config = new ProducerConfig(props);  
    Producer<String, String> producer = new Producer<String, String>(config);
    //KeyedMessage<String, String> km = new KeyedMessage<String, String>(TOPIC, "first event testing");
  
    //producer.send(km);
    
    File f = new File("/Users/kamalsingh/Downloads/events_feed.csv");
   // File f = new File("/tmp/events_feed.csv");
    BufferedReader reader=null;
    try {
      reader=new BufferedReader(new InputStreamReader(new FileInputStream(f)));
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    // temporary restirct
    int count=0;
    while(reader.readLine()!=null && count<1000 )
    {
      KeyedMessage<String, String> sendm = new KeyedMessage<String, String>(TOPIC, reader.readLine());
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      producer.send(sendm);
      count++;
    }
    
    //KeyedMessage<String, String> data = new KeyedMessage<String, String>(TOPIC, finalEvent);
    
  }
  
  

}
