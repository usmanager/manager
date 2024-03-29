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

package pt.unl.fct.miei.usmanagement.manager.services.bash;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.unl.fct.miei.usmanagement.manager.exceptions.ManagerException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BashService {

	public String getUsername() {
		BashCommandResult usernameResult = executeCommandSync("whoami");
		if (!usernameResult.isSuccessful()) {
			throw new ManagerException("Unable to get username of this machine: %s", usernameResult.getError());
		}
		return usernameResult.getOutput().get(0);
	}

	public String getPublicIp() {
		BashCommandResult publicIpResult = executeCommandSync("curl https://ipinfo.io/ip");
		if (!publicIpResult.isSuccessful()) {
			throw new ManagerException("Unable to get public ip: %s", publicIpResult.getError());
		}
		return publicIpResult.getOutput().get(0);
	}

	public String getPrivateIp() {
		BashCommandResult privateIpResult = executeCommandSync("hostname -I | awk '{print $1}'");
		if (!privateIpResult.isSuccessful()) {
			throw new ManagerException("Unable to get private ip: %s", privateIpResult.getError());
		}
		return privateIpResult.getOutput().get(0);
	}

	public List<String> initDockerSwarm(String advertiseAddress, String listenAddress) {
		String command = String.format("docker swarm init --advertise-addr %s --listen-addr %s", /*--availability drain*/
			advertiseAddress, listenAddress);
		BashCommandResult initDockerSwarmResult = executeCommandSync(command);
		if (!initDockerSwarmResult.isSuccessful()) {
			throw new ManagerException("Unable to init docker swarm at %s: %s", advertiseAddress, initDockerSwarmResult.getError());
		}
		return initDockerSwarmResult.getOutput();
	}

	public void cleanup(String... files) {
		String cleanupCommand = String.format("rm -f" + Strings.repeat(" %s", files.length), (Object) files);
		executeCommandSync(cleanupCommand);
	}

	public BashCommandResult executeCommandSync(String command) {
		return executeCommand(command, true);
	}

	public void executeCommandAsync(String command) {
		executeCommand(command, false);
	}

	private BashCommandResult executeCommand(String command, boolean wait) {
		Runtime r = Runtime.getRuntime();
		String[] commands = {"bash", "-c", command};
		try {
			Process p = r.exec(commands);
			if (!wait) {
				log.info("Executing command: {}", command);
				return null;
			}
			p.waitFor();
			int exitStatus = p.exitValue();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			List<String> output = new LinkedList<>();
			String s;
			while ((s = stdInput.readLine()) != null) {
				output.add(s);
			}
			stdInput.close();
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			List<String> error = new LinkedList<>();
			while ((s = stdError.readLine()) != null) {
				error.add(s);
			}
			stdError.close();
			log.info("Executing command: {}\nResult: {}\nError: {}", command, String.join("\n", output), String.join("\n", error));
			return new BashCommandResult(command, exitStatus, output, error);
		}
		catch (InterruptedException | IOException e) {
			log.error("Failed to execute bash command: {}", command);
			return new BashCommandResult(command, -1, null, List.of(e.getMessage()));
		}
	}

	public void executeBackgroundProcess(String command, String outputFile) {
		String executeCommand = String.format("nohup %s >/dev/null 2>&1 & echo $! > /tmp/pid_%d.txt", command, System.currentTimeMillis());
		ProcessBuilder builder = new ProcessBuilder("bash", "-c", executeCommand);
		String file = String.format("%s_%d.log", outputFile == null ? "process" : outputFile, System.currentTimeMillis());
		Path outputFilePath = Paths.get("/tmp/" + file);
		builder.redirectOutput(outputFilePath.toFile());
		builder.redirectError(outputFilePath.toFile());
		try {
			builder.start();
		}
		catch (IOException e) {
			log.error("Failed to execute command {} in the background: {}", command, e.getMessage());
		}
	}

	public boolean stopBackgroundProcesses() {
		String filesCommand = "ls /tmp | grep pid_*";
		BashCommandResult filesCommandResult = executeCommand(filesCommand, true);
		if (filesCommandResult == null || !filesCommandResult.isSuccessful()) {
			return false;
		}
		List<String> files = filesCommandResult.getOutput();
		String killCommand = files.stream().map(file -> "kill -9 `cat /tmp/" + file + "`").collect(Collectors.joining("; "));
		executeCommand(killCommand, true);
		String rmCommand = "rm -rf " + files.stream().map(file -> "/tmp/" + file).collect(Collectors.joining(" "));
		BashCommandResult rmCommandResult = executeCommand(rmCommand, true);
		return rmCommandResult != null && rmCommandResult.isSuccessful();
	}
}
