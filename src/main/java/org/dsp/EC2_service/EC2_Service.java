package org.dsp.EC2_service;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.waiters.Ec2Waiter;
import software.amazon.awssdk.regions.Region;

import java.util.Base64;

public class EC2_Service {
    /*private final String USAGE = """
            To run this example, supply an instance name and AMI image id
            Both values can be obtained from the AWS Console
            Ex: CreateInstance <instance-name> <ami-image-id>
            """;*/


    private final Ec2Client ec2;
    private final String amiId = "ami-00e95a9222311e8ed";
    private Region region;

    public EC2_Service(Region region) {
        this.region = region;
        ec2 = Ec2Client.builder()
                .region(this.region)
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


