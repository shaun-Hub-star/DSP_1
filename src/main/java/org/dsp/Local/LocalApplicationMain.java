package org.dsp.Local;

public class LocalApplicationMain {
    /*
    * >java -jar yourjar.jar inputFileName outputFileName n [terminate]
    *
    *
    * yourjar.jar is the name of the jar file containing your code.
    • inputFileName is the name of the input file.
    • outputFileName is the name of the output html file.
    • n is the workers’ files ratio (max files per worker).
    • terminate indicates that the application should terminate the manager at the end.
    * */
    public static void main(String[] args) {
        LocalApplication localApplication;
        if (args.length < 2 || args.length > 4)
            throw new RuntimeException("inputFileName outputFileName n [terminate]");
        String inputFile = args[0];
        String outputFile = args[1];
        int ratioOfComputers;
        try {
            ratioOfComputers = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
            throw e;

        }

        localApplication = new LocalApplication(inputFile, outputFile, ratioOfComputers);
        localApplication.run();

        //at the end:
        if (args.length == 4 && args[3].equals("terminate"))
            localApplication.terminateManager();


    }


}
