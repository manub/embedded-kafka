package net.manub.embeddedkafka

class EmbeddedKafkaWithRunningKafkaOnFoundPortSpec
    extends EmbeddedKafkaSpecSupport
    with EmbeddedKafka {

  "the withRunningKafkaOnFoundPort method" should {
    "start and stop Kafka and Zookeeper successfully on non-zero ports" in {
      val userDefinedConfig = EmbeddedKafkaConfig(kafkaPort = 12345, zooKeeperPort = 12346)
      val actualConfig = withRunningKafkaOnFoundPort(userDefinedConfig) { actualConfig =>
        actualConfig shouldBe userDefinedConfig
        bothKafkaAndZkAreAvailable(actualConfig)
        actualConfig
      }
      bothKafkaAndZkAreNotAvailable(actualConfig)
    }

    "start and stop multiple Kafka and Zookeeper successfully on arbitrary available ports" in {
      val userDefinedConfig = EmbeddedKafkaConfig(kafkaPort = 0, zooKeeperPort = 0)
      val actualConfig1 = withRunningKafkaOnFoundPort(userDefinedConfig) { actualConfig1 =>
        bothKafkaAndZkAreAvailable(actualConfig1)
        publishStringMessageToKafka("topic", "message1")(actualConfig1)
        consumeFirstStringMessageFrom("topic")(actualConfig1) shouldBe "message1"
        val actualConfig2 = withRunningKafkaOnFoundPort(userDefinedConfig) { actualConfig2 =>
          bothKafkaAndZkAreAvailable(actualConfig2)
          publishStringMessageToKafka("topic", "message2")(actualConfig2)
          consumeFirstStringMessageFrom("topic")(actualConfig2) shouldBe "message2"
          val allConfigs = Seq(userDefinedConfig, actualConfig1, actualConfig2)
          // Confirm both actual configs are running on separate non-zero ports, but otherwise equal
          allConfigs.map(_.kafkaPort).distinct should have size 3
          allConfigs.map(_.zooKeeperPort).distinct should have size 3
          allConfigs.map(_.copy(kafkaPort = 0, zooKeeperPort = 0)).distinct should have size 1
          actualConfig2
        }
        bothKafkaAndZkAreNotAvailable(actualConfig2)
        actualConfig1
      }
      bothKafkaAndZkAreNotAvailable(actualConfig1)
    }

    "work with a simple example using implicits" in {
      val userDefinedConfig = EmbeddedKafkaConfig(kafkaPort = 0, zooKeeperPort = 0)
      withRunningKafkaOnFoundPort(userDefinedConfig) { implicit actualConfig =>
        publishStringMessageToKafka("topic", "message")
        consumeFirstStringMessageFrom("topic") shouldBe "message"
      }
    }
  }

  private def bothKafkaAndZkAreAvailable(config: EmbeddedKafkaConfig): Unit = {
    kafkaIsAvailable(config.kafkaPort)
    zookeeperIsAvailable(config.zooKeeperPort)
  }

  private def bothKafkaAndZkAreNotAvailable(config: EmbeddedKafkaConfig): Unit = {
    kafkaIsNotAvailable(config.kafkaPort)
    zookeeperIsNotAvailable(config.zooKeeperPort)
  }
}
