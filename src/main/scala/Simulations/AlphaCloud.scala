package Simulations

import HelperUtils.*
import HelperUtils.AlphaCloudUtil.mapBrokerToDatacenter
import Simulations.AlphaCloudNetworkDatacenter.{createNetworkCloudletList, createNetworkDatacenter, createNetworkVmList}
import org.cloudbus.cloudsim.allocationpolicies.{VmAllocationPolicyRandom, VmAllocationPolicyRoundRobin, VmAllocationPolicySimple}
import org.cloudbus.cloudsim.brokers.{DatacenterBrokerBestFit, DatacenterBrokerFirstFit, DatacenterBrokerHeuristic, DatacenterBrokerSimple}
import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletSimple}
import org.cloudbus.cloudsim.core.{CloudSim, Simulation}
import org.cloudbus.cloudsim.datacenters.{Datacenter, DatacenterSimple}
import org.cloudbus.cloudsim.distributions.UniformDistr
import org.cloudbus.cloudsim.hosts.{Host, HostSimple}
import org.cloudbus.cloudsim.power.models.{PowerModelHostSimple, PowerModelHostSpec}
import org.cloudbus.cloudsim.provisioners.{PeProvisionerSimple, ResourceProvisionerSimple}
import org.cloudbus.cloudsim.resources.{Pe, PeSimple}
import org.cloudbus.cloudsim.schedulers.cloudlet.{CloudletSchedulerCompletelyFair, CloudletSchedulerSpaceShared, CloudletSchedulerTimeShared}
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic
import org.cloudbus.cloudsim.vms.{Vm, VmCost, VmSimple}
import org.cloudsimplus.builders.tables.CloudletsTableBuilder

import collection.JavaConverters.*

class AlphaCloud

