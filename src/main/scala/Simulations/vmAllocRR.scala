package Simulations

import HelperUtils.{CreateLogger, ObtainConfigReference}
import Simulations.BasicCloudSimPlusExample.{config, logger}
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyRoundRobin
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletSimple}
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.{Datacenter, DatacenterSimple}
import org.cloudbus.cloudsim.hosts.{Host, HostSimple}
import org.cloudbus.cloudsim.resources.{Pe, PeSimple}
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic
import org.cloudbus.cloudsim.vms.{Vm, VmSimple}
import org.cloudsimplus.builders.tables.CloudletsTableBuilder

import collection.JavaConverters.*
import scala.::

class VmAllocRR

object VmAllocRR {
  val config = ObtainConfigReference("cloudSimulatorVmAllocRR") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }
  val logger = CreateLogger(classOf[VmAllocRR])

  def vmAllocationPolicyRoundRobin () : Unit = {
    val simulation = new CloudSim();

    val datacenter : Datacenter = createDatacenter(simulation);

    // Create Broker aka users
    val broker0 = new DatacenterBrokerSimple(simulation);

    // Create Vm And CloudLets
    val vmList = createVms();
    val cloudLetList = createCloudLets();

    // submit vm and cloudlet
    broker0.submitVmList(vmList.asJava);
    broker0.submitCloudletList(cloudLetList.asJava);

    // Start Simulation
    simulation.start();

    // Get Simulation Result
    new CloudletsTableBuilder(broker0.getCloudletFinishedList()).build();

    //Print VMs CPU Usage
    vmList.foreach(vm => {
      val vmCpuUsageMean = vm.getCpuUtilizationStats.getMean() * 100;
      logger.info(s"CPU ${vm.getId} USAGE MEAN: $vmCpuUsageMean");
    });
  }

  def createCloudLets() : List[Cloudlet] = {
    val utilizationModel = new UtilizationModelDynamic(config.getDouble("cloudSimulatorVmAllocRR.utilizationRatio"));

    def createCloudLets_helper(ccAmount:Int, ccList:List[Cloudlet] = List[Cloudlet]()) : List[Cloudlet] = {
      if (ccAmount <= 0) {
        return ccList.reverse;
      } else {
        val newCCList = new CloudletSimple(
          config.getLong("cloudSimulatorVmAllocRR.cloudlet.length"),
          config.getInt("cloudSimulatorVmAllocRR.cloudlet.PEs"),
          utilizationModel
        ) :: ccList;
        createCloudLets_helper(ccAmount - 1, newCCList);
      }
    }

    val cloudletList = createCloudLets_helper(config.getInt("cloudSimulatorVmAllocRR.cloudlet.Amount"));
    logger.info(s"Created List of CloudLets: $cloudletList");

    return cloudletList;
  }

  def createVms() : List[Vm] = {
    def createVms_helper(vmAmount:Int, vmList:List[Vm] = List[Vm]()) : List[Vm] = {
      if (vmAmount <= 0) {
        return vmList.reverse;
      } else {
        val newVmList = new VmSimple(config.getLong("cloudSimulatorVmAllocRR.vm.mipsCapacity"),
          config.getLong("cloudSimulatorVmAllocRR.vm.Pe_Amount"))
          .setRam(config.getLong("cloudSimulatorVmAllocRR.vm.RAMInMBs"))
          .setBw(config.getLong("cloudSimulatorVmAllocRR.vm.BandwidthInMBps"))
          .setCloudletScheduler(new CloudletSchedulerTimeShared())
          .setSize(config.getLong("cloudSimulatorVmAllocRR.vm.StorageInMBs")) :: vmList;
        createVms_helper(vmAmount - 1, newVmList);
      }
    }

    // Enable Stats for Vm
    val vmList:List[Vm] = createVms_helper(config.getInt("cloudSimulatorVmAllocRR.vm.Amount")).map(vm => { vm.enableUtilizationStats(); vm});
    logger.info(s"Created List Of Vm: $vmList");
    return vmList;
  }


  def createDatacenter(simulation : CloudSim) : Datacenter = {
    def createPes(peAmount:Int, peList:List[Pe] = List[Pe]()) : List[Pe] = {
      if (peAmount <= 0) {
        return peList.reverse;
      } else {
        val newPeList : List[Pe] = new PeSimple(config.getLong("cloudSimulatorVmAllocRR.host.mipsCapacity")) :: peList;
        createPes(peAmount - 1, newPeList);
      }
    }

    def createHost(hostAmount:Int, hostList:List[Host] = List[Host]()): List[Host] = {
      if (hostAmount <= 0) {
        return hostList.reverse
      } else {
        val newHostList:List[Host] = new HostSimple(
          config.getLong("cloudSimulatorVmAllocRR.host.RAMInMBs"),
          config.getLong("cloudSimulatorVmAllocRR.host.BandwidthInMBps"),
          config.getLong("cloudSimulatorVmAllocRR.host.StorageInMBs"),
          createPes(config.getInt("cloudSimulatorVmAllocRR.host.Pe_Amount")).asJava,
          false
        ) :: hostList;
        createHost(hostAmount - 1, newHostList);
      }
    }

    val hostList = createHost(config.getInt("cloudSimulatorVmAllocRR.host.Amount"));
    logger.info(s"Create a list of hostList: $hostList");
    return new DatacenterSimple(simulation, hostList.asJava, new VmAllocationPolicyRoundRobin());
  }

}