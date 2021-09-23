package Simulations

import HelperUtils.AlphaCloudUtil.mapBrokerToDatacenter
import HelperUtils.CloudModelEnum
import Simulations.AlphaCloud.{config, createCloudLetList, createPaasDatacenters, createSaasDatacenters, createVmList, logger}
import Simulations.AlphaCloudNetworkDatacenter.{createNetworkCloudletList, createNetworkDatacenter, createNetworkVmList}
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared
import org.cloudbus.cloudsim.vms.VmCost
import org.cloudsimplus.builders.tables.CloudletsTableBuilder

import collection.JavaConverters.*

class DeltaCloud

object DeltaCloud:

  /*
  * Timeshare Simulation for Paas DataCenter
  * Only use PaaS Datacenter
  */
  def deltaCloud(): Unit = {
    val simulation = new CloudSim();

    val saasDatacenter = createSaasDatacenters(simulation);
    val paasDatacenter = createPaasDatacenters(simulation);
    val iaasDatacenter = createNetworkDatacenter(simulation);

    // Create Datacenter List
    val datacenterList = List(saasDatacenter, paasDatacenter, iaasDatacenter);

    // Paas Timeshare Broker (Use for comparision with Paas Broker)
    val paasBrokerV2 = new DatacenterBrokerSimple((simulation), "Paas_Broker(TimeShare)");
    val paasVmListV2 = createVmList(config.getInt("AlphaCloud.vm.Amount"));
    logger.info(s"Created Vm List: $paasVmListV2");
    val paasCloudletListV2 = createCloudLetList(config.getInt("AlphaCloud.cloudlet.Amount"));
    logger.info(s"Created Cloudlet List: $paasCloudletListV2");
    mapBrokerToDatacenter(paasBrokerV2, datacenterList, CloudModelEnum.PAAS);
    paasVmListV2.foreach(vm => {vm.setCloudletScheduler(new CloudletSchedulerTimeShared())});
    paasBrokerV2.submitVmList(paasVmListV2.asJava);
    paasBrokerV2.submitCloudletList(paasCloudletListV2.asJava);

    // Start simulation
    simulation.start();

    val paasCostListV2 = paasVmListV2.map(vm => {
      val cost = VmCost(vm);
      cost.getTotalCost();
    });

    // Print Table Result
    new CloudletsTableBuilder(paasBrokerV2.getCloudletFinishedList()).build();
    logger.info("PAAS BROKER TABLE (TIME SHARE CLOUDLET SCHEDULER)");
    logger.info(s"Totat Cost for PAAS(TIMESHARE) is: ${paasCostListV2.sum}")
  }

