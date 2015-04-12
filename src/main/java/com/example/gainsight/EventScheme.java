package com.example.gainsight;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.List;

import backtype.storm.spout.Scheme;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class EventScheme implements Scheme  {

  
  public static final String TIME  = "time";
  public static final String USER_ID   = "userid";
  public static final String EVENT = "event";
  public static final String EVENT_TYPE = "eventyype";
  public static final String SKU  = "sku";
  public static final String PRICE   = "price";
  public static final String SS   = "ss";
  public static final String ORDER_ID   = "orderid";
  
  public List<Object> deserialize(byte[] bytes) {
    // TODO Auto-generated method stub
    try 
    {
String truckEvent = new String(bytes, "UTF-8");
String[] pieces = truckEvent.split("\\|");

Timestamp time = Timestamp.valueOf(pieces[0]);
String userid = pieces[1];
String event = pieces[2];
String eventtype = pieces[3];
String sku= pieces[4];
String price  = pieces[5];
String ss  = pieces[6];
String orderid  = pieces[7];

return new Values(cleanup(time.toString()), cleanup(userid), 
  event, cleanup(eventtype), cleanup(sku), cleanup(price),cleanup(ss),cleanup(orderid));

} 
    catch (UnsupportedEncodingException e) 
    {
        //LOG.error(e);
        throw new RuntimeException(e);
}
  }

  public Fields getOutputFields() {
    // TODO Auto-generated method stub
    return new Fields(TIME,
      USER_ID,
     EVENT, 
     EVENT_TYPE, 
     SKU, 
     PRICE,SS,ORDER_ID);

  }

  private String cleanup(String str)
  {
      if (str != null)
      {
          return str.trim().replace("\n", "").replace("\t", "");
      } 
      else
      {
          return str;
      }
      
  }
  
}
