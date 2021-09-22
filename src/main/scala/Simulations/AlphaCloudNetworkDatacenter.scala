package Simulations

import HelperUtils.{AlphaCloudletScheduler, CloudModelEnum}
import Simulations.AlphaCloud.{config, logger}
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyRoundRobin
import org.cloudbus.cloudsim.cloudlets.network.{CloudletExecutionTask, CloudletReceiveTask, CloudletSendTask, CloudletTask, NetworkCloudlet}
import org.cloudbus.cloudsim.core.{CloudSim, Simulation}
import org.cloudbus.cloudsim.datacenters.network.NetworkDatacenter
import org.cloudbus.cloudsim.hosts.Host
import org.cloudbus.cloudsim.hosts.network.NetworkHost
import org.cloudbus.cloudsim.network.switches.EdgeSwitch
import org.cloudbus.cloudsim.provisioners.{PeProvisionerSimple, ResourceProvisionerSimple}
import org.cloudbus.cloudsim.resources.{Pe, PeSimple}
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic
import org.cloudbus.cloudsim.vms.network.NetworkVm

import collection.JavaConverters.*

class AlphaCloudNetworkDatacenter

object AlphaCloudNetworkDatacenter:

  /*
  * Create A Network Datacenters.
  */
  def createNetworkDatacenter(simulation: CloudSim): NetworkDatacenter = {
    val networkHostList = createHostList(config.getInt("AlphaCloud.host.Amount"));
    val networkDatacenter = new NetworkDatacenter(simulation, networkHostList.asJava, new VmAllocationPolicyRoundRobin());
    networkDatacenter.setSchedulingInterval(config.getDouble("AlphaCloud.host.SchedulingInterval"));
    networkDatacenter.setName(CloudModelEnum.IAAS);
    networkDatacenter.getCharacteristics()
      .setCostPerBw(config.getDouble("AlphaCloud.IaaS.CostPerBW"))
      .setCostPerMem(config.getDouble("AlphaCloud.IaaS.CostPerMem"))
      .setCostPerSecond(config.getDouble("AlphaCloud.IaaS.CostPerSecond"))
      .setCostPerStorage(config.getDouble("AlphaCloud.IaaS.CostPerStorage"))

    createNetwork(simulation, networkDatacenter);
    logger.info(s"Created Network Host List: $networkHostList");
    logger.info(s"Created Network DataCenter: $networkDatacenter");
    return networkDatacenter;
  }

  /*
  * Create A switch for the iaas datacenters
  * Then Connect all the host to one switch
  */
  def createNetwork(simulation: CloudSim, datacenter: NetworkDatacenter): Unit = {
    val newEdge = new EdgeSwitch(simulation, datacenter);
    datacenter.addSwitch(newEdge);

    datacenter.getHostList.forEach((host:NetworkHost) => {
      newEdge.connectHost(host);
    });
  }

  def createHostList(hostAmount:Int, hostList:List[NetworkHost] = List[NetworkHost]()): List[NetworkHost] = {
    if (hostAmount <= 0) {
      return hostList.reverse
    } else {
      val newHostList:List[NetworkHost] = createNetworkHost() :: hostList;
      createHostList(hostAmount - 1, newHostList);
    }
  }

  /*
  * Create Cloudlet List And Assign Vm in descending order
  */
  def createNetworkCloudletList(vmList:List[NetworkVm]) : List[NetworkCloudlet] = {
    def createNetworkCloudletList_helper(amount:Int, networkCCList:List[NetworkCloudlet] = List[NetworkCloudlet]()) : List[NetworkCloudlet] = {
      if (amount <= 0) {
        return networkCCList.reverse;
      } else {
        val newNetworkCC = createNetworkCloudlet(vmList(amount % vmList.size)) :: networkCCList;
        return createNetworkCloudletList_helper(amount - 1, newNetworkCC);
      }
    }

    val networkVmList = createNetworkCloudletList_helper(config.getInt("AlphaCloud.cloudlet.Amount"));
    logger.info(s"Created Network Cloudlet List: $networkVmList");
    createTaskNetworkCloudlets(networkVmList);
    return networkVmList;
  }

  /*
  * Add network for the network cloudlet
  * network cloudlet 1 will send to 2, and 2 will send to 3, etc..
  */
  private def createTaskNetworkCloudlets(networkCCList: List[NetworkCloudlet]): Unit = {
    networkCCList.foreach(nce => {
      addExecutionTask(nce);
    });

    networkCCList.reduce( (nce1, nce2) => {
      addSendTask(nce1, nce2);
      addRecieveTask(nce2, nce1);
      return nce2;
    })
  }

  /*
  * Add  execution task to the list in network cloud let
  */
  private def addExecutionTask(cloudlet:NetworkCloudlet): Unit = {
    val task = new CloudletExecutionTask(cloudlet.getTasks.size(), config.getLong("AlphaCloud.cloudlet.length"));
    task.setMemory(config.getLong("AlphaCloud.cloudlet.RAMInMBs"));
    cloudlet.addTask(task);
  }

  /*
  * Add reieve Task to the list in network cloudlet
  */
  private def addRecieveTask(cloudlet: NetworkCloudlet, sourceCloudlet: NetworkCloudlet): Unit = {
    val task = new CloudletReceiveTask(cloudlet.getTasks.size(), sourceCloudlet.getVm());
    task.setMemory(config.getLong("AlphaCloud.cloudlet.RAMInMBs"));
    task.setExpectedPacketsToReceive(config.getInt("AlphaCloud.networkCloudlet.packetAmount"));
    cloudlet.addTask(task);
  }

  /*
  * Add send task to the list in networkcloudlet
  */
  private def addSendTask(sourceCloudlet:NetworkCloudlet, destinationCloudlet:NetworkCloudlet): Unit = {
    val task = new CloudletSendTask(sourceCloudlet.getTasks.size());
    task.setMemory(config.getLong("AlphaCloud.cloudlet.RAMInMBs"));
    sourceCloudlet.addTask(task);

    def addPacket(task:CloudletSendTask, destinationCloudlet:NetworkCloudlet, numPacket: Int, packetLength: Long): Unit = {
      if (numPacket > 0) {
        task.addPacket(destinationCloudlet, packetLength);
        addPacket(task, destinationCloudlet, numPacket - 1, packetLength);
      }
    }

    val numPacket = config.getInt("AlphaCloud.networkCloudlet.packetAmount");
    val packetLength = config.getLong("AlphaCloud.networkCloudlet.packetLength");
    addPacket(task, destinationCloudlet, numPacket, packetLength);
  }

  def createNetworkVmList() : List[NetworkVm] = {
    def createNetworkVmList_helper(amount:Int, networkVmList: List[NetworkVm] = List[NetworkVm]()) : List[NetworkVm] = {
      if (amount <= 0) {
        return networkVmList.reverse;
      } else {
        val newNetworkVmList = createNetworkVm() :: networkVmList;
        createNetworkVmList_helper(amount - 1, newNetworkVmList);
      }
    }

    val networkVmList = createNetworkVmList_helper(config.getInt("AlphaCloud.vm.Amount"))
    logger.info(s"Created network vm list: $networkVmList");
    return networkVmList;
  }

  def createNetworkVm(): NetworkVm = {
    val networkVm:NetworkVm = new NetworkVm (
      config.getLong("AlphaCloud.vm.mipsCapacity"),
      config.getInt("AlphaCloud.vm.PEs")
    );

    networkVm
      .setRam(config.getLong("AlphaCloud.vm.RAMInMBs"))
      .setBw(config.getLong("AlphaCloud.vm.BandwidthInMBps"))
      .setSize(config.getLong("AlphaCloud.vm.StorageInMBs"))
      .setCloudletScheduler(new AlphaCloudletScheduler()); // default cloudlet scheduler

    networkVm.enableUtilizationStats();
    return networkVm;
  }

  def createNetworkCloudlet(vm: NetworkVm): NetworkCloudlet = {
    val utilizationModel = new UtilizationModelDynamic(config.getDouble("AlphaCloud.utilizationRatio"));

    val networkCloudlet = new NetworkCloudlet(config.getLong("AlphaCloud.cloudlet.length"), config.getInt("AlphaCloud.cloudlet.PEs"));
    networkCloudlet
      .setMemory(config.getLong("AlphaCloud.cloudlet.RAMInMBs"))
      .setFileSize(config.getLong("AlphaCloud.cloudlet.fileSize"))
      .setOutputSize(config.getLong("AlphaCloud.cloudlet.outputSize"))
      .setUtilizationModel(utilizationModel)
      .setVm(vm)
      .setBroker(vm.getBroker())

    return networkCloudlet;
  }

  def createPEList(peAmount:Int, peList:List[Pe] = List[Pe]()) : List[Pe] = {
    if (peAmount <= 0) {
      return peList.reverse;
    } else {
      val newPeList : List[Pe] = new PeSimple(config.getLong("AlphaCloud.host.mipsCapacity"), new PeProvisionerSimple()) :: peList;
      createPEList(peAmount - 1, newPeList);
    }
  }

  def createNetworkHost(): NetworkHost = {
    val peList = createPEList(config.getInt("AlphaCloud.host.PEs"));
    val networkHost:NetworkHost = new NetworkHost(
      config.getLong("AlphaCloud.host.RAMInMBs"),
      config.getLong("AlphaCloud.host.BandwidthInMBps"),
      config.getLong("AlphaCloud.host.StorageInMBs"),
      peList.asJava
    );

    networkHost
      .setVmScheduler(new VmSchedulerTimeShared())
      .setRamProvisioner(new ResourceProvisionerSimple())
      .setBwProvisioner(new ResourceProvisionerSimple())

    return networkHost;
  }

