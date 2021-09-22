package Simulations

import HelperUtils.{AlphaCloudUtil, CloudModelEnum}
import Simulations.AlphaCloud.config
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.core.{CloudSim, Simulation}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import collection.JavaConverters.*

class AlphaCloudTest extends AnyFlatSpec with Matchers{
  behavior of "AlphaCloud Test Suite";

  it should "Create A Right Amount Of Host" in {
    val hostList = AlphaCloud.createHostList(40);
    hostList.size shouldBe 40;
  }

  it should "Create SaaS Datacenter" in {
    val simulation = new CloudSim();
    val dc = AlphaCloud.createSaasDatacenters(simulation);
    dc.getName shouldBe CloudModelEnum.SAAS;
  }

  it should "Create PaaS Datacenter" in {
    val simulation = new CloudSim();
    val dc = AlphaCloud.createPaasDatacenters(simulation);
    dc.getName shouldBe CloudModelEnum.PAAS;
  }

  it should "Create A Right Amount Of Pe" in {
    val peList = AlphaCloud.createPEList(5);
    peList.size shouldBe 5;
  }

  it should "Create A Right Amount Of Vm" in {
    val vmList = AlphaCloud.createVmList(9);
    vmList.size shouldBe 9;
  }

  it should "Create A Right Amount Of Cloudlet" in {
    val ccList = AlphaCloud.createCloudLetList(22);
    ccList.size shouldBe 22;
  }

  it should "Create Correct CloudModelEnum" in {
    CloudModelEnum.IAAS shouldBe "IAAS_MODEL";
    CloudModelEnum.SAAS shouldBe "SAAS_MODEL";
    CloudModelEnum.PAAS shouldBe "PAAS_MODEL";
  }

  it should "Map To The Right Datacenter" in {
    val simulation = new CloudSim();
    val saas = AlphaCloud.createSaasDatacenters(simulation);
    val paas = AlphaCloud.createPaasDatacenters(simulation);
    val dcList = List (saas, paas);
    val broker = new DatacenterBrokerSimple(simulation);
    val choosenDc = AlphaCloudUtil.mapBrokerToDatacenter(broker, dcList, CloudModelEnum.SAAS);
    choosenDc shouldBe saas;
  }

  behavior of "AlphaCloud Network Test Suite";

  it should "Create Right Amount Of Vm Network List" in {
    val vmNetworkList = AlphaCloudNetworkDatacenter.createNetworkVmList();
    val vmNetworkListSize = config.getInt("AlphaCloud.vm.Amount");
    vmNetworkList.size shouldBe vmNetworkListSize;
  }

  it should "Create Right Amount Of Network Host List" in {
    val networkHostList = AlphaCloudNetworkDatacenter.createHostList(5);
    networkHostList.size shouldBe 5;
  }

  it should "Create Right Amount Of Network Cloudlet" in {
    val cloudletNetworkList = AlphaCloudNetworkDatacenter.createNetworkCloudletList(AlphaCloudNetworkDatacenter.createNetworkVmList());
    val cloudletSize = config.getInt("AlphaCloud.cloudlet.Amount");
    cloudletNetworkList.size shouldBe cloudletSize;
  }

  it should "Create IAAS DC" in {
    val simulation = new CloudSim();
    val dc = AlphaCloudNetworkDatacenter.createNetworkDatacenter(simulation);
    dc.getName shouldBe CloudModelEnum.IAAS;
  }
}
