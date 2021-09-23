import HelperUtils.{CreateLogger, ObtainConfigReference}
import Simulations._
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.util.control.Breaks

object Simulation:
  val logger = CreateLogger(classOf[Simulation])

  @main def runSimulation =
    cli_helper();

    println("================================================");
    println("                     DONE                       ");
    println("================================================");

  def cli_helper() : Unit = {
    println();
    println();
    println("================================================");
    println("              CLOUD SIM SIUMLATION              ");
    println("================================================");

    println("Enter 1 To Run Basic Example");
    println("Enter 2 To Run Vm Alloc RR Example");
    println("Enter 3 To Run Horizontal Scaling Eaxmple");
    println("Enter 4 To Run AlphaCloud Simulation");
    println("Enter 5 To Run DeltaCloud Simulation");
    println("Enter 0 To Exit");

    print("Enter Your Choice: ")
    val userInput = scala.io.StdIn.readInt();

    if (userInput == 1) {
      BasicCloudSimPlusExample.Start()
    } else if (userInput == 2) {
      VmAllocRR.vmAllocationPolicyRoundRobin();
    } else if (userInput == 3) {
      HorizontalScaling.LBHorizontalScaling();
    } else if (userInput == 4) {
      AlphaCloud.alphaCloud();
    } else if (userInput == 5) {
      DeltaCloud.deltaCloud();
    }

  if (userInput != 0)
      cli_helper();
  }

class Simulation