/*
 * MIT License
 *
 * Copyright (c) 2020 manager
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package pt.unl.fct.miei.usmanagement.manager.services.hosts.cloud.aws;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.AllocateAddressRequest;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AssociateAddressResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;
import com.amazonaws.services.ec2.model.DisassociateAddressResult;
import com.amazonaws.services.ec2.model.DomainType;
import com.amazonaws.services.ec2.model.DryRunResult;
import com.amazonaws.services.ec2.model.DryRunSupportedRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.amazonaws.services.ec2.model.ReleaseAddressResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import pt.unl.fct.miei.usmanagement.manager.containers.ContainerConstants;
import pt.unl.fct.miei.usmanagement.manager.exceptions.EntityNotFoundException;
import pt.unl.fct.miei.usmanagement.manager.exceptions.ManagerException;
import pt.unl.fct.miei.usmanagement.manager.hosts.HostAddress;
import pt.unl.fct.miei.usmanagement.manager.hosts.cloud.AwsRegion;
import pt.unl.fct.miei.usmanagement.manager.regions.RegionEnum;
import pt.unl.fct.miei.usmanagement.manager.services.configurations.ConfigurationsService;
import pt.unl.fct.miei.usmanagement.manager.services.regions.RegionsService;
import pt.unl.fct.miei.usmanagement.manager.services.remote.ssh.SshService;
import pt.unl.fct.miei.usmanagement.manager.util.Timing;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AwsService {

	private static final int BOOT_TIMEOUT = (int) TimeUnit.MINUTES.toMillis(2);

	private final SshService sshService;
	private final RegionsService regionService;
	private final Environment environment;
	private final ConfigurationsService configurationsService;

	private final Map<AwsRegion, AmazonEC2> ec2Clients;
	private final AWSStaticCredentialsProvider awsStaticCredentialsProvider;
	private final String awsInstanceSecurityGroup;
	private final String awsInstanceKeyPair;
	private final String awsInstanceType;
	private final String awsInstanceTag;
	private final int awsMaxRetries;
	private final int awsConnectionTimeout;
	private final String awsUsername;

	@Getter
	private final Map<RegionEnum, Address> allocatedElasticIps;

	public AwsService(SshService sshService, AwsProperties awsProperties, RegionsService regionService,
					  Environment environment, ConfigurationsService configurationsService) {
		this.sshService = sshService;
		this.regionService = regionService;
		this.environment = environment;
		this.configurationsService = configurationsService;
		this.ec2Clients = new HashMap<>(AwsRegion.getAvailableRegionsCount());
		String awsAccessKey = awsProperties.getAccess().getKey();
		String awsSecretAccessKey = awsProperties.getAccess().getSecretKey();
		BasicAWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretAccessKey);
		this.awsStaticCredentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
		this.awsInstanceSecurityGroup = awsProperties.getInstance().getSecurityGroup();
		this.awsInstanceKeyPair = awsProperties.getInstance().getKeyPair();
		this.awsInstanceType = awsProperties.getInstance().getType();
		this.awsInstanceTag = awsProperties.getInstance().getTag();
		this.awsMaxRetries = awsProperties.getMaxRetries();
		this.awsConnectionTimeout = awsProperties.getConnectionTimeout();
		this.awsUsername = awsProperties.getAccess().getUsername();
		this.allocatedElasticIps = new HashMap<>();
	}

	private AmazonEC2 getEC2Client() {
		return getEC2Client(AwsRegion.getDefaultZone());
	}

	private AmazonEC2 getEC2Client(AwsRegion region) {
		AmazonEC2 amazonEC2 = ec2Clients.get(region);
		/*ClientConfiguration configuration = new ClientConfiguration()
			.withConnectionTimeout((int) TimeUnit.SECONDS.toMillis(5))
			.withRequestTimeout((int) TimeUnit.SECONDS.toMillis(5))
			.withClientExecutionTimeout((int) TimeUnit.SECONDS.toMillis(10));*/
		if (amazonEC2 == null) {
			amazonEC2 = AmazonEC2ClientBuilder
				.standard()
				.withRegion(region.getZone())
				.withCredentials(awsStaticCredentialsProvider)
				/*.withClientConfiguration(configuration)*/
				.build();
			ec2Clients.put(region, amazonEC2);
		}
		return amazonEC2;
	}


	private CompletableFuture<Void> retryGetInstances(Throwable first, int retry, AwsRegion awsRegion,
													  List<Instance> instances) {
		if (retry >= 3) {
			return CompletableFuture.failedFuture(first);
		}
		return CompletableFuture
			.supplyAsync(() -> this.getInstances(awsRegion))
			.thenAccept(instances::addAll)
			.thenApply(CompletableFuture::completedFuture)
			.exceptionally(t -> {
				first.addSuppressed(t);
				return retryGetInstances(first, retry + 1, awsRegion, instances);
			})
			.thenCompose(Function.identity());
	}

	public List<Instance> getOwnInstances() {
		String managerId = environment.getProperty(ContainerConstants.Environment.Manager.ID);
		return getInstances().stream().filter(instance ->
			instance.getTags().stream().anyMatch(tag -> Objects.equals(tag.getKey(), "ManagerId") && Objects.equals(tag.getValue(), managerId))
		).collect(Collectors.toList());
	}

	public List<Instance> getInstances() {
		List<Instance> instances = new LinkedList<>();

		List<AwsRegion> regions = AwsRegion.getAwsRegions();
		CompletableFuture<?>[] requests = new CompletableFuture[regions.size()];
		int count = 0;
		for (AwsRegion awsRegion : regions) {
			CompletableFuture<?> future = CompletableFuture
				.supplyAsync(() -> this.getInstances(awsRegion))
				.thenAccept(instances::addAll)
				.thenApply(CompletableFuture::completedFuture)
				.exceptionally(t -> retryGetInstances(t, 0, awsRegion, instances))
				.thenCompose(Function.identity());
			requests[count++] = future;
		}

		CompletableFuture.allOf(requests).join();

		return instances;
	}

	public List<Instance> getInstances(AwsRegion region) {
		List<Instance> instances = new ArrayList<>();
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		DescribeInstancesResult result;
		try {
			AmazonEC2 client = getEC2Client(region);
			do {
				result = client.describeInstances(request);
				result.getReservations().stream().map(Reservation::getInstances).flatMap(List::stream)
					.filter(this::isUsManagerInstance).forEach(instances::add);
				request.setNextToken(result.getNextToken());
			} while (result.getNextToken() != null);
		}
		catch (SdkClientException e) {
			log.error("Unable to get instances from region {}: {}", region.getName(), e.getMessage());
		}
		return instances;
	}

	public Instance getInstance(String id, AwsRegion region) {
		for (int i = 0; i < awsMaxRetries; i++) {
			try {
				List<Instance> instances = getInstances(region);
				return instances.stream().filter(instance -> instance.getInstanceId().equals(id))
					.findFirst().orElseThrow(() -> new EntityNotFoundException(Instance.class, "id", id, "region", region.getName()));
			}
			catch (Exception e) {
				log.error("Unable to get instance {} on region {}: {}. Retrying... ({}/{})", id, region, e.getMessage(), i + 1, awsMaxRetries);
				Timing.sleep(i + 1, TimeUnit.SECONDS);
			}
		}
		throw new ManagerException("Unable to get instance %s on region %s", id, region.getRegion());
	}

	public List<AwsSimpleInstance> getSimpleInstances() {
		return getInstances().stream().map(AwsSimpleInstance::new).collect(Collectors.toList());
	}

	public Instance createInstance(AwsRegion region, InstanceType type) {
		log.info("Launching new instance of type {} at region {} {}", type, region.getZone(), region.getName());
		String instanceId = launchInstance(region, type);
		configurationsService.addConfiguration(instanceId);
		Instance instance = waitInstanceState(instanceId, AwsInstanceState.RUNNING, region);
		String publicIpAddress = instance.getPublicIpAddress();
		log.info("New aws instance created: instanceId = {}, publicIpAddress = {}", instanceId, publicIpAddress);
		try {
			waitToBoot(instance);
		}
		catch (IOException e) {
			throw new ManagerException("Error while waiting for instance %s to boot: %s", instance.getInstanceId(), e.getMessage());
		}
		return instance;
	}

	public String launchInstance(AwsRegion region, InstanceType type) {
		RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
			.withImageId(region.getAmi().get(type))
			.withInstanceType(type)
			.withMinCount(1)
			.withMaxCount(1)
			.withSecurityGroups(awsInstanceSecurityGroup)
			.withKeyName(awsInstanceKeyPair);
		AmazonEC2 amazonEC2 = getEC2Client(region);
		RunInstancesResult result = amazonEC2.runInstances(runInstancesRequest);
		Instance instance = result.getReservation().getInstances().get(0);
		String instanceId = instance.getInstanceId();
		String instanceName = String.format("ubuntu-%d", System.currentTimeMillis());
		CreateTagsRequest createTagsRequest = new CreateTagsRequest().withResources(instanceId).withTags(
			new Tag("Name", instanceName), new Tag(awsInstanceTag, "true"),
			new Tag("ManagerId", environment.getProperty(ContainerConstants.Environment.Manager.ID))
		);
		amazonEC2.createTags(createTagsRequest);
		return instanceId;
	}

	public Instance startInstance(String instanceId, AwsRegion region, boolean wait) {
		log.info("Starting instance {}", instanceId);
		Instance instance = setInstanceState(instanceId, AwsInstanceState.RUNNING, region, wait);
		try {
			waitToBoot(instance);
		}
		catch (IOException e) {
			throw new ManagerException("Error while waiting for instance %s to boot: %s", instance.getInstanceId(), e.getMessage());
		}
		return instance;
	}

	private void startInstanceById(String instanceId, AwsRegion region) {
		DryRunSupportedRequest<StartInstancesRequest> dryRequest = () ->
			new StartInstancesRequest().withInstanceIds(instanceId).getDryRunRequest();
		AmazonEC2 amazonEC2 = getEC2Client(region);
		DryRunResult<StartInstancesRequest> dryResponse = amazonEC2.dryRun(dryRequest);
		if (!dryResponse.isSuccessful()) {
			throw new ManagerException(dryResponse.getDryRunResponse().getErrorMessage());
		}
		StartInstancesRequest request = new StartInstancesRequest().withInstanceIds(instanceId);
		amazonEC2.startInstances(request);
	}

	public Instance stopInstance(String instanceId, AwsRegion region, boolean wait) {
		log.info("Stopping instance {}", instanceId);
		return setInstanceState(instanceId, AwsInstanceState.STOPPED, region, wait);
	}

	private void stopInstanceById(String instanceId, AwsRegion region) {
		DryRunSupportedRequest<StopInstancesRequest> dryRequest = () ->
			new StopInstancesRequest().withInstanceIds(instanceId).getDryRunRequest();
		AmazonEC2 amazonEC2 = getEC2Client(region);
		DryRunResult<StopInstancesRequest> dryResponse = amazonEC2.dryRun(dryRequest);
		if (!dryResponse.isSuccessful()) {
			throw new ManagerException(dryResponse.getDryRunResponse().getErrorMessage());
		}
		StopInstancesRequest request = new StopInstancesRequest().withInstanceIds(instanceId);
		amazonEC2.stopInstances(request);
	}

	public void terminateInstance(String instanceId, AwsRegion region, boolean wait) {
		log.info("Terminating instance {}", instanceId);
		setInstanceState(instanceId, AwsInstanceState.TERMINATED, region, wait);
	}

	private void terminateInstanceById(String instanceId, AwsRegion region) {
		DryRunSupportedRequest<TerminateInstancesRequest> dryRequest = () ->
			new TerminateInstancesRequest().withInstanceIds(instanceId).getDryRunRequest();
		AmazonEC2 amazonEC2 = getEC2Client(region);
		DryRunResult<TerminateInstancesRequest> dryResponse = amazonEC2.dryRun(dryRequest);
		if (!dryResponse.isSuccessful()) {
			throw new ManagerException(dryResponse.getDryRunResponse().getErrorMessage());
		}
		TerminateInstancesRequest request = new TerminateInstancesRequest().withInstanceIds(instanceId);
		amazonEC2.terminateInstances(request);
	}

	private Instance setInstanceState(String id, AwsInstanceState state, AwsRegion region, boolean wait) {
		Instance instance = getInstance(id, region);
		int instanceState = instance.getState().getCode();
		if (instanceState == state.getCode()) {
			log.info("Instance {} is already on state {}", id, state.getState());
			return instance;
		}
		try {
			switch (state) {
				case RUNNING:
					startInstanceById(id, region);
					break;
				case STOPPED:
					stopInstanceById(id, region);
					break;
				case TERMINATED:
					terminateInstanceById(id, region);
					break;
				default:
					throw new UnsupportedOperationException();
			}
			if (wait) {
				instance = waitInstanceState(id, state, region);
			}
			log.info("Setting instance {} to {} state", id, state.getState());
			return instance;
		}
		catch (ManagerException e) {
			log.error("Failed to set instance {} to {} state: {}", id, state.getState(), e.getMessage());
		}

		throw new ManagerException("Unable to set instance state %d within %d tries", state.getState(), awsMaxRetries);
	}

	private Instance waitInstanceState(String instanceId, AwsInstanceState state, AwsRegion region) {
		Instance[] instance = new Instance[1];
		try {
			Timing.wait(() -> {
				instance[0] = getInstance(instanceId, region);
				return instance[0].getState().getCode() == state.getCode();
			}, awsConnectionTimeout);
		}
		catch (TimeoutException e) {
			throw new ManagerException("Unknown status of instance %s %s operation: Timed out", instanceId, state.getState());
		}
		return instance[0];
	}

	public void waitToBoot(Instance instance) throws IOException {
		HostAddress hostAddress = new HostAddress(awsUsername, instance.getPublicIpAddress(), instance.getPrivateIpAddress());
		String error = null;
		final int retries = 5;
		for (int i = 0; i < retries; i++) {
			try {
				SSHClient client = sshService.waitAvailability(hostAddress, BOOT_TIMEOUT);
				sshService.executeCommand("whoami", hostAddress, client, BOOT_TIMEOUT);
				return;
			}
			catch (IOException e) {
				error = e.getMessage();
				log.info("Error while waiting for instance {} to boot: {}... retrying ({}/{})", instance.getInstanceId(), e.getMessage(), i + 1, retries);
				Timing.sleep(i + 1, TimeUnit.SECONDS);
			}
		}
		throw new IOException(error);
	}

	private boolean isUsManagerInstance(Instance instance) {
		return instance.getTags().stream().anyMatch(tag ->
			Objects.equals(tag.getKey(), awsInstanceTag) && Objects.equals(tag.getValue(), "true"));
	}

	public String allocateElasticIpAddress(RegionEnum region) {
		final AwsRegion awsRegion = AwsRegion.getRegionsToAwsRegions().get(region);
		final AmazonEC2 ec2 = getEC2Client(awsRegion);
		AllocateAddressRequest allocateRequest = new AllocateAddressRequest().withDomain(DomainType.Vpc);
		AllocateAddressResult allocateResponse = ec2.allocateAddress(allocateRequest);
		log.info("Allocated elastic ip address at region {}", awsRegion);
		return allocateResponse.getAllocationId();
	}

	public AssociateAddressResult associateElasticIpAddress(RegionEnum region, String allocationId, String instanceId) {
		final AwsRegion awsRegion = AwsRegion.getRegionsToAwsRegions().get(region);
		final AmazonEC2 ec2 = getEC2Client(awsRegion);
		AssociateAddressRequest associateRequest = new AssociateAddressRequest()
			.withInstanceId(instanceId)
			.withAllocationId(allocationId);
		return ec2.associateAddress(associateRequest);
	}

	public DisassociateAddressResult dissociateElasticIpAddress(RegionEnum region, String associationId) {
		final AwsRegion awsRegion = AwsRegion.getRegionsToAwsRegions().get(region);
		final AmazonEC2 ec2 = getEC2Client(awsRegion);
		DisassociateAddressRequest disassociateAddressRequest = new DisassociateAddressRequest()
			.withAssociationId(associationId);
		return ec2.disassociateAddress(disassociateAddressRequest);
	}

	public List<Address> getElasticIpAddresses(AwsRegion region) {
		final AmazonEC2 ec2 = getEC2Client(region);
		DescribeAddressesResult result = ec2.describeAddresses();
		return result.getAddresses();
	}

	public ReleaseAddressResult releaseElasticIpAddress(String allocationId, RegionEnum region) {
		final AwsRegion awsRegion = AwsRegion.getRegionsToAwsRegions().get(region);
		final AmazonEC2 ec2 = getEC2Client(awsRegion);
		ReleaseAddressRequest request = new ReleaseAddressRequest().withAllocationId(allocationId);
		return ec2.releaseAddress(request);
	}
}
