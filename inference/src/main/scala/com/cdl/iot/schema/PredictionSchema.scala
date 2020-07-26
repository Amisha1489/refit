package com.cdl.iot.schema

import cdl.iot.dto.Prediction.Prediction
import org.apache.flink.api.common.serialization.{DeserializationSchema, SerializationSchema}
import org.apache.flink.api.common.typeinfo.TypeInformation

class PredictionSchema extends SerializationSchema[Prediction] with DeserializationSchema[Prediction] {
  override def serialize(element: Prediction): Array[Byte] = element.toProtoString.getBytes

  override def deserialize(message: Array[Byte]): Prediction = Prediction.parseFrom(message)

  override def isEndOfStream(nextElement: Prediction): Boolean = true

  override def getProducedType: TypeInformation[Prediction] = TypeInformation.of(classOf[Prediction])
}
