package net.manub.embeddedkafka.streams

import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig, UUIDs}
import org.apache.kafka.streams.{KafkaStreams, Topology}
import org.apache.log4j.Logger
import org.scalatest.Suite

/** Helper trait for testing Kafka Streams.
  * It creates an embedded Kafka Instance for each test case.
  * Use `runStreams` to execute your streams.
  */
trait EmbeddedKafkaStreams extends EmbeddedKafka with TestStreamsConfig {
  this: Suite =>

  private val logger = Logger.getLogger(classOf[EmbeddedKafkaStreams])

  /** Execute Kafka streams and pass a block of code that can
    * operate while the streams are active.
    * The code block can be used for publishing and consuming messages in Kafka.
    *
    * @param topicsToCreate the topics that should be created in Kafka before launching the streams.
    * @param topology       the streams topology that will be used to instantiate the streams with
    *                       a default configuration (all state directories are different and
    *                       in temp folders)
    * @param extraConfig    additional KafkaStreams configuration (overwrite existing keys in
    *                       default config)
    * @param block          the code block that will executed while the streams are active.
    *                       Once the block has been executed the streams will be closed.
    */
  def runStreams(topicsToCreate: Seq[String],
                 topology: Topology,
                 extraConfig: Map[String, AnyRef] = Map.empty)(block: => Any)(
      implicit config: EmbeddedKafkaConfig): Any =
    withRunningKafka {
      topicsToCreate.foreach(topic => createCustomTopic(topic))
      val streamId = UUIDs.newUuid().toString
      logger.debug(s"Creating stream with Application ID: [$streamId]")
      val streams =
        new KafkaStreams(topology, streamConfig(streamId, extraConfig))
      streams.start()
      try {
        block
      } finally {
        streams.close()
      }
    }(config)
}
