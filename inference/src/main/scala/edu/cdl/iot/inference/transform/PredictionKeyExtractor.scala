package edu.cdl.iot.inference.transform

import edu.cdl.iot.protocol.Prediction.Prediction
import org.apache.flink.streaming.connectors.pulsar.partitioner.PulsarKeyExtractor

class PredictionKeyExtractor extends PulsarKeyExtractor[Prediction] {
//  override def serializeKey(element: SensorData): Array[Byte] = s"${element.timestamp}_${element.sensorId}".getBytes()
//
//  override def getTopic(element: SensorData): String = "persistent://sample/standalone/ns1/events"
  override def getKey(in: Prediction): String = s"${in.timestamp}_${in.sensorId}"
}
