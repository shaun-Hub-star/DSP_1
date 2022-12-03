package org.dsp.AWS_SERVICES.EC2_service;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.waiters.Ec2Waiter;

import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


public class EC2_Service {
    /*private final String USAGE = """
            To run this example, supply an instance name and AMI image id
            Both values can be obtained from the AWS Console
            Ex: CreateInstance <instance-name> <ami-image-id>
            """;*/


    private final Ec2Client ec2;
    private final String amiId = "ami-0b0dcb5067f052a63";
    private Region region;

    public EC2_Service(Region region) {
        this.region = region;
        ec2 = Ec2Client.builder()
                .region(this.region)
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();
    }

/*    private static void createManagerIfNotExists() {
        System.out.println("[DEBUG] Create manager if not exist");

        if (!aws.checkIfInstanceExist(aws.managerTag)) {
            System.out.println("[DEBUG] Manager doesn't exist.");
            String managerScript = "#!/bin/bash\n" +
                    "echo Manager jar running\n" +
                    "echo s3://" + aws.bucketName + "/" + aws.managerJarKey + "\n" +
                    "mkdir ManagerFiles\n" +
                    "aws s3 cp s3://" + aws.bucketName + "/" + aws.managerJarKey + " ./ManagerFiles/" + aws.managerJarName + "\n" +
                    "echo manager copy the jar from s3\n" +
                    "java -jar /ManagerFiles/" + aws.managerJarName + "\n";

            managerInstanceID = aws.createInstance(managerScript, aws.managerTag, 1);
            System.out.println("[DEBUG] Manager created and started!.");
        } else {
            System.out.println("[DEBUG] Manager already exists and run.");
        }
    }*/


    public String createEc2Instance(String name, String userDataScript, InstanceType computerType) {

        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .instanceType(computerType)
                .imageId(amiId)
                .maxCount(1)
                .minCount(1)
                .keyName("vockey")
                .iamInstanceProfile(IamInstanceProfileSpecification.builder().name("LabInstanceProfile").build())
                .userData(Base64.getEncoder().encodeToString(userDataScript.getBytes()))
                .build();

        RunInstancesResponse response = ec2.runInstances(runRequest);

        String instanceId = response.instances().get(0).instanceId();

        Tag tag = Tag.builder()
                .key("Name")
                .value(name)
                .build();

        CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build();

        try {
            ec2.createTags(tagRequest);
            System.out.printf(
                    "Successfully started EC2 instance %s based on AMI %s",
                    instanceId, amiId);

        } catch (Ec2Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        // snippet-end:[ec2.java2.create_instance.main]
        System.out.println("Done!");

        return instanceId;
    }

    public List<String> getAllNotRunningInstancesWithAGivenTag(String tag, String tagValue){
        Filter filter = Filter.builder().name("tag:" + tag).values(tagValue).build();

        DescribeInstancesRequest request = DescribeInstancesRequest.builder().filters(filter).build();

        List<Reservation> reservations = ec2.describeInstances(request).reservations();

        /*
        * The state of the instance as a 16-bit unsigned integer.
        The high byte is all the bits between 2^8 and (2^16)-1, which equals decimal values between 256 and 65,535. These numerical values are used for internal purposes and should be ignored.
        The low byte is all the bits between 2^0 and (2^8)-1, which equals decimal values between 0 and 255.
        The valid values for instance-state-code will all be in the range of the low byte, and they are:
        0 : pending
        16 : running
        32 : shutting-down
        48 : terminated
        64 : stopping
        80 : stopped
        *
        * */
        LinkedList<String> terminatedInstancesIds = new LinkedList<>();
        for (Reservation reservation : reservations) {
            List<Instance> instances = reservation.instances();
            for (Instance instance : instances) {
                int stateCode = instance.state().code();
                if(stateCode >= 32)
                    terminatedInstancesIds.add(instance.instanceId());

            }
        }
        return terminatedInstancesIds;

    }

    public void terminateEC2(String instanceId) {
        try (Ec2Waiter ec2Waiter = Ec2Waiter.builder()
                .overrideConfiguration(b -> b.maxAttempts(100))
                .client(ec2)
                .build()) {


            TerminateInstancesRequest ti = TerminateInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            ec2.terminateInstances(ti);

            DescribeInstancesRequest instanceRequest = DescribeInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();
            System.out.println("[Debug] in termination");
            WaiterResponse<DescribeInstancesResponse> waiterResponse = ec2Waiter.waitUntilInstanceTerminated(instanceRequest);
            waiterResponse.matched().response().ifPresent(System.out::println);
            System.out.println(instanceId + " is terminated!");

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }
    }

}


