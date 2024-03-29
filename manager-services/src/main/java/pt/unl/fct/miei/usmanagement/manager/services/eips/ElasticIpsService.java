package pt.unl.fct.miei.usmanagement.manager.services.eips;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.AssociateAddressResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.ReleaseAddressResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.unl.fct.miei.usmanagement.manager.eips.ElasticIp;
import pt.unl.fct.miei.usmanagement.manager.eips.ElasticIps;
import pt.unl.fct.miei.usmanagement.manager.exceptions.EntityNotFoundException;
import pt.unl.fct.miei.usmanagement.manager.exceptions.ManagerException;
import pt.unl.fct.miei.usmanagement.manager.hosts.HostAddress;
import pt.unl.fct.miei.usmanagement.manager.hosts.cloud.AwsRegion;
import pt.unl.fct.miei.usmanagement.manager.hosts.cloud.CloudHost;
import pt.unl.fct.miei.usmanagement.manager.regions.RegionEnum;
import pt.unl.fct.miei.usmanagement.manager.services.communication.kafka.KafkaService;
import pt.unl.fct.miei.usmanagement.manager.services.containers.ContainersService;
import pt.unl.fct.miei.usmanagement.manager.services.docker.nodes.NodesService;
import pt.unl.fct.miei.usmanagement.manager.services.hosts.cloud.CloudHostsService;
import pt.unl.fct.miei.usmanagement.manager.services.hosts.cloud.aws.AwsService;
import pt.unl.fct.miei.usmanagement.manager.services.regions.RegionsService;
import pt.unl.fct.miei.usmanagement.manager.util.EntityUtils;
import pt.unl.fct.miei.usmanagement.manager.util.Timing;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ElasticIpsService {

	private final AwsService awsService;
	private final CloudHostsService cloudHostsService;
	private final NodesService nodesService;
	private final ContainersService containersService;
	private final KafkaService kafkaService;

	private final ElasticIps elasticIps;

	public ElasticIpsService(@Lazy AwsService awsService, @Lazy CloudHostsService cloudHostsService,
							 @Lazy NodesService nodesService, @Lazy ContainersService containersService,
							 KafkaService kafkaService, ElasticIps elasticIps) {
		this.awsService = awsService;
		this.cloudHostsService = cloudHostsService;
		this.nodesService = nodesService;
		this.containersService = containersService;
		this.kafkaService = kafkaService;
		this.elasticIps = elasticIps;
	}

	@Transactional(readOnly = true)
	public List<ElasticIp> getElasticIps() {
		return elasticIps.findAll();
	}

	public ElasticIp getElasticIp(Long id) {
		return elasticIps.findById(id).orElseThrow(() ->
			new EntityNotFoundException(ElasticIp.class, "id", id.toString()));
	}

	public boolean hasElasticIpByPublicIp(String publicIpAddress) {
		return elasticIps.hasElasticIpByPublicIp(publicIpAddress);
	}

	public ElasticIp getElasticIp(RegionEnum regionEnum) {
		return elasticIps.getElasticIpByRegion(regionEnum).orElseThrow(() ->
			new EntityNotFoundException(ElasticIp.class, "region", regionEnum.name()));
	}

	public ElasticIp getElasticIp(String allocationId) {
		return elasticIps.getElasticIpByAllocationId(allocationId).orElseThrow(() ->
			new EntityNotFoundException(ElasticIp.class, "allocationId", allocationId));
	}

	public ElasticIp addElasticIp(ElasticIp elasticIp) {
		checkElasticIpDoesntExist(elasticIp);
		elasticIp = saveElasticIp(elasticIp);
		/*ElasticIp kafkaElasticIp = elasticIp;
		kafkaElasticIp.setNew(true);
		kafkaService.sendElasticIp(kafkaElasticIp);*/
		kafkaService.sendElasticIp(elasticIp);
		return elasticIp;
	}

	public ElasticIp saveElasticIp(ElasticIp elasticIp) {
		log.info("Saving elasticIp {}", ToStringBuilder.reflectionToString(elasticIp));
		return elasticIps.save(elasticIp);
	}

	public ElasticIp addOrUpdateElasticIp(ElasticIp elasticIp) {
		if (elasticIp.getId() != null) {
			Optional<ElasticIp> elasticIpOptional = elasticIps.findById(elasticIp.getId());
			if (elasticIpOptional.isPresent()) {
				ElasticIp existingElasticIp = elasticIpOptional.get();
				EntityUtils.copyValidProperties(elasticIp, existingElasticIp);
				return saveElasticIp(existingElasticIp);
			}
		}
		return saveElasticIp(elasticIp);
	}

	public void clearElasticIps() {
		elasticIps.deleteAll();
	}

	private void checkElasticIpDoesntExist(ElasticIp elasticIp) {
		RegionEnum region = elasticIp.getRegion();
		if (elasticIps.hasElasticIp(region)) {
			throw new DataIntegrityViolationException("ElasticIp already exists on region " + region.name());
		}
	}

	public String allocateElasticIpAddress(RegionEnum region) {
		return awsService.allocateElasticIpAddress(region);
	}

	public Map<RegionEnum, Address> allocateElasticIpAddresses() {
		String errorMessage = null;
		final int retries = 5;
		for (int i = 0; i < retries; i++) {
			try {
				Map<RegionEnum, List<Address>> currentAddresses = getElasticIpAddresses();

				RegionEnum[] regions = RegionEnum.values();

				Map<RegionEnum, CompletableFuture<?>> requests = new HashMap<>(regions.length);
				for (RegionEnum region : regions) {
					List<Address> addresses = currentAddresses.get(region);
					if (addresses == null || addresses.isEmpty() || addresses.get(0).getAssociationId() != null) {
						CompletableFuture<?> future = CompletableFuture
							.supplyAsync(() -> allocateElasticIpAddress(region)).exceptionally(ex -> null);
						requests.put(region, future);
					}
				}

				CompletableFuture.allOf(requests.values().toArray(new CompletableFuture[0])).get();

				Map<RegionEnum, Address> elasticIpAddresses = getElasticIpAddresses().entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
				elasticIpAddresses.forEach((region, address) -> {
					ElasticIp elasticIp = ElasticIp.builder().region(region).allocationId(address.getAllocationId())
						.publicIp(address.getPublicIp()).build();
					addElasticIp(elasticIp);
				});
				return elasticIpAddresses;

			}
			catch (SdkClientException | InterruptedException | ExecutionException | ManagerException e) {
				errorMessage = e.getMessage();
				log.error("Failed to allocate elastic ip addresses: {}. Retrying {}/{}", errorMessage, i + 1, retries);
			}
		}
		throw new ManagerException("Failed to allocate elastic ip addresses: %s", errorMessage);
	}

	public CloudHost associateElasticIpAddress(RegionEnum region, String allocationId, CloudHost cloudHost) {
		ElasticIp elasticIp = getElasticIp(allocationId);
		String instanceId = cloudHost.getInstanceId();
		HostAddress previousHostAddress = cloudHost.getAddress();
		AssociateAddressResult associateResult = awsService.associateElasticIpAddress(region, allocationId, instanceId);
		String associationId = associateResult.getAssociationId();
		elasticIp.setAssociationId(associationId);
		elasticIp.setInstanceId(instanceId);
		elasticIp = saveElasticIp(elasticIp);
		kafkaService.sendElasticIp(elasticIp);
		log.info("Associated public ip address {} from elastic ip {} to cloud instance {} with association id {}",
			elasticIp.getPublicIp(), elasticIp.getAllocationId(), instanceId, associationId);
		AwsRegion awsRegion = AwsRegion.getRegionsToAwsRegions().get(region);
		Instance instance = awsService.getInstance(instanceId, awsRegion);
		cloudHost.setPublicIpAddress(instance.getPublicIpAddress());
		cloudHost = cloudHostsService.updateCloudHost(cloudHost);
		nodesService.updateAddress(previousHostAddress, elasticIp.getPublicIp());
		containersService.updateAddress(previousHostAddress, elasticIp.getPublicIp());
		return cloudHost;
	}

	private CompletableFuture<Void> retryGetElasticIpAddresses(Throwable first, int retry,
															   RegionEnum region, AwsRegion awsRegion,
															   Map<RegionEnum, List<Address>> elasticIpAddresses) {
		if (retry >= 3) {
			return CompletableFuture.failedFuture(first);
		}
		return CompletableFuture
			.supplyAsync(() -> awsService.getElasticIpAddresses(awsRegion))
			.thenAccept(elasticIps -> elasticIpAddresses.put(region, elasticIps))
			.thenApply(CompletableFuture::completedFuture)
			.exceptionally(t -> {
				first.addSuppressed(t);
				return retryGetElasticIpAddresses(first, retry + 1, region, awsRegion, elasticIpAddresses);
			})
			.thenCompose(Function.identity());
	}

	public Map<RegionEnum, List<Address>> getElasticIpAddresses() {
		Map<RegionEnum, List<Address>> elasticIpAddresses = new HashMap<>(RegionEnum.values().length);

		RegionEnum[] regions = RegionEnum.values();
		CompletableFuture<?>[] requests = new CompletableFuture[regions.length];
		int count = 0;
		for (RegionEnum region : regions) {
			AwsRegion awsRegion = AwsRegion.getRegionsToAwsRegions().get(region);
			CompletableFuture<?> future = CompletableFuture
				.supplyAsync(() -> awsService.getElasticIpAddresses(awsRegion))
				.thenAccept(elasticIps -> elasticIpAddresses.put(region, elasticIps))
				.thenApply(CompletableFuture::completedFuture)
				.exceptionally(t -> retryGetElasticIpAddresses(t, 0, region, awsRegion, elasticIpAddresses))
				.thenCompose(Function.identity());
			requests[count++] = future;
		}

		CompletableFuture.allOf(requests).join();

		return elasticIpAddresses;
	}

	public ReleaseAddressResult releaseElasticIpAddress(String allocationId, RegionEnum region) {
		ReleaseAddressResult result = awsService.releaseElasticIpAddress(allocationId, region);
		ElasticIp elasticIp = getElasticIp(allocationId);
		elasticIps.delete(elasticIp);
		kafkaService.sendDeleteElasticIp(elasticIp);
		log.info("Released elastic ip {}", allocationId);
		return result;
	}

	public void releaseElasticIpAddresses() {
		List<CompletableFuture<?>> requests = new LinkedList<>();

		for (Map.Entry<RegionEnum, List<Address>> regionElasticIpAddresses : getElasticIpAddresses().entrySet()) {
			RegionEnum region = regionElasticIpAddresses.getKey();
			List<Address> addresses = regionElasticIpAddresses.getValue();
			for (Address address : addresses) {
				String allocationId = address.getAllocationId();
				CompletableFuture<?> releaseRequest = CompletableFuture.supplyAsync(() -> releaseElasticIpAddress(allocationId, region));
				requests.add(releaseRequest);
			}
		}

		CompletableFuture.allOf(requests.toArray(new CompletableFuture[0])).join();
	}

	public void reset() {
		log.info("Clearing all elastic ips");
		List<ElasticIp> elasticIps = getElasticIps();
		this.elasticIps.deleteAll();
		elasticIps.forEach(kafkaService::sendDeleteElasticIp);
	}

	public ElasticIp dissociate(CloudHost cloudHost) {
		RegionEnum region = cloudHost.getAwsRegion().getRegion();
		ElasticIp elasticIp = getElasticIp(region);
		try {
			awsService.dissociateElasticIpAddress(region, elasticIp.getAssociationId());
		}
		catch (AmazonEC2Exception e) {
			log.error("Error while dissociating elastic ip {}: {}", elasticIp.getAllocationId(), e.getMessage());
		}
		elasticIp.setAssociationId(null);
		elasticIp.setInstanceId(null);
		elasticIp = saveElasticIp(elasticIp);
		kafkaService.sendElasticIp(elasticIp);
		return elasticIp;
	}

	public HostAddress getHost(RegionEnum region) {
		ElasticIp elasticIp = getElasticIp(region);
		String instanceId = elasticIp.getInstanceId();
		if (instanceId != null) {
			return cloudHostsService.getCloudHostById(instanceId).getAddress();
		}
		else {
			AwsRegion awsRegion = AwsRegion.getRegionsToAwsRegions().get(region);
			CloudHost cloudHost = cloudHostsService.launchInstance(awsRegion, InstanceType.T2Medium);
			String allocationId = elasticIp.getAllocationId();
			return associateElasticIpAddress(region, allocationId, cloudHost).getAddress();
		}
	}

	public void deleteElasticIp(Long id) {
		elasticIps.deleteById(id);
	}
}
