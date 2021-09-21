package HelperUtils

import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletExecution}
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerAbstract

import java.util
import java.util.stream.Collectors.toList
import scala.collection.JavaConverters._

/*
* Alpha Cloudlet Scheduler will optimize how cloudlet get assign into the vm (Mix between space share and best fit)
* Since Timeshared are underperform when # of cloudlet is > # of Vm
* Space share is good in the above situation but not always optimal
*/
class AlphaCloudletScheduler extends CloudletSchedulerAbstract {

  override def getCloudletWaitingList: util.List[CloudletExecution] = super.getCloudletWaitingList

  def movePauseCCToExec(cloudlet:CloudletExecution) : Double = {
    getCloudletPausedList().remove(cloudlet);
    addCloudletToExecList(cloudlet);
    return cloudletEstimatedFinishTime(cloudlet, getVm().getSimulation().clock());
  }

  def cloudletResume(cloudlet:Cloudlet): Double =
    return findCloudletInList(cloudlet, getCloudletPausedList())
      .map(movePauseCCToExec)
      .orElse(0.0);

  /*
  * Cloud can be submit to the execution list if and only if there is enough resources
  */
  def canExecuteCloudletInternal (cloudlet:CloudletExecution): Boolean =
    return isThereEnoughFreePesForCloudlet(cloudlet);

  /*
  * Preemption Functionality using algorithm from bestfit cloudlet scheduling
  */
  override def moveNextCloudletsFromWaitingToExecList(currentTime: Double): Double = {
    val preemptCC = preemptExecCloudletsWithExpiredVRuntimeAndMoveToWaitingList();
    val nextFinishCC = super.moveNextCloudletsFromWaitingToExecList(currentTime);

    preemptCC.foreach( ce => {
      ce.setVirtualRuntime(computeCloudletInitialVirtualRuntime(ce));
    });

    return nextFinishCC;
  }

  def computeCloudletInitialVirtualRuntime(cloudlet:CloudletExecution) : Double = {
    val inverseOfCId = Integer.MAX_VALUE / (cloudlet.getCloudletId() + 1.0);
    return -Math.abs(cloudlet.getCloudlet().getPriority() + inverseOfCId);
  }

  def preemptExecCloudletsWithExpiredVRuntimeAndMoveToWaitingList(): List[CloudletExecution] = {
     val expiredCloudlet:java.util.List[CloudletExecution] = getCloudletExecList().stream().filter((ce) => {ce.getVirtualRuntime() >= ce.getTimeSlice()}).collect(toList());
    expiredCloudlet.forEach(ce => {addCloudletToWaitingList(removeCloudletFromExecList(ce))});
    return expiredCloudlet.asScala.toList;
  }
}