object AlphaCloud:
  val config = ObtainConfigReference("AlphaCloud") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }
  val logger = CreateLogger(classOf[VmAllocRR]);
  val seed = config.getInt("AlphaCloud.seed");
  /*
  * Start Alpha simulation.
  * 1. Create different types of datacenters (different model)
  * 2. Create brokers which will eventually get map into the correct datacenter
  * 3. Finish simulation and print out the result and price
  */
  def alphaCloud() : Unit = {
    val simulation = new CloudSim();

    val saasDatacenter = createSaasDatacenters(simulation);
    val paasDatacenter = createPaasDatacenters(simulation);
    val iaasDatacenter = createNetworkDatacenter(simulation);

    // Create Datacenter List
    val datacenterList = List(saasDatacenter, paasDatacenter, iaasDatacenter);

    // Saas Broker
    val saasBroker = new DatacenterBrokerSimple(simulation, "Saas_Broker");
    val saasVmList = createVmList(config.getInt("AlphaCloud.vm.Amount"));
    logger.info(s"Created Vm List: $saasVmList");
    val saasCloudletList = createCloudLetList(config.getInt("AlphaCloud.cloudlet.Amount"));
    logger.info(s"Created Cloudlet List: $saasCloudletList");
    mapBrokerToDatacenter(saasBroker, datacenterList, CloudModelEnum.SAAS);
    saasBroker.submitVmList(saasVmList.asJava);
    saasBroker.submitCloudletList(saasCloudletList.asJava);

    // Paas Broker (AlphaCloudlet Scheduler)
    val paasBroker = new DatacenterBrokerSimple(simulation, "Paas_Broker");
    val paasVmList = createVmList(config.getInt("AlphaCloud.vm.Amount"));
    logger.info(s"Created Vm List: $paasVmList");
    val paasCloudletList = createCloudLetList(config.getInt("AlphaCloud.cloudlet.Amount"));
    logger.info(s"Created Cloudlet List: $paasCloudletList");
    mapBrokerToDatacenter(paasBroker, datacenterList, CloudModelEnum.PAAS);
    paasBroker.submitVmList(paasVmList.asJava);
    paasBroker.submitCloudletList(paasCloudletList.asJava);

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

    // IAAS Broker
    val iaasBroker = new DatacenterBrokerSimple(simulation, "Iaas_Broker");
    val iaasVmList = createNetworkVmList();
    val iaasCCList = createNetworkCloudletList(iaasVmList);
    mapBrokerToDatacenter(iaasBroker, datacenterList, CloudModelEnum.IAAS);
    iaasBroker.submitVmList(iaasVmList.asJava);
    iaasBroker.submitCloudletList(iaasCCList.asJava);

    // Start simulation
    simulation.start();

    // Print out the cost
    val saasCostList = saasVmList.map(vm => {
      val cost = VmCost(vm);
      cost.getTotalCost();
    });

    val paasCostList = paasVmList.map(vm => {
      val cost = VmCost(vm);
      cost.getTotalCost();
    });

    val paasCostListV2 = paasVmListV2.map(vm => {
      val cost = VmCost(vm);
      cost.getTotalCost();
    });

    val iaasCost = iaasVmList.map(vm => {
      val cost = VmCost(vm);
      cost.getTotalCost();
    })

    // Print Table Result
    new CloudletsTableBuilder(saasBroker.getCloudletFinishedList()).build();
    val softwareFee = config.getDouble("AlphaCloud.SaaS.softwareFeesPerSec");

    logger.info("SAAS BROKER TABLE");
    logger.info(s"Totat Cost for SAAS is: ${saasCostList.sum + (saasBroker.getShutdownTime * softwareFee)}");

    new CloudletsTableBuilder(paasBroker.getCloudletFinishedList()).build();
    logger.info("PAAS BROKER TABLE");
    logger.info(s"Totat Cost for PAAS is: ${paasCostList.sum}")

    new CloudletsTableBuilder(paasBrokerV2.getCloudletFinishedList()).build();
    logger.info("PAAS BROKER TABLE (TIME SHARE CLOUDLET SCHEDULER)");
    logger.info(s"Totat Cost for PAAS(TIMESHARE) is: ${paasCostListV2.sum}")

    new CloudletsTableBuilder(iaasBroker.getCloudletFinishedList()).build();
    logger.info("IAAS NETWORK DATACENTER");
    logger.info(s"Totat Cost for IAAS: ${iaasCost.sum}")
  }

  /*
  * Create Paas with the Round robin vm allocation policy
  */
  def createPaasDatacenters(simulation: Simulation) : Datacenter = {
    val hostList = createHostList(config.getInt("AlphaCloud.host.Amount"));
    logger.info(s"Created Host List: $hostList");

    val datacenter = new DatacenterSimple(simulation, hostList.asJava, new VmAllocationPolicyRoundRobin());
    datacenter.getCharacteristics()
      .setCostPerBw(config.getDouble("AlphaCloud.PaaS.CostPerBW"))
      .setCostPerMem(config.getDouble("AlphaCloud.PaaS.CostPerMem"))
      .setCostPerSecond(config.getDouble("AlphaCloud.PaaS.CostPerSecond"))
      .setCostPerStorage(config.getDouble("AlphaCloud.PaaS.CostPerStorage"))

    datacenter.setName(CloudModelEnum.PAAS);
    logger.info(s"Created PaaS Datacenter $datacenter");
    return datacenter;
  }

  /*
  * Create Saas with the default vmallocationpolicy
  */
  def createSaasDatacenters(simulation: Simulation) : Datacenter = {
    val hostList = createHostList(config.getInt("AlphaCloud.host.Amount"));
    logger.info(s"Created Host List: $hostList");

    val datacenter = new DatacenterSimple(simulation, hostList.asJava, new VmAllocationPolicySimple());
    datacenter.getCharacteristics()
      .setCostPerBw(config.getDouble("AlphaCloud.SaaS.CostPerBW"))
      .setCostPerMem(config.getDouble("AlphaCloud.SaaS.CostPerMem"))
      .setCostPerSecond(config.getDouble("AlphaCloud.SaaS.CostPerSecond"))
      .setCostPerStorage(config.getDouble("AlphaCloud.PaaS.CostPerStorage"))

    datacenter.setName(CloudModelEnum.SAAS);
    datacenter.setSchedulingInterval(config.getDouble("AlphaCloud.host.SchedulingInterval"))
    logger.info(s"Created SaaS Datacenter $datacenter");
    return datacenter;
  }

  def createPEList(peAmount:Int, peList:List[Pe] = List[Pe]()) : List[Pe] = {
    if (peAmount <= 0) {
      return peList.reverse;
    } else {
      val newPeList : List[Pe] = new PeSimple(config.getLong("AlphaCloud.host.mipsCapacity"), new PeProvisionerSimple()) :: peList;
      createPEList(peAmount - 1, newPeList);
    }
  }

  def createHostList(hostAmount:Int, hostList:List[Host] = List[Host]()): List[Host] = {
    if (hostAmount <= 0) {
      return hostList.reverse
    } else {
      val newHostList:List[Host] = createHost() :: hostList;
      createHostList(hostAmount - 1, newHostList);
    }
  }

  def createVmList(vmAmount:Int, vmList:List[Vm] = List[Vm]()) : List[Vm] = {
    if (vmAmount <= 0) {
      return vmList.reverse;
    } else {
      val newVmList = createVm() :: vmList;
      createVmList(vmAmount - 1, newVmList);
    }
  }

  def createCloudLetList(ccAmount:Int, ccList:List[Cloudlet] = List[Cloudlet]()) : List[Cloudlet] = {
    val utilizationModel = new UtilizationModelDynamic(config.getDouble("AlphaCloud.utilizationRatio"));

    if (ccAmount <= 0) {
      return ccList.reverse;
    } else {
      val newCCList = createCloudLet() :: ccList;
      createCloudLetList(ccAmount - 1, newCCList);
    }
  }

  def createHost() : Host = {
    val host = new HostSimple(
      config.getLong("AlphaCloud.host.RAMInMBs"),
      config.getLong("AlphaCloud.host.BandwidthInMBps"),
      config.getLong("AlphaCloud.host.StorageInMBs"),
      createPEList(config.getInt("AlphaCloud.host.PEs")).asJava,
    )
      .setVmScheduler(new VmSchedulerTimeShared())
      .setRamProvisioner(new ResourceProvisionerSimple())
      .setBwProvisioner(new ResourceProvisionerSimple())

    return host;
  }

  def createVm() : Vm = {
    val newVm = new VmSimple(
      config.getLong("AlphaCloud.vm.mipsCapacity"),
      config.getLong("AlphaCloud.vm.PEs")
    )
      .setRam(config.getLong("AlphaCloud.vm.RAMInMBs"))
      .setBw(config.getLong("AlphaCloud.vm.BandwidthInMBps"))
      .setSize(config.getLong("AlphaCloud.vm.StorageInMBs"))
      .setCloudletScheduler(new AlphaCloudletScheduler()); // default cloudlet scheduler

    newVm.enableUtilizationStats();
    return newVm;
  }

  def createCloudLet() : Cloudlet = {
    val utilizationModel = new UtilizationModelDynamic(config.getDouble("AlphaCloud.utilizationRatio"));

    return new CloudletSimple(
      config.getLong("AlphaCloud.cloudlet.length"),
      config.getInt("AlphaCloud.cloudlet.PEs"),
    )
      .setFileSize(config.getLong("AlphaCloud.cloudlet.fileSize"))
      .setOutputSize(config.getLong("AlphaCloud.cloudlet.outputSize"))
      .setUtilizationModel(utilizationModel)
  }
