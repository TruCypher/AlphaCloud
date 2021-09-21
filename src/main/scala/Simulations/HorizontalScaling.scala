package Simulations

import HelperUtils.{CreateLogger, ObtainConfigReference}
import org.cloudbus.cloudsim.allocationpolicies.{VmAllocationPolicy, VmAllocationPolicySimple}
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletSimple}
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.{Datacenter, DatacenterSimple}
import org.cloudbus.cloudsim.distributions.{ContinuousDistribution, UniformDistr}
import org.cloudbus.cloudsim.hosts.{Host, HostSimple}
import org.cloudbus.cloudsim.provisioners.{PeProvisionerSimple, ResourceProvisioner, ResourceProvisionerSimple}
import org.cloudbus.cloudsim.resources.{Pe, PeSimple}
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic
import org.cloudbus.cloudsim.vms.{Vm, VmSimple}
import org.cloudsimplus.autoscaling.{HorizontalVmScaling, HorizontalVmScalingSimple}
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.cloudsimplus.listeners.EventInfo

import java.util.function.Supplier
import collection.JavaConverters.*

class HorizontalScaling

object HorizontalScaling {
  val config = ObtainConfigReference("cloudSimulatorHorizontalScale") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }
  val logger = CreateLogger(classOf[HorizontalScaling])
  val seed = config.getInt("cloudSimulatorHorizontalScale.seed"); // For Testing purposes
  val rand = new UniformDistr(0, config.getDouble("cloudSimulatorHorizontalScale.cloudlet.Amount"), seed);

  def LBHorizontalScaling(): Unit = {
    // Create Cloud Sim
    val simulation = new CloudSim();

    // Create Datacenter
    createDataCenter(simulation);

    // Create Broker
    val broker = new DatacenterBrokerSimple(simulation);
    // Wailt for x seconds before delete an idle VM
    broker.setVmDestructionDelay(10);

    // Create Simulation Listener
    simulation.addOnClockTickListener(createNewCloudlets(_, broker));

    // Create scalable Vm
    val vmList = createListOfScalableVms();

    // Create cloudlet
    val cloudletList = createCloudLets();

    //User's request
    broker.submitVmList(vmList.asJava);
    broker.submitCloudletList(cloudletList.asJava);

    simulation.start();

    // Get Simulation Result
    new CloudletsTableBuilder(broker.getCloudletFinishedList()).build();
  }

  def createCloudLets() : List[Cloudlet] = {
    val cloudletList = createCloudLets_helper(config.getInt("cloudSimulatorHorizontalScale.cloudlet.Amount"));
    logger.info(s"Created List of CloudLets: $cloudletList");
    return cloudletList;
  }

  def createCloudLets_helper(ccAmount:Int = 1, ccList:List[Cloudlet] = List[Cloudlet]()) : List[Cloudlet] = {
    val utilizationModel = new UtilizationModelDynamic(config.getDouble("cloudSimulatorHorizontalScale.utilizationRatio"));
    val cloudLength = List(2000, 4000, 10000, 16000, 2000, 30000, 20000);

    if (ccAmount <= -1) {
      return ccList.reverse;
    } else {
      val newCCList = new CloudletSimple(
        cloudLength(rand.sample().toInt),
        config.getInt("cloudSimulatorHorizontalScale.cloudlet.PEs"),
      )
        .setFileSize(config.getLong("cloudSimulatorHorizontalScale.cloudlet.fileSize"))
        .setOutputSize(config.getLong("cloudSimulatorHorizontalScale.cloudlet.outputSize"))
        .setUtilizationModel(utilizationModel)
        :: ccList;

      createCloudLets_helper(ccAmount - 1, newCCList);
    }
  }

  def createListOfScalableVms(): List[Vm] = {
    def listOfVms(vmAmount:Int, vmList:List[Vm] = List[Vm]()) : List[Vm] = {
      if (vmAmount <= 0) {
        return vmList.reverse;
      } else {
        val newVm = createVm();
        createHorizontalVmScaling(newVm);
        val newVmList = newVm :: vmList;
        listOfVms(vmAmount - 1, newVmList);
      }
    }

    val vmList = listOfVms(config.getInt("cloudSimulatorHorizontalScale.vm.Amount"));
    logger.info(s"Created Scalable Vm List: $vmList");
    return vmList;
  }

  def createHorizontalVmScaling(vm:Vm): Unit = {
    val horizontalScaling:HorizontalVmScaling = new HorizontalVmScalingSimple();
    horizontalScaling
      .setVmSupplier(() => createVm())
      .setOverloadPredicate(isVmOverLoad);

    vm.setHorizontalScaling(horizontalScaling);
  }

  def isVmOverLoad(vm:Vm) : Boolean = {
    return vm.getCpuPercentUtilization() > 0.7;
  }

  def createVm() : Vm = {
    return new VmSimple(
      config.getLong("cloudSimulatorHorizontalScale.vm.mipsCapacity"),
      config.getLong("cloudSimulatorHorizontalScale.vm.PES")
    )
      .setRam(config.getLong("cloudSimulatorHorizontalScale.vm.RAMInMBs"))
      .setBw(config.getLong("cloudSimulatorHorizontalScale.vm.BandwidthInMBps"))
      .setSize(config.getLong("cloudSimulatorHorizontalScale.vm.StorageInMBs"))
      .setCloudletScheduler(new CloudletSchedulerTimeShared())
  }

  def createDataCenter(simulation:CloudSim): Unit = {
    def createPes(peAmount:Int, peList:List[Pe] = List[Pe]()) : List[Pe] = {
      if (peAmount <= 0) {
        return peList.reverse;
      } else {
        val newPeList : List[Pe] = new PeSimple(
          config.getLong("cloudSimulatorHorizontalScale.host.mipsCapacity"),
          new PeProvisionerSimple()
        ) :: peList;
        createPes(peAmount - 1, newPeList);
      }
    }

    def createHostList(hostAmount:Int, hostList:List[Host] = List[Host]()) : List[Host] = {
      if (hostAmount <= 0) {
        return hostList.reverse;
      } else {
        val newHostList = new HostSimple(
          config.getLong("cloudSimulatorHorizontalScale.host.RAMInMBs"),
          config.getLong("cloudSimulatorHorizontalScale.host.BandwidthInMBps"),
          config.getLong("cloudSimulatorHorizontalScale.host.StorageInMBs"),
          createPes(config.getInt("cloudSimulatorHorizontalScale.host.PES")).asJava
        )
          .setRamProvisioner(new ResourceProvisionerSimple())
          .setBwProvisioner(new ResourceProvisionerSimple())
          .setVmScheduler(new VmSchedulerTimeShared()) :: hostList;
        createHostList(hostAmount - 1, newHostList);
      }
    }

    val hostList:List[Host] = createHostList(config.getInt("cloudSimulatorHorizontalScale.host.Amount"));
    logger.info(s"Created Host List: $hostList");
    val dc0 : Datacenter = new DatacenterSimple(simulation, hostList.asJava, new VmAllocationPolicySimple());
    dc0.setSchedulingInterval(config.getDouble("cloudSimulatorHorizontalScale.SchedulingInterval"));
    logger.info(s"Created Horizontal Scale DataCenter: $dc0");
  }

  def createNewCloudlets(eventInfo: EventInfo, broker:DatacenterBrokerSimple): Unit = {
    val time = eventInfo.getTime().toLong;
    if (time % config.getInt("cloudSimulatorHorizontalScale.cloudlet.CreationInterval") == 0 && time <= 50) {
      val cloudLetNum = config.getInt("cloudSimulatorHorizontalScale.cloudlet.Amount");
      logger.info(s"Create $cloudLetNum cloud let at time $time");
      val cloudletList = createCloudLets_helper(cloudLetNum);
      broker.submitCloudletList(cloudletList.asJava);
    }
  }

}
