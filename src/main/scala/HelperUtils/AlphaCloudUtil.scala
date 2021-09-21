package HelperUtils

import Simulations.AlphaCloud.{logger, seed}
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.datacenters.Datacenter
import org.cloudbus.cloudsim.distributions.UniformDistr

class AlphaCloudUtil 

object AlphaCloudUtil:
  /*
  * Broker filtered list of Datacenter Based on their infomation (@See CloudModelEnum) For type of model
  * Then Broker chooses a random datacenter from a filtered list
  */
  def mapBrokerToDatacenter(brokerSimple: DatacenterBrokerSimple, datacenterList: List[Datacenter], cloud: CloudModelEnum.CloudModelEnum): Unit = {
    val filteredDcList = datacenterList.filter(dc => {
      dc.getName == cloud;
    })

    val rand = new UniformDistr(0, filteredDcList.size, seed);
    logger.info(s"${brokerSimple.getName} Chooses: $cloud, Available Datacenters: $filteredDcList");
    brokerSimple.setDatacenterMapper((dc, vm) => filteredDcList(rand.sample().toInt));
  }
