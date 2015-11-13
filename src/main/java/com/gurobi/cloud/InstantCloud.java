package com.gurobi.cloud;

import com.gurobi.cloud.client.InstantCloudClient;
import com.gurobi.cloud.model.InstantCloudLicense;
import com.gurobi.cloud.model.InstantCloudMachine;

public class InstantCloud {

  private static void printLicenses(InstantCloudLicense[] licenses) {
    if (licenses.length > 0) {
      System.out.println("License Credit  Rate Plan       Expiration");
    }
    for (int i = 0; i < licenses.length; i++) {
      InstantCloudLicense license = licenses[i];
      System.out.println(license.licenseId + '\t' + license.credit + '\t' +
                         license.ratePlan + '\t' + license.expiration);
    }
  }

  private static void printMachines(InstantCloudMachine[] machines)  {
    for (int i = 0; i < machines.length; i++) {
      InstantCloudMachine machine = machines[i];
      System.out.println("Machine name: " + machine.DNSName);
      System.out.println("\tlicense type: " + machine.licenseType);
      System.out.println("\tstate: " + machine.state);
      System.out.println("\tmachine type: " + machine.machineType);
      System.out.println("\tregion: " + machine.region);
      System.out.println("\tidle shutdown: " + machine.idleShutdown);
      System.out.println("\tuser password: " + machine.userPassword);
      System.out.println("\tcreate time: " + machine.createTime);
      System.out.println("\tlicense id: " + machine.licenseId);
      System.out.println("\tmachine id: " + machine.machineId);
    }
  }

  private static void usage() {
    System.out.println("InstantCloud command [options]");
    System.out.println();
    System.out.println("Here command is one of the following:");
    System.out.println("\tlaunch\tLaunch a set of Gurobi machines");
    System.out.println("\tkill\tKill a set of Gurobi machines");
    System.out.println("\tlicenses\tShow the licenses associated with your account");
    System.out.println("\tmachines\tShow currently running machines");
    System.out.println();
    System.out.println("General options:");
    System.out.println(" --help (-h): this message");
    System.out.println(" --id   (-I): set your access id");
    System.out.println(" --key  (-K): set your secret key");
  }

  private static String getId(String id) {
    if (id != null) {
      return id;
    } else if (System.getenv("IC_ACCESS_ID") != null) {
      return System.getenv("IC_ACCESS_ID");
    } else {
      System.out.println("Could not find access id. Set the access id with --id");
      System.out.println("Or by setting the environmental variable IC_ACCESS_ID");
      System.exit(1);
      return null;
    }
  }

  private static String getKey(String id) {
    if (id != null) {
      return id;
    } else if (System.getenv("IC_SECRET_KEY") != null) {
      return System.getenv("IC_SECRET_KEY");
    } else {
      System.out.println("Could not find secret key. Set the secret key with --key");
      System.out.println("Or by setting the environmental variable IC_SECRET_KEY");
      System.exit(1);
      return null;
    }
  }


  public static void main(String[] args) throws Exception {
    String accessId  = null;
    String secretKey = null;
    String command   = null;
    int i;

    for (i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.equals("-h") || arg.equals("--help")) {
        usage();
        System.exit(1);
      } else if (arg.equals("-I") || arg.equals("--id")) {
        accessId = args[i+1];
        i++;
      } else if (arg.equals("-K") || arg.equals("--key")) {
        secretKey = args[i+1];
        i++;
      } else if (arg.equals("licenses") ||
                 arg.equals("machines") ||
                 arg.equals("launch")   ||
                 arg.equals("kill")       ) {
        command = arg;
      } else {
        break;
      }
    }

    if (command == null) {
      System.out.println("Missing command");
      usage();
      System.exit(1);
    }

    accessId  = getId(accessId);
    secretKey = getKey(secretKey);

    InstantCloudClient instantCloud = new InstantCloudClient(accessId, secretKey);
    // instantCloud.verbose = true; //uncomment for verbose info on API requests

    if (command.equals("licenses")) {
      InstantCloudLicense[] licenses = instantCloud.getLicenses();
      printLicenses(licenses);
    } else if (command.equals("machines")) {
      InstantCloudMachine[] machines = instantCloud.getMachines();
      printMachines(machines);
    } else if (command.equals("kill")) {
      if (args.length -i < 1) {
        System.out.println("InstantCloud kill requires machine id argument(s)");
        System.exit(1);
      }
      String[] machineIds = new String[args.length-i];
      int j = 0;
      for (; i < args.length; i++) {
        machineIds[j] = args[i];
        j++;
      }
      InstantCloudMachine[] machines = instantCloud.killMachines(machineIds);
      System.out.println("Machines Killed");
      printMachines(machines);
    } else if (command.equals("launch")) {
      int numMachines = 1;
      String licenseType  = null;
      String userPassword = null;
      String licenseId    = null;
      String region       = null;
      int    idleShutdown = 60;
      String machineType  = null;

      if (args.length - i < 1) {
        System.out.println("InstantCloud launch requires numMachines argument");
        System.exit(1);
      }

      for (; i < args.length; i++) {
        String arg = args[i];
        if (arg.equals("-n") || arg.equals("--nummachines")) {
          numMachines = Integer.parseInt(args[i+1]);
          i++;
        } else if (arg.equals("-l") || arg.equals("--licensetype")) {
          licenseType = args[i+1];
          i++;
        } else if (arg.equals("-p") || arg.equals("--password")) {
          userPassword = args[i+1];
          i++;
        } else if (arg.equals("-i") || arg.equals("--licenseid")) {
          licenseId = args[i+1];
          i++;
        } else if (arg.equals("-r") || arg.equals("region")) {
          region = args[i+1];
          i++;
        } else if (arg.equals("-s") || arg.equals("--idleshutdown")) {
          idleShutdown = Integer.parseInt(args[i+1]);
          i++;
        } else if (arg.equals("-m") || arg.equals("--machinetype")) {
          machineType = args[i+1];
          i++;
        } else {
          System.out.println("Unrecognized launch option: " + arg);
        }
      }

      InstantCloudMachine[] machines = instantCloud.launchMachines(numMachines,
                                                                   licenseType,
                                                                   licenseId,
                                                                   userPassword,
                                                                   region,
                                                                   idleShutdown,
                                                                   machineType);
      System.out.println("Machines Launched");
      printMachines(machines);
    } else {
      System.out.println("Unrecognized command:" + command);
      System.exit(1);
    }
  }
}
