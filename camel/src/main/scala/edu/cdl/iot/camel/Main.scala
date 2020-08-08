package edu.cdl.iot.camel

import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.pulsar.PulsarComponent
import org.apache.camel.impl.{DefaultCamelContext, SimpleRegistry}

import org.apache.pulsar.client.api.PulsarClient

import scala.collection.immutable.HashMap
import collection.JavaConverters.mapAsJavaMapConverter

object Main {
  def main(args: Array[String]): Unit = {
    val client = PulsarClient.builder().serviceUrl("pulsar://127.0.0.1:6650").build()
    val registry = new SimpleRegistry
    registry.put("pulsarClient", client)

    val camel = new DefaultCamelContext()
    val pulCompo = new PulsarComponent(camel)
    pulCompo.setPulsarClient(client)
    camel.addComponent("pulsar", pulCompo)
    camel.addRoutes(new RouteBuilder() {
      @throws[Exception]
      def configure(): Unit = {
        from("pulsar:persistent://sample/standalone/ns1/sensors")
          .log(s"Recieved: ${body()}")
        //.to(s"file:${System.getProperty("user.dir")}/camel/src/main/scala/edu/cdl/iot/camel/data/outbox/test.txt")
      }
    })

    camel.start()

    Thread.sleep(10000)

    camel.stop()
    client.close()
  }
}
