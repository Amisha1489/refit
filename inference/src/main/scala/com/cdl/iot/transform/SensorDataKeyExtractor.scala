package com.cdl.iot.transform

import cdl.iot.dto.SensorData.SensorData
import org.apache.flink.streaming.connectors.pulsar.partitioner.PulsarKeyExtractor

class SensorDataKeyExtractor extends PulsarKeyExtractor[SensorData] {
//  override def serializeKey(element: SensorData): Array[Byte] = s"${element.timestamp}_${element.sensorId}".getBytes()
//
//  override def getTopic(element: SensorData): String = "persistent://sample/standalone/ns1/events"
  override def getKey(in: SensorData): String = s"${in.timestamp}_${in.sensorId}"
}
